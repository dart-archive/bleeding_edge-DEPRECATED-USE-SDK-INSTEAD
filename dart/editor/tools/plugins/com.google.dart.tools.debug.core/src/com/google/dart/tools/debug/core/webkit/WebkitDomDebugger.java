/*
 * Copyright (c) 2013, the Dart project authors.
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * A WIP DOMDebugger domain object.
 * <p>
 * DOM debugging allows setting breakpoints on particular DOM operations and events. JavaScript
 * execution will stop on these operations as if there was a regular breakpoint set.
 */
public class WebkitDomDebugger extends WebkitDomain {

  public static enum DomBreakpointType {
    attribute_modified,
    node_removed,
    subtree_modified;

    public static DomBreakpointType value(String str) {
      try {
        return DomBreakpointType.valueOf(str.replace('-', '_'));
      } catch (IllegalArgumentException exception) {
        return null;
      } catch (NullPointerException exception) {
        return null;
      }
    }

    @Override
    public String toString() {
      return name().replace('_', '-');
    }
  }

  public WebkitDomDebugger(WebkitConnection connection) {
    super(connection);
  }

  public void removeDOMBreakpoint() {

  }

  /**
   * Removes DOM breakpoint that was set using setDOMBreakpoint.
   * 
   * @param nodeId Identifier of the node to remove breakpoint from.
   * @param type Type of the breakpoint to remove.
   * @throws IOException
   */
  public void removeDOMBreakpoint(int nodeId, DomBreakpointType type) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOMDebugger.removeDOMBreakpoint");
      request.put("params", new JSONObject().put("nodeId", nodeId).put("type", type.toString()));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Removes breakpoint on particular DOM event.
   * 
   * @param eventName event name
   * @throws IOException
   */
  public void removeEventListenerBreakpoint(String eventName) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOMDebugger.removeEventListenerBreakpoint");
      request.put("params", new JSONObject().put("eventName", eventName));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Removes breakpoint from XMLHttpRequest.
   * 
   * @param url Resource URL substring.
   * @throws IOException
   */
  public void removeXHRBreakpoint(String url) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOMDebugger.removeXHRBreakpoint");
      request.put("params", new JSONObject().put("url", url));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Sets breakpoint on particular operation with DOM.
   * 
   * @param nodeId Identifier of the node to set breakpoint on.
   * @param type Type of the operation to stop upon.
   * @throws IOException
   */
  public void setDOMBreakpoint(int nodeId, DomBreakpointType type) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOMDebugger.setDOMBreakpoint");
      request.put("params", new JSONObject().put("nodeId", nodeId).put("type", type.toString()));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Sets breakpoint on particular DOM event.
   * 
   * @param eventName DOM Event name to stop on (any DOM event will do).
   * @throws IOException
   */
  public void setEventListenerBreakpoint(String eventName) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOMDebugger.setEventListenerBreakpoint");
      request.put("params", new JSONObject().put("eventName", eventName));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Sets breakpoint on XMLHttpRequest.
   * 
   * @param url Resource URL substring. All XHRs having this substring in the URL will get stopped
   *          upon.
   * @throws IOException
   */
  public void setXHRBreakpoint(String url) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "DOMDebugger.setXHRBreakpoint");
      request.put("params", new JSONObject().put("url", url));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

}
