package com.twinsolution.construction.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

public class DashboardDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryResponse {
        private Long totalDocuments;
        private Integer documentsDelta;
        private Long inProgressProjects;
        private Long completedProjects;
        private Long chatSessionsCount;
        private Integer chatSessionsDelta;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeSeriesResponse {
        private List<TimeSeriesData> data;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeSeriesData {
        private String date;
        private Long count;
    }
}
