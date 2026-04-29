package com.orienet.tresorie.service;

import com.orienet.tresorie.model.ChampDynamique;
import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.repository.CaisseRepository;
import com.orienet.tresorie.repository.ChampDynamiqueRepository;
import com.orienet.tresorie.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository      operationRepository;
    private final CaisseRepository         caisseRepository;
    private final CaisseService            caisseService;
    private final ChampDynamiqueRepository champRepository;

    private static final Set<String> NATURES_VALIDES = Set.of(
            "Encaissement", "Décaissement", "Créance", "Dette", "Solde de départ"
    );

    // ─── Lister opérations actives ────────────────────────────────
    public List<Operation> findAll()                 { return operationRepository.findByArchiveeFalse(); }
    public Optional<Operation> findById(Long id)     { return operationRepository.findById(id); }
    public List<Operation> findByCaisse(String c)    { return operationRepository.findByCaisseAndArchiveeFalse(c); }
    public List<Operation> findByNatureFlux(String n){ return operationRepository.findByNatureFluxAndArchiveeFalse(n); }
    public List<Operation> findByEtat(String e)      { return operationRepository.findByEtatAndArchiveeFalse(e); }
    public List<Operation> findByDateBetween(LocalDate d, LocalDate f) {
        return operationRepository.findByDateFluxBetweenAndArchiveeFalse(d, f);
    }

    // ─── Lister opérations archivées ─────────────────────────────
    public List<Operation> findArchivees() { return operationRepository.findByArchiveeTrue(); }

    // ─── SAUVEGARDER ─────────────────────────────────────────────
    @Transactional
    public Operation sauvegarder(Operation operation, Map<String, String> valeursDyn, String creePar) {
        validerNatureFlux(operation.getNatureFlux());
        verifierCaisse(operation.getCaisse());
        operation.setArchivee(false);
        operation.setValeursDynamiques(mapToJson(valeursDyn));
        operation.setCreePar(creePar);
        Operation saved = operationRepository.save(operation);
        recalculer(saved.getCaisse());
        return saved;
    }

    // ─── MODIFIER ────────────────────────────────────────────────
    @Transactional
    public Operation modifier(Long id, Operation modifiee, Map<String, String> valeursDyn) {
        validerNatureFlux(modifiee.getNatureFlux());
        verifierCaisse(modifiee.getCaisse());

        Operation existing = operationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opération introuvable : " + id));

        String ancienneCaisse = existing.getCaisse();
        String ancienEtat     = existing.getEtat();

        existing.setDateFlux(modifiee.getDateFlux());
        existing.setNatureFlux(modifiee.getNatureFlux());
        existing.setCaisse(modifiee.getCaisse());
        existing.setModeFlux(modifiee.getModeFlux());
        existing.setTitulaireFlux(modifiee.getTitulaireFlux());
        existing.setMontant(modifiee.getMontant());
        existing.setCcp(modifiee.getCcp());
        existing.setFamille(modifiee.getFamille());
        existing.setDesignation(modifiee.getDesignation());
        existing.setDescription(modifiee.getDescription());
        existing.setEtat(modifiee.getEtat());

        Map<String, String> existingDyn = jsonToMap(existing.getValeursDynamiques());
        if (valeursDyn != null) existingDyn.putAll(valeursDyn);
        existing.setValeursDynamiques(mapToJson(existingDyn));

        Operation saved = operationRepository.save(existing);

        // Recalculer la caisse dans tous les cas
        // (l'état Annulé est géré dans les requêtes SQL du repository)
        recalculer(ancienneCaisse);
        if (!Objects.equals(ancienneCaisse, saved.getCaisse()))
            recalculer(saved.getCaisse());

        return saved;
    }

    // ─── ARCHIVER (au lieu de supprimer) ─────────────────────────
    // L'opération passe à archivee=true → disparaît du tableau principal
    // La caisse N'EST PAS recalculée : le montant reste comptabilisé
    @Transactional
    public void archiver(Long id) {
        Operation op = operationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opération introuvable : " + id));

        op.setArchivee(true);
        op.setDateArchivage(LocalDate.now());
        operationRepository.save(op);

        // PAS de recalcul intentionnel :
        // Caisse 11000 + archive op 1000 → caisse reste 11000
    }

    // ─── RESTAURER depuis les archives ───────────────────────────
    @Transactional
    public void restaurer(Long id) {
        Operation op = operationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opération introuvable : " + id));

        op.setArchivee(false);
        op.setDateArchivage(null);
        operationRepository.save(op);

        // Recalculer : l'opération réintègre la caisse
        recalculer(op.getCaisse());
    }

    // ─── SUPPRIMER DÉFINITIVEMENT (depuis les archives) ──────────
    @Transactional
    public void supprimerDefinitivement(Long id) {
        Operation op = operationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opération introuvable : " + id));
        if (!op.isArchivee())
            throw new RuntimeException("Seules les opérations archivées peuvent être supprimées définitivement.");
        operationRepository.deleteById(id);
        // Pas besoin de recalculer : l'op était déjà archivée (exclue)
    }

    // ─── CHAMPS DYNAMIQUES ───────────────────────────────────────
    @Transactional
    public ChampDynamique ajouterChamp(String nomChamp) {
        if (nomChamp == null || nomChamp.isBlank())
            throw new RuntimeException("❌ Le nom du champ est obligatoire.");
        String nom = nomChamp.trim();
        if (champRepository.findByNomChamp(nom).isPresent())
            throw new RuntimeException("❌ Le champ \"" + nom + "\" existe déjà.");
        int ordre = champRepository.findMaxOrdre() + 1;
        return champRepository.save(ChampDynamique.builder().nomChamp(nom).ordre(ordre).build());
    }

    @Transactional
    public void supprimerChamp(Long id) {
        champRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Champ introuvable."));
        champRepository.deleteById(id);
    }

    public List<ChampDynamique> listerChamps() {
        return champRepository.findAllByOrderByOrdreAsc();
    }

    public Map<String, String> getValeursDynamiques(Operation op) {
        return jsonToMap(op.getValeursDynamiques());
    }

    // ─── JSON manuel ─────────────────────────────────────────────
    public String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey() == null) continue;
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(e.getKey())).append("\":");
            sb.append("\"").append(escapeJson(e.getValue() != null ? e.getValue() : "")).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public Map<String, String> jsonToMap(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isBlank() || json.trim().equals("{}")) return result;
        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}"))   content = content.substring(0, content.length() - 1);
        content = content.trim();
        if (content.isEmpty()) return result;
        int i = 0;
        while (i < content.length()) {
            int ks = content.indexOf('"', i);       if (ks < 0) break;
            int ke = content.indexOf('"', ks + 1);  if (ke < 0) break;
            String key = unescapeJson(content.substring(ks + 1, ke));
            int colon = content.indexOf(':', ke);   if (colon < 0) break;
            int vs = content.indexOf('"', colon);   if (vs < 0) break;
            int ve = vs + 1;
            while (ve < content.length()) {
                if (content.charAt(ve) == '"' && content.charAt(ve - 1) != '\\') break;
                ve++;
            }
            result.put(key, unescapeJson(content.substring(vs + 1, ve)));
            i = ve + 1;
            while (i < content.length() && (content.charAt(i) == ',' || content.charAt(i) == ' ')) i++;
        }
        return result;
    }

    private String escapeJson(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r").replace("\t","\\t");
    }
    private String unescapeJson(String s) {
        return s.replace("\\\"","\"").replace("\\\\","\\")
                .replace("\\n","\n").replace("\\r","\r").replace("\\t","\t");
    }

    // ─── Validation ──────────────────────────────────────────────
    private void validerNatureFlux(String nature) {
        if (nature == null || nature.isBlank())
            throw new RuntimeException("❌ La Nature de Flux est obligatoire.");
        if (!NATURES_VALIDES.contains(nature))
            throw new RuntimeException("❌ Nature invalide : \"" + nature + "\".");
    }
    private void verifierCaisse(String nom) {
        if (nom != null && !nom.isBlank())
            caisseRepository.findByNom(nom)
                    .orElseThrow(() -> new RuntimeException("Caisse introuvable : \"" + nom + "\""));
    }
    private void recalculer(String nom) {
        if (nom != null && !nom.isBlank())
            caisseService.recalculerTotaux(nom);
    }
}