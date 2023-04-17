/*
 *   gcloudlicensing - LoginController.java
 *
 *   Copyright (c) 2022-2023, Slinky Software
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   A copy of the GNU Affero General Public License is located in the 
 *   AGPL-3.0.md supplied with the source code.
 *
 */
package com.slinkytoybox.gcloud.licensing.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@Controller
@RequestMapping("/auth")
public class LoginController {

//    @Autowired
//    private ModelDefaults modelDefaults;
//    
    @Autowired
    private Environment env;

    @RequestMapping("/login")
    public String login(Model model, WebRequest request) {
        final String logPrefix = "authLogin() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing /auth/login request", logPrefix);
        String registrationId = env.getProperty("auth.saml.registration-id", "none");
        return "redirect:/saml2/authenticate/" + registrationId;

// TODO: Clean up this code as we dont present a login screen anymore
//        final String pageTitle = "Login";
//        if (request == null || request.getParameter("error") == null || request.getParameter("error").isEmpty()) {
//            model.addAttribute("showError", 0);
//        }
//        else {
//            model.addAttribute("showError", 1);
//            model.addAttribute("errorText", "Your login was unsuccessful. Please try again");
//        }
//
//        if (request != null && request.getParameter("logout") != null) {
//            model.addAttribute("showSuccess", 1);
//            model.addAttribute("successText", "You have been successfully logged out.");
//
//        }
//        String authType = env.getProperty("auth.saml.provider", "none");
//
//        model.addAttribute("authlogo", "/assets/images/" + authType + "-logo.png");
//        model.addAttribute("authurl", "/saml2/authenticate/" + registrationId);
//        modelDefaults.updateModelDefaults(model, pageTitle, "none", "Login", "/auth/login");
//        return "auth/login";
    }

    @RequestMapping("/logout")
    public String logout(Model models, HttpServletRequest request) throws ServletException {
        request.logout();
        return "redirect:/";
    }
}
