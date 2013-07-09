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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sap.prd.mobile.ios.ota.lib.OtaBuildHtmlGenerator.Parameters;

/**
 * This class generates the HTML page which is created during the build and uploaded to Nexus.
 * 
 * @param <Parameters>
 */
public class OtaBuildHtmlGenerator extends VelocityBase<Parameters>
{

  public final static String HTML_URL = "htmlUrl";
  public static final String HTML_SERVICE_URL = "htmlServiceUrl";
  public static final String TITLE = "title";
  public static final String BUNDLE_IDENTIFIER = "bundleIdentifier";
  public static final String BUNDLE_VERSION = "bundleVersion";
  public static final String IPA_CLASSIFIER = "ipaClassifier";
  public static final String OTA_CLASSIFIER = "otaClassifier";

  /**
   * Parameters required for the <code>OtaBuildHtmlGenerator</code>.
   */
  public static class Parameters extends com.sap.prd.mobile.ios.ota.lib.VelocityBase.Parameters
  {
    /**
     * @param htmlServiceUrl
     *          The URL of the OTA HTML Service
     * @param title
     *          The title of the App
     * @param bundleIdentifier
     *          The bundleIdentifier of the App
     * @param bundleVersion
     *          The bundleVersion of the App
     * @param ipaClassifier
     *          The classifier used in the IPA artifact. If null no classifier will be used. is
     *          used.
     * @param otaClassifier
     *          The classifier used in the OTA HTML artifact. If null no classifier will be used.
     * @param initParams
     *          A map containing additional parameters.
     * @throws MalformedURLException
     */
    public Parameters(URL htmlServiceUrl, String title, String bundleIdentifier, String bundleVersion,
          String ipaClassifier, String otaClassifier, Map<String, String> initParams)
          throws MalformedURLException
    {
      super();
      URL htmlUrl = OtaHtmlGenerator.generateHtmlServiceUrl(htmlServiceUrl, title, bundleIdentifier, bundleVersion,
            ipaClassifier, otaClassifier);
      mappings.put(HTML_URL, htmlUrl.toExternalForm());
      mappings.put(HTML_SERVICE_URL, htmlServiceUrl);
      mappings.put(TITLE, title);
      mappings.put(BUNDLE_IDENTIFIER, bundleIdentifier);
      mappings.put(BUNDLE_VERSION, bundleVersion);
      mappings.put(IPA_CLASSIFIER, ipaClassifier);
      mappings.put(OTA_CLASSIFIER, otaClassifier);
      if(initParams != null) {
        for(String name : initParams.keySet()) {
          mappings.put(name, initParams.get(name));
        }
      }
    }
  }


  static final String DEFAULT_TEMPLATE = "buildTemplate.html";
  private static Map<String, OtaBuildHtmlGenerator> instances = new HashMap<String, OtaBuildHtmlGenerator>();

  public static OtaBuildHtmlGenerator getInstance() {
    return getInstance(null);
  }
  
  public static synchronized OtaBuildHtmlGenerator getInstance(String template)
  {
    if(StringUtils.isEmpty(template)) {
      template = DEFAULT_TEMPLATE;
    }
    OtaBuildHtmlGenerator instance;
    if (!instances.keySet().contains(template)) {
      instance = new OtaBuildHtmlGenerator(template);
      instances.put(template, instance);
    } else {
      instance = instances.get(template);
    }
    return instance;
  }

  private OtaBuildHtmlGenerator(String template)
  {
    super(validateTemplate(template));
  }
  
  private static String validateTemplate(String template)
  {
    if(template == null || template.trim().length() == 0) return DEFAULT_TEMPLATE;
    return template;
  }

}
