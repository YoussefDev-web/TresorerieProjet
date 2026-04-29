package com.orienet.tresorie.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "utilisateurs")
@Data
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    /** ROLE_ADMIN ou ROLE_UTILISATEUR */
    @Column(nullable = false)
    private String role;

    private boolean actif = true;
}
