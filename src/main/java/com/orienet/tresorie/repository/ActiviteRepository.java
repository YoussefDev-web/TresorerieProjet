package com.orienet.tresorie.repository;

import com.orienet.tresorie.model.Activite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActiviteRepository extends JpaRepository<Activite, Long> {
    List<Activite> findAllByOrderByDateActionDesc();
}
