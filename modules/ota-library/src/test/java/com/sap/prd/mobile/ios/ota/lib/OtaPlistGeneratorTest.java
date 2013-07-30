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

import static com.sap.prd.mobile.ios.ota.lib.Constants.*;
import static com.sap.prd.mobile.ios.ota.lib.LibUtils.buildMap;
import static com.sap.prd.mobile.ios.ota.lib.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.sap.prd.mobile.ios.ota.lib.LibUtils;
import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator;
import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator.Parameters;

public class OtaPlistGeneratorTest
{

  private final static String STRING_TAG_START = "<string>";
  private final static String STRING_TAG_END = "</string>";

  private final static String referer = "http://hostname:8080/path/MyApp.htm";
  private final static String title = "MyApp";
  private final static String checkIpaURL = "http://hostname:8080/path/MyApp.ipa";
  private final static String bundleIdentifier = "com.sap.xyz.MyApp";
  private final static String bundleVersion = "1.0.2";
  private final static String otaClassifier = "otaClassifier";
  private final static String ipaClassifier = "ipaClassifier";
  private final static String refererWithClassifier = "http://hostname:8080/path/MyApp-" + otaClassifier + ".htm";
  private final static String checkIpaURLWithClassifier = "http://hostname:8080/path/MyApp-" + ipaClassifier + ".ipa";

  final static String long_service = "http://abcdefgServer.wdf.sap.corp:8080/ota-service/PLIST";
  final static String long_referer = "http://nexus.wdf.sap.corp:8081/nexus/very/long/path/to/the/build/results/which/seems/to/be/endless/blablabla/blablabla/blablabla/blablabla/blablabla/blablabla/blablabla/blablabla/blablabla/blablabla/blablabla/blablabla/blablabla/blablabla/MyApp.htm";
  final static String long_title = "This is our App with a very long Name which does not end bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla";
  final static String long_identifier = "com.sap.verylongdepartmentname.verylongothername.verylongothername.verylongothername.verylongothername.verylongothername.verylongothername.verylongothername.verylongothername.verylongothername.verylongothername.MyLongAppName";
  final static String long_version = "12345678901234567890.12345678901234567890.12345678901234567890";

  @Test
  public void testCorrectValues() throws IOException
  {
    String generated = OtaPlistGenerator.getInstance().generate(
          new Parameters(buildMap(KEY_REFERER, referer, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier,
                KEY_BUNDLE_VERSION, bundleVersion)));
    assertContains(STRING_TAG_START + title + STRING_TAG_END, generated);
    assertContains(STRING_TAG_START + bundleVersion + STRING_TAG_END, generated);
    assertContains(STRING_TAG_START + checkIpaURL + STRING_TAG_END, generated);

    generated = OtaPlistGenerator.getInstance().generate(
          new Parameters(buildMap(KEY_REFERER, refererWithClassifier, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier,
                KEY_BUNDLE_VERSION, bundleVersion, KEY_IPA_CLASSIFIER, ipaClassifier, KEY_OTA_CLASSIFIER, otaClassifier)));
    assertContains(STRING_TAG_START + title + STRING_TAG_END, generated);
    assertContains(STRING_TAG_START + bundleVersion + STRING_TAG_END, generated);
    assertContains(STRING_TAG_START + checkIpaURLWithClassifier + STRING_TAG_END, generated);

    generated = OtaPlistGenerator.getInstance().generate(
          new Parameters(buildMap(KEY_REFERER, refererWithClassifier, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier,
                KEY_BUNDLE_VERSION, bundleVersion, KEY_OTA_CLASSIFIER, otaClassifier)));
    assertContains(STRING_TAG_START + title + STRING_TAG_END, generated);
    assertContains(STRING_TAG_START + bundleVersion + STRING_TAG_END, generated);
    assertContains(STRING_TAG_START + checkIpaURL + STRING_TAG_END, generated);

    generated = OtaPlistGenerator.getInstance().generate(
          new Parameters(buildMap(KEY_REFERER, referer, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier,
                KEY_BUNDLE_VERSION, bundleVersion, KEY_IPA_CLASSIFIER, ipaClassifier)));
    assertContains(STRING_TAG_START + title + STRING_TAG_END, generated);
    assertContains(STRING_TAG_START + bundleVersion + STRING_TAG_END, generated);
    assertContains(STRING_TAG_START + checkIpaURLWithClassifier + STRING_TAG_END, generated);

  }

  @Test
  public void testAbnormalValues() throws IOException
  {
    //No classifier in referer
    String generated = OtaPlistGenerator.getInstance().generate(
          new Parameters(buildMap(KEY_REFERER, referer, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier,
                KEY_BUNDLE_VERSION, bundleVersion, KEY_IPA_CLASSIFIER, ipaClassifier, KEY_OTA_CLASSIFIER, otaClassifier)));
    assertContains(STRING_TAG_START + checkIpaURL + STRING_TAG_END, generated);

    //No classifier specified for ota
    generated = OtaPlistGenerator.getInstance().generate(
          new Parameters(buildMap(KEY_REFERER, refererWithClassifier, KEY_TITLE, title, KEY_BUNDLE_IDENTIFIER, bundleIdentifier,
                KEY_BUNDLE_VERSION, bundleVersion, KEY_IPA_CLASSIFIER, ipaClassifier)));
    assertContains(STRING_TAG_START + "http://hostname:8080/path/MyApp-" + otaClassifier + "-" + ipaClassifier + ".ipa"
          + STRING_TAG_END, generated);
  }

  @Test
  public void testGenerateURL() throws IOException
  {
    URL url = OtaPlistGenerator.generatePlistRequestUrl(new URL(long_service),
          buildMap(KEY_REFERER, long_referer, KEY_TITLE, long_title, KEY_BUNDLE_IDENTIFIER, long_identifier,
                KEY_BUNDLE_VERSION, long_version, KEY_IPA_CLASSIFIER, ipaClassifier, KEY_OTA_CLASSIFIER, otaClassifier));
    assertEquals(10, StringUtils.countMatches(url.toExternalForm(), "/"));
    System.out.println("Length: " + url.toExternalForm().length() + " - " + url.toExternalForm());
    assertTrue(url.toExternalForm().startsWith(long_service));
    assertContains("/" + LibUtils.encode("Referer=" + long_referer) + "/", url.toExternalForm());
    assertContains("/" + LibUtils.encode("title=" + long_title) + "/", url.toExternalForm());
    assertContains("/" + LibUtils.encode("bundleIdentifier=" + long_identifier) + "/", url.toExternalForm());
    assertContains("/" + LibUtils.encode("bundleVersion=" + long_version), url.toExternalForm());
    assertContains("/" + LibUtils.encode("ipaClassifier=" + ipaClassifier), url.toExternalForm());
    assertContains("/" + LibUtils.encode("otaClassifier=" + otaClassifier), url.toExternalForm());

    url = OtaPlistGenerator.generatePlistRequestUrl(new URL(long_service),
          buildMap(KEY_REFERER, long_referer, KEY_TITLE, long_title, KEY_BUNDLE_IDENTIFIER, long_identifier,
                KEY_BUNDLE_VERSION, long_version));
    assertEquals(8, StringUtils.countMatches(url.toExternalForm(), "/"));

    url = OtaPlistGenerator.generatePlistRequestUrl(new URL(long_service),
          buildMap(KEY_REFERER, long_referer, KEY_TITLE, long_title, KEY_BUNDLE_IDENTIFIER, long_identifier,
                KEY_BUNDLE_VERSION, long_version, KEY_OTA_CLASSIFIER, otaClassifier));
    assertEquals(9, StringUtils.countMatches(url.toExternalForm(), "/"));

  }

}
