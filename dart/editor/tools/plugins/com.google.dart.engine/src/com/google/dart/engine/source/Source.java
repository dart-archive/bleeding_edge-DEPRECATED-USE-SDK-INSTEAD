/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.source;

import java.io.File;

/**
 * The interface <code>Source</code> defines the behavior of objects representing source code that
 * can be compiled.
 */
public interface Source {
  /**
   * Return the file represented by this source, or <code>null</code> if this source does not exist.
   * 
   * @return the file represented by this source
   */
  public File getFile();

  /**
   * Return <code>true</code> if this source is in one of the system libraries.
   * 
   * @return <code>true</code> if this is in a system library
   */
  public boolean isInSystemLibrary();

  /**
   * Resolve the given URI relative to the location of this source.
   * 
   * @param uri the URI to be resolved against this source
   * @return a source representing the resolved URI
   */
  public Source resolve(String uri);
}
