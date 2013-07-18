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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.sonatype.plexus.components.cipher.Base64;

public class LibUtils
{

  public static URL buildUrl(String urlPrefix, String urlSuffix) throws MalformedURLException
  {
    String urlPrefixFixed = urlPrefix.trim();
    while (urlPrefixFixed.endsWith("/")) { //Remove trailing '/'
      urlPrefixFixed = urlPrefixFixed.substring(0, urlPrefixFixed.length() - 1);
    }
    URL url = new URL(urlPrefixFixed + "/" + urlSuffix);
    return url;
  }

  /**
   * Generates the link to the IPA file based on the referer to the HTML file, the used
   * ipaClassifier and otaClassifier. If no classifiers are specified the IPA URL will have the same
   * value as the referer, except of the file extension.
   * 
   * @param referer
   *          Referer to the HTML file located next to the IPA file
   * @param ipaClassifier
   *          classifier used in the IPA file. Can be null.
   * @param otaClassifier
   *          classifier used in the OTA HTML file. Can be null.
   * @return
   * @throws MalformedURLException
   */
  public static URL generateDirectIpaUrl(String referer, String ipaClassifier, String otaClassifier)
        throws MalformedURLException
  {
    int idx = referer.lastIndexOf(".");
    if (idx > 0 && idx > referer.length() - 6) {
      String ipaUrl = referer.substring(0, idx) + ".ipa";
      if (!StringUtils.isEmpty(otaClassifier)) {
        if (!StringUtils.isEmpty(ipaClassifier)) {
          ipaUrl = replaceLast(ipaUrl, "-" + otaClassifier, "-" + ipaClassifier);
        }
        else {
          ipaUrl = replaceLast(ipaUrl, "-" + otaClassifier, "");
        }
      }
      else {
        if (!StringUtils.isEmpty(ipaClassifier)) {
          ipaUrl = ipaUrl.substring(0, idx) + "-" + ipaClassifier + ".ipa";
        }
      }
      return new URL(ipaUrl);
    }
    else {
      throw new MalformedURLException("Referer does not end with a file (e.g. .htm): "+referer);
    }
  }

  public static String replaceLast(String string, String searchString, String replaceString)
  {
    if (string == null || searchString == null || replaceString == null) {
      throw new NullPointerException();
    }
    if (StringUtils.isEmpty(searchString)) {
      return string;
    }
    if (StringUtils.countMatches(string, searchString) <= 1) {
      return string.replace(searchString, replaceString);
    }
    else {
      int idx = string.lastIndexOf(searchString);
      return string.substring(0, idx) + string.substring(idx).replace(searchString, replaceString);
    }
  }

  public static String urlEncode(String string)
  {
    if (string == null) {
      return null;
    }
    try {
      return URLEncoder.encode(string, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e); //should never happen
    }
  }

  public static String urlDecode(String string)
  {
    if (string == null) {
      return null;
    }
    try {
      return URLDecoder.decode(string, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e); //should never happen
    }
  }

  public static String encode(String string)
  {
    if (string == null) {
      return null;
    }
    try {
      byte[] bytes = string.getBytes("UTF-8");
      byte[] base64Bytes = Base64.encodeBase64(bytes);
      String base64String = new String(base64Bytes, "US-ASCII");
      return URLEncoder.encode(base64String, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e); //should never happen
    }
  }

  public static String decode(String string)
  {
    if (string == null) {
      return null;
    }
    try {
      String urlDecodedString = URLDecoder.decode(string, "UTF-8");
      byte[] urlDecodedBytes = urlDecodedString.getBytes("US-ASCII");
      byte[] decodedBase64Bytes = Base64.decodeBase64(urlDecodedBytes);
      return new String(decodedBase64Bytes, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e); //should never happen
    }
  }

}
