package com.talentfinder.ddc.project;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api")
public class PostesController {

    @Autowired
    private PosteRepository posteRepository;

    @GetMapping("/posts")
    public List<Poste> getPosts() {
        return posteRepository.findAll();
    }
}