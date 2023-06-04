/*
 *   gcloudlicensemanagement - MailContentBuilder.java
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
package com.slinkytoybox.gcloud.licensing.businesslogic;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Service
@AllArgsConstructor
@Slf4j
public class MailContentBuilder {
    
    @Autowired
    private final TemplateEngine templateEngine;

    public String buildMessage(String template, Object templateObject) {
        final String logPrefix = "buildMessage() - ";
        log.trace("{}Entering method", logPrefix);
        Context context = new Context();
        context.setVariable("mailTemplate", templateObject);
        log.trace("{}Added templateObject as mailTemplate: {}", logPrefix, templateObject);
        log.debug("{}Processing alert message against template: {}", logPrefix, template);
        return templateEngine.process("email/" + template, context);
    }
}
