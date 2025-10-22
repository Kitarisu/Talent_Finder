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
import java.text.Normalizer;
import java.util.*;

@RestController
public class UploadController {

    private final Path cvDir = Paths.get("src/main/resources/cv_candidate");
    private final Path letterDir = Paths.get("src/main/resources/letter_candidate");

    public UploadController() throws IOException {
        Files.createDirectories(cvDir);
        Files.createDirectories(letterDir);
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

            String base = sanitize(lastName) + "_" + sanitize(firstName);

            if (cv != null && !cv.isEmpty()) {
                String ext = getExtension(cv.getOriginalFilename());
                Files.copy(cv.getInputStream(), cvDir.resolve("CV_" + base + ext), StandardCopyOption.REPLACE_EXISTING);
            }

            if (letter != null && !letter.isEmpty()) {
                String ext = getExtension(letter.getOriginalFilename());
                Files.copy(letter.getInputStream(), letterDir.resolve("LM_" + base + ext), StandardCopyOption.REPLACE_EXISTING);
            }

            return ResponseEntity.ok(Map.of("message", "Upload réussi"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<?> listFiles() {
        try {
            Map<String, String> cvMap = new HashMap<>();
            Map<String, String> letterMap = new HashMap<>();

            for (Path p : Files.list(cvDir).filter(Files::isRegularFile).toArray(Path[]::new)) {
                String f = p.getFileName().toString();
                if (f.toUpperCase().startsWith("CV_")) {
                    int idx = f.lastIndexOf('.');
                    String base = (idx >= 0) ? f.substring(3, idx) : f.substring(3);
                    cvMap.put(base, f);
                }
            }

            for (Path p : Files.list(letterDir).filter(Files::isRegularFile).toArray(Path[]::new)) {
                String f = p.getFileName().toString();
                if (f.toUpperCase().startsWith("LM_")) {
                    int idx = f.lastIndexOf('.');
                    String base = (idx >= 0) ? f.substring(3, idx) : f.substring(3);
                    letterMap.put(base, f);
                }
            }

            Set<String> bases = new TreeSet<>();
            bases.addAll(cvMap.keySet());
            bases.addAll(letterMap.keySet());

            List<Map<String, String>> candidates = new ArrayList<>();
            for (String base : bases) {
                Map<String, String> item = new HashMap<>();
                item.put("base", base);
                item.put("displayName", makeDisplayName(base));
                item.put("cv", cvMap.getOrDefault(base, ""));
                item.put("letter", letterMap.getOrDefault(base, ""));
                candidates.add(item);
            }

            return ResponseEntity.ok(Map.of("candidates", candidates));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Impossible de lister les fichiers"));
        }
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

    private String sanitize(String s) {
        if (s == null) return "UNKNOWN";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "").toUpperCase();
        return n.isEmpty() ? "UNKNOWN" : n;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return (idx >= 0) ? filename.substring(idx) : "";
    }

    private String makeDisplayName(String base) {
        if (base == null) return "";
        String[] parts = base.split("_");
        if (parts.length >= 2) return capitalize(parts[1]) + " " + capitalize(parts[0]);
        return capitalize(base.replace('_', ' '));
    }

    private String capitalize(String s) {
        s = s.toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}