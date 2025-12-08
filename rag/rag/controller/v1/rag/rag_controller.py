from fastapi import APIRouter, HTTPException, UploadFile, File, Form, Path, Query
from fastapi.responses import JSONResponse
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field
import logging
import asyncio
import os
import tempfile
from datetime import datetime

# Services
from services.rag_document_pdf_loader_service import document_loader_service
from services.rag_document_csv_loader_service import csv_document_loader_service
from services.rag_text_spliter_service import clova_text_splitter_service, ChunkingConfig
from services.rag_embedding_service import clova_embedding_service
from services.rag_indexing_service import chroma_indexing_service
from services.rag_retrieval_service import rag_retrieval_service, RetrievalConfig

logger = logging.getLogger(__name__)
router = APIRouter()

# Pydantic 모델
class DocumentIndexRequest(BaseModel):
    """문서 색인 요청"""
    chunking_config: Optional[ChunkingConfig] = Field(None, description="청킹 설정")
    document_source: str = Field(..., description="문서 출처")

class DocumentIndexResponse(BaseModel):
    """문서 색인 응답"""
    success: bool = Field(..., description="성공 여부")
    message: str = Field(..., description="응답 메시지")
    document_count: int = Field(..., description="총 문서 개수")
    chunk_count: int = Field(..., description="총 청크 개수")
    indexed_ids: List[str] = Field(..., description="색인된 문서 ID 리스트")

class CSVIndexRequest(BaseModel):
    """CSV 문서 색인 요청"""
    document_source: str = Field(..., description="문서 출처")
    encoding: str = Field("utf-8", description="파일 인코딩 (utf-8, cp949, latin-1)")

class SearchRequest(BaseModel):
    """검색 요청"""
    query: str = Field(..., description="검색 쿼리", min_length=1)
    project_id: Optional[str] = Field(None, description="프로젝트 ID (필터링용)")
    top_k: int = Field(5, description="최대 반환 문서 개수", ge=1, le=20)
    similarity_threshold: float = Field(0.1, description="유사도 임계값", ge=0.0, le=1.0)
    enable_reranking: bool = Field(False, description="리랭킹 사용 여부")
    filter_source: Optional[str] = Field(None, description="문서 출처 필터")
    max_context_length: int = Field(4000, description="최대 컨텍스트 길이", ge=100, le=10000)

class SearchResponse(BaseModel):
    """검색 응답"""
    success: bool = Field(..., description="성공 여부")
    query: str = Field(..., description="검색 쿼리")
    results: List[Dict[str, Any]] = Field(..., description="검색 결과")
    context: str = Field(..., description="생성된 컨텍스트")
    contexts: List[str] = Field(..., description="검색된 문서 텍스트 리스트 (RAGAS 평가용)")
    total_results: int = Field(..., description="총 결과 개수")

class CollectionStatsResponse(BaseModel):
    """컬렉션 통계 응답"""
    success: bool = Field(..., description="성공 여부")
    stats: Dict[str, Any] = Field(..., description="통계 정보")

@router.post("/documents/upload",
             response_model=DocumentIndexResponse,
             tags=["RAG"],
             summary="PDF 문서 업로드 및 색인")
async def upload_pdf_and_index_document(
    file: UploadFile = File(..., description="업로드할 PDF 파일"),
    document_source: str = Form(..., description="문서 출처"),
    project_id: str = Form(..., description="프로젝트 ID"),
    alpha: float = Form(-100, description="청킹 alpha 값"),
    post_process_max_size: int = Form(2000, description="청킹 최대 길이"),
    post_process_min_size: int = Form(500, description="청킹 최소 길이")
):
    """
    PDF 문서를 업로드하여 RAG 벡터DB에 색인합니다.
    
    **처리 과정:**
    1. PDF 파일 업로드
    2. PyMuPDFLoader로 문서 추출
    3. 전처리
    4. Clova Studio API로 청크 분할
    5. BGE-M3 임베딩 생성
    6. ChromaDB에 벡터 색인
    
    **지원 파일:** PDF만 가능
    **최대 파일 크기:** 50MB
    """
    try:
        # 파일 확장자 확인
        if not file.filename.endswith('.pdf'):
            raise HTTPException(status_code=400, detail="PDF 파일만 업로드 가능합니다.")
        
        # 파일 크기 제한 (50MB)
        file_content = await file.read()
        if len(file_content) > 50 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="파일 크기는 50MB를 초과할 수 없습니다.")
        
        # 임시 파일 저장
        with tempfile.NamedTemporaryFile(delete=False, suffix='.pdf') as tmp_file:
            tmp_file.write(file_content)
            tmp_file_path = tmp_file.name
        
        try:
            # 1. 문서 추출 및 전처리
            logger.info(f"PDF 문서 추출 시작: {file.filename}")
            documents = document_loader_service.load_and_preprocess(tmp_file_path)
            
            if not documents:
                raise HTTPException(status_code=400, detail="문서에서 추출된 내용이 없습니다.")
            
            # 2. 청킹 설정
            chunking_config = ChunkingConfig(
                alpha=alpha,
                post_process_max_size=post_process_max_size,
                post_process_min_size=post_process_min_size
            )
            
            # 3. 문서 청크 분할
            logger.info("문서 청크 분할 시작")
            chunked_documents = await clova_text_splitter_service.split_documents_async(
                documents, chunking_config
            )
            
            if not chunked_documents:
                raise HTTPException(status_code=500, detail="문서 청크 분할에 실패했습니다.")
            
            # 4. 임베딩 생성
            logger.info(f"{len(chunked_documents)}개 청크 임베딩 생성 시작")
            texts = [doc.page_content for doc in chunked_documents]
            embeddings = await clova_embedding_service.aembed_documents(texts)
            
            # 5. 벡터 색인 (project_id 메타데이터 포함)
            logger.info(f"벡터 색인 시작 (project_id: {project_id})")
            # 각 문서에 project_id 메타데이터 추가
            for doc in chunked_documents:
                doc.metadata['project_id'] = project_id

            indexed_ids = chroma_indexing_service.add_documents(
                documents=chunked_documents,
                embeddings=embeddings,
                document_source=document_source
            )
            
            return DocumentIndexResponse(
                success=True,
                message=f"문서 '{file.filename}' 색인 완료",
                document_count=len(documents),
                chunk_count=len(chunked_documents),
                indexed_ids=indexed_ids
            )
            
        finally:
            # 임시 파일 삭제
            os.unlink(tmp_file_path)
            
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"문서 색인 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"문서 색인 중 오류 발생: {str(e)}")

@router.post("/documents/upload-csv",
             response_model=DocumentIndexResponse,
             tags=["RAG"],
             summary="CSV 문서 업로드 및 색인")
async def upload_csv_and_index_document(
    file: UploadFile = File(..., description="업로드할 CSV 파일"),
    document_source: str = Form(..., description="문서 출처"),
    project_id: str = Form(..., description="프로젝트 ID"),
    encoding: str = Form("utf-8", description="파일 인코딩 (utf-8, cp949, latin-1)")
):
    """
    CSV 문서를 업로드하여 RAG 벡터DB에 색인합니다.
    
    **처리 과정:**
    1. CSV 파일 업로드
    2. pandas로 CSV 데이터 읽기
    3. 각 행을 개별 Document로 변환 (행별 청킹)
    4. BGE-M3 임베딩 생성
    5. ChromaDB에 벡터 색인
    
    **특징:**
    - 각 CSV 행이 하나의 청크(Document)가 됨
    - 컬럼명과 값이 "컬럼명: 값" 형태로 저장
    - 각 행의 메타데이터에 모든 컬럼 정보 포함
    
    **지원 파일:** CSV만 가능
    **최대 파일 크기:** 50MB
    **지원 인코딩:** UTF-8, CP949, Latin-1
    """
    try:
        # 파일 확장자 확인
        if not file.filename.endswith('.csv'):
            raise HTTPException(status_code=400, detail="CSV 파일만 업로드 가능합니다.")
        
        # 파일 크기 제한 (50MB)
        file_content = await file.read()
        if len(file_content) > 50 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="파일 크기는 50MB를 초과할 수 없습니다.")
        
        # 임시 파일 저장
        tmp_file_path = csv_document_loader_service.save_temp_csv_file(
            file_content, file.filename.replace('.csv', '')
        )
        
        try:
            # 1. CSV 문서 추출 및 전처리 (각 행이 개별 Document)
            logger.info(f"CSV 문서 추출 시작: {file.filename}")
            documents = csv_document_loader_service.load_and_preprocess(
                tmp_file_path, encoding=encoding
            )
            
            if not documents:
                raise HTTPException(status_code=400, detail="CSV 파일에서 추출된 내용이 없습니다.")
            
            # 2. CSV는 이미 행별로 청킹되어 있으므로 추가 청킹 불필요
            # 각 행이 이미 하나의 청크임
            chunked_documents = documents

            # 3. 임베딩 생성
            logger.info(f"{len(chunked_documents)}개 행(청크) 임베딩 생성 시작")
            texts = [doc.page_content for doc in chunked_documents]
            embeddings = await clova_embedding_service.aembed_documents(texts)

            # 4. 벡터 색인 (project_id 메타데이터 포함)
            logger.info(f"벡터 색인 시작 (project_id: {project_id})")
            # 각 문서에 project_id 메타데이터 추가
            for doc in chunked_documents:
                doc.metadata['project_id'] = project_id

            indexed_ids = chroma_indexing_service.add_documents(
                documents=chunked_documents,
                embeddings=embeddings,
                document_source=document_source
            )
            
            return DocumentIndexResponse(
                success=True,
                message=f"CSV 파일 '{file.filename}' 색인 완료 (각 행이 개별 청크로 처리됨)",
                document_count=1,  # CSV 파일 1개
                chunk_count=len(chunked_documents),  # 행 개수 = 청크 개수
                indexed_ids=indexed_ids
            )
            
        finally:
            # 임시 파일 삭제
            os.unlink(tmp_file_path)
            
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"CSV 문서 색인 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"CSV 문서 색인 중 오류 발생: {str(e)}")

@router.post("/search", 
             response_model=SearchResponse, 
             tags=["RAG"], 
             summary="문서 검색")
async def search_documents(request: SearchRequest):
    """
    색인된 문서에서 유사한 내용을 검색합니다.
    
    **처리 과정:**
    1. 쿼리 BGE-M3 임베딩 생성
    2. ChromaDB에서 벡터 유사도 검색
    3. 유사도 임계값 필터링
    4. (옵션) 리랭킹
    5. 검색 결과로 컨텍스트 생성
    
    **예시:**
    - "파이썬에서 리스트를 정렬하는 방법"
    - "Python 파일 입출력 예제"
    """
    try:
        # 검색 설정
        retrieval_config = RetrievalConfig(
            top_k=request.top_k,
            similarity_threshold=request.similarity_threshold,
            enable_reranking=request.enable_reranking
        )
        
        # 메타데이터 필터 설정
        filter_metadata = {}

        # 프로젝트 ID 필터 (우선순위 높음)
        if request.project_id:
            filter_metadata["project_id"] = request.project_id
            logger.info(f"프로젝트 필터 적용: project_id={request.project_id}")

        # 문서 출처 필터 (추가 필터)
        if request.filter_source:
            filter_metadata["document_source"] = request.filter_source
            logger.info(f"문서 출처 필터 적용: document_source={request.filter_source}")

        # 필터가 없으면 None으로 설정
        if not filter_metadata:
            filter_metadata = None

        # 문서 검색
        logger.info(f"문서 검색 시작: '{request.query[:100]}...'")
        search_results = await rag_retrieval_service.search_documents_async(
            query=request.query,
            config=retrieval_config,
            filter_metadata=filter_metadata
        )
        
        # 컨텍스트 생성
        context = rag_retrieval_service._create_context_from_results(
            search_results, max_length=request.max_context_length
        )

        # contexts 리스트 생성 (RAGAS 평가용)
        contexts = [result['content'] for result in search_results]

        return SearchResponse(
            success=True,
            query=request.query,
            results=search_results,
            context=context,
            contexts=contexts,
            total_results=len(search_results)
        )
        
    except Exception as e:
        logger.error(f"문서 검색 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"문서 검색 중 오류 발생: {str(e)}")

@router.get("/search/{query}", 
            response_model=SearchResponse, 
            tags=["RAG"], 
            summary="간단 문서 검색")
async def simple_search(
    query: str = Path(..., description="검색 쿼리"),
    top_k: int = Query(5, description="최대 반환 문서 개수", ge=1, le=20),
    threshold: float = Query(0.1, description="유사도 임계값", ge=0.0, le=1.0)
):
    """
    GET 방식으로 간단히 문서 검색을 수행합니다.
    
    **예시:**
    - GET /v1/rag/search/파이썬?top_k=3&threshold=0.7
    """
    request = SearchRequest(
        query=query,
        top_k=top_k,
        similarity_threshold=threshold
    )
    return await search_documents(request)

@router.delete("/documents/{document_source}",
               tags=["RAG"],
               summary="문서 삭제 (출처 기준)")
async def delete_documents_by_source(
    document_source: str = Path(..., description="삭제할 문서 출처")
):
    """
    출처 기준으로 문서를 삭제합니다.

    **주의:** 해당 출처의 문서가 없을 수 있습니다.
    """
    try:
        deleted_count = chroma_indexing_service.delete_documents_by_source(document_source)

        if deleted_count == 0:
            raise HTTPException(
                status_code=404,
                detail=f"문서 출처 '{document_source}'에 해당하는 문서가 없습니다."
            )

        return {
            "success": True,
            "message": f"문서 출처 '{document_source}'에서 {deleted_count}개 문서 삭제 완료",
            "deleted_count": deleted_count
        }

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"문서 삭제 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"문서 삭제 중 오류 발생: {str(e)}")

@router.delete("/documents/project/{project_id}",
               tags=["RAG"],
               summary="프로젝트 문서 일괄 삭제")
async def delete_documents_by_project(
    project_id: str = Path(..., description="삭제할 프로젝트 ID")
):
    """
    특정 프로젝트의 모든 문서를 삭제합니다.

    **주의:** 해당 프로젝트의 모든 문서가 영구 삭제됩니다.
    """
    try:
        # 프로젝트 ID로 문서 조회
        results = chroma_indexing_service.collection.get(
            where={"project_id": project_id}
        )

        if not results['ids']:
            raise HTTPException(
                status_code=404,
                detail=f"프로젝트 ID '{project_id}'에 해당하는 문서가 없습니다."
            )

        # 문서 삭제
        chroma_indexing_service.collection.delete(ids=results['ids'])
        deleted_count = len(results['ids'])

        logger.info(f"프로젝트 '{project_id}'의 {deleted_count}개 문서 삭제 완료")

        return {
            "success": True,
            "message": f"프로젝트 '{project_id}'의 {deleted_count}개 문서 삭제 완료",
            "deleted_count": deleted_count,
            "project_id": project_id
        }

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"프로젝트 문서 삭제 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"프로젝트 문서 삭제 중 오류 발생: {str(e)}")

@router.get("/stats", 
            response_model=CollectionStatsResponse, 
            tags=["RAG"], 
            summary="컬렉션 통계")
async def get_collection_stats():
    """
    RAG 벡터DB의 통계 정보를 조회합니다.
    
    **제공 정보:**
    - 총 문서 개수
    - 임베딩 차원 정보
    - 색인 설정
    - 기타 정보
    """
    try:
        stats = chroma_indexing_service.get_collection_stats()
        
        return CollectionStatsResponse(
            success=True,
            stats=stats
        )
        
    except Exception as e:
        logger.error(f"통계 조회 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"통계 조회 중 오류 발생: {str(e)}")

@router.delete("/reset", 
               tags=["RAG"], 
               summary="컬렉션 전체 초기화")
async def reset_collection():
    """
    전체 RAG 컬렉션을 초기화합니다.
    
    **주의:** 모든 색인된 문서가 삭제됩니다. 신중히 사용하세요.
    """
    try:
        success = chroma_indexing_service.reset_collection()
        
        if not success:
            raise HTTPException(status_code=500, detail="컬렉션 초기화에 실패했습니다.")
        
        return {
            "success": True,
            "message": "RAG 컬렉션이 성공적으로 초기화되었습니다."
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"컬렉션 초기화 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"컬렉션 초기화 중 오류 발생: {str(e)}")

@router.get("/health", 
            tags=["RAG"], 
            summary="RAG 서비스 헬스체크")
async def health_check():
    """
    RAG 서비스의 헬스체크를 수행합니다.
    
    **점검 항목:**
    - 임베딩 서비스 정상 동작
    - 벡터 DB 연결 상태
    - API 응답 상태
    """
    try:
        # 임베딩 서비스 점검
        test_embedding = await clova_embedding_service.aembed_query("테스트")
        embedding_status = len(test_embedding) == clova_embedding_service.embedding_dimension
        
        # 벡터 DB 점검
        stats = chroma_indexing_service.get_collection_stats()
        db_status = bool(stats)
        
        status = {
            "success": True,
            "embedding_service": embedding_status,
            "vector_db": db_status,
            "timestamp": datetime.now().isoformat(),
            "version": "1.0.0"
        }
        
        if not (embedding_status and db_status):
            status["success"] = False
            return JSONResponse(status_code=503, content=status)
        
        return status
        
    except Exception as e:
        logger.error(f"헬스체크 오류: {str(e)}")
        return JSONResponse(
            status_code=503,
            content={
                "success": False,
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            }
        )