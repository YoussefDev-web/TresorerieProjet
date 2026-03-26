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

    // ─── Opérations ACTIVES (non archivées) ──────────────────────
    List<Operation> findByArchiveeFalse();
    List<Operation> findByCaisseAndArchiveeFalse(String caisse);
    List<Operation> findByNatureFluxAndArchiveeFalse(String natureFlux);
    List<Operation> findByEtatAndArchiveeFalse(String etat);
    List<Operation> findByDateFluxBetweenAndArchiveeFalse(LocalDate debut, LocalDate fin);

    // ─── Opérations ARCHIVÉES ─────────────────────────────────────
    List<Operation> findByArchiveeTrue();

    // ─────────────────────────────────────────────────────────────
    // Totaux pour recalcul de caisse :
    //   - exclut les opérations ARCHIVÉES (archivee = false)
    //   - exclut les opérations ANNULÉES  (etat != 'Annulé')
    // ─────────────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Encaissement' " +
            "AND o.archivee = false AND (o.etat IS NULL OR o.etat <> 'Annulé')")
    BigDecimal sumEncaissementByCaisse(@Param("caisse") String caisse);

    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Décaissement' " +
            "AND o.archivee = false AND (o.etat IS NULL OR o.etat <> 'Annulé')")
    BigDecimal sumDecaissementByCaisse(@Param("caisse") String caisse);

    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Créance' " +
            "AND o.archivee = false AND (o.etat IS NULL OR o.etat <> 'Annulé')")
    BigDecimal sumCreanceByCaisse(@Param("caisse") String caisse);

    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Dette' " +
            "AND o.archivee = false AND (o.etat IS NULL OR o.etat <> 'Annulé')")
    BigDecimal sumDetteByCaisse(@Param("caisse") String caisse);

    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Solde de départ' " +
            "AND o.archivee = false AND (o.etat IS NULL OR o.etat <> 'Annulé')")
    BigDecimal sumSoldeDepartByCaisse(@Param("caisse") String caisse);
}