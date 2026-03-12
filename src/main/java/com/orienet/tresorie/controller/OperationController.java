package com.orienet.tresorie.controller;

import com.orienet.tresorie.model.Caisse;
import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.service.CaisseService;
import com.orienet.tresorie.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;
    private final CaisseService    caisseService;

    // ──────────────────────────────────────────────────────────────
    // PAGE AJOUT : GET /operations/nouvelle
    // → templates/nouvelle-operation.html
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/nouvelle")
    public String nouvelleOperationForm(Model model) {
        model.addAttribute("operation", new Operation());
        model.addAttribute("caisses", caisseService.findAll());
        model.addAttribute("mode", "ajout");   // pour différencier dans le HTML
        return "nouvelle-operation";
    }

    // ──────────────────────────────────────────────────────────────
    // SAUVEGARDER AJOUT : POST /operations/sauvegarder
    // ──────────────────────────────────────────────────────────────
    @PostMapping("/sauvegarder")
    public String sauvegarder(
            @ModelAttribute("operation") Operation operation,
            RedirectAttributes redirectAttributes) {
        try {
            operationService.sauvegarder(operation);
            redirectAttributes.addFlashAttribute("succes",
                    "✅ Opération enregistrée avec succès !");
            return "redirect:/tresorerie";

        } catch (RuntimeException e) {
            // Renvoyer vers la page d'ajout avec le message d'erreur
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/operations/nouvelle";
        }
    }

    // ──────────────────────────────────────────────────────────────
    // PAGE MODIFICATION : GET /operations/modifier/{id}
    // → templates/modifier-operation.html  (page isolée)
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/modifier/{id}")
    public String modifierForm(@PathVariable Long id, Model model) {
        Operation operation = operationService.findById(id)
                .orElseThrow(() -> new RuntimeException("Opération introuvable : " + id));

        model.addAttribute("operation", operation);
        model.addAttribute("caisses", caisseService.findAll());
        model.addAttribute("mode", "modification");
        return "modifier-operation";   // page séparée
    }

    // ──────────────────────────────────────────────────────────────
    // SAUVEGARDER MODIFICATION : POST /operations/modifier/{id}
    // ──────────────────────────────────────────────────────────────
    @PostMapping("/modifier/{id}")
    public String sauvegarderModification(
            @PathVariable Long id,
            @ModelAttribute("operation") Operation operation,
            RedirectAttributes redirectAttributes) {
        try {
            operationService.modifier(id, operation);
            redirectAttributes.addFlashAttribute("succes",
                    "✅ Opération modifiée avec succès !");
            return "redirect:/tresorerie";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/operations/modifier/" + id;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // SUPPRIMER : GET /operations/supprimer/{id}
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/supprimer/{id}")
    public String supprimer(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            operationService.supprimer(id);
            redirectAttributes.addFlashAttribute("succes",
                    "✅ Opération supprimée.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/flux-tresorerie";
    }
}