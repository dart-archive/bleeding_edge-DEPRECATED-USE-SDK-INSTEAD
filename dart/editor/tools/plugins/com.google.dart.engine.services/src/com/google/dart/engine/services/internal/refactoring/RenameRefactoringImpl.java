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

import com.google.common.base.Objects;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeElementName;

/**
 * Abstract implementation of {@link RenameRefactoring}.
 */
public abstract class RenameRefactoringImpl extends RefactoringImpl implements RenameRefactoring {
  /**
   * @return the {@link Edit} to replace the given {@link SearchMatch} reference.
   */
  protected static Edit createReferenceEdit(SearchMatch reference, String newText) {
    return new Edit(reference.getSourceRange(), newText);
  }

  /**
   * @return if given unqualified {@link SearchMatch} intersects with visibility range of
   *         {@link LocalElement}.
   */
  protected static boolean isReferenceInLocalRange(LocalElement localElement, SearchMatch reference) {
    if (reference.isQualified()) {
      return false;
    }
    Source localSource = localElement.getSource();
    Source referenceSource = reference.getElement().getSource();
    SourceRange localRange = localElement.getVisibleRange();
    SourceRange referenceRange = reference.getSourceRange();
    return Objects.equal(referenceSource, localSource) && referenceRange.intersects(localRange);
  }

  protected final SearchEngine searchEngine;

  protected final Element element;

  protected String newName;

  public RenameRefactoringImpl(SearchEngine searchEngine, Element element) {
    this.searchEngine = searchEngine;
    this.element = element;
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkNewName(String newName) {
    RefactoringStatus result = new RefactoringStatus();
    if (Objects.equal(newName, element.getName())) {
      result.addFatalError("Choose another name.");
    }
    return result;
  }

  @Override
  public String getCurrentName() {
    return element.getName();
  }

  @Override
  public String getNewName() {
    return newName;
  }

  @Override
  public void setNewName(String newName) {
    this.newName = newName;
  }

  /**
   * @return the {@link Edit} to rename declaration of renaming {@link Element}.
   */
  protected final Edit createDeclarationRenameEdit() {
    return createDeclarationRenameEdit(element);
  }

  /**
   * @return the {@link Edit} to rename declaration if the given {@link Element}.
   */
  protected final Edit createDeclarationRenameEdit(Element element) {
    return new Edit(rangeElementName(element), newName);
  }

  /**
   * @return the {@link Edit} to set new name for the given {@link SearchMatch} reference.
   */
  protected final Edit createReferenceRenameEdit(SearchMatch reference) {
    return createReferenceEdit(reference, newName);
  }
}
