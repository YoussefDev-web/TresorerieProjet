package com.orienet.tresorie.service;

import com.orienet.tresorie.model.Caisse;
import com.orienet.tresorie.repository.CaisseRepository;
import com.orienet.tresorie.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CaisseService {

    private final CaisseRepository    caisseRepository;
    private final OperationRepository operationRepository;

    // ─── Lister toutes les caisses ────────────────────────────────
    public List<Caisse> findAll() {
        return caisseRepository.findAll();
    }

    // ─── Trouver par ID ───────────────────────────────────────────
    public Optional<Caisse> findById(int id) {
        return caisseRepository.findById(id);
    }

    // ─── Trouver par nom ──────────────────────────────────────────
    public Optional<Caisse> findByNom(String nom) {
        return caisseRepository.findByNom(nom);
    }

    // ─── Ajouter / modifier une caisse ────────────────────────────
    @Transactional
    public Caisse sauvegarder(Caisse caisse) {
        return caisseRepository.save(caisse);
    }

    // ─── Supprimer une caisse ─────────────────────────────────────
    @Transactional
    public void supprimer(int id) {
        caisseRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────
    // Recalculer les totaux d'une caisse
    //
    // Règles métier :
    //   encaissement  = SUM(montant) où natureFlux = 'Encaissement'
    //   decaissement  = SUM(montant) où natureFlux = 'Décaissement'
    //   creance       = SUM(montant) où natureFlux = 'Créance'       ← séparé, affiché seul
    //   dette         = SUM(montant) où natureFlux = 'Dette'         ← séparé, affiché seul
    //
    //   cashDisponible = encaissement - decaissement                 ← formule simplifiée
    //                    (Créance et Dette sont affichées séparément,
    //                     elles n'entrent PAS dans le cash disponible)
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public void recalculerTotaux(String nomCaisse) {

        // 1. Chercher la caisse
        Caisse caisse = caisseRepository.findByNom(nomCaisse)
                .orElseThrow(() -> new RuntimeException(
                        "Caisse introuvable : " + nomCaisse));

        // 2. Calculer chaque total depuis les opérations
        BigDecimal encaissement;
        BigDecimal decaissement;
        BigDecimal creance;
        BigDecimal dette;

        if ("caisse centrale".equalsIgnoreCase(nomCaisse)) {
            // La caisse centrale regroupe le total de toutes les autres caisses (toutes les opérations)
            encaissement = orZero(operationRepository.sumEncaissementGlobal());
            decaissement = orZero(operationRepository.sumDecaissementGlobal());
            creance      = orZero(operationRepository.sumCreanceGlobal());
            dette        = orZero(operationRepository.sumDetteGlobal());
        } else {
            // Calcul normal pour une caisse spécifique
            encaissement = orZero(operationRepository.sumEncaissementByCaisse(nomCaisse));
            decaissement = orZero(operationRepository.sumDecaissementByCaisse(nomCaisse));
            creance      = orZero(operationRepository.sumCreanceByCaisse(nomCaisse));
            dette        = orZero(operationRepository.sumDetteByCaisse(nomCaisse));
        }

        // 3. Cash Disponible = Encaissement - Décaissement
        //    Créance et Dette sont stockées séparément pour affichage
        //    mais n'influencent PAS le cash disponible
        BigDecimal cashDisponible = encaissement.subtract(decaissement);

        // 4. Sauvegarder
        caisse.setEncaissement(encaissement);
        caisse.setDecaissement(decaissement);
        caisse.setCreance(creance);
        caisse.setDette(dette);
        caisse.setCashDisponible(cashDisponible);

        caisseRepository.save(caisse);
    }

    // ─── Utilitaire null → zéro ───────────────────────────────────
    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}