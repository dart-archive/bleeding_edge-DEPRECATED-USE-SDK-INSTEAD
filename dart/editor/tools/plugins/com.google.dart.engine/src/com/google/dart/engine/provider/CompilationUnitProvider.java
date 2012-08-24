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
package com.google.dart.engine.provider;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.source.Source;

/**
 * The interface {@code CompilationUnitProvider} defines the behavior of objects that can be used to
 * access the compilation unit associated with a given {@link Source source}.
 */
public interface CompilationUnitProvider {
  /**
   * Return the compilation unit associated with the given source, or {@code null} if the contents
   * of the source could not be accessed or if the contents could not be parsed as a compilation
   * unit.
   * 
   * @param source the source associated with the compilation unit to be returned
   * @return the compilation unit associated with the given source
   */
  public CompilationUnit getCompilationUnit(Source source);
}
