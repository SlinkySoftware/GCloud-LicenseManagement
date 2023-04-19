/*
 *   gcloudlicensing - SAMLAuthenticationConfiguration.java
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

import java.io.File;
import java.security.cert.X509Certificate;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.security.x509.X509Support;
import org.springframework.boot.autoconfigure.session.DefaultCookieSerializerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j

public class SAMLAuthenticationConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SAMLLoginSettings samlSettings,  Environment env) throws Exception {
        final String logPrefix = "securityFilterChain(HttpSecurity) - ";
        log.trace("{}Entering method", logPrefix);
        http
                .saml2Login(samlSettings)
                .saml2Logout()
                .and()
                .authorizeHttpRequests()
                .requestMatchers("/assets/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/saml2/**").permitAll()
                .requestMatchers("/content/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                .requestMatchers("/version").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/login*").permitAll()
                .requestMatchers("/logout*").permitAll()
                .requestMatchers("/source/**").permitAll()
                .requestMatchers("/metrics/health**/**").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/**").authenticated()
                .and()
                .csrf().disable().headers().frameOptions().disable()
                .and()
                .sessionManagement()
                .invalidSessionUrl("/?error=is")
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .sessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy())
                .sessionAuthenticationErrorUrl("/?error=se")
                .and()
                .logout()
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutSuccessUrl("/?error=lo")
                .permitAll();
        log.trace("{}HTTP Security: {}", logPrefix, http);
        RelyingPartyRegistrationResolver relyingPartyRegistrationResolver = new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrations(env));
        log.trace("{}RelyingPartyRegistrationResolver: {}", logPrefix, relyingPartyRegistrationResolver);

        Saml2MetadataFilter filter = new Saml2MetadataFilter(relyingPartyRegistrationResolver, new OpenSamlMetadataResolver());
        log.trace("{}Saml2MetadataFilter: {}", logPrefix, filter);

        http.addFilterBefore(filter, Saml2WebSsoAuthenticationFilter.class);
        log.trace("{}HTTP Security after filter: {}", logPrefix, http);
        log.trace("{}Building Security Filter Chain", logPrefix);
        return http.build();
    }

    @Bean
    protected RelyingPartyRegistrationRepository relyingPartyRegistrations(Environment env) throws Exception {
        final String logPrefix = "relyingPartyRegistrations() - ";
        log.trace("{}Entering method", logPrefix);
        String ssoUrl = env.getProperty("auth.saml.sso.url");
        String entityId = env.getProperty("auth.saml.entity-id");
        String registrationId = env.getProperty("auth.saml.registration-id");
        String certificateFile = env.getProperty("auth.saml.certificate", "none");
        Boolean wantSignedRequests = Boolean.getBoolean(env.getProperty("auth.saml.signing", "false"));
        RelyingPartyRegistration registration;
        if (certificateFile.isEmpty() || certificateFile.equalsIgnoreCase("none")) {
            log.trace("{}Building configuration without certificate", logPrefix);
            registration = RelyingPartyRegistration
                    .withRegistrationId(registrationId)
                    .assertingPartyDetails(party -> party
                    .entityId(entityId)
                    .singleSignOnServiceLocation(ssoUrl)
                    .wantAuthnRequestsSigned(wantSignedRequests)
                    )
                    .singleLogoutServiceLocation("{baseUrl}/logout/saml2/slo/" + registrationId)
                    .build();

        }
        else {
            File verificationKey = new File(certificateFile);
            log.trace("{}Building configuration with certificate : {}", logPrefix, verificationKey);
            X509Certificate certificate = X509Support.decodeCertificate(verificationKey);
            Saml2X509Credential credential = Saml2X509Credential.verification(certificate);
            registration = RelyingPartyRegistration
                    .withRegistrationId(registrationId)
                    .assertingPartyDetails(party -> party
                    .entityId(entityId)
                    .singleSignOnServiceLocation(ssoUrl)
                    .wantAuthnRequestsSigned(wantSignedRequests)
                    .verificationX509Credentials(c -> c.add(credential))
                    )
                    .singleLogoutServiceLocation("{baseUrl}/logout/saml2/slo/" + registrationId)
                    .build();
        }
        log.trace("{}Build registration: {}", logPrefix, registration);
        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    @Bean
    public DefaultCookieSerializerCustomizer cookieSerializerCustomizer() {
        return cookieSerializer -> cookieSerializer.setSameSite(null);
    }
}
