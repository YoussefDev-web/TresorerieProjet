package com.orienet.tresorie.service;

import com.orienet.tresorie.model.ChampDynamique;
import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.repository.CaisseRepository;
import com.orienet.tresorie.repository.ChampDynamiqueRepository;
import com.orienet.tresorie.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ─── CRUD opérations ─────────────────────────────────────────
    public List<Operation>    findAll()               { return operationRepository.findAll(); }
    public Optional<Operation> findById(Long id)      { return operationRepository.findById(id); }
    public List<Operation>    findByCaisse(String c)  { return operationRepository.findByCaisse(c); }
    public List<Operation>    findByNatureFlux(String n){ return operationRepository.findByNatureFlux(n); }
    public List<Operation>    findByEtat(String e)    { return operationRepository.findByEtat(e); }

    // ─── SAUVEGARDER ─────────────────────────────────────────────
    @Transactional
    public Operation sauvegarder(Operation operation, Map<String, String> valeursDyn) {
        validerNatureFlux(operation.getNatureFlux());
        verifierCaisse(operation.getCaisse());

        // Convertir la Map en JSON et stocker dans la colonne
        operation.setValeursDynamiques(mapToJson(valeursDyn));

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

        // Fusionner : garder les anciennes valeurs + écraser avec les nouvelles
        Map<String, String> existingDyn = jsonToMap(existing.getValeursDynamiques());
        if (valeursDyn != null) existingDyn.putAll(valeursDyn);
        existing.setValeursDynamiques(mapToJson(existingDyn));

        Operation saved = operationRepository.save(existing);
        recalculer(ancienneCaisse);
        if (!Objects.equals(ancienneCaisse, saved.getCaisse()))
            recalculer(saved.getCaisse());
        return saved;
    }

    // ─── SUPPRIMER ───────────────────────────────────────────────
    @Transactional
    public void supprimer(Long id) {
        Operation op = operationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Opération introuvable : " + id));
        String caisse = op.getCaisse();
        operationRepository.deleteById(id);
        recalculer(caisse);
    }

    // ─── AJOUTER UN CHAMP ────────────────────────────────────────
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

    // ─── SUPPRIMER UN CHAMP ──────────────────────────────────────
    @Transactional
    public void supprimerChamp(Long id) {
        champRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Champ introuvable."));
        champRepository.deleteById(id);
    }

    // ─── LISTER LES CHAMPS ───────────────────────────────────────
    public List<ChampDynamique> listerChamps() {
        return champRepository.findAllByOrderByOrdreAsc();
    }

    // ─── LIRE LES VALEURS DYNAMIQUES D'UNE OPÉRATION ─────────────
    public Map<String, String> getValeursDynamiques(Operation op) {
        return jsonToMap(op.getValeursDynamiques());
    }

    // ─────────────────────────────────────────────────────────────
    // JSON MANUEL — pas de dépendance externe
    // Map {"Référence":"REF-001","Projet":"BM"} ↔ String JSON
    // ─────────────────────────────────────────────────────────────

    /**
     * Convertit une Map<String,String> en JSON string.
     * Ex: {"Référence":"REF-001","Projet":"Bestmobile"}
     */
    public String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            sb.append("\"").append(escapeJson(entry.getValue() != null ? entry.getValue() : "")).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Parse un JSON string en Map<String,String>.
     * Supporte uniquement le format plat {"clé":"valeur",...}
     */
    public Map<String, String> jsonToMap(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isBlank() || json.trim().equals("{}")) return result;

        String content = json.trim();
        // Enlever les accolades
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}"))   content = content.substring(0, content.length() - 1);
        content = content.trim();
        if (content.isEmpty()) return result;

        // Parser chaque paire "clé":"valeur"
        int i = 0;
        while (i < content.length()) {
            // Chercher la clé
            int ks = content.indexOf('"', i);
            if (ks < 0) break;
            int ke = content.indexOf('"', ks + 1);
            if (ke < 0) break;
            String key = unescapeJson(content.substring(ks + 1, ke));

            // Chercher le ":"
            int colon = content.indexOf(':', ke);
            if (colon < 0) break;

            // Chercher la valeur
            int vs = content.indexOf('"', colon);
            if (vs < 0) break;
            int ve = vs + 1;
            // Gérer les guillemets échappés
            while (ve < content.length()) {
                if (content.charAt(ve) == '"' && content.charAt(ve - 1) != '\\') break;
                ve++;
            }
            String value = unescapeJson(content.substring(vs + 1, ve));

            result.put(key, value);
            i = ve + 1;
            // Sauter la virgule
            while (i < content.length() && (content.charAt(i) == ',' || content.charAt(i) == ' ')) i++;
        }
        return result;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\")
                .replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }

    // ─── Validation ──────────────────────────────────────────────
    private void validerNatureFlux(String nature) {
        if (nature == null || nature.isBlank())
            throw new RuntimeException("❌ La Nature de Flux est obligatoire.");
        if (!NATURES_VALIDES.contains(nature))
            throw new RuntimeException("❌ Nature invalide : \"" + nature + "\". Valeurs acceptées : "
                    + String.join(", ", NATURES_VALIDES));
    }

    private void verifierCaisse(String nomCaisse) {
        if (nomCaisse != null && !nomCaisse.isBlank())
            caisseRepository.findByNom(nomCaisse)
                    .orElseThrow(() -> new RuntimeException("Caisse introuvable : \"" + nomCaisse + "\""));
    }

    private void recalculer(String nomCaisse) {
        if (nomCaisse != null && !nomCaisse.isBlank())
            caisseService.recalculerTotaux(nomCaisse);
    }
}