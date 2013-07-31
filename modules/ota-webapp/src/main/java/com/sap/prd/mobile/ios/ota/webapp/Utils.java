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

import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_REFERER;
import static com.sap.prd.mobile.ios.ota.lib.LibUtils.decode;
import static com.sap.prd.mobile.ios.ota.webapp.OtaHtmlService.HTML_SERVICE_SERVLET_NAME;
import static com.sap.prd.mobile.ios.ota.webapp.OtaPlistService.PLIST_SERVICE_SERVLET_NAME;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.awt.Dimension;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.plexus.components.cipher.Base64;

import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;

public class Utils
{

  private static final String US_ASCII = "US-ASCII";
  private static final String UTF_8 = "UTF-8";

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
    String referer = request.getParameter(KEY_REFERER);
    if (referer == null) {
      referer = request.getHeader(KEY_REFERER);
    }
    else {
      if (!referer.contains("://")) {
        referer = decode(referer);
        int idx = referer.indexOf("://");
        if (idx < 0) throw new IOException(":// still not contained after decoding Referer");
        idx = referer.lastIndexOf("=", idx);
        if (idx >= 0) referer = referer.substring(idx + 1);
      }
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

  public static Map<String, String> getParametersAndReferer(HttpServletRequest request, HttpServletResponse response,
        boolean exceptionIfRefererMissing) throws IOException, ServletException
  {
    Map<String, String> params = new HashMap<String, String>();
    String referer = exceptionIfRefererMissing ? getRefererSendError(request, response) : getReferer(request);
    Map<String, String[]> requestParams = request.getParameterMap();
    for (String key : requestParams.keySet()) {
      String[] values = requestParams.get(key);
      if (values.length == 1) params.put(key, values[0]);
    }
    params.put(KEY_REFERER, referer);
    return params;
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
   * <li>Each element is split into key and value at '=' and put into the result map.<br>
   * If no '=' is contained the value of the key is null.
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
   * @param serviceUrlPattern
   *          The name of the service in the URI
   * @return <code>String</code> array containing <code>String[1]</code> and <code>String[2]</code>
   *         elements
   */
  public static Map<String, String> extractSlashedEncodedParametersFromUri(HttpServletRequest request, String serviceUrlPattern)
  {
    Map<String, String> result = new HashMap<String, String>();
    String uri = request.getRequestURI();

    if (!uri.startsWith(request.getContextPath())) throw new IllegalStateException(
          format("URI '%s' does not start with context path '%s'", uri, request.getContextPath()));
    uri = uri.substring(request.getContextPath().length()); // e.g. "/ota-service"

    if (!uri.startsWith(serviceUrlPattern)) throw new IllegalStateException(
          format("URI '%s' does not start with serviceUrlPattern '%s'", uri, serviceUrlPattern));
    uri = uri.substring(serviceUrlPattern.length()); // e.g. "/PLIST"

    if (uri.startsWith("/")) uri = uri.substring("/".length());

    String[] elements = uri.split("/");
    for (String element : elements) {
      String[] keyValue = parseKeyValuePair(decode(element));
      result.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : null);
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

  public static void sendQRCode(HttpServletRequest request, HttpServletResponse response, String contents)
        throws IOException, WriterException, URISyntaxException
  {
    sendQRCode(request, response, contents, null, null);
  }

  public static void sendQRCode(HttpServletRequest request, HttpServletResponse response, String contents,
        MatrixToImageConfig config, Dimension dimension) throws IOException, WriterException, URISyntaxException
  {
    response.setContentType("image/png");
    ServletOutputStream os = response.getOutputStream();
    QREncoder.encode(contents, os, config, dimension);
    os.flush();
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

  //user-agent infos: E.g. http://html5-mobile.de/blog/wichtigsten-user-agents-mobile-devices-jquery-mobile    
  // user-agent=Mozilla/5.0 (iPhone; CPU iPhone OS 5_1_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3
  // user-agent=Mozilla/5.0 (iPad; CPU OS 6_1_3 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10B329 Safari/8536.25
  // user-agent=Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)

  public static boolean isIDevice(HttpServletRequest request) throws UnknownInformationException
  {
    String agent = request.getHeader("user-agent");
    if (agent == null) throw new UnknownInformationException("user-agent not specified in header");
    return agent.contains("iPhone") || agent.contains("iPad");
  }

  public static boolean isMobile(HttpServletRequest request) throws UnknownInformationException
  {
    String agent = request.getHeader("user-agent");
    if (agent == null) throw new UnknownInformationException("user-agent not specified in header");
    if (agent.contains("Mobile")) return true;
    if (agent.contains("iPhone") || agent.contains("iPad")) return true;
    if (agent.contains("Android")) return true;
    //TODO: ...
    return false;
  }

  public static boolean isDesktop(HttpServletRequest request) throws UnknownInformationException
  {
    return !isMobile(request);
  }

  @SuppressWarnings("serial")
  static class UnknownInformationException extends RuntimeException
  {
    public UnknownInformationException(String string)
    {
      super(string);
    }
  }

  public static String getRequestInfosForLog(HttpServletRequest request)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("REQUEST URL: ").append(request.getRequestURL());
    sb.append(", REQUEST PARAMETERS:{");
    Map<String, String[]> params = request.getParameterMap();
    boolean first = true;
    for (Object key : params.keySet()) {
      String[] values = params.get(key);
      if (!first) sb.append(", ");
      sb.append(key).append("=").append(Arrays.toString(values));
      first = false;
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Gets the <code>url-pattern</code> configured in the <code>servlet-mapping</code> for the specified <code>servletName</code>.<br/>
   * The trailing "/*" is removed.
   * @param request
   * @param servletName
   * @return url-pattern without "/*"
   */
  public static String getServletMappingUrlPattern(HttpServletRequest request, String servletName)
  {
    try {
      ServletRegistration servletRegistration = request.getServletContext().getServletRegistration(servletName);
      String firstMapping = servletRegistration.getMappings().iterator().next();
      if (firstMapping.endsWith("/*")) firstMapping = firstMapping.substring(0, firstMapping.length() - "/*".length());
      return firstMapping;
      
    } catch (NoSuchMethodError e) {
      //Workaround - Servlet 3.0 API required - not contained before Tomcat 7
      if(servletName.equals(HTML_SERVICE_SERVLET_NAME)) {
        return "/HTML";
      } else if(servletName.equals(PLIST_SERVICE_SERVLET_NAME)) {
        return "/PLIST";
      } else {
        throw e;
      }
    }
  }

  /**
   * Returns the base URL of the service having the specified <code>servletName</code>.
   * @param request
   * @param servletName
   * @return base URL of the service
   * @throws MalformedURLException
   */
  public static URL getServiceBaseUrl(HttpServletRequest request, String servletName) throws MalformedURLException
  {
    String requestUrl = request.getRequestURL().toString(); //e.g. "http://host:8765/ota-service/HTML/UmVmZXJlcj1odHRw..."
    String contextPath = request.getContextPath().toString(); //e.g. "/ota-service" or "" if root context
    String serviceUrlPattern = getServletMappingUrlPattern(request, servletName); //e.g. "/PLIST"

    String result;
    if(!isEmpty(contextPath)) {
      int idx = requestUrl.indexOf(contextPath);
      if (idx < 0) throw new IllegalStateException(format("Cannot find '%s' in '%s'", contextPath, requestUrl));
      result = requestUrl.substring(0, idx + contextPath.length()); //e.g. "http://host:8765/ota-service"
    } else { //root context
      int idx = requestUrl.indexOf("//");
      idx = requestUrl.indexOf("/", idx+"//".length());
      result = requestUrl.substring(0, idx); //e.g. "http://host:8765"
    }
    result += serviceUrlPattern; //e.g. "http://host:8765/ota-service/PLIST"
    return new URL(result);
  }

}
