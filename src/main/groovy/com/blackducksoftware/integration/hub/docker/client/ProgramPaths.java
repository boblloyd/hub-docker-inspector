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
package com.blackducksoftware.integration.hub.docker.client;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProgramPaths {
    private static final String CONTAINER_JAR_PATH = "/opt/blackduck/hub-docker-inspector/hub-docker-inspector.jar";

    private static final String JAR_FILENAME = "hub-docker-inspector.jar";

    private static final String JAR_FILE_SUFFIX = ".jar";

    private static final String FILE_URI_PREFIX = "file:";

    private static final String RESULT_JSON_FILENAME = "result.json";

    private static final String OUTPUT_DIR = "output/";

    private static final String WORKING_DIR = "working/";

    private static final String TARGET_DIR = "target/";

    private static final String TEMP_DIR = "temp/";

    private static final String CONFIG_DIR = "config/";

    private static final String CONTAINER_PROGRAM_DIR = "/opt/blackduck/hub-docker-inspector/";

    @Value("${on.host}")
    private boolean onHost;

    @Value("${host.working.dir.path:notused}")
    private String hostWorkingDirPath;

    @Value("${hub.codelocation.prefix}")
    private String codeLocationPrefix;

    @Value("${jar.path}")
    private String givenJarPath;

    private String hubDockerPgmDirPath;
    private String hubDockerPgmDirPathContainer;
    public static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";

    private final Logger logger = LoggerFactory.getLogger(ProgramPaths.class);

    private String hubDockerConfigDirPath;
    private String hubDockerTempDirPath;
    private String hubDockerConfigDirPathContainer;
    private String hubDockerConfigFilePath;
    private String hubDockerTargetDirPath;
    private String hubDockerTargetDirPathContainer;
    private String hubDockerJarPathActual;
    private String hubDockerJarPathHost;
    private String hubDockerWorkingDirPath;
    private String hubDockerOutputPath;
    private String hubDockerOutputPathContainer;
    private String hubDockerResultPath;

    private boolean initDone = false;

    private String getProgramDirPath() {
        if (onHost) {
            return getProgramDirPathHost();
        } else {
            return getProgramDirPathContainer();
        }
    }

    private String getProgramDirPathHost() {
        if (!hostWorkingDirPath.endsWith("/")) {
            hostWorkingDirPath = String.format("%s/", hostWorkingDirPath);
        }
        return hostWorkingDirPath;
    }

    private String getProgramDirPathContainer() {
        return CONTAINER_PROGRAM_DIR;
    }

    public void init() {
        if (initDone) {
            return;
        }
        logger.debug(String.format("givenJarPath: %s", givenJarPath));
        if (StringUtils.isBlank(hubDockerPgmDirPath)) {
            hubDockerPgmDirPath = getProgramDirPath();
        }
        if (StringUtils.isBlank(hubDockerJarPathHost)) {
            hubDockerJarPathHost = givenJarPath;
        }
        hubDockerPgmDirPathContainer = getProgramDirPathContainer();
        hubDockerConfigDirPath = hubDockerPgmDirPath + CONFIG_DIR;
        hubDockerTempDirPath = hubDockerPgmDirPath + TEMP_DIR;
        hubDockerConfigDirPathContainer = hubDockerPgmDirPathContainer + CONFIG_DIR;
        hubDockerConfigFilePath = hubDockerConfigDirPath + APPLICATION_PROPERTIES_FILENAME;
        hubDockerTargetDirPath = hubDockerPgmDirPath + TARGET_DIR;
        hubDockerTargetDirPathContainer = hubDockerPgmDirPathContainer + TARGET_DIR;
        hubDockerWorkingDirPath = hubDockerPgmDirPath + WORKING_DIR;
        hubDockerOutputPath = hubDockerPgmDirPath + OUTPUT_DIR;
        hubDockerOutputPathContainer = getProgramDirPathContainer() + OUTPUT_DIR;
        hubDockerResultPath = hubDockerOutputPath + RESULT_JSON_FILENAME;

        final String qualifiedJarPathString = getQualifiedJarPath();
        logger.debug(String.format("qualifiedJarPathString: %s", qualifiedJarPathString));
        final String prefix = FILE_URI_PREFIX;
        final int startIndex = qualifiedJarPathString.indexOf(prefix) + prefix.length();
        final int endIndex = qualifiedJarPathString.indexOf(JAR_FILE_SUFFIX) + JAR_FILE_SUFFIX.length();
        hubDockerJarPathActual = qualifiedJarPathString.substring(startIndex, endIndex);
        logger.debug(String.format("hubDockerJarPathActual: %s", hubDockerJarPathActual));
        initDone = true;
    }

    public String normalizeJarFilename(final String hostJarPath) throws IOException {
        final File fromFile = new File(hostJarPath);
        final File toFile = new File(getHubDockerTempDirPath() + JAR_FILENAME);
        FileUtils.copyFile(fromFile, toFile);
        return toFile.getAbsolutePath();
    }

    public String getQualifiedJarPath() {
        return DockerClientManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    public String getHubDockerConfigDirPath() {
        init();
        return hubDockerConfigDirPath;
    }

    public String getHubDockerTempDirPath() {
        init();
        return hubDockerTempDirPath;
    }

    public String getHubDockerConfigDirPathContainer() {
        init();
        return hubDockerConfigDirPathContainer;
    }

    public String getHubDockerConfigFilePath() {
        init();
        return hubDockerConfigFilePath;
    }

    public String getHubDockerTargetDirPath() {
        init();
        return hubDockerTargetDirPath;
    }

    public String getHubDockerTargetDirPathContainer() {
        init();
        return hubDockerTargetDirPathContainer;
    }

    public String getHubDockerPgmDirPath() {
        init();
        return hubDockerPgmDirPath;
    }

    public String getHubDockerPgmDirPathContainer() {
        init();
        return hubDockerPgmDirPathContainer;
    }

    public String getHubDockerJarPathHost() {
        init();
        return hubDockerJarPathHost;
    }

    public String getHubDockerJarPathContainer() {
        init();
        return CONTAINER_JAR_PATH;
    }

    public String getHubDockerJarPathActual() {
        init();
        return hubDockerJarPathActual;
    }

    public String getHubDockerWorkingDirPath() {
        init();
        return hubDockerWorkingDirPath;
    }

    public String getHubDockerOutputPath() {
        init();
        return hubDockerOutputPath;
    }

    public String getHubDockerResultPath() {
        init();
        return hubDockerResultPath;
    }

    public String getHubDockerOutputPathContainer() {
        init();
        return hubDockerOutputPathContainer;
    }

    public void setHubDockerPgmDirPath(final String hubDockerPgmDirPath) {
        this.hubDockerPgmDirPath = hubDockerPgmDirPath;
    }

    public String getImageTarFilename(final String imageName, final String tagName) {
        return String.format("%s_%s.tar", imageName, tagName);
    }

    public String getContainerFileSystemTarFilename(final String imageName, final String tagName) {
        return String.format("%s_%s_containerfilesystem.tar.gz", slashesToUnderscore(imageName), tagName);
    }

    public String getTargetImageFileSystemRootDirName(final String imageName, final String imageTag) {
        return String.format("image_%s_v_%s", imageName.replaceAll("/", "_"), imageTag);
    }

    public String getCodeLocationName(final String imageName, final String imageTag, final String pkgMgrFilePath, final String pkgMgrName) {
        if (!StringUtils.isBlank(codeLocationPrefix)) {
            return String.format("%s_%s_%s_%s_%s", codeLocationPrefix, slashesToUnderscore(imageName), imageTag, slashesToUnderscore(pkgMgrFilePath), pkgMgrName);
        }
        return String.format("%s_%s_%s_%s", slashesToUnderscore(imageName), imageTag, slashesToUnderscore(pkgMgrFilePath), pkgMgrName);
    }

    public String getBdioFilename(final String imageName, final String pkgMgrFilePath, final String hubProjectName, final String hubVersionName) {
        return String.format("%s_%s_%s_%s_bdio.jsonld", cleanImageName(imageName), cleanPath(pkgMgrFilePath), cleanHubProjectName(hubProjectName), hubVersionName);
    }

    public String getDependencyNodesFilename(final String imageName, final String pkgMgrFilePath, final String hubProjectName, final String hubVersionName) {
        return String.format("%s_%s_%s_%s_dependencies.json", cleanImageName(imageName), cleanPath(pkgMgrFilePath), cleanHubProjectName(hubProjectName), hubVersionName);
    }

    void setCodeLocationPrefix(final String codeLocationPrefix) {
        this.codeLocationPrefix = codeLocationPrefix;
    }

    public String cleanHubProjectName(final String hubProjectName) {
        return slashesToUnderscore(hubProjectName);
    }

    public String cleanImageName(final String imageName) {
        return colonsToUnderscores(slashesToUnderscore(imageName));
    }

    public String cleanPath(final String path) {
        return slashesToUnderscore(path);
    }

    private String slashesToUnderscore(final String imageName) {
        return imageName.replaceAll("/", "_");
    }

    private String colonsToUnderscores(final String imageName) {
        return imageName.replaceAll(":", "_");
    }
}
