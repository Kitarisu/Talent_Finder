package com.talentfinder.ddc.project;

import java.text.Normalizer;
import java.util.Locale;

public class Candidate {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String base;       // SANITIZED LAST_FIRST
    private String cvFile;     // stored filename or null
    private String letterFile; // stored filename or null

    public Candidate() {}

    public Candidate(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.base = makeBase(lastName, firstName);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.base = makeBase(this.lastName, this.firstName);
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.base = makeBase(this.lastName, this.firstName);
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }

    public String getCvFile() { return cvFile; }
    public void setCvFile(String cvFile) { this.cvFile = cvFile; }

    public String getLetterFile() { return letterFile; }
    public void setLetterFile(String letterFile) { this.letterFile = letterFile; }

    public String getDisplayName() {
        String f = firstName == null ? "" : capitalize(firstName);
        String l = lastName == null ? "" : capitalize(lastName);
        return (f + " " + l).trim();
    }

    public static String makeBase(String lastName, String firstName) {
        return sanitize(lastName) + "_" + sanitize(firstName);
    }

    public static String sanitize(String s) {
        if (s == null) return "UNKNOWN";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.replaceAll("[^A-Za-z0-9]+", "_")
             .replaceAll("^_+|_+$", "")
             .toUpperCase(Locale.ROOT);
        return n.isEmpty() ? "UNKNOWN" : n;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s == null ? "" : s;
        s = s.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", base='" + base + '\'' +
                ", cvFile='" + cvFile + '\'' +
                ", letterFile='" + letterFile + '\'' +
                '}';
    }
}