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
  public void messageAdded(String message) {
    lastMessage = message;

    message += "\n";

    buffer.append(message);

    for (IStreamListener listener : listeners.toArray(new IStreamListener[listeners.size()])) {
      listener.streamAppended(message, this);
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

}
