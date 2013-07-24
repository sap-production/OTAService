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
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_HTML_QRCODE_URL;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_IPA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_OTA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_PLIST_URL;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_REFERER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_TITLE;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.Parameters;

/**
 * This class generates the HTML page which is created by the OTA HTML service.
 */
public class OtaHtmlGenerator extends VelocityBase<Parameters>
{

  /**
   * Parameters required for the <code>OtaHtmlGenerator</code>.
   */
  public static class Parameters extends com.sap.prd.mobile.ios.ota.lib.VelocityBase.Parameters
  {
    /**
     * @param referer
     *          The original referer to the initial HTML page (e.g. in Nexus)
     * @param title
     *          The title of the App
     * @param bundleIdentifier
     *          The bundle identifier
     * @param plistUrl
     *          The complete OTA PLIST Service URL for this App containing all parameters.
     * @param htmlServiceQrcodeUrl
     * @param ipaClassifier
     *          The classifier used in the IPA artifact. If null no classifier will be used.
     * @param otaClassifier
     *          The classifier used in the OTA HTML artifact. If null no classifier will be used.
     * @throws MalformedURLException
     */
    public Parameters(URL plistUrl, URL htmlServiceQrcodeUrl, Map<String, String> requestParams,
          Map<String, String> initParams)
          throws MalformedURLException
    {
      super();
      URL ipaUrl = LibUtils.generateDirectIpaUrl(requestParams.get(KEY_REFERER), requestParams.get(KEY_IPA_CLASSIFIER),
            requestParams.get(KEY_OTA_CLASSIFIER));
      if (initParams != null) {
        mappings.putAll(initParams);
      }
      mappings.putAll(requestParams);
      mappings.put(Constants.KEY_IPA_URL, ipaUrl.toExternalForm());
      mappings.put(KEY_PLIST_URL, plistUrl.toExternalForm());
      mappings.put(KEY_HTML_QRCODE_URL, htmlServiceQrcodeUrl == null ? null : htmlServiceQrcodeUrl.toExternalForm());
    }
  }

  static final String DEFAULT_TEMPLATE = "template.html";
  private static Map<String, OtaHtmlGenerator> instances = new HashMap<String, OtaHtmlGenerator>();

  public static OtaHtmlGenerator getInstance()
  {
    return getInstance(null);
  }

  public static synchronized OtaHtmlGenerator getInstance(String template)
  {
    return getInstance(template, false);
  }

  public static synchronized OtaHtmlGenerator getInstance(String template, boolean forceNewInstance)
  {
    if (isEmpty(template)) {
      template = DEFAULT_TEMPLATE;
    }
    else {
      File file = new File(template);
      if (file.isFile() && file.getName().equals(DEFAULT_TEMPLATE))
        throw new IllegalArgumentException(format(
              "Custom template (configured in e.g. 'Tomcat/conf/Catalina/localhost/ota-service.xml') " +
                    "must not be named '%s'. Current path: '%s'", DEFAULT_TEMPLATE, template));
    }

    OtaHtmlGenerator instance;

    if (forceNewInstance || !instances.keySet().contains(template)) {
      instance = new OtaHtmlGenerator(template);
      instances.put(template, instance);
    }
    else {
      instance = instances.get(template);
    }

    return instance;
  }

  private OtaHtmlGenerator(String template)
  {
    super(template);
  }

  /**
   * Generates the URL for a specific request to the HTML service.
   * 
   * @param htmlServiceUrl
   *          The base URL to the service. E.g. http://apple-ota.wdf.sap.corp:1080/ota-service/HTML
   * @param title
   *          The title of the App
   * @param bundleIdentifier
   *          The bundleIdentifier of the App
   * @param bundleVersion
   *          The bundleVersion of the App
   * @param ipaClassifier
   *          The classifier used in the IPA artifact. If null no classifier will be used.
   * @param otaClassifier
   *          The classifier used in the OTA HTML artifact. If null no classifier will be used.
   * @return the URL
   * @throws MalformedURLException
   */
  public static URL generateHtmlServiceUrl(URL htmlServiceUrl, Map<String, String> params) throws MalformedURLException
  {
    if (params.get(KEY_REFERER) == null) {
      return new URL(String.format("%s?%s=%s&%s=%s&%s=%s%s%s",
            htmlServiceUrl.toExternalForm(),
            KEY_TITLE, LibUtils.urlEncode(params.get(KEY_TITLE)),
            KEY_BUNDLE_IDENTIFIER, LibUtils.urlEncode(params.get(KEY_BUNDLE_IDENTIFIER)),
            KEY_BUNDLE_VERSION, LibUtils.urlEncode(params.get(KEY_BUNDLE_VERSION)),
            (StringUtils.isEmpty(params.get(KEY_IPA_CLASSIFIER)) ? "" :
                  String.format("&%s=%s", KEY_IPA_CLASSIFIER, LibUtils.urlEncode(params.get(KEY_IPA_CLASSIFIER)))),
            (StringUtils.isEmpty(params.get(KEY_OTA_CLASSIFIER)) ? "" :
                  String.format("&%s=%s", KEY_OTA_CLASSIFIER, LibUtils.urlEncode(params.get(KEY_OTA_CLASSIFIER))))
        ));

    }
    else {
      return new URL(String.format("%s?%s=%s&%s=%s&%s=%s&%s=%s%s%s",
            htmlServiceUrl.toExternalForm(),
            KEY_REFERER, LibUtils.encode(KEY_REFERER + "=" + params.get(KEY_REFERER)),
            KEY_TITLE, LibUtils.urlEncode(params.get(KEY_TITLE)),
            KEY_BUNDLE_IDENTIFIER, LibUtils.urlEncode(params.get(KEY_BUNDLE_IDENTIFIER)),
            KEY_BUNDLE_VERSION, LibUtils.urlEncode(params.get(KEY_BUNDLE_VERSION)),
            (StringUtils.isEmpty(params.get(KEY_IPA_CLASSIFIER)) ? "" :
                  String.format("&%s=%s", KEY_IPA_CLASSIFIER, LibUtils.urlEncode(params.get(KEY_IPA_CLASSIFIER)))),
            (StringUtils.isEmpty(params.get(KEY_OTA_CLASSIFIER)) ? "" :
                  String.format("&%s=%s", KEY_OTA_CLASSIFIER, LibUtils.urlEncode(params.get(KEY_OTA_CLASSIFIER))))
        ));
    }
  }

}
