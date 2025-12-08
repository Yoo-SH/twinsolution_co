import httpx
import asyncio
from typing import List, Optional, Dict, Any
from langchain_core.documents import Document
import logging
import os
from dataclasses import dataclass

logger = logging.getLogger(__name__)

@dataclass
class ChunkingConfig:
    """청킹 설정"""
    alpha: float = -100  # 텍스트 분할 강도 조정 파라미터
    seg_cnt: int = -1  # 텍스트 분할 개수 설정
    post_process: bool = True  # 후처리 사용
    post_process_max_size: int = 2000  # 분할 최대 크기
    post_process_min_size: int = 500   # 분할 최소 크기

class ClovaTextSplitterService:
    """Clova Studio 분할 서비스 API를 사용한 텍스트 청킹 서비스"""
    
    def __init__(self):
        self.api_key = os.getenv("CLOVA_STUDIO_API_KEY")
        self.request_id = os.getenv("CLOVA_STUDIO_REQUEST_ID")
        self.base_url = "https://clovastudio.stream.ntruss.com"
        self.endpoint = "/v1/api-tools/segmentation"
        
        if not self.api_key:
            logger.warning("CLOVA_STUDIO_API_KEY 환경 변수가 설정되지 않았습니다.")
    
    async def _make_api_request(self, text: str, config: ChunkingConfig) -> Dict[str, Any]:
        """
        Clova Studio 분할 서비스 API 요청
        
        Args:
            text: 분할할 텍스트
            config: 청킹 설정
            
        Returns:
            Dict: API 응답
        """
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}",
        }
        
        if self.request_id:
            headers["X-NCP-CLOVASTUDIO-REQUEST-ID"] = self.request_id
        
        payload = {
            "text": text,
            "alpha": config.alpha,
            "segCnt": config.seg_cnt,
            "postProcess": config.post_process,
            "postProcessMaxSize": config.post_process_max_size,
            "postProcessMinSize": config.post_process_min_size
        }
        
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{self.base_url}{self.endpoint}",
                json=payload,
                headers=headers
            )
            response.raise_for_status()
            return response.json()
    
    async def split_text_async(self, text: str, config: Optional[ChunkingConfig] = None) -> List[str]:
        """
        텍스트를 분할하는 비동기 함수
        
        Args:
            text: 분할할 텍스트 (최대 12만자)
            config: 청킹 설정
            
        Returns:
            List[str]: 분할된 텍스트 청크 리스트
        """
        if not text.strip():
            return []
        
        if not self.api_key:
            logger.warning("API 키가 없어서 분할 서비스 사용할 수 없습니다.")
            return self._fallback_split(text)
        
        config = config or ChunkingConfig()
        
        try:
            # 텍스트 길이 제한 (12만자 초과)
            if len(text) > 120000:
                logger.warning("텍스트 길이가 초과됩니다. 잘라서 처리합니다.")
                text = text[:120000]
            
            response = await self._make_api_request(text, config)
            
            if response.get("status", {}).get("code") != "20000":
                raise Exception(f"API 오류: {response.get('status', {}).get('message', 'Unknown error')}")
            
            result = response.get("result", {})
            topic_segments = result.get("topicSeg", [])
            
            # 분할된 결과를 청크로 변환
            chunks = []
            for segment in topic_segments:
                if segment:  # 빈 세그먼트 제외
                    chunk_text = " ".join(segment).strip()
                    if chunk_text:
                        chunks.append(chunk_text)
            
            logger.info(f"Clova Studio API로 {len(chunks)} 개 청크 생성")
            return chunks
            
        except Exception as e:
            logger.error(f"Clova Studio API 분할 실패: {str(e)}")
            logger.info("기본 분할 서비스 사용합니다.")
            return self._fallback_split(text)
    
    def split_text(self, text: str, config: Optional[ChunkingConfig] = None) -> List[str]:
        """
        텍스트를 분할하는 동기 함수 (비동기 함수를 감쌈)
        
        Args:
            text: 분할할 텍스트
            config: 청킹 설정
            
        Returns:
            List[str]: 분할된 텍스트 청크 리스트
        """
        try:
            loop = asyncio.get_event_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
        
        return loop.run_until_complete(self.split_text_async(text, config))
    
    def _fallback_split(self, text: str, max_chunk_size: int = 2000, overlap: int = 200) -> List[str]:
        """
        API 실패 시 기본 분할 서비스 사용
        
        Args:
            text: 분할할 텍스트
            max_chunk_size: 최대 청크 크기
            overlap: 중복 크기
            
        Returns:
            List[str]: 분할된 텍스트 청크 리스트
        """
        if not text.strip():
            return []
        
        # 문장 단위로 분할 (간단한 문장 구분자 기준)
        sentences = []
        current_sentence = ""
        
        for char in text:
            current_sentence += char
            if char in '.!?' and len(current_sentence.strip()) > 10:
                sentences.append(current_sentence.strip())
                current_sentence = ""
        
        if current_sentence.strip():
            sentences.append(current_sentence.strip())
        
        # 청크 생성
        chunks = []
        current_chunk = ""
        
        for sentence in sentences:
            if len(current_chunk + sentence) <= max_chunk_size:
                current_chunk += sentence + " "
            else:
                if current_chunk.strip():
                    chunks.append(current_chunk.strip())
                current_chunk = sentence + " "
        
        if current_chunk.strip():
            chunks.append(current_chunk.strip())
        
        return chunks
    
    async def split_documents_async(self, documents: List[Document], config: Optional[ChunkingConfig] = None) -> List[Document]:
        """
        Document 리스트를 청크로 분할 (비동기)
        
        Args:
            documents: 분할할 문서 리스트
            config: 청킹 설정
            
        Returns:
            List[Document]: 분할된 Document 청크 리스트
        """
        chunk_documents = []
        
        for doc in documents:
            chunks = await self.split_text_async(doc.page_content, config)
            
            for i, chunk in enumerate(chunks):
                chunk_metadata = doc.metadata.copy()
                chunk_metadata.update({
                    'chunk_index': i,
                    'total_chunks': len(chunks),
                    'chunk_method': 'clova_studio_segmentation',
                    'original_length': len(doc.page_content),
                    'chunk_length': len(chunk)
                })
                
                chunk_doc = Document(
                    page_content=chunk,
                    metadata=chunk_metadata
                )
                chunk_documents.append(chunk_doc)
        
        logger.info(f"총 {len(chunk_documents)} 개 청크 생성 완료")
        return chunk_documents
    
    def split_documents(self, documents: List[Document], config: Optional[ChunkingConfig] = None) -> List[Document]:
        """
        Document 리스트를 청크로 분할 (동기)
        
        Args:
            documents: 분할할 문서 리스트
            config: 청킹 설정
            
        Returns:
            List[Document]: 분할된 Document 청크 리스트
        """
        try:
            loop = asyncio.get_event_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
        
        return loop.run_until_complete(self.split_documents_async(documents, config))

# 싱글톤 인스턴스
clova_text_splitter_service = ClovaTextSplitterService()