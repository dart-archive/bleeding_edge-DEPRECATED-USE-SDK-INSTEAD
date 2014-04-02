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

package com.google.dart.tools.debug.core.pubserve;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A Pub connection return value object. The return value can either be an error (untyped) or an
 * untyped result object.
 */
public class PubResult<T> {

  static <T> PubResult<T> createFrom(JSONObject params) throws JSONException {
    PubResult<T> result = new PubResult<T>();

    if (params.has("error")) {
      result.setError(params.get("error"));
    }

    return result;
  }

  static JSONObject createJsonErrorResult(String message) throws JSONException {
    JSONObject obj = new JSONObject();

    obj.put("error", message);

    return obj;
  }

  private Object error;
  private T result;
  private boolean wasThrown;

  PubResult() {

  }

  public Object getError() {
    return error;
  }

  public String getErrorMessage() {
    return String.valueOf(error);
  }

  public T getResult() {
    return result;
  }

  public boolean getWasThrown() {
    return wasThrown;
  }

  public boolean isError() {
    return error != null;
  }

  @Override
  public String toString() {
    if (error != null) {
      return error.toString();
    } else if (result != null) {
      return result.toString();
    } else {
      return super.toString();
    }
  }

  void setError(Object error) {
    this.error = error;
  }

  void setResult(T result) {
    this.result = result;
  }

  void setWasThrown(boolean value) {
    this.wasThrown = value;
  }
}
