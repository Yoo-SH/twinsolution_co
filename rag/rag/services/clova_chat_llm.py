"""CLOVA Studio Chat 모델을 위한 LangChain 래퍼

RAGAS 평가에서 OpenAI 대신 CLOVA Studio API를 사용하기 위한 래퍼 클래스입니다.
LangChain의 BaseChatModel을 구현하여 RAGAS와 호환됩니다.
"""

from typing import Any, List, Optional, Iterator
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import (
    BaseMessage,
    HumanMessage,
    AIMessage,
    SystemMessage,
    ChatMessage
)
from langchain_core.outputs import ChatGeneration, ChatResult, ChatGenerationChunk
from langchain_core.callbacks.manager import CallbackManagerForLLMRun
from pydantic import Field, ConfigDict
import httpx
import os
import logging
import json

logger = logging.getLogger(__name__)


class ClovaStudioChatModel(BaseChatModel):
    """CLOVA Studio HCX 모델을 위한 LangChain BaseChatModel 구현

    RAGAS 메트릭 계산을 위해 OpenAI ChatGPT 대신 CLOVA Studio API를 사용합니다.

    Attributes:
        model: CLOVA Studio 모델명 (HCX-005, HCX-DASH-002)
        temperature: 샘플링 온도 (0.0-1.0)
        max_tokens: 최대 생성 토큰 수
        api_key: CLOVA Studio API 키
        triage_server_url: Triage Server 엔드포인트

    Example:
        >>> llm = ClovaStudioChatModel(model="HCX-DASH-002", temperature=0.0)
        >>> messages = [HumanMessage(content="안녕하세요")]
        >>> result = llm.invoke(messages)
    """

    model_config = ConfigDict(arbitrary_types_allowed=True)

    model: str = Field(default="HCX-DASH-002", description="CLOVA Studio 모델명")
    temperature: float = Field(default=0.0, ge=0.0, le=1.0, description="샘플링 온도")
    max_tokens: int = Field(default=1000, gt=0, description="최대 생성 토큰 수")
    api_key: str = Field(default_factory=lambda: os.getenv("CLOVA_STUDIO_API_KEY", ""))
    triage_server_url: str = Field(
        default="http://localhost:8080",
        description="Triage Server 베이스 URL"
    )
    timeout: float = Field(default=60.0, description="API 호출 타임아웃 (초)")

    @property
    def _llm_type(self) -> str:
        """LLM 타입 식별자 반환"""
        return "clova-studio-chat"

    @property
    def _identifying_params(self) -> dict:
        """모델 식별 파라미터 반환"""
        return {
            "model": self.model,
            "temperature": self.temperature,
            "max_tokens": self.max_tokens,
        }

    def _convert_messages_to_clova_format(self, messages: List[BaseMessage]) -> List[dict]:
        """LangChain 메시지를 CLOVA Studio API 형식으로 변환

        Args:
            messages: LangChain 메시지 리스트

        Returns:
            CLOVA Studio API 형식의 메시지 리스트
        """
        clova_messages = []

        for msg in messages:
            if isinstance(msg, HumanMessage):
                role = "user"
            elif isinstance(msg, AIMessage):
                role = "assistant"
            elif isinstance(msg, SystemMessage):
                role = "system"
            elif isinstance(msg, ChatMessage):
                role = msg.role
            else:
                logger.warning(f"알 수 없는 메시지 타입: {type(msg)}, user로 처리")
                role = "user"

            clova_messages.append({
                "role": role,
                "content": msg.content
            })

        return clova_messages

    def _generate(
        self,
        messages: List[BaseMessage],
        stop: Optional[List[str]] = None,
        run_manager: Optional[CallbackManagerForLLMRun] = None,
        **kwargs: Any,
    ) -> ChatResult:
        """CLOVA Studio API를 호출하여 응답 생성

        Args:
            messages: 입력 메시지 리스트
            stop: 정지 시퀀스 (선택)
            run_manager: 콜백 매니저
            **kwargs: 추가 파라미터

        Returns:
            ChatResult: 생성된 응답
        """
        # 메시지 변환
        clova_messages = self._convert_messages_to_clova_format(messages)

        # API 요청 페이로드
        payload = {
            "messages": clova_messages,
            "temperature": self.temperature,
            "maxTokens": self.max_tokens,
        }

        if stop:
            payload["stop"] = stop

        # 추가 파라미터 병합
        payload.update(kwargs)

        # API 호출
        try:
            endpoint = f"{self.triage_server_url}/api/clovax/chat/completions/{self.model}"

            logger.debug(f"CLOVA API 호출: {endpoint}")
            logger.debug(f"요청 메시지 수: {len(clova_messages)}")

            with httpx.Client(timeout=self.timeout) as client:
                response = client.post(
                    endpoint,
                    json=payload,
                    headers={"Content-Type": "application/json"}
                )
                response.raise_for_status()
                result = response.json()

            # 응답 파싱
            if result.get("status", {}).get("code") != "20000":
                error_msg = result.get("status", {}).get("message", "Unknown error")
                raise Exception(f"CLOVA API 오류: {error_msg}")

            content = result.get("result", {}).get("message", {}).get("content", "")
            usage = result.get("result", {}).get("usage", {})

            logger.debug(f"응답 생성 완료 (토큰: {usage.get('totalTokens', 'N/A')})")

            # ChatResult 생성
            generation = ChatGeneration(
                message=AIMessage(content=content),
                generation_info={
                    "usage": usage,
                    "finish_reason": result.get("result", {}).get("finishReason", "stop"),
                    "model": self.model,
                }
            )

            return ChatResult(generations=[generation])

        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP 오류: {e.response.status_code} - {e.response.text}")
            raise Exception(f"CLOVA API HTTP 오류: {e.response.status_code}")
        except httpx.RequestError as e:
            logger.error(f"요청 오류: {str(e)}")
            raise Exception(f"CLOVA API 연결 실패: {str(e)}")
        except Exception as e:
            logger.error(f"예상치 못한 오류: {str(e)}")
            raise

    async def _agenerate(
        self,
        messages: List[BaseMessage],
        stop: Optional[List[str]] = None,
        run_manager: Optional[CallbackManagerForLLMRun] = None,
        **kwargs: Any,
    ) -> ChatResult:
        """비동기 응답 생성 (RAGAS는 주로 동기 방식 사용)

        Args:
            messages: 입력 메시지 리스트
            stop: 정지 시퀀스 (선택)
            run_manager: 콜백 매니저
            **kwargs: 추가 파라미터

        Returns:
            ChatResult: 생성된 응답
        """
        # 메시지 변환
        clova_messages = self._convert_messages_to_clova_format(messages)

        # API 요청 페이로드
        payload = {
            "messages": clova_messages,
            "temperature": self.temperature,
            "maxTokens": self.max_tokens,
        }

        if stop:
            payload["stop"] = stop

        payload.update(kwargs)

        # 비동기 API 호출
        try:
            endpoint = f"{self.triage_server_url}/api/clovax/chat/completions/{self.model}"

            logger.debug(f"CLOVA API 비동기 호출: {endpoint}")

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    endpoint,
                    json=payload,
                    headers={"Content-Type": "application/json"}
                )
                response.raise_for_status()
                result = response.json()

            # 응답 파싱
            if result.get("status", {}).get("code") != "20000":
                error_msg = result.get("status", {}).get("message", "Unknown error")
                raise Exception(f"CLOVA API 오류: {error_msg}")

            content = result.get("result", {}).get("message", {}).get("content", "")
            usage = result.get("result", {}).get("usage", {})

            # ChatResult 생성
            generation = ChatGeneration(
                message=AIMessage(content=content),
                generation_info={
                    "usage": usage,
                    "finish_reason": result.get("result", {}).get("finishReason", "stop"),
                    "model": self.model,
                }
            )

            return ChatResult(generations=[generation])

        except Exception as e:
            logger.error(f"비동기 호출 오류: {str(e)}")
            raise


# 편의 함수
def create_clova_chat_llm(
    model: str = "HCX-DASH-002",
    temperature: float = 0.0,
    max_tokens: int = 1000,
) -> ClovaStudioChatModel:
    """CLOVA Studio Chat 모델 인스턴스 생성

    Args:
        model: 모델명 (HCX-005 또는 HCX-DASH-002)
        temperature: 샘플링 온도
        max_tokens: 최대 토큰 수

    Returns:
        ClovaStudioChatModel 인스턴스

    Example:
        >>> llm = create_clova_chat_llm(model="HCX-DASH-002", temperature=0.0)
    """
    return ClovaStudioChatModel(
        model=model,
        temperature=temperature,
        max_tokens=max_tokens,
    )


if __name__ == "__main__":
    """테스트 코드"""
    import asyncio

    # 로깅 설정
    logging.basicConfig(
        level=logging.DEBUG,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    # 모델 생성
    llm = ClovaStudioChatModel(
        model="HCX-DASH-002",
        temperature=0.0,
    )

    # 테스트 메시지
    messages = [
        HumanMessage(content="안녕하세요. 간단히 인사해주세요.")
    ]

    print("=" * 60)
    print("CLOVA Studio Chat LLM 테스트")
    print("=" * 60)

    # 동기 호출 테스트
    print("\n[동기 호출 테스트]")
    try:
        result = llm.invoke(messages)
        # invoke()는 AIMessage를 반환하므로 직접 접근
        if hasattr(result, 'content'):
            # result가 AIMessage인 경우
            print(f"응답: {result.content}")
        else:
            # result가 ChatResult인 경우
            print(f"응답: {result.generations[0].message.content}")
            print(f"사용량: {result.generations[0].generation_info.get('usage', {})}")
    except Exception as e:
        print(f"오류: {e}")
        import traceback
        traceback.print_exc()

    # 비동기 호출 테스트
    print("\n[비동기 호출 테스트]")
    async def test_async():
        try:
            result = await llm.ainvoke(messages)
            # ainvoke()도 동일하게 처리
            if hasattr(result, 'content'):
                print(f"응답: {result.content}")
            else:
                print(f"응답: {result.generations[0].message.content}")
                print(f"사용량: {result.generations[0].generation_info.get('usage', {})}")
        except Exception as e:
            print(f"오류: {e}")
            import traceback
            traceback.print_exc()

    asyncio.run(test_async())

    print("\n" + "=" * 60)
    print("테스트 완료")
    print("=" * 60)
