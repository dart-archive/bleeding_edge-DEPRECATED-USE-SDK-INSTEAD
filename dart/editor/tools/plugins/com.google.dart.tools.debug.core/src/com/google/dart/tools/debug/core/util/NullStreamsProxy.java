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

package com.google.dart.tools.debug.core.util;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy2;

import java.io.IOException;

/**
 * A non-op implementation of an IStreamsProxy.
 */
public class NullStreamsProxy implements IStreamsProxy2 {
  private class NullStreamMonitor implements IStreamMonitor {

    public NullStreamMonitor() {

    }

    @Override
    public void addListener(IStreamListener listener) {

    }

    @Override
    public String getContents() {
      return "";
    }

    @Override
    public void removeListener(IStreamListener listener) {

    }
  }

  private NullStreamMonitor outputStreamMonitor;
  private NullStreamMonitor errorStreamMonitor;

  public NullStreamsProxy() {
    outputStreamMonitor = new NullStreamMonitor();
    errorStreamMonitor = new NullStreamMonitor();
  }

  @Override
  public void closeInputStream() throws IOException {

  }

  @Override
  public IStreamMonitor getErrorStreamMonitor() {
    return errorStreamMonitor;
  }

  @Override
  public IStreamMonitor getOutputStreamMonitor() {
    return outputStreamMonitor;
  }

  @Override
  public void write(String input) throws IOException {

  }

}
