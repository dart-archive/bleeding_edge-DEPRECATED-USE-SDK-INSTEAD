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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.Source;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Abstract implementation of {@link RenameRefactoring}.
 */
public abstract class RenameRefactoringImpl extends RenameRefactoring {
  protected final SearchEngine searchEngine;
  protected final Element element;
  protected final Source elementSource;

  protected String newName;

  public RenameRefactoringImpl(SearchEngine searchEngine, Element element) {
    this.searchEngine = searchEngine;
    this.element = element;
    this.elementSource = element.getSource();
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    RefactoringStatus result = new RefactoringStatus();
    if (Objects.equal(newName, element.getName())) {
      result.addFatalError("Choose another name.");
    }
    return result;
  }

  /**
   * @return the {@link Set} with all direct and indirect sub {@link ClassElement}s of the given.
   */
  public Set<ClassElement> getSubClassElements(ClassElement seed) {
    Set<ClassElement> subClasses = Sets.newHashSet();
    // prepare queue
    LinkedList<ClassElement> subClassQueue = Lists.newLinkedList();
    subClassQueue.add(seed);
    // process queue
    while (!subClassQueue.isEmpty()) {
      ClassElement subClass = subClassQueue.removeFirst();
      if (subClasses.add(subClass)) {
        List<SearchMatch> subMatches = searchEngine.searchSubtypes(subClass, null, null);
        for (SearchMatch subMatch : subMatches) {
          ClassElement subClassNew = (ClassElement) subMatch.getElement();
          subClassQueue.addLast(subClassNew);
        }
      }
    }
    subClasses.remove(seed);
    return subClasses;
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
  protected Edit createDeclarationRenameEdit(Element element) {
    return new Edit(element.getNameOffset(), element.getName().length(), newName);
  }

  /**
   * @return the {@link Edit} to set new name for the given {@link SearchMatch} reference.
   */
  protected final Edit createReferenceRenameEdit(SearchMatch reference) {
    return new Edit(reference.getSourceRange(), newName);
  }
}
