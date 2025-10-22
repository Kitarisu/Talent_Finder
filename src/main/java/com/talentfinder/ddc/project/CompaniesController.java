package com.talentfinder.ddc.project;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CompaniesController {

    // Stockée dans une ArrayList (modifiable à l'exécution si besoin)
    private final List<String> companies = new ArrayList<>(List.of(
        "Acme Solutions",
        "BlueRiver Technologies",
        "Nova Industries",
        "GreenLeaf Consultancy",
        "Atlas Logistics",
        "BrightSpace Learning",
        "Orion Software",
        "Helix Medical",
        "Metro Retail Group",
        "Solaris Energy"
    ));

    @GetMapping("/companies")
    public List<String> getCompanies() {
        return companies;
    }


}