package com.cardsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkUploadResponse {
    private int totalRows;
    private int successCount;
    private int failedCount;
    private List<FailedRowDetail> failures;

    @Data
    @Builder
    public static class FailedRowDetail {
        private int rowNumber;
        private String studentNumber;
        private String errorMessage;
    }
}