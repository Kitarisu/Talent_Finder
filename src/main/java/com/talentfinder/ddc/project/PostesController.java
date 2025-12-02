package com.talentfinder.ddc.project;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PostesController {

    @Autowired
    private PosteRepository posteRepository;

    @GetMapping("/posts")
    public List<Poste> getPosts() {
        return posteRepository.findAll();
    }

    @PostMapping("/posts")
    public ResponseEntity<Poste> addPoste(@RequestBody Poste poste) {
        if (poste.getIntitule() == null || poste.getIntitule().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (poste.getStatut() == null || poste.getStatut().trim().isEmpty()) {
            poste.setStatut("ouvert");
        }
        Poste saved = posteRepository.save(poste);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePoste(@PathVariable Long id) {
        if (!posteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        posteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<Poste> updatePoste(@PathVariable Long id, @RequestBody Poste updates) {
        return posteRepository.findById(id)
                .map(poste -> {
                    if (updates.getStatut() != null && !updates.getStatut().trim().isEmpty()) {
                        poste.setStatut(updates.getStatut());
                    }
                    if (updates.getIntitule() != null && !updates.getIntitule().trim().isEmpty()) {
                        poste.setIntitule(updates.getIntitule());
                    }
                    Poste updated = posteRepository.save(poste);
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}