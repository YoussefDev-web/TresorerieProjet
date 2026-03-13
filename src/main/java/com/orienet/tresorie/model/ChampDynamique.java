package com.orienet.tresorie.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "champ_dynamique")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChampDynamique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom du champ tel qu'affiché dans le tableau (ex: "Référence", "Projet")
    @Column(name = "nom_champ", nullable = false, unique = true, length = 100)
    private String nomChamp;

    // Ordre d'affichage dans le tableau
    @Column(name = "ordre")
    private Integer ordre;
}