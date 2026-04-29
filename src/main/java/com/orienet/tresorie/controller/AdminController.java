package com.orienet.tresorie.controller;

import com.orienet.tresorie.model.Utilisateur;
import com.orienet.tresorie.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UtilisateurService utilisateurService;

    // ─── Liste des utilisateurs ────────────────────────────────────
    @GetMapping("/utilisateurs")
    public String listeUtilisateurs(Model model) {
        model.addAttribute("utilisateurs", utilisateurService.listerTous());
        return "admin/utilisateurs";
    }

    // ─── Formulaire nouvel utilisateur ─────────────────────────────
    @GetMapping("/utilisateurs/nouveau")
    public String nouveauUtilisateur(Model model) {
        model.addAttribute("utilisateur", new Utilisateur());
        return "admin/nouvel-utilisateur";
    }

    // ─── Sauvegarder nouvel utilisateur ───────────────────────────
    @PostMapping("/utilisateurs/sauvegarder")
    public String sauvegarder(@ModelAttribute Utilisateur utilisateur,
                              RedirectAttributes ra) {
        try {
            utilisateurService.creerUtilisateur(utilisateur);
            ra.addFlashAttribute("succes", "✅ Utilisateur \"" + utilisateur.getUsername() + "\" créé avec succès.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/admin/utilisateurs/nouveau";
        }
        return "redirect:/admin/utilisateurs";
    }

    // ─── Supprimer un utilisateur ──────────────────────────────────
    @PostMapping("/utilisateurs/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            utilisateurService.supprimer(id);
            ra.addFlashAttribute("succes", "✅ Utilisateur supprimé.");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", "Erreur lors de la suppression : " + e.getMessage());
        }
        return "redirect:/admin/utilisateurs";
    }
}
