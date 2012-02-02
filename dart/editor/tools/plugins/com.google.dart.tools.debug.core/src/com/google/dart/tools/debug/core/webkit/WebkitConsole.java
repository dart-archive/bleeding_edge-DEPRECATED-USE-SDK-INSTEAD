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

/**
 * A WIP console domain object.
 */
public class WebkitConsole extends WebkitDomain {

  public static interface ConsoleListener {
    public void messageAdded(String message);

    public void messageRepeatCountUpdated(int count);

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
        handlePageNotification(method, params);
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

  public void reload() throws IOException {
    sendSimpleCommand("Console.reload");
  }

  public void removeConsoleListener(ConsoleListener listener) {
    listeners.remove(listener);
  }

  protected void handlePageNotification(String method, JSONObject params) throws JSONException {
    if (method.equals(MESSAGE_ADDED)) {
      JSONObject message = params.getJSONObject("message");

      for (ConsoleListener listener : listeners) {
        listener.messageAdded(message.getString("text"));
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
      DartDebugCorePlugin.logWarning("unhandled notification: " + method);
    }
  }

}
