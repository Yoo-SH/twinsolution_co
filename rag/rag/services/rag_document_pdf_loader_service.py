from langchain_community.document_loaders import PyMuPDFLoader
from langchain_core.documents import Document
from typing import List, Optional
import logging
import os
import re

logger = logging.getLogger(__name__)

class DocumentLoaderService:
    """PDF 문서 로더 서비스"""

    def __init__(self):
        self.supported_extensions = ['.pdf']

    def load_pdf(self, file_path: str) -> List[Document]:
        """
        PDF 파일을 로드하여 Document 리스트로 반환합니다.

        Args:
            file_path: PDF 파일 경로

        Returns:
            List[Document]: 로드된 Document 리스트
        """
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"파일을 찾을 수 없습니다: {file_path}")

        if not file_path.lower().endswith('.pdf'):
            raise ValueError("지원하지 않는 파일 형식입니다. PDF 파일만 허용됩니다.")

        try:
            loader = PyMuPDFLoader(file_path)
            documents = loader.load()

            # 메타데이터 추가
            for doc in documents:
                doc.metadata['source_file'] = os.path.basename(file_path)
                doc.metadata['file_type'] = 'pdf'
                doc.metadata['loader_type'] = 'PyMuPDFLoader'

            logger.info(f"PDF 문서 로드 완료: {len(documents)}개 문서")
            return documents

        except Exception as e:
            logger.error(f"PDF 로드 오류: {str(e)}")
            raise Exception(f"PDF 로드 중 오류 발생: {str(e)}")

    def preprocess_text(self, text: str) -> str:
        """
        텍스트 전처리

        Args:
            text: 원본 텍스트

        Returns:
            str: 전처리된 텍스트
        """
        if not text:
            return ""

        # 1. 연속된 공백을 하나로 치환
        text = re.sub(r'\s+', ' ', text)

        # 2. 허용된 문자(한글, 영문, 숫자, 일부 특수문자)만 남김
        text = re.sub(r'[^\w\s가-힣.,!?;:()\-\'"]+', '', text)

        # 3. 연속된 개행을 하나로 치환
        text = re.sub(r'\n+', '\n', text)

        # 4. 양쪽 공백 제거
        text = text.strip()

        return text

    def merge_documents_by_page(self, documents: List[Document]) -> List[Document]:
        """
        여러 페이지의 Document를 하나로 합칩니다.

        Args:
            documents: 원본 Document 리스트

        Returns:
            List[Document]: 병합된 Document 리스트
        """
        if not documents:
            return []

        merged_docs = []
        current_content = ""
        current_metadata = documents[0].metadata.copy()

        for doc in documents:
            # 텍스트 전처리
            processed_text = self.preprocess_text(doc.page_content)

            if processed_text.strip():
                current_content += processed_text + "\n\n"

        if current_content.strip():
            merged_doc = Document(
                page_content=current_content.strip(),
                metadata=current_metadata
            )
            merged_docs.append(merged_doc)

        return merged_docs

    def load_and_preprocess(self, file_path: str) -> List[Document]:
        """
        PDF 파일을 로드하고 전처리하여 반환합니다.

        Args:
            file_path: PDF 파일 경로

        Returns:
            List[Document]: 전처리된 Document 리스트
        """
        # 1. PDF 로드
        documents = self.load_pdf(file_path)

        # 2. 페이지 병합 및 전처리
        processed_documents = self.merge_documents_by_page(documents)

        logger.info(f"전처리된 문서 개수: {len(processed_documents)}개")
        return processed_documents

# 싱글톤 인스턴스 생성
document_loader_service = DocumentLoaderService()