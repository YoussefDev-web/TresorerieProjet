package com.orienet.tresorie.controller;

import com.orienet.tresorie.model.Caisse;
import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.service.CaisseService;
import com.orienet.tresorie.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ReportingController {

    private final OperationService operationService;
    private final CaisseService caisseService;

    @GetMapping("/reporting")
    public String reportingPage(Model model) {
        return "reporting"; // renvoie la vue reporting.html
    }

    @GetMapping("/api/caisse/recalculate-all")
    public String recalculateAllCaisses() {
        List<Caisse> caisses = caisseService.findAll();
        for (Caisse c : caisses) {
            caisseService.recalculerTotaux(c.getNom());
        }
        return "redirect:/flux-tresorerie";
    }

    @GetMapping("/api/reporting/stats")
    @ResponseBody
    public Map<String, Object> getReportingStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Solde des caisses
        List<Caisse> caisses = caisseService.findAll();
        List<String> caisseLabels = new ArrayList<>();
        List<BigDecimal> caisseBalances = new ArrayList<>();
        List<String> caisseNames = new ArrayList<>();

        for (Caisse c : caisses) {
            // Exclure la caisse centrale pour éviter de fausser les graphiques avec le total global
            if ("caisse centrale".equalsIgnoreCase(c.getNom())) {
                continue;
            }
            caisseLabels.add(c.getNom());
            caisseBalances.add(c.getCashDisponible() != null ? c.getCashDisponible() : BigDecimal.ZERO);
            caisseNames.add(c.getNom());
        }

        stats.put("caisses", Map.of(
                "labels", caisseLabels,
                "data", caisseBalances));

        // Liste des noms de caisses pour le sélecteur frontend
        stats.put("caisseNames", caisseNames);

        // 2. Répartition par nature de flux (Encaissement, Décaissement, etc.)
        // Uniquement les opérations validées
        List<Operation> actives = operationService.findAll().stream()
                .filter(op -> "Validé".equals(op.getEtat()))
                .collect(Collectors.toList());

        Map<String, BigDecimal> fluxMap = new LinkedHashMap<>();
        fluxMap.put("Encaissement", BigDecimal.ZERO);
        fluxMap.put("Décaissement", BigDecimal.ZERO);
        fluxMap.put("Créance", BigDecimal.ZERO);
        fluxMap.put("Dette", BigDecimal.ZERO);

        for (Operation op : actives) {
            if (op.getNatureFlux() != null && fluxMap.containsKey(op.getNatureFlux())) {
                BigDecimal current = fluxMap.get(op.getNatureFlux());
                fluxMap.put(op.getNatureFlux(),
                        current.add(op.getMontant() != null ? op.getMontant() : BigDecimal.ZERO));
            }
        }

        stats.put("flux", Map.of(
                "labels", new ArrayList<>(fluxMap.keySet()),
                "data", new ArrayList<>(fluxMap.values())));

        // 3. Évolution mensuelle (Générale - toutes les caisses)
        stats.put("evolution", buildEvolutionData(actives));

        return stats;
    }

    /**
     * Endpoint dédié pour l'évolution mensuelle avec filtre par caisse.
     * Si caisse est vide ou "all", retourne les données de toutes les caisses.
     */
    @GetMapping("/api/reporting/evolution")
    @ResponseBody
    public Map<String, Object> getEvolutionByCaisse(
            @RequestParam(value = "caisse", required = false, defaultValue = "all") String caisse) {

        List<Operation> actives = operationService.findAll().stream()
                .filter(op -> "Validé".equals(op.getEtat()))
                .collect(Collectors.toList());

        // Filtrer par caisse si spécifié (ignorer si 'all' ou 'Caisse Centrale' car ils représentent le global)
        if (caisse != null && !caisse.isEmpty() && !"all".equalsIgnoreCase(caisse) && !"caisse centrale".equalsIgnoreCase(caisse)) {
            actives = actives.stream()
                    .filter(op -> caisse.equalsIgnoreCase(op.getCaisse()))
                    .collect(Collectors.toList());
        }

        return buildEvolutionData(actives);
    }

    /**
     * Méthode utilitaire pour construire les données d'évolution mensuelle.
     */
    private Map<String, Object> buildEvolutionData(List<Operation> operations) {
        Map<String, BigDecimal> encaissementsMensuels = new TreeMap<>();
        Map<String, BigDecimal> decaissementsMensuels = new TreeMap<>();
        DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Operation op : operations) {
            if (op.getDateFlux() != null) {
                String mois = op.getDateFlux().format(ymFormatter);
                BigDecimal montant = op.getMontant() != null ? op.getMontant() : BigDecimal.ZERO;

                if ("Encaissement".equals(op.getNatureFlux())) {
                    encaissementsMensuels.put(mois,
                            encaissementsMensuels.getOrDefault(mois, BigDecimal.ZERO).add(montant));
                } else if ("Décaissement".equals(op.getNatureFlux())) {
                    decaissementsMensuels.put(mois,
                            decaissementsMensuels.getOrDefault(mois, BigDecimal.ZERO).add(montant));
                }
            }
        }

        Set<String> tousLesMois = new TreeSet<>();
        tousLesMois.addAll(encaissementsMensuels.keySet());
        tousLesMois.addAll(decaissementsMensuels.keySet());

        List<String> moisLabels = new ArrayList<>(tousLesMois);
        List<BigDecimal> encaissementsData = new ArrayList<>();
        List<BigDecimal> decaissementsData = new ArrayList<>();
        List<BigDecimal> soldeData = new ArrayList<>();

        for (String mois : moisLabels) {
            BigDecimal enc = encaissementsMensuels.getOrDefault(mois, BigDecimal.ZERO);
            BigDecimal dec = decaissementsMensuels.getOrDefault(mois, BigDecimal.ZERO);
            encaissementsData.add(enc);
            decaissementsData.add(dec);
            soldeData.add(enc.subtract(dec));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", moisLabels);
        result.put("encaissements", encaissementsData);
        result.put("decaissements", decaissementsData);
        result.put("solde", soldeData);
        return result;
    }
}
