package com.twinsolution.construction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

public class ProjectDto {

    @Schema(name = "ProjectRequest", description = "프로젝트 생성 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @Schema(description = "프로젝트 이름 (필수)", example = "서울시 강남구 오피스텔 신축 공사", required = true)
        private String name;

        @Schema(description = "프로젝트 설명", example = "지하 2층, 지상 15층 규모의 오피스텔 건설 프로젝트")
        private String description;

        @Schema(description = "위치 (필수)", example = "서울특별시 강남구 역삼동 123-45", required = true)
        private String location;

        @Schema(description = "지역/지구 (필수)", example = "준주거지역", required = true)
        private String zoning;

        @Schema(description = "용도 (필수)", example = "오피스텔", required = true)
        private String usage;

        @Schema(description = "연면적 (제곱미터, 필수)", example = "5000.5", required = true)
        private Double totalFloorArea;

        @Schema(description = "층수 (필수)", example = "지하 2층, 지상 15층", required = true)
        private String floors;

        @Schema(description = "주차 대수 (필수)", example = "50", required = true)
        private Integer parkingSpaces;

        @Schema(description = "프로젝트 상태 (선택사항, 기본값: 초기단계)",
                example = "초기단계",
                allowableValues = {"초기단계", "승인대기", "서류검토", "설계진행"})
        private String status;

        @Schema(description = "LLM Provider (선택사항, 기본값: OPENAI)",
                example = "OPENAI",
                allowableValues = {"OPENAI", "OLLAMA"})
        private String llmProvider;

        @Schema(description = "LLM 모델명 (선택사항, 기본값: gpt-3.5-turbo)",
                example = "gpt-3.5-turbo")
        private String modelName;
    }

    @Schema(name = "ProjectResponse", description = "프로젝트 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        @Schema(description = "프로젝트 ID", example = "1")
        private Long id;

        @Schema(description = "프로젝트 이름", example = "서울시 강남구 오피스텔 신축 공사")
        private String name;

        @Schema(description = "프로젝트 설명", example = "지하 2층, 지상 15층 규모의 오피스텔 건설 프로젝트")
        private String description;

        @Schema(description = "위치", example = "서울특별시 강남구 역삼동 123-45")
        private String location;

        @Schema(description = "지역/지구", example = "준주거지역")
        private String zoning;

        @Schema(description = "용도", example = "오피스텔")
        private String usage;

        @Schema(description = "연면적 (제곱미터)", example = "5000.5")
        private Double totalFloorArea;

        @Schema(description = "층수", example = "지하 2층, 지상 15층")
        private String floors;

        @Schema(description = "주차 대수", example = "50")
        private Integer parkingSpaces;

        @Schema(description = "프로젝트 상태", example = "초기단계")
        private String status;

        @Schema(description = "진행률 (0-100)", example = "25")
        private Integer progress;

        @Schema(description = "LLM Provider", example = "OPENAI")
        private String llmProvider;

        @Schema(description = "LLM 모델명", example = "gpt-3.5-turbo")
        private String modelName;

        @Schema(description = "생성일시")
        private LocalDateTime createdAt;

        @Schema(description = "수정일시")
        private LocalDateTime updatedAt;

        @Schema(description = "최근 채팅 세션 일시")
        private LocalDateTime latestChatSessionAt;
    }

    @Schema(name = "ProjectSummary", description = "프로젝트 요약 정보")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        @Schema(description = "프로젝트 ID", example = "1")
        private Long id;

        @Schema(description = "프로젝트 이름", example = "서울시 강남구 오피스텔 신축 공사")
        private String name;

        @Schema(description = "프로젝트 상태", example = "초기단계")
        private String status;

        @Schema(description = "진행률 (0-100)", example = "25")
        private Integer progress;

        @Schema(description = "생성일시")
        private LocalDateTime createdAt;
    }

    @Schema(name = "ProjectUpdateRequest", description = "프로젝트 수정 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        @Schema(description = "프로젝트 이름", example = "서울시 강남구 오피스텔 신축 공사")
        private String name;

        @Schema(description = "프로젝트 설명", example = "지하 2층, 지상 15층 규모의 오피스텔 건설 프로젝트")
        private String description;

        @Schema(description = "위치", example = "서울특별시 강남구 역삼동 123-45")
        private String location;

        @Schema(description = "지역/지구", example = "준주거지역")
        private String zoning;

        @Schema(description = "용도", example = "오피스텔")
        private String usage;

        @Schema(description = "연면적 (제곱미터)", example = "5000.5")
        private Double totalFloorArea;

        @Schema(description = "층수", example = "지하 2층, 지상 15층")
        private String floors;

        @Schema(description = "주차 대수", example = "50")
        private Integer parkingSpaces;

        @Schema(description = "프로젝트 상태",
                example = "설계진행",
                allowableValues = {"초기단계", "승인대기", "서류검토", "설계진행"})
        private String status;

        @Schema(description = "LLM Provider",
                example = "OLLAMA",
                allowableValues = {"OPENAI", "OLLAMA"})
        private String llmProvider;

        @Schema(description = "LLM 모델명",
                example = "llama2")
        private String modelName;
    }
}
