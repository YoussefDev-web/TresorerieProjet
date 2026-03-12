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
    private String natureFlux; // Exemple: Vente, Achat, Salaire

    @Column(name = "Caisse")
    private String caisse; // Description de l'opération

    @Column(name = "ModeFlux")
    private String modeFlux; // Exemple: Espèces, Chèque, Virement

    @Column(name = "TitulaireFlux")
    private String titulaireFlux; // Exemple: Caisse Centrale, BP, Attijari

    // Utilisation de BigDecimal pour éviter les erreurs d'arrondi sur l'argent
    @Column(name = "montant", precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "CC/P")
    private String ccp;

    @Column(name = "Famille")
    private String famille;

    @Column(name = "Designation")
    private String designation;

    @Column(name = "Description")
    private String description;

    @Column(name = "Etat")
    private String etat;
}