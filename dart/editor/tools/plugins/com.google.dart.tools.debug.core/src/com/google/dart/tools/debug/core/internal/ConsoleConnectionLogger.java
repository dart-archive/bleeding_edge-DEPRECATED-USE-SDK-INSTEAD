/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.core.internal;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.chromium.sdk.ConnectionLogger;

/**
 * Connection logger that writes both incoming and outgoing streams into logWriter with simple
 * annotations.
 */
public class ConsoleConnectionLogger implements ConnectionLogger {

  public ConsoleConnectionLogger() {

  }

  @Override
  public StreamListener getIncomingStreamListener() {
    return new StreamListener() {
      @Override
      public void addContent(CharSequence text) {
        DartDebugCorePlugin.logInfo("<< " + text);
      }

      @Override
      public void addSeparator() {

      }
    };
  }

  @Override
  public StreamListener getOutgoingStreamListener() {
    return new StreamListener() {
      @Override
      public void addContent(CharSequence text) {
        DartDebugCorePlugin.logInfo(">> " + text);
      }

      @Override
      public void addSeparator() {

      }
    };
  }

  @Override
  public void handleEos() {

  }

  @Override
  public void setConnectionCloser(ConnectionCloser connectionCloser) {

  }

  @Override
  public void start() {

  }

}
