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
package com.google.dart.tools.core.frog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A JSON object that represents a response from the frog server
 */
public class Response {
  public enum Kind {
    Done,
    Message,
    Unknown
  }

  private static final Map<String, Kind> kindMap = new HashMap<String, Kind>();
  static {
    kindMap.put("done", Kind.Done);
    kindMap.put("message", Kind.Message);
  }

  private final JSONObject json;

  public Response(JSONObject json) {
    this.json = json;
  }

  public ResponseDone createDoneResponse() throws JSONException {
    return new ResponseDone(json);
  }

  public ResponseMessage createMessageResponse() throws JSONException {
    return new ResponseMessage(json);
  }

  public int getId() throws JSONException {
    return json.getInt("id");
  }

  public Kind getKind() throws JSONException {
    Kind kind = kindMap.get(getKindText());
    return kind != null ? kind : Kind.Unknown;
  }

  public String getKindText() throws JSONException {
    return json.getString("kind");
  }

  @Override
  public String toString() {
    return "Response[" + json.toString() + "]";
  }

}
