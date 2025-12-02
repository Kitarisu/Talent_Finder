package com.talentfinder.ddc.project;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

@Service
public class PDFExtractionService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    // Extraire le texte du PDF
    public String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    // Extraire les informations du CV
    public Map<String, Object> extractCVData(MultipartFile cvFile) throws IOException {
        String text = extractTextFromPDF(cvFile);
        Map<String, Object> data = new HashMap<>();
        
        // Téléphone (pattern: 06, 07, +33, etc. avec ou sans espaces)
        // Formats supportés: 0601020304 | 06 01 02 03 04 | +33601020304 | +33 6 01 02 03 04
        Pattern phonePattern = Pattern.compile("(?:0|\\+33\\s?)\\s*[1-9](?:\\s*\\d){8}");
        Matcher phoneMatcher = phonePattern.matcher(text);
        if (phoneMatcher.find()) {
            String phone = phoneMatcher.group().replaceAll("\\s+", "");
            data.put("telephone", phone);
        }

        // Email
        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        Matcher emailMatcher = emailPattern.matcher(text);
        if (emailMatcher.find()) {
            data.put("email", emailMatcher.group());
        }

        // Extraire les compétences (chercher des keywords)
        List<String> competences = extractCompetences(text);
        if (!competences.isEmpty()) {
            data.put("competences", objectMapper.writeValueAsString(competences));
        }

        // Extraire les langues
        List<String> langues = extractLangues(text);
        if (!langues.isEmpty()) {
            data.put("langues", objectMapper.writeValueAsString(langues));
        }

        // Extraire les diplômes (chercher les keywords)
        List<String> diplomes = extractDiplomes(text);
        if (!diplomes.isEmpty()) {
            data.put("diplomes", objectMapper.writeValueAsString(diplomes));
        }

        // Extraire les expériences
        List<String> experiences = extractExperiences(text);
        if (!experiences.isEmpty()) {
            data.put("experiences", objectMapper.writeValueAsString(experiences));
        }

        return data;
    }

    // Extraire les compétences
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

    // Extraire les langues
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

    // Extraire les diplômes
    private List<String> extractDiplomes(String text) {
        List<String> diplomes = new ArrayList<>();
        String[] degreeKeywords = {"Master", "Licence", "Bac", "Diplôme", "Certification", "Bachelor", "MBA"};
        
        String lowerText = text.toLowerCase();
        for (String degree : degreeKeywords) {
            if (lowerText.contains(degree.toLowerCase())) {
                diplomes.add(degree);
            }
        }
        return diplomes;
    }

    // Extraire les expériences (très simplifié)
    private List<String> extractExperiences(String text) {
        List<String> experiences = new ArrayList<>();
        
        // Chercher les patterns de dates (YYYY-YYYY)
        Pattern datePattern = Pattern.compile("(\\d{4})\\s*-\\s*(\\d{4})");
        Matcher matcher = datePattern.matcher(text);
        
        while (matcher.find()) {
            experiences.add("Expérience: " + matcher.group(1) + " - " + matcher.group(2));
        }
        
        return experiences;
    }
}