package com.talentfinder.ddc.project;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

@Service
public class PDFExtractionService {
    
    @Autowired(required = false)
    private LLMExtractionService llmExtractionService;

    public String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public Map<String, Object> extractCVData(MultipartFile cvFile) throws IOException {
        return extractCVData(cvFile, null);
    }

    public Map<String, Object> extractCVData(MultipartFile cvFile, MultipartFile letterFile) throws IOException {
        String cvText = extractTextFromPDF(cvFile);
        String letterText = null;
        
        if (letterFile != null && !letterFile.isEmpty()) {
            letterText = extractTextFromPDF(letterFile);
        }

        Map<String, Object> data = new HashMap<>();
        
        // Extraction basique via regex
        Pattern phonePattern = Pattern.compile("(?:0|\\+33\\s?)\\s*[1-9](?:\\s*\\d){8}");
        Matcher phoneMatcher = phonePattern.matcher(cvText);
        if (phoneMatcher.find()) {
            String phone = phoneMatcher.group().replaceAll("\\s+", "");
            data.put("telephone", phone);
        }

        // Compétences techniques
        List<String> competences = extractCompetences(cvText);
        if (!competences.isEmpty()) {
            data.put("competences", String.join(", ", competences));
        }

        // Diplômes
        List<String> diplomes = extractDiplomes(cvText);
        if (!diplomes.isEmpty()) {
            data.put("diplomes", String.join(", ", diplomes));
        }

        // Langues (détection basique par regex)
        List<String> langues = extractLangues(cvText);
        if (!langues.isEmpty()) {
            data.put("langues", String.join(", ", langues));
        }

        // Enrichissement via LLM pour informations complexes
        if (llmExtractionService != null) {
            llmExtractionService.enrichExtractedData(data, cvText, letterText);
        }

        return data;
    }

    private List<String> extractCompetences(String text) {
        List<String> competences = new ArrayList<>();
        String[] keywords = {"Java", "Python", "JavaScript", "C++", "C#", "PHP", "Ruby", "Go", "Rust",
                            "SQL", "MongoDB", "Spring Boot", "React", "Angular", "Vue", "Docker", "Kubernetes",
                            "AWS", "Azure", "Git", "Linux", "Windows", "REST API", "GraphQL"};
        
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                competences.add(keyword);
            }
        }
        return competences;
    }

    private List<String> extractDiplomes(String text) {
        List<String> diplomes = new ArrayList<>();
        String[] keywords = {"Licence", "Master", "Baccalauréat", "BTS", "BUT", "DUT", "DEUG", 
                            "Ingénieur", "MBA", "DEUST"};
        
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                diplomes.add(keyword);
            }
        }
        return diplomes;
    }

    private List<String> extractLangues(String text) {
        List<String> langues = new ArrayList<>();
        String[] languages = {"Français", "Anglais", "Espagnol", "Allemand", "Italien", "Chinois", "Japonais", "Russe", "Arabe", "Portugais"};
        
        String lowerText = text.toLowerCase();
        for (String lang : languages) {
            if (lowerText.contains(lang.toLowerCase())) {
                langues.add(lang);
            }
        }
        return langues;
    }
}
