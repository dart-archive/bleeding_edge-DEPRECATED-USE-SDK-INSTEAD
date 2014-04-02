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

package com.google.dart.engine.internal.context;

/**
 * The enumerated type {@code Priority} defines the priority levels used to return sources in an
 * optimal order. A smaller ordinal value equates to a higher priority.
 */
public enum SourcePriority {
  /**
   * Used for a Dart source that is known to be a part contained in a library that was recently
   * resolved. These parts are given a higher priority because there is a high probability that
   * their AST structure is still in the cache and therefore would not need to be re-created.
   */
  PRIORITY_PART,

  /**
   * Used for a Dart source that is known to be a library.
   */
  LIBRARY,

  /**
   * Used for a Dart source whose kind is unknown.
   */
  UNKNOWN,

  /**
   * Used for a Dart source that is known to be a part but whose library has not yet been resolved.
   */
  NORMAL_PART,

  /**
   * Used for an HTML source.
   */
  HTML;
}
