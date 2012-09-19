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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// TODO(devoncarew): implement the stack trace object
// http://code.google.com/chrome/devtools/docs/protocol/tot/console.html#type-StackTrace

/**
 * A WIP console domain object.
 */
public class WebkitConsole extends WebkitDomain {

  public static interface ConsoleListener {
    /**
     * Issued when new console message is added.
     * 
     * @param message
     */
    public void messageAdded(String message);

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

  protected void handleConsoleNotification(String method, JSONObject params) throws JSONException {
    // If we get this specific error message, there's an additional "url" field with the bad
    // reference.
    final String FAILED_TO_LOAD = "Failed to load resource";

    if (method.equals(MESSAGE_ADDED)) {
      JSONObject message = params.getJSONObject("message");

      String text = message.getString("text");

      if (text != null && text.contains(FAILED_TO_LOAD)) {
        if (message.has("url")) {
          text += "\n  " + message.getString("url");
        }
      }

      for (ConsoleListener listener : listeners) {
        listener.messageAdded(text);
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
