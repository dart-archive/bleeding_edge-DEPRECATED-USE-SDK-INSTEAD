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
import com.google.dart.tools.debug.core.webkit.WebkitNode.WebkitAttribute;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

// TODO: additional DOM functionality we could expose:
// moveTo requestNode resolveNode setAttributesAsText setNodeName

// TODO: we need to track all DOM nodeIds returned to us; the node info is only sent once

// TODO: create a DomDocument model object. It will be in charge of populating itself when queried.
// It will fire events when it is changed (from notifications from the browser). It will probably
// also expose some manipulation methods that will call through to the WebkitDom class.

/**
 * A WIP DOM domain object.
 * <p>
 * This domain exposes DOM read/write operations. Each DOM Node is represented with its mirror
 * object that has an id. This id can be used to get additional information on the Node, resolve it
 * into the JavaScript object wrapper, etc. It is important that client receives DOM events only for
 * the nodes that are known to the client. Backend keeps track of the nodes that were sent to the
 * client and never sends the same node twice. It is client's responsibility to collect information
 * about the nodes that were sent to the client.
 * <p>
 * Note that iframe owner elements will return corresponding document elements as their child nodes.
 */
public class WebkitDom extends WebkitDomain {

  public static interface DomListener {
    /**
     * Fired when Document has been totally updated. Node ids are no longer valid.
     */
    public void documentUpdated();
  }

  public static class HighlightConfig {
    /**
     * The border highlight fill color (default: transparent).
     */
    public RGBA borderColor;

    /**
     * The content box highlight fill color (default: transparent).
     */
    public RGBA contentColor;

    /**
     * The margin highlight fill color (default: transparent).
     */
    public RGBA marginColor;

    /**
     * The padding highlight fill color (default: transparent).
     */
    public RGBA paddingColor;

    /**
     * Whether the node info tooltip should be shown (default: false).
     */
    public Boolean showInfo;

    public HighlightConfig() {

    }

    public JSONObject toJSONObject() throws JSONException {
      JSONObject obj = new JSONObject();

      if (borderColor != null) {
        obj.put("borderColor", borderColor.toJSONObject());
      }

      if (contentColor != null) {
        obj.put("contentColor", contentColor.toJSONObject());
      }

      if (marginColor != null) {
        obj.put("marginColor", marginColor.toJSONObject());
      }

      if (paddingColor != null) {
        obj.put("paddingColor", paddingColor.toJSONObject());
      }

      if (showInfo != null) {
        obj.put("showInfo", showInfo.booleanValue());
      }

      return obj;
    }
  }

  public static interface InspectorListener {
    public void detached(String reason);

    public void targetCrashed();
  }

  public static class RGBA {
    /**
     * The alpha component, in the [0-1] range (default: 1).
     */
    public Double a;

    /**
     * The red component, in the [0-255] range.
     */
    public int r;

    /**
     * The green component, in the [0-255] range.
     */
    public int g;

    /**
     * The blue component, in the [0-255] range.
     */
    public int b;

    public RGBA(int r, int g, int b) {
      this.r = r;
      this.g = g;
      this.b = b;
    }

    public RGBA(int r, int g, int b, double a) {
      this.r = r;
      this.g = g;
      this.b = b;
      this.a = a;
    }

    public JSONObject toJSONObject() throws JSONException {
      JSONObject obj = new JSONObject();

      if (a != null) {
        obj.put("a", a);
      }

      obj.put("r", r);
      obj.put("g", g);
      obj.put("b", b);

      return obj;
    }
  }

  public static HighlightConfig DEFAULT_HIGHLIGHT;

  static {
    DEFAULT_HIGHLIGHT = new HighlightConfig();
    DEFAULT_HIGHLIGHT.borderColor = new RGBA(0, 0, 0);
    DEFAULT_HIGHLIGHT.contentColor = new RGBA(255, 255, 204, 0.4d);
    DEFAULT_HIGHLIGHT.showInfo = true;
  }

  private final static String DOM_DOCUMENT_UPDATED = "DOM.documentUpdated";

  private static final String INSPECTOR_DETACHED = "Inspector.detached";
  private static final String INSPECTOR_TARGET_CRASHED = "Inspector.targetCrashed";

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

  /**
   * Focuses the given element.
   * 
   * @param nodeId Id of the node to focus.
   * @throws IOException
   */
  @WebkitUnsupported
  public void focus(int nodeId) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.focus");
      request.put("params", new JSONObject().put("nodeId", nodeId));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Returns attributes for the specified node.
   * 
   * @param nodeId Id of the node to retrieve attributes for.
   * @param callback
   * @throws IOException
   */
  public void getAttributes(int nodeId, final WebkitCallback<List<WebkitAttribute>> callback)
      throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.getAttributes");
      request.put("params", new JSONObject().put("nodeId", nodeId));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetAttributesResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void getDocument(final WebkitCallback<WebkitNode> callback) throws IOException {
    sendSimpleCommand("DOM.getDocument", new Callback() {
      @Override
      public void handleResult(JSONObject result) throws JSONException {
        callback.handleResult(convertGetDocumentResult(result));
      }
    });
  }

  public WebkitNode getDocumentSync() throws IOException {
    @SuppressWarnings("unchecked")
    final WebkitResult<WebkitNode>[] result = new WebkitResult[1];
    final CountDownLatch latch = new CountDownLatch(1);

    getDocument(new WebkitCallback<WebkitNode>() {
      @Override
      public void handleResult(WebkitResult<WebkitNode> r) {
        result[0] = r;

        latch.countDown();
      }
    });

    try {
      latch.await();
    } catch (InterruptedException e) {

    }

    if (result[0].isError()) {
      throw new IOException(result[0].getErrorMessage());
    } else {
      return result[0].getResult();
    }
  }

  public void getOuterHtml(int nodeId, final WebkitCallback<String> callback) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.getOuterHtml");
      request.put("params", new JSONObject().put("nodeId", nodeId));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertGetOuterHtmlResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Hides DOM node highlight.
   * 
   * @throws IOException
   */
  public void hideHighlight() throws IOException {
    sendSimpleCommand("DOM.hideHighlight");
  }

  /**
   * Highlights DOM node with given id.
   * 
   * @param nodeId
   * @param highlightConfig
   */
  public void highlightNode(int nodeId, HighlightConfig highlightConfig) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.highlightNode");
      request.put(
          "params",
          new JSONObject().put("nodeId", nodeId).put(
              "highlightConfig",
              highlightConfig.toJSONObject()));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Highlights given rectangle. Coordinates are absolute with respect to the main frame viewport.
   * 
   * @param x
   * @param y
   * @param width
   * @param height
   * @param color
   * @param outlineColor
   * @throws IOException
   */
  public void highlightRect(int x, int y, int width, int height, RGBA color, RGBA outlineColor)
      throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.highlightRect");
      request.put(
          "params",
          new JSONObject().put("x", x).put("y", y).put("width", width).put("height", height).put(
              "color",
              color.toJSONObject()).put("outlineColor", outlineColor.toJSONObject()));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Executes querySelector on a given node.
   * 
   * @param nodeId
   * @param selector
   * @param callback
   * @throws IOException
   */
  public void querySelector(int nodeId, String selector, final WebkitCallback<Integer> callback)
      throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.querySelector");
      request.put("params", new JSONObject().put("nodeId", nodeId).put("selector", selector));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertQuerySelectorResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Executes querySelectorAll on a given node.
   * 
   * @param nodeId
   * @param selector
   * @param callback
   * @throws IOException
   */
  public void querySelectorAll(int nodeId, String selector,
      final WebkitCallback<List<Integer>> callback) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.querySelectorAll");
      request.put("params", new JSONObject().put("nodeId", nodeId).put("selector", selector));

      connection.sendRequest(request, new Callback() {
        @Override
        public void handleResult(JSONObject result) throws JSONException {
          callback.handleResult(convertQuerySelectorAllResult(result));
        }
      });
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Removes attribute with given name from an element with given id.
   * 
   * @param nodeId Id of the element to remove attribute from.
   * @param name Name of the attribute to remove.
   * @throws IOException
   */
  public void removeAttribute(int nodeId, String name) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.removeAttribute");
      request.put("params", new JSONObject().put("nodeId", nodeId).put("name", name));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void removeDomListener(DomListener listener) {
    domListeners.remove(listener);
  }

  public void removeInspectorListener(InspectorListener listener) {
    inspectorListeners.remove(listener);
  }

  /**
   * Removes node with given id.
   * 
   * @param nodeId
   * @throws IOException
   */
  public void removeNode(int nodeId) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.removeNode");
      request.put("params", new JSONObject().put("nodeId", nodeId));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Requests that children of the node with given id are returned to the caller in form of
   * setChildNodes events.
   * 
   * @param nodeId Id of the node to get children for
   * @throws IOException
   */
  public void requestChildNodes(int nodeId) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.requestChildNodes");
      request.put("params", new JSONObject().put("nodeId", nodeId));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Sets attribute for an element with given id.
   * 
   * @param nodeId
   * @param name
   * @param value
   * @throws IOException
   */
  public void setAttributeValue(int nodeId, String name, String value) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.setAttributeValue");
      request.put(
          "params",
          new JSONObject().put("nodeId", nodeId).put("name", name).put("value", value));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Enters the 'inspect' mode. In this mode, elements that user is hovering over are highlighted.
   * Backend then generates 'inspect' command upon element selection.
   * 
   * @param enabled true to enable inspection mode, false to disable it
   * @param highlightConfig a descriptor for the highlight appearance of hovered-over nodes. May be
   *          omitted if <code>enabled == false</code>
   * @throws IOException
   */
  @WebkitUnsupported
  public void setInspectModeEnabled(boolean enabled, HighlightConfig highlightConfig)
      throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.setInspectModeEnabled");
      request.put(
          "params",
          new JSONObject().put("enabled", enabled).put(
              "highlightConfig",
              highlightConfig.toJSONObject()));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Sets node value for a node with given id.
   * 
   * @param nodeId Id of the node to set value for.
   * @param value New node's value.
   * @throws IOException
   */
  public void setNodeValue(int nodeId, String value) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOM.setNodeValue");
      request.put("params", new JSONObject().put("nodeId", nodeId).put("value", value));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Sets node HTML markup, returns new node id.
   * 
   * @param nodeId Id of the node to set value for.
   * @param outerHTML Outer HTML markup to set.
   * @throws IOException
   */
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
    } else if (method.equals(INSPECTOR_TARGET_CRASHED)) {
      for (InspectorListener listener : inspectorListeners) {
        listener.targetCrashed();
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

  private WebkitResult<List<WebkitAttribute>> convertGetAttributesResult(JSONObject object)
      throws JSONException {
    WebkitResult<List<WebkitAttribute>> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONArray arr = object.getJSONObject("result").getJSONArray("attributes");

      List<WebkitAttribute> attributes = WebkitAttribute.createFrom(arr);

      result.setResult(attributes);
    }

    return result;
  }

  private WebkitResult<WebkitNode> convertGetDocumentResult(JSONObject object) throws JSONException {
    WebkitResult<WebkitNode> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONObject rootNode = object.getJSONObject("result").getJSONObject("root");

      result.setResult(WebkitNode.createFrom(rootNode));
    }

    return result;
  }

  private WebkitResult<String> convertGetOuterHtmlResult(JSONObject object) throws JSONException {
    WebkitResult<String> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      String html = object.getJSONObject("result").getString("outerHTML");

      result.setResult(html);
    }

    return result;
  }

  private WebkitResult<List<Integer>> convertQuerySelectorAllResult(JSONObject object)
      throws JSONException {
    WebkitResult<List<Integer>> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      JSONArray arr = object.getJSONObject("result").getJSONArray("nodeIds");

      List<Integer> idArray = new ArrayList<Integer>(arr.length());

      for (int i = 0; i < arr.length(); i++) {
        idArray.add(arr.getInt(i));
      }

      result.setResult(idArray);
    }

    return result;
  }

  private WebkitResult<Integer> convertQuerySelectorResult(JSONObject object) throws JSONException {
    WebkitResult<Integer> result = WebkitResult.createFrom(object);

    if (object.has("result")) {
      int nodeId = object.getJSONObject("result").getInt("nodeId");

      result.setResult(nodeId);
    }

    return result;
  }

}
