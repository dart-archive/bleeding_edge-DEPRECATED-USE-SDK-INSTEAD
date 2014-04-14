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

/**
 * The interface {@link MergeableOperation} is an {@link ServerOperation}s which can be merged with
 * (and into) other {@link ServerOperation}s.
 */
public interface MergeableOperation extends ServerOperation {
  /**
   * Merges the given operation into this one.
   * 
   * @return {@code true} if merge was successful.
   */
  boolean mergeWith(ServerOperation operation);
}
