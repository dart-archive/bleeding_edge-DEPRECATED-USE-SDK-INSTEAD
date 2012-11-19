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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A WIP DOM domain object.
 */
public class WebkitDom extends WebkitDomain {

  public static interface DomListener {
    /**
     * Fired when Document has been totally updated. Node ids are no longer valid.
     */
    public void documentUpdated();
  }

  public static interface InspectorListener {
    public void detached(String reason);
  }

  private final static String DOM_DOCUMENT_UPDATED = "DOM.documentUpdated";

  private static final String INSPECTOR_DETACHED = "Inspector.detached";

  private List<DomListener> domListeners = new ArrayList<DomListener>();
  private List<InspectorListener> inspectorListeners = new ArrayList<InspectorListener>();

  public WebkitDom(WebkitConnection connection) {
    super(connection);

    connection.registerNotificationHandler("DOM.", new NotificationHandler() {
      @Override
      public void handleNotification(String method, JSONObject params) throws JSONException {
        handleDOMNotification(method, params);
      }
    });

    connection.registerNotificationHandler("Inspector.", new NotificationHandler() {
      @Override
      public void handleNotification(String method, JSONObject params) throws JSONException {
        handleInspectorNotification(method, params);
      }
    });
  }

  public void addDomListener(DomListener listener) {
    domListeners.add(listener);
  }

  public void addInspectorListener(InspectorListener listener) {
    inspectorListeners.add(listener);
  }

  public void getDocument(final WebkitCallback<WebkitNode> callback) throws IOException {
    sendSimpleCommand("DOM.getDocument", new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        callback.handleResult(convertGetDocumentResult(result));
      }
    });
  }

  public void removeDomListener(DomListener listener) {
    domListeners.remove(listener);
  }

  public void removeInspectorListener(InspectorListener listener) {
    inspectorListeners.remove(listener);
  }

  public void setOuterHTML(int nodeId, String outerHTML) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.setOuterHTML");
      request.put("params", new JSONObject().put("nodeId", nodeId).put("outerHTML", outerHTML));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  protected void handleDOMNotification(String method, JSONObject params) {
    if (method.equals(DOM_DOCUMENT_UPDATED)) {
      for (DomListener listener : domListeners) {
        listener.documentUpdated();
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

  protected void handleInspectorNotification(String method, JSONObject params) {
    if (method.equals(INSPECTOR_DETACHED)) {
      // {"method":"Inspector.detached","params":{"reason":"target_closed"}}
      String reason = params.optString("reason", null);

      for (InspectorListener listener : inspectorListeners) {
        listener.detached(reason);
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

  private WebkitResult<WebkitNode> convertGetDocumentResult(JSONObject object) throws JSONException {
    WebkitResult<WebkitNode> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONObject rootNode = object.getJSONObject("result").getJSONObject("root");

      result.setResult(WebkitNode.createFrom(rootNode));
    }

    return result;
  }

}
