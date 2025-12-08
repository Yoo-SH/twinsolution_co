# CLOVAX - RAG Service for Medical Triage

의료 트리아지 시스템을 위한 RAG(검색 증강 생성) 전용 서비스입니다.
의료 문서를 벡터 DB에 저장하고, 관련 문서를 검색하여 정확한 의료 정보를 제공합니다.

> **주요 변경사항**: 채팅 기능은 Triage Server(Spring Boot)에서 직접 CLOVA Studio API를 호출하도록 변경되었습니다.
> CLOVAX는 RAG 문서 검색 서비스만 제공합니다.

## 🚀 주요 기능

### 🧠 RAG (검색 증강 생성)
- **문서 업로드**: PDF 문서를 벡터 DB에 색인
- **의미 검색**: BGE-M3 임베딩을 통한 유사도 검색
- **컨텍스트 기반 답변**: 검색된 문서를 바탕으로 정확한 답변 생성
- **하이브리드 검색**: 벡터 검색 + 키워드 검색 (확장 가능)

### 🎨 기타 기능
- **완전한 API 문서**: FastAPI 자동 생성 문서
- **헬스 체크**: 서비스 상태 및 통계 조회
- **ChromaDB 통합**: 고성능 벡터 DB 활용

## 🏗️ 시스템 아키텍처

```mermaid
graph TB
    subgraph "클라이언트 계층"
        CLIENT[웹 클라이언트/앱]
    end

    subgraph "API Gateway 계층"
        FASTAPI[FastAPI Server<br/>main.py]
        SWAGGER[Swagger UI<br/>/docs]
    end

    subgraph "컨트롤러 계층"
        CHAT_CTRL[Chat Controller<br/>chat.py]
        STREAM_CTRL[Streaming Controller<br/>streaming.py]
        RAG_CTRL[RAG Controller<br/>rag_controller.py]
    end

    subgraph "서비스 계층"
        CLOVA_SVC[CLOVA Chat Service<br/>clova_chat_service.py]
        MEMORY_SVC[Chat Memory Service<br/>chat_memory_service.py]
        
        subgraph "RAG 서비스들"
            DOC_LOADER[Document Loader<br/>rag_document_loader_service.py]
            TEXT_SPLIT[Text Splitter<br/>rag_text_spliter_service.py]
            EMBED_SVC[Embedding Service<br/>rag_embedding_service.py]
            INDEX_SVC[Indexing Service<br/>rag_indexing_service.py]
            RETRIEVAL_SVC[Retrieval Service<br/>rag_retrieval_service.py]
        end
    end

    subgraph "데이터 계층"
        CHROMA_DB[(ChromaDB<br/>벡터 저장소)]
        MEMORY_STORE[(메모리 저장소<br/>세션 관리)]
    end

    subgraph "외부 API"
        CLOVA_API[CLOVA Studio API<br/>clovastudio.stream.ntruss.com]
    end

    CLIENT --> FASTAPI
    FASTAPI --> SWAGGER
    FASTAPI --> CHAT_CTRL
    FASTAPI --> STREAM_CTRL
    FASTAPI --> RAG_CTRL

    CHAT_CTRL --> CLOVA_SVC
    CHAT_CTRL --> MEMORY_SVC
    CHAT_CTRL --> RETRIEVAL_SVC

    STREAM_CTRL --> CLOVA_SVC
    STREAM_CTRL --> MEMORY_SVC
    STREAM_CTRL --> RETRIEVAL_SVC

    RAG_CTRL --> DOC_LOADER
    RAG_CTRL --> TEXT_SPLIT
    RAG_CTRL --> EMBED_SVC
    RAG_CTRL --> INDEX_SVC
    RAG_CTRL --> RETRIEVAL_SVC

    CLOVA_SVC --> CLOVA_API
    MEMORY_SVC --> MEMORY_STORE

    DOC_LOADER --> EMBED_SVC
    TEXT_SPLIT --> CLOVA_API
    EMBED_SVC --> CLOVA_API
    INDEX_SVC --> CHROMA_DB
    RETRIEVAL_SVC --> EMBED_SVC
    RETRIEVAL_SVC --> INDEX_SVC

    classDef controller fill:#e1f5fe
    classDef service fill:#f3e5f5
    classDef storage fill:#fff3e0
    classDef external fill:#ffebee

    class CHAT_CTRL,STREAM_CTRL,RAG_CTRL controller
    class CLOVA_SVC,MEMORY_SVC,DOC_LOADER,TEXT_SPLIT,EMBED_SVC,INDEX_SVC,RETRIEVAL_SVC service
    class CHROMA_DB,MEMORY_STORE storage
    class CLOVA_API external
```

## 💾 기술 스택

```mermaid
graph LR
    subgraph "백엔드"
        PYTHON[Python 3.8+]
        FASTAPI[FastAPI]
        PYDANTIC[Pydantic V2]
        UVICORN[Uvicorn]
    end

    subgraph "AI/ML"
        LANGCHAIN[LangChain]
        BGE[BGE-M3 Embedding]
        CLOVA[CLOVA Studio]
    end

    subgraph "벡터 DB"
        CHROMA[ChromaDB]
        COSINE[Cosine Similarity]
    end

    subgraph "문서 처리"
        PYMUPDF[PyMuPDF]
        PDF[PDF Loader]
    end

    subgraph "HTTP"
        HTTPX[HTTPX]
        REQUESTS[Requests]
    end

    classDef backend fill:#e8f5e8
    classDef ai fill:#fff3e0
    classDef vector fill:#e3f2fd
    classDef doc fill:#fce4ec
    classDef http fill:#f1f8e9

    class PYTHON,FASTAPI,PYDANTIC,UVICORN backend
    class LANGCHAIN,BGE,CLOVA ai
    class CHROMA,COSINE vector
    class PYMUPDF,PDF doc
    class HTTPX,REQUESTS http
```

## 🔄 채팅 처리 플로우

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant CC as Chat Controller
    participant MS as Memory Service
    participant RS as RAG Service
    participant CS as CLOVA Service
    participant DB as ChromaDB
    participant API as CLOVA API

    C->>CC: 채팅 요청 (useRAG: true)
    
    alt RAG 사용
        CC->>RS: 문서 검색 요청
        RS->>DB: 벡터 유사도 검색
        DB-->>RS: 관련 문서 반환
        RS-->>CC: 컨텍스트 생성
    end
    
    alt 멀티턴 대화
        CC->>MS: 이전 대화 조회
        MS-->>CC: 대화 기록 반환
    end
    
    CC->>CS: CLOVA API 호출
    CS->>API: HTTP 요청
    API-->>CS: AI 응답
    CS-->>CC: 처리된 응답
    
    CC->>MS: 대화 저장
    CC-->>C: 최종 응답 (ragUsed: true)
```

## 📚 RAG 파이프라인

```mermaid
graph LR
    subgraph "문서 업로드 과정"
        A[PDF 업로드] --> B[문서 추출<br/>PyMuPDF]
        B --> C[텍스트 전처리]
        C --> D[청크 분할<br/>CLOVA Segmentation]
        D --> E[임베딩 생성<br/>BGE-M3]
        E --> F[벡터 색인<br/>ChromaDB]
    end
    
    subgraph "검색 과정"
        G[사용자 쿼리] --> H[쿼리 임베딩<br/>BGE-M3]
        H --> I[코사인 유사도<br/>검색]
        I --> J[임계값 필터링<br/>threshold > 0.1]
        J --> K[컨텍스트 생성]
        K --> L[프롬프트 통합]
    end
    
    F -.-> I
    
    classDef upload fill:#e8f5e8
    classDef search fill:#fff3e0
    
    class A,B,C,D,E,F upload
    class G,H,I,J,K,L search
```

## 🧠 메모리 관리 시스템

```mermaid
graph TD
    subgraph "세션 관리"
        A[새 요청] --> B{sessionId 존재?}
        B -->|No| C[UUID 생성]
        B -->|Yes| D[기존 세션 사용]
        C --> E[새 세션 시작]
        D --> E
    end
    
    subgraph "메모리 타입"
        E --> F{memoryType}
        F -->|buffer_window| G[Buffer Window<br/>최근 N개 대화]
        F -->|token_buffer| H[Token Buffer<br/>토큰 수 제한]
    end
    
    subgraph "대화 저장"
        I[AI 응답 생성] --> J[사용자 메시지 저장]
        J --> K[AI 응답 저장]
        K --> L[메모리 관리<br/>오래된 대화 제거]
    end
    
    G --> I
    H --> I
    
    classDef session fill:#e3f2fd
    classDef memory fill:#f3e5f5
    classDef storage fill:#fff3e0
    
    class A,B,C,D,E session
    class F,G,H memory
    class I,J,K,L storage
```

## 📋 지원 모델

### HCX-005 (비전 모델)
- 최대 128,000 토큰 (입력 + 출력)
- 이미지 입력 지원 (최대 5개)
- 추론 모델 지원
- 지원 형식: BMP, PNG, JPG, JPEG, WEBP
- 이미지 크기: 20MB 이하
- 이미지 비율: 1:5 ~ 5:1

### HCX-DASH-002 (경량화 모델)
- 최대 32,000 토큰 (입력 + 출력)
- 텍스트 전용
- 빠른 응답 속도

## 🚫 제한사항

### 모든 모델에서 지원하지 않는 기능
- **Function Calling**: 지원하지 않음
- **Structured Outputs**: 지원하지 않음

### Task API 제한사항
- 이미지 입력 미지원
- 추론 기능 미지원
- Function Calling 미지원
- Structured Outputs 미지원

### 추론 모델 제한사항
- `stop` 파라미터 사용 불가
- `maxTokens`와 `maxCompletionTokens` 동시 사용 불가

## 🛠️ 설치 및 실행

### 1. 의존성 설치
```bash
pip install -r requirements.txt
```

### 2. 환경 변수 설정
`.env` 파일을 생성하고 다음 내용을 추가하세요:

```env
# CLOVA Studio API 설정
CLOVA_STUDIO_API_KEY=your_clova_studio_api_key_here
CLOVA_STUDIO_BASE_URL=https://clovastudio.stream.ntruss.com

# 서버 설정
HOST=0.0.0.0
PORT=8000
RELOAD=false
DEBUG=false
```

### 3. 서버 실행
```bash
python main.py
```

또는 uvicorn을 직접 사용:
```bash
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload --log-level debug
```

## 📖 API 사용법

### 기본 채팅
```json
POST /api/v1/chat-completions/HCX-005
{
  "messages": [
    {
      "role": "user",
      "content": "안녕하세요!"
    }
  ]
}
```

### RAG 기반 채팅
```json
POST /api/v1/chat-completions/HCX-005
{
  "messages": [
    {
      "role": "user",
      "content": "BTS 지민에 대해 알려주세요"
    }
  ],
  "useRAG": true,
  "ragTopK": 3,
  "ragThreshold": 0.1
}
```

### 멀티턴 대화
```json
POST /api/v1/chat-completions/HCX-005
{
  "messages": [
    {
      "role": "user",
      "content": "이전에 무엇을 얘기했나요?"
    }
  ],
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "memoryType": "buffer_window",
  "memoryK": 10
}
```

### 문서 업로드
```bash
curl -X POST "http://localhost:8000/api/v1/RAG/documents/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@document.pdf" \
  -F "document_source=my_document" \
  -F "alpha=-100" \
  -F "post_process_max_size=2000" \
  -F "post_process_min_size=500"
```

## 📊 모니터링

### 헬스체크
```bash
GET /api/v1/RAG/health
```

### 통계 조회
```bash
GET /api/v1/RAG/stats
```

### API 문서
서버 실행 후 다음 URL에서 Swagger 문서를 확인할 수 있습니다:
- **Swagger UI**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`

## 🏃‍♂️ 실행 예시

1. **서버 시작**
   ```bash
   python main.py
   ```

2. **문서 업로드**
   ```bash
   curl -X POST "http://localhost:8000/api/v1/RAG/documents/upload" \
     -F "file=@article.pdf" \
     -F "document_source=news_articles"
   ```

3. **RAG 채팅 테스트**
   ```bash
   curl -X POST "http://localhost:8000/api/v1/chat-completions/HCX-005" \
     -H "Content-Type: application/json" \
     -d '{
       "messages": [{"role": "user", "content": "업로드한 문서에서 BTS 관련 소식을 알려줘"}],
       "useRAG": true
     }'
   ```

## 🔧 개발 가이드

### 프로젝트 구조
```
clovax/
├── main.py                     # FastAPI 애플리케이션 진입점
├── core/
│   └── config.py              # 설정 관리
├── apis/v1/
│   ├── chat_completions/      # 채팅 API
│   │   ├── chat.py           # 일반 채팅
│   │   └── streaming.py      # 스트리밍 채팅
│   └── rag/
│       └── rag_controller.py # RAG API
├── services/                  # 비즈니스 로직
│   ├── clova_chat_service.py # CLOVA API 통신
│   ├── chat_memory_service.py # 메모리 관리
│   ├── rag_*.py              # RAG 관련 서비스들
└── schemas/                   # 데이터 모델
    ├── request/
    └── response/
```

### 확장 가능한 기능들
- **키워드 검색**: 벡터 검색과 함께 사용할 키워드 검색
- **문서 필터링**: 특정 출처나 타입의 문서만 검색
- **리랭킹**: 검색 결과를 다시 정렬하는 고급 기능
- **다중 임베딩**: 여러 임베딩 모델을 동시에 사용
- **실시간 문서 업데이트**: 문서 변경 시 자동 재색인

이 프로젝트는 확장성을 고려하여 설계되었으며, 새로운 기능을 쉽게 추가할 수 있습니다.