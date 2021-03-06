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
package com.blackducksoftware.integration.hub.docker.dockerinspector.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.dockerinspector.help.ValueDescription;

@Component
public class Config {
    private static final String INSPECTOR_OS_UBUNTU = "ubuntu";
    private final static String GROUP_PUBLIC = "public";
    private final static String GROUP_PRIVATE = "private";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Black Duck Hub connection details
    @ValueDescription(description = "Hub URL", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.url:}")
    private String hubUrl = "";

    @ValueDescription(description = "Hub Timeout in seconds", defaultValue = "120", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.timeout:120}")
    private Integer hubTimeout = 120;

    @ValueDescription(description = "Hub token", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.api.token:}")
    private String hubApiToken = "";

    @ValueDescription(description = "Hub Username", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.username:}")
    private String hubUsername = "";

    @ValueDescription(description = "Hub Password", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.password:}")
    private String hubPassword = "";

    // The properties in this section must be set if you must connect to the Hub through a proxy
    @ValueDescription(description = "Hub Proxy Host", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.proxy.host:}")
    private String hubProxyHost = "";

    @ValueDescription(description = "Hub Proxy Port", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.proxy.port:}")
    private String hubProxyPort = "";

    @ValueDescription(description = "Hub Proxy Username", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.proxy.username:}")
    private String hubProxyUsername = "";

    @ValueDescription(description = "Hub Proxy Password", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.proxy.password:}")
    private String hubProxyPassword = "";

    // If using an https Hub server, you can choose to always trust the server certificates
    @ValueDescription(description = "Hub Always Trust Cert?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.always.trust.cert:false}")
    private Boolean hubAlwaysTrustCert = Boolean.FALSE;

    // The default project name will be the Docker image name
    @ValueDescription(description = "Hub Project Name", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.project.name:}")
    private String hubProjectName = "";

    // The default version name will be Docker image tag
    @ValueDescription(description = "Hub Project Version", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.project.version:}")
    private String hubProjectVersion = "";

    // Working directory
    @ValueDescription(description = "Working Directory Path", defaultValue = "/tmp/hub-docker-inspector-files", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${working.dir.path:/tmp/hub-docker-inspector-files}")
    private String workingDirPath = "";

    // If false, will leave behind the files created in the working dir
    @ValueDescription(description = "Cleanup Working Dir?", defaultValue = "true", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${cleanup.working.dir:true}")
    private Boolean cleanupWorkingDir = Boolean.TRUE;

    // If Hub Docker Inspector cannot derive it automatically,
    // use linux.distro to specify the target image linux distribution
    // (ubuntu, debian, busybox, centos, fedora, redhat, alpine)
    @ValueDescription(description = "Linux Distribution Name", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${linux.distro:}")
    private String linuxDistro = "";

    // Timeout for external command execution (to pull a docker image, etc.)
    @ValueDescription(description = "Command Timeout (Milliseconds)", defaultValue = "120000", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${command.timeout:120000}")
    private Long commandTimeout = 120000L;

    // Logging level: ERROR, WARN, INFO, DEBUG, TRACE
    @ValueDescription(description = "Logging Level (WARN, INFO, DEBUG, TRACE)", defaultValue = "INFO", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${logging.level.com.blackducksoftware:INFO}")
    private String loggingLevel = "";

    // Path on host of a directory into which the resulting output files will be copied
    @ValueDescription(description = "Path to directory for output files", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${output.path:}")
    private String outputPath = "";

    // Set to true to include the container file system tarfile in the output
    @ValueDescription(description = "Include container filesystem (a large file) in output?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${output.include.containerfilesystem:false}")
    private Boolean outputIncludeContainerfilesystem = Boolean.FALSE;

    // If you want to add a prefix to the code location name, specify it here
    @ValueDescription(description = "Hub CodeLocation prefix", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${hub.codelocation.prefix:}")
    private String hubCodelocationPrefix = "";

    // Path to the hub-docker-inspector .jar file
    // Only used by hub-docker-inspector.sh
    @ValueDescription(description = "Hub Docker Inspector .jar file path", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${jar.path:}")
    private String jarPath = "";

    // The following properties should not normally be set/changed by the user
    @ValueDescription(description = "Docker Image name:tag", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.image:}")
    private String dockerImage = "";

    @ValueDescription(description = "Docker tarfile path", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.tar:}")
    private String dockerTar = "";

    @ValueDescription(description = "docker.image.id", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.image.id:}")
    private String dockerImageId = "";

    @ValueDescription(description = "Docker Image Repo; Use with docker.image.tag to select one image from a tarfile", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.image.repo:}")
    private String dockerImageRepo = "";

    @ValueDescription(description = "Docker Image Tag; Use with docker.image.repo to select one image from a tarfile", defaultValue = "", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${docker.image.tag:}")
    private String dockerImageTag = "";

    @ValueDescription(description = "Running on host?", defaultValue = "true", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${on.host:true}")
    private Boolean onHost = Boolean.TRUE;

    @ValueDescription(description = "Caller Name", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${caller.name:}")
    private String callerName = "";

    @ValueDescription(description = "caller.version", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${caller.version:}")
    private String callerVersion = "";

    @ValueDescription(description = "Phone Home?", defaultValue = "true", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${phone.home:true}")
    private Boolean phoneHome = Boolean.TRUE;

    @ValueDescription(description = "Upload BDIO?", defaultValue = "true", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${upload.bdio:true}")
    private Boolean uploadBdio = Boolean.TRUE;

    @ValueDescription(description = "Repository name for the Hub Docker Inspector images", defaultValue = "blackducksoftware", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${inspector.repository:blackducksoftware}")
    private String inspectorRepository = "blackducksoftware";

    @ValueDescription(description = "Hub Docker Inspector image \"family\"", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${inspector.image.family:}")
    private String inspectorImageFamily = "";

    @ValueDescription(description = "Hub Docker Inspector image version", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${inspector.image.version:}")
    private String inspectorImageVersion = "";

    @ValueDescription(description = "Remove target image after saving it?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${cleanup.target.image:false}")
    private Boolean cleanupTargetImage = Boolean.FALSE;

    @ValueDescription(description = "Stop inspector container after using it?", defaultValue = "true", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${cleanup.inspector.container:true}")
    private Boolean cleanupInspectorContainer = Boolean.TRUE;

    @ValueDescription(description = "Remove inspector image after using it?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${cleanup.inspector.image:false}")
    private Boolean cleanupInspectorImage = Boolean.FALSE;

    @ValueDescription(description = "The host's path to the dir shared with the imageinspector containers. Only needed if using existing imageinspector containers", defaultValue = "/tmp/hub-docker-inspector-files/shared", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${shared.dir.path.local:/tmp/hub-docker-inspector-files/shared}")
    private String sharedDirPathLocal = "/tmp/hub-docker-inspector-files/shared";

    @ValueDescription(description = "The container's path to the shared directory. Only needed if using existing imageinspector containers", defaultValue = "/opt/blackduck/hub-imageinspector-ws/shared", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${shared.dir.path.imageinspector:/opt/blackduck/hub-imageinspector-ws/shared}")
    private String sharedDirPathImageInspector = "/opt/blackduck/hub-imageinspector-ws/shared";

    @ValueDescription(description = "The URL of the (already running) imageinspector service to use", defaultValue = "", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${imageinspector.service.url:}")
    private String imageInspectorUrl = "";

    // Properties for pull/start services/containers as needed mode:

    @ValueDescription(description = "Start ImageInspector services (containers) as needed?", defaultValue = "false", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.start:false}")
    private Boolean imageInspectorServiceStart = Boolean.FALSE;

    @ValueDescription(description = "alpine image inspector container port", defaultValue = "8080", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${imageinspector.service.container.port.alpine:8080}")
    private String imageInspectorContainerPortAlpine = "8080";

    @ValueDescription(description = "centos image inspector container port", defaultValue = "8081", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${imageinspector.service.container.port.centos:8081}")
    private String imageInspectorContainerPortCentos = "8081";

    @ValueDescription(description = "ubuntu image inspector container port", defaultValue = "8082", group = Config.GROUP_PRIVATE, deprecated = false)
    @Value("${imageinspector.service.container.port.ubuntu:8082}")
    private String imageInspectorContainerPortUbuntu = "8082";

    @ValueDescription(description = "alpine image inspector host port", defaultValue = "9000", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.port.alpine:9000}")
    private String imageInspectorHostPortAlpine = "9000";

    @ValueDescription(description = "centos image inspector host port", defaultValue = "9001", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.port.centos:9001}")
    private String imageInspectorHostPortCentos = "9001";

    @ValueDescription(description = "ubuntu image inspector host port", defaultValue = "9002", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.port.ubuntu:9002}")
    private String imageInspectorHostPortUbuntu = "9002";

    // In "start containers" mode, default is specified by distro; in "use existing", it's specified by URL
    @ValueDescription(description = "Default image inspector Linus distro (alpine, centos, or ubuntu)", defaultValue = "ubuntu", group = Config.GROUP_PUBLIC, deprecated = false)
    @Value("${imageinspector.service.distro.default:ubuntu}")
    private String imageInspectorDefaultDistro = INSPECTOR_OS_UBUNTU;

    // Environment Variables
    @Value("${BD_HUB_PASSWORD:}")
    private String hubPasswordEnvVar = "";

    @Value("${BD_HUB_TOKEN:}")
    private String hubApiTokenEnvVar = "";

    @Value("${SCAN_CLI_OPTS:}")
    private String scanCliOptsEnvVar = "";

    @Value("${DOCKER_INSPECTOR_JAVA_OPTS:}")
    private String dockerInspectorJavaOptsValue = "";

    private TreeSet<DockerInspectorOption> publicOptions;
    private Map<String, DockerInspectorOption> optionsByKey;
    private Map<String, DockerInspectorOption> optionsByFieldName;
    private TreeSet<String> allKeys;

    public String get(final String key) throws IllegalArgumentException, IllegalAccessException {
        final DockerInspectorOption opt = optionsByKey.get(key);
        if (opt == null) {
            return null;
        }
        return opt.getResolvedValue();
    }

    public boolean isPublic(final String key) throws IllegalArgumentException, IllegalAccessException {
        final DockerInspectorOption opt = optionsByKey.get(key);
        if (opt == null) {
            return false;
        }
        return Config.GROUP_PUBLIC.equals(opt.getGroup());
    }

    public SortedSet<DockerInspectorOption> getPublicConfigOptions() throws IllegalArgumentException, IllegalAccessException {
        return publicOptions;
    }

    public SortedSet<String> getAllKeys() throws IllegalArgumentException, IllegalAccessException {
        return allKeys;
    }

    @PostConstruct
    public void init() throws IllegalArgumentException, IllegalAccessException {
        final Object configObject = this;
        publicOptions = new TreeSet<>();
        allKeys = new TreeSet<>();
        optionsByKey = new HashMap<>();
        optionsByFieldName = new HashMap<>();
        for (final Field field : configObject.getClass().getDeclaredFields()) {
            final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
            if (declaredAnnotations.length > 0) {
                for (final Annotation annotation : declaredAnnotations) {
                    if (annotation.annotationType().getName().equals(ValueDescription.class.getName())) {
                        logger.debug(String.format("ValueDescription annotated config object field: %s", field.getName()));
                        final String propMappingString = field.getAnnotation(Value.class).value();
                        final String propName = SpringValueUtils.springKeyFromValueAnnotation(propMappingString);
                        final Object fieldValueObject = field.get(configObject);
                        if (fieldValueObject == null) {
                            logger.warn(String.format("propName %s field is null", propName));
                            continue;
                        }
                        final String value = fieldValueObject.toString();
                        logger.trace(String.format("adding prop key %s [value: %s]", propName, value));
                        allKeys.add(propName);
                        final ValueDescription valueDescription = field.getAnnotation(ValueDescription.class);
                        final DockerInspectorOption opt = new DockerInspectorOption(propName, field.getName(), value, valueDescription.description(), field.getType(), valueDescription.defaultValue(), valueDescription.group(),
                                valueDescription.deprecated());
                        optionsByKey.put(propName, opt);
                        logger.trace(String.format("adding field name %s to optionsByFieldName", field.getName()));
                        optionsByFieldName.put(field.getName(), opt);
                        if (!Config.GROUP_PRIVATE.equals(valueDescription.group())) {
                            publicOptions.add(opt);
                        } else {
                            logger.debug(String.format("private prop: propName: %s, fieldName: %s, group: %s, description: %s", propName, field.getName(), valueDescription.group(), valueDescription.description()));
                        }
                    }
                }
            }
        }
    }

    public String getLoggingLevel() {
        return optionsByFieldName.get("loggingLevel").getResolvedValue();
    }

    public String getHubUrl() {
        return optionsByFieldName.get("hubUrl").getResolvedValue();
    }

    public Integer getHubTimeout() {
        return new Integer(optionsByFieldName.get("hubTimeout").getResolvedValue());
    }

    public String getHubApiToken() {
        return optionsByFieldName.get("hubApiToken").getResolvedValue();
    }

    public String getHubUsername() {
        return unEscape(optionsByFieldName.get("hubUsername").getResolvedValue());
    }

    public String getHubPassword() {
        return optionsByFieldName.get("hubPassword").getResolvedValue();
    }

    public String getHubProxyHost() {
        return optionsByFieldName.get("hubProxyHost").getResolvedValue();
    }

    public String getHubProxyPort() {
        return optionsByFieldName.get("hubProxyPort").getResolvedValue();
    }

    public String getHubProxyUsername() {
        return optionsByFieldName.get("hubProxyUsername").getResolvedValue();
    }

    public String getHubProxyPassword() {
        return optionsByFieldName.get("hubProxyPassword").getResolvedValue();
    }

    public boolean isHubAlwaysTrustCert() {
        return optionsByFieldName.get("hubAlwaysTrustCert").getResolvedValue().equals("true");
    }

    public String getHubProjectName() {
        return unEscape(optionsByFieldName.get("hubProjectName").getResolvedValue());
    }

    public String getHubProjectVersion() {
        return unEscape(optionsByFieldName.get("hubProjectVersion").getResolvedValue());
    }

    public String getWorkingDirPath() {
        if (StringUtils.isNotBlank(getImageInspectorUrl()) || isImageInspectorServiceStart()) {
            return optionsByFieldName.get("sharedDirPathLocal").getResolvedValue();
        }
        return optionsByFieldName.get("workingDirPath").getResolvedValue();
    }

    public boolean isCleanupWorkingDir() {
        return optionsByFieldName.get("cleanupWorkingDir").getResolvedValue().equals("true");
    }

    public String getLinuxDistro() {
        return optionsByFieldName.get("linuxDistro").getResolvedValue();
    }

    public Long getCommandTimeout() {
        return new Long(optionsByFieldName.get("commandTimeout").getResolvedValue());
    }

    public String getOutputPath() {
        return optionsByFieldName.get("outputPath").getResolvedValue();
    }

    public boolean isOutputIncludeContainerfilesystem() {
        return optionsByFieldName.get("outputIncludeContainerfilesystem").getResolvedValue().equals("true");
    }

    public String getHubCodelocationPrefix() {
        return optionsByFieldName.get("hubCodelocationPrefix").getResolvedValue();
    }

    public String getDockerImage() {
        return optionsByFieldName.get("dockerImage").getResolvedValue();
    }

    public String getDockerTar() {
        return unEscape(optionsByFieldName.get("dockerTar").getResolvedValue());
    }

    public String getDockerImageId() {
        return optionsByFieldName.get("dockerImageId").getResolvedValue();
    }

    public String getDockerImageRepo() {
        return optionsByFieldName.get("dockerImageRepo").getResolvedValue();
    }

    public String getDockerImageTag() {
        return optionsByFieldName.get("dockerImageTag").getResolvedValue();
    }

    public boolean isOnHost() {
        return optionsByFieldName.get("onHost").getResolvedValue().equals("true");
    }

    public String getCallerName() {
        return optionsByFieldName.get("callerName").getResolvedValue();
    }

    public String getInspectorRepository() {
        return optionsByFieldName.get("inspectorRepository").getResolvedValue();
    }

    public String getInspectorImageFamily() {
        return optionsByFieldName.get("inspectorImageFamily").getResolvedValue();
    }

    public String getInspectorImageVersion() {
        return optionsByFieldName.get("inspectorImageVersion").getResolvedValue();
    }

    public String getSharedDirPathImageInspector() {
        return optionsByFieldName.get("sharedDirPathImageInspector").getResolvedValue();
    }

    public String getSharedDirPathLocal() {
        return optionsByFieldName.get("sharedDirPathLocal").getResolvedValue();
    }

    public String getImageInspectorUrl() {
        return optionsByFieldName.get("imageInspectorUrl").getResolvedValue();
    }

    public Integer getImageInspectorContainerPortAlpine() {
        return new Integer(optionsByFieldName.get("imageInspectorContainerPortAlpine").getResolvedValue());
    }

    public Integer getImageInspectorContainerPortCentos() {
        return new Integer(optionsByFieldName.get("imageInspectorContainerPortCentos").getResolvedValue());
    }

    public Integer getImageInspectorContainerPortUbuntu() {
        return new Integer(optionsByFieldName.get("imageInspectorContainerPortUbuntu").getResolvedValue());
    }

    public Integer getImageInspectorHostPortAlpine() {
        return new Integer(optionsByFieldName.get("imageInspectorHostPortAlpine").getResolvedValue());
    }

    public Integer getImageInspectorHostPortCentos() {
        return new Integer(optionsByFieldName.get("imageInspectorHostPortCentos").getResolvedValue());
    }

    public Integer getImageInspectorHostPortUbuntu() {
        return new Integer(optionsByFieldName.get("imageInspectorHostPortUbuntu").getResolvedValue());
    }

    public String getImageInspectorDefaultDistro() {
        return optionsByFieldName.get("imageInspectorDefaultDistro").getResolvedValue();
    }

    public String getCallerVersion() {
        return optionsByFieldName.get("callerVersion").getResolvedValue();
    }

    public boolean isPhoneHome() {
        return optionsByFieldName.get("phoneHome").getResolvedValue().equals("true");
    }

    public String getScanCliOptsEnvVar() {
        return scanCliOptsEnvVar;
    }

    public String getHubPasswordEnvVar() {
        return hubPasswordEnvVar;
    }

    public String getHubApiTokenEnvVar() {
        return hubApiTokenEnvVar;
    }

    public String getDockerInspectorJavaOptsValue() {
        return dockerInspectorJavaOptsValue;
    }

    public boolean isUploadBdio() {
        return optionsByFieldName.get("uploadBdio").getResolvedValue().equals("true");
    }

    public void setUploadBdio(final boolean value) {
        optionsByFieldName.get("uploadBdio").setResolvedValue(Boolean.toString(value));
    }

    public boolean isCleanupTargetImage() {
        return optionsByFieldName.get("cleanupTargetImage").getResolvedValue().equals("true");
    }

    public boolean isCleanupInspectorContainer() {
        return optionsByFieldName.get("cleanupInspectorContainer").getResolvedValue().equals("true");
    }

    public boolean isCleanupInspectorImage() {
        return optionsByFieldName.get("cleanupInspectorImage").getResolvedValue().equals("true");
    }

    public boolean isImageInspectorServiceStart() {
        return optionsByFieldName.get("imageInspectorServiceStart").getResolvedValue().equals("true");
    }

    public void setDockerImageRepo(final String newValue) {
        optionsByFieldName.get("dockerImageRepo").setResolvedValue(newValue);
    }

    public void setDockerImageTag(final String newValue) {
        optionsByFieldName.get("dockerImageTag").setResolvedValue(newValue);
    }

    public void setWorkingDirPath(final String newValue) {
        optionsByFieldName.get("workingDirPath").setResolvedValue(newValue);
    }

    public void setHubCodelocationPrefix(final String newValue) {
        optionsByFieldName.get("hubCodelocationPrefix").setResolvedValue(newValue);
    }

    public void setLoggingLevel(final String newValue) {
        optionsByFieldName.get("loggingLevel").setResolvedValue(newValue);
    }

    private String unEscape(final String origString) {
        logger.trace(String.format("origString: %s", origString));
        final String unEscapedString = origString.replaceAll("%20", " ");
        logger.trace(String.format("unEscapedString: %s", unEscapedString));
        return unEscapedString;
    }

    // This is here to prevent eclipse from making config property members final
    protected void preventFinal() {
        this.callerName = null;
        this.callerVersion = null;
        this.cleanupWorkingDir = null;
        this.commandTimeout = null;
        this.dockerImage = null;
        this.dockerImageId = null;
        this.dockerImageRepo = null;
        this.dockerImageTag = null;
        this.dockerInspectorJavaOptsValue = null;
        this.dockerTar = null;
        this.hubAlwaysTrustCert = null;
        this.hubCodelocationPrefix = null;
        this.hubPassword = null;
        this.hubPasswordEnvVar = null;
        this.hubApiTokenEnvVar = null;
        this.hubProjectName = null;
        this.hubProjectVersion = null;
        this.hubProxyHost = null;
        this.hubProxyPassword = null;
        this.hubProxyPort = null;
        this.hubProxyUsername = null;
        this.hubTimeout = null;
        this.hubUrl = null;
        this.hubUsername = null;
        this.hubApiToken = null;
        this.jarPath = null;
        this.linuxDistro = null;
        this.loggingLevel = null;
        this.onHost = null;
        this.outputIncludeContainerfilesystem = null;
        this.outputPath = null;
        this.phoneHome = null;
        this.scanCliOptsEnvVar = null;
        this.workingDirPath = null;
        this.uploadBdio = null;
        this.inspectorRepository = null;
        this.cleanupInspectorContainer = null;
        this.cleanupInspectorImage = null;
        this.cleanupTargetImage = null;
        this.inspectorImageFamily = null;
        this.inspectorImageVersion = null;
        this.sharedDirPathImageInspector = null;
        this.sharedDirPathLocal = null;
        this.imageInspectorUrl = null;
        this.imageInspectorServiceStart = null;
        this.imageInspectorContainerPortAlpine = null;
        this.imageInspectorContainerPortCentos = null;
        this.imageInspectorContainerPortUbuntu = null;

        this.imageInspectorHostPortAlpine = null;
        this.imageInspectorHostPortCentos = null;
        this.imageInspectorHostPortUbuntu = null;
        this.imageInspectorDefaultDistro = null;
    }
}
