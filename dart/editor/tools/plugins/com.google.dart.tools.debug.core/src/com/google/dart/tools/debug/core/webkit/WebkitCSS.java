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
import com.google.dart.tools.debug.core.webkit.WebkitConnection.Callback;
import com.google.dart.tools.debug.core.webkit.WebkitConnection.NotificationHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// virtual void getAllStyleSheets(ErrorString*, RefPtr<InspectorArray>& styleSheetInfos);
// virtual void getStyleSheet(ErrorString*, const String& styleSheetId, RefPtr<InspectorObject>&
// result);
// virtual void getStyleSheetText(ErrorString*, const String& styleSheetId, String* result);
// virtual void setStyleSheetText(ErrorString*, const String& styleSheetId, const String& text);
// virtual void setPropertyText(ErrorString*, const RefPtr<InspectorObject>& styleId, int
// propertyIndex, const String& text, bool overwrite, RefPtr<InspectorObject>& result);
// virtual void toggleProperty(ErrorString*, const RefPtr<InspectorObject>& styleId, int
// propertyIndex, bool disable, RefPtr<InspectorObject>& result);
// virtual void setRuleSelector(ErrorString*, const RefPtr<InspectorObject>& ruleId, const String&
// selector, RefPtr<InspectorObject>& result);
// virtual void getSupportedCSSProperties(ErrorString*, RefPtr<InspectorArray>& result);

/**
 * A WIP css domain object. This class is not officially supported via the webkit protocol yet.
 */
public class WebkitCSS extends WebkitDomain {
  public static interface CSSListener {
    /**
     * Called when the given style sheet changes.
     * 
     * @param styleSheetId
     */
    public void styleSheetChanged(String styleSheetId);
  }

  private static final String STYLE_SHEET_CHANGED = "CSS.styleSheetChanged";

  private List<CSSListener> listeners = new ArrayList<WebkitCSS.CSSListener>();

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

  public void getAllStyleSheets(final WebkitCallback<WebkitStyleSheetRef[]> callback)
      throws IOException {
    if (callback == null) {
      throw new IllegalArgumentException("callback is required");
    }

    sendSimpleCommand("CSS.getAllStyleSheets", new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        callback.handleResult(convertGetAllStyleSheetsResult(result));
      }
    });
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
    // CSS.mediaQueryResultChanged

    if (method.equals(STYLE_SHEET_CHANGED)) {
      String styleSheetId = params.getString("styleSheetId");

      for (CSSListener listener : listeners) {
        listener.styleSheetChanged(styleSheetId);
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

  private WebkitResult<WebkitStyleSheetRef[]> convertGetAllStyleSheetsResult(JSONObject object)
      throws JSONException {
    // "result":{"headers":[]}

    //"result":{
    //  "headers":[
    //             {"title":"","styleSheetId":"1", ...},
    //             {"title":"","styleSheetId":"2", ...}
    //  ]
    //}

    WebkitResult<WebkitStyleSheetRef[]> result = WebkitResult.createFrom(object);

    result.setResult(new WebkitStyleSheetRef[0]);

    if (object.has("result")) {
      JSONObject obj = object.getJSONObject("result");

      if (obj.has("headers")) {
        result.setResult(WebkitStyleSheetRef.createFrom(obj.getJSONArray("headers")));
      }
    }

    return result;
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
