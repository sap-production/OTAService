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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;

public class TestUtils
{

  public static void assertContains(String expected, String value)
  {
    if (!value.contains(expected)) {
      fail(String.format("Expected is contained: '%s' but was '%s'", expected, value));
    }
  }

  public static void mockServletContextUrlMappings(HttpServletRequest request)
  {
    ServletContext mockedServletContext = mock(ServletContext.class);

    ServletRegistration mockedPlistServletRegistration = mock(ServletRegistration.class);
    Collection<String> plistList = new ArrayList<String>();
    plistList.add("/PLIST/*");
    when(mockedPlistServletRegistration.getMappings()).thenReturn(plistList);
    
    ServletRegistration mockedHtmlServletRegistration = mock(ServletRegistration.class);
    Collection<String> htmlList = new ArrayList<String>();
    htmlList.add("/HTML/*");
    when(mockedHtmlServletRegistration.getMappings()).thenReturn(htmlList);
    
    when(mockedServletContext.getServletRegistration("otaPlistService")).thenReturn(mockedPlistServletRegistration);
    when(mockedServletContext.getServletRegistration("otaHtmlService")).thenReturn(mockedHtmlServletRegistration);
    when(request.getServletContext()).thenReturn(mockedServletContext);
  }
  
}
