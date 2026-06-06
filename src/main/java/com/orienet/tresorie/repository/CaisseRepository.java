package com.orienet.tresorie.repository;

import com.orienet.tresorie.model.Caisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaisseRepository extends JpaRepository<Caisse, Integer> {

    Optional<Caisse> findByNom(String nom);

    // Resynchronise le compteur auto-increment H2 avec le max ID existant
    @Modifying
    @Query(value = "ALTER TABLE caisse ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM caisse)", nativeQuery = true)
    void resetAutoIncrement();
}