package com.talentfinder.ddc.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CompaniesController {

    @Autowired
    private CompanyRepository companyRepository;

    @GetMapping("/companies")
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        if (!companyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        companyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}