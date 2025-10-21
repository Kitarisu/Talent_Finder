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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class UploadController {

    private final Path cvDir = Paths.get("src/main/resources/cv_candidate");
    private final Path letterDir = Paths.get("src/main/resources/letter_candidate");

    public UploadController() throws IOException {
        Files.createDirectories(cvDir);
        Files.createDirectories(letterDir);
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam String firstName,
                                    @RequestParam String lastName,
                                    @RequestParam(required = false) String email,
                                    @RequestParam(required = false) MultipartFile cv,
                                    @RequestParam(required = false) MultipartFile letter) {
        try {
            String base = sanitize(lastName) + "_" + sanitize(firstName);

            String savedCv = null;
            String savedLetter = null;

            if (cv != null && !cv.isEmpty()) {
                String ext = getExtension(cv.getOriginalFilename());
                String newName = "CV_" + base + ext;
                Path target = cvDir.resolve(newName);
                Files.copy(cv.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                savedCv = newName;
            }

            if (letter != null && !letter.isEmpty()) {
                String ext = getExtension(letter.getOriginalFilename());
                String newName = "LM_" + base + ext;
                Path target = letterDir.resolve(newName);
                Files.copy(letter.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                savedLetter = newName;
            }

            java.util.Map<String,Object> resp = new java.util.HashMap<>();
            resp.put("message", "Upload réussi");
            resp.put("cvFile", savedCv);
            resp.put("letterFile", savedLetter);
            return ResponseEntity.ok().body(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<?> listFiles() {
        try (Stream<Path> cvs = Files.list(cvDir);
             Stream<Path> letters = Files.list(letterDir)) {

            List<String> cvList = cvs.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());

            List<String> letterList = letters.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());

            // Map base -> filename
            Map<String, String> cvMap = new HashMap<>();
            for (String f : cvList) {
                if (f.toUpperCase().startsWith("CV_")) {
                    int idx = f.lastIndexOf('.');
                    String base = (idx >= 0) ? f.substring(3, idx) : f.substring(3);
                    cvMap.put(base, f);
                }
            }
            Map<String, String> letterMap = new HashMap<>();
            for (String f : letterList) {
                if (f.toUpperCase().startsWith("LM_")) {
                    int idx = f.lastIndexOf('.');
                    String base = (idx >= 0) ? f.substring(3, idx) : f.substring(3);
                    letterMap.put(base, f);
                }
            }

            // union of bases
            Set<String> bases = new TreeSet<>(String::compareTo);
            bases.addAll(cvMap.keySet());
            bases.addAll(letterMap.keySet());

            List<Map<String, Object>> candidates = new ArrayList<>();
            for (String base : bases) {
                Map<String, Object> item = new HashMap<>();
                item.put("base", base);
                item.put("displayName", makeDisplayName(base));
                item.put("cv", cvMap.get(base));       // may be null
                item.put("letter", letterMap.get(base)); // may be null
                candidates.add(item);
            }

            return ResponseEntity.ok(Map.of("candidates", candidates));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Impossible de lister les fichiers"));
        }
    }

    @GetMapping("/download/cv/{filename:.+}")
    public ResponseEntity<Resource> downloadCv(@PathVariable String filename) {
        return downloadFile(cvDir, filename);
    }

    @GetMapping("/download/letter/{filename:.+}")
    public ResponseEntity<Resource> downloadLetter(@PathVariable String filename) {
        return downloadFile(letterDir, filename);
    }

    private ResponseEntity<Resource> downloadFile(Path dir, String filename) {
        try {
            // Prevent path traversal
            String safe = filename.replaceAll("[\\\\/]+", "");
            Path file = dir.resolve(safe).normalize();
            if (!file.startsWith(dir.normalize())) {
                return ResponseEntity.status(403).build();
            }
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(file.toUri());
            String encoded = URLEncoder.encode(file.getFileName().toString(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(Files.size(file))
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private String sanitize(String s) {
        if (s == null) return "UNKNOWN";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.replaceAll("[^\\p{Alnum}]+", "_");
        n = n.replaceAll("^_+|_+$", "");
        n = n.toUpperCase();
        return n.isEmpty() ? "UNKNOWN" : n;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return (idx >= 0) ? filename.substring(idx) : "";
    }

    private String makeDisplayName(String base) {
        if (base == null || base.isEmpty()) return "";
        String[] parts = base.split("_");
        // base is SANITIZED LAST_FIRST[_...], all uppercase
        if (parts.length >= 2) {
            String last = parts[0];
            String first = parts[1];
            return titleCase(first) + " " + titleCase(last);
        } else {
            return titleCase(base.replace('_', ' '));
        }
    }

    private String titleCase(String s) {
        s = s.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        String[] parts = s.split("[ _]+");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            if (i < parts.length - 1) out.append(' ');
        }
        return out.toString();
    }
}