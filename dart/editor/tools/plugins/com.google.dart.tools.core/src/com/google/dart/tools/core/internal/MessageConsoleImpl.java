/*
 * Copyright (c) 2011, the Dart project authors.
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

package com.google.dart.tools.core.internal;

import com.google.dart.tools.core.MessageConsole;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic {@link MessageConsole} implementation.
 */
public class MessageConsoleImpl implements MessageConsole {

  private final List<MessageStream> streams = new ArrayList<MessageStream>();

  @Override
  public void addStream(MessageStream stream) {
    if (!streams.contains(stream)) {
      streams.add(stream);
    }
  }

  @Override
  public void clear() {
    for (MessageStream stream : streams) {
      stream.clear();
    }
  }

  @Override
  public void print(String s) {
    for (MessageStream stream : streams) {
      stream.print(s);
    }
  }

  @Override
  public void println() {
    for (MessageStream stream : streams) {
      stream.println();
    }
  }

  @Override
  public void println(String s) {
    for (MessageStream stream : streams) {
      stream.println(s);
    }
  }

  @Override
  public void removeStream(MessageStream stream) {
    streams.remove(stream);
  }

}
