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

import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_ACTION;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_BUNDLE_IDENTIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_BUNDLE_VERSION;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_IPA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_ITMS_REDIRECT;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_OTA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_QRCODE;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_REFERER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_TITLE;
import static com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator.generatePlistRequestUrl;
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlService.getPlistServiceBaseUrl;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.extractParametersFromUri;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.getMatrixToImageConfig;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.getParametersAndReferer;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.getValueFromUriParameterMap;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.sendQRCode;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.awt.Dimension;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator;
import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator.Parameters;

@SuppressWarnings("serial")
public class OtaPlistService extends HttpServlet
{

  private final Logger LOG = Logger.getLogger(OtaPlistService.class.getSimpleName());

  public final static String SERVICE_NAME = "PLIST"; //todo: dynamic

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {

    try {
      Map<String, String> params = getParametersAndReferer(request, response, false);

      String[][] extractedParametersFromUri = extractParametersFromUri(request, SERVICE_NAME);
      dubParameters(KEY_REFERER, params, extractedParametersFromUri, false);
      dubParameters(KEY_TITLE, params, extractedParametersFromUri, true);
      dubParameters(KEY_BUNDLE_IDENTIFIER, params, extractedParametersFromUri, true);
      dubParameters(KEY_BUNDLE_VERSION, params, extractedParametersFromUri, true);
      dubParameters(KEY_IPA_CLASSIFIER, params, extractedParametersFromUri, true);
      dubParameters(KEY_OTA_CLASSIFIER, params, extractedParametersFromUri, true);
      dubParameters(KEY_ACTION, params, extractedParametersFromUri, true);

      if (params.get(KEY_REFERER) == null) {
        response.sendError(400, "Referer required");
        return;
      }

      LOG.info(String.format("GET request from '%s' with referer '%s' and parameters %s",
            request.getRemoteAddr(), params.get(KEY_REFERER), params));

      final String action = params.get(KEY_ACTION);
      if (StringUtils.equals(action, KEY_QRCODE)) {

        LOG.info(String.format("GET request from '%s' with referer '%s', action:qrcode" +
              "and parameters %s", request.getRemoteAddr(), params.get(KEY_REFERER), params));

        String plistUrl = generatePlistRequestUrl(getPlistServiceBaseUrl(request), params).toExternalForm();
        String data = plistUrl + "?action=itmsRedirect";
        LOG.info("Sending QRCode for " + data);
        sendQRCode(request, response, data, getMatrixToImageConfig(request), new Dimension(400, 400));

      }
      else if (StringUtils.equals(action, KEY_ITMS_REDIRECT)) {

        LOG.info(String.format("GET request from '%s' with referer '%s', action:itmsRedirect" +
              "and parameters %s", request.getRemoteAddr(), params.get(KEY_REFERER), params));

        URL plistUrl = generatePlistRequestUrl(getPlistServiceBaseUrl(request), params);

        String itmsServiceLink = "itms-services:///?action=download-manifest&url=" + plistUrl.toExternalForm();
        LOG.info("Sending ItmsServiceRedirect for " + itmsServiceLink);
        response.sendRedirect(itmsServiceLink);

      }
      else {

        response.setContentType("application/xml");
        PrintWriter writer = response.getWriter();
        try {
          OtaPlistGenerator.getInstance().generate(writer, new Parameters(params));
          writer.flush();
        }
        finally {
          closeQuietly(writer);
        }

      }

    }
    catch (Exception e) {
      LOG.log(
            Level.SEVERE,
            String.format(
                  "Exception while processing GET request from '%s' (%s)", request.getRemoteAddr(),
                  Utils.getRequestParams(request)), e);
    }
  }

  /**
   * Updates the requestParams Map for the specified KEY if needed.<br/>
   * requestParams and extractedUriParams might contain values for the specified key.<br/>
   * If requestParamLeading is true it is checked if the request parameter value is null, if yes the
   * according uri Parameter is set.<br/>
   * If requestParamLeading is false it is checked if the uri parameter value is not null, if it is
   * not null the according uri Parameter is set.<br/>
   * 
   * @param KEY
   * @param requestParams
   * @param extractedUriParams
   * @param requestParamLeading
   */
  private void dubParameters(final String KEY, Map<String, String> requestParams, String[][] extractedUriParams,
        boolean requestParamLeading)
  {
    final String uriValue = getValueFromUriParameterMap(extractedUriParams, KEY);
    final String reqValue = requestParams.get(KEY);
    if (requestParamLeading) {
      if (reqValue == null) requestParams.put(KEY, uriValue);
    }
    else {
      if (uriValue != null) requestParams.put(KEY, uriValue);
    }
  }

}
