package net.es.nsi.pce.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The topology viewer loads a topology through the Spring context, builds its JUNG graph and renders
 * into an image. JUNG's mouse plugins need a display, so this is skipped without one; CI runs it
 * under Xvfb.
 */
public class TopologyViewerTest {
    private static TopologyViewer viewer;

    @BeforeClass
    public static void build() throws Exception {
        Assume.assumeFalse("needs a display", GraphicsEnvironment.isHeadless());
        System.setProperty("configPath", "src/test/resources/config/");
        System.setProperty("topologyConfigFile", "src/test/resources/config/topology-api.xml");
        viewer = new TopologyViewer("src/test/resources/config/");
    }

    @Test
    public void hasAVertexPerNetwork() {
        for (String domain : new String[] {"domain-a", "domain-b", "domain-c"}) {
            assertNotNull(domain, viewer.getNetworkByName(domain));
        }
        assertNull(viewer.getNetworkByName("domain-d"));
    }

    @Test
    public void rendersWithoutADisplay() {
        viewer.setSize(800, 600);
        viewer.doLayout();
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        viewer.paint(image.getGraphics());
        assertEquals(800, image.getWidth());
    }
}
