package com.zhaoxin.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 */
@Controller
public class HomeController {
    
    /**
     * 首页
     */
    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }
}

