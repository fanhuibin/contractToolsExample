package com.zhaoxinms.contract.tools.aicomponent.rules;

import java.util.List;
import java.util.Map;

/**
 * Prompt specification loaded from rules JSON.
 */
public class PromptSpec {
    private List<String> global;
    private List<String> negative;
    private Map<String, List<String>> fields; // key: field name, value: list of instructions
    private List<String> format;

    public List<String> getGlobal() { return global; }
    public void setGlobal(List<String> global) { this.global = global; }
    public List<String> getNegative() { return negative; }
    public void setNegative(List<String> negative) { this.negative = negative; }
    public Map<String, List<String>> getFields() { return fields; }
    public void setFields(Map<String, List<String>> fields) { this.fields = fields; }
    public List<String> getFormat() { return format; }
    public void setFormat(List<String> format) { this.format = format; }
}


