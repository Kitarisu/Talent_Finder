package com.talentfinder.ddc.project;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class UploadController {

    // ne pas écrire dans src/main/resources pour éviter de déclencher un redémarrage devtools
    private final Path cvDir = Paths.get("data", "cv_candidate");
    private final Path letterDir = Paths.get("data", "letter_candidate");

    // stockage en mémoire des candidats (initialisé au démarrage)
    private final List<Candidate> candidates = new CopyOnWriteArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public UploadController() throws IOException {
        Files.createDirectories(cvDir);      // crée data/cv_candidate
        Files.createDirectories(letterDir);  // crée data/letter_candidate
        // candidates list starts empty; add initial entries here if needed
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam String firstName,
                                    @RequestParam String lastName,
                                    @RequestParam String email,
                                    @RequestParam(required = false) MultipartFile cv,
                                    @RequestParam(required = false) MultipartFile letter) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email requis"));
            }

            Candidate cand = new Candidate(firstName, lastName, email);
            cand.setId(idCounter.getAndIncrement());

            if (cv != null && !cv.isEmpty()) {
                String ext = getExtension(cv.getOriginalFilename());
                String newName = "CV_" + cand.getBase() + ext;
                Files.copy(cv.getInputStream(), cvDir.resolve(newName), StandardCopyOption.REPLACE_EXISTING);
                cand.setCvFile(newName);
            }

            if (letter != null && !letter.isEmpty()) {
                String ext = getExtension(letter.getOriginalFilename());
                String newName = "LM_" + cand.getBase() + ext;
                Files.copy(letter.getInputStream(), letterDir.resolve(newName), StandardCopyOption.REPLACE_EXISTING);
                cand.setLetterFile(newName);
            }

            candidates.add(cand);

            System.out.println("Candidate added: id=" + cand.getId() + " base=" + cand.getBase()
                    + " cv=" + cand.getCvFile() + " letter=" + cand.getLetterFile());

            return ResponseEntity.ok(Map.of(
                    "message", "Upload réussi",
                    "candidate", Map.of(
                            "id", cand.getId(),
                            "firstName", cand.getFirstName(),
                            "lastName", cand.getLastName(),
                            "email", cand.getEmail(),
                            "base", cand.getBase(),
                            "cv", cand.getCvFile(),
                            "letter", cand.getLetterFile()
                    )
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<?> listFiles() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Candidate c : candidates) {
            Map<String,Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("base", c.getBase());
            m.put("displayName", c.getDisplayName());
            m.put("cv", c.getCvFile() == null ? "" : c.getCvFile());
            m.put("letter", c.getLetterFile() == null ? "" : c.getLetterFile());
            list.add(m);
        }
        return ResponseEntity.ok(Map.of("candidates", list));
    }

    @GetMapping("/download/cv/{filename:.+}")
    public ResponseEntity<Resource> downloadCv(@PathVariable String filename) { return downloadFile(cvDir, filename); }

    @GetMapping("/download/letter/{filename:.+}")
    public ResponseEntity<Resource> downloadLetter(@PathVariable String filename) { return downloadFile(letterDir, filename); }

    private ResponseEntity<Resource> downloadFile(Path dir, String filename) {
        try {
            String safe = filename.replaceAll("[\\\\/]+", "");
            Path file = dir.resolve(safe).normalize();
            if (!file.startsWith(dir.normalize()) || !Files.exists(file) || !Files.isRegularFile(file)) {
                return ResponseEntity.notFound().build();
            }
            Resource r = new UrlResource(file.toUri());
            String encoded = URLEncoder.encode(file.getFileName().toString(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(Files.size(file))
                    .body(r);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return (idx >= 0) ? filename.substring(idx) : "";
    }
}