package com.orienet.tresorie.repository;

import com.orienet.tresorie.model.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {

    // ─── Filtres ──────────────────────────────────────────────────
    List<Operation> findByCaisse(String caisse);
    List<Operation> findByNatureFlux(String natureFlux);
    List<Operation> findByEtat(String etat);
    List<Operation> findByDateFluxBetween(LocalDate debut, LocalDate fin);

    // ─────────────────────────────────────────────────────────────
    // Totaux par caisse selon nature de flux
    // Ces requêtes sont appelées par CaisseService.recalculerTotaux()
    // après chaque ajout / modification / suppression d'opération
    // ─────────────────────────────────────────────────────────────

    // Somme des Encaissements
    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Encaissement'")
    BigDecimal sumEncaissementByCaisse(@Param("caisse") String caisse);

    // Somme des Décaissements
    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Décaissement'")
    BigDecimal sumDecaissementByCaisse(@Param("caisse") String caisse);

    // Somme des Créances
    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Créance'")
    BigDecimal sumCreanceByCaisse(@Param("caisse") String caisse);

    // Somme des Dettes
    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Dette'")
    BigDecimal sumDetteByCaisse(@Param("caisse") String caisse);

    // Somme des Soldes de départ
    // → ajouté directement au cashDisponible comme base de départ
    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Solde de départ'")
    BigDecimal sumSoldeDepartByCaisse(@Param("caisse") String caisse);
}