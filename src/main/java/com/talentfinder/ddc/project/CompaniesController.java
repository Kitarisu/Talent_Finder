package com.talentfinder.ddc.project;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CompaniesController {

    // stockage dans une ArrayList modifiable
    private final List<Company> companies = new ArrayList<>(List.of(
        new Company(1, "Acme Solutions"),
        new Company(2, "BlueRiver Technologies"),
        new Company(3, "Nova Industries"),
        new Company(4, "GreenLeaf Consultancy"),
        new Company(5, "Atlas Logistics"),
        new Company(6, "BrightSpace Learning"),
        new Company(7, "Orion Software"),
        new Company(8, "Helix Medical"),
        new Company(9, "Metro Retail Group"),
        new Company(10, "Solaris Energy")
    ));

    @GetMapping("/companies")
    public List<Company> getCompanies() {
        return companies;
    }

   
}