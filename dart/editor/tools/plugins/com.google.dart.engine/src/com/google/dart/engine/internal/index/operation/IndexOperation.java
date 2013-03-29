/*
 * Copyright 2013, the Dart project authors.
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
package com.google.dart.engine.internal.index.operation;

import com.google.dart.engine.source.Source;

/**
 * The interface {@link IndexOperation} defines the behavior of objects used to perform operations
 * on an index.
 * 
 * @coverage dart.engine.index
 */
public interface IndexOperation {
  /**
   * Return {@code true} if this operation returns information from the index.
   * 
   * @return {@code true} if this operation returns information from the index
   */
  public boolean isQuery();

  /**
   * Perform the operation implemented by this operation.
   */
  public void performOperation();

  /**
   * Return {@code true} if this operation should be removed from the operation queue when the
   * given resource has been removed.
   * 
   * @param source the {@link Source} that has been removed
   * @return {@code true} if this operation should be removed from the operation queue as a
   *         result of removing the resource
   */
  public boolean removeWhenSourceRemoved(Source source);
}
