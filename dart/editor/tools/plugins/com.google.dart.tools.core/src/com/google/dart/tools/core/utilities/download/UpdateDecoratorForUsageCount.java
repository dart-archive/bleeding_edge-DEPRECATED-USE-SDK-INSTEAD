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

import org.eclipse.core.runtime.CoreException;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

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
    //cid=6cdf06c7-2800-4144-8f30-e7925175d4ba  -- Random GUID persisted in workspace
    //&r=c537bd11-1cf8-4244-98ff-90f3fe63ccbe   -- Random tag appended each time
    //&v=1.7.1                                  -- Version

    StringBuilder sb = new StringBuilder();

    if (!url.contains("?")) {
      sb.append("?");
    } else {
      sb.append("&");
    }

    String cid = getCIDParam();
    String r = getRandomParam();
    String v = getVersionParam();

    sb.append("cid=");
    sb.append(cid);

    sb.append("&");
    sb.append("v=");
    sb.append(v);

    sb.append("&");
    sb.append("r=");
    sb.append(r);

    return url + sb.toString();
  }

  /**
   * Get the Client ID parameter, generating a new one where needed
   */
  public static String getCIDParam() {
    String cid = DartCore.getPlugin().getPrefs().get(UpdateDecoratorForUsageCount.PREF_USER_CID, "");

    if (cid.isEmpty()) {
      //Generate a new CID
      cid = UUID.randomUUID().toString();
      DartCore.getPlugin().getPrefs().put(UpdateDecoratorForUsageCount.PREF_USER_CID, cid);

      try {
        DartCore.getPlugin().savePrefs();
      } catch (CoreException e) {
        DartCore.logError(e);
      }
    }
    return cid;
  }

  /**
   * Get a random parameter for appending to the query
   */
  static String getRandomParam() {
    return UUID.randomUUID().toString();
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
