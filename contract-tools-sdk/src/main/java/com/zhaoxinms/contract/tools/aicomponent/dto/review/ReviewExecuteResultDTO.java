package com.zhaoxinms.contract.tools.aicomponent.dto.review;

import lombok.Data;

import java.util.List;

@Data
public class ReviewExecuteResultDTO {
    private String traceId;
    private Long profileId;
    private List<Long> pointIds;
    private List<FindingDTO> findings;

    @Data
    public static class FindingDTO {
        private Long clauseTypeId;
        private String clauseTypeName;
        private Long pointId;
        private String pointCode;
        private String pointName;
        private String algorithmType;
        private List<PromptSimpleDTO> prompts;
    }

    @Data
    public static class PromptSimpleDTO {
        private String promptKey;
        private String name;
        private String message;
        private String statusType;
    }
}


