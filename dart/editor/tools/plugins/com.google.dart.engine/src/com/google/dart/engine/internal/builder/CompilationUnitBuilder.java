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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.provider.CompilationUnitProvider;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code CompilationUnitBuilder} build an element model for a single
 * compilation unit.
 */
public class CompilationUnitBuilder {
  /**
   * The provider used to access the compilation unit associated with a given source.
   */
  private CompilationUnitProvider provider;

  /**
   * Initialize a newly created compilation unit element builder.
   * 
   * @param provider the provider used to access the compilation unit associated with a given source
   */
  public CompilationUnitBuilder(CompilationUnitProvider provider) {
    this.provider = provider;
  }

  /**
   * Build the compilation unit element for the given source.
   * 
   * @param compilationUnitSource the source describing the compilation unit
   * @return the compilation unit element that was built
   */
  public CompilationUnitElementImpl buildCompilationUnit(Source compilationUnitSource) {
    CompilationUnit unit = provider.getCompilationUnit(compilationUnitSource);
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    unit.accept(builder);

    CompilationUnitElementImpl element = new CompilationUnitElementImpl(
        compilationUnitSource.getFile().getName());
    element.setAccessors(holder.getAccessors());
    element.setFields(holder.getFields());
    element.setFunctions(holder.getFunctions());
    element.setSource(compilationUnitSource);
    element.setTypeAliases(holder.getTypeAliases());
    element.setTypes(holder.getTypes());
    return element;
  }
}
