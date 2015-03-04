/*
 * The MIT License
 *
 * Copyright 2013 Jesse Glick.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.post_build_token_root;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequestSettings;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.UrlUtils;
import hudson.model.FreeStyleProject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.PresetData;
import org.xml.sax.SAXException;

public class BuildRootActionTest {

    private static final Logger logger = Logger.getLogger(BuildRootAction.class.getName());
    @BeforeClass public static void logging() {
        logger.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    @Rule public JenkinsRule j = new JenkinsRule();

    @PresetData(PresetData.DataSet.NO_ANONYMOUS_READACCESS)
    @Test public void build() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("stuff");
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.login("alice", "alice");
        HtmlForm form = wc.getPage(p, "configure").getFormByName("config");
        form.getInputByName("pseudoRemoteTrigger").setChecked(true);
        form.getInputByName("authToken").setValueAttribute("secret");
        j.submit(form);
        @SuppressWarnings("deprecation") hudson.model.BuildAuthorizationToken token = p.getAuthToken();
        assertNotNull(token);
        assertEquals("secret", token.getToken());
        wc = j.createWebClient();
        try { // XXX assertFails in 1.504+
            fail("should have failed but got: " + wc.getPage(p, "build?token=secret"));
        } catch (FailingHttpStatusCodeException x) {
            assertEquals(HttpURLConnection.HTTP_FORBIDDEN, x.getStatusCode());
        }
        j.waitUntilNoActivity();
        assertEquals(0, p.getBuilds().size());
        wc.goTo("buildByTokenPost/build?job=stuff&token=secret&delay=0sec");
        j.waitUntilNoActivity();
        assertEquals(1, p.getBuilds().size());
        wc.goTo("buildByTokenPost/build?job=stuff&token=secret&delay=0sec");
        j.waitUntilNoActivity();
        assertEquals(2, p.getBuilds().size());
        try {
            wc.goTo("buildByTokenPost/build?job=stuff&token=socket&delay=0sec");
        } catch (FailingHttpStatusCodeException x) {
            assertEquals(HttpURLConnection.HTTP_FORBIDDEN, x.getStatusCode());
        }
        j.waitUntilNoActivity();
        assertEquals(2, p.getBuilds().size());

        // doesn't work because jenkins says I've provided no crimb, dunno what that means
        // 403 No valid crumb was included in the request for http://localhost:38280/buildByTokenPost/build?job=stuff&token=secret&delay=0sec
//        postTo(wc, "buildByTokenPost/build?job=stuff&token=secret&delay=0sec", "some content");
//        j.waitUntilNoActivity();
//        assertEquals(3, p.getBuilds().size());

    }

    // XXX test buildWithParameters, polling


    public HtmlPage postTo(JenkinsRule.WebClient wc, String relative, String body) throws IOException, SAXException {
        Page p = postTo(wc, relative, "text/html", body);
        if (p instanceof HtmlPage) {
            return (HtmlPage) p;
        } else {
            throw new AssertionError("Expected text/html but instead the content type was "+p.getWebResponse().getContentType());
        }
    }

    public Page postTo(JenkinsRule.WebClient wc, String relative, String expectedContentType, String body)
            throws IOException, SAXException
    {
        WebRequestSettings request = new WebRequestSettings(UrlUtils.toUrlUnsafe(wc.getContextPath() + relative));
        request.setHttpMethod(HttpMethod.POST);
        request.setRequestBody(body);
        Page p = wc.getPage(wc.getCurrentWindow().getTopWindow(), request);

        assertThat(p.getWebResponse().getContentType(), is(expectedContentType));
        return p;
    }


}
