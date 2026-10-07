package net.es.nsi.pce.pf;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** The avoidLoops switch on the dijkstraPCE bean in the shipped config/beans.xml, and its default. */
@RunWith(Parameterized.class)
public class DijkstraPCEConfigTest {
    private static final String PROPERTY = "<property name=\"avoidLoops\" value=\"true\" />";

    @Parameter(0)
    public String name;
    @Parameter(1)
    public String property;
    @Parameter(2)
    public boolean expected;

    @Parameters(name = "{0}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
            {"as shipped", PROPERTY, true},
            {"switched off", PROPERTY.replace("true", "false"), false},
            {"property removed", "", true},
        });
    }

    @Test
    public void readsAvoidLoopsFromBeansXml() throws Exception {
        String shipped = new String(Files.readAllBytes(Paths.get("config/beans.xml")), StandardCharsets.UTF_8);
        assertThat("config/beans.xml sets avoidLoops", shipped.contains(PROPERTY), is(true));

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
                new ByteArrayResource(shipped.replace(PROPERTY, property).getBytes(StandardCharsets.UTF_8)));
        assertThat(factory.getBean("dijkstraPCE", DijkstraPCE.class).isAvoidLoops(), is(expected));
    }
}
