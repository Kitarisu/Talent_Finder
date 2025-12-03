package com.talentfinder.ddc.project;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class LLMExtractionService {

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String MODEL = "mistral";
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 120000;
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY = 1000;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isOllamaAvailable() {
        try {
            URL url = URI.create(OLLAMA_BASE_URL).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            System.err.println("Ollama indisponible: " + e.getMessage());
            return false;
        }
    }

    public void enrichExtractedData(Map<String, Object> data, String cvText, String letterText) {
        if (!isOllamaAvailable()) {
            System.out.println("LLM indisponible, enrichissement ignoré");
            return;
        }

        try {
            String combinedText = cvText;
            if (letterText != null && !letterText.isEmpty()) {
                combinedText += "\n\n--- LETTRE DE MOTIVATION ---\n" + letterText;
            }

            String startTime = LocalDateTime.now().format(TIME_FORMATTER);
            System.out.println("Enrichissement LLM en cours... [" + startTime + "]");

            // Téléphone (complément si absent)
            if (data.get("telephone") == null || ((String) data.get("telephone")).isEmpty()) {
                System.out.println("  Extraction téléphone...");
                String phone = extractPhoneFromLLM(combinedText);
                if (phone != null && !phone.isEmpty()) {
                    data.put("telephone", phone);
                    System.out.println("  Téléphone trouvé: " + phone);
                }
            }

            // Expériences
            System.out.println("  Extraction expériences...");
            String experiences = extractExperiencesFromLLM(combinedText);
            if (experiences != null && !experiences.isEmpty()) {
                data.put("experiences", experiences);
                int count = countLines(experiences);
                System.out.println("  " + count + " expérience(s) trouvée(s)");
            }

            // Langues
            System.out.println("  Extraction langues...");
            String langues = extractLanguesFromLLM(combinedText);
            if (langues != null && !langues.isEmpty()) {
                // Fusionner avec les langues détectées par regex
                String existingLangues = (String) data.get("langues");
                if (existingLangues != null && !existingLangues.isEmpty()) {
                    langues = mergeLangues(existingLangues, langues);
                }
                data.put("langues", langues);
                System.out.println("  Langues extraites: " + langues);
            }

            // Soft Skills
            System.out.println("  Extraction soft skills...");
            String softSkills = extractSoftSkillsFromLLM(combinedText);
            if (softSkills != null && !softSkills.isEmpty()) {
                data.put("softSkills", softSkills);
                int count = softSkills.split(",").length;
                System.out.println("  " + count + " soft skill(s) trouvé(s)");
            }

            // Permis de conduire
            System.out.println("  Extraction permis de conduire...");
            String permis = extractPermisFromLLM(combinedText);
            if (permis != null && !permis.isEmpty()) {
                data.put("permisDeConduite", permis);
                System.out.println("  Permis trouvés: " + permis);
            }

            String endTime = LocalDateTime.now().format(TIME_FORMATTER);
            System.out.println("Enrichissement LLM terminé [" + endTime + "]");
        } catch (Exception e) {
            System.err.println("Erreur enrichissement LLM: " + e.getMessage());
        }
    }

    private String extractPhoneFromLLM(String text) {
        try {
            String prompt = "Extrais UNIQUEMENT le numéro de téléphone du texte suivant. " +
                          "Réponds UNIQUEMENT avec le numéro en format français (ex: 0601020304 ou +33601020304). " +
                          "Si absent, réponds: VIDE\n\n" + 
                          truncateText(text, 1000);

            String response = callOllamaWithRetry(prompt).trim();
            
            if (!response.equalsIgnoreCase("VIDE") && isValidPhone(response)) {
                return response.replaceAll("\\s+", "");
            }
        } catch (Exception e) {
            System.err.println("Erreur extraction téléphone: " + e.getMessage());
        }
        return null;
    }

    private String extractExperiencesFromLLM(String text) {
        try {
            String prompt = "Extrais UNIQUEMENT les expériences professionnelles du texte suivant.\n" +
                          "Une expérience professionnelle est un emploi, un poste ou une mission occupée dans une entreprise ou une organisation.\n" +
                          "IMPORTANT: N'inclus RIEN d'autre que le poste, l'entreprise et les dates. N'inclus PAS les compétences, les diplômes, les formations, ni les certifications.\n" +
                          "N'extrais PAS les expériences personnelles ou les projets académiques, UNIQUEMENT les emplois officiels.\n" +
                          "Formate les en texte lisible, une expérience par ligne.\n" +
                          "Exemple de format attendu:\n" +
                          "Développeur Java chez Google (2020-2023)\n" +
                          "Ingénieur FullStack chez Microsoft (2023-Présent)\n" +
                          "Consultant IT chez Accenture (01/2019-06/2019)\n\n" +
                          "Si aucune expérience professionnelle trouvée, réponds: VIDE\n\n" +
                          truncateText(text, 2000);

            String response = callOllamaWithRetry(prompt).trim();
            return response.equalsIgnoreCase("VIDE") ? null : response;
        } catch (Exception e) {
            System.err.println("Erreur extraction expériences: " + e.getMessage());
        }
        return null;
    }

    private String extractLanguesFromLLM(String text) {
        try {
            String prompt = "Identifie UNIQUEMENT les langues parlées ou écrites mentionnées EXPLICITEMENT dans le texte suivant.\n" +
                          "Les langues valides sont: Français, Anglais, Espagnol, Allemand, Italien, Chinois, Japonais, Russe, Arabe, Portugais.\n" +
                          "Une langue n'est extraite QUE SI:\n" +
                          "1. Elle est explicitement mentionnée (ex: 'Anglais courant', 'Parle russe', 'TOEFL 110')\n" +
                          "2. OU c'est la langue principale du document (ex: le CV est rédigé en français ou en anglais)\n" +
                          "N'INFÈRE PAS les langues à partir de compétences techniques, de noms de technologies ou d'hobbies.\n" +
                          "Par exemple: TypeScript ne signifie PAS connaître le russe, lire des mangas ne signifie PAS connaître le japonais.\n" +
                          "Réponds avec une liste séparée par des virgules.\n" +
                          "Exemple de format attendu:\n" +
                          "Français, Anglais\n\n" +
                          "Si aucune langue trouvée de façon évidente, réponds: VIDE\n\n" +
                          truncateText(text, 1500);

            String response = callOllamaWithRetry(prompt).trim();
            return response.equalsIgnoreCase("VIDE") ? null : response;
        } catch (Exception e) {
            System.err.println("Erreur extraction langues: " + e.getMessage());
        }
        return null;
    }

    private String extractSoftSkillsFromLLM(String text) {
        try {
            String prompt = "Extrais UNIQUEMENT les soft skills (compétences transversales) du texte suivant. " +
                          "Les soft skills sont: communication, leadership, travail d'équipe, gestion de projet, résolution de problèmes, " +
                          "créativité, adaptabilité, organisation, etc.\n" +
                          "N'extrais PAS les compétences techniques (Java, Python, Docker, etc.).\n" +
                          "Réponds avec une liste séparée par des virgules.\n" +
                          "Exemple de format attendu:\n" +
                          "Communication, Leadership, Gestion de projet, Travail d'équipe\n\n" +
                          "Si aucun soft skill trouvé, réponds: VIDE\n\n" +
                          truncateText(text, 2000);

            String response = callOllamaWithRetry(prompt).trim();
            return response.equalsIgnoreCase("VIDE") ? null : response;
        } catch (Exception e) {
            System.err.println("Erreur extraction soft skills: " + e.getMessage());
        }
        return null;
    }

    private String extractPermisFromLLM(String text) {
        try {
            String prompt = "Extrais UNIQUEMENT les catégories de permis de conduire mentionnées dans le texte suivant. " +
                          "Les catégories valides sont: A, AM, B, C, D, F. " +
                          "Réponds avec une liste séparée par des virgules.\n" +
                          "Exemple de format attendu:\n" +
                          "A, B, C\n\n" +
                          "Si aucun permis trouvé, réponds: VIDE\n\n" +
                          truncateText(text, 1000);

            String response = callOllamaWithRetry(prompt).trim();
            
            if (response.equalsIgnoreCase("VIDE") || response.isEmpty()) {
                return null;
            }

            String[] categories = response.split(",");
            List<String> validCategories = new ArrayList<>();
            
            for (String cat : categories) {
                cat = cat.trim().toUpperCase();
                if (cat.matches("[ABCDF]|AM")) {
                    validCategories.add(cat);
                }
            }
            
            return validCategories.isEmpty() ? null : String.join(", ", validCategories);
        } catch (Exception e) {
            System.err.println("Erreur extraction permis: " + e.getMessage());
        }
        return null;
    }

    private String mergeLangues(String existing, String llmResult) {
        Set<String> languages = new LinkedHashSet<>();
        
        if (existing != null && !existing.isEmpty()) {
            for (String lang : existing.split(",")) {
                languages.add(lang.trim());
            }
        }
        
        if (llmResult != null && !llmResult.isEmpty()) {
            for (String lang : llmResult.split(",")) {
                languages.add(lang.trim());
            }
        }
        
        return String.join(", ", languages);
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) text.lines().filter(l -> !l.trim().isEmpty()).count();
    }

    private String callOllamaWithRetry(String prompt) throws IOException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return callOllama(prompt);
            } catch (IOException e) {
                if (attempt < MAX_RETRIES) {
                    System.out.println("  Tentative " + attempt + " échouée, nouvelle tentative...");
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interruption", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new IOException("Ollama indisponible après " + MAX_RETRIES + " tentatives");
    }

    private String callOllama(String prompt) throws IOException {
        StringBuilder result = new StringBuilder();
        HttpURLConnection connection = null;
        
        try {
            URL url = URI.create(OLLAMA_API_URL).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String payload = "{" +
                "\"model\": \"" + MODEL + "\"," +
                "\"prompt\": " + objectMapper.writeValueAsString(prompt) + "," +
                "\"stream\": false" +
                "}";

            try (var os = connection.getOutputStream()) {
                os.write(payload.getBytes("utf-8"));
                os.flush();
            }

            if (connection.getResponseCode() == 200) {
                try (var is = connection.getInputStream();
                     var reader = new java.io.InputStreamReader(is, "utf-8");
                     var bufferedReader = new java.io.BufferedReader(reader)) {
                    
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        try {
                            Map<String, Object> responseObj = objectMapper.readValue(line, Map.class);
                            if (responseObj.containsKey("response")) {
                                result.append(responseObj.get("response"));
                            }
                        } catch (Exception e) {
                            // Ignorer les lignes invalides
                        }
                    }
                }
            } else {
                throw new IOException("Erreur HTTP " + connection.getResponseCode());
            }

        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    // Ignorer erreur déconnexion
                }
            }
        }

        return result.toString().trim();
    }

    private boolean isValidPhone(String phone) {
        String cleaned = phone.replaceAll("\\s+", "");
        Pattern pattern = Pattern.compile("^(?:0|\\+33)[1-9]\\d{8,9}$");
        return pattern.matcher(cleaned).matches();
    }

    private String truncateText(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}