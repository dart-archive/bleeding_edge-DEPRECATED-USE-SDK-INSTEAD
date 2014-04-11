/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local;

import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerListener;

import java.util.Map;

/**
 * In-process implementation of {@link AnalysisServer}.
 * 
 * @coverage dart.server.local
 */
public class LocalAnalysisServerImpl implements AnalysisServer {
  private static final String VERSION = "0.0.1";

  @Override
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void applyChanges(String contextId, ChangeSet changeSet) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String createContext(String name, String sdkDirectory, Map<String, String> packageMap) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteContext(String contextId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOptions(String contextId, AnalysisOptions options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setPrioritySources(String contextId, Source[] sources) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String version() {
    return VERSION;
  }
}
