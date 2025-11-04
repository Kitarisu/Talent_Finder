package com.talentfinder.ddc.project;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // conservé pour compatibilité (prénom + nom)
    private String firstName;
    private String lastName;
    private String poste;
    private String email;

    private String cvFileName;
    private String cvContentType;
    @Lob
    private byte[] cvData;

    private String letterFileName;
    private String letterContentType;
    @Lob
    private byte[] letterData;

    public Candidate() {}

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPoste() { return poste; }
    public void setPoste(String poste) { this.poste = poste; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCvFileName() { return cvFileName; }
    public void setCvFileName(String cvFileName) { this.cvFileName = cvFileName; }

    public String getCvContentType() { return cvContentType; }
    public void setCvContentType(String cvContentType) { this.cvContentType = cvContentType; }

    public byte[] getCvData() { return cvData; }
    public void setCvData(byte[] cvData) { this.cvData = cvData; }

    public String getLetterFileName() { return letterFileName; }
    public void setLetterFileName(String letterFileName) { this.letterFileName = letterFileName; }

    public String getLetterContentType() { return letterContentType; }
    public void setLetterContentType(String letterContentType) { this.letterContentType = letterContentType; }

    public byte[] getLetterData() { return letterData; }
    public void setLetterData(byte[] letterData) { this.letterData = letterData; }
}