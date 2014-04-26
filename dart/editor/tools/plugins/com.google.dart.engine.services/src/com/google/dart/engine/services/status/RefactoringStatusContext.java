/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.services.status;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.translation.DartName;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeElementName;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;

/**
 * {@link RefactoringStatusContext} can be used to annotate a {@link RefactoringStatusEntry} with
 * additional information typically presented in the user interface.
 */
public class RefactoringStatusContext {
  /**
   * @return the {@link RefactoringStatusContext} that corresponds to the given {@link SearchMatch}.
   */
  public static RefactoringStatusContext create(SearchMatch match) {
    Element enclosingElement = match.getElement();
    return new RefactoringStatusContext(
        enclosingElement.getContext(),
        enclosingElement.getSource(),
        match.getSourceRange());
  }

  private final AnalysisContext context;

  private final Source source;

  private final SourceRange range;

  public RefactoringStatusContext(AnalysisContext context, Source source, SourceRange range) {
    this.context = context;
    this.source = source;
    this.range = range;
  }

  /**
   * Creates a new {@link RefactoringStatusContext} which corresponds to the given {@link AstNode}.
   */
  @DartName("forNode")
  public RefactoringStatusContext(AstNode node) {
    CompilationUnit unit = node.getAncestor(CompilationUnit.class);
    CompilationUnitElement unitElement = unit.getElement();
    this.context = unitElement.getContext();
    this.source = unitElement.getSource();
    this.range = rangeNode(node);
  }

  /**
   * Creates a new {@link RefactoringStatusContext} which corresponds to given location in the
   * {@link Source} of the given {@link CompilationUnit}.
   */
  @DartName("forUnit")
  public RefactoringStatusContext(CompilationUnit unit, SourceRange range) {
    CompilationUnitElement unitElement = unit.getElement();
    this.context = unitElement.getContext();
    this.source = unitElement.getSource();
    this.range = range;
  }

  /**
   * @return the {@link RefactoringStatusContext} which corresponds to the declaration of the given
   *         {@link Element}.
   */
  @DartName("forElement")
  public RefactoringStatusContext(Element element) {
    this.context = element.getContext();
    this.source = element.getSource();
    this.range = rangeElementName(element);
  }

  /**
   * @return the {@link AnalysisContext} in which this status occurs.
   */
  public AnalysisContext getContext() {
    return context;
  }

  /**
   * @return the {@link SourceRange} with specific location where this status occurs.
   */
  public SourceRange getRange() {
    return range;
  }

  /**
   * @return the {@link Source} in which this status occurs.
   */
  public Source getSource() {
    return source;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[source=");
    builder.append(source);
    builder.append(", range=");
    builder.append(range);
    builder.append("]");
    return builder.toString();
  }
}
