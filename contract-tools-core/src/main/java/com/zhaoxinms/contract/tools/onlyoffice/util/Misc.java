package com.zhaoxinms.contract.tools.onlyoffice.util;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class Misc {
    public String convertUserDescriptions(String username, List<String> description) { // cenvert user descriptions to
                                                                                       // the specified format
        String result = "<div class=\"user-descr\"><b>" + username + "</b><br/><ul>"
            + description.stream().map(text -> "<li>" + text + "</li>").collect(Collectors.joining()) + "</ul></div>";
        return result;
    }
}
