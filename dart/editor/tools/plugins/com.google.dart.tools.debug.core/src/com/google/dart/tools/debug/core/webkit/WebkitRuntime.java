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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * A WIP runtime domain object.
 * <p>
 * Runtime domain exposes JavaScript runtime by means of remote evaluation and mirror objects.
 * Evaluation results are returned as mirror object that expose object type, string representation
 * and unique identifier that can be used for further object reference. Original objects are
 * maintained in memory unless they are either explicitly released or are released along with the
 * other objects in their object group.
 * 
 * @see http://code.google.com/chrome/devtools/docs/protocol/tot/runtime.html
 */
public class WebkitRuntime extends WebkitDomain {

  public static class CallArgument {

    public static CallArgument fromDouble(double d) {
      CallArgument arg = new CallArgument();
      arg.value = new Double(d);
      return arg;
    }

    public static CallArgument fromInt(int i) {
      CallArgument arg = new CallArgument();
      arg.value = new Integer(i);
      return arg;
    }

    public static CallArgument fromObjectId(String objectId) {
      CallArgument arg = new CallArgument();
      arg.objectId = objectId;
      return arg;
    }

    public static CallArgument fromString(String str) {
      CallArgument arg = new CallArgument();
      arg.value = str;
      return arg;
    }

    private Object value;
    private String objectId;

    public JSONObject toJson() throws JSONException {
      JSONObject obj = new JSONObject();

      if (objectId != null) {
        obj.put("objectId", objectId);
      } else {
        obj.put("value", value);
      }

      return obj;
    }

  }

  public WebkitRuntime(WebkitConnection connection) {
    super(connection);
  }

  /**
   * Calls function with given declaration on the given object. Object group of the result is
   * inherited from the target object.
   * 
   * @param objectId Identifier of the object to call function on.
   * @param functionDeclaration Declaration of the function to call.
   * @param arguments Call arguments. All call arguments must belong to the same JavaScript world as
   *          the target object.
   * @param returnByValue Whether the result is expected to be a JSON object which should be sent by
   *          value.
   * @param callback
   * @throws IOException
   */
  public void callFunctionOn(String objectId, String functionDeclaration,
      List<CallArgument> arguments, boolean returnByValue,
      final WebkitCallback<WebkitRemoteObject> callback) throws IOException {
    // TODO: optional
    // boolean doNotPauseOnExceptionsAndMuteConsole
    // Specifies whether evaluation should stop on exceptions and mute console. Overrides setPauseOnException state.

    try {
      JSONObject request = new JSONObject();

      JSONObject params = new JSONObject();

      params.put("objectId", objectId);
      params.put("functionDeclaration", functionDeclaration);
      params.put("returnByValue", returnByValue);

      if (arguments != null) {
        params.put("arguments", argsToArray(arguments));
      }

      request.put("method", "Runtime.callFunctionOn").put("params", params);

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertEvaluateResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void callListLength(String objectId, final WebkitCallback<Integer> callback)
      throws IOException {
    if (objectId == null) {
      WebkitResult<Integer> result = new WebkitResult<Integer>();
      result.setResult(new Integer(0));
      callback.handleResult(result);
      return;
    }

    try {
      JSONObject request = new JSONObject();

      request.put("method", "Runtime.callFunctionOn");
      request.put(
          "params",
          new JSONObject().put("objectId", objectId).put("functionDeclaration", "() => length").put(
              "returnByValue",
              false));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          WebkitResult<WebkitRemoteObject> functionResult = convertEvaluateResult(result);

          WebkitResult<Integer> r = new WebkitResult<Integer>();

          if (functionResult.getWasThrown()) {
            r.setError(functionResult.getResult().getValue());
          } else {
            r.setResult(functionResult.getResult() == null ? new Integer(0) : new Integer(
                functionResult.getResult().getValue()));
          }

          callback.handleResult(r);
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
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
    if (objectId == null) {
      WebkitResult<String> result = new WebkitResult<String>();
      result.setResult(null);
      callback.handleResult(result);
      return;
    }

    try {
      JSONObject request = new JSONObject();

      request.put("method", "Runtime.callFunctionOn");
      request.put(
          "params",
          new JSONObject().put("objectId", objectId).put("functionDeclaration", "() => toString()").put(
              "returnByValue",
              false));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          WebkitResult<WebkitRemoteObject> functionResult = convertEvaluateResult(result);

          WebkitResult<String> r = new WebkitResult<String>();

          if (functionResult.getWasThrown()) {
            r.setError(functionResult.getResult().getValue());
          } else {
            r.setResult(functionResult.getResult() == null ? "null"
                : functionResult.getResult().getValue());
          }

          callback.handleResult(r);
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Evaluates expression on global object.
   * 
   * @param expression Expression to evaluate.
   * @param objectGroup (optional) Symbolic group name that can be used to release multiple objects.
   * @param returnByValue (optional) Whether the result is expected to be a JSON object that should
   *          be sent by value.
   * @throws IOException
   */
  public void evaluate(String expression, String objectGroup, boolean returnByValue,
      final WebkitCallback<WebkitRemoteObject> callback) throws IOException {
    // TODO: optional
    // boolean doNotPauseOnExceptionsAndMuteConsole
    // Specifies whether evaluation should stop on exceptions and mute console. Overrides setPauseOnException state.

    try {
      JSONObject request = new JSONObject();

      JSONObject params = new JSONObject();
      params.put("expression", expression);
      params.put("returnByValue", returnByValue);

      if (objectGroup != null) {
        params.put("objectGroup", objectGroup);
      }

      request.put("method", "Runtime.evaluate").put("params", params);

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertEvaluateResult(result));
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
   * @param object identifier of the object to return properties for
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

  /**
   * Tells inspected instance (worker or page) that it can run in case it was started paused.
   * 
   * @throws IOException
   */
  @WebkitUnsupported
  public void run() throws IOException {
    sendSimpleCommand("Runtime.run");
  }

  protected WebkitResult<WebkitRemoteObject> convertEvaluateResult(JSONObject object)
      throws JSONException {
    WebkitResult<WebkitRemoteObject> result = WebkitResult.createFrom(object);

//    "result": {
//      "result": <RemoteObject>,
//      "wasThrown": <boolean> 
//    }

    if (object.has("result")) {
      JSONObject obj = object.getJSONObject("result");

      WebkitRemoteObject remoteObject = WebkitRemoteObject.createFrom(obj.getJSONObject("result"));

      if (JsonUtils.getBoolean(obj, "wasThrown")) {
        result.setWasThrown(true);
        result.setResult(remoteObject);
      } else {
        result.setResult(remoteObject);
      }
    }

    return result;
  }

  private JSONArray argsToArray(List<CallArgument> arguments) throws JSONException {
    JSONArray arr = new JSONArray();

    for (CallArgument arg : arguments) {
      arr.put(arg.toJson());
    }

    return arr;
  }

  private WebkitResult<WebkitPropertyDescriptor[]> convertGetPropertiesResult(
      WebkitRemoteObject parentObject, JSONObject object) throws JSONException {
    WebkitResult<WebkitPropertyDescriptor[]> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONObject obj = object.getJSONObject("result");

      result.setResult(WebkitPropertyDescriptor.createFrom(obj.getJSONArray("result")));
    }

    return result;
  }

}
