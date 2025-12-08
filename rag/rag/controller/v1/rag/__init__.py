# RAG API 모듈
from .rag_controller import router as rag_router

from fastapi import APIRouter

# router 객체 생성
router = APIRouter()
router.include_router(rag_router, prefix="/RAG")