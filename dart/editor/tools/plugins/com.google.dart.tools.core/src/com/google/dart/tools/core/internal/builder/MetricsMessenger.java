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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.compiler.CompilerConfiguration;

import java.util.LinkedList;

/**
 * The MetricsMessenger class tells all of its listeners namely the MetricsManager when a new
 * Library is compiled so it can gather the requisite statistics
 */
public class MetricsMessenger {

  /**
   * The list of all the {@link MetricsListener} that want to be notified of new builds.
   */
  private final LinkedList<MetricsListener> listeners = new LinkedList<MetricsListener>();

  /**
   * Singleton instance of MetricsMessenger.
   */
  private static MetricsMessenger singleton = new MetricsMessenger();

  /**
   * @return The singleton instance of this class.
   */
  public static MetricsMessenger getSingleton() {
    return singleton;
  }

  /**
   * Add a listener to be notified on new CompilerBuilds.
   * 
   * @param listener the MetricsListener that wants to be notified.
   */
  public void addListener(MetricsListener listener) {
    listeners.add(listener);
  }

  /**
   * Used to tell the {@link MetricsListener}s that a new compilation unit has been built.
   * 
   * @param config The CompilerConfiguration that contains a handle to CompilerMetrics.
   * @param libName Name of the library that was just compiled.
   */
  public void fireUpdates(CompilerConfiguration config, String libName) {
    for (MetricsListener listener : listeners) {
      listener.update(config, libName);
    }
  }

  public void removeListener(MetricsListener listener) {
    listeners.remove(listener);
  }
}
