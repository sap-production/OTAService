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

import static com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.BUNDLE_IDENTIFIER;
import static com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.BUNDLE_VERSION;
import static com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.IPA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.OTA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.TITLE;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.QR_OFF_COLOR;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.QR_OFF_COLOR_DEFAULT;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.QR_ON_COLOR;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.QR_ON_COLOR_DEFAULT;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.getMatrixToImageConfig;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.sendQRCode;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator;
import com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.Parameters;
import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator;

@SuppressWarnings("serial")
public class OtaHtmlService extends HttpServlet
{

  public final static String SERVICE_NAME = "HTML"; //todo: dynamic

  public final static String ACTION = "action";

  private final Logger LOG = Logger.getLogger(OtaPlistService.class.getSimpleName());
  public final static String HTML_TEMPLATE_PATH_KEY = "htmlTemplatePath";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    //TODO: REWORK. PlistService now uses Base64+URLEncoded parameters, and no URL Parameters but slashes!

    try {
      String originalReferer = Utils.getRefererSendError(request, response);
      //String referer = removeFilePartFromURL(originalReferer);

      final String action = request.getParameter(ACTION);
      if (action != null && action.equals("qrcode")) {

        URL htmlServiceUrl = OtaHtmlGenerator.generateHtmlServiceUrl(
              getHtmlServiceUrl(request),
              request.getParameter(TITLE),
              request.getParameter(BUNDLE_IDENTIFIER),
              request.getParameter(BUNDLE_VERSION),
              request.getParameter(IPA_CLASSIFIER),
              request.getParameter(OTA_CLASSIFIER)
          );
        sendQRCode(request, response, htmlServiceUrl.toString(), getMatrixToImageConfig(request));

      }
      else {

        URL plistUrl = OtaPlistGenerator.generatePlistRequestUrl(
              getPlistServiceUrl(request),
              originalReferer,
              request.getParameter(TITLE),
              request.getParameter(BUNDLE_IDENTIFIER),
              request.getParameter(BUNDLE_VERSION),
              request.getParameter(IPA_CLASSIFIER),
              request.getParameter(OTA_CLASSIFIER)
          );

        URL htmlServiceQrcodeUrl = OtaHtmlGenerator.generateHtmlServiceUrl(
              getHtmlServiceUrl(request),
              request.getParameter(TITLE),
              request.getParameter(BUNDLE_IDENTIFIER),
              request.getParameter(BUNDLE_VERSION),
              request.getParameter(IPA_CLASSIFIER),
              request.getParameter(OTA_CLASSIFIER)
          );
        htmlServiceQrcodeUrl = new URL(htmlServiceQrcodeUrl.toExternalForm() + "&"+
          ACTION+"=qrcode&"+
          QR_ON_COLOR+"="+QR_ON_COLOR_DEFAULT+"&"+
          QR_OFF_COLOR+"="+QR_OFF_COLOR_DEFAULT);

        LOG.info(String.format("GET request from '%s' with referer '%s' and parameters '%s', '%s', '%s', '%s', '%s'",
              request.getRemoteAddr(), originalReferer, request.getParameter(TITLE), request
                .getParameter(BUNDLE_IDENTIFIER), request.getParameter(BUNDLE_VERSION),
              request.getParameter(IPA_CLASSIFIER), request.getParameter(OTA_CLASSIFIER)));

        HashMap<String, String> initParameters = getInitParameters();
        String htmlTemplatePath = initParameters.get(HTML_TEMPLATE_PATH_KEY);

        PrintWriter writer = response.getWriter();
        try {
          OtaHtmlGenerator.getInstance(htmlTemplatePath).generate(
                writer,
                new Parameters(originalReferer, request.getParameter(TITLE), request.getParameter(BUNDLE_IDENTIFIER),
                      plistUrl, htmlServiceQrcodeUrl, request.getParameter(IPA_CLASSIFIER),
                      request.getParameter(OTA_CLASSIFIER), initParameters));
          writer.flush();
        }
        finally {
          closeQuietly(writer);
        }
      }

    }
    catch (Exception e) {
      LOG.log(Level.SEVERE, String.format(
            "Exception while processing GET request from '%s'", request.getRemoteAddr()), e);
    }
  }

  private HashMap<String, String> getInitParameters()
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
    return map;
  }

  String getPlistServiceUrl(HttpServletRequest request)
  {
    return getServiceUrl(request, OtaPlistService.SERVICE_NAME);
  }

  URL getHtmlServiceUrl(HttpServletRequest request) throws MalformedURLException
  {
    return new URL(getServiceUrl(request, OtaHtmlService.SERVICE_NAME));
  }

  private String getServiceUrl(HttpServletRequest request, String serviceName)
  {
    if (request.getRequestURL() == null) {
      return null;
    }
    String serviceUrl = request.getRequestURL().toString();
    int lastSlash = serviceUrl.lastIndexOf("/");
    serviceUrl = serviceUrl.substring(0, lastSlash);
    String plistServiceUrl = serviceUrl + "/" + serviceName;
    return plistServiceUrl;
  }

}
