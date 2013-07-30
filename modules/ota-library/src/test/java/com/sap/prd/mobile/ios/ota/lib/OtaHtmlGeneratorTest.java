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
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_REFERER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_TITLE;
import static com.sap.prd.mobile.ios.ota.lib.LibUtils.buildMap;
import static com.sap.prd.mobile.ios.ota.lib.TestUtils.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.Test;

import com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.Parameters;

public class OtaHtmlGeneratorTest
{

  private final static String referer = "http://hostname:8080/path/MyApp.htm";
  private final static String title = "MyApp";
  private final static String checkIpaURL = "http://hostname:8080/path/MyApp.ipa";
  private final static String bundleIdentifier = "com.sap.xyz.MyApp";
  private final static String bundleVersion = "1.0.2";
  private final static String otaClassifier = "otaClassifier";
  private final static String ipaClassifier = "ipaClassifier";

  private final static Map<String, String> paramMap = buildMap(
        KEY_REFERER, referer, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier, KEY_BUNDLE_VERSION, bundleVersion,
        KEY_IPA_CLASSIFIER, ipaClassifier, KEY_OTA_CLASSIFIER, otaClassifier);

  private static final String plistServiceUrl = "http://ota-server:8080/OTAService/PLIST";

  private static HashMap<String, String> initParams = new HashMap<String, String>();

  @Test
  public void testCorrectValues() throws IOException
  {
    URL plistURL = OtaPlistGenerator.generatePlistRequestUrl(new URL(plistServiceUrl), paramMap);
    String generated = OtaHtmlGenerator.getInstance().generate(
          new Parameters(plistURL, null, buildMap(KEY_REFERER, referer, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier),
                initParams));

    assertContains(String.format("Install App: %s", title), generated);

    TestUtils.assertOtaLink(generated, plistURL.toString(), bundleIdentifier);

    Pattern checkIpaLinkPattern = Pattern.compile("href='([^']+)'[^>]*>Install via iTunes</a>");
    Matcher checkIpaLinkMatcher = checkIpaLinkPattern.matcher(generated);
    assertTrue("Ipa link not found", checkIpaLinkMatcher.find());
    assertEquals(checkIpaURL, checkIpaLinkMatcher.group(1));
  }

  @Test
  public void testAlternativeTemplateByResource() throws IOException
  {
    URL plistURL = OtaPlistGenerator.generatePlistRequestUrl(new URL(plistServiceUrl), paramMap);
    String generated = OtaHtmlGenerator.getInstance("alternativeTemplate.html").generate(
          new Parameters(plistURL, null, buildMap(KEY_REFERER, referer, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier),
                initParams));
    checkAlternativeResult(plistURL, generated);
  }

  @Test
  public void testAlternativeTemplateByFile() throws IOException
  {
    URL plistURL = OtaPlistGenerator.generatePlistRequestUrl(new URL(plistServiceUrl), paramMap);
    File templateFile = new File("./src/test/resources/alternativeTemplate.html");
    assertTrue("File does not exist at " + templateFile.getAbsolutePath(), templateFile.isFile());
    String generated = OtaHtmlGenerator.getInstance(templateFile.getAbsolutePath()).generate(
          new Parameters(plistURL, null, buildMap(KEY_REFERER, referer, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier), initParams));
    checkAlternativeResult(plistURL, generated);
  }

  private void checkAlternativeResult(URL plistURL, String generated)
  {
    assertContains("ALTERNATIVE HTML TEMPLATE", generated);
    assertContains(String.format("Install App: %s", title), generated);
    assertContains("<a href='itms-services:///?action=download-manifest&url=" + plistURL + "'>OTA</a>", generated);
    assertContains("<a href='" + checkIpaURL + "'>IPA</a>", generated);
  }

  @Test
  public void testGenerateHtmlServiceUrl() throws MalformedURLException
  {
    URL url = new URL("http://apple-ota.wdf.sap.corp:1080/ota-service/HTML");
    assertEquals(
          "http://apple-ota.wdf.sap.corp:1080/ota-service/HTML?" +
                "title=MyApp&bundleIdentifier=com.sap.myApp.XYZ&bundleVersion=3.4.5.6&" +
                "ipaClassifier=ipaClassifier&otaClassifier=otaClassifier",
          OtaHtmlGenerator.generateHtmlServiceUrl(url,
                buildMap(KEY_TITLE, "MyApp", KEY_BUNDLE_IDENTIFIER, "com.sap.myApp.XYZ", KEY_BUNDLE_VERSION, "3.4.5.6",
                      KEY_IPA_CLASSIFIER, ipaClassifier, KEY_OTA_CLASSIFIER, otaClassifier)).toExternalForm());
    assertEquals(
          "http://apple-ota.wdf.sap.corp:1080/ota-service/HTML?" +
                "title=MyApp+With+Special%24_Char%26&bundleIdentifier=com.sap.myApp.XYZ&bundleVersion=3.4.5.6&" +
                "ipaClassifier=ipaClassifier&otaClassifier=otaClassifier",
          OtaHtmlGenerator.generateHtmlServiceUrl(url,
                buildMap(KEY_TITLE, "MyApp With Special$_Char&", KEY_BUNDLE_IDENTIFIER, "com.sap.myApp.XYZ", KEY_BUNDLE_VERSION,
                      "3.4.5.6", KEY_IPA_CLASSIFIER, ipaClassifier, KEY_OTA_CLASSIFIER, otaClassifier)).toExternalForm());
    assertEquals(
          "http://apple-ota.wdf.sap.corp:1080/ota-service/HTML?" +
                "title=MyApp&bundleIdentifier=com.sap.myApp.XYZ&bundleVersion=3.4.5.6",
          OtaHtmlGenerator.generateHtmlServiceUrl(url,
                buildMap(KEY_TITLE, "MyApp", KEY_BUNDLE_IDENTIFIER, "com.sap.myApp.XYZ", KEY_BUNDLE_VERSION, "3.4.5.6"))
            .toExternalForm());
    assertEquals(
          "http://apple-ota.wdf.sap.corp:1080/ota-service/HTML?" +
                "title=MyApp&bundleIdentifier=com.sap.myApp.XYZ&bundleVersion=3.4.5.6&otaClassifier=otaClassifier",
          OtaHtmlGenerator.generateHtmlServiceUrl(url,
                buildMap(KEY_TITLE, "MyApp", KEY_BUNDLE_IDENTIFIER, "com.sap.myApp.XYZ", KEY_BUNDLE_VERSION, "3.4.5.6",
                      KEY_OTA_CLASSIFIER, otaClassifier)).toExternalForm());
  }

  public void getNewInstanceCorrectResource() throws FileNotFoundException
  {
    assertEquals(OtaHtmlGenerator.DEFAULT_TEMPLATE,
          OtaHtmlGenerator.getInstance(OtaHtmlGenerator.DEFAULT_TEMPLATE).template.getName());
  }

  @Test
  public void getNewInstanceNull() throws FileNotFoundException
  {
    assertEquals(OtaHtmlGenerator.DEFAULT_TEMPLATE,
          OtaHtmlGenerator.getInstance(null).template.getName());
  }

  @Test
  public void getNewInstanceEmpty() throws FileNotFoundException
  {
    assertEquals(OtaHtmlGenerator.DEFAULT_TEMPLATE,
          OtaHtmlGenerator.getInstance("").template.getName());
  }

  @Test(expected = ResourceNotFoundException.class)
  public void getNewInstanceWrongResource() throws FileNotFoundException
  {
    assertEquals(OtaHtmlGenerator.DEFAULT_TEMPLATE,
          OtaHtmlGenerator.getInstance("doesnotexist.htm").template.getName());
  }

  @Test
  public void getNewInstanceCorrectFile() throws FileNotFoundException
  {
    assertEquals(
          "alternativeTemplate.html",
          OtaHtmlGenerator.getInstance(new File("./src/test/resources/alternativeTemplate.html").getAbsolutePath()).template
            .getName());
  }

  @Test(expected = ResourceNotFoundException.class)
  public void getNewInstanceWrongFile() throws FileNotFoundException
  {
    assertEquals(OtaHtmlGenerator.DEFAULT_TEMPLATE,
          OtaHtmlGenerator.getInstance(new File("./doesnotexist.htm").getAbsolutePath()).template.getName());
  }

}
