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
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;
    private final CaisseService    caisseService;

    // ─── Nouvelle opération ───────────────────────────────────────
    @GetMapping("/operations/nouvelle")
    public String nouvelle(Model model) {
        model.addAttribute("operation",        new Operation());
        model.addAttribute("caisses",          caisseService.findAll());
        model.addAttribute("champsDynamiques", operationService.listerChamps());
        return "nouvelle-operation";
    }

    @PostMapping("/operations/sauvegarder")
    public String sauvegarder(@ModelAttribute Operation operation,
                              HttpServletRequest request,
                              RedirectAttributes ra) {
        try {
            operationService.sauvegarder(operation, extraireChampsDynamiques(request));
            ra.addFlashAttribute("succes", "✅ Opération ajoutée !");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/operations/nouvelle";
        }
        return "redirect:/flux-tresorerie";
    }

    // ─── Modifier ─────────────────────────────────────────────────
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

    @PostMapping("/operations/modifier/{id}")
    public String modifierSave(@PathVariable Long id,
                               @ModelAttribute Operation operation,
                               HttpServletRequest request,
                               RedirectAttributes ra) {
        try {
            operationService.modifier(id, operation, extraireChampsDynamiques(request));
            ra.addFlashAttribute("succes", "✅ Opération modifiée !");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/operations/modifier/" + id;
        }
        return "redirect:/flux-tresorerie";
    }

    // ─── Archiver (au lieu de supprimer) ─────────────────────────
    // L'opération est masquée du tableau principal et n'impacte plus la caisse
    @GetMapping("/operations/archiver/{id}")
    public String archiver(@PathVariable Long id, RedirectAttributes ra) {
        try {
            operationService.archiver(id);
            ra.addFlashAttribute("succes", "📦 Opération archivée.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/flux-tresorerie";
    }

    // ─── Page Archives ────────────────────────────────────────────
    @GetMapping("/archives")
    public String archives(Model model) {
        List<Operation> archivees = operationService.findArchivees();

        Map<Long, Map<String, String>> valeursDynMap = new HashMap<>();
        for (Operation op : archivees) {
            valeursDynMap.put(op.getId(), operationService.jsonToMap(op.getValeursDynamiques()));
        }

        model.addAttribute("operations",       archivees);
        model.addAttribute("champsDynamiques", operationService.listerChamps());
        model.addAttribute("valeursDynMap",    valeursDynMap);
        return "archives";
    }

    // ─── Restaurer depuis les archives ───────────────────────────
    @GetMapping("/archives/restaurer/{id}")
    public String restaurer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            operationService.restaurer(id);
            ra.addFlashAttribute("succes", "✅ Opération restaurée dans le tableau principal.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/archives";
    }

    // ─── Supprimer définitivement (depuis archives seulement) ────
    @GetMapping("/archives/supprimer/{id}")
    public String supprimerDefinitivement(@PathVariable Long id, RedirectAttributes ra) {
        try {
            operationService.supprimerDefinitivement(id);
            ra.addFlashAttribute("succes", "🗑️ Opération supprimée définitivement.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/archives";
    }

    // ─── Gestion champs dynamiques ────────────────────────────────
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

    // ─── Utilitaire : extraire params champ_* ────────────────────
    private Map<String, String> extraireChampsDynamiques(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("champ_") && values.length > 0) {
                String nom    = key.substring(6);
                String valeur = values[0];
                if (valeur != null && !valeur.isBlank())
                    result.put(nom, valeur.trim());
            }
        });
        return result;
    }
}