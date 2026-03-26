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

    // JSON des valeurs des champs dynamiques
    @Column(name = "valeurs_dynamiques", columnDefinition = "TEXT")
    private String valeursDynamiques;

    // ── Archivage ──────────────────────────────────────────────────
    // true  = opération archivée (n'apparaît plus dans le tableau principal)
    // false = opération active (par défaut)
    @Column(name = "archivee", nullable = false)
    @Builder.Default
    private boolean archivee = false;

    // Date à laquelle l'opération a été archivée
    @Column(name = "date_archivage")
    private LocalDate dateArchivage;
}