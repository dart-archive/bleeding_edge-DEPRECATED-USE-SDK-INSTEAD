/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.util;

/**
 * A class for extracting the error message and location from a JSONTokener message so that we don't
 * have to modify the JSON source.
 */
public class JSONErrorMessageParser {
  private String errMsg;
  private int offset;

  /**
   * Extract information from the specified JSON error message
   * 
   * @param originalErrMsg the JSON error message (not <code>null</code>)
   */
  public JSONErrorMessageParser(String originalErrMsg) {
    errMsg = originalErrMsg;
    offset = 0;

    // Strip filename off beginning of message
    if (errMsg.startsWith("Error reading ")) {
      for (int i = 14; i < errMsg.length(); i++) {
        if (!Character.isWhitespace(errMsg.charAt(i))) {
          continue;
        }
        i++;
        if (i >= errMsg.length() || Character.isWhitespace(errMsg.charAt(i))) {
          break;
        }
        errMsg = errMsg.substring(i);
        break;
      }
    }

    // Strip " at character ###" off the end of the message
    for (int i = errMsg.length() - 2; i > 0; i--) {
      if (Character.isDigit(errMsg.charAt(i))) {
        continue;
      }
      i++;
      if (!errMsg.substring(0, i).endsWith(" at character ")) {
        break;
      }
      try {
        offset = Integer.valueOf(errMsg.substring(i));
        errMsg = errMsg.substring(0, i - 14);
      } catch (NumberFormatException ignored) {
        // Fall through without modifying the error message
      }
      break;
    }

    // Strip JSON reference off beginning of error message
    if (errMsg.startsWith("JSONObject[\"")) {
      int i = errMsg.indexOf('"', 12);
      if (i > 12 && i + 2 < errMsg.length()) {
        errMsg = errMsg.substring(11, i + 1) + errMsg.substring(i + 2);
      }
    }
  }

  /**
   * Answer the error message extracted from the original JSON error message
   * 
   * @return the error message (not <code>null</code>)
   */
  public String getErrorMessage() {
    return errMsg;
  }

  /**
   * Answer the offset in the file where the error occurred, or zero if the offset could not be
   * extracted from the original JSON error message.
   * 
   * @return the offset
   */
  public int getOffset() {
    return offset;
  }
}
