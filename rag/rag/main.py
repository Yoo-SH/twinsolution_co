from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
import logging
import time
import sys
import asyncio
from core.config import settings

# Windows에서 ProactorEventLoop 사용 시 ConnectionResetError 해결
if sys.platform == 'win32':
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
# Chat completions, tasks, models는 Triage Server에서 직접 호출하므로 제거
# from apis.v1.chat_completions import router as chat_completions_router
# from apis.v1.tasks import router as tasks_router
# from apis.v1.models import router as models_router

# RAG 및 RAGAS 서비스 제공
from controller.v1.rag import router as rag_router
from controller.v1.ragas import router as ragas_router



app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
    openapi_tags=[
        {
            "name": "RAG",
            "description": "RAG (검색 증강 생성) 서비스 - 의료 문서 검색 및 벡터 DB 관리"
        },
        {
            "name": "RAGAS",
            "description": "RAGAS 평가 시스템 - RAG 시스템 성능 평가 및 분석 (Retrieval-Augmented Generation Assessment)"
        }
    ]
)


logging.basicConfig(
    level=getattr(logging, getattr(settings, "LOG_LEVEL", "INFO")),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

# 상세 요청/응답 로깅 미들웨어
@app.middleware("http")
async def detailed_logging_middleware(request: Request, call_next):
    start_time = time.time()
    
    # 요청 정보 로깅
    logger.info("=== RAG Service API 요청 시작 ===")
    logger.info(f"요청 URL: {request.method} {request.url}")
    logger.info(f"클라이언트 IP: {request.client.host if request.client else '알 수 없음'}")
    logger.info(f"User-Agent: {request.headers.get('user-agent', '없음')}")
    logger.info(f"Content-Type: {request.headers.get('content-type', '없음')}")
    
    # 헤더 로깅 (민감한 정보 제외)
    logger.info("=== 요청 헤더 ===")
    for header_name, header_value in request.headers.items():
        if header_name.lower() in ['authorization', 'cookie']:
            header_value = '[MASKED]'
        logger.info(f"{header_name}: {header_value}")
    
    # 쿼리 파라미터 로깅
    if request.query_params:
        logger.info("=== 쿼리 파라미터 ===")
        for param_name, param_value in request.query_params.items():
            logger.info(f"{param_name}: {param_value}")
    
    # 요청 본문 로깅 (POST/PUT 요청인 경우)
    body = None
    if request.method in ["POST", "PUT", "PATCH"]:
        try:
            body = await request.body()
            if body and len(body) < 10000:  # 10KB 미만인 경우만 로깅
                body_str = body.decode('utf-8')
                logger.info(f"=== 요청 본문 ===\n{body_str}")
            elif body:
                logger.info(f"=== 요청 본문 ===\n[크기가 큰 본문: {len(body)} bytes]")
        except Exception as e:
            logger.warning(f"요청 본문 읽기 실패: {e}")
    
    try:
        # 요청 처리
        response = await call_next(request)
        
        # 처리 시간 계산
        process_time = time.time() - start_time
        
        # 응답 정보 로깅
        logger.info("=== RAG Service API 응답 완료 ===")
        logger.info(f"응답 상태: {response.status_code}")
        logger.info(f"처리 시간: {process_time:.3f}초")
        logger.info(f"Content-Type: {response.headers.get('content-type', '없음')}")

        # 응답 헤더 로깅
        logger.info("=== 응답 헤더 ===")
        for header_name, header_value in response.headers.items():
            logger.info(f"{header_name}: {header_value}")

        # 성능 경고
        if process_time > 5.0:
            logger.warning(f"⚠️ 느린 응답 감지: {process_time:.3f}초 - {request.method} {request.url.path}")
        elif process_time > 1.0:
            logger.info(f"🐌 보통 속도 응답: {process_time:.3f}초 - {request.method} {request.url.path}")
        else:
            logger.info(f"⚡ 빠른 응답: {process_time:.3f}초 - {request.method} {request.url.path}")

        logger.info("=== RAG Service API 처리 완료 ===\n")
        
        return response
        
    except Exception as e:
        process_time = time.time() - start_time
        logger.error("=== RAG Service API 요청 처리 중 오류 발생 ===")
        logger.error(f"요청: {request.method} {request.url}")
        logger.error(f"처리 시간: {process_time:.3f}초")
        logger.error(f"오류 내용: {str(e)}")
        logger.error("=== RAG Service API 오류 처리 완료 ===\n")
        raise

# CORS 미들웨어 설정 (상세 로깅 포함)
allowed_origins = getattr(settings, 'CORS_ORIGINS', ["*"])
logger.info("=== CORS 설정 초기화 ===")
logger.info(f"허용된 Origins: {allowed_origins}")
logger.info(f"Credentials 허용: True")
logger.info(f"허용된 Methods: *")
logger.info(f"허용된 Headers: *")

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# API 라우터 등록 - RAG 및 RAGAS 서비스 제공
# chat_completions, tasks, models는 Triage Server에서 직접 CLOVA Studio를 호출
app.include_router(rag_router, prefix=settings.API_V1_STR)
app.include_router(ragas_router, prefix=settings.API_V1_STR)

@app.get("/")
async def root():
    return {
        "message": "CLOVAX RAG & RAGAS Service",
        "description": "의료 문서 검색, 벡터 DB 관리 및 RAG 시스템 성능 평가 서비스",
        "services": {
            "rag": "의료 문서 업로드, 임베딩, 벡터 검색",
            "ragas": "RAG 시스템 성능 평가, 분석, 리포트 생성"
        },
        "note": "채팅 기능은 Triage Server에서 직접 CLOVA Studio를 호출합니다",
        "version": settings.VERSION,
        "docs": "/docs"
    }

@app.get("/health")
async def health_check():
    return {"status": "healthy"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.RELOAD
    )
