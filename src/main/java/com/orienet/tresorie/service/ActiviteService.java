package com.orienet.tresorie.service;

import com.orienet.tresorie.model.Activite;
import com.orienet.tresorie.repository.ActiviteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActiviteService {

    private final ActiviteRepository activiteRepository;

    public void logAction(String utilisateur, String action, String details) {
        Activite activite = Activite.builder()
                .utilisateur(utilisateur != null ? utilisateur : "inconnu")
                .action(action)
                .details(details)
                .dateAction(LocalDateTime.now())
                .build();
        activiteRepository.save(activite);
    }

    public List<Activite> getHistorique() {
        return activiteRepository.findAllByOrderByDateActionDesc();
    }
}
