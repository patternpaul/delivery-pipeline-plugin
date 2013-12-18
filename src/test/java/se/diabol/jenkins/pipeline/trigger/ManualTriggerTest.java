package se.diabol.jenkins.pipeline.trigger;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.util.StreamTaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.WithoutJenkins;
import se.diabol.jenkins.pipeline.PipelineFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ManualTriggerTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();


    @Test
    public void triggerSimple() throws Exception {
        FreeStyleProject a = jenkins.createFreeStyleProject("a");
        FreeStyleProject b = jenkins.createFreeStyleProject("b");
        ManualTrigger trigger = new ManualTrigger("b");
        a.getPublishersList().add(trigger);
        jenkins.getInstance().rebuildDependencyGraph();

        jenkins.buildAndAssertSuccess(a);
        jenkins.waitUntilNoActivity();
        assertNotNull(a.getLastBuild());
        assertNull(b.getLastBuild());

        trigger.trigger(a.getLastBuild(), b);
        jenkins.waitUntilNoActivity();

        assertNotNull(b.getLastBuild());

        //TODO move tests to correct testclass
        assertTrue(PipelineFactory.isManualTrigger(b));
        assertFalse(PipelineFactory.isManualTrigger(a));


        List<CauseAction> causeActions =  b.getLastBuild().getActions(CauseAction.class);
        assertEquals(2, causeActions.size());
        Cause cause1 = causeActions.get(0).getCauses().get(0);
        assertTrue(cause1 instanceof Cause.UserCause);
        //assertEquals(causeActions.get(0).getCauses().get(0));
    }

    @Test
    public void testParameters() throws Exception {
        FreeStyleProject a = jenkins.createFreeStyleProject("a");
        FreeStyleProject b = jenkins.createFreeStyleProject("b");
        List<AbstractBuildParameters> params = new ArrayList<AbstractBuildParameters>();
        params.add(new PredefinedBuildParameters("PARAM1=$BUILD_NUMBER"));

        ManualTriggerConfig config = new ManualTriggerConfig("b", ResultCondition.SUCCESS, false, params);
        List<ManualTriggerConfig> configs = new ArrayList<ManualTriggerConfig>();
        configs.add(config);
        ManualTrigger trigger = new ManualTrigger(configs);
        a.getPublishersList().add(trigger);


        ParameterDefinition parameter = new StringParameterDefinition("PARAM1", "", "");

        ParametersDefinitionProperty parameterProperty = new ParametersDefinitionProperty(parameter);

        b.addProperty(parameterProperty);

        b.getBuildersList().add(new AssertEnvironmentVariable("PARAM1", "1"));

        jenkins.getInstance().rebuildDependencyGraph();

        jenkins.buildAndAssertSuccess(a);
        jenkins.waitUntilNoActivity();
        assertNotNull(a.getLastBuild());
        assertNull(b.getLastBuild());

        trigger.trigger(a.getLastBuild(), b);
        jenkins.waitUntilNoActivity();

        assertNotNull(b.getLastBuild());



        List<CauseAction> causeActions =  b.getLastBuild().getActions(CauseAction.class);
        assertEquals(2, causeActions.size());

    }


    @Test
    @WithoutJenkins
    public void testSettersAndGetters() {
        ManualTrigger trigger = new ManualTrigger((List<ManualTriggerConfig>) null);
        assertNull(trigger.getTriggerConfigs());
        assertNull(trigger.getProject());
    }


    private class AssertEnvironmentVariable extends TestBuilder {
        private String name;
        private String value;

        private AssertEnvironmentVariable(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                               BuildListener listener) throws InterruptedException, IOException {
            EnvVars env = build.getEnvironment(new StreamTaskListener(System.out, null));
            assertTrue(env.containsKey(name));
            assertEquals(value, env.get(name));
            return true;
        }
    }


}
