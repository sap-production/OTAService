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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestUtils
{

  public static void assertContains(String expected, String value)
  {
    if (!value.contains(expected)) {
      fail(String.format("Expected is contained: '%s' but was '%s'", expected, value));
    }
  }
  
  public static void assertOtaLink(CharSequence s, String plistURL, String bundleIdentifier)
  {
    Pattern checkOtaLinkPattern = Pattern.compile("href='itms-services:///\\?action=download-manifest&url=([^']+)'>Install Over-The-Air</a>");
    Matcher checkOtaLinkMatcher = checkOtaLinkPattern.matcher(s);
    assertTrue("Ota link not found in "+s, checkOtaLinkMatcher.find());
    assertEquals(plistURL, checkOtaLinkMatcher.group(1));
  }

}
