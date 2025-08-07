package com.zhaoxinms.contract.tools.aicomponent.model;

import lombok.Data;
import java.util.List;

@Data
public class FulfillmentConfig {
    private List<String> taskTypes;
    private List<String> keywords;
    private List<String> timeRules;
    private List<String> selectedTaskTypes;
    private List<String> selectedKeywords;
    private List<String> selectedTimeRules;
}
