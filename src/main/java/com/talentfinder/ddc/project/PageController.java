package com.talentfinder.ddc.project;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/candidater")
    public String candidater() {
        return "candidater";
    }

    @GetMapping("/entreprises")
    public String entreprises() {
        return "entreprises";
    }

    @GetMapping("/candidatures")
    public String candidatures() {
        return "candidatures";
    }

}