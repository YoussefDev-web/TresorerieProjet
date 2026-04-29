package com.orienet.tresorie.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activite")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String utilisateur;
    
    private String action; // AJOUT, MODIFICATION, ARCHIVAGE, RESTAURATION, SUPPRESSION

    @Column(length = 1000)
    private String details;

    private LocalDateTime dateAction;
}
