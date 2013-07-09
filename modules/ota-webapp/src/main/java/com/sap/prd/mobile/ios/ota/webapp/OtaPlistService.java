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
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlService.getPlistServiceUrl;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.extractParametersFromUri;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.getMatrixToImageConfig;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.getValueFromUriParameterMap;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.sendQRCode;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator;
import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator.Parameters;

@SuppressWarnings("serial")
public class OtaPlistService extends HttpServlet
{

  private final Logger LOG = Logger.getLogger(OtaPlistService.class.getSimpleName());

  public final static String SERVICE_NAME = "PLIST"; //todo: dynamic

  public final static String ACTION = "action";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    try {
      String title = null;
      String bundleIdentifier = null;
      String bundleVersion = null;
      String ipaClassifier = null;
      String otaClassifier = null;
      String action = null;

      String originalReferer = Utils.getReferer(request);

      if (request.getParameterMap().size() != 0) {
        title = request.getParameter(OtaPlistGenerator.TITLE);
        bundleIdentifier = request.getParameter(OtaPlistGenerator.BUNDLE_IDENTIFIER);
        bundleVersion = request.getParameter(OtaPlistGenerator.BUNDLE_VERSION);
        ipaClassifier = request.getParameter(OtaPlistGenerator.IPA_CLASSIFIER);
        otaClassifier = request.getParameter(OtaPlistGenerator.OTA_CLASSIFIER);
        action = request.getParameter(ACTION);
      }

      String[][] extractedParametersFromUri = extractParametersFromUri(request, SERVICE_NAME);
      String uriReferer = getValueFromUriParameterMap(extractedParametersFromUri, OtaPlistGenerator.REFERER);
      originalReferer = uriReferer == null ? originalReferer : uriReferer;
      title = title != null ? title : getValueFromUriParameterMap(extractedParametersFromUri, OtaPlistGenerator.TITLE);
      bundleIdentifier = bundleIdentifier != null ? bundleIdentifier : getValueFromUriParameterMap(
            extractedParametersFromUri,
            OtaPlistGenerator.BUNDLE_IDENTIFIER);
      bundleVersion = bundleVersion != null ? bundleVersion : getValueFromUriParameterMap(extractedParametersFromUri,
            OtaPlistGenerator.BUNDLE_VERSION);
      ipaClassifier = ipaClassifier != null ? ipaClassifier : getValueFromUriParameterMap(extractedParametersFromUri,
            OtaPlistGenerator.IPA_CLASSIFIER);
      otaClassifier = otaClassifier != null ? otaClassifier : getValueFromUriParameterMap(extractedParametersFromUri,
            OtaPlistGenerator.OTA_CLASSIFIER);
      action = action != null ? action : getValueFromUriParameterMap(extractedParametersFromUri, ACTION);

      if (originalReferer == null) {
        response.sendError(400, "Referer required");
        return;
      }
      //referer = removeFilePartFromURL(originalReferer);

      LOG.info(String.format("GET request from '%s' with referer '%s' and parameters '%s', '%s', '%s'",
            request.getRemoteAddr(), originalReferer, title, bundleIdentifier, bundleVersion));

      if (action != null && action.equals("qrcode")) {

        LOG.info(String.format("GET request from '%s' with referer '%s', action:qrcode" +
              "and parameters '%s', '%s', '%s', '%s', '%s'",
              request.getRemoteAddr(), originalReferer, request.getParameter(TITLE),
              request.getParameter(BUNDLE_IDENTIFIER), request.getParameter(BUNDLE_VERSION),
              request.getParameter(IPA_CLASSIFIER), request.getParameter(OTA_CLASSIFIER)));

        String plistUrl = OtaPlistGenerator.generatePlistRequestUrl(getPlistServiceUrl(request),
              originalReferer, title, bundleIdentifier, bundleVersion, ipaClassifier, otaClassifier).toExternalForm();
        String data = plistUrl + "?action=itmsRedirect";
        LOG.info("Sending QRCode for " + data);
        sendQRCode(request, response, data, getMatrixToImageConfig(request));

      }
      else if (action != null && action.equals("itmsRedirect")) {

        LOG.info(String.format("GET request from '%s' with referer '%s', action:itmsRedirect" +
              "and parameters '%s', '%s', '%s', '%s', '%s'",
              request.getRemoteAddr(), originalReferer, request.getParameter(TITLE),
              request.getParameter(BUNDLE_IDENTIFIER), request.getParameter(BUNDLE_VERSION),
              request.getParameter(IPA_CLASSIFIER), request.getParameter(OTA_CLASSIFIER)));

        URL plistUrl = OtaPlistGenerator.generatePlistRequestUrl(getPlistServiceUrl(request),
              originalReferer, title, bundleIdentifier, bundleVersion, ipaClassifier, otaClassifier);

        String itmsServiceLink = "itms-services:///?action=download-manifest&url=" + plistUrl.toExternalForm();
        LOG.info("Sending ItmsServiceRedirect for " + itmsServiceLink);
        response.sendRedirect(itmsServiceLink);

      }
      else {

        response.setContentType("application/xml");
        PrintWriter writer = response.getWriter();
        try {
          OtaPlistGenerator.getInstance().generate(writer,
                new Parameters(originalReferer, title, bundleIdentifier, bundleVersion, ipaClassifier, otaClassifier));
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

}
