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
        new Company(1, "Kitarisu Solutions : 2025 COTY (Corporation of the Year) Premium edition with the limited edition figure of our CEO included!"),
        new Company(2, "skibiTeam"),
        new Company(3, "Bombardino CrocoCorp."),
        new Company(4, "Beans Corp."),
        new Company(5, "KMS Industry"),
        new Company(6, "Sybau Aeronautics"),
        new Company(7, "Gooners Expertise Inc."),
        new Company(8, "The Bald Company"),
        new Company(9, "Patapim Associations"),
        new Company(10, "Cumpilator-3000"),
        new Company(11, "The Brotherhood of Nerds"),
        new Company(12, "Seg Fault Enterprises")

    ));

    @GetMapping("/companies")
    public List<Company> getCompanies() {
        return companies;
    }

   
}