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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.webkit.WebkitConnection.NotificationHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO(devoncarew): review and add new css methods to this file

/**
 * A WIP css domain object.
 * <p>
 * This domain exposes CSS read/write operations. All CSS objects (stylesheets, rules, and styles)
 * have an associated <code>id</code> used in subsequent operations on the related object. Each
 * object type has a specific <code>id</code> structure, and those are not interchangeable between
 * objects of different kinds. CSS objects can be loaded using the <code>get*ForNode()</code> calls
 * (which accept a DOM node id). A client can also discover all the existing stylesheets with the
 * <code>getAllStyleSheets()</code> method (or keeping track of the <code>styleSheetAdded</code>/
 * <code>styleSheetRemoved</code> events) and subsequently load the required stylesheet contents
 * using the <code>getStyleSheet[Text]()</code> methods.
 */
@WebkitUnsupported
public class WebkitCSS extends WebkitDomain {
  public static interface CSSListener {
    /**
     * Fires whenever a MediaQuery result changes (for example, after a browser window has been
     * resized.) The current implementation considers only viewport-dependent media features.
     */
    public void mediaQueryResultChanged();

    /**
     * Called when a style sheet is added.
     * 
     * @param styleSheet
     */
    public void styleSheetAdded(WebkitStyleSheetRef styleSheet);

    /**
     * Called when the given style sheet changes.
     * 
     * @param styleSheetId
     */
    public void styleSheetChanged(String styleSheetId);

    /**
     * Called when the given style sheet is removed.
     * 
     * @param styleSheetId
     */
    public void styleSheetRemoved(String styleSheetId);
  }

  private static final String STYLE_SHEET_ADDED = "CSS.styleSheetAdded";
  private static final String STYLE_SHEET_CHANGED = "CSS.styleSheetChanged";
  private static final String STYLE_SHEET_REMOVED = "CSS.styleSheetRemoved";

  private static final String MEDIA_QUERY_RESULT_CHANGED = "CSS.mediaQueryResultChanged";

  private List<CSSListener> listeners = new ArrayList<WebkitCSS.CSSListener>();
  private List<WebkitStyleSheetRef> styleSheets = Collections.synchronizedList(new ArrayList<WebkitStyleSheetRef>());

  /**
   * @param connection
   */
  public WebkitCSS(WebkitConnection connection) {
    super(connection);

    connection.registerNotificationHandler("CSS.", new NotificationHandler() {
      @Override
      public void handleNotification(String method, JSONObject params) throws JSONException {
        handleCssNotification(method, params);
      }
    });
  }

  public void addCSSListener(CSSListener listener) {
    listeners.add(listener);
  }

  public void disable() throws IOException {
    sendSimpleCommand("CSS.disable");
  }

  public void enable() throws IOException {
    sendSimpleCommand("CSS.enable");
  }

  public void getStyleSheet(String styleSheetId, final WebkitCallback<WebkitStyleSheet> callback)
      throws IOException {
// "result":{
//    "styleSheet":{
//       "text":"h1 { font-size: 10pt }",
//       "styleSheetId":"1",
//       "rules":[
//          {
//             "sourceLine":3,
//             "style":{ ... },
//             "sourceURL":"http://0.0.0.0:3030/Users/dcarew/projects/dart/dart/samples/clock/Clock.html",
//             "selectorText":"h1",
//             "ruleId":{
//                "ordinal":0,
//                "styleSheetId":"1"
//             },
//             "origin":"regular",
//             "selectorRange":{
//                "start":0,
//                "end":2
//             }
//          }
//       ]
//    }
// }

//     "result":{"styleSheet":{"text":"h2 {\n  font-size: 8pt;\n}\n","styleSheetId":"2","rules" :
//     [{"sourceLine":0,"style":{"styleId":{"ordinal":0,"styleSheetId":"2"},"height":"","range":{"start" :
//     4,"end":23},"width":"","cssText":"\n  font-size: 8pt;\n","shorthandEntries":[],"cssProperties" :
//     [{"text":"font-size: 8pt;","range":{"start":3,"end":18},"status":"active","name":"font-size",
//     "implicit":false,"value":"8pt"}]},"sourceURL":
//     "http://0.0.0.0:3030/Users/foo/projects/dart/dart/samples/clock/clockstyle.css"
//     ,"selectorText":"h2","ruleId":{"ordinal":0,"styleSheetId":"2"},"origin":"regular","selectorRange"
//     :{"start":0,"end":2}}]}}

    try {
      JSONObject request = new JSONObject();

      request.put("method", "CSS.getStyleSheet");
      request.put("params", new JSONObject().put("styleSheetId", styleSheetId));

      connection.sendRequest(request, new WebkitConnection.Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetStyleSheetResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public List<WebkitStyleSheetRef> getStyleSheets() {
    return styleSheets;
  }

  public void getStyleSheetText(String styleSheetId, final WebkitCallback<String> callback)
      throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "CSS.getStyleSheetText");
      request.put("params", new JSONObject().put("styleSheetId", styleSheetId));

      connection.sendRequest(request, new WebkitConnection.Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetStyleSheetTextResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getSupportedCSSProperties(final WebkitCallback<String[]> callback) throws IOException {
    sendSimpleCommand("CSS.getSupportedCSSProperties", new WebkitConnection.Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        callback.handleResult(convertGetSupportedPropertiesResult(result));
      }
    });
  }

  public void removeCSSListener(CSSListener listener) {
    listeners.remove(listener);
  }

  public void setStyleSheetText(String styleSheetId, String text) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "CSS.setStyleSheetText");
      request.put("params", new JSONObject().put("styleSheetId", styleSheetId).put("text", text));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  protected void handleCssNotification(String method, JSONObject params) throws JSONException {
    if (method.equals(STYLE_SHEET_ADDED)) {
      // {"method":"CSS.styleSheetAdded","params":{"header":{"title":"","frameId":"69818.1",
      // "sourceURL":"http://127.0.0.1:3030/Users/devoncarew/dart/todomvc-47/web/out/index.html",
      // "origin":"regular","styleSheetId":"7","disabled":false}}}

      WebkitStyleSheetRef styleSheet = WebkitStyleSheetRef.createFrom(params.getJSONObject("header"));

      styleSheets.add(styleSheet);

      //String styleSheetId = params.getJSONObject("header").getString("styleSheetId");

      for (CSSListener listener : listeners) {
        listener.styleSheetAdded(styleSheet);
      }
    } else if (method.equals(STYLE_SHEET_CHANGED)) {
      String styleSheetId = params.getString("styleSheetId");

      for (CSSListener listener : listeners) {
        listener.styleSheetChanged(styleSheetId);
      }
    } else if (method.equals(STYLE_SHEET_REMOVED)) {
      String styleSheetId = params.getString("styleSheetId");

      for (CSSListener listener : listeners) {
        listener.styleSheetRemoved(styleSheetId);
      }
    } else if (method.equals(MEDIA_QUERY_RESULT_CHANGED)) {
      for (CSSListener listener : listeners) {
        listener.mediaQueryResultChanged();
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

  void frameStartedLoading() {
    // Clear out our cached style sheet information.
    styleSheets.clear();
  }

  private WebkitResult<WebkitStyleSheet> convertGetStyleSheetResult(JSONObject object)
      throws JSONException {
    WebkitResult<WebkitStyleSheet> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      result.setResult(WebkitStyleSheet.createFrom(object.getJSONObject("result").getJSONObject(
          "styleSheet")));
    }

    return result;
  }

  private WebkitResult<String> convertGetStyleSheetTextResult(JSONObject object)
      throws JSONException {
    WebkitResult<String> result = WebkitResult.createFrom(object);

    // "result":{"text":"h1 { font-size: 10pt }"}

    if (object.has("result")) {
      JSONObject obj = object.getJSONObject("result");

      result.setResult(obj.getString("text"));
    }

    return result;
  }

  private WebkitResult<String[]> convertGetSupportedPropertiesResult(JSONObject object)
      throws JSONException {
    WebkitResult<String[]> result = WebkitResult.createFrom(object);

    // "result": {
    //   "cssProperties": [ "color","direction","display","font","font-family",
    //     "font-size","font-style","font-variant","font-weight","text-rendering","-webkit-font-feature-settings" ...
    //   ]
    // }

    if (object.has("result")) {
      JSONObject obj = object.getJSONObject("result");

      JSONArray arr = obj.getJSONArray("cssProperties");

      String[] properties = new String[arr.length()];

      for (int i = 0; i < properties.length; i++) {
        properties[i] = arr.getString(i);
      }

      result.setResult(properties);
    }

    return result;
  }

}
