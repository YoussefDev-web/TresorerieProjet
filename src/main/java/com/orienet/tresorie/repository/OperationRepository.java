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

    // ─── Opérations ACTIVES ───────────────────────────────────────
    List<Operation> findByArchiveeFalse();
    List<Operation> findByCaisseAndArchiveeFalse(String caisse);
    List<Operation> findByNatureFluxAndArchiveeFalse(String natureFlux);
    List<Operation> findByEtatAndArchiveeFalse(String etat);
    List<Operation> findByDateFluxBetweenAndArchiveeFalse(LocalDate debut, LocalDate fin);

    // ─── Opérations ARCHIVÉES ─────────────────────────────────────
    List<Operation> findByArchiveeTrue();

    // ─── Valeurs distinctes pour les listes déroulantes du filtre ─
    @Query("SELECT DISTINCT o.natureFlux   FROM Operation o WHERE o.archivee = false AND o.natureFlux   IS NOT NULL ORDER BY o.natureFlux")
    List<String> findDistinctNatureFlux();

    @Query("SELECT DISTINCT o.caisse       FROM Operation o WHERE o.archivee = false AND o.caisse       IS NOT NULL ORDER BY o.caisse")
    List<String> findDistinctCaisse();

    @Query("SELECT DISTINCT o.modeFlux     FROM Operation o WHERE o.archivee = false AND o.modeFlux     IS NOT NULL ORDER BY o.modeFlux")
    List<String> findDistinctModeFlux();

    @Query("SELECT DISTINCT o.titulaireFlux FROM Operation o WHERE o.archivee = false AND o.titulaireFlux IS NOT NULL ORDER BY o.titulaireFlux")
    List<String> findDistinctTitulaireFlux();

    @Query("SELECT DISTINCT o.ccp          FROM Operation o WHERE o.archivee = false AND o.ccp          IS NOT NULL ORDER BY o.ccp")
    List<String> findDistinctCcp();

    @Query("SELECT DISTINCT o.famille      FROM Operation o WHERE o.archivee = false AND o.famille      IS NOT NULL ORDER BY o.famille")
    List<String> findDistinctFamille();

    @Query("SELECT DISTINCT o.designation  FROM Operation o WHERE o.archivee = false AND o.designation  IS NOT NULL ORDER BY o.designation")
    List<String> findDistinctDesignation();

    @Query("SELECT DISTINCT o.etat         FROM Operation o WHERE o.archivee = false AND o.etat         IS NOT NULL ORDER BY o.etat")
    List<String> findDistinctEtat();

    // ─── Filtre avancé combiné (toutes les sections du modal) ─────
    // Tous les paramètres sont optionnels (null = ignorer ce filtre)
    @Query("SELECT o FROM Operation o WHERE o.archivee = false " +
            "AND (:natureFlux    IS NULL OR o.natureFlux    = :natureFlux) " +
            "AND (:dateDebut     IS NULL OR o.dateFlux     >= :dateDebut) " +
            "AND (:dateFin       IS NULL OR o.dateFlux     <= :dateFin) " +
            "AND (:caisse        IS NULL OR o.caisse        = :caisse) " +
            "AND (:modeFlux      IS NULL OR o.modeFlux      = :modeFlux) " +
            "AND (:titulaireFlux IS NULL OR o.titulaireFlux = :titulaireFlux) " +
            "AND (:ccp           IS NULL OR o.ccp           = :ccp) " +
            "AND (:famille       IS NULL OR o.famille       = :famille) " +
            "AND (:designation   IS NULL OR o.designation   = :designation) " +
            "AND (:etat          IS NULL OR o.etat          = :etat) " +
            "ORDER BY o.dateFlux DESC")
    List<Operation> filtrerAvance(
            @Param("natureFlux")    String natureFlux,
            @Param("dateDebut")     LocalDate dateDebut,
            @Param("dateFin")       LocalDate dateFin,
            @Param("caisse")        String caisse,
            @Param("modeFlux")      String modeFlux,
            @Param("titulaireFlux") String titulaireFlux,
            @Param("ccp")           String ccp,
            @Param("famille")       String famille,
            @Param("designation")   String designation,
            @Param("etat")          String etat
    );

    // ─── Totaux pour recalcul de caisse ──────────────────────────
    // Inclut uniquement les opérations VALIDÉES
    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Encaissement' " +
            "AND o.etat = 'Validé'")
    BigDecimal sumEncaissementByCaisse(@Param("caisse") String caisse);

    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Décaissement' " +
            "AND o.etat = 'Validé'")
    BigDecimal sumDecaissementByCaisse(@Param("caisse") String caisse);

    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Créance' " +
            "AND o.etat = 'Validé'")
    BigDecimal sumCreanceByCaisse(@Param("caisse") String caisse);

    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Dette' " +
            "AND o.etat = 'Validé'")
    BigDecimal sumDetteByCaisse(@Param("caisse") String caisse);

    @Query("SELECT COALESCE(SUM(o.montant), 0) FROM Operation o " +
            "WHERE o.caisse = :caisse AND o.natureFlux = 'Solde de départ' " +
            "AND o.etat = 'Validé'")
    BigDecimal sumSoldeDepartByCaisse(@Param("caisse") String caisse);
}