package com.orienet.tresorie.repository;

import com.orienet.tresorie.model.Caisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaisseRepository extends JpaRepository<Caisse, Integer> {

    Optional<Caisse> findByNom(String nom);
}