/*
 *   gcloudlicensemanagement - AzureADConnection.java
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
package com.slinkytoybox.gcloud.licensing.connection;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.Organization;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.OrganizationCollectionPage;
import com.slinkytoybox.gcloud.licensing.init.PlatformEncryption;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Service("AzureADConnection")
@DependsOn("CloudDatabaseConnection")
@Slf4j
public class AzureADConnection {

    @Value("${azure.graph.pfx-file:NOT_SET}")
    private String pfxFile;

    @Value("${azure.graph.pfx-password:NOT_SET}")
    private String pfxPassword;

    @Value("${azure.graph.client-id:NOT_SET}")
    private String clientId;

    @Value("${azure.graph.tenant-id:NOT_SET}")
    private String tenantId;
    
    @Autowired
    private PlatformEncryption pe;
    
    @Autowired
    private Environment env;
    
    private GraphServiceClient graphClient;

    @PostConstruct
    public void startAzureAd() {
        final String logPrefix = "startAzureAd() - ";
        log.trace("{}Entering Method", logPrefix);

        String pfxPassword = env.getProperty("azure.graph.pfx-password", "NOT_SET");
        
        if (pfxFile.equalsIgnoreCase("NOT_SET") || pfxPassword.equalsIgnoreCase("NOT_SET") || clientId.equalsIgnoreCase("NOT_SET")
                || tenantId.equalsIgnoreCase("NOT_SET")) {
            log.error("{}Microsoft Azure Graph paramaters 'azure.graph.pfx-file|pfx-password|client-id|tenant-id' are not defined correctly", logPrefix);
            throw new IllegalArgumentException("Microsoft Azure Graph paramaters 'azure.graph.pfx-file|pfx-password|client-id|tenant-id' are not defined correctly");
        }

        log.info("{}Initialisating Azure AD Graph connection", logPrefix);

        log.trace("{}Creating token credential", logPrefix);
        final TokenCredential credential = new ClientCertificateCredentialBuilder()
                .pfxCertificate(pfxFile, pe.decrypt(pfxPassword))
                .clientId(clientId)
                .tenantId(tenantId)
                .build();

        TokenCredentialAuthProvider tcap = new TokenCredentialAuthProvider(credential);

        log.debug("{}Creating Graph Service Client", logPrefix);

        graphClient = GraphServiceClient.builder().authenticationProvider(tcap).buildClient();

        log.debug("{}Created client, testing connection", logPrefix);
        
        OrganizationCollectionPage orgPage = graphClient.organization().buildRequest().get();
        List<Organization> orgs = (orgPage == null ?  new ArrayList<>() : orgPage.getCurrentPage());
        
        log.info("{}Connected to MS Graph - Found {} organisations", logPrefix, orgs.size());
        for(Organization org:orgs) {
            log.debug("{}Organisation: {} ({})", logPrefix, org.displayName, org.id);
        }
        if (orgs.isEmpty()) {
            throw new IllegalStateException("Cannot load Microsoft Graph - No Organisations found");
        }

    }
    
    
    public GraphServiceClient getClient () {
        return graphClient;
    }
}
