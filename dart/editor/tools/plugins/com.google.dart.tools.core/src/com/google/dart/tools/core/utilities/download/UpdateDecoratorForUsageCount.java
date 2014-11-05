/*
 * Copyright (c) 2014, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.utilities.download;

import com.google.dart.tools.core.DartCore;

import java.io.UnsupportedEncodingException;

/**
 * Methods for handling user counting on update URLs
 */
public class UpdateDecoratorForUsageCount {

  /**
   * Preference for storing the user's anonymous CID
   */
  public static final String PREF_USER_CID = "userCid";

  /**
   * Take a base URL for a revision and append query parameters to it before requesting update
   */
  public static String decorateURL(String url) {
    //Data that should be appended
    //e.g.
    //&r=c537bd11-1cf8-4244-98ff-90f3fe63ccbe   -- Random tag appended each time

    StringBuilder sb = new StringBuilder();

    if (!url.contains("?")) {
      sb.append("?");
    } else {
      sb.append("&");
    }

    String v = getVersionParam();

    sb.append("v=");
    sb.append(v);

    return url + sb.toString();
  }

  /**
   * Get an URL encoded form of the version parameter
   */
  static String getVersionParam() {
    try {
      return java.net.URLEncoder.encode(getVersionString(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return "versonErr";
    }
  }

  private static String getVersionString() {
    return DartCore.getVersion();
  }

}
