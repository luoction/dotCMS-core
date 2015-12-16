package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.dotmarketing.portlets.rules.actionlet.SetRequestAttributeActionlet.REQUEST_KEY;
import static com.dotmarketing.portlets.rules.actionlet.SetRequestAttributeActionlet.REQUEST_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class SetRequestAttributeActionletTest {

    @Test
    public void testGeneralConfiguration() throws Exception {
        SetRequestAttributeActionlet actionlet = new SetRequestAttributeActionlet();
        assertThat(actionlet.getI18nKey(), is("api.system.ruleengine.actionlet.SetRequestAttribute"));
        assertThat("It has two parameters.", actionlet.getParameterDefinitions().size(), is(2));
        assertThat(actionlet.getId(), is("SetRequestAttributeActionlet"));
    }

    /**
     * Define some test cases for validating the URL. TestNG will run each of these cases as a separate test.
     * This is a great way to test a large number of allowed inputs... and also helps makes your test count look amazing.
     */
    @DataProvider(name = "cases")
    public Object[][] noConditionCases() {

        return new TestCase[][]{
            {new TestCase("Null key is not valid", null, "anything", false)},
            {new TestCase("An empty string for key is invalid", "", "anything", false)},
            {new TestCase("A single character key is valid", "a", "anything", true)},
            {new TestCase("A null value is valid", "foo", null, true)},
            {new TestCase("An empty string value is valid", "foo", "", true)},
        };
    }

    @Test(dataProvider = "cases")
    public void testValidateParameters(TestCase theCase) throws Exception {
        SetRequestAttributeActionlet actionlet = new SetRequestAttributeActionlet();
        List<ParameterModel> list = ImmutableList.of(
            new ParameterModel(REQUEST_KEY, theCase.key),
            new ParameterModel(REQUEST_VALUE, theCase.value)
        );
        RuleAction actionInstance = new RuleAction();
        actionInstance.setParameters(list);
        Exception exception = null;
        try {
            actionlet.doCheckValid(actionInstance);
        } catch (Exception e) {
            exception = e;
        }
        if(theCase.valid && exception != null) {
            exception.printStackTrace();
        }
        assertThat(theCase.msg, exception, theCase.valid ? nullValue() : notNullValue());
    }



    @Test
    public void testExecuteActionClearsWhenNullValueSent() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        String keyValue = "Foo";
        String valueValue = null;
        Map<String, ParameterModel> params = ImmutableMap.of(
            REQUEST_KEY, new ParameterModel(REQUEST_KEY, keyValue),
            REQUEST_VALUE, new ParameterModel(REQUEST_VALUE, valueValue)
        );

        SetRequestAttributeActionlet actionlet = new SetRequestAttributeActionlet();
        actionlet.evaluate(request, null, new SetRequestAttributeActionlet.Instance(params));

        Mockito.verify(request).removeAttribute(keyValue);
    }

    public static class TestCase {

        String msg;
        String key;
        String value;
        boolean valid;

        public TestCase(String msg, String key, String value, boolean valid) {
            this.msg = msg;
            this.key = key;
            this.value = value;
            this.valid = valid;
        }

        @Override
        public String toString() {
            return msg;
        }
    }

}