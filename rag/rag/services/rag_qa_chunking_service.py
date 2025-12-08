"""
Q&A 형식 문서를 위한 커스텀 청킹 서비스
TwinSolutionGuideline.pdf와 같은 구조화된 Q&A 문서에 최적화
"""

import re
from typing import List, Dict, Optional
from langchain_core.documents import Document
import logging

logger = logging.getLogger(__name__)


class QAChunkingService:
    """Q&A 형식 문서를 위한 지능형 청킹 서비스"""

    def __init__(self):
        # Q&A 패턴 (Q1., Q2., Q3. 등)
        self.qa_pattern = re.compile(r'(Q\d+)\.\s+(.+?)(?=Q\d+\.|$)', re.DOTALL)

        # 섹션 헤더 패턴 (1.1, 1.2, 2, 3 등)
        self.section_pattern = re.compile(r'^(\d+(?:\.\d+)?)\s+(.+?)$', re.MULTILINE)

        # 법령 근거 패턴
        self.legal_basis_pattern = re.compile(r'•\s*근거:\s*(.+?)(?=•|$)', re.DOTALL)

    def extract_qa_chunks(self, text: str, metadata: Optional[Dict] = None) -> List[Document]:
        """
        Q&A 형식 텍스트를 각 질문-답변 단위로 청킹

        Args:
            text: 전체 텍스트
            metadata: 기본 메타데이터

        Returns:
            List[Document]: 청크된 문서 리스트
        """
        if not text.strip():
            return []

        metadata = metadata or {}
        chunks = []

        # 현재 섹션 정보 추적
        current_section = None
        current_section_title = None

        # 섹션 헤더 찾기
        sections = self._extract_sections(text)

        # Q&A 찾기
        qa_matches = list(self.qa_pattern.finditer(text))

        logger.info(f"총 {len(qa_matches)}개의 Q&A 발견")

        for match in qa_matches:
            q_number = match.group(1)  # Q1, Q2 등
            qa_content = match.group(2).strip()

            # 이 Q&A가 속한 섹션 찾기
            qa_start_pos = match.start()
            section_info = self._find_section_for_position(sections, qa_start_pos)

            # 질문과 답변 분리
            lines = qa_content.split('\n', 1)
            question = lines[0].strip()
            answer = lines[1].strip() if len(lines) > 1 else ""

            # 법령 근거 추출
            legal_basis = self._extract_legal_basis(answer)

            # 메타데이터 구성
            chunk_metadata = metadata.copy()
            chunk_metadata.update({
                'question_id': q_number,
                'question': question,
                'section_number': section_info['number'] if section_info else None,
                'section_title': section_info['title'] if section_info else None,
                'category': section_info['category'] if section_info else None,
                'has_legal_basis': bool(legal_basis),
                'legal_basis': legal_basis,
                'chunk_type': 'qa',
                'chunk_method': 'qa_pattern_recognition'
            })

            # 전체 Q&A를 하나의 청크로
            chunk_text = f"{q_number}. {question}\n\n{answer}"

            chunk_doc = Document(
                page_content=chunk_text,
                metadata=chunk_metadata
            )
            chunks.append(chunk_doc)

        logger.info(f"총 {len(chunks)}개 청크 생성 완료")
        return chunks

    def _extract_sections(self, text: str) -> List[Dict]:
        """
        문서에서 섹션 정보 추출

        Returns:
            List[Dict]: [{'number': '1.1', 'title': '소방 및 안전', 'position': 123, 'category': 'Fire Safety'}]
        """
        sections = []

        # 주요 섹션 매핑
        category_map = {
            '소방': 'Fire Safety',
            '안전': 'Fire Safety',
            '건축': 'Licensing & Admin',
            '인허가': 'Licensing & Admin',
            '감리': 'Supervision & Management',
            '건설 관리': 'Supervision & Management',
            '설계': 'Design & Technical Standards',
            '기술': 'Design & Technical Standards'
        }

        for match in self.section_pattern.finditer(text):
            section_num = match.group(1)
            section_title = match.group(2).strip()

            # 카테고리 판단
            category = None
            for keyword, cat in category_map.items():
                if keyword in section_title:
                    category = cat
                    break

            sections.append({
                'number': section_num,
                'title': section_title,
                'position': match.start(),
                'category': category
            })

        return sections

    def _find_section_for_position(self, sections: List[Dict], position: int) -> Optional[Dict]:
        """
        특정 위치가 속한 섹션 찾기
        """
        current_section = None
        for section in sections:
            if section['position'] <= position:
                current_section = section
            else:
                break
        return current_section

    def _extract_legal_basis(self, text: str) -> List[str]:
        """
        답변에서 법령 근거 추출

        Returns:
            List[str]: 법령 근거 목록
        """
        legal_bases = []
        matches = self.legal_basis_pattern.finditer(text)

        for match in matches:
            basis = match.group(1).strip()
            # 불필요한 공백 제거
            basis = re.sub(r'\s+', ' ', basis)
            legal_bases.append(basis)

        return legal_bases

    def extract_with_fallback(
        self,
        text: str,
        metadata: Optional[Dict] = None,
        fallback_chunk_size: int = 1500,
        fallback_overlap: int = 200
    ) -> List[Document]:
        """
        Q&A 패턴 인식 실패 시 RecursiveCharacterTextSplitter로 폴백
        """
        # 먼저 Q&A 패턴 시도
        chunks = self.extract_qa_chunks(text, metadata)

        if len(chunks) > 0:
            logger.info(f"Q&A 패턴 인식 성공: {len(chunks)}개 청크")
            return chunks

        # 패턴 인식 실패 시 폴백
        logger.warning("Q&A 패턴 인식 실패, RecursiveCharacterTextSplitter 사용")

        from langchain.text_splitter import RecursiveCharacterTextSplitter

        # Q&A 구분자를 우선순위로 설정
        splitter = RecursiveCharacterTextSplitter(
            chunk_size=fallback_chunk_size,
            chunk_overlap=fallback_overlap,
            separators=[
                "\n\nQ",  # Q&A 구분
                "\n\n## ",  # Markdown 헤더
                "\n\n",  # 문단
                "\n",  # 줄바꿈
                "。",  # 문장 끝 (한국어)
                ". ",  # 문장 끝 (영어)
                " ",  # 공백
                ""  # 문자
            ],
            length_function=len
        )

        fallback_chunks = splitter.create_documents([text], [metadata or {}])

        # 메타데이터에 폴백 방식 표시
        for chunk in fallback_chunks:
            chunk.metadata['chunk_method'] = 'recursive_character_fallback'

        logger.info(f"폴백 청킹 완료: {len(fallback_chunks)}개 청크")
        return fallback_chunks


# 싱글톤 인스턴스
qa_chunking_service = QAChunkingService()
