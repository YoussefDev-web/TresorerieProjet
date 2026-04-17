package com.orienet.tresorie.service;

import com.orienet.tresorie.model.Caisse;
import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.repository.CaisseRepository;
import com.orienet.tresorie.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CopiloteIAService {

    private final CaisseRepository    caisseRepository;
    private final OperationRepository operationRepository;

    // Clé API Gemini — À récupérer sur https://aistudio.google.com/
// Remplace par TA clé générée
    private static final String GEMINI_API_KEY = "AIzaSyCf1J1jSt63b5yX6zuqgvB5Wg-lL64_Hn8";
    // Utilisation du modèle Flash qui est gratuit et rapide
    // On utilise la version v1 (stable) et le nom de modèle standard
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + GEMINI_API_KEY;

    // ─────────────────────────────────────────────────────────────
    // Méthode principale : reçoit la question, construit le contexte,
    // appelle Claude et retourne la réponse
    // ─────────────────────────────────────────────────────────────
    public String poserQuestion(String question, List<Map<String, String>> historique) {
        String contexte = construireContexte();
        String prompt   = construirePrompt(contexte, question);
        return appellerClaude(prompt, historique);
    }

    // ─────────────────────────────────────────────────────────────
    // Construire le contexte complet depuis la base de données
    // ─────────────────────────────────────────────────────────────
    private String construireContexte() {
        StringBuilder ctx = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate maintenant = LocalDate.now();
        LocalDate debutMois  = maintenant.withDayOfMonth(1);
        LocalDate debutSemaine = maintenant.minusDays(maintenant.getDayOfWeek().getValue() - 1);

        // ── 1. Situation des caisses ──────────────────────────────
        ctx.append("=== SITUATION DES CAISSES ===\n");
        List<Caisse> caisses = caisseRepository.findAll();
        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalEnc  = BigDecimal.ZERO;
        BigDecimal totalDec  = BigDecimal.ZERO;

        for (Caisse c : caisses) {
            BigDecimal cash = orZero(c.getCashDisponible());
            BigDecimal enc  = orZero(c.getEncaissement());
            BigDecimal dec  = orZero(c.getDecaissement());
            totalCash = totalCash.add(cash);
            totalEnc  = totalEnc.add(enc);
            totalDec  = totalDec.add(dec);
            ctx.append(String.format("Caisse '%s': Cash=%.2f dh | Encaissement=%.2f dh | Décaissement=%.2f dh | Créance=%.2f dh | Dette=%.2f dh\n",
                    c.getNom(), cash, enc, dec, orZero(c.getCreance()), orZero(c.getDette())));
        }
        ctx.append(String.format("TOTAL GLOBAL: Cash=%.2f dh | Encaissements=%.2f dh | Décaissements=%.2f dh\n\n", totalCash, totalEnc, totalDec));

        // ── 2. Opérations du mois en cours ───────────────────────
        ctx.append("=== OPÉRATIONS DU MOIS EN COURS (").append(debutMois.format(fmt)).append(" → ").append(maintenant.format(fmt)).append(") ===\n");
        List<Operation> opsMois = operationRepository.findByDateFluxBetweenAndArchiveeFalse(debutMois, maintenant);
        ctx.append("Nombre d'opérations ce mois: ").append(opsMois.size()).append("\n");

        Map<String, BigDecimal> encParCaisse = new HashMap<>();
        Map<String, BigDecimal> decParCaisse = new HashMap<>();
        long enAttente = 0; long annulees = 0; long validees = 0;

        for (Operation op : opsMois) {
            if ("Encaissement".equals(op.getNatureFlux()) && !"Annulé".equals(op.getEtat()))
                encParCaisse.merge(orStr(op.getCaisse()), orZero(op.getMontant()), BigDecimal::add);
            if ("Décaissement".equals(op.getNatureFlux()) && !"Annulé".equals(op.getEtat()))
                decParCaisse.merge(orStr(op.getCaisse()), orZero(op.getMontant()), BigDecimal::add);
            if ("En attente".equals(op.getEtat())) enAttente++;
            if ("Annulé".equals(op.getEtat()))     annulees++;
            if ("Validé".equals(op.getEtat()))     validees++;
        }
        ctx.append(String.format("Validées: %d | En attente: %d | Annulées: %d\n", validees, enAttente, annulees));
        encParCaisse.forEach((c, v) -> ctx.append(String.format("  Encaissements %s ce mois: %.2f dh\n", c, v)));
        decParCaisse.forEach((c, v) -> ctx.append(String.format("  Décaissements %s ce mois: %.2f dh\n", c, v)));

        // ── 3. Opérations de la semaine ───────────────────────────
        ctx.append("\n=== OPÉRATIONS DE LA SEMAINE (").append(debutSemaine.format(fmt)).append(" → ").append(maintenant.format(fmt)).append(") ===\n");
        List<Operation> opsSemaine = operationRepository.findByDateFluxBetweenAndArchiveeFalse(debutSemaine, maintenant);
        BigDecimal encSemaine = opsSemaine.stream()
                .filter(o -> "Encaissement".equals(o.getNatureFlux()) && !"Annulé".equals(o.getEtat()))
                .map(o -> orZero(o.getMontant())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal decSemaine = opsSemaine.stream()
                .filter(o -> "Décaissement".equals(o.getNatureFlux()) && !"Annulé".equals(o.getEtat()))
                .map(o -> orZero(o.getMontant())).reduce(BigDecimal.ZERO, BigDecimal::add);
        ctx.append(String.format("Encaissements semaine: %.2f dh | Décaissements semaine: %.2f dh\n", encSemaine, decSemaine));
        ctx.append(String.format("Variation cash semaine: %.2f dh\n\n", encSemaine.subtract(decSemaine)));

        // ── 4. Top 5 opérations récentes ─────────────────────────
        ctx.append("=== 5 DERNIÈRES OPÉRATIONS ===\n");
        operationRepository.findByArchiveeFalse().stream()
                .sorted(Comparator.comparing(Operation::getDateFlux, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .forEach(op -> ctx.append(String.format(
                        "- [%s] %s | %s | %.2f dh | Caisse: %s | Etat: %s\n",
                        op.getDateFlux() != null ? op.getDateFlux().format(fmt) : "?",
                        orStr(op.getNatureFlux()), orStr(op.getTitulaireFlux()),
                        orZero(op.getMontant()), orStr(op.getCaisse()), orStr(op.getEtat()))));

        // ── 5. Anomalies potentielles ─────────────────────────────
        ctx.append("\n=== ANOMALIES POTENTIELLES ===\n");
        List<Operation> toutesOps = operationRepository.findByArchiveeFalse();

        // Opérations en attente depuis plus de 7 jours
        long vieilles = toutesOps.stream()
                .filter(o -> "En attente".equals(o.getEtat()) && o.getDateFlux() != null
                        && o.getDateFlux().isBefore(maintenant.minusDays(7)))
                .count();
        if (vieilles > 0)
            ctx.append(String.format("⚠️ %d opération(s) en attente depuis plus de 7 jours\n", vieilles));

        // Gros montants (> 50 000 dh)
        long grosMontants = toutesOps.stream()
                .filter(o -> o.getMontant() != null && o.getMontant().compareTo(new BigDecimal("50000")) > 0
                        && !"Annulé".equals(o.getEtat()))
                .count();
        if (grosMontants > 0)
            ctx.append(String.format("⚠️ %d opération(s) avec montant > 50 000 dh\n", grosMontants));

        // Doublons potentiels (même montant + même titulaire + même date)
        Map<String, Long> doublons = toutesOps.stream()
                .filter(o -> o.getMontant() != null && o.getTitulaireFlux() != null && o.getDateFlux() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getMontant() + "|" + o.getTitulaireFlux() + "|" + o.getDateFlux(),
                        Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!doublons.isEmpty())
            ctx.append(String.format("⚠️ %d doublon(s) potentiel(s) détecté(s)\n", doublons.size()));

        if (vieilles == 0 && grosMontants == 0 && doublons.isEmpty())
            ctx.append("✅ Aucune anomalie détectée\n");

        // ── 6. Date du jour ───────────────────────────────────────
        ctx.append("\n=== DATE ACTUELLE ===\n");
        ctx.append("Aujourd'hui : ").append(maintenant.format(fmt)).append("\n");

        return ctx.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // Construire le prompt système envoyé à Claude
    // ─────────────────────────────────────────────────────────────
    private String construirePrompt(String contexte, String question) {
        return "Tu es le Copilote IA de trésorerie de l'entreprise Orienet/Bestmobile. " +
                "Tu es un expert financier qui analyse les données de trésorerie et répond de façon claire et professionnelle. " +
                "Tu réponds dans la même langue que la question (français, arabe ou anglais). " +
                "Tu utilises les données réelles fournies pour répondre avec précision. " +
                "Tu formules tes réponses de façon concise avec des chiffres exacts. " +
                "Si tu détectes des problèmes, tu les signales clairement. " +
                "Tu peux utiliser des emojis pour rendre les réponses plus lisibles. " +
                "Ne jamais inventer de chiffres — utilise uniquement les données fournies.\n\n" +
                "DONNÉES RÉELLES DE LA TRÉSORERIE :\n" + contexte + "\n\n" +
                "QUESTION : " + question;
    }

    // ─────────────────────────────────────────────────────────────
    // Appel HTTP à l'API Claude
    // ─────────────────────────────────────────────────────────────
    private String appellerClaude(String prompt, List<Map<String, String>> historique) {
        try {
            // Construction du JSON pour Gemini
            // Note : On envoie le prompt complet qui contient déjà le contexte
            String body = "{" +
                    "\"contents\": [{" +
                    "\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]" +
                    "}]" +
                    "}";

            URL url = new URL(GEMINI_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) response.append(line);
            }

            return extraireTexteReponse(response.toString());

        } catch (Exception e) {
            return "❌ Erreur lors de la connexion à Gemini : " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Extraire le texte de la réponse JSON Claude
    // Exemple : {"content":[{"type":"text","text":"..."}]}
    // ─────────────────────────────────────────────────────────────
    private String extraireTexteReponse(String json) {
        try {
            // On cherche le champ "text" spécifique à la réponse de Gemini
            String marker = "\"text\": \"";
            int start = json.indexOf(marker);

            if (start == -1) {
                // Gestion des erreurs renvoyées par Google
                if (json.contains("\"error\"")) {
                    return "❌ Erreur Google API : " + json;
                }
                return "❌ Réponse inattendue de Gemini.";
            }

            int textStart = start + marker.length();
            int textEnd = textStart;

            // On cherche la fin de la chaîne en ignorant les guillemets échappés \"
            while (textEnd < json.length()) {
                char c = json.charAt(textEnd);
                if (c == '"' && json.charAt(textEnd - 1) != '\\') break;
                textEnd++;
            }

            String result = json.substring(textStart, textEnd);
            return unescapeJson(result);

        } catch (Exception e) {
            return "❌ Erreur lors du traitement du JSON.";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────────────────────────
    private BigDecimal orZero(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private String orStr(String s)          { return s != null ? s : ""; }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        return s.replace("\\n", "\n").replace("\\r", "\r")
                .replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
