package com.orienet.tresorie.service;

import com.orienet.tresorie.model.Utilisateur;
import com.orienet.tresorie.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

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

    public void supprimer(Long id) {
        utilisateurRepository.deleteById(id);
    }
}
