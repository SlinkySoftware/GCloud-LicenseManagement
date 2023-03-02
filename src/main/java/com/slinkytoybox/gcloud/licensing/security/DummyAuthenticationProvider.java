/*
 *   gcloudlicensing - DummyAuthenticationProvider.java
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

package com.slinkytoybox.gcloud.licensing.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.*;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
public class DummyAuthenticationProvider implements AuthenticationProvider {

    public DummyAuthenticationProvider() {
        super();
    }

    @Override
    public Authentication authenticate(final Authentication authentication) {
        final String logPrefix = "authenticate() - ";
        log.trace("{}Entering method", logPrefix);

        final String name = authentication.getName();
        final String password = authentication.getCredentials().toString();
        log.info("{}Authenticating user {}", logPrefix, name);
        
        log.info("{}User authenticated successfully", logPrefix);

        log.debug("{}Determining granted authorities", logPrefix);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_DUMMY"));

        log.debug("{}Creating authentication token", logPrefix);
        UserDetails principal = new User(name, password, authorities);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, password, authorities);
        log.debug("{}Returning token: {}", logPrefix, auth);
        return auth;

    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(UsernamePasswordAuthenticationToken.class);
    }

}
