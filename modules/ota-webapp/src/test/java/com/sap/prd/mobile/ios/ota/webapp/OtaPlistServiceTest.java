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
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_REFERER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_TITLE;
import static com.sap.prd.mobile.ios.ota.lib.LibUtils.encode;
import static com.sap.prd.mobile.ios.ota.lib.TestUtils.assertContains;
import static com.sap.prd.mobile.ios.ota.webapp.OtaPlistService.PLIST_TEMPLATE_PATH_KEY;
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlService.HTML_TEMPLATE_PATH_KEY;
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlServiceTest.TEST_BUNDLEIDENTIFIER;
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlServiceTest.TEST_BUNDLEVERSION;
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlServiceTest.TEST_IPA_LINK;
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlServiceTest.TEST_REFERER;
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlServiceTest.TEST_TITLE;
import static com.sap.prd.mobile.ios.ota.webapp.TestUtils.mockResponse;
import static com.sap.prd.mobile.ios.ota.webapp.TestUtils.mockServletContextInitParameters;
import static com.sap.prd.mobile.ios.ota.webapp.TestUtils.mockServletContextUrlMappings;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class OtaPlistServiceTest
{
  final static String TEST_SERVICE_URL = "http://ota-server:8080/PLIST";
  final static String TEST_CONTEXT_PATH = "";
  
  private final static String STRING_TAG_START = "<string>";
  private final static String STRING_TAG_END = "</string>";
  private final static String PLIST = "PLIST";

  private final static String[] DEFAULT_INIT_PARAMS = {
    PLIST_TEMPLATE_PATH_KEY, "" , KEY_DEBUG, "true"
  };
  
  private static String TEST_ALTERNATIVE_TEMPLATE = new File("./src/test/resources/alternativeTemplate.plist").getAbsolutePath();

  
  @Test
  public void testWithURLParameters() throws ServletException, IOException
  {
    OtaPlistService service = new OtaPlistService();
    service = (OtaPlistService)TestUtils.mockServletContextInitParameters(service, DEFAULT_INIT_PARAMS);
    StringWriter writer = new StringWriter();

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn("");
    when(request.getRequestURI()).thenReturn("/PLIST");
    mockServletContextUrlMappings(request);

    when(request.getParameter(KEY_REFERER)).thenReturn(TEST_REFERER);
    Map<String, String[]> map = new HashMap<String, String[]>();
    map.put(KEY_REFERER, new String[]{TEST_REFERER});
    map.put(KEY_TITLE, new String[]{TEST_TITLE});
    map.put(KEY_BUNDLE_IDENTIFIER, new String[]{TEST_BUNDLEIDENTIFIER});
    map.put(KEY_BUNDLE_VERSION, new String[]{TEST_BUNDLEVERSION});
    when(request.getParameterMap()).thenReturn(map);
    
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(new PrintWriter(writer));

    service.doGet(request, response);

    String result = writer.getBuffer().toString();
    assertContains(STRING_TAG_START + TEST_TITLE + STRING_TAG_END, result);
    assertContains(STRING_TAG_START + TEST_BUNDLEVERSION + STRING_TAG_END, result);
    assertContains(STRING_TAG_START + TEST_BUNDLEIDENTIFIER + STRING_TAG_END, result);
    assertContains(STRING_TAG_START + TEST_IPA_LINK + STRING_TAG_END, result);
  }

  @Test
  public void testWithSlashSeparatedParameters() throws ServletException, IOException
  {
    OtaPlistService service = new OtaPlistService();
    service = (OtaPlistService)TestUtils.mockServletContextInitParameters(service, DEFAULT_INIT_PARAMS);

    StringWriter writer = new StringWriter();

    HttpServletRequest request = mock(HttpServletRequest.class);

    StringBuilder sb = new StringBuilder();
    sb.append("/abc/").append(PLIST).append("/");
    sb.append(encode(KEY_REFERER + "=" + TEST_REFERER)).append("/");
    sb.append(encode(KEY_TITLE + "=" + TEST_TITLE)).append("/");
    sb.append(encode(KEY_BUNDLE_IDENTIFIER + "=" + TEST_BUNDLEIDENTIFIER)).append("/");
    sb.append(encode(KEY_BUNDLE_VERSION + "=" + TEST_BUNDLEVERSION));
    when(request.getRequestURI()).thenReturn(sb.toString());
    when(request.getContextPath()).thenReturn("/abc");

    mockServletContextUrlMappings(request);
    
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(new PrintWriter(writer));

    service.doGet(request, response);

    String result = writer.getBuffer().toString();
    assertContains(STRING_TAG_START + TEST_TITLE + STRING_TAG_END, result);
    assertContains(STRING_TAG_START + TEST_BUNDLEVERSION + STRING_TAG_END, result);
    assertContains(STRING_TAG_START + TEST_BUNDLEIDENTIFIER + STRING_TAG_END, result);
    assertContains(STRING_TAG_START + TEST_IPA_LINK + STRING_TAG_END, result);
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
    assertContains("ALTERNATIVE PLIST TEMPLATE", result);
    assertContains("<string>" + TEST_BUNDLEIDENTIFIER + "_iOS8Fix</string>", result);
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
  
}