/*
 * Copyright (c) 2012, the Dart project authors.
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

package com.google.dart.tools.debug.core.webkit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A lightweight representation of a Webkit CSS style sheet.
 * 
 * @see WebkitStyleSheet
 */
public class WebkitStyleSheetRef {

  static WebkitStyleSheetRef[] createFrom(JSONArray arr) throws JSONException {
    WebkitStyleSheetRef[] results = new WebkitStyleSheetRef[arr.length()];

    for (int i = 0; i < arr.length(); i++) {
      results[i] = createFrom(arr.getJSONObject(i));
    }

    return results;
  }

  static WebkitStyleSheetRef createFrom(JSONObject obj) throws JSONException {
    // {"title":"","sourceURL":"http://0.0.0.0:3030/Users/foo/projects/dart/dart/samples/clock/Clock.html",
    // "styleSheetId":"1","disabled":false}

    WebkitStyleSheetRef ref = new WebkitStyleSheetRef();

    ref.title = JsonUtils.getString(obj, "title");
    ref.sourceURL = JsonUtils.getString(obj, "sourceURL");
    ref.styleSheetId = JsonUtils.getString(obj, "styleSheetId");
    ref.disabled = JsonUtils.getBoolean(obj, "disable");

    return ref;
  }

  private String title;
  private String styleSheetId;
  private String sourceURL;
  private boolean disabled;

  public String getSourceURL() {
    return sourceURL;
  }

  public String getStyleSheetId() {
    return styleSheetId;
  }

  public String getTitle() {
    return title;
  }

  public boolean isDisabled() {
    return disabled;
  }

  @Override
  public String toString() {
    return "[" + styleSheetId + "," + sourceURL + "]";
  }

}
