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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.internal.index.IndexContributor;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.RefactoringFactory;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.SourceFactory;

/**
 * Abstract test for testing {@link RenameRefactoring}s.
 */
public abstract class RenameRefactoringImplTest extends RefactoringImplTest {
  protected RenameRefactoring refactoring;
  protected Change refactoringChange;

  /**
   * Asserts that {@link #refactoring} status is OK.
   */
  protected final void assertRefactoringStatusOK() throws Exception {
    assertRefactoringStatusOK(refactoring);
  }

  /**
   * Checks that all conditions are <code>OK</code> and applying {@link Change} to the
   * {@link #testUnit} is same source as given lines.
   */
  protected final void assertSuccessfulRename(String... lines) throws Exception {
    assertRefactoringStatusOK();
    refactoringChange = refactoring.createChange(pm);
    assertTestChangeResult(refactoringChange, makeSource(lines));
  }

  /**
   * @return the {@link RenameRefactoring} for given {@link Element}.
   */
  protected final void createRenameRefactoring(Element element) {
    refactoring = RefactoringFactory.createRenameRefactoring(searchEngine, element);
  }

  /**
   * @return the {@link RenameRefactoring} for {@link Element} of the {@link SimpleIdentifier} at
   *         the given search pattern.
   */
  protected final void createRenameRefactoring(String search) {
    SimpleIdentifier identifier = findIdentifier(search);
    Element element = identifier.getBestElement();
    if (element instanceof PrefixElement) {
      element = IndexContributor.getImportElement(identifier);
    }
    createRenameRefactoring(element);
  }

  /**
   * @return new {@link AnalysisContext} with SDK.
   */
  protected final AnalysisContext newAnalysisContext() {
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(defaultSdk));
    context.setSourceFactory(sourceFactory);
    return context;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    refactoring = null;
    refactoringChange = null;
  }
}
