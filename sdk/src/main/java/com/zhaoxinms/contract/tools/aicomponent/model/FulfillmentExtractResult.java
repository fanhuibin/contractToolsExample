package com.zhaoxinms.contract.tools.aicomponent.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class FulfillmentExtractResult {
    private boolean success;
    private String message;
    private List<Map<String, Object>> tasks;
}
