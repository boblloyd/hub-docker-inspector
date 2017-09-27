/**
 * Hub Docker Inspector
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.docker.hub;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.bom.BomImportRequestService;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

@Component
public class HubClient {
    private final Logger logger = LoggerFactory.getLogger(HubClient.class);

    @Autowired
    private HubPassword hubPassword;

    @Value("${hub.url}")
    private String hubUrl;

    @Value("${hub.timeout}")
    private String hubTimeout;

    @Value("${hub.username}")
    private String hubUsername;

    @Value("${SCAN_CLI_OPTS:}")
    private String scanCliOptsEnvVar;

    @Value("${hub.proxy.host}")
    private String hubProxyHostProperty;

    @Value("${hub.proxy.port}")
    private String hubProxyPortProperty;

    @Value("${hub.proxy.username}")
    private String hubProxyUsernameProperty;

    @Value("${hub.proxy.password}")
    private String hubProxyPasswordProperty;

    @Value("${command.timeout}")
    private long commandTimeout;

    @Value("${hub.always.trust.cert}")
    private Boolean setAlwaysTrustServerCertificate;

    public boolean isValid() {
        return createBuilder().isValid();
    }

    public void testHubConnection() throws HubIntegrationException {
        final HubServerConfig hubServerConfig = createBuilder().build();
        final CredentialsRestConnection credentialsRestConnection;
        try {
            credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger));
            credentialsRestConnection.connect();
        } catch (final IntegrationException e) {
            final String msg = String.format("Error connecting to Hub: %s", e.getMessage());
            throw new HubIntegrationException(msg);
        }
        logger.info("Successful connection to the Hub.");
    }

    public void uploadBdioToHub(final File bdioFile) throws IntegrationException {
        final HubServerConfig hubServerConfig = createBuilder().build();

        final CredentialsRestConnection credentialsRestConnection = hubServerConfig.createCredentialsRestConnection(new Slf4jIntLogger(logger));
        final HubServicesFactory hubServicesFactory = new HubServicesFactory(credentialsRestConnection);
        final BomImportRequestService bomImportRequestService = hubServicesFactory.createBomImportRequestService();
        bomImportRequestService.importBomFile(bdioFile, "application/ld+json");
        logger.info(String.format("Uploaded bdio file %s to %s", bdioFile.getName(), hubServerConfig.getHubUrl()));
    }

    private HubServerConfigBuilder createBuilder() {
        final String hubPasswordString = hubPassword.get();
        String hubProxyHost = hubProxyHostProperty;
        String hubProxyPort = hubProxyPortProperty;
        String hubProxyUsername = hubProxyUsernameProperty;
        String hubProxyPassword = hubProxyPasswordProperty;
        if ((StringUtils.isBlank(hubProxyHostProperty)) && (!StringUtils.isBlank(scanCliOptsEnvVar))) {
            final List<String> scanCliOpts = Arrays.asList(scanCliOptsEnvVar.split("\\s"));
            for (String opt : scanCliOpts) {
                opt = opt.trim();
                if ((opt.startsWith("-Dhttp.proxy.host=")) || (opt.startsWith("-Dhttps.proxy.host="))) {
                    hubProxyHost = getValue(opt);
                } else if ((opt.startsWith("-Dhttp.proxy.port=")) || (opt.startsWith("-Dhttps.proxy.port="))) {
                    hubProxyPort = getValue(opt);
                } else if ((opt.startsWith("-Dhttp.proxy.username=")) || (opt.startsWith("-Dhttps.proxy.username="))) {
                    hubProxyUsername = getValue(opt);
                } else if ((opt.startsWith("-Dhttp.proxy.password=")) || (opt.startsWith("-Dhttps.proxy.password="))) {
                    hubProxyPassword = getValue(opt);
                }
            }
        }

        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setHubUrl(hubUrl);
        hubServerConfigBuilder.setUsername(hubUsername);
        hubServerConfigBuilder.setPassword(hubPasswordString);

        hubServerConfigBuilder.setTimeout(hubTimeout);
        hubServerConfigBuilder.setProxyHost(hubProxyHost);
        hubServerConfigBuilder.setProxyPort(hubProxyPort);
        hubServerConfigBuilder.setProxyUsername(hubProxyUsername);
        hubServerConfigBuilder.setProxyPassword(hubProxyPassword);

        if (setAlwaysTrustServerCertificate == null) {
            setAlwaysTrustServerCertificate = true;
        }
        hubServerConfigBuilder.setAlwaysTrustServerCertificate(setAlwaysTrustServerCertificate);

        return hubServerConfigBuilder;
    }

    private String getValue(final String nameEqualsValue) {
        final List<String> nameValue = Arrays.asList(nameEqualsValue.split("="));
        String value = null;
        if (nameValue.size() == 2) {
            value = nameValue.get(1);
        }
        return value;
    }
}