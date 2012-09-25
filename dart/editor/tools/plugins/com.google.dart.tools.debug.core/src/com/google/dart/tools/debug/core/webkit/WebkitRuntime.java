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
 * A WIP runtime domain object.
 * 
 * @see http://code.google.com/chrome/devtools/docs/protocol/tot/runtime.html
 */
public class WebkitRuntime extends WebkitDomain {

  public WebkitRuntime(WebkitConnection connection) {
    super(connection);
  }

  /**
   * Calls the toString() method on the given remote object. This is a convenience method for the
   * Runtime.callFunctionOn call.
   * 
   * @param objectId
   * @throws IOException
   */
  public void callToString(String objectId, final WebkitCallback<String> callback)
      throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Runtime.callFunctionOn");
      request.put(
          "params",
          new JSONObject().put("objectId", objectId).put(
              "functionDeclaration",
              "function(){return this.toString();}").put("returnByValue", false));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertCallFunctionOnResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
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
  public void getProperties(final WebkitRemoteObject object, boolean ownProperties,
      final WebkitCallback<WebkitPropertyDescriptor[]> callback) throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("callback is required");
    }

    try {
      JSONObject request = new JSONObject();

      request.put("method", "Runtime.getProperties");
      request.put(
          "params",
          new JSONObject().put("objectId", object.getObjectId()).put("ownProperties", ownProperties));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetPropertiesResult(object, result));
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

  protected WebkitResult<String> convertCallFunctionOnResult(JSONObject object)
      throws JSONException {
    WebkitResult<String> result = WebkitResult.createFrom(object);

//    "result": {
//      "result": <RemoteObject>,
//      "wasThrown": <boolean> 
//    }

    if (object.has("result")) {
      JSONObject obj = object.getJSONObject("result");

      WebkitRemoteObject remoteObject = WebkitRemoteObject.createFrom(obj.getJSONObject("result"));

      if (JsonUtils.getBoolean(obj, "wasThrown")) {
        result.setError(remoteObject);
      } else {
        result.setResult(remoteObject.getValue());
      }
    }

    return result;
  }

  private WebkitResult<WebkitPropertyDescriptor[]> convertGetPropertiesResult(
      WebkitRemoteObject parentObject, JSONObject object) throws JSONException {
    WebkitResult<WebkitPropertyDescriptor[]> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONObject obj = object.getJSONObject("result");

      WebkitPropertyDescriptor[] properties = WebkitPropertyDescriptor.createFrom(obj.getJSONArray("result"));

      for (WebkitPropertyDescriptor property : properties) {
        if (property.getName().equals(WebkitPropertyDescriptor.CLASS_INFO)) {
          parentObject.setClassInfo(property.getValue());
        }
      }

      result.setResult(properties);
    }

    return result;
  }

}
