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

package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.debug.core.webkit.WebkitConnection;
import com.google.dart.tools.debug.core.webkit.WebkitConsole;
import com.google.dart.tools.debug.core.webkit.WebkitConsole.CallFrame;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a Dartium specific implementation of an IStreamMonitor.
 */
class DartiumStreamMonitor implements IStreamMonitor, WebkitConsole.ConsoleListener {
  private final static String FAILED_TO_LOAD = "Failed to load resource";
  private final static String CHROME_THUMB = "chrome://thumb/";
  private final static String CHROME_SEARCH_THUMB = "chrome-search://thumb/";
  private final static String NEWTAB_MESSAGE = "_/chrome/newtab?";

  private List<IStreamListener> listeners = new ArrayList<IStreamListener>();

  private String lastMessage;
  private String lastUrl;
  private int lastLine;
  private List<CallFrame> lastStackTrace;

  private StringBuilder buffer = new StringBuilder();

  private WebkitConnection connection;

  public DartiumStreamMonitor() {

  }

  @Override
  public void addListener(IStreamListener listener) {
    listeners.add(listener);
  }

  @Override
  public String getContents() {
    return buffer.toString();
  }

  @Override
  public void messageAdded(final String message, final String url, int line,
      List<CallFrame> stackTrace) {
    lastMessage = message;
    lastUrl = url;
    lastLine = line;
    lastStackTrace = stackTrace;

    if (!shouldIgnoreMessage(message, url)) {
      String text = message;

      // If we get a failed to load message, also include the url that didn't load.
      if (message != null && message.startsWith(FAILED_TO_LOAD)) {
        if (url != null) {
          text += "\n  " + url;
        }
      }

      // Rodent.toString (file:///Users/foo.../debuggertest/pets.dart:79:7)
      if (stackTrace != null && stackTrace.size() > 0) {
        // If we're not printing out a blank line.
        if (text.trim().length() > 0) {
          CallFrame topFrame = stackTrace.get(0);

          // dartbug.com/16805
          if (!"undefined".equals(topFrame.url)) {
            text += " (" + topFrame.url + ":" + topFrame.lineNumber + ")";
          }
        }
      }

      text += "\n";

      buffer.append(text);

      for (IStreamListener listener : listeners.toArray(new IStreamListener[listeners.size()])) {
        listener.streamAppended(text, this);
      }
    }
  }

  @Override
  public void messageRepeatCountUpdated(int count) {
    messageAdded(lastMessage, lastUrl, lastLine, lastStackTrace);
  }

  @Override
  public void messagesCleared() {
    lastMessage = null;
    buffer.setLength(0);
  }

  @Override
  public void removeListener(IStreamListener listener) {
    listeners.remove(listener);
  }

  protected void connectTo(WebkitConnection connection) throws IOException {
    if (this.connection != null) {
      messagesCleared();

      this.connection.getConsole().removeConsoleListener(this);
    }

    this.connection = connection;

    connection.getConsole().addConsoleListener(this);
    connection.getConsole().enable();
  }

  protected void messageAdded(String message) {
    messageAdded(message, null, -1, null);
  }

  boolean shouldIgnoreMessage(String message, String url) {
    if (message == null || url == null) {
      return false;
    }

    // Ignore all "failed to load" messages from chrome://thumb/... urls.
    if (message.startsWith(FAILED_TO_LOAD)
        && (url.startsWith(CHROME_THUMB) || url.startsWith(CHROME_SEARCH_THUMB))) {
      return true;
    }

    // Ignore "Application Cache Checking event" messages.
    if (url.contains(NEWTAB_MESSAGE)) {
      return true;
    }

    // Ignore invalid -webkit property messages.
    if (message.indexOf("Invalid CSS property name: -webkit") != -1
        || message.indexOf("Invalid CSS property value: -webkit") != -1) {
      return true;
    }

    return false;
  }
}
