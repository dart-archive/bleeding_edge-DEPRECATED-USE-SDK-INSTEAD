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
package com.google.dart.tools.core.frog;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A JSON object that represents a response from the frog server
 */
public class ResponseObject extends JSONObject {

  public ResponseObject(String message) throws JSONException {
    super(message);
  }

  public String getFileName() throws JSONException {
    return ((JSONObject) getSpan()).getString("file");
  }

  public int getId() throws JSONException {
    return getInt("id");
  }

  public String getKind() throws JSONException {
    return getString("kind");
  }

  public String getMessage() throws JSONException {
    return getString("message");
  }

  public String getPrefix() throws JSONException {
    return getString("prefix");
  }

  public Object getSpan() throws JSONException {
    return get("span");
  }

  public boolean hasSpan() throws JSONException {
    return getSpan() instanceof JSONObject;
  }

  public boolean isTrueResult() throws JSONException {
    return getBoolean("result");
  }

}
