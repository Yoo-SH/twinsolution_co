# Twinsolution Co

## Frontend Setup

1. 루트의 `frontend` 디렉터리에서 필요한 의존성을 설치합니다.
   ```bash
   cd frontend
   npm install
   ```
2. 환경 변수를 위해 `.env` 파일을 생성하고 백엔드 주소를 지정합니다.
   ```bash
   VITE_API_BASE_URL=http://localhost:8080
   ```
   `.env` 파일은 Git에 커밋되지 않으므로 로컬에서만 관리하세요.
3. 개발 서버 실행:
   ```bash
   npm run dev
   ```

## Backend & Swagger

- 백엔드를 실행하려면 루트에서 `./gradlew bootRun`을 수행합니다.
- 모든 REST API는 Swagger UI에서 확인할 수 있습니다:  
  `http://localhost:8080/swagger-ui/index.html`
- Swagger 문서에는 엔드포인트 설명, 요청/응답 예시, 프로젝트 기반 AI 채팅 옵션이 모두 포함되어 있으니 프론트엔드 개발 시 참고하세요.

## Notes

- OpenAI API 키는 백엔드 환경 변수로만 관리되고 저장소에 포함되지 않습니다.
- 프론트엔드의 AI 채팅 기능은 프로젝트별 엔드포인트(`/api/projects/{projectId}/messages`)를 사용합니다.
