package com.orienet.tresorie.controller;

import com.orienet.tresorie.model.Caisse;
import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.service.CaisseService;
import com.orienet.tresorie.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CaisseController {

    private final CaisseService caisseService;
    private final OperationService operationService;

    // ─── Page principale : dashboard + liste des opérations ───────
    @GetMapping("/tresorerie")
    public String fluxTresorerie(Model model) {
        List<Caisse> caisses = caisseService.findAll();
        List<Operation> operations = operationService.findAll();

        model.addAttribute("caisses", caisses);
        model.addAttribute("operations", operations);

        return "flux-tresorerie"; // → templates/flux-tresorerie.html
    }
}