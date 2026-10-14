package net.es.nsi.pce.config;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** The command lines the PCE and the TopologyViewer are started with, the Helm chart's included. */
@RunWith(Parameterized.class)
public class StartupArgsTest {
    @Parameter(0)
    public String name;
    @Parameter(1)
    public String[] args;
    @Parameter(2)
    public String configPath;
    @Parameter(3)
    public String topologyConfigFile;

    @Parameters(name = "{0}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
            {"Helm chart", new String[] {"-topologyConfigFile=/nsi-pce/config/topology-dds.xml"},
                "config/", "/nsi-pce/config/topology-dds.xml"},
            {"run.sh", new String[] {"-topologyConfigFile", "config/topology-dds.xml"}, "config/", "config/topology-dds.xml"},
            {"config directory", new String[] {"-c", "/etc/pce/"}, "/etc/pce/", "config/topology-dds.xml"},
            {"config directory attached, no slash", new String[] {"-c/etc/pce"}, "/etc/pce/", "config/topology-dds.xml"},
            {"none", new String[] {}, "config/", "config/topology-dds.xml"},
        });
    }

    @Test
    public void setsTheSystemProperties() throws Exception {
        assertEquals(configPath, StartupArgs.apply(StartupArgs.parse(args)));
        assertEquals(configPath, System.getProperty(StartupArgs.CONFIG_PATH));
        assertEquals(topologyConfigFile, System.getProperty(StartupArgs.TOPOLOGY_CONFIG_FILE_ARGNAME));
    }
}
