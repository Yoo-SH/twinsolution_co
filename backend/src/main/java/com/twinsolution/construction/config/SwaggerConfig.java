package com.twinsolution.construction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("건설 SLM 시스템 API")
                        .description("Twin Solution을 위한 AI 기반 건설 문서 관리 및 분석 시스템 API 문서입니다.\n\n" +
                                "본 시스템은 건설 프로젝트의 문서를 관리하고, AI를 활용하여 문서를 분석하며, " +
                                "챗봇을 통해 사용자와 상호작용할 수 있는 기능을 제공합니다.\n\n" +
                                "## 주요 기능\n" +
                                "- **프로젝트 관리**: 건설 프로젝트 생성 및 관리\n" +
                                "- **문서 관리**: 프로젝트별 문서 업로드, 조회, 삭제\n" +
                                "- **문서 분석**: AI를 활용한 문서 자동 분석 및 리포트 생성\n" +
                                "- **챗봇**: 프로젝트 문서 기반 AI 챗봇 대화\n" +
                                "- **청킹 시스템**: 문서를 작은 단위로 분할하여 효율적인 검색 및 분석 지원\n" +
                                "- **대시보드**: 전체 시스템 현황 요약\n" +
                                "- **API 연동**: 외부 API 관리 및 테스트\n" +
                                "- **설정 관리**: 시스템 설정 관리")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TRIPLE MAJOR")
                                .email("support@twinsolution.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://api.twinsolution.com")
                                .description("프로덕션 서버")
                ));
    }
}
