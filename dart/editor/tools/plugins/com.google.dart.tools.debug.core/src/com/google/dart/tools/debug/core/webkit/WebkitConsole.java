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
import java.util.List;

/**
 * A WIP console domain object.
 * <p>
 * Console domain defines methods and events for interaction with the JavaScript console. Console
 * collects messages created by means of the JavaScript Console API. One needs to enable this domain
 * using enable command in order to start receiving the console messages. Browser collects messages
 * issued while console domain is not enabled as well and reports them using messageAdded
 * notification upon enabling.
 */
public class WebkitConsole extends WebkitDomain {

  public static class CallFrame {
    static List<CallFrame> createFrom(JSONArray arr) throws JSONException {
      if (arr == null) {
        return null;
      }

      List<CallFrame> frames = new ArrayList<WebkitConsole.CallFrame>(arr.length());

      for (int i = 0; i < arr.length(); i++) {
        frames.add(CallFrame.createFrom(arr.getJSONObject(i)));
      }

      return frames;
    }

    static CallFrame createFrom(JSONObject obj) throws JSONException {
      CallFrame frame = new CallFrame();

      frame.columnNumber = obj.getInt("columnNumber");
      frame.functionName = obj.getString("functionName");
      frame.lineNumber = obj.getInt("lineNumber");
      frame.url = obj.getString("url");

      return frame;
    }

    /** JavaScript script column number. */
    public int columnNumber;

    /** JavaScript function name. */
    public String functionName;

    /** JavaScript script line number. */
    public int lineNumber;

    /** JavaScript script name or url. */
    public String url;

  }

  public static interface ConsoleListener {
    /**
     * Issued when new console message is added.
     * 
     * @param message
     * @param url an optional parameter indicating the source url
     * @param line line number in the resource that generated this message
     */
    public void messageAdded(String message, String url, int line);

    /**
     * Issued when subsequent message(s) are equal to the previous one(s).
     * 
     * @param count new repeat count value
     */
    public void messageRepeatCountUpdated(int count);

    /**
     * Issued when console is cleared. This happens either upon clearMessages command or after page
     * navigation.
     */
    public void messagesCleared();
  }

  private static final String MESSAGE_ADDED = "Console.messageAdded";
  private static final String MESSAGE_CLEARED = "Console.messagesCleared";
  private static final String MESSAGE_COUNT_UPDATED = "Console.messageRepeatCountUpdated";

  private List<ConsoleListener> listeners = new ArrayList<ConsoleListener>();

  public WebkitConsole(WebkitConnection connection) {
    super(connection);

    connection.registerNotificationHandler("Console.", new NotificationHandler() {
      @Override
      public void handleNotification(String method, JSONObject params) throws JSONException {
        handleConsoleNotification(method, params);
      }
    });
  }

  public void addConsoleListener(ConsoleListener listener) {
    listeners.add(listener);
  }

  public void disable() throws IOException {
    sendSimpleCommand("Console.disable");
  }

  public void enable() throws IOException {
    sendSimpleCommand("Console.enable");
  }

  public void removeConsoleListener(ConsoleListener listener) {
    listeners.remove(listener);
  }

  /**
   * Toggles monitoring of XMLHttpRequest. If <code>true</code>, console will receive messages upon
   * each XHR issued.
   * 
   * @param enabled Monitoring enabled state
   * @throws IOException
   */
  @WebkitUnsupported
  public void setMonitoringXHREnabled(boolean enabled) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Console.setMonitoringXHREnabled");
      request.put("params", new JSONObject().put("enabled", enabled));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  @SuppressWarnings("unused")
  protected void handleConsoleNotification(String method, JSONObject params) throws JSONException {
    if (method.equals(MESSAGE_ADDED)) {
      JSONObject message = params.getJSONObject("message");

      String text = message.getString("text");
      String url = message.optString("url");
      /** Line number in the resource that generated this message. */
      int line = message.optInt("line", -1);
      /** Message severity. */
      String level = message.optString("level");
      /** Message source. */
      String source = message.optString("source");
      /** JavaScript stack trace for assertions and error messages. */
      List<CallFrame> stackTrace = CallFrame.createFrom(message.optJSONArray("stackTrace"));

      for (ConsoleListener listener : listeners) {
        listener.messageAdded(text, url, line);
      }
    } else if (method.equals(MESSAGE_CLEARED)) {
      for (ConsoleListener listener : listeners) {
        listener.messagesCleared();
      }
    } else if (method.equals(MESSAGE_COUNT_UPDATED)) {
      for (ConsoleListener listener : listeners) {
        listener.messageRepeatCountUpdated(params.getInt("count"));
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

}
