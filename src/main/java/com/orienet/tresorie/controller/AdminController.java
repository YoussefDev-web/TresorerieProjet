package com.orienet.tresorie.controller;

import com.orienet.tresorie.model.Caisse;
import com.orienet.tresorie.model.Utilisateur;
import com.orienet.tresorie.service.CaisseService;
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
    private final CaisseService      caisseService;

    // ═══════════════════════════════════════════════════════════════
    //  GESTION DES UTILISATEURS
    // ═══════════════════════════════════════════════════════════════

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

    // ─── Formulaire modifier un utilisateur ───────────────────────
    @GetMapping("/utilisateurs/modifier/{id}")
    public String modifierUtilisateurForm(@PathVariable Long id, Model model,
                                          RedirectAttributes ra) {
        var utilisateur = utilisateurService.findById(id);
        if (utilisateur.isEmpty()) {
            ra.addFlashAttribute("erreur", "Utilisateur introuvable.");
            return "redirect:/admin/utilisateurs";
        }
        model.addAttribute("utilisateur", utilisateur.get());
        return "admin/modifier-utilisateur";
    }

    // ─── Sauvegarder modifications utilisateur ────────────────────
    @PostMapping("/utilisateurs/modifier/{id}")
    public String modifierUtilisateur(@PathVariable Long id,
                                      @RequestParam String nom,
                                      @RequestParam String prenom,
                                      @RequestParam String username,
                                      @RequestParam(required = false) String password,
                                      @RequestParam String role,
                                      @RequestParam(required = false, defaultValue = "false") boolean actif,
                                      RedirectAttributes ra) {
        try {
            utilisateurService.modifierUtilisateur(id, nom, prenom, username, password, role, actif);
            ra.addFlashAttribute("succes", "✅ Utilisateur \"" + username + "\" modifié avec succès.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/admin/utilisateurs/modifier/" + id;
        }
        return "redirect:/admin/utilisateurs";
    }

    // ═══════════════════════════════════════════════════════════════
    //  GESTION DES CAISSES
    // ═══════════════════════════════════════════════════════════════

    // ─── Liste des caisses ─────────────────────────────────────────
    @GetMapping("/caisses")
    public String listeCaisses(Model model) {
        model.addAttribute("caisses", caisseService.findAll());
        return "admin/caisses";
    }

    // ─── Formulaire nouvelle caisse ────────────────────────────────
    @GetMapping("/caisses/nouvelle")
    public String nouvelleCaisse(Model model) {
        model.addAttribute("caisse", new Caisse());
        return "admin/nouvelle-caisse";
    }

    // ─── Sauvegarder nouvelle caisse ──────────────────────────────
    @PostMapping("/caisses/sauvegarder")
    public String sauvegarderCaisse(@RequestParam String nom,
                                    RedirectAttributes ra) {
        try {
            String nomTrim = nom.trim();
            if (nomTrim.isEmpty()) {
                throw new RuntimeException("Le nom de la caisse ne peut pas être vide.");
            }
            // Vérifier si une caisse avec ce nom existe déjà
            if (caisseService.findByNom(nomTrim).isPresent()) {
                throw new RuntimeException("Une caisse portant le nom \"" + nomTrim + "\" existe déjà.");
            }
            Caisse caisse = new Caisse();
            caisse.setNom(nomTrim);
            caisse.setEncaissement(java.math.BigDecimal.ZERO);
            caisse.setDecaissement(java.math.BigDecimal.ZERO);
            caisse.setCreance(java.math.BigDecimal.ZERO);
            caisse.setDette(java.math.BigDecimal.ZERO);
            caisse.setCashDisponible(java.math.BigDecimal.ZERO);
            caisseService.sauvegarder(caisse);
            ra.addFlashAttribute("succes", "✅ Caisse \"" + nomTrim + "\" créée avec succès.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/admin/caisses/nouvelle";
        }
        return "redirect:/admin/caisses";
    }

    // ─── Supprimer une caisse ─────────────────────────────────────
    @PostMapping("/caisses/supprimer/{id}")
    public String supprimerCaisse(@PathVariable int id, RedirectAttributes ra) {
        try {
            Caisse caisse = caisseService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Caisse introuvable."));
            if ("caisse centrale".equalsIgnoreCase(caisse.getNom())) {
                throw new RuntimeException("Impossible de supprimer la Caisse Centrale.");
            }
            caisseService.supprimer(id);
            ra.addFlashAttribute("succes", "✅ Caisse \"" + caisse.getNom() + "\" supprimée.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/caisses";
    }
}
