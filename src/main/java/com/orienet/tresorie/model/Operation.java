package com.orienet.tresorie.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "operation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "DateOperation")
    private LocalDate dateFlux;

    @Column(name = "NatureFlux")
    private String natureFlux;

    @Column(name = "Caisse")
    private String caisse;

    @Column(name = "ModeFlux")
    private String modeFlux;

    @Column(name = "TitulaireFlux")
    private String titulaireFlux;

    @Column(name = "montant", precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "CC_P")
    private String ccp;

    @Column(name = "Famille")
    private String famille;

    @Column(name = "Designation")
    private String designation;

    @Column(name = "Description")
    private String description;

    @Column(name = "Etat")
    private String etat;

    // Stocke les valeurs des champs dynamiques en JSON
    // Exemple : {"Référence":"REF-001","Projet":"Bestmobile"}
    @Column(name = "valeurs_dynamiques", columnDefinition = "TEXT")
    private String valeursDynamiques;
}