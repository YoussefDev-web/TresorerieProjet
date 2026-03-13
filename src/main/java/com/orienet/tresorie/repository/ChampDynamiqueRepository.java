package com.orienet.tresorie.repository;

import com.orienet.tresorie.model.ChampDynamique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChampDynamiqueRepository extends JpaRepository<ChampDynamique, Long> {

    // Récupérer tous les champs triés par ordre d'affichage
    List<ChampDynamique> findAllByOrderByOrdreAsc();

    // Vérifier si un champ avec ce nom existe déjà
    Optional<ChampDynamique> findByNomChamp(String nomChamp);

    // Compter pour générer l'ordre automatique
    @Query("SELECT COALESCE(MAX(c.ordre), 0) FROM ChampDynamique c")
    Integer findMaxOrdre();
}