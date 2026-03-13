package com.orienet.tresorie.controller;

import com.orienet.tresorie.model.Operation;
import com.orienet.tresorie.service.CaisseService;
import com.orienet.tresorie.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CaisseController {

    private final CaisseService    caisseService;
    private final OperationService operationService;

    @GetMapping("/flux-tresorerie")
    public String fluxTresorerie(Model model) {
        List<Operation> operations = operationService.findAll();

        // Pour chaque opération, parser le JSON → Map<nomChamp, valeur>
        // Thymeleaf utilisera : valeursDynMap[op.id]['Référence']
        Map<Long, Map<String, String>> valeursDynMap = new HashMap<>();
        for (Operation op : operations) {
            valeursDynMap.put(op.getId(), operationService.jsonToMap(op.getValeursDynamiques()));
        }

        model.addAttribute("caisses",          caisseService.findAll());
        model.addAttribute("operations",       operations);
        model.addAttribute("champsDynamiques", operationService.listerChamps());
        model.addAttribute("valeursDynMap",    valeursDynMap);
        return "flux-tresorerie";
    }
}