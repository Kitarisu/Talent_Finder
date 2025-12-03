package com.talentfinder.ddc.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class UploadController {

    @Autowired
    private CandidateRepository candidateRepository;
    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private PDFExtractionService pdfExtractionService;

    @PostMapping("/apply")
    public String handleFileUpload(@RequestParam("firstName") String firstName,
                                   @RequestParam("lastName") String lastName,
                                   @RequestParam("poste") String poste,
                                   @RequestParam("email") String email,
                                   @RequestParam(value = "consentStorageData", defaultValue = "false") boolean consentStorageData,
                                   @RequestParam(value = "consentShareData", defaultValue = "false") boolean consentShareData,
                                   @RequestParam("cvFile") MultipartFile file,
                                   @RequestParam(value = "letterFile", required = false) MultipartFile letter,
                                   Model model) {
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            byte[] cvBytes = file.getBytes();
            String cvName = file.getOriginalFilename();
            String cvType = file.getContentType();

            Candidate candidate = new Candidate();
            candidate.setFirstName(firstName);
            candidate.setLastName(lastName);
            candidate.setName((firstName + " " + lastName).trim());
            candidate.setPoste(poste);
            candidate.setEmail(email);
            candidate.setConsentStorageData(consentStorageData);
            candidate.setConsentShareData(consentShareData);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            candidate.setDateCandidature(LocalDateTime.now().format(formatter));

            candidate.setCvFileName(cvName);
            candidate.setCvContentType(cvType);
            candidate.setCvData(cvBytes);

            // Extraire les données du CV
            try {
                Map<String, Object> cvData = pdfExtractionService.extractCVData(file);
                if (cvData.containsKey("telephone")) {
                    candidate.setTelephone((String) cvData.get("telephone"));
                }
                if (cvData.containsKey("competences")) {
                    candidate.setCompetences((String) cvData.get("competences"));
                }
                if (cvData.containsKey("langues")) {
                    candidate.setLangues((String) cvData.get("langues"));
                }
                if (cvData.containsKey("diplomes")) {
                    candidate.setDiplomes((String) cvData.get("diplomes"));
                }
                if (cvData.containsKey("experiences")) {
                    candidate.setExperiences((String) cvData.get("experiences"));
                }
            } catch (IOException e) {
                System.err.println("Erreur lors de l'extraction du CV: " + e.getMessage());
            }

            if (letter != null && !letter.isEmpty()) {
                candidate.setLetterFileName(letter.getOriginalFilename());
                candidate.setLetterContentType(letter.getContentType());
                candidate.setLetterData(letter.getBytes());
            }

            // sauvegarde locale optionnelle
            Path path = Paths.get(UPLOAD_DIR + cvName);
            Files.write(path, cvBytes);

            candidateRepository.save(candidate);
            model.addAttribute("message", "Candidature enregistrée avec succès !");
        } catch (IOException e) {
            model.addAttribute("message", "Erreur lors du téléchargement du CV : " + e.getMessage());
        }
        return "candidater";
    }

    @GetMapping("/files")
    @ResponseBody
    public Map<String, Object> listFiles() {
        List<Candidate> candidates = candidateRepository.findAll();
        List<Map<String, Object>> out = new ArrayList<>();

        for (Candidate c : candidates) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("firstName", c.getFirstName());
            item.put("lastName", c.getLastName());
            item.put("displayName", (c.getFirstName() == null ? "" : c.getFirstName()) + " " + (c.getLastName() == null ? "" : c.getLastName()));
            item.put("base", c.getEmail());
            item.put("poste", c.getPoste());
            item.put("cv", c.getCvFileName());
            item.put("letter", c.getLetterFileName());
            out.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("candidates", out);
        return result;
    }

    // (les endpoints de téléchargement restent inchangés)
    @GetMapping("/files/{id}/cv")
    public ResponseEntity<ByteArrayResource> downloadCv(@PathVariable Long id) {
        return (ResponseEntity<ByteArrayResource>) candidateRepository.findById(id)
                .map(c -> {
                    byte[] data = c.getCvData();
                    if (data == null) return ResponseEntity.noContent().build();
                    ByteArrayResource resource = new ByteArrayResource(data);
                    String type = c.getCvContentType() != null ? c.getCvContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + c.getCvFileName() + "\"")
                            .contentType(MediaType.parseMediaType(type))
                            .contentLength(data.length)
                            .body(resource);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/files/{id}/letter")
    public ResponseEntity<ByteArrayResource> downloadLetter(@PathVariable Long id) {
        return (ResponseEntity<ByteArrayResource>) candidateRepository.findById(id)
                .map(c -> {
                    byte[] data = c.getLetterData();
                    if (data == null) return ResponseEntity.noContent().build();
                    ByteArrayResource resource = new ByteArrayResource(data);
                    String type = c.getLetterContentType() != null ? c.getLetterContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + c.getLetterFileName() + "\"")
                            .contentType(MediaType.parseMediaType(type))
                            .contentLength(data.length)
                            .body(resource);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/download/cv/{filename}")
    public ResponseEntity<ByteArrayResource> downloadCvByFilename(@PathVariable String filename) {
        String decoded = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        for (Candidate c : candidateRepository.findAll()) {
            if (decoded.equals(c.getCvFileName())) {
                byte[] data = c.getCvData();
                if (data == null) return ResponseEntity.noContent().build();
                ByteArrayResource resource = new ByteArrayResource(data);
                String type = c.getCvContentType() != null ? c.getCvContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + c.getCvFileName() + "\"")
                        .contentType(MediaType.parseMediaType(type))
                        .contentLength(data.length)
                        .body(resource);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/download/letter/{filename}")
    public ResponseEntity<ByteArrayResource> downloadLetterByFilename(@PathVariable String filename) {
        String decoded = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        return (ResponseEntity<ByteArrayResource>) candidateRepository.findAll().stream()
                .filter(c -> decoded.equals(c.getLetterFileName()))
                .findFirst()
                .map(c -> {
                    byte[] data = c.getLetterData();
                    if (data == null) return ResponseEntity.noContent().build();
                    ByteArrayResource resource = new ByteArrayResource(data);
                    String type = c.getLetterContentType() != null ? c.getLetterContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + c.getLetterFileName() + "\"")
                            .contentType(MediaType.parseMediaType(type))
                            .contentLength(data.length)
                            .body(resource);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

   @GetMapping("/answer/{id}")
    public String showAnswerForm(@PathVariable Long id, Model model) {
        return candidateRepository.findById(id)
                .map(candidate -> {
                    model.addAttribute("candidateId", id);
                    model.addAttribute("candidateName", 
                        candidate.getFirstName() + " " + candidate.getLastName());
                    return "reponse-candidature";
                })
                .orElse("redirect:/candidatures");
    }

    @PostMapping("/api/reponse/{id}")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> submitAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, String> reponse) {
        
        return candidateRepository.findById(id)
                .map(candidate -> {
                    Map<String,Object> data = new HashMap<>();
                    data.put("idCandidature", String.valueOf(candidate.getId()));
                    
                    Map<String,Object> reponseMap = new HashMap<>();
                    reponseMap.put("statut", reponse.get("statut"));
                    data.put("reponse", reponseMap);
                    
                    data.put("commentaire", reponse.get("commentaire"));
                    data.put("intitulePoste", candidate.getPoste());
                    data.put("dateDecision", java.time.LocalDate.now().toString());
                    
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, 
                                MediaType.APPLICATION_JSON_VALUE)
                            .body(data);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }




    @GetMapping("/transfer/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> transferCandidate(@PathVariable Long id) {
        return candidateRepository.findById(id)
                .map(c -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("idCandidature", String.valueOf(c.getId()));
                    data.put("nom", c.getLastName());
                    data.put("prenom", c.getFirstName());
                    data.put("mail", c.getEmail());
                    data.put("telephone", ""); // à compléter si champ ajouté dans Candidate
                    data.put("posteVise", c.getPoste());
                    data.put("dateDisponibilite", "");
                    data.put("dateCandidature", "");

                    // Exemples de structures vides (à enrichir si tu ajoutes ces infos plus tard)
                    data.put("diplomes", List.of());
                    data.put("experiences", List.of());
                    data.put("competences", List.of());
                    data.put("permisDeConduite", List.of());
                    data.put("langues", List.of());

                    Map<String, Object> fichiers = new HashMap<>();
                    fichiers.put("cv_filename", c.getCvFileName());
                    fichiers.put("lm_filename", c.getLetterFileName());
                    data.put("fichiers", fichiers);

                    return ResponseEntity.ok(data);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/candidate/{id}")
    @ResponseBody
    public ResponseEntity<Candidate> getCandidate(@PathVariable Long id) {
        return candidateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/candidate/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addCandidateFromJson(@RequestBody Map<String, Object> jsonData) {
        try {
            Candidate candidate = new Candidate();
            
            // Extraction des champs du JSON
            candidate.setFirstName((String) jsonData.get("prenom"));
            candidate.setLastName((String) jsonData.get("nom"));
            candidate.setEmail((String) jsonData.get("mail"));
            candidate.setPoste((String) jsonData.get("posteVise"));
            candidate.setTelephone((String) jsonData.get("telephone"));
            candidate.setDateCandidature((String) jsonData.get("dateCandidature"));
            
            // Nom complet
            String firstName = (String) jsonData.get("prenom");
            String lastName = (String) jsonData.get("nom");
            candidate.setName((firstName + " " + lastName).trim());
            
            // Conversion des listes en JSON strings
            ObjectMapper mapper = new ObjectMapper();
            if (jsonData.get("competences") != null) {
                candidate.setCompetences(mapper.writeValueAsString(jsonData.get("competences")));
            }
            if (jsonData.get("langues") != null) {
                candidate.setLangues(mapper.writeValueAsString(jsonData.get("langues")));
            }
            if (jsonData.get("diplomes") != null) {
                candidate.setDiplomes(mapper.writeValueAsString(jsonData.get("diplomes")));
            }
            if (jsonData.get("experiences") != null) {
                candidate.setExperiences(mapper.writeValueAsString(jsonData.get("experiences")));
            }
            if (jsonData.get("permisDeConduite") != null) {
                candidate.setPermisDeConduite(mapper.writeValueAsString(jsonData.get("permisDeConduite")));
            }
            
            // Fichiers (noms seulement, pas de données binaires)
            Map<String, String> fichiers = (Map<String, String>) jsonData.get("fichiers");
            if (fichiers != null) {
                candidate.setCvFileName(fichiers.get("cv_filename"));
                candidate.setLetterFileName(fichiers.get("lm_filename"));
            }
            
            // Consentements (par défaut false)
            candidate.setConsentStorageData(false);
            candidate.setConsentShareData(false);
            
            // Sauvegarder
            Candidate saved = candidateRepository.save(candidate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("message", "Candidature ajoutée avec succès");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Erreur lors de l'ajout: " + e.getMessage()));
        }
    }

    @PostMapping("/api/transfer/send/{companyId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendTransferToCompany(
            @PathVariable Long companyId,
            @RequestBody Map<String, Object> candidateData) {
        try {
            // Vérifier que l'entreprise existe
            // (dans un vrai système, on ferait une vérification en BDD)
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Données transférées avec succès à l'entreprise " + companyId);
            response.put("candidateId", candidateData.get("idCandidature"));
            response.put("companyId", companyId);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            // Dans un vrai projet, on enverrait les données ici (API externe, email, etc.)
            System.out.println("Transfer envoyé à l'entreprise " + companyId + 
                             " pour le candidat: " + candidateData.get("idCandidature"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Erreur: " + e.getMessage()));
        }
    }
}