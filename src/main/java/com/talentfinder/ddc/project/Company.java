package com.talentfinder.ddc.project;

import jakarta.persistence.*;

@Entity
@Table(name = "entreprises")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entreprise")
    private Long id;

    @Column(name = "nom", nullable = false)
    private String name;

    public Company() {}

    public Company(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Company(int id, String name) {
        this.id = (long) id;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}