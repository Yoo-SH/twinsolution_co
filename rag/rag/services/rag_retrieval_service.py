from typing import List, Optional, Dict, Any, Tuple
from langchain_core.documents import Document
import logging
import asyncio
from dataclasses import dataclass
from .rag_embedding_service import clova_embedding_service
from .rag_indexing_service import chroma_indexing_service

logger = logging.getLogger(__name__)

@dataclass
class RetrievalConfig:
    """
    검색 설정
    """
    top_k: int = 5  # 반환할 문서 개수
    similarity_threshold: float = 0.1  # 유사도 임계값 (0~1)
    enable_reranking: bool = False  # 리랭킹 사용 여부
    rerank_top_k: int = 10  # 리랭킹 시 고려할 상위 문서 개수

class RAGRetrievalService:
    """RAG 검색 서비스"""
    
    def __init__(self):
        self.embedding_service = clova_embedding_service
        self.indexing_service = chroma_indexing_service
    
    async def search_documents_async(self, 
                                   query: str, 
                                   config: Optional[RetrievalConfig] = None,
                                   filter_metadata: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """
        비동기 문서 검색
        
        Args:
            query: 검색 쿼리
            config: 검색 설정
            filter_metadata: 메타데이터 필터
            
        Returns:
            List[Dict]: 검색 결과 문서 리스트
        """
        config = config or RetrievalConfig()
        
        try:
            # 1. 쿼리 임베딩 생성
            logger.info(f"쿼리 임베딩 생성: '{query[:100]}...'")
            query_embedding = await self.embedding_service.aembed_query(query)
            
            # 2. 검색할 문서 개수 결정
            search_k = config.rerank_top_k if config.enable_reranking else config.top_k
            
            search_results = self.indexing_service.search_similar_documents(
                query_embedding=query_embedding,
                top_k=search_k,
                filter_metadata=filter_metadata
            )
            
            # 3. 유사도 임계값 필터링
            filtered_results = [
                result for result in search_results 
                if result['similarity'] >= config.similarity_threshold
            ]
            
            logger.info(f"유사도 필터링: {len(search_results)} -> {len(filtered_results)}개 문서")
            
            # 4. 리랭킹 (옵션)
            if config.enable_reranking and len(filtered_results) > config.top_k:
                reranked_results = await self._rerank_documents_async(
                    query, filtered_results[:config.rerank_top_k]
                )
                final_results = reranked_results[:config.top_k]
            else:
                final_results = filtered_results[:config.top_k]
            
            logger.info(f"최종 반환 문서: {len(final_results)}개")
            return final_results
            
        except Exception as e:
            logger.error(f"문서 검색 오류: {str(e)}")
            return []
    
    def search_documents(self, 
                        query: str, 
                        config: Optional[RetrievalConfig] = None,
                        filter_metadata: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """
        동기 문서 검색
        
        Args:
            query: 검색 쿼리
            config: 검색 설정
            filter_metadata: 메타데이터 필터
            
        Returns:
            List[Dict]: 검색 결과 문서 리스트
        """
        try:
            loop = asyncio.get_event_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
        
        return loop.run_until_complete(
            self.search_documents_async(query, config, filter_metadata)
        )
    
    async def _rerank_documents_async(self, 
                                    query: str, 
                                    documents: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        문서 리랭킹 (간단한 키워드 매칭 기반)
        
        Args:
            query: 검색 쿼리
            documents: 후보 문서 리스트
            
        Returns:
            List[Dict]: 리랭킹된 문서 리스트
        """
        try:
            # 쿼리 키워드 추출 (공백 기준)
            query_keywords = set(query.lower().split())
            
            # 각 문서에 대해 키워드 매칭 점수 계산
            for doc in documents:
                content_lower = doc['content'].lower()
                
                # 키워드 매칭 점수
                keyword_matches = sum(1 for keyword in query_keywords if keyword in content_lower)
                keyword_score = keyword_matches / len(query_keywords) if query_keywords else 0
                
                # 최종 점수 = 임베딩 유사도 * 0.7 + 키워드 점수 * 0.3
                original_similarity = doc['similarity']
                combined_score = original_similarity * 0.7 + keyword_score * 0.3
                
                doc['rerank_score'] = combined_score
                doc['keyword_score'] = keyword_score
            
            # 점수 기준 내림차순 정렬
            reranked_documents = sorted(documents, key=lambda x: x['rerank_score'], reverse=True)
            
            logger.info(f"리랭킹 결과: {len(reranked_documents)}개 문서")
            return reranked_documents
            
        except Exception as e:
            logger.error(f"문서 리랭킹 오류: {str(e)}")
            return documents
    
    def get_relevant_context(self, 
                           query: str, 
                           config: Optional[RetrievalConfig] = None,
                           max_context_length: int = 4000) -> str:
        """
        검색 결과 문서들을 컨텍스트로 생성
        
        Args:
            query: 검색 쿼리
            config: 검색 설정
            max_context_length: 최대 컨텍스트 길이
            
        Returns:
            str: 생성된 컨텍스트
        """
        search_results = self.search_documents(query, config)
        
        if not search_results:
            return ""
        
        # 검색 결과를 컨텍스트로 합치기
        context_parts = []
        current_length = 0
        
        for i, result in enumerate(search_results, 1):
            content = result['content']
            similarity = result['similarity']
            source = result['metadata'].get('source_file', 'Unknown')
            
            # 컨텍스트 부분 생성
            context_part = f"[문서 {i}] (유사도: {similarity:.3f}, 출처: {source})\n{content}\n"
            
            # 최대 길이 초과 시 중단
            if current_length + len(context_part) > max_context_length:
                break
            
            context_parts.append(context_part)
            current_length += len(context_part)
        
        context = "\n".join(context_parts)
        
        logger.info(f"컨텍스트 생성 결과: {len(context_parts)}개 문서, {len(context)}자")
        return context
    
    async def hybrid_search_async(self, 
                                query: str,
                                config: Optional[RetrievalConfig] = None,
                                filter_metadata: Optional[Dict[str, Any]] = None) -> Tuple[List[Dict[str, Any]], str]:
        """
        하이브리드 검색 (벡터 검색 + 키워드 검색)
        
        Args:
            query: 검색 쿼리
            config: 검색 설정
            filter_metadata: 메타데이터 필터
            
        Returns:
            Tuple[List[Dict], str]: (검색 결과, 컨텍스트)
        """
        config = config or RetrievalConfig()
        
        # 1. 벡터 검색
        vector_results = await self.search_documents_async(query, config, filter_metadata)
        
        # 2. 키워드 검색 (아직 미구현)
        keyword_results = self._keyword_search(query, filter_metadata)
        
        # 3. 결과 합치기 (중복 제거)
        combined_results = self._combine_search_results(vector_results, keyword_results)
        
        # 4. 컨텍스트 생성
        context = self._create_context_from_results(combined_results)
        
        return combined_results[:config.top_k], context
    
    def _keyword_search(self, 
                       query: str, 
                       filter_metadata: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """
        키워드 기반 검색 (미구현)
        
        Args:
            query: 검색 쿼리
            filter_metadata: 메타데이터 필터
            
        Returns:
            List[Dict]: 키워드 검색 결과
        """
        # 실제 구현 필요. 현재는 빈 리스트 반환.
        # 추후 DB 또는 색인에서 키워드 기반 검색 구현 가능.
        logger.info("키워드 검색은 아직 구현되어 있지 않습니다.")
        return []
    
    def _combine_search_results(self, 
                              vector_results: List[Dict[str, Any]], 
                              keyword_results: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        벡터/키워드 검색 결과 합치기
        
        Args:
            vector_results: 벡터 검색 결과
            keyword_results: 키워드 검색 결과
            
        Returns:
            List[Dict]: 합쳐진 검색 결과
        """
        seen_ids = set()
        combined = []
        
        # 벡터 검색 결과 먼저 추가
        for result in vector_results:
            if result['id'] not in seen_ids:
                seen_ids.add(result['id'])
                combined.append(result)
        
        # 키워드 검색 결과 추가 (중복 제거)
        for result in keyword_results:
            if result['id'] not in seen_ids:
                seen_ids.add(result['id'])
                combined.append(result)
        
        return combined
    
    def _create_context_from_results(self, 
                                   results: List[Dict[str, Any]], 
                                   max_length: int = 4000) -> str:
        """
        검색 결과로부터 컨텍스트 생성
        
        Args:
            results: 검색 결과
            max_length: 최대 길이
            
        Returns:
            str: 생성된 컨텍스트
        """
        if not results:
            return ""
        
        context_parts = []
        current_length = 0
        
        for i, result in enumerate(results, 1):
            content = result['content']
            similarity = result.get('similarity', 0)
            
            part = f"[문서 {i}] (유사도: {similarity:.3f})\n{content}\n"
            
            if current_length + len(part) > max_length:
                break
            
            context_parts.append(part)
            current_length += len(part)
        
        return "\n".join(context_parts)

# 싱글턴 인스턴스
rag_retrieval_service = RAGRetrievalService()