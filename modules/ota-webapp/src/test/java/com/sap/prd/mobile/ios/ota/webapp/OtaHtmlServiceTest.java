/*
 * #%L
 * Over-the-air deployment webapp
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
package com.sap.prd.mobile.ios.ota.webapp;

import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_BUNDLE_IDENTIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_BUNDLE_VERSION;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_DEBUG;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_IPA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_OTA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_REFERER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_TITLE;
import static com.sap.prd.mobile.ios.ota.lib.LibUtils.buildMap;
import static com.sap.prd.mobile.ios.ota.lib.TestUtils.assertContains;
import static com.sap.prd.mobile.ios.ota.lib.TestUtils.assertOtaLink;
import static com.sap.prd.mobile.ios.ota.webapp.BaseServlet.APPLICATION_BASE_URL_KEY;
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlService.HTML_TEMPLATE_PATH_KEY;
import static com.sap.prd.mobile.ios.ota.webapp.TestUtils.mockResponse;
import static com.sap.prd.mobile.ios.ota.webapp.TestUtils.mockServletContextInitParameters;
import static com.sap.prd.mobile.ios.ota.webapp.TestUtils.mockServletContextUrlMappings;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator;

public class OtaHtmlServiceTest
{

  final static String TEST_SERVICE_URL = "http://ota-server:8080/HTML";
  final static String TEST_CONTEXT_PATH = "";

  final static String TEST_REFERER = "http://nexus:8081/abc/MyHHH.htm";
  final static String TEST_IPA_LINK = "http://nexus:8081/abc/MyHHH.ipa";
  final static String TEST_TITLE = "TestXYZ";
  static final String TEST_BUNDLEIDENTIFIER = "com.sap.myapp.MyABC";
  static final String TEST_BUNDLEVERSION = "1.0.5";
  static final String TEST_OTACLASSIFIER = "otaClassifier";
  static final String TEST_IPACLASSIFIER = "ipaClassifier";
  final static String TEST_REFERER_WITH_CLASSIFIER = "http://nexus:8081/abc/MyHHH-" + KEY_OTA_CLASSIFIER + ".htm";
  final static String TEST_IPA_LINK_WITH_CLASSIFIER = "http://nexus:8081/abc/MyHHH-" + KEY_IPA_CLASSIFIER + ".ipa";
  final static String DIFFERING_APPLICATION_BASE_URL = "https://other-ota-server:1234";

  private final static String[] DEFAULT_INIT_PARAMS = {
    HTML_TEMPLATE_PATH_KEY, "" , KEY_DEBUG, "true"
  };
  
  private static URL TEST_PLIST_URL;
  private static URL DIFFERING_PLIST_URL;
  private static String TEST_OTA_LINK;
  private static URL TEST_PLIST_URL_WITH_CLASSIFIERS;

  private static String TEST_ALTERNATIVE_TEMPLATE = new File("./src/test/resources/alternativeTemplate.html")
    .getAbsolutePath();

  private final static String CHECK_TITLE = String.format("Install App: %s", TEST_TITLE);

  @BeforeClass
  public static void beforeClass() throws IOException
  {
    TEST_PLIST_URL = OtaPlistGenerator.generatePlistRequestUrl(new URL("http://ota-server:8080/PLIST"),
          buildMap(KEY_REFERER, TEST_REFERER, KEY_TITLE, TEST_TITLE, KEY_BUNDLE_IDENTIFIER, TEST_BUNDLEIDENTIFIER,
                KEY_BUNDLE_VERSION, TEST_BUNDLEVERSION));
    DIFFERING_PLIST_URL = OtaPlistGenerator.generatePlistRequestUrl(new URL(DIFFERING_APPLICATION_BASE_URL+"/PLIST"),
          buildMap(KEY_REFERER, TEST_REFERER, KEY_TITLE, TEST_TITLE, KEY_BUNDLE_IDENTIFIER, TEST_BUNDLEIDENTIFIER,
                KEY_BUNDLE_VERSION, TEST_BUNDLEVERSION));
    TEST_OTA_LINK = String.format("<a href='itms-services:///?action=download-manifest&url=%s'>", TEST_PLIST_URL);
    TEST_PLIST_URL_WITH_CLASSIFIERS = OtaPlistGenerator.generatePlistRequestUrl(
          new URL("http://ota-server:8080/PLIST"),
          buildMap(KEY_REFERER, TEST_REFERER_WITH_CLASSIFIER, KEY_TITLE, TEST_TITLE, KEY_BUNDLE_IDENTIFIER,
                TEST_BUNDLEIDENTIFIER,
                KEY_BUNDLE_VERSION, TEST_BUNDLEVERSION, KEY_IPA_CLASSIFIER, KEY_IPA_CLASSIFIER, KEY_OTA_CLASSIFIER,
                KEY_OTA_CLASSIFIER));
  }

  @Before
  public void before()
  {
    assertNotNull(TEST_PLIST_URL);
    assertNotNull(TEST_OTA_LINK);
  }

  @Test
  public void testCorrectValues() throws ServletException, IOException
  {
    OtaHtmlService service = new OtaHtmlService();
    service = (OtaHtmlService)mockServletContextInitParameters(service, DEFAULT_INIT_PARAMS);
    StringWriter writer = new StringWriter();

    HttpServletRequest request = mockRequest();
    HttpServletResponse response = mockResponse(writer);
    service.doPost(request, response);

    String result = writer.getBuffer().toString();
    assertContains(CHECK_TITLE, result);
    assertContains(TEST_IPA_LINK, result);
    assertOtaLink(result, TEST_PLIST_URL.toString(), TEST_BUNDLEIDENTIFIER);
    assertContains(TEST_PLIST_URL.toExternalForm(), result);
  }

  @Test
  public void testWithConfiguration() throws ServletException, IOException
  {
    OtaHtmlService service = new OtaHtmlService();
    service = (OtaHtmlService)mockServletContextInitParameters(service, DEFAULT_INIT_PARAMS, HTML_TEMPLATE_PATH_KEY, TEST_ALTERNATIVE_TEMPLATE);
    StringWriter writer = new StringWriter();

    HttpServletRequest request = mockRequest();
    HttpServletResponse response = mockResponse(writer);

    service.doPost(request, response);

    String result = writer.getBuffer().toString();
    assertContains("ALTERNATIVE HTML TEMPLATE", result);
    assertContains(CHECK_TITLE, result);
    assertContains("<a href='itms-services:///?action=download-manifest&url=" + TEST_PLIST_URL + "'>OTA</a>", result);
    assertContains("<a href='" + TEST_IPA_LINK + "'>IPA</a>", result);
  }

  private HttpServletRequest mockRequest()
  {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(KEY_REFERER)).thenReturn(TEST_REFERER);
    when(request.getRequestURL()).thenReturn(new StringBuffer(TEST_SERVICE_URL));
    when(request.getContextPath()).thenReturn(TEST_CONTEXT_PATH);

    mockServletContextUrlMappings(request);
    
    Map<String, String[]> map = new HashMap<String, String[]>();
    map.put(KEY_TITLE, new String[] { TEST_TITLE });
    map.put(KEY_BUNDLE_IDENTIFIER, new String[] { TEST_BUNDLEIDENTIFIER });
    map.put(KEY_BUNDLE_VERSION, new String[] { TEST_BUNDLEVERSION });
    when(request.getParameterMap()).thenReturn(map);

    return request;
  }

  @Test
  public void testWithClassifiers() throws ServletException, IOException
  {
    OtaHtmlService service = new OtaHtmlService();
    service = (OtaHtmlService)mockServletContextInitParameters(service, DEFAULT_INIT_PARAMS);
    StringWriter writer = new StringWriter();

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(KEY_REFERER)).thenReturn(TEST_REFERER_WITH_CLASSIFIER);
    when(request.getRequestURL()).thenReturn(new StringBuffer(TEST_SERVICE_URL));
    when(request.getContextPath()).thenReturn("");

    Map<String, String[]> map = new HashMap<String, String[]>();
    map.put(KEY_TITLE, new String[] { TEST_TITLE });
    map.put(KEY_BUNDLE_IDENTIFIER, new String[] { TEST_BUNDLEIDENTIFIER });
    map.put(KEY_BUNDLE_VERSION, new String[] { TEST_BUNDLEVERSION });
    map.put(KEY_IPA_CLASSIFIER, new String[] { TEST_IPACLASSIFIER });
    map.put(KEY_OTA_CLASSIFIER, new String[] { TEST_OTACLASSIFIER });
    when(request.getParameterMap()).thenReturn(map);

    mockServletContextUrlMappings(request);
    
    HttpServletResponse response = mockResponse(writer);

    service.doPost(request, response);

    String result = writer.getBuffer().toString();
    assertContains(CHECK_TITLE, result);
    assertContains(TEST_IPA_LINK_WITH_CLASSIFIER, result);
    assertOtaLink(result, TEST_PLIST_URL_WITH_CLASSIFIERS.toString(), TEST_BUNDLEIDENTIFIER);
    assertContains(TEST_PLIST_URL_WITH_CLASSIFIERS.toExternalForm(), result);
  }

  @Test
  public void testConfiguredApplicationBaseUrl() throws ServletException, IOException
  {
    OtaHtmlService service = new OtaHtmlService();
    service = (OtaHtmlService)TestUtils.mockServletContextInitParameters(service, DEFAULT_INIT_PARAMS, APPLICATION_BASE_URL_KEY, DIFFERING_APPLICATION_BASE_URL);
    StringWriter writer = new StringWriter();

    HttpServletRequest request = mockRequest();
    HttpServletResponse response = mockResponse(writer);
    service.doPost(request, response);

    String result = writer.getBuffer().toString();
    assertContains(CHECK_TITLE, result);
    assertContains(TEST_IPA_LINK, result);
    assertOtaLink(result, DIFFERING_PLIST_URL.toString(), TEST_BUNDLEIDENTIFIER);
    assertContains(DIFFERING_PLIST_URL.toExternalForm(), result);
    assertContains(DIFFERING_APPLICATION_BASE_URL, result);
  }
  
}