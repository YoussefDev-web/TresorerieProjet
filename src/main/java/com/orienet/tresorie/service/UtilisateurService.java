package com.orienet.tresorie.service;

import com.orienet.tresorie.model.Utilisateur;
import com.orienet.tresorie.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crée un compte admin par défaut au premier démarrage.
     * Identifiant : admin / Mot de passe : admin123
     */
    @PostConstruct
    public void initAdminParDefaut() {
        if (!utilisateurRepository.existsByRole("ROLE_ADMIN")) {
            Utilisateur admin = new Utilisateur();
            admin.setNom("Administrateur");
            admin.setPrenom("Système");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ROLE_ADMIN");
            admin.setActif(true);
            utilisateurRepository.save(admin);
            System.out.println("✅ Compte admin créé : admin / admin123");
        }
    }

    public Optional<Utilisateur> findById(Long id) {
        return utilisateurRepository.findById(id);
    }

    public List<Utilisateur> listerTous() {
        return utilisateurRepository.findAll();
    }

    public void creerUtilisateur(Utilisateur utilisateur) {
        if (utilisateurRepository.findByUsername(utilisateur.getUsername()).isPresent()) {
            throw new RuntimeException("L'identifiant \"" + utilisateur.getUsername() + "\" est déjà utilisé.");
        }
        utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);
    }

    /**
     * Modifie les informations d'un utilisateur existant.
     * Le mot de passe n'est ré-encodé que s'il est renseigné (non vide).
     */
    public void modifierUtilisateur(Long id, String nom, String prenom, String username,
                                     String password, String role, boolean actif) {
        Utilisateur existant = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // Vérifier l'unicité du username si changé
        if (!existant.getUsername().equals(username)) {
            if (utilisateurRepository.findByUsername(username).isPresent()) {
                throw new RuntimeException("L'identifiant \"" + username + "\" est déjà utilisé par un autre compte.");
            }
        }

        existant.setNom(nom);
        existant.setPrenom(prenom);
        existant.setUsername(username);
        existant.setRole(role);
        existant.setActif(actif);

        // Ne changer le mot de passe que si un nouveau est fourni
        if (password != null && !password.trim().isEmpty()) {
            existant.setPassword(passwordEncoder.encode(password));
        }

        utilisateurRepository.save(existant);
    }

    public void supprimer(Long id) {
        utilisateurRepository.deleteById(id);
    }
}
