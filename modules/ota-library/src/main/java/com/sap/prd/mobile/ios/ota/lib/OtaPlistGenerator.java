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
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_IPA_URL;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_OTA_CLASSIFIER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_REFERER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_TITLE;
import static com.sap.prd.mobile.ios.ota.lib.LibUtils.encode;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator.Parameters;

/**
 * This class generates the PLIST file which is created by the OTA PLIST service.
 */
public class OtaPlistGenerator extends VelocityBase<Parameters>
{

  /**
   * Parameters required for the <code>OtaPlistGenerator</code>.
   */
  public static class Parameters extends com.sap.prd.mobile.ios.ota.lib.VelocityBase.Parameters
  {

    /**
     * @param referer
     *          The original referer to the initial HTML page (e.g. in Nexus)
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
     * @throws MalformedURLException
     */
    public Parameters(Map<String, String> requestParams)
          throws MalformedURLException
    {
      super();
      URL ipaURL = LibUtils.generateDirectIpaUrl(requestParams.get(KEY_REFERER),
            requestParams.get(KEY_IPA_CLASSIFIER), requestParams.get(KEY_OTA_CLASSIFIER));
      mappings.put(KEY_IPA_URL, ipaURL.toExternalForm());
      mappings.put(KEY_BUNDLE_IDENTIFIER, requestParams.get(KEY_BUNDLE_IDENTIFIER));
      mappings.put(KEY_BUNDLE_VERSION, requestParams.get(KEY_BUNDLE_VERSION));
      mappings.put(KEY_TITLE, requestParams.get(KEY_TITLE));
    }
  }

  private static final String DEFAULT_TEMPLATE = "template.plist";
  private static Map<String, OtaPlistGenerator> instances = new HashMap<String, OtaPlistGenerator>();

  public static synchronized OtaPlistGenerator getInstance()
  {
    return getInstance(null);
  }

  public static synchronized OtaPlistGenerator getInstance(String template)
  {
    return getInstance(template, false);
  }
  
  public static synchronized OtaPlistGenerator getInstance(String template, boolean forceNewInstance)
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

    OtaPlistGenerator instance;

    if (forceNewInstance || !instances.keySet().contains(template)) {
      instance = new OtaPlistGenerator(template);
      instances.put(template, instance);
    }
    else {
      instance = instances.get(template);
    }

    return instance;
  }
  
  private OtaPlistGenerator(String template)
  {
    super(template);
  }

  @Override
  public synchronized String generate(Parameters parameters) throws IOException
  {
    return super.generate(parameters);
  }

  @Override
  public synchronized void generate(PrintWriter writer, Parameters parameters) throws IOException
  {
    super.generate(writer, parameters);
  }

  /**
   * Generates the URL for a specific request to the PLIST service.
   * 
   * @param plistServiceUrl
   *          The base URL to the service. E.g. http://apple-ota.wdf.sap.corp:1080/ota-service/PLIST
   * @param referer
   *          The original referer to the initial HTML page (e.g. in Nexus)
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
   * @throws IOException
   */
  public static URL generatePlistRequestUrl(URL plistServiceUrl, Map<String, String> params) throws IOException
  {
    if (plistServiceUrl == null) {
      throw new NullPointerException("serviceUrl null");
    }
    String urlString = String.format("%s/%s/%s/%s/%s%s%s",
          plistServiceUrl,
          LibUtils.encode(KEY_REFERER + "=" + params.get(KEY_REFERER)),
          LibUtils.encode(KEY_TITLE + "=" + params.get(KEY_TITLE)),
          LibUtils.encode(KEY_BUNDLE_IDENTIFIER + "=" + params.get(KEY_BUNDLE_IDENTIFIER)),
          LibUtils.encode(KEY_BUNDLE_VERSION + "=" + params.get(KEY_BUNDLE_VERSION)),
          (isEmpty(params.get(KEY_IPA_CLASSIFIER)) ? "" : "/" + encode(KEY_IPA_CLASSIFIER + "=" + params.get(KEY_IPA_CLASSIFIER))),
          (isEmpty(params.get(KEY_OTA_CLASSIFIER)) ? "" : "/" + encode(KEY_OTA_CLASSIFIER + "=" + params.get(KEY_OTA_CLASSIFIER)))
      );
    return new URL(urlString);
  }

}
