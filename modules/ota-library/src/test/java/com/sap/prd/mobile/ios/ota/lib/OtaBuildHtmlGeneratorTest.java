/*
 * #%L
 * Over-the-air deployment library
 * %%
 * Copyright (C) 2012 SAP AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.sap.prd.mobile.ios.ota.lib;

import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_BUNDLE_IDENTIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_BUNDLE_VERSION;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_IPA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_OTA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_TITLE;
import static com.sap.prd.mobile.ios.ota.lib.TestUtils.assertContains;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.Test;

import com.sap.prd.mobile.ios.ota.lib.OtaBuildHtmlGenerator.Parameters;

public class OtaBuildHtmlGeneratorTest
{

  private final static String HTML_SERVICE = "http://apple-ota.wdf.sap.corp:8080/ota-service/HTML";

  private final static String title = "MyApp";
  private final static String bundleIdentifier = "com.sap.xyz.MyApp";
  private final static String bundleVersion = "1.0.2";
  private final static String ipaClassifier = "ipaClassifier";
  private final static String otaClassifier = "otaClassifier";

  @Test
  public void testCorrectValues() throws IOException
  {
    URL htmlServiceUrl = new URL(HTML_SERVICE);
    String generated = OtaBuildHtmlGenerator.getInstance().generate(
          new Parameters(htmlServiceUrl, title, bundleIdentifier, bundleVersion, ipaClassifier, otaClassifier, null));
    assertContains(KEY_TITLE + "=" + title, generated);
    assertContains(KEY_BUNDLE_IDENTIFIER + "=" + bundleIdentifier, generated);
    assertContains(KEY_BUNDLE_VERSION + "=" + bundleVersion, generated);
    assertContains(KEY_IPA_CLASSIFIER + "=" + ipaClassifier, generated);
    assertContains(KEY_OTA_CLASSIFIER + "=" + otaClassifier, generated);
    assertContains(
          "<iframe id=\"iframe\" src=\""
                + HTML_SERVICE
                + "?title=MyApp&bundleIdentifier=com.sap.xyz.MyApp&bundleVersion=1.0.2&ipaClassifier=ipaClassifier&otaClassifier=otaClassifier",
          generated);
  }

  @Test
  public void testAlternativeTemplateByResource() throws IOException
  {
    URL htmlServiceUrl = new URL(HTML_SERVICE);
    String generated = OtaBuildHtmlGenerator.getInstance("alternativeBuildTemplate.html").generate(
          new Parameters(htmlServiceUrl, title, bundleIdentifier, bundleVersion, ipaClassifier, otaClassifier, null));
    checkAlternativeResult(generated);
  }

  @Test
  public void testAlternativeTemplateByFile() throws IOException
  {
    URL htmlServiceUrl = new URL(HTML_SERVICE);
    String generated = OtaBuildHtmlGenerator.getInstance("./src/test/resources/alternativeBuildTemplate.html")
      .generate(
            new Parameters(htmlServiceUrl, title, bundleIdentifier, bundleVersion, ipaClassifier, otaClassifier, null));
    checkAlternativeResult(generated);
  }

  private void checkAlternativeResult(String generated)
  {
    assertContains(KEY_TITLE + "=" + title, generated);
    assertContains("ALTERNATIVE HTML BUILD TEMPLATE", generated);
    assertContains(KEY_BUNDLE_IDENTIFIER + "=" + bundleIdentifier, generated);
    assertContains(KEY_BUNDLE_VERSION + "=" + bundleVersion, generated);
    assertContains(KEY_IPA_CLASSIFIER + "=" + ipaClassifier, generated);
    assertContains(KEY_OTA_CLASSIFIER + "=" + otaClassifier, generated);
    assertContains(
          "<a href=\""
                + HTML_SERVICE
                + "?title=MyApp&bundleIdentifier=com.sap.xyz.MyApp&bundleVersion=1.0.2&ipaClassifier=ipaClassifier&otaClassifier=otaClassifier\"",
          generated);
  }

  @Test
  public void testProject() throws MalformedURLException, IOException
  {
    URL htmlServiceUrl = new URL(HTML_SERVICE);
    OtaBuildHtmlGenerator.getInstance().generate(
          new Parameters(htmlServiceUrl, "MyApp", "com.sap.tip.production.ios.ota.test", "1.0", "Production-iphoneos", "OTA-Installer", null));
  }

  public void getNewInstanceCorrectResource() throws FileNotFoundException
  {
    assertEquals(OtaBuildHtmlGenerator.DEFAULT_TEMPLATE,
          OtaBuildHtmlGenerator.getInstance(OtaBuildHtmlGenerator.DEFAULT_TEMPLATE).template.getName());
  }

  @Test
  public void getNewInstanceNull() throws FileNotFoundException
  {
    assertEquals(OtaBuildHtmlGenerator.DEFAULT_TEMPLATE,
          OtaBuildHtmlGenerator.getInstance(null).template.getName());
  }

  @Test
  public void getNewInstanceEmpty() throws FileNotFoundException
  {
    assertEquals(OtaBuildHtmlGenerator.DEFAULT_TEMPLATE,
          OtaBuildHtmlGenerator.getInstance("").template.getName());
  }

  @Test(expected = ResourceNotFoundException.class)
  public void getNewInstanceWrongResource() throws FileNotFoundException
  {
    assertEquals(OtaBuildHtmlGenerator.DEFAULT_TEMPLATE,
          OtaBuildHtmlGenerator.getInstance("doesnotexist.htm").template.getName());
  }

  @Test
  public void getNewInstanceCorrectFile() throws FileNotFoundException
  {
    assertEquals("alternativeBuildTemplate.html",
          OtaBuildHtmlGenerator.getInstance(new File("./src/test/resources/alternativeBuildTemplate.html")
            .getAbsolutePath()).template.getName());
  }

  @Test(expected = ResourceNotFoundException.class)
  public void getNewInstanceWrongFile() throws FileNotFoundException
  {
    assertEquals(OtaBuildHtmlGenerator.DEFAULT_TEMPLATE,
          OtaBuildHtmlGenerator.getInstance(new File("./doesnotexist.htm").getAbsolutePath()).template.getName());
  }

}
