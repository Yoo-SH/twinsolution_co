from typing import List, Optional, Dict, Any
import chromadb
from chromadb.config import Settings
from langchain_core.documents import Document
from langchain_community.vectorstores import Chroma
import logging
import os
import uuid
from datetime import datetime

logger = logging.getLogger(__name__)

class ChromaIndexingService:
    """Chroma DB 인덱싱 서비스"""

    def __init__(self, 
                 persist_directory: str = "./chroma_db",
                 collection_name: str = "rag_documents"):
        """
        ChromaDB 인덱싱 서비스 초기화

        Args:
            persist_directory: 저장 디렉토리 경로
            collection_name: 컬렉션 이름
        """
        self.persist_directory = persist_directory
        self.collection_name = collection_name

        # 디렉토리 생성
        os.makedirs(persist_directory, exist_ok=True)

        # Chroma 클라이언트 생성
        self.client = chromadb.PersistentClient(
            path=persist_directory,
            settings=Settings(
                anonymized_telemetry=False,
                allow_reset=True
            )
        )

        # 컬렉션 초기화
        self._init_collection()

    def _init_collection(self):
        """컬렉션 초기화"""
        try:
            # 기존 컬렉션 목록 조회
            collections = self.client.list_collections()
            collection_names = [col.name for col in collections]

            if self.collection_name in collection_names:
                self.collection = self.client.get_collection(self.collection_name)
                logger.info(f"기존 컬렉션 '{self.collection_name}' 불러옴")
            else:
                # 새 컬렉션 생성 (코사인 유사도 사용)
                self.collection = self.client.create_collection(
                    name=self.collection_name,
                    metadata={"hnsw:space": "cosine"}  # BGE-M3에서 코사인 유사도 사용
                )
                logger.info(f"새 컬렉션 '{self.collection_name}' 생성 완료")

        except Exception as e:
            logger.error(f"컬렉션 초기화 실패: {str(e)}")
            raise

    def add_documents(self, 
                     documents: List[Document], 
                     embeddings: List[List[float]],
                     document_source: str = "unknown") -> List[str]:
        """
        문서와 임베딩을 컬렉션에 추가

        Args:
            documents: 문서 리스트
            embeddings: 임베딩 벡터 리스트
            document_source: 문서 출처 (예: 파일명)

        Returns:
            List[str]: 추가된 문서의 ID 리스트
        """
        if len(documents) != len(embeddings):
            raise ValueError("문서와 임베딩의 개수가 일치하지 않습니다.")

        try:
            # 문서 ID 생성
            doc_ids = [str(uuid.uuid4()) for _ in range(len(documents))]

            # 메타데이터 및 텍스트 추출
            metadatas = []
            texts = []

            for doc in documents:
                metadata = doc.metadata.copy()
                metadata.update({
                    'document_source': document_source,
                    'indexed_at': datetime.now().isoformat(),
                    'embedding_model': 'bge-m3',
                    'similarity_metric': 'cosine'
                })
                metadatas.append(metadata)
                texts.append(doc.page_content)

            # ChromaDB에 추가
            self.collection.add(
                embeddings=embeddings,
                documents=texts,
                metadatas=metadatas,
                ids=doc_ids
            )

            logger.info(f"{len(documents)}개의 문서를 컬렉션 '{self.collection_name}'에 추가 완료")
            return doc_ids

        except Exception as e:
            logger.error(f"문서 인덱싱 실패: {str(e)}")
            raise

    def search_similar_documents(self, 
                               query_embedding: List[float], 
                               top_k: int = 5,
                               filter_metadata: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """
        유사 문서 검색

        Args:
            query_embedding: 쿼리 임베딩 벡터
            top_k: 반환할 문서 개수
            filter_metadata: 메타데이터 필터

        Returns:
            List[Dict]: 검색 결과 리스트
        """
        try:
            # ChromaDB에서 검색
            results = self.collection.query(
                query_embeddings=[query_embedding],
                n_results=top_k,
                where=filter_metadata
            )

            # 결과 정리
            search_results = []

            if results['documents'] and results['documents'][0]:
                for i in range(len(results['documents'][0])):
                    result = {
                        'id': results['ids'][0][i],
                        'content': results['documents'][0][i],
                        'metadata': results['metadatas'][0][i],
                        'distance': results['distances'][0][i],
                        'similarity': 1 - results['distances'][0][i]  # 코사인 거리 → 유사도
                    }
                    search_results.append(result)

            logger.debug(f"검색 결과: {len(search_results)}개 문서")
            return search_results

        except Exception as e:
            logger.error(f"문서 검색 실패: {str(e)}")
            return []

    def update_document(self, 
                       doc_id: str, 
                       content: str, 
                       embedding: List[float], 
                       metadata: Optional[Dict[str, Any]] = None) -> bool:
        """
        문서 업데이트

        Args:
            doc_id: 문서 ID
            content: 새 문서 내용
            embedding: 새 임베딩 벡터
            metadata: 새 메타데이터

        Returns:
            bool: 업데이트 성공 여부
        """
        try:
            update_metadata = metadata or {}
            update_metadata['updated_at'] = datetime.now().isoformat()

            self.collection.update(
                ids=[doc_id],
                embeddings=[embedding],
                documents=[content],
                metadatas=[update_metadata]
            )

            logger.info(f"문서 {doc_id} 업데이트 완료")
            return True

        except Exception as e:
            logger.error(f"문서 업데이트 실패: {str(e)}")
            return False

    def delete_document(self, doc_id: str) -> bool:
        """
        문서 삭제

        Args:
            doc_id: 삭제할 문서 ID

        Returns:
            bool: 삭제 성공 여부
        """
        try:
            self.collection.delete(ids=[doc_id])
            logger.info(f"문서 {doc_id} 삭제 완료")
            return True

        except Exception as e:
            logger.error(f"문서 삭제 실패: {str(e)}")
            return False

    def delete_documents_by_source(self, document_source: str) -> int:
        """
        특정 출처의 문서 일괄 삭제

        Args:
            document_source: 문서 출처

        Returns:
            int: 삭제된 문서 개수
        """
        try:
            # 출처로 문서 조회
            results = self.collection.get(
                where={"document_source": document_source}
            )

            if results['ids']:
                self.collection.delete(ids=results['ids'])
                deleted_count = len(results['ids'])
                logger.info(f"출처 '{document_source}'의 {deleted_count}개 문서 삭제 완료")
                return deleted_count
            else:
                logger.info(f"출처 '{document_source}'에 해당하는 문서가 없습니다.")
                return 0

        except Exception as e:
            logger.error(f"문서 일괄 삭제 실패: {str(e)}")
            return 0

    def get_collection_stats(self) -> Dict[str, Any]:
        """
        컬렉션 통계 정보 반환

        Returns:
            Dict: 통계 정보
        """
        try:
            count = self.collection.count()

            stats = {
                'collection_name': self.collection_name,
                'total_documents': count,
                'persist_directory': self.persist_directory,
                'similarity_metric': 'cosine',
                'embedding_dimension': 1024,  # BGE-M3
                'embedding_model': 'bge-m3'
            }

            return stats

        except Exception as e:
            logger.error(f"통계 정보 조회 실패: {str(e)}")
            return {}

    def reset_collection(self) -> bool:
        """
        컬렉션 초기화 (모든 데이터 삭제)

        Returns:
            bool: 초기화 성공 여부
        """
        try:
            # 컬렉션 삭제 후 재생성
            self.client.delete_collection(self.collection_name)
            self._init_collection()

            logger.info(f"컬렉션 '{self.collection_name}' 초기화 완료")
            return True

        except Exception as e:
            logger.error(f"컬렉션 초기화 실패: {str(e)}")
            return False

# 싱글톤 인스턴스
chroma_indexing_service = ChromaIndexingService()