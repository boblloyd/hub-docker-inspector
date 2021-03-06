/**
 * hub-docker-inspector
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.docker.dockerinspector.hubclient;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;

@Component
public class HubSecrets {

    @Autowired
    private Config config;

    public String getPassword() {
        String hubPassword = config.getHubPasswordEnvVar();
        if (!StringUtils.isBlank(config.getHubPassword())) {
            hubPassword = config.getHubPassword();
        }
        return hubPassword;
    }

    public String getApiToken() {
        String hubApiToken = config.getHubApiTokenEnvVar();
        if (!StringUtils.isBlank(config.getHubApiToken())) {
            hubApiToken = config.getHubApiToken();
        }
        return hubApiToken;
    }
}
