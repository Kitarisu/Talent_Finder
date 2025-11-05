package com.talentfinder.ddc.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CompaniesController {

    @Autowired
    private CompanyRepository companyRepository;

    @GetMapping("/companies")
    public List<Company> getCompanies() {
        return companyRepository.findAll();
    }
}