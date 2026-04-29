package com.orienet.tresorie.controller;

import com.orienet.tresorie.service.ActiviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ActiviteController {

    private final ActiviteService activiteService;

    @GetMapping("/admin/activites")
    public String activites(Model model) {
        model.addAttribute("activites", activiteService.getHistorique());
        return "activites";
    }
}
