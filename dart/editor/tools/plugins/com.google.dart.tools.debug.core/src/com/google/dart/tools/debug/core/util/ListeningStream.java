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

package com.google.dart.tools.debug.core.util;

/**
 * A class that can process new text input, send messages when new input is received, and maintains
 * a buffer of the last 20,000 chars written.
 */
class ListeningStream {
  public static interface StreamListener {
    public void handleStreamData(String data);
  }

  private static final int MAX_SIZE = 20000;

  private StringBuilder buffer = new StringBuilder();
  private StreamListener listener;

  public ListeningStream() {

  }

  public synchronized void appendData(String data) {
    if (listener != null) {
      listener.handleStreamData(data);
    }

    buffer.append(data);

    if (buffer.length() > MAX_SIZE) {
      int remove = buffer.length() - MAX_SIZE;

      buffer.replace(0, remove, "");
    }
  }

  public synchronized void setListener(StreamListener l) {
    listener = l;

    if (buffer.length() > 0) {
      listener.handleStreamData(buffer.toString());
    }
  }

  @Override
  public String toString() {
    return buffer.toString();
  }

}
