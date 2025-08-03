package com.zhaoxinms.contract.tools.onlyoffice.configurations;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope("prototype")
@Getter
@Setter
public class Review {
    private String reviewDisplay = "markup";
    private Boolean trackChanges = false;
}
