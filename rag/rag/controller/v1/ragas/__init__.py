# RAGAS 평가 API 모듈
from .ragas_controller import router as ragas_router

from fastapi import APIRouter

# router 객체 생성
router = APIRouter()
router.include_router(ragas_router, prefix="/ragas")
