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

package com.google.dart.engine.internal.index.file;

import com.google.dart.engine.context.AnalysisContext;

/**
 * A manager for {@link IndexNode}s.
 * 
 * @coverage dart.engine.index
 */
public interface NodeManager {
  /**
   * Returns number of locations in all nodes.
   */
  public int getLocationCount();

  /**
   * Removes all nodes.
   */
  void clear();

  /**
   * Returns the {@link IndexNode} with the given name, {@code null} if not found.
   */
  IndexNode getNode(String name);

  /**
   * Returns the shared {@link StringCodec} instance.
   */
  StringCodec getStringCodec();

  /**
   * Returns a new {@link IndexNode}.
   */
  IndexNode newNode(AnalysisContext context);

  /**
   * Associates the given {@link IndexNode} with the given name.
   */
  void putNode(String name, IndexNode node);

  /**
   * Removes the {@link IndexNode} with the given name.
   */
  void removeNode(String name);
}
