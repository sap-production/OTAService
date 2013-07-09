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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;

import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator.Parameters;

/**
 * This class generates the PLIST file which is created by the OTA PLIST service.
 */
public class OtaPlistGenerator extends VelocityBase<Parameters>
{

  public static final String IPA_URL = "ipaUrl";
  public static final String BUNDLE_IDENTIFIER = "bundleIdentifier";
  public static final String BUNDLE_VERSION = "bundleVersion";
  public static final String TITLE = "title";
  public static final String IPA_CLASSIFIER = "ipaClassifier";
  public static final String OTA_CLASSIFIER = "otaClassifier";
  public static final String REFERER = "Referer";

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
    public Parameters(String referer, String title, String bundleIdentifier, String bundleVersion,
          String ipaClassifier, String otaClassifier)
          throws MalformedURLException
    {
      super();
      URL ipaURL = LibUtils.generateDirectIpaUrl(referer, ipaClassifier, otaClassifier);
      mappings.put(IPA_URL, ipaURL.toExternalForm());
      mappings.put(BUNDLE_IDENTIFIER, bundleIdentifier);
      mappings.put(BUNDLE_VERSION, bundleVersion);
      mappings.put(TITLE, title);
    }
  }

  private static final String DEFAULT_TEMPLATE = "template.plist";
  private static OtaPlistGenerator instance = null;

  public static synchronized OtaPlistGenerator getInstance()
  {
    if (instance == null) {
      instance = new OtaPlistGenerator();
    }
    return instance;
  }

  private OtaPlistGenerator()
  {
    super(DEFAULT_TEMPLATE);
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
  public static URL generatePlistRequestUrl(String plistServiceUrl, String referer, String title,
        String bundleIdentifier, String bundleVersion, String ipaClassifier, String otaClassifier) throws IOException
  {
    if (plistServiceUrl == null) {
      throw new NullPointerException("serviceUrl null");
    }
    String urlString = String.format("%s/%s/%s/%s/%s%s%s",
          plistServiceUrl,
          LibUtils.encode(REFERER + "=" + referer),
          LibUtils.encode(TITLE + "=" + title),
          LibUtils.encode(BUNDLE_IDENTIFIER + "=" + bundleIdentifier),
          LibUtils.encode(BUNDLE_VERSION + "=" + bundleVersion),
          (StringUtils.isEmpty(ipaClassifier) ? "" : "/" + LibUtils.encode(IPA_CLASSIFIER + "=" + ipaClassifier)),
          (StringUtils.isEmpty(otaClassifier) ? "" : "/" + LibUtils.encode(OTA_CLASSIFIER + "=" + otaClassifier))
      );
    return new URL(urlString);
  }

}
