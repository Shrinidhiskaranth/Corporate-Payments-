package com.nmamit.payments.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Sends the root URL to the JSF home page. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/index.xhtml";
    }
}
