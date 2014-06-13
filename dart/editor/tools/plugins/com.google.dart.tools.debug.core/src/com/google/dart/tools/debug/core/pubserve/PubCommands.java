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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to encapsulate the available pub protocol commands.
 */
public class PubCommands {
  private PubConnection connection;

  protected PubCommands(PubConnection connection) {
    this.connection = connection;
  }

  /**
   * Sends a message to pub to indicate that it should shutdown when connection is closed. There is
   * no result returned by pub.
   * 
   * @throws IOException
   */
  public void exitOnClose() throws IOException {
    // {"method": "exitOnClose"}

    try {
      JSONObject request = new JSONObject();

      request.put("jsonrpc", "2.0");

      request.put("method", "exitOnClose");

      connection.sendRequest(request, null);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Given an asset ID, returns the relative URI path that asset would be served at.
   * 
   * @param path
   * @param callback
   * @throws IOException
   */
  public void pathToUrl(String path, final PubCallback<String> callback) throws IOException {
    // { "method": "pathToUrls", "path": "web/index.html" }
    // ==>{ "urls": ["http://localhost:8080/index.html"] }

    try {
      JSONObject request = new JSONObject();

      request.put("jsonrpc", "2.0");

      request.put("method", "pathToUrls");

      Map<String, String> map = new HashMap<String, String>();
      map.put("path", path);
      request.put("params", map);

      connection.sendRequest(request, new PubConnection.Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertAssetToUrlResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Given a relative directory path within the entrypoint package, binds a new port to serve from
   * that path and returns its URL.
   * <p>
   * If successful, it returns a map containing the URL that can be used to access the directory.
   */
  public void serveDirectory(String path, final PubCallback<String> callback) throws IOException {
    // { "method": "serveDirectory","path": "example/awesome" } 
    // ==> {  "url": "http://localhost:8083" } 
    try {
      JSONObject request = new JSONObject();

      request.put("jsonrpc", "2.0");

      request.put("method", "serveDirectory");

      Map<String, String> map = new HashMap<String, String>();
      map.put("path", path);
      request.put("params", map);

      connection.sendRequest(request, new PubConnection.Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertServeDirectoryResult(result));
        }

      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }

  }

  /**
   * Given a URL to an asset that is served by pub, returns the ID of the asset that would be
   * accessed by that URL.
   * <p>
   * If successful, it returns a map containing the asset ID's package and path
   * 
   * @param url
   * @param callback
   * @throws IOException
   */
  public void urlToAssetId(String url, final PubCallback<PubAsset> callback) throws IOException {
    // {"method": "urlToAssetId", "uri": "<relative uri path>"}
    // ==> {"package": "<package>", "path": "<path>"}

    try {
      JSONObject request = new JSONObject();

      request.put("jsonrpc", "2.0");

      request.put("method", "urlToAssetId");

      Map<String, String> map = new HashMap<String, String>();
      map.put("url", url);

      request.put("params", map);

      connection.sendRequest(request, new PubConnection.Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertUrlToAssetResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  private PubResult<String> convertAssetToUrlResult(JSONObject obj) throws JSONException {
    PubResult<String> result = PubResult.createFrom(obj);

    //TODO(keertip): return multiple urls
    if (obj.has("result")) {
      JSONObject jsonObj = (JSONObject) obj.get("result");
      if (jsonObj.has("urls")) {
        result.setResult(jsonObj.getJSONArray("urls").getString(0));
      }
    }
    return result;
  }

  private PubResult<String> convertServeDirectoryResult(JSONObject obj) throws JSONException {
    PubResult<String> result = PubResult.createFrom(obj);
    if (obj.has("result")) {
      JSONObject resObj = (JSONObject) obj.get("result");
      if (resObj.has("url")) {
        result.setResult(resObj.getString("url"));
      }
    }
    return result;
  }

  private PubResult<PubAsset> convertUrlToAssetResult(JSONObject obj) throws JSONException {
    PubResult<PubAsset> result = PubResult.createFrom(obj);
    if (obj.has("result")) {
      JSONObject resObj = (JSONObject) obj.get("result");
      result.setResult(PubAsset.createFrom(resObj));
    }
    return result;
  }

}
