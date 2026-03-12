package com.orienet.tresorie.service;

import com.orienet.tresorie.model.Caisse;
import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.repository.CaisseRepository;
import com.orienet.tresorie.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository operationRepository;
    private final CaisseRepository    caisseRepository;
    private final CaisseService       caisseService;

    // ─── Lister toutes les opérations ─────────────────────────────
    public List<Operation> findAll() {
        return operationRepository.findAll();
    }

    // ─── Trouver par ID ───────────────────────────────────────────
    public Optional<Operation> findById(Long id) {
        return operationRepository.findById(id);
    }

    // ─── Filtrer par caisse ────────────────────────────────────────
    public List<Operation> findByCaisse(String caisse) {
        return operationRepository.findByCaisse(caisse);
    }

    // ─── Filtrer par nature de flux ───────────────────────────────
    public List<Operation> findByNatureFlux(String natureFlux) {
        return operationRepository.findByNatureFlux(natureFlux);
    }

    // ─── Filtrer par état ─────────────────────────────────────────
    public List<Operation> findByEtat(String etat) {
        return operationRepository.findByEtat(etat);
    }

    // ──────────────────────────────────────────────────────────────
    // SAUVEGARDER une nouvelle opération
    //
    // Étapes :
    //   1. Vérifier que la caisse existe en base (par son nom)
    //   2. Sauvegarder l'opération
    //   3. Recalculer les totaux de la caisse (encaissement,
    //      décaissement, créance, dette, cashDisponible)
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public Operation sauvegarder(Operation operation) {

        String nomCaisse = operation.getCaisse();

        // 1. Vérifier que la caisse existe
        if (nomCaisse != null && !nomCaisse.isBlank()) {
            Caisse caisse = caisseRepository.findByNom(nomCaisse)
                    .orElseThrow(() -> new RuntimeException(
                            "Caisse introuvable : \"" + nomCaisse + "\". "
                                    + "Veuillez vérifier le nom de la caisse."));
        }

        // 2. Sauvegarder l'opération
        Operation saved = operationRepository.save(operation);

        // 3. Recalculer les totaux de la caisse
        //    → le montant est ajouté selon la nature du flux :
        //      Encaissement  → encaissement  +montant
        //      Décaissement  → decaissement  +montant
        //      Créance       → creance       +montant
        //      Dette         → dette         +montant
        //      Solde départ  → cashDisponible +montant
        if (nomCaisse != null && !nomCaisse.isBlank()) {
            caisseService.recalculerTotaux(nomCaisse);
        }

        return saved;
    }

    // ──────────────────────────────────────────────────────────────
    // MODIFIER une opération existante
    //
    // Si la caisse a changé → recalculer l'ancienne ET la nouvelle
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public Operation modifier(Long id, Operation operationModifiee) {

        Operation existing = operationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opération introuvable : " + id));

        String ancienneCaisse = existing.getCaisse();
        String nouvelleCaisse = operationModifiee.getCaisse();

        // Vérifier que la nouvelle caisse existe
        if (nouvelleCaisse != null && !nouvelleCaisse.isBlank()) {
            caisseRepository.findByNom(nouvelleCaisse)
                    .orElseThrow(() -> new RuntimeException(
                            "Caisse introuvable : \"" + nouvelleCaisse + "\""));
        }

        // Mettre à jour les champs
        existing.setDateFlux(operationModifiee.getDateFlux());
        existing.setNatureFlux(operationModifiee.getNatureFlux());
        existing.setCaisse(nouvelleCaisse);
        existing.setModeFlux(operationModifiee.getModeFlux());
        existing.setTitulaireFlux(operationModifiee.getTitulaireFlux());
        existing.setMontant(operationModifiee.getMontant());
        existing.setCcp(operationModifiee.getCcp());
        existing.setFamille(operationModifiee.getFamille());
        existing.setDesignation(operationModifiee.getDesignation());
        existing.setDescription(operationModifiee.getDescription());
        existing.setEtat(operationModifiee.getEtat());

        Operation saved = operationRepository.save(existing);

        // Recalculer l'ancienne caisse
        if (ancienneCaisse != null && !ancienneCaisse.isBlank()) {
            caisseService.recalculerTotaux(ancienneCaisse);
        }

        // Recalculer la nouvelle caisse si elle a changé
        if (nouvelleCaisse != null && !nouvelleCaisse.isBlank()
                && !nouvelleCaisse.equals(ancienneCaisse)) {
            caisseService.recalculerTotaux(nouvelleCaisse);
        }

        return saved;
    }

    // ──────────────────────────────────────────────────────────────
    // SUPPRIMER une opération
    //
    // Recalculer la caisse après suppression
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public void supprimer(Long id) {
        Operation operation = operationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opération introuvable : " + id));

        String nomCaisse = operation.getCaisse();
        operationRepository.deleteById(id);

        // Recalculer les totaux après suppression
        if (nomCaisse != null && !nomCaisse.isBlank()) {
            caisseService.recalculerTotaux(nomCaisse);
        }
    }
}