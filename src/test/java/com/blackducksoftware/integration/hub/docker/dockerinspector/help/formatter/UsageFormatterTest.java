package com.blackducksoftware.integration.hub.docker.dockerinspector.help.formatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import com.blackducksoftware.integration.hub.docker.dockerinspector.config.Config;
import com.blackducksoftware.integration.hub.docker.dockerinspector.config.DockerInspectorOption;

@RunWith(SpringRunner.class)
public class UsageFormatterTest {

    @InjectMocks
    private UsageFormatter usageFormatter;

    @Mock
    private Config config;

    @Test
    public void test() throws IllegalArgumentException, IllegalAccessException, IOException {
        final SortedSet<DockerInspectorOption> configOptions = new TreeSet<>();
        configOptions.add(new DockerInspectorOption("hub.url", "hubUrl", "testHubUrl", "Hub URL", String.class, "", "public", false));
        Mockito.when(config.getPublicConfigOptions()).thenReturn(configOptions);

        final List<String> usageStrings = usageFormatter.getStringList();
        assertTrue(usageStrings.size() >= 16);
        assertEquals("Usage: hub-docker-inspector.sh <options>", usageStrings.get(0));
        assertEquals("options: any supported property can be set by adding to the command line", usageStrings.get(1));
        final String usageString = StringUtils.join(usageStrings, ";");
        assertTrue(usageString.contains("hub.url [String]: Hub URL"));
    }

}
