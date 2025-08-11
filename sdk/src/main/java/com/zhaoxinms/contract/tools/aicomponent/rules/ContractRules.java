package com.zhaoxinms.contract.tools.aicomponent.rules;


/**
 * Contract rules root model that mirrors JSON structure under
 * resources/contract-extract-rules/*.json
 */
public class ContractRules {
    private String version;
    private String contractType;
    private PromptSpec prompt;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public PromptSpec getPrompt() { return prompt; }
    public void setPrompt(PromptSpec prompt) { this.prompt = prompt; }
}


