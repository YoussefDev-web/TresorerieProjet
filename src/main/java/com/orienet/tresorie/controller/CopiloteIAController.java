package com.orienet.tresorie.controller;

import com.orienet.tresorie.service.CopiloteIAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CopiloteIAController {

    private final CopiloteIAService copiloteIAService;

    // ── Page du copilote ──────────────────────────────────────────
    @GetMapping("/copilote")
    public String copilotePage() {
        return "copilote";
    }

    // ── Endpoint REST : reçoit la question + historique → retourne la réponse ──
    @PostMapping("/copilote/chat")
    @ResponseBody
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, Object> payload) {
        try {
            String question = (String) payload.get("question");

            // Récupérer l'historique de conversation
            @SuppressWarnings("unchecked")
            List<Map<String, String>> historique = (List<Map<String, String>>) payload.getOrDefault("historique", new ArrayList<>());

            if (question == null || question.isBlank()) {
                Map<String, String> err = new HashMap<>();
                err.put("erreur", "La question ne peut pas être vide.");
                return ResponseEntity.badRequest().body(err);
            }

            String reponse = copiloteIAService.poserQuestion(question.trim(), historique);

            Map<String, String> result = new HashMap<>();
            result.put("reponse", reponse);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("erreur", "Erreur serveur : " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }
}