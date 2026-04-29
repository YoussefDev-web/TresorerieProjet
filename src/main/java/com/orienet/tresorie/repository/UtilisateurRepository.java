package com.orienet.tresorie.repository;

import com.orienet.tresorie.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByUsername(String username);

    boolean existsByRole(String role);
}
