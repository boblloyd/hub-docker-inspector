package com.blackducksoftware.integration.hub.docker.dockerinspector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.imageinspector.name.Names;
import com.blackducksoftware.integration.test.annotation.IntegrationTest;

@Category(IntegrationTest.class)
public class DockerInspectorTest {
    private static int IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE = 8080;
    private static int IMAGE_INSPECTOR_PORT_IN_CONTAINER_ALPINE = 8080;
    private static int IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS = 8081;
    private static int IMAGE_INSPECTOR_PORT_IN_CONTAINER_CENTOS = 8081;
    private static int IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU = 8082;
    private static int IMAGE_INSPECTOR_PORT_IN_CONTAINER_UBUNTU = 8082;

    private static int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE = 8100;
    private static int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS = 8101;
    private static int START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU = 8102;

    private static String SHARED_DIR_PATH_IN_CONTAINER = "/opt/blackduck/shared";

    private static File dirSharedWithContainer;
    private static File containerTargetDir;
    private static File containerOutputDir;

    private static ProgramVersion programVersion;
    private static String dateTimeStamp;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dateTimeStamp = getTimestamp();
        programVersion = new ProgramVersion();
        programVersion.init();
        printDockerVersion();
        System.out.printf("Running containers:\n%s\n", getRunningContainers(false));
        System.out.printf("All containers:\n%s\n", getAllContainers(false));
        removeDockerInspectorContainers();
        System.out.printf("All containers:\n%s\n", getAllContainers(false));
        startContainer("alpine", IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE, IMAGE_INSPECTOR_PORT_IN_CONTAINER_ALPINE);
        startContainer("centos", IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS, IMAGE_INSPECTOR_PORT_IN_CONTAINER_CENTOS);
        startContainer("ubuntu", IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU, IMAGE_INSPECTOR_PORT_IN_CONTAINER_UBUNTU);

        boolean alpineUp = false;
        boolean centosUp = false;
        boolean ubuntuUp = false;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(10000L);
            if (!alpineUp) {
                alpineUp = isUp(IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE);
            }
            if (!centosUp) {
                centosUp = isUp(IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS);
            }
            if (!ubuntuUp) {
                ubuntuUp = isUp(IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU);
            }
            if (alpineUp && centosUp && ubuntuUp) {
                break;
            }
        }
        assertTrue(alpineUp && centosUp && ubuntuUp);

        final File testDir = new File(TestUtils.TEST_DIR_REL_PATH);
        dirSharedWithContainer = new File(testDir, "containerShared");
        containerTargetDir = new File(dirSharedWithContainer, "target");
        containerOutputDir = new File(dirSharedWithContainer, "output");

        createWriteableDirTolerantly(testDir);
        createWriteableDirTolerantly(dirSharedWithContainer);
        createWriteableDirTolerantly(containerTargetDir);
        createWriteableDirTolerantly(containerOutputDir);
    }

    private static String getTimestamp() {
        String timestamp = Long.toString(new Date().getTime());
        final int len = timestamp.length();
        if (len > 8) {
            timestamp = timestamp.substring(len - 8);
        }
        return timestamp;
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        cleanUpContainers();
    }

    private static void cleanUpContainers() throws InterruptedException {
        stopContainer("alpine");
        stopContainer("centos");
        stopContainer("ubuntu");
        Thread.sleep(30000L);
        removeContainer("alpine");
        removeContainer("centos");
        removeContainer("ubuntu");
        Thread.sleep(10000L);
        ensureContainerRemoved("alpine");
        ensureContainerRemoved("centos");
        ensureContainerRemoved("ubuntu");
    }

    @Test
    public void testUbuntuExec() throws IOException, InterruptedException, IntegrationException {
        testImage("ubuntu:17.04", "ubuntu", "17.04", "var_lib_dpkg", true, false);
    }

    @Test
    public void testUbuntuStartContainer() throws IOException, InterruptedException, IntegrationException {
        testImage("ubuntu:17.04", "ubuntu", "17.04", "var_lib_dpkg", true, true);
    }

    @Test
    public void testAlpineExec() throws IOException, InterruptedException, IntegrationException {
        testImage("alpine:3.6", "alpine", "3.6", "lib_apk", true, false);
    }

    @Test
    public void testAlpineStartContainer() throws IOException, InterruptedException, IntegrationException {
        testImage("alpine:3.6", "alpine", "3.6", "lib_apk", true, true);
    }

    @Test
    public void testBusyboxExec() throws IOException, InterruptedException, IntegrationException {
        testImage("busybox:latest", "busybox", "latest", "noPkgMgr", true, false);
    }

    @Test
    public void testBusyboxStartContainer() throws IOException, InterruptedException, IntegrationException {
        testImage("busybox:latest", "busybox", "latest", "noPkgMgr", true, true);
    }

    @Test
    public void testAlpineLatestExec() throws IOException, InterruptedException, IntegrationException {
        testImage("alpine", "alpine", "latest", "lib_apk", false, false);
    }

    @Test
    public void testAlpineLatestStartContainer() throws IOException, InterruptedException, IntegrationException {
        testImage("alpine", "alpine", "latest", "lib_apk", false, true);
    }

    @Test
    public void testCentosExec() throws IOException, InterruptedException, IntegrationException {
        testImage("centos:7.3.1611", "centos", "7.3.1611", "var_lib_rpm", true, false);
    }

    @Test
    public void testCentosStartContainer() throws IOException, InterruptedException, IntegrationException {
        testImage("centos:7.3.1611", "centos", "7.3.1611", "var_lib_rpm", true, true);
    }

    @Test
    public void testHubWebappExec() throws IOException, InterruptedException, IntegrationException {
        testImage("blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0", "lib_apk", true, false);
    }

    @Test
    public void testHubWebappStartContainer() throws IOException, InterruptedException, IntegrationException {
        testImage("blackducksoftware/hub-webapp:4.0.0", "blackducksoftware_hub-webapp", "4.0.0", "lib_apk", true, true);
    }

    @Test
    public void testHubZookeeperExec() throws IOException, InterruptedException, IntegrationException {
        testImage("blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0", "lib_apk", true, false);
    }

    @Test
    public void testHubZookeeperStartContainer() throws IOException, InterruptedException, IntegrationException {
        testImage("blackducksoftware/hub-zookeeper:4.0.0", "blackducksoftware_hub-zookeeper", "4.0.0", "lib_apk", true, true);
    }

    @Test
    public void testTomcatExec() throws IOException, InterruptedException, IntegrationException {
        testImage("tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", "var_lib_dpkg", true, false);
    }

    @Test
    public void testTomcatStartContainer() throws IOException, InterruptedException, IntegrationException {
        testImage("tomcat:6.0.53-jre7", "tomcat", "6.0.53-jre7", "var_lib_dpkg", true, true);
    }

    @Test
    public void testRhelExec() throws IOException, InterruptedException, IntegrationException {
        testImage("dnplus/rhel:6.5", "dnplus_rhel", "6.5", "var_lib_rpm", true, false);
    }

    @Test
    public void testRhelStartContainer() throws IOException, InterruptedException, IntegrationException {
        testImage("dnplus/rhel:6.5", "dnplus_rhel", "6.5", "var_lib_rpm", true, true);
    }

    @Test
    public void testWhiteoutExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("whiteouttest.tar");
        testTar("build/images/test/whiteouttest.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_dpkg", true, false, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testWhiteoutStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("whiteouttest.tar");
        testTar("build/images/test/whiteouttest.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_dpkg", true, true, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAggregateTarfileImageOneExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        testTar("build/images/test/aggregated.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_dpkg", true, false, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAggregateTarfileImageOneStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/whiteouttest";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        testTar("build/images/test/aggregated.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_dpkg", true, true, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAggregateTarfileImageTwoExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        testTar("build/images/test/aggregated.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_rpm", true, false, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAggregateTarfileImageTwoStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String tag = "1.0";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("aggregated.tar");
        testTar("build/images/test/aggregated.tar", repo.replaceAll("/", "_"), repo, tag, tag, "var_lib_rpm", true, true, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecifiedExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        testTar("build/images/test/alpine.tar", repo.replaceAll("/", "_"), repo, tag, tag, "lib_apk", false, true, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAlpineLatestTarRepoTagSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = "latest";
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        testTar("build/images/test/alpine.tar", repo.replaceAll("/", "_"), repo, tag, tag, "lib_apk", false, true, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecifiedExec() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = null;
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        testTar("build/images/test/alpine.tar", repo, tag, null, "latest", "lib_apk", false, false, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAlpineLatestTarRepoTagNotSpecifiedStartContainer() throws IOException, InterruptedException, IntegrationException {
        final String repo = "alpine";
        final String tag = null;
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromTarFilename("alpine.tar");
        testTar("build/images/test/alpine.tar", repo, tag, null, "latest", "lib_apk", false, true, null, true, outputContainerFileSystemFile);
    }

    @Test
    public void testAlpineUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "alpine";
        final String targetTag = "3.6";
        final String targetPkgMgrLib = "lib_apk";
        final String tarFileBaseName = "alpine36";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        final String imageInspectorPlatform = "alpine";
        testUsingExistingContainer(targetRepo, targetTag, targetPkgMgrLib, tarFileBaseName, imageInspectorPlatform, portOnHost);
    }

    @Test
    public void testWhiteoutUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "blackducksoftware/whiteouttest";
        final String targetTag = "1.0";
        final String targetPkgMgrLib = "var_lib_dpkg";
        final String tarFileBaseName = "whiteouttest";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        final String imageInspectorPlatform = "alpine";
        testUsingExistingContainer(targetRepo, targetTag, targetPkgMgrLib, tarFileBaseName, imageInspectorPlatform, portOnHost);
    }

    @Test
    public void testCentosUsingExistingAlpineContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "blackducksoftware/centos_minus_vim_plus_bacula";
        final String targetTag = "1.0";
        final String targetPkgMgrLib = "var_lib_rpm";
        final String tarFileBaseName = "centos_minus_vim_plus_bacula";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE;
        final String imageInspectorPlatform = "alpine";
        testUsingExistingContainer(targetRepo, targetTag, targetPkgMgrLib, tarFileBaseName, imageInspectorPlatform, portOnHost);
    }

    @Test
    public void testUbuntuUsingExistingCentosContainer() throws IOException, InterruptedException, IntegrationException {
        final String targetRepo = "ubuntu";
        final String targetTag = "14.04";
        final String targetPkgMgrLib = "var_lib_dpkg";
        final String tarFileBaseName = "ubuntu1404";
        final int portOnHost = IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS;
        final String imageInspectorPlatform = "centos";
        testUsingExistingContainer(targetRepo, targetTag, targetPkgMgrLib, tarFileBaseName, imageInspectorPlatform, portOnHost);
    }

    @Test
    public void testPullJar() throws IOException, InterruptedException, IntegrationException {
        final File workingDir = new File(String.format("%s/pulljar", TestUtils.TEST_DIR_REL_PATH));
        FileUtils.deleteDirectory(workingDir);
        workingDir.mkdirs();
        System.out.println(String.format("workingDir: %s", workingDir.getAbsolutePath()));
        final FilenameFilter jarFileFilter = getJarFilenameFilter();
        final File[] jarFilesBefore = workingDir.listFiles(jarFileFilter);
        assertTrue(String.format("%s should be an empty directory", workingDir.getAbsolutePath()), jarFilesBefore.length == 0);

        final File script = new File("build/hub-docker-inspector.sh");
        final List<String> partialCmd = Arrays.asList(script.getAbsolutePath(), "--pulljar");
        // Arrays.asList returns a fixed size list; need a variable sized list
        final List<String> fullCmd = new ArrayList<>();
        fullCmd.addAll(partialCmd);

        System.out.println(String.format("Running --pulljar end to end test"));
        TestUtils.execCmd(workingDir, String.join(" ", fullCmd), 30000L, true);
        System.out.println("hub-docker-inspector --pulljar done; verifying results...");

        final File[] jarFilesAfter = workingDir.listFiles(jarFileFilter);
        final boolean foundOne = jarFilesAfter.length == 1;
        for (final File jarFile : jarFilesAfter) {
            System.out.println(String.format("Found jar file: %s", jarFile.getName()));
            jarFile.delete();
        }
        assertTrue("Expected a single pulled jar file", foundOne);
    }

    private void testUsingExistingContainer(final String targetRepo, final String targetTag, final String targetPkgMgrLib, final String tarFileBaseName, final String imageInspectorPlatform, final int portOnHost)
            throws IOException, InterruptedException, IntegrationException {

        final String tarFileName = String.format("%s.tar", tarFileBaseName);
        final File targetTar = new File(containerTargetDir, tarFileName);
        FileUtils.copyFile(new File(String.format("build/images/test/%s", tarFileName)), targetTar);
        targetTar.setReadable(true, false);
        final List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add(String.format("--imageinspector.service.url=http://localhost:%d", portOnHost));
        additionalArgs.add(String.format("--shared.dir.path.local=%s", dirSharedWithContainer.getAbsolutePath()));
        additionalArgs.add(String.format("--shared.dir.path.imageinspector=%s", SHARED_DIR_PATH_IN_CONTAINER));
        final File outputContainerFileSystemFile = new File(String.format("%s/output/%s_containerfilesystem.tar.gz", TestUtils.TEST_DIR_REL_PATH, tarFileBaseName));
        testTar(targetTar.getAbsolutePath(), targetRepo, null, null, targetTag, targetPkgMgrLib, true, false, additionalArgs, false, outputContainerFileSystemFile);
    }

    private FilenameFilter getJarFilenameFilter() {
        final FilenameFilter jarFileFilter = (dir, name) -> {
            if (name.endsWith(".jar")) {
                return true;
            } else {
                return false;
            }
        };
        return jarFileFilter;
    }

    private File getOutputContainerFileSystemFileFromTarFilename(final String tarFilename) {
        final String path = String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, Names.getContainerFileSystemTarFilename(null, tarFilename));
        System.out.println(String.format("Expecting output container filesystem file at: %s", path));
        return new File(path);
    }

    private File getOutputContainerFileSystemFileFromImageSpec(final String imageNameTag) {
        final String path = String.format("%s/output/%s", TestUtils.TEST_DIR_REL_PATH, Names.getContainerFileSystemTarFilename(imageNameTag, null));
        System.out.println(String.format("Expecting output container filesystem file at: %s", path));
        return new File(path);
    }

    private void testTar(final String inspectTargetTarfile, final String imageForBdioFilename, final String repo, final String tag, final String tagForBdioFilename, final String pkgMgrPathString, final boolean requireBdioMatch,
            final boolean startContainersAsNeeded,
            final List<String> additionalArgs, final boolean needWorkingDir, final File outputContainerFileSystemFile)
            throws IOException, InterruptedException, IntegrationException {

        final String inspectTargetArg = String.format("--docker.tar=%s", inspectTargetTarfile);

        ensureFileDoesNotExist(outputContainerFileSystemFile);

        final File actualBdio = new File(
                String.format(String.format("%s/output/%s_%s_%s_%s_bdio.jsonld", TestUtils.TEST_DIR_REL_PATH, imageForBdioFilename.replaceAll("/", "_"), pkgMgrPathString, imageForBdioFilename.replaceAll("/", "_"), tagForBdioFilename)));
        ensureFileDoesNotExist(actualBdio);

        final List<String> cmd = new ArrayList<>();
        cmd.add("build/hub-docker-inspector.sh");
        cmd.add("--upload.bdio=false");
        cmd.add(String.format("--jar.path=build/libs/hub-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        cmd.add(String.format("--output.path=%s/output", TestUtils.TEST_DIR_REL_PATH));
        cmd.add("--output.include.containerfilesystem=true");
        cmd.add("--hub.always.trust.cert=true");
        if (repo != null) {
            cmd.add(String.format("--docker.image.repo=%s", repo));
        }
        if (tag != null) {
            cmd.add(String.format("--docker.image.tag=%s", tag));
        }
        cmd.add("--logging.level.com.blackducksoftware=DEBUG");
        if (needWorkingDir) {
            final File workingDir = new File(String.format("%s/endToEnd", TestUtils.TEST_DIR_REL_PATH));
            TestUtils.deleteDirIfExists(workingDir);
            cmd.add(String.format("--working.dir.path=%s", workingDir.getAbsolutePath()));
        }
        cmd.add(inspectTargetArg);
        if (startContainersAsNeeded) {
            cmd.add("--imageinspector.service.start=true");
            cmd.add(String.format("--imageinspector.service.port.alpine=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE));
            cmd.add(String.format("--imageinspector.service.port.centos=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS));
            cmd.add(String.format("--imageinspector.service.port.ubuntu=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU));
        }
        if (additionalArgs != null && additionalArgs.size() > 0) {
            cmd.addAll(additionalArgs);
        }
        System.out.println(String.format("Running end to end test on %s with command %s", inspectTargetTarfile, cmd.toString()));
        TestUtils.execCmd(String.join(" ", cmd), 240000L, true);
        System.out.println("hub-docker-inspector done; verifying results...");
        System.out.printf("Expecting output BDIO file: %s\n", actualBdio.getAbsolutePath());
        assertTrue(actualBdio.exists());
        if (requireBdioMatch) {
            final File expectedBdio = new File(
                    String.format(String.format("src/test/resources/bdio/%s_%s_%s_%s_bdio.jsonld", imageForBdioFilename.replaceAll("/", "_"), pkgMgrPathString, imageForBdioFilename.replaceAll("/", "_"), tagForBdioFilename)));
            final List<String> exceptLinesContainingThese = new ArrayList<>();
            exceptLinesContainingThese.add("\"@id\":");
            exceptLinesContainingThese.add("spdx:created");
            exceptLinesContainingThese.add("Tool:");
            final boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
            assertTrue(outputBdioMatches);
        }

        assertTrue(String.format("%s does not exist", outputContainerFileSystemFile.getAbsolutePath()), outputContainerFileSystemFile.exists());
    }

    private void testImage(final String inspectTargetImageRepoTag, final String repo, final String tag, final String pkgMgrPathString, final boolean requireBdioMatch, final boolean startContainersAsNeeded)
            throws IOException, InterruptedException, IntegrationException {
        final File outputContainerFileSystemFile = getOutputContainerFileSystemFileFromImageSpec(inspectTargetImageRepoTag);
        final String inspectTargetArg = String.format("--docker.image=%s", inspectTargetImageRepoTag);
        ensureFileDoesNotExist(outputContainerFileSystemFile);
        final File actualBdio = new File(String.format(String.format("%s/output/%s_%s_%s_%s_bdio.jsonld", TestUtils.TEST_DIR_REL_PATH, repo, pkgMgrPathString, repo, tag)));
        ensureFileDoesNotExist(actualBdio);

        final List<String> cmd = new ArrayList<>();
        cmd.add("build/hub-docker-inspector.sh");
        cmd.add("--upload.bdio=false");
        cmd.add(String.format("--jar.path=build/libs/hub-docker-inspector-%s.jar", programVersion.getProgramVersion()));
        cmd.add(String.format("--output.path=%s/output", TestUtils.TEST_DIR_REL_PATH));
        cmd.add("--output.include.containerfilesystem=true");
        cmd.add("--hub.always.trust.cert=true");
        if (repo != null) {
            cmd.add(String.format("--docker.image.repo=%s", repo));
        }
        if (tag != null) {
            cmd.add(String.format("--docker.image.tag=%s", tag));
        }
        cmd.add("--logging.level.com.blackducksoftware=DEBUG");
        if (startContainersAsNeeded) {
            cmd.add("--imageinspector.service.start=true");
            cmd.add(String.format("--imageinspector.service.port.alpine=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_ALPINE));
            cmd.add(String.format("--imageinspector.service.port.centos=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_CENTOS));
            cmd.add(String.format("--imageinspector.service.port.ubuntu=%d", START_AS_NEEDED_IMAGE_INSPECTOR_PORT_ON_HOST_UBUNTU));
            cmd.add(String.format("--shared.dir.path.local=%s/containerShared", TestUtils.TEST_DIR_REL_PATH));
        } else {
            final File workingDir = new File(String.format("%s/endToEnd", TestUtils.TEST_DIR_REL_PATH));
            TestUtils.deleteDirIfExists(workingDir);
            cmd.add(String.format("--working.dir.path=%s", workingDir.getAbsolutePath()));
        }
        cmd.add(inspectTargetArg);

        System.out.println(String.format("Running end to end test on %s with command %s", inspectTargetImageRepoTag, cmd.toString()));
        TestUtils.execCmd(String.join(" ", cmd), 30000L, true);
        System.out.println("hub-docker-inspector done; verifying results...");
        System.out.printf("Expecting output BDIO file: %s\n", actualBdio.getAbsolutePath());
        assertTrue(actualBdio.exists());
        if (requireBdioMatch) {
            final File expectedBdio = new File(String.format(String.format("src/test/resources/bdio/%s_%s_%s_%s_bdio.jsonld", repo, pkgMgrPathString, repo, tag)));
            final List<String> exceptLinesContainingThese = new ArrayList<>();
            exceptLinesContainingThese.add("\"@id\":");
            exceptLinesContainingThese.add("spdx:created");
            exceptLinesContainingThese.add("Tool:");
            final boolean outputBdioMatches = TestUtils.contentEquals(expectedBdio, actualBdio, exceptLinesContainingThese);
            assertTrue(outputBdioMatches);
        }

        assertTrue(outputContainerFileSystemFile.exists());
    }

    private void ensureFileDoesNotExist(final File outputContainerFileSystemFile) throws IOException {
        Files.deleteIfExists(outputContainerFileSystemFile.toPath());
        assertFalse(outputContainerFileSystemFile.exists());
    }

    private static void createWriteableDirTolerantly(final File dir) {
        System.out.printf("Creating and setting a+wx permission on: %s\n", dir.getAbsolutePath());
        createDirTolerantly(dir);
        setWriteExecutePermissionsTolerantly(dir);
        logPermissions(dir);
    }

    private static void logPermissions(final File dir) {
        Set<PosixFilePermission> perms = null;
        try {
            perms = Files.getPosixFilePermissions(dir.toPath());
            System.out.printf("* Dir %s now has perms: %s\n", dir.getAbsolutePath(), perms.toString());
        } catch (final IOException e) {
            System.out.printf("Unable to read back perms for dir %s: %s\n", dir.getAbsolutePath(), e.getMessage());
        }
    }

    private static void createDirTolerantly(final File dir) {
        try {
            dir.mkdirs();
        } catch (final Exception e) {
            System.out.printf("Error creating directory %s: %s\n", dir.getAbsoluteFile(), e.getMessage());
        }
        if (!dir.exists()) {
            System.out.printf("ERROR: Attempted to create directory %s, but it still does not exist\n", dir.getAbsoluteFile());
        }
    }

    private static void setWriteExecutePermissionsTolerantly(final File file) {
        try {
            file.setWritable(true, false);
        } catch (final Exception e) {
            System.out.printf("Error making directory %s writeable: %s\n", file.getAbsolutePath(), e.getMessage());
        }
        try {
            file.setExecutable(true, false);
        } catch (final Exception e) {
            System.out.printf("Error making directory %s writeable: %s\n", file.getAbsolutePath(), e.getMessage());
        }
    }

    private static boolean isUp(final int port) {
        String response;
        try {
            response = TestUtils.execCmd(String.format("curl -i http://localhost:%d/health", port), 30000L, true);
        } catch (IOException | InterruptedException | IntegrationException e) {
            return false;
        }
        if (response.startsWith("HTTP/1.1 200")) {
            return true;
        }
        return false;
    }

    private static void startContainer(final String imageInspectorPlatform, final int portOnHost, final int portInContainer) throws IOException, InterruptedException, IntegrationException {
        final String containerName = getContainerName(imageInspectorPlatform);
        final String cmd = String.format("docker run -d -t --name %s -p %d:%d -v \"$(pwd)\"/%s/containerShared:%s blackducksoftware/%s-%s:%s",
                containerName, portOnHost,
                portInContainer,
                TestUtils.TEST_DIR_REL_PATH,
                SHARED_DIR_PATH_IN_CONTAINER,
                programVersion.getInspectorImageFamily(), imageInspectorPlatform, programVersion.getInspectorImageVersion());
        TestUtils.execCmd(cmd, 120000L, true);
    }

    private static String getContainerName(final String imageInspectorPlatform) {
        return String.format("dockerInspectorTestImageInspector_%s_%s", imageInspectorPlatform, dateTimeStamp);
    }

    private static void printDockerVersion() {
        try {
            TestUtils.execCmd("docker version", 20000L, true);
        } catch (final Exception e) {
            System.out.printf("Error running docker version command: %s\n", e.getMessage());
        }
    }

    private static void stopContainer(final String imageInspectorPlatform) {
        final String containerName = getContainerName(imageInspectorPlatform);
        try {
            TestUtils.execCmd(String.format("docker stop %s", containerName), 120000L, true);
        } catch (final Exception e) {
            System.out.printf("Error stopping container %s: %s\n", containerName, e.getMessage());
        }
    }

    private static void removeContainer(final String imageInspectorPlatform) {
        final String containerName = getContainerName(imageInspectorPlatform);
        try {
            TestUtils.execCmd(String.format("docker rm -f %s", containerName), 120000L, true);
        } catch (final Exception e) {
            System.out.printf("Error removing container %s: %s\n", containerName, e.getMessage());
        }
    }

    private static void ensureContainerRemoved(final String imageInspectorPlatform) {
        final String containerName = getContainerName(imageInspectorPlatform);
        String dockerPsResponse;
        boolean containerStillExists = true;
        for (int tryCount = 0; tryCount < 20; tryCount++) {
            System.out.printf("Checking to see if container %s was removed\n", containerName);
            try {
                dockerPsResponse = getAllContainers(true);
                if (!dockerPsResponse.contains(containerName)) {
                    containerStillExists = false;
                    System.out.printf("Container %s was removed\n", containerName);
                    break;
                }
                Thread.sleep(5000L);
            } catch (final Exception e) {
                System.out.printf("Error stopping container %s: %s\n", containerName, e.getMessage());
            }
        }
        if (containerStillExists) {
            System.out.printf("ERROR: Failed to remove container %s\n", containerName);
        }
    }

    private static String getAllContainers(final boolean logStdout) throws IOException, InterruptedException, IntegrationException {
        return TestUtils.execCmd("docker ps -a", 120000L, logStdout);
    }

    private static String getRunningContainers(final boolean logStdout) throws IOException, InterruptedException, IntegrationException {
        return TestUtils.execCmd("docker ps", 120000L, logStdout);
    }

    private static void removeDockerInspectorContainers() throws IOException, InterruptedException, IntegrationException {
        System.out.println("Stopping/Removing docker inspector containers");
        final String psAllOutput = TestUtils.execCmd("docker ps -a", 120000L, false);
        final String[] lines = psAllOutput.split("\n");
        for (final String line : lines) {
            System.out.printf("Line: %s\n", line);
            if (line.startsWith("CONTAINER")) {
                continue;
            }
            final String[] fields = line.split("\\s+");
            final String containerName = fields[fields.length - 1];
            System.out.printf("Container name: %s\n", containerName);
            if (containerName.startsWith("hub-imageinspector-ws-alpine_") || containerName.startsWith("hub-imageinspector-ws-centos_") || containerName.startsWith("hub-imageinspector-ws-ubuntu_")) {
                TestUtils.execCmd(String.format("docker stop %s", containerName), 120000L, false);
                Thread.sleep(10000L);
                TestUtils.execCmd(String.format("docker rm -f %s", containerName), 120000L, false);
            }
        }
    }

}
