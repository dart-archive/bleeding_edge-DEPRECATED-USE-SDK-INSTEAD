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
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;

/**
 * Instances of the class {@code CompilationUnitBuilder} build an element model for a single
 * compilation unit.
 * 
 * @coverage dart.engine.resolver
 */
public class CompilationUnitBuilder {
  /**
   * Initialize a newly created compilation unit element builder.
   */
  public CompilationUnitBuilder() {
    super();
  }

  /**
   * Build the compilation unit element for the given source.
   * 
   * @param source the source describing the compilation unit
   * @param unit the AST structure representing the compilation unit
   * @return the compilation unit element that was built
   * @throws AnalysisException if the analysis could not be performed
   */
  public CompilationUnitElementImpl buildCompilationUnit(Source source, CompilationUnit unit)
      throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.resolve.start();
    try {
      if (unit == null) {
        return null;
      }
      ElementHolder holder = new ElementHolder();
      ElementBuilder builder = new ElementBuilder(holder);
      unit.accept(builder);

      CompilationUnitElementImpl element = new CompilationUnitElementImpl(source.getShortName());
      element.setAccessors(holder.getAccessors());
      element.setEnums(holder.getEnums());
      element.setFunctions(holder.getFunctions());
      element.setSource(source);
      element.setTypeAliases(holder.getTypeAliases());
      element.setTypes(holder.getTypes());
      element.setTopLevelVariables(holder.getTopLevelVariables());
      unit.setElement(element);
      return element;
    } finally {
      timeCounter.stop();
    }
  }
}
