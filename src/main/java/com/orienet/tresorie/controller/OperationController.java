package com.orienet.tresorie.controller;

import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.service.CaisseService;
import com.orienet.tresorie.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;
    private final CaisseService    caisseService;

    // ── Nouvelle opération ────────────────────────────────────────
    @GetMapping("/operations/nouvelle")
    public String nouvelle(Model model) {
        model.addAttribute("operation",        new Operation());
        model.addAttribute("caisses",          caisseService.findAll());
        model.addAttribute("champsDynamiques", operationService.listerChamps());
        return "nouvelle-operation";
    }

    // ── Sauvegarder ───────────────────────────────────────────────
    // On utilise HttpServletRequest pour lire TOUS les paramètres
    // y compris les champ_* qui ne font pas partie du modèle Operation
    @PostMapping("/operations/sauvegarder")
    public String sauvegarder(@ModelAttribute Operation operation,
                              HttpServletRequest request,
                              RedirectAttributes ra) {
        try {
            Map<String, String> valeursDyn = extraireChampsDynamiques(request);
            operationService.sauvegarder(operation, valeursDyn);
            ra.addFlashAttribute("succes", "✅ Opération ajoutée !");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/operations/nouvelle";
        }
        return "redirect:/flux-tresorerie";
    }

    // ── Modifier (GET) ────────────────────────────────────────────
    @GetMapping("/operations/modifier/{id}")
    public String modifier(@PathVariable Long id, Model model) {
        Operation op = operationService.findById(id)
                .orElseThrow(() -> new RuntimeException("Opération introuvable : " + id));
        model.addAttribute("operation",         op);
        model.addAttribute("caisses",           caisseService.findAll());
        model.addAttribute("champsDynamiques",  operationService.listerChamps());
        model.addAttribute("valeursDynamiques", operationService.getValeursDynamiques(op));
        return "modifier-operation";
    }

    // ── Modifier (POST) ───────────────────────────────────────────
    @PostMapping("/operations/modifier/{id}")
    public String modifierSave(@PathVariable Long id,
                               @ModelAttribute Operation operation,
                               HttpServletRequest request,
                               RedirectAttributes ra) {
        try {
            Map<String, String> valeursDyn = extraireChampsDynamiques(request);
            operationService.modifier(id, operation, valeursDyn);
            ra.addFlashAttribute("succes", "✅ Opération modifiée !");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/operations/modifier/" + id;
        }
        return "redirect:/flux-tresorerie";
    }

    // ── Supprimer ─────────────────────────────────────────────────
    @GetMapping("/operations/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            operationService.supprimer(id);
            ra.addFlashAttribute("succes", "✅ Opération supprimée.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/flux-tresorerie";
    }

    // ── Gestion des champs dynamiques ─────────────────────────────
    @GetMapping("/champs")
    public String champsPage(Model model) {
        model.addAttribute("champsDynamiques", operationService.listerChamps());
        return "gestion-champs";
    }

    @PostMapping("/champs/ajouter")
    public String ajouterChamp(@RequestParam String nomChamp, RedirectAttributes ra) {
        try {
            operationService.ajouterChamp(nomChamp);
            ra.addFlashAttribute("succes", "✅ Champ \"" + nomChamp.trim() + "\" ajouté !");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/champs";
    }

    @PostMapping("/champs/supprimer/{id}")
    public String supprimerChamp(@PathVariable Long id, RedirectAttributes ra) {
        try {
            operationService.supprimerChamp(id);
            ra.addFlashAttribute("succes", "✅ Champ supprimé.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/champs";
    }

    // ─────────────────────────────────────────────────────────────
    // Lit depuis HttpServletRequest tous les params dont le nom
    // commence par "champ_"
    // Ex : champ_Référence=REF-001  →  {"Référence":"REF-001"}
    // Utiliser HttpServletRequest garantit qu'on récupère TOUS les
    // paramètres même quand @ModelAttribute est présent
    // ─────────────────────────────────────────────────────────────
    private Map<String, String> extraireChampsDynamiques(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("champ_") && values.length > 0) {
                String nomChamp = key.substring(6); // supprimer "champ_"
                String valeur   = values[0];
                if (valeur != null && !valeur.isBlank()) {
                    result.put(nomChamp, valeur.trim());
                }
            }
        });
        return result;
    }
}