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

import com.google.dart.tools.debug.core.webkit.WebkitConsole;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a Dartium specific implementation of an IStreamMonitor.
 */
class DartiumStreamMonitor implements IStreamMonitor, WebkitConsole.ConsoleListener {
  private final static String FAILED_TO_LOAD = "Failed to load resource";
  private final static String CHROME_THUMB = "chrome://thumb/";

  private List<IStreamListener> listeners = new ArrayList<IStreamListener>();

  private String lastMessage;
  private StringBuilder buffer = new StringBuilder();

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
  public void messageAdded(final String message, final String url, int line) {
    String text = message;

    // If we get a failed to load message, also include the url that didn't load.
    if (message != null && message.startsWith(FAILED_TO_LOAD)) {
      if (url != null) {
        text += "\n  " + url;
      }
    }

    lastMessage = text;

    text += "\n";

    if (!shouldIgnoreMessage(message, url)) {
      buffer.append(text);

      for (IStreamListener listener : listeners.toArray(new IStreamListener[listeners.size()])) {
        listener.streamAppended(text, this);
      }
    }
  }

  @Override
  public void messageRepeatCountUpdated(int count) {
    messageAdded(lastMessage);
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

  void messageAdded(String message) {
    messageAdded(message, null, -1);
  }

  boolean shouldIgnoreMessage(String message, String url) {
    if (message == null || url == null) {
      return false;
    }

    // Ignore all "failed to load" messages from chrome://thumb/... urls.
    if (message.startsWith(FAILED_TO_LOAD) && url.startsWith(CHROME_THUMB)) {
      return true;
    }

    return false;
  }

}
