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

import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.source.Source;

/**
 * Abstract implementation of {@link RenameRefactoring}.
 */
public abstract class RenameRefactoringImpl extends RenameRefactoring {
  protected final SearchEngine searchEngine;
  private final Element element;
  protected final Source elementSource;

  protected String newName;

  public RenameRefactoringImpl(SearchEngine searchEngine, Element element) {
    this.searchEngine = searchEngine;
    this.element = element;
    this.elementSource = element.getAncestor(CompilationUnitElement.class).getSource();
  }

  @Override
  public void setNewName(String newName) {
    this.newName = newName;
  }

  /**
   * @return the {@link Edit} to rename declaration of renaming {@link Element}.
   */
  protected final Edit createDeclarationRenameEdit() {
    return new Edit(element.getNameOffset(), element.getName().length(), newName);
  }

  /**
   * @return the {@link Edit} to set new name for the given {@link SearchMatch} reference.
   */
  protected final Edit createReferenceRenameEdit(SearchMatch reference) {
    return new Edit(reference.getSourceRange(), newName);
  }
}
