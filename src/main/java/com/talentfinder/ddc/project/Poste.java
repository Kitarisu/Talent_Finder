package com.talentfinder.ddc.project;

import jakarta.persistence.*;

@Entity
@Table(name = "postes")
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_poste")
    private Long id;

    @Column(name = "intitule", nullable = false)
    private String intitule;

    @Column(name = "statut", nullable = false)
    private String statut;

    public Poste() {}

    public Poste(String intitule, String statut) {
        this.intitule = intitule;
        this.statut = statut;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIntitule() { return intitule; }
    public void setIntitule(String intitule) { this.intitule = intitule; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}