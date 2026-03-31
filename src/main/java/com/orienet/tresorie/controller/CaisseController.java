package com.orienet.tresorie.controller;

import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.repository.OperationRepository;
import com.orienet.tresorie.service.CaisseService;
import com.orienet.tresorie.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CaisseController {

    private final CaisseService       caisseService;
    private final OperationService    operationService;
    private final OperationRepository operationRepository;

    @GetMapping("/flux-tresorerie")
    public String fluxTresorerie(
            // ── Section 1 : Mouvement de flux ──
            @RequestParam(required = false) String natureFlux,
            // ── Section 2 : Filtre par date ──
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            // ── Section 3 : Quatre filtres ──
            @RequestParam(required = false) String caisse,
            @RequestParam(required = false) String modeFlux,
            @RequestParam(required = false) String titulaireFlux,
            @RequestParam(required = false) String ccp,
            @RequestParam(required = false) String famille,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String etat,
            // ── Indique si un filtre a été appliqué ──
            @RequestParam(required = false) String filtre,
            Model model) {

        // Nettoyer les chaînes vides → null (pour que le filtre JPQL fonctionne)
        natureFlux    = blank(natureFlux);
        caisse        = blank(caisse);
        modeFlux      = blank(modeFlux);
        titulaireFlux = blank(titulaireFlux);
        ccp           = blank(ccp);
        famille       = blank(famille);
        designation   = blank(designation);
        etat          = blank(etat);

        // Filtre actif UNIQUEMENT si au moins un vrai critère est renseigné
        // On ignore complètement le param "filtre=1" du bouton submit
        List<Operation> operations;
        boolean filtreActif =
                natureFlux != null || dateDebut != null || dateFin != null ||
                        caisse != null || modeFlux != null || titulaireFlux != null ||
                        ccp != null || famille != null || designation != null || etat != null;

        if (filtreActif) {
            operations = operationRepository.filtrerAvance(
                    natureFlux, dateDebut, dateFin,
                    caisse, modeFlux, titulaireFlux,
                    ccp, famille, designation, etat
            );
        } else {
            operations = operationService.findAll();
        }

        // Parser le JSON de chaque opération
        Map<Long, Map<String, String>> valeursDynMap = new HashMap<>();
        for (Operation op : operations) {
            valeursDynMap.put(op.getId(), operationService.jsonToMap(op.getValeursDynamiques()));
        }

        // Listes déroulantes du modal (valeurs existantes en base)
        model.addAttribute("listNatureFlux",    operationRepository.findDistinctNatureFlux());
        model.addAttribute("listCaisse",        operationRepository.findDistinctCaisse());
        model.addAttribute("listModeFlux",      operationRepository.findDistinctModeFlux());
        model.addAttribute("listTitulaire",     operationRepository.findDistinctTitulaireFlux());
        model.addAttribute("listCcp",           operationRepository.findDistinctCcp());
        model.addAttribute("listFamille",       operationRepository.findDistinctFamille());
        model.addAttribute("listDesignation",   operationRepository.findDistinctDesignation());
        model.addAttribute("listEtat",          operationRepository.findDistinctEtat());

        // Données principales
        model.addAttribute("caisses",           caisseService.findAll());
        model.addAttribute("operations",        operations);
        model.addAttribute("champsDynamiques",  operationService.listerChamps());
        model.addAttribute("valeursDynMap",     valeursDynMap);
        model.addAttribute("filtreActif",       filtreActif);

        // Repasser les valeurs sélectionnées pour les ré-afficher dans le modal
        model.addAttribute("f_natureFlux",      natureFlux);
        model.addAttribute("f_dateDebut",       dateDebut);
        model.addAttribute("f_dateFin",         dateFin);
        model.addAttribute("f_caisse",          caisse);
        model.addAttribute("f_modeFlux",        modeFlux);
        model.addAttribute("f_titulaireFlux",   titulaireFlux);
        model.addAttribute("f_ccp",             ccp);
        model.addAttribute("f_famille",         famille);
        model.addAttribute("f_designation",     designation);
        model.addAttribute("f_etat",            etat);

        return "flux-tresorerie";
    }

    // Convertit une chaîne vide en null
    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}