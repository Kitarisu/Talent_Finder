package com.talentfinder.ddc.project;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/candidater")
    public String candidater() {
        return "candidater";
    }

    @GetMapping("/candidatures")
    public String candidatures() {
        return "candidatures";
    }

    @GetMapping("/entreprises")
    public String entreprises() {
        return "entreprises";
    }

    @GetMapping("/postes")
    public String postes() {
        return "postes";
    }
}