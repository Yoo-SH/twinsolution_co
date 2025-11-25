CREATE TABLE `Chat_Message` (
	`id`	PK	NOT NULL	COMMENT '개별 메시지의 고유 식별자입니다.',
	`session_id`	FK	NOT NULL	COMMENT '이 메시지가 속한 대화 세션의 ID입니다. Chat_Session 테이블의 session_id를 참조하는 외래 키로, 한 세션에 여러 메시지가 연결됩니다. 세션이 삭제되면 관련 메시지도 모두 삭제(ON DELETE CASCADE)되도록 설정합니다.',
	`role`	VARCHAR(10)	NOT NULL	COMMENT '메시지 보낸 주체를 나타냅니다. 'user' (사용자) 또는 'assistant' (AI) 등의 값을 가지며, 이를 통해 대화에서 누가 말한 내용인지 구분합니다.',
	`content`	TEXT	NOT NULL	COMMENT '메시지의 실제 내용입니다. 사용자의 질문 텍스트 혹은 AI의 답변 텍스트가 여기에 저장됩니다.',
	`created_at`	DATETIME	NOT NULL	COMMENT '메시지가 시스템에 기록된 시간을 나타냅니다. 메시지 생성 시 자동으로 현재 시간으로 저장되며, 대화 순서를 시간 순으로 정렬하는데 사용됩니다.'
);

CREATE TABLE `API_Log` (
	`id`	PK	NOT NULL	COMMENT 'API 호출 로그 항목의 고유 ID입니다.',
	`api_id`	FK	NOT NULL	COMMENT '어떤 API에 대한 호출인지 나타냅니다. API 테이블의 api_id를 참조하는 외래 키입니다. 해당 API 설정이 삭제될 경우 로그들도 함께 삭제될 수 있습니다.',
	`status`	VARCHAR(10)	NOT NULL	COMMENT '해당 호출의 결과 상태를 저장합니다. 예: 'success' (성공) 또는 'error' (실패) 등의 값으로 호출 결과를 표시합니다.',
	`status_CODE`	INT	NOT NULL	COMMENT '필요하다면 HTTP 응답 코드를 문자열로 저장해도 됩니다 (예: '200', '404').',
	`message`	TEXT	NULL	COMMENT '호출 결과에 대한 부가 메시지나 요약 정보를 저장합니다. 예를 들어 성공 시 "OK", 실패 시 오류 유형이나 코드 (예: "Timeout", "Invalid Key") 등을 기록합니다.',
	`created_at`	DATETIME	NULL
);

CREATE TABLE `API` (
	`id`	PK	NOT NULL	COMMENT '외부 API 설정의 고유 식별자입니다.',
	`name`	VARCHAR(100)	NOT NULL	COMMENT 'API의 이름 또는 설명입니다. 예를 들어 '공공데이터포털_건축비' 등 해당 API가 무엇인지 알 수 있는 명칭입니다.',
	`base_url`	VARCHAR(255)	NOT NULL	COMMENT 'PI 호출에 사용할 기본 URL 엔드포인트입니다. 예를 들어 https://www.g2b.go.kr/api/construction-prices와 같은 전체 경로 또는 주요 도메인을 저장합니다.',
	`method`	VARCHAR(10)	NOT NULL	COMMENT 'API 호출 방식 (HTTP 메소드)을 저장합니다. 'GET', 'POST' 등 API 요청에 필요한 메소드를 명시합니다',
	`auth_key`	VARCHAR(10)	NULL	COMMENT 'API 호출에 필요한 인증 키 또는 토큰을 저장합니다. 공공데이터 포털의 인증키와 같이 필요 시 사용되며, 없는 API의 경우 NULL이 될 수 있습니다. (필요한 경우 암호화하여 저장을 고려)',
	`status`	VARCHAR(10)	NOT NULL	COMMENT '해당 API의 상태를 나타냅니다. 'active'(활성) 또는 'inactive'(비활성) 등으로 사용 설정 여부를 표시하며, 만약 최근 호출에서 오류가 발생했다면 'error' 상태로 변경할 수 있습니다.',
	`craeted_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL
);

CREATE TABLE `Document` (
	`id`	PK	NOT NULL	COMMENT '문서의 고유 식별자이며, 기본 키입니다. 각 업로드된 또는 생성된 문서마다 부여됩니다.',
	`project_id`	FK	NOT NULL	COMMENT '당 문서가 속한 프로젝트의 ID입니다. Project 테이블의 project_id를 참조하는 외래 키로서, 문서는 반드시 하나의 프로젝트에 연결됩니다. 프로젝트가 삭제될 경우 해당 프로젝트의 문서들도 함께 삭제되도록 ON DELETE CASCADE 규칙을 설정할 수 있습니다.',
	`name`	VARCHAR(255)	NOT NULL	COMMENT '문서 이름 또는 파일명을 저장합니다. 예를 들어 업로드된 파일의 원래 이름이나 생성된 문서의 제목입니다.',
	`file_type`	VARCHAR(10)	NOT NULL	COMMENT '문서 파일 형식을 저장합니다. 예: 'PDF', 'HWP', 'DOCX', 'TXT' 등 파일의 확장자나 유형을 나타냅니다.',
	`file_path`	VARCHAR(255)	NOT NULL	COMMENT '서버 내에 저장된 문서 파일의 경로 또는 식별자를 저장합니다. 예를 들어 파일시스템 경로나 클라우드 스토리지 URL 등이 될 수 있습니다.',
	`origin`	VARCHAR(10)	NOT NULL	COMMENT '문서의 출처를 기록합니다. 'uploaded' (사용자 업로드 문서) 또는 'generated' (AI에 의해 생성된 문서) 등의 값을 가질 수 있습니다.',
	`status`	VARCHAR(20)	NOT NULL	COMMENT '문서 처리 상태를 나타냅니다. 예를 들어 '업로드됨', '분할완료', '분석완료' 등의 상태를 정의하여 문서의 처리 단계 진행 상황을 추적합니다.',
	`chunk_size`	INT	NULL	COMMENT '이 문서를 청크로 분할할 때 사용한 청크 크기를 저장합니다 (문서의 텍스트를 몇 글자 또는 몇 바이트 단위로 잘랐는지). 문서 업로드 시 사용자가 별도로 지정하지 않았다면 기본 청크 크기 값을 사용하며, 문서가 아직 분할되지 않은 경우 NULL일 수 있습니다.',
	`chunck_overlap`	INT	NULL	COMMENT '청크 분할 시 겹쳐지는 부분의 크기를 저장합니다. 예를 들어 청크 간에 200자씩 중복된다면 200으로 기록합니다. 이 값도 문서 분할 수행 시 설정되며, 분할 전에는 NULL일 수 있습니다.',
	`created_at`	DATETIME	NOT NULL	COMMENT '문서가 시스템에 업로드되거나 생성된 날짜와 시간을 저장합니다.',
	`updated_at`	DATETIME	NULL
);

CREATE TABLE `project` (
	`id`	PK	NOT NULL	COMMENT '프로젝트를 식별하는 기본 키 식별자입니다. 각 프로젝트마다 고유한 값이며 NULL 값을 허용하지 않습니다.',
	`name`	VARCHAR(100	NOT NULL	COMMENT '프로젝트 이름을 저장합니다. 예를 들어, 특정 건축허가 신청 건의 이름이나 제목입니다.',
	`description`	JSON	NULL	COMMENT '프로젝트에 대한 설명이나 부가 정보를 저장합니다. 길이에 제한이 없으며 필요할 경우 NULL일 수 있습니다.',
	`status`	VARCHAR(20)	NOT NULL	COMMENT '프로젝트의 현재 상태를 나타냅니다. 예를 들어 '준비중', '진행중', '완료' 등의 값으로 프로젝트 진행 상태를 표현합니다',
	`created_at`	DATETIME	NOT NULL	COMMENT '프로젝트의 현재 상태를 나타냅니다. 예를 들어 '준비중', '진행중', '완료' 등의 값으로 프로젝트 진행 상태를 표현합니다',
	`updated_at`	DATETIME	NULL	COMMENT '프로젝트가 완료된 시점을 기록합니다. 프로젝트 완료 시점에 이 필드가 채워지며, 완료 전에는 NULL로 남겨둡니다.'
);

CREATE TABLE `setting` (
	`id`	PK	NOT NULL	COMMENT '청크 설정 레코드의 식별자입니다. (일반적으로 하나의 설정 레코드만 사용됨)',
	`chunk_size`	INT	NOT NULL	COMMENT '문서를 청크로 분할할 때 기본적으로 사용할 청크 크기 값입니다. 예를 들어 1000이라면 한 청크에 최대 1000자의 텍스트를 담습니다.',
	`chunk_overlap`	INT	NOT NULL	COMMENT '청크 분할 시 겹치는 부분의 크기 값입니다. 예를 들어 200이라면 청크 간 200자의 중복이 발생하도록 분할합니다.',
	`updated_at`	DATETIME	NULL
);

CREATE TABLE `Chat_Session` (
	`id`	PK	NOT NULL	COMMENT '대화 세션의 고유 식별자입니다.',
	`project_id`	FK	NOT NULL	COMMENT '해당 대화가 연관된 프로젝트의 ID입니다. Project 테이블의 project_id를 참조하는 외래 키입니다. (이번 설계에서는 사용자 권한을 다루지 않으므로 프로젝트 단위로 대화 컨텍스트를 구분합니다.)',
	`created_at`	DATETIME	NOT NULL	COMMENT '대화 세션이 시작된 시간입니다. 새로운 대화창을 열었을 때의 시간이 자동 저장됩니다.',
	`updated_at`	DATETIME	NOT NULL	COMMENT '세션에서 마지막으로 대화가 오간 시각입니다. 사용자가 질문하거나 AI가 답변할 때마다 이 필드를 현재 시각으로 업데이트하여, 세션의 최신 활동 시간을 추적합니다. 대시보드 등에 최근 대화 목록을 표시할 때 사용됩니다.',
	`quick_question`	JSON	NOT NULL	COMMENT '빠른 질문 예시데이터를 넣을 수 있는 필드입니다.'
);

CREATE TABLE `Document_Chunk` (
	`id`	PK	NOT NULL	COMMENT '청크의 고유 식별자이며 기본 키입니다.',
	`document_id`	FK	NOT NULL	COMMENT '해당 청크가 속한 원본 문서의 ID입니다. Document 테이블의 document_id를 참조하는 외래 키로, 청크는 반드시 하나의 문서에 귀속됩니다. 문서가 삭제되면 관련 청크들도 삭제되도록 ON DELETE CASCADE를 설정합니다.',
	`index`	INT	NOT NULL	COMMENT '청크의 순서 인덱스입니다. 원본 문서에서 청크들이 생성된 순서를 나타내며, 0 또는 1부터 시작하는 일련번호로 저장됩니다. 이를 통해 원본 문서의 청크 순서를 복원할 수 있습니다.',
	`content`	TEXT	NOT NULL	COMMENT '청크에 해당하는 실제 텍스트 내용입니다. 원본 문서에서 분리된 한 조각의 내용으로, 청크 크기(chunk_size)에 따라 잘라낸 문자열을 저장합니다. (주의: 텍스트가 매우 클 경우를 대비해 TEXT 타입으로 설정하였습니다.)',
	`created_at`	DATETIME	NOT NULL
);

CREATE TABLE `Analysis_Report` (
	`id`	PK	NOT NULL	COMMENT '분석 결과 레코드의 고유 식별자입니다',
	`document_id`	FK	NOT NULL	COMMENT '어떤 문서에 대한 분석 결과인지를 나타냅니다. Document 테이블의 해당 문서를 가리키는 외래 키입니다. 문서가 삭제될 경우 연결된 분석 결과도 삭제되도록 설정할 수 있습니다.',
	`content`	TEXT	NOT NULL	COMMENT '분석 결과의 종류를 나타내는 필드입니다. 예를 들어 'summary' (요약), 'extraction' (정보 추출) 등 결과의 유형을 표시합니다. 하나의 문서에 대해 여러 종류의 분석 결과를 저장할 수 있음을 염두에 둔 컬럼입니다.',
	`created_at`	DATETIME	NOT NULL
);

ALTER TABLE `Chat_Message` ADD CONSTRAINT `PK_CHAT_MESSAGE` PRIMARY KEY (
	`id`,
	`session_id`
);

ALTER TABLE `API_Log` ADD CONSTRAINT `PK_API_LOG` PRIMARY KEY (
	`id`,
	`api_id`
);

ALTER TABLE `API` ADD CONSTRAINT `PK_API` PRIMARY KEY (
	`id`
);

ALTER TABLE `Document` ADD CONSTRAINT `PK_DOCUMENT` PRIMARY KEY (
	`id`,
	`project_id`
);

ALTER TABLE `project` ADD CONSTRAINT `PK_PROJECT` PRIMARY KEY (
	`id`
);

ALTER TABLE `setting` ADD CONSTRAINT `PK_SETTING` PRIMARY KEY (
	`id`
);

ALTER TABLE `Chat_Session` ADD CONSTRAINT `PK_CHAT_SESSION` PRIMARY KEY (
	`id`,
	`project_id`
);

ALTER TABLE `Document_Chunk` ADD CONSTRAINT `PK_DOCUMENT_CHUNK` PRIMARY KEY (
	`id`,
	`document_id`
);

ALTER TABLE `Analysis_Report` ADD CONSTRAINT `PK_ANALYSIS_REPORT` PRIMARY KEY (
	`id`,
	`document_id`
);

ALTER TABLE `Chat_Message` ADD CONSTRAINT `FK_Chat_Session_TO_Chat_Message_1` FOREIGN KEY (
	`session_id`
)
REFERENCES `Chat_Session` (
	`id`
);

ALTER TABLE `API_Log` ADD CONSTRAINT `FK_API_TO_API_Log_1` FOREIGN KEY (
	`api_id`
)
REFERENCES `API` (
	`id`
);

ALTER TABLE `Document` ADD CONSTRAINT `FK_project_TO_Document_1` FOREIGN KEY (
	`project_id`
)
REFERENCES `project` (
	`id`
);

ALTER TABLE `Chat_Session` ADD CONSTRAINT `FK_project_TO_Chat_Session_1` FOREIGN KEY (
	`project_id`
)
REFERENCES `project` (
	`id`
);

ALTER TABLE `Document_Chunk` ADD CONSTRAINT `FK_Document_TO_Document_Chunk_1` FOREIGN KEY (
	`document_id`
)
REFERENCES `Document` (
	`id`
);

ALTER TABLE `Analysis_Report` ADD CONSTRAINT `FK_Document_TO_Analysis_Report_1` FOREIGN KEY (
	`document_id`
)
REFERENCES `Document` (
	`id`
);

