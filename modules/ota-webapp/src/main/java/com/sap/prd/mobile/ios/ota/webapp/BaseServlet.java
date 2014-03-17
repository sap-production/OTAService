/*
 * #%L
 * Over-the-air deployment webapp
 * %%
 * Copyright (C) 2014 SAP AG
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

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

@SuppressWarnings("serial")
public abstract class BaseServlet extends HttpServlet
{
  
  public static final String APPLICATION_BASE_URL_KEY= "applicationBaseUrl";

  @Override
  public String getInitParameter(String name)
  {
    return this.getServletContext().getInitParameter(name);
  }

  @Override
  public Enumeration<String> getInitParameterNames()
  {
    return this.getServletContext().getInitParameterNames();
  }
  
  /**
   * Returns an unmodifiable map containing all init parameters. 
   * @return
   */
  protected Map<String, String> getInitParameters()
  {
    HashMap<String, String> map = new HashMap<String, String>();
    try {
      Enumeration<String> initParameterNames = this.getServletContext().getInitParameterNames();
      while (initParameterNames.hasMoreElements()) {
        String name = initParameterNames.nextElement();
        map.put(name, this.getServletContext().getInitParameter(name));
      }
    }
    catch (IllegalStateException e) {
      if (!e.getMessage().equals("ServletConfig has not been initialized")) throw e;
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * Returns the configured Application Base URL or null if not configured via init parameters.<br/>
   * Use getApplicationBaseUrl(HttpServletRequest) to generate the URL based on a request.
   * @return the URL or null
   * @throws MalformedURLException
   */
  public URL getApplicationBaseUrl() throws MalformedURLException
  {
    String baseUrlString = getInitParameter(APPLICATION_BASE_URL_KEY);
    if(StringUtils.isBlank(baseUrlString)) return null;
    return new URL(baseUrlString);
  }
  
  /**
   * Returns the Application Base URL based on the request.
   * @return the URL
   * @throws MalformedURLException
   */
  public URL getApplicationBaseUrl(HttpServletRequest request) throws MalformedURLException
  {
    String requestUrl = request.getRequestURL().toString(); //e.g. "http://host:8765/ota-service/HTML/UmVmZXJlcj1odHRw..."
    String contextPath = request.getContextPath().toString(); //e.g. "/ota-service" or "" if root context

    String result;
    if (!isEmpty(contextPath)) {
      int idx = requestUrl.indexOf(contextPath);
      if (idx < 0) throw new IllegalStateException(format("Cannot find '%s' in '%s'", contextPath, requestUrl));
      result = requestUrl.substring(0, idx + contextPath.length()); //e.g. "http://host:8765/ota-service"
    }
    else { //root context
      int idx = requestUrl.indexOf("//");
      idx = requestUrl.indexOf("/", idx + "//".length());
      result = requestUrl.substring(0, idx); //e.g. "http://host:8765"
    }
    return new URL(result);
  }

  /**
   * Returns the base URL of the service having the specified <code>servletName</code>.
   * 
   * @param request
   * @param servletName
   * @return base URL of the service
   * @throws MalformedURLException
   */
  public URL getServiceBaseUrl(HttpServletRequest request, String servletName) throws MalformedURLException
  {
    URL applicationBaseUrl = getApplicationBaseUrl();  //e.g. "http://host:8765/ota-service"
    if(applicationBaseUrl == null) applicationBaseUrl = getApplicationBaseUrl(request);  //e.g. "http://host:8765/ota-service"
    String serviceUrlPattern = Utils.getServletMappingUrlPattern(request, servletName); //e.g. "/PLIST"
    return new URL(applicationBaseUrl.toExternalForm() + serviceUrlPattern);
  }

}
