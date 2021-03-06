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
package com.blackducksoftware.integration.hub.docker.dockerinspector.dockerclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.ProgramPaths;
import com.blackducksoftware.integration.hub.docker.dockerinspector.hubclient.HubSecrets;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.api.ImageInspectorOsEnum;
import com.blackducksoftware.integration.hub.imageinspector.name.ImageNameResolver;
import com.blackducksoftware.integration.hub.imageinspector.name.Names;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.command.SaveImageCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;

@Component
public class DockerClientManager {
    private static final String CONTAINER_APPNAME_LABEL_KEY = "app";
    private static final String IMAGEINSPECTOR_APP_NAME_LABEL_VALUE = "hub-imageinspector-ws";

    private static final String CONTAINER_OS_LABEL_KEY = "os";

    private static final String IMAGE_TARFILE_PROPERTY = "docker.tar";
    private static final String IMAGE_PROPERTY = "docker.image";
    private static final String IMAGE_REPO_PROPERTY = "docker.image.repo";
    private static final String IMAGE_TAG_PROPERTY = "docker.image.tag";
    private static final String ON_HOST_PROPERTY = "on.host";

    private static final String INSPECT_PROPERTY = "inspect"; // true
    private static final String INSPECT_IN_CONTAINER_PROPERTY = "inspect.in.container"; // false
    private static final String UPLOAD_BDIO_PROPERTY = "upload.bdio"; // false
    private static final String DETECT_PKG_MGR_PROPERTY = "detect.pkg.mgr"; // true

    private static final String OUTPUT_INCLUDE_DOCKER_TARFILE_PROPERTY = "output.include.dockertarfile";
    private static final String OUTPUT_INCLUDE_CONTAINER_FILE_SYSTEM_TARFILE_PROPERTY = "output.include.containerfilesystem";
    private final Logger logger = LoggerFactory.getLogger(DockerClientManager.class);

    @Autowired
    private HubDockerClient hubDockerClient;

    @Autowired
    private HubSecrets hubSecrets;

    @Autowired
    private ProgramPaths programPaths;

    @Autowired
    private HubDockerProperties hubDockerProperties;

    @Autowired
    private Config config;

    public File getTarFileFromDockerImageById(final String imageId, final File imageTarDirectory) throws HubIntegrationException, IOException {

        final DockerClient dockerClient = hubDockerClient.getDockerClient();
        final InspectImageCmd inspectImageCmd = dockerClient.inspectImageCmd(imageId);
        final InspectImageResponse imageDetails = inspectImageCmd.exec();
        final List<String> repoTags = imageDetails.getRepoTags();
        if (repoTags.size() == 0) {
            throw new HubIntegrationException(String.format("Unable to get image name:tag for image ID %s", imageId));
        }

        final ImageNameResolver resolver = new ImageNameResolver(repoTags.get(0));
        final String imageName = resolver.getNewImageRepo().get();
        final String tagName = resolver.getNewImageTag().get();
        logger.debug(String.format("Converted image ID %s to image name:tag %s:%s", imageId, imageName, tagName));
        final File imageTarFile = saveImageToDir(imageTarDirectory, Names.getImageTarFilename(imageName, tagName), imageName, tagName);
        return imageTarFile;
    }

    public File getTarFileFromDockerImage(final String imageName, final String tagName, final File imageTarDirectory) throws HubIntegrationException, IOException {
        Optional<String> targetImageId = Optional.empty();
        try {
            targetImageId = Optional.ofNullable(pullImage(imageName, tagName));
        } catch (final Exception e) {
            logger.info(String.format("Unable to pull %s:%s; Proceeding anyway since the image might be in local docker image cache. Error on pull: %s", imageName, tagName, e.getMessage()));
        }
        final File imageTarFile = saveImageToDir(imageTarDirectory, Names.getImageTarFilename(imageName, tagName), imageName, tagName);
        if (config.isCleanupTargetImage() && targetImageId.isPresent()) {
            try {
                removeImage(targetImageId.get());
            } catch (final HubIntegrationException e) {
                logger.warn(String.format("Unable to remove target image with ID %s: %s", targetImageId.get(), e.getMessage()));
            }
        }
        return imageTarFile;
    }

    public String pullImage(final String imageName, final String tagName) throws HubIntegrationException {
        logger.info(String.format("Pulling image %s:%s", imageName, tagName));
        final DockerClient dockerClient = hubDockerClient.getDockerClient();
        final PullImageCmd pull = dockerClient.pullImageCmd(imageName).withTag(tagName);
        try {
            pull.exec(new PullImageResultCallback()).awaitSuccess();
        } catch (final NotFoundException e) {
            throw new HubIntegrationException(String.format("Pull failed: Image %s:%s not found. Please check the image name/tag. Error: %s", imageName, tagName, e.getMessage()), e);
        }
        final Image justPulledImage = getLocalImage(dockerClient, imageName, tagName);
        if (justPulledImage == null) {
            final String msg = String.format("Pulled image %s:%s not found in image list.", imageName, tagName);
            logger.error(msg);
            throw new HubIntegrationException(msg);
        }
        return justPulledImage.getId();
    }

    public void removeImage(final String imageId) throws HubIntegrationException {
        if (imageId == null) {
            logger.debug("removeImage(): given imageId is null; not doing anything");
            return;
        }
        try {
            final DockerClient dockerClient = hubDockerClient.getDockerClient();
            final RemoveImageCmd rmCmd = dockerClient.removeImageCmd(imageId);
            logger.info(String.format("Removing image %s", imageId));
            rmCmd.exec();
            logger.debug(String.format("Image %s removed", imageId));
        } catch (final Throwable e) {
            logger.warn(String.format("Unable to remove image with ID %s: %s", imageId, e.getMessage()));
        }
    }

    public String runByExec(final String runOnImageName, final String runOnTagName, final String runOnImageId, final File dockerTarFile, final boolean copyJar, final String targetImage, final String targetImageRepo,
            final String targetImageTag)
            throws InterruptedException, IOException, IllegalArgumentException, IllegalAccessException, IntegrationException {
        final String imageNameTag = String.format("%s:%s", runOnImageName, runOnTagName);
        logger.info(String.format("Running container based on image %s", imageNameTag));
        final String extractorContainerName = programPaths.deriveContainerName(runOnImageName);
        logger.debug(String.format("Container name: %s", extractorContainerName));
        final DockerClient dockerClient = hubDockerClient.getDockerClient();
        final String tarFileDirInSubContainer = programPaths.getHubDockerTargetDirPathContainer();
        final String tarFilePathInSubContainer = programPaths.getHubDockerTargetDirPathContainer() + dockerTarFile.getName();

        final String containerId = prepareContainerForExec(dockerClient, imageNameTag, extractorContainerName, hubSecrets.getPassword(), hubSecrets.getApiToken());
        setPropertiesInSubContainer(dockerClient, containerId, tarFilePathInSubContainer, tarFileDirInSubContainer, dockerTarFile, targetImage, targetImageRepo, targetImageTag);
        copyFileToContainer(dockerClient, containerId, dockerTarFile.getAbsolutePath(), tarFileDirInSubContainer);
        if (copyJar) {
            copyFileToContainer(dockerClient, containerId, programPaths.getHubDockerJarPathHost(), programPaths.getHubDockerPgmDirPathContainer());
        }

        final List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-Dfile.encoding=UTF-8");
        if (!StringUtils.isBlank(config.getDockerInspectorJavaOptsValue())) {
            final String[] dockerInspectorJavaOptsParts = config.getDockerInspectorJavaOptsValue().split("\\p{Space}");
            for (int i = 0; i < dockerInspectorJavaOptsParts.length; i++) {
                cmd.add(dockerInspectorJavaOptsParts[i]);
            }
        }
        cmd.add("-jar");
        cmd.add(String.format("/opt/blackduck/hub-docker-inspector/%s", programPaths.getHubDockerJarFilenameHost()));
        cmd.add(String.format("--spring.config.location=%s", "/opt/blackduck/hub-docker-inspector/config/application.properties"));
        cmd.add(String.format("--docker.tar=%s", tarFilePathInSubContainer));
        execCommandInContainer(dockerClient, imageNameTag, containerId, cmd);
        logger.debug(String.format("Container's output files are in %s because it was mounted under the container's output dir", programPaths.getHubDockerOutputPathHost()));
        return containerId;
    }

    public String startContainerAsService(final String imageId, final String containerName, final ImageInspectorOsEnum inspectorOs, final int containerPort, final int hostPort, final String containerPathToOutputDir,
            final String inspectorUrlAlpine, final String inspectorUrlCentos, final String inspectorUrlUbuntu)
            throws IntegrationException {
        logger.info(String.format("Starting container: %s", containerName));
        logger.debug(String.format("\tImage ID: %s", imageId));
        final DockerClient dockerClient = hubDockerClient.getDockerClient();
        stopRemoveContainerIfExists(dockerClient, containerName);

        logger.debug(String.format("Creating container %s from image %s", containerName, imageId));
        final String imageInspectorOsName = inspectorOs.name();
        final String cmd = String.format("java -jar /opt/blackduck/hub-imageinspector-ws/hub-imageinspector-ws.jar --server.port=%d --current.linux.distro=%s --inspector.url.alpine=%s --inspector.url.centos=%s --inspector.url.ubuntu=%s",
                containerPort,
                imageInspectorOsName, inspectorUrlAlpine, inspectorUrlCentos, inspectorUrlUbuntu);
        logger.debug(String.format("Starting service with cmd: %s", cmd));
        final Map<String, String> labels = new HashMap<>(1);
        labels.put(CONTAINER_APPNAME_LABEL_KEY, IMAGEINSPECTOR_APP_NAME_LABEL_VALUE);
        labels.put(CONTAINER_OS_LABEL_KEY, imageInspectorOsName);
        final Bind bind = createBindMount(config.getSharedDirPathLocal(), config.getSharedDirPathImageInspector());
        final CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageId).withName(containerName).withBinds(bind).withLabels(labels).withCmd(cmd.split(" "));
        final ExposedPort exposedPort = new ExposedPort(containerPort);
        createContainerCmd.withExposedPorts(exposedPort);
        final PortBinding portBinding = new PortBinding(Binding.bindPort(hostPort), exposedPort);
        createContainerCmd.withPortBindings(portBinding);
        final List<String> envAssignments = new ArrayList<>();
        if (StringUtils.isBlank(config.getHubProxyHost()) && !StringUtils.isBlank(config.getScanCliOptsEnvVar())) {
            envAssignments.add(String.format("SCAN_CLI_OPTS=%s", config.getScanCliOptsEnvVar()));
        }
        createContainerCmd.withEnv(envAssignments);
        final CreateContainerResponse containerResponse = createContainerCmd.exec();
        final String containerId = containerResponse.getId();

        dockerClient.startContainerCmd(containerId).exec();
        logger.debug(String.format("Started container %s from image %s", containerId, imageId));

        return containerId;
    }

    public void stopRemoveContainer(final String containerId) throws HubIntegrationException {
        final DockerClient dockerClient = hubDockerClient.getDockerClient();
        stopContainer(dockerClient, containerId);
        removeContainer(dockerClient, containerId);
    }

    public void logServiceLogAsDebug(final String containerId) {
        final StringBuilder stringBuilder = new StringBuilder();
        final StringBuilderLogReader callback = new StringBuilderLogReader(stringBuilder);
        DockerClient dockerClient;
        try {
            dockerClient = hubDockerClient.getDockerClient();
        } catch (final HubIntegrationException e1) {
            logger.debug(String.format("Error getting docker client: %s", e1.getMessage()));
            try {
                callback.close();
            } catch (final IOException e) {
            }
            return;
        }
        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdErr(true)
                    .withStdOut(true)
                    .withTailAll()
                    .exec(callback)
                    .awaitCompletion();
        } catch (final InterruptedException e) {
            logger.debug(String.format("Error getting container log: %s", e.getMessage()));
        }

        final String log = callback.builder.toString();
        logger.debug(String.format("Image inspector service log:\n%s", log));
        try {
            callback.close();
        } catch (final IOException e) {
        }
    }

    private static class StringBuilderLogReader extends LogContainerResultCallback {
        public StringBuilder builder;

        public StringBuilderLogReader(final StringBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void onNext(final Frame item) {
            builder.append(new String(item.getPayload()));
            super.onNext(item);
        }
    }

    private File saveImageToDir(final File imageTarDirectory, final String imageTarFilename, final String imageName, final String tagName) throws IOException, HubIntegrationException {
        final File imageTarFile = new File(imageTarDirectory, imageTarFilename);
        saveImageToFile(imageName, tagName, imageTarFile);
        return imageTarFile;
    }

    private Image getLocalImage(final DockerClient dockerClient, final String imageName, final String tagName) {
        Image localImage = null;
        final List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(imageName).exec();
        for (final Image image : images) {
            if (image == null) {
                logger.warn("Encountered a null image in local docker registry");
                continue;
            }
            final String[] tags = image.getRepoTags();
            if (tags == null) {
                logger.warn("Encountered an image with a null tag list in local docker registry");
            } else {
                for (final String tag : tags) {
                    if (tag == null) {
                        continue;
                    }
                    if (tag.contains(tagName)) {
                        localImage = image;
                        break;
                    }
                }
            }
            if (localImage != null) {
                break;
            }
        }
        return localImage;
    }

    private void removeContainer(final DockerClient dockerClient, final String containerId) {
        final RemoveContainerCmd rmCmd = dockerClient.removeContainerCmd(containerId);
        logger.debug(String.format("Removing container %s", containerId));
        rmCmd.exec();
        logger.debug(String.format("Container %s removed", containerId));
    }

    private void stopContainer(final DockerClient dockerClient, final String containerId) {
        final Long timeoutMilliseconds = config.getCommandTimeout();
        final int timeoutSeconds = (int) (timeoutMilliseconds / 60000L);
        logger.debug(String.format("Stopping container %s", containerId));
        final StopContainerCmd stopCmd = dockerClient.stopContainerCmd(containerId).withTimeout(timeoutSeconds);
        stopCmd.exec();
        logger.debug(String.format("Container %s stopped", containerId));
    }

    public String getDockerEngineVersion() {
        logger.debug("Requesting version string from Docker engine");
        try {
            final DockerClient dockerClient = hubDockerClient.getDockerClient();
            final Info dockerInfo = dockerClient.infoCmd().exec();
            final String engineVersion = dockerInfo.getServerVersion();
            logger.debug(String.format("Docker Engine (Server) Version: %s", engineVersion));
            if (engineVersion == null) {
                return "Unknown";
            } else {
                return engineVersion;
            }
        } catch (final HubIntegrationException e) {
            return "Unknown";
        }
    }

    public void copyFileToContainer(final DockerClient dockerClient, final String containerId, final String srcPath, final String destPath) throws IOException {
        logger.debug(String.format("Copying %s to container %s:%s", srcPath, containerId, destPath));
        try (final CopyArchiveToContainerCmd copyCmd = dockerClient.copyArchiveToContainerCmd(containerId).withHostResource(srcPath).withRemotePath(destPath)) {
            copyCmd.exec();
        }
    }

    private void setPropertiesInSubContainer(final DockerClient dockerClient, final String containerId, final String tarFilePathInSubContainer, final String tarFileDirInSubContainer, final File dockerTarFile, final String targetImage,
            final String targetImageRepo, final String targetImageTag) throws IOException, IllegalArgumentException, IllegalAccessException {
        logger.debug("Creating properties file inside container");
        hubDockerProperties.load();
        hubDockerProperties.set(IMAGE_TARFILE_PROPERTY, tarFilePathInSubContainer);
        hubDockerProperties.set(IMAGE_PROPERTY, targetImage);
        hubDockerProperties.set(IMAGE_REPO_PROPERTY, targetImageRepo);
        hubDockerProperties.set(IMAGE_TAG_PROPERTY, targetImageTag);
        hubDockerProperties.set(OUTPUT_INCLUDE_DOCKER_TARFILE_PROPERTY, "false");
        hubDockerProperties.set(OUTPUT_INCLUDE_CONTAINER_FILE_SYSTEM_TARFILE_PROPERTY, new Boolean(config.isOutputIncludeContainerfilesystem()).toString());
        hubDockerProperties.set(ON_HOST_PROPERTY, "false");
        hubDockerProperties.set(DETECT_PKG_MGR_PROPERTY, "true");
        hubDockerProperties.set(INSPECT_PROPERTY, "true");
        hubDockerProperties.set(INSPECT_IN_CONTAINER_PROPERTY, "false");
        hubDockerProperties.set(UPLOAD_BDIO_PROPERTY, "false");

        final String pathToPropertiesFileForSubContainer = String.format("%s%s", programPaths.getHubDockerTargetDirPathHost(), ProgramPaths.APPLICATION_PROPERTIES_FILENAME);
        hubDockerProperties.save(pathToPropertiesFileForSubContainer);

        copyFileToContainer(dockerClient, containerId, pathToPropertiesFileForSubContainer, programPaths.getHubDockerConfigDirPathContainer());

        logger.trace(String.format("Docker image tar file: %s", dockerTarFile.getAbsolutePath()));
        logger.trace(String.format("Docker image tar file path in sub-container: %s", tarFilePathInSubContainer));
    }

    private String prepareContainerForExec(final DockerClient dockerClient, final String imageId, final String extractorContainerName, final String hubPassword, final String hubApiToken) {
        stopRemoveContainerIfExists(dockerClient, extractorContainerName);
        logger.debug(String.format("Creating container %s from image %s", extractorContainerName, imageId));
        final Bind bind = createBindMount(programPaths.getHubDockerOutputPathHost(), programPaths.getHubDockerOutputPathContainer());
        final CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageId).withStdinOpen(true).withTty(true).withName(extractorContainerName).withBinds(bind).withCmd("/bin/bash");
        final CreateContainerResponse containerResponse = createContainerCmd.exec();
        final String containerId = containerResponse.getId();
        dockerClient.startContainerCmd(containerId).exec();
        logger.debug(String.format("Started container %s from image %s", containerId, imageId));
        return containerId;
    }

    private void stopRemoveContainerIfExists(final DockerClient dockerClient, final String extractorContainerName) {
        String oldContainerId;

        final Container extractorContainer = getRunningContainerByContainerName(dockerClient, extractorContainerName);
        if (extractorContainer != null) {
            logger.debug(String.format("Extractor container status: %s", extractorContainer.getStatus()));
            oldContainerId = extractorContainer.getId();
            if (extractorContainer.getStatus().startsWith("Up")) {
                logger.debug("The extractor container is running; stopping it");
                dockerClient.stopContainerCmd(oldContainerId).exec();
            }
            logger.debug("The extractor container exists; removing it");
            dockerClient.removeContainerCmd(oldContainerId).exec();
        }
    }

    private Bind createBindMount(final String pathOnHost, final String pathOnContainer) {
        logger.debug(String.format("Mounting host:%s to container:%s", pathOnHost, pathOnContainer));
        final File dirOnHost = new File(pathOnHost);
        final Volume volume = new Volume(pathOnContainer);
        final Bind bind = new Bind(dirOnHost.getAbsolutePath(), volume, AccessMode.rw);
        return bind;
    }

    public Container getRunningContainerByAppName(final DockerClient dockerClient, final String targetAppName, final ImageInspectorOsEnum targetInspectorOs) throws HubIntegrationException {
        final List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (final Container container : containers) {
            logger.debug(String.format("Checking container %s to see if it has labels app = %s, os = %s", container.getNames()[0], targetAppName, targetInspectorOs.name()));
            final String containerAppName = container.getLabels().get(CONTAINER_APPNAME_LABEL_KEY);
            final String containerOsName = container.getLabels().get(CONTAINER_OS_LABEL_KEY);
            logger.debug(String.format("Comparing app name %s to %s, os name %s to %s", targetAppName, containerAppName, targetInspectorOs.name(), containerOsName));
            if (targetAppName.equals(containerAppName) && targetInspectorOs.name().equalsIgnoreCase(containerOsName)) {
                logger.debug("\tIt's a match");
                return container;
            }
        }
        throw new HubIntegrationException(String.format("No running container found with app = %s, os = %s", targetAppName, targetInspectorOs.name()));
    }

    private Container getRunningContainerByContainerName(final DockerClient dockerClient, final String extractorContainerName) {
        Container extractorContainer = null;
        final List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (final Container container : containers) {
            for (final String name : container.getNames()) {
                // name prefixed with '/' for some reason
                logger.debug(String.format("Checking running container %s to see if it is %s", name, extractorContainerName));
                if (name.contains(extractorContainerName)) {
                    logger.debug("The extractor container already exists");
                    extractorContainer = container;
                    break;
                }
            }
            if (extractorContainer != null) {
                break;
            }
        }
        return extractorContainer;
    }

    private void execCommandInContainer(final DockerClient dockerClient, final String imageId, final String containerId, final List<String> cmd) throws InterruptedException {
        logger.info(String.format("Running %s in container %s from image %s", cmd.get(0), containerId, imageId));
        final ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerId).withAttachStdout(true).withAttachStderr(true);
        // final String[] cmdArr = (String[]) cmd.toArray();
        final String[] cmdArr = new String[cmd.size()];
        for (int i = 0; i < cmd.size(); i++) {
            logger.debug(String.format("cmdArr[%d]=%s", i, cmd.get(i)));
            cmdArr[i] = cmd.get(i);
        }
        execCreateCmd.withCmd(cmdArr);
        final ExecCreateCmdResponse execCreateCmdResponse = execCreateCmd.exec();
        logger.debug("Invoking container appropriate for this target image");
        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
        logger.debug("The container execution has completed");
    }

    private void saveImageToFile(final String imageName, final String tagName, final File imageTarFile) throws IOException, HubIntegrationException {
        InputStream tarInputStream = null;
        try {
            logger.info(String.format("Saving the docker image to : %s", imageTarFile.getCanonicalPath()));
            final DockerClient dockerClient = hubDockerClient.getDockerClient();
            final String imageToSave = String.format("%s:%s", imageName, tagName);
            final SaveImageCmd saveCommand = dockerClient.saveImageCmd(imageToSave);
            tarInputStream = saveCommand.exec();
            FileUtils.copyInputStreamToFile(tarInputStream, imageTarFile);
        } finally {
            IOUtils.closeQuietly(tarInputStream);
        }
    }
}
