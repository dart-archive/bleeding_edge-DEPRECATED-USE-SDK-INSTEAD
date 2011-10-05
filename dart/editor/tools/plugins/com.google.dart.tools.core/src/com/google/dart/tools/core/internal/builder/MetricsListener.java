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

/**
 * MetricsListeners get updated when new compilation units are built currently the only listener
 * that exists is com.google.dart.tools.ui.internal.dartc.metrics.MetricsManager
 */
public interface MetricsListener {

  /**
   * A new Compilation unit has been built so our listener is being notified.
   */
  public void update(CompilerConfiguration config, String libName);

}
