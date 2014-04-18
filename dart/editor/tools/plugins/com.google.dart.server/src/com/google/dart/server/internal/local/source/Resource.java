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

package com.google.dart.server.internal.local.source;

import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.UriKind;

/**
 * A file or directory abstraction.
 * 
 * @coverage dart.server.local
 */
public interface Resource {
  /**
   * Creates a new {@link Source} instance that serves this resource.
   */
  Source createSource(UriKind uriKind);

  /**
   * Tests whether this resource exists.
   */
  boolean exists();

  /**
   * Returns a new {@link Resource} that corresponds to the given path.
   * 
   * @param the {@code '/'} separated path
   */
  Resource getChild(String path);

  /**
   * Returns the absolute path.
   */
  String getPath();
}
