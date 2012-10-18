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
package com.google.dart.tools.core.model;

import java.net.URI;

/**
 * Information about source part, i.e. {@link CompilationUnit}.
 */
public interface DartPart {
  DartPart[] EMPTY_ARRAY = new DartPart[0];

  /**
   * @return the {@link DartLibrary} defining {@link CompilationUnit}.
   */
  CompilationUnit getUnit();

  /**
   * @return the {@link SourceRange} of the {@link DartPart}, not <code>null</code>.
   */
  SourceRange getSourceRange();

  /**
   * @return the {@link SourceRange} of the {@link URI}, not <code>null</code>.
   */
  SourceRange getUriRange();
}
