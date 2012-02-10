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

import com.google.dart.tools.debug.core.webkit.WebkitConnection.Callback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

// TODO(devoncarew): Runtime.callFunctionOn
// TODO(devoncarew): Runtime.evaluate

/**
 * A WIP runime domain object.
 * 
 * @see http://code.google.com/chrome/devtools/docs/protocol/tot/runtime.html
 */
public class WebkitRuntime extends WebkitDomain {

  public WebkitRuntime(WebkitConnection connection) {
    super(connection);
  }

  /**
   * Returns properties of a given object. Object group of the result is inherited from the target
   * object.
   * <p>
   * If successful, the WebkitResult object will contain an array of property descriptors.
   * 
   * @param objectGroup identifier of the object to return properties for
   * @param ownProperties if true, returns properties belonging only to the element itself, not to
   *          its prototype chain
   * @param callback
   * @throws IOException
   */
  public void getProperties(String objectId, boolean ownProperties, final WebkitCallback callback)
      throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("method", "Runtime.getProperties");
      request.put("params",
          new JSONObject().put("objectId", objectId).put("ownProperties", ownProperties));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetPropertiesResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Releases remote object with given id.
   * 
   * @param objectId
   * @throws IOException
   */
  public void releaseObject(String objectId) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Runtime.releaseObject");
      request.put("params", new JSONObject().put("objectId", objectId));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Releases all remote objects that belong to a given group.
   * 
   * @param objectGroup
   * @throws IOException
   */
  public void releaseObjectGroup(String objectGroup) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Runtime.releaseObjectGroup");
      request.put("params", new JSONObject().put("objectGroup", objectGroup));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  private WebkitResult convertGetPropertiesResult(JSONObject object) throws JSONException {
    WebkitResult result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONObject obj = object.getJSONObject("result");

      result.setResult(WebkitPropertyDescriptor.createFrom(obj.getJSONArray("result")));
    }

    return result;
  }

}
