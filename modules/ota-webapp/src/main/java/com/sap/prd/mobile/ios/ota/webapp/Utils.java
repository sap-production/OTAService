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

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.plexus.components.cipher.Base64;

import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.sap.prd.mobile.ios.ota.lib.LibUtils;

public class Utils
{

  private static final String US_ASCII = "US-ASCII";
  private static final String UTF_8 = "UTF-8";

  /**
   * Removes the file part of a URL if it has one
   * 
   * @param url
   */
  public static String removeFilePartFromURL(String url)
  {
    int idxDot = url.lastIndexOf('.');
    int idxSlash = url.lastIndexOf('/');
    if (idxDot < 0 || idxSlash < 0) {
      return url;
    }
    if (idxDot > idxSlash) {
      return url.substring(0, idxSlash);
    }
    return url;
  }

  /**
   * Returns the referer from parameter 'Referer' or from header parameter 'Referer'. The request
   * parameter 'Referer' (if set) has priority to the header referer.
   * 
   * @param request
   * @return referer or null if not existing in neither of both
   * @throws IOException
   */
  public static String getReferer(HttpServletRequest request)
        throws IOException
  {
    String referer = request.getParameter("Referer");
    if (referer == null) {
      referer = request.getHeader("Referer");
    }
    return referer;
  }

  /**
   * Returns the referer from parameter 'Referer' or from header parameter 'Referer'. The request
   * parameter 'Referer' (if set) has priority to the header referer.
   * 
   * If no referer can be found a 400 error is send to the client and this method throws a
   * ServletException.
   * 
   * @param request
   * @param response
   * @return
   * @throws IOException
   * @throws ServletException
   *           thrown if no referer
   */
  public static String getRefererSendError(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
  {
    String referer = Utils.getReferer(request);
    if (referer == null) {
      response.sendError(400, "Referer required");
      throw new ServletException("Referer missing");
    }
    return referer;
  }

  public static String urlEncode(String string)
  {
    if (string == null) {
      return null;
    }
    try {
      byte[] bytes = string.getBytes(UTF_8);
      byte[] base64Bytes = Base64.encodeBase64(bytes);
      String base64String = new String(base64Bytes, US_ASCII);
      return URLEncoder.encode(base64String, UTF_8);
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
      String urlDecodedString = URLDecoder.decode(string, UTF_8);
      byte[] urlDecodedBytes = urlDecodedString.getBytes(US_ASCII);
      byte[] decodedBase64Bytes = Base64.decodeBase64(urlDecodedBytes);
      return new String(decodedBase64Bytes, UTF_8);
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e); //should never happen
    }
  }

  /**
   * This method parses an URI after the <code>serviceName</code> into the elements separated by '/'
   * and '=' and returns the key value pairs.<br>
   * <ol>
   * <li>First the serviceName element is identified. Only elements after it are parsed.
   * <li>The single elements are split at '/'
   * <li>Each element is URLDecoded
   * <li>Each element is split into key and value at '=' and put into a String[2].<br>
   * If no '=' is contained the element is put into a String[1].
   * </ol>
   * Here some examples: <br>
   * <table border='1'>
   * <tr>
   * <th>uri</th>
   * <th>result</th>
   * <th>comment</th>
   * </tr>
   * <tr>
   * <td>/mywebapp/serviceName/a=b/c=d/e=f</td>
   * <td>{{"a", "b"}, {"c", "d"}, {"e", "f"}}</td>
   * <td>key value pairs are split</td>
   * </tr>
   * <tr>
   * <td>/mywebapp/serviceName/a%3Db/c%3Dd/e%3Df</td>
   * <td>{{"a", "b"}, {"c", "d"}, {"e", "f"}}</td>
   * <td>URLEncoded elements are first decoded</td>
   * </tr>
   * <tr>
   * <td>/mywebapp/serviceName/a=b=c</td>
   * <td>{{"a", "b=c"}}</td>
   * <td>key/value pairs are only split at the first '='</td>
   * </tr>
   * <tr>
   * <td>/mywebapp/serviceName/b/c=d/f</td>
   * <td>{{"b"}, {"c", "d"}, {"f"}}</td>
   * <td>Single values are returned in a <code>String[1]</code></td>
   * </tr>
   * <tr>
   * <td>/mywebapp/NOserviceName/a=b</td>
   * <td><code>null</code></td>
   * <td>If the <code>serviceName</code> is missing null is returned</code></td>
   * </tr>
   * </table>
   * 
   * 
   * @param request
   *          The request containing the requestURI
   * @param serviceName
   *          The name of the service in the URI
   * @return <code>String</code> array containing <code>String[1]</code> and <code>String[2]</code>
   *         elements
   */
  public static String[][] extractParametersFromUri(HttpServletRequest request, String serviceName)
  {
    if (request.getRequestURI() == null) {
      return null;
    }
    String[] requestURI = request.getRequestURI().split("/");
    String[][] result = null;
    boolean startFound = false;
    int readIdx = 0, writeIdx = 0;
    for (String element : requestURI) {
      readIdx++;
      if (startFound) {
        String decoded = LibUtils.decode(element);
        result[writeIdx++] = parseKeyValuePair(decoded);
      }
      else {
        if (element.equals(serviceName)) {
          startFound = true;
          result = new String[requestURI.length - readIdx][2];
        }
      }
    }
    return result;
  }

  /**
   * Splits "key=value" at '=' and returns {"key", "value"}.<br>
   * For "value" (without '=') {"value"} is returned.
   * 
   * @param decoded
   * @return
   */
  static String[] parseKeyValuePair(String decoded)
  {
    if (decoded == null) {
      return null;
    }
    int idx = decoded.indexOf("=");
    if (idx < 0) {
      return new String[] { decoded };
    }
    else {
      return new String[] { decoded.substring(0, idx), decoded.substring(idx + 1) };
    }
  }

  /**
   * Searches in the map for an element having the specified key assigned and returns the value.
   * 
   * @param map
   * @param key
   * @return
   */
  public static String getValueFromUriParameterMap(String[][] map, String key)
  {
    if (map == null || key == null) {
      return null;
    }
    for (String[] element : map) {
      if (element.length == 2) {
        if (element[0] != null && key.equals(element[0])) {
          return element[1];
        }
      }
    }
    return null;
  }

  public static void sendQRCode(HttpServletRequest request, HttpServletResponse response, String contents)
        throws IOException, WriterException, URISyntaxException
  {
    sendQRCode(request, response, contents, null);
  }

  public static void sendQRCode(HttpServletRequest request, HttpServletResponse response, String contents,
        MatrixToImageConfig config) throws IOException, WriterException, URISyntaxException
  {
    response.setContentType("image/png");
    ServletOutputStream os = response.getOutputStream();
    try {
      if (config == null) {
        QREncoder.encode(contents, os);
      }
      else {
        QREncoder.encode(contents, os, config);
      }
      os.flush();
    }
    finally {
      closeQuietly(os);
    }
  }

  public final static String QR_ON_COLOR = "qrOnColor";
  public final static String QR_OFF_COLOR = "qrOffColor";
  public final static int QR_ON_COLOR_DEFAULT = 0xFF000000;
  public final static int QR_OFF_COLOR_DEFAULT = 0x00FFFFFF;

  public static MatrixToImageConfig getMatrixToImageConfig(HttpServletRequest request)
  {
    String onString = request.getParameter(QR_ON_COLOR);
    String offString = request.getParameter(QR_OFF_COLOR);
    if (onString == null || offString == null) return null;
    int on = Integer.parseInt(onString);
    int off = Integer.parseInt(offString);
    return new MatrixToImageConfig(on, off);
  }

}
