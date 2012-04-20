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
package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.common.base.Objects;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.List;

/**
 * {@link DartRenameProcessor} for top-level {@link DartVariableDeclaration}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameGlobalVariableProcessor extends RenameTopLevelProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameGlobalVariableProcessor"; //$NON-NLS-1$

  private final DartVariableDeclaration variable;

  /**
   * @param variable the {@link DartVariableDeclaration} to rename, not <code>null</code>.
   */
  public RenameGlobalVariableProcessor(DartVariableDeclaration variable) {
    super(variable);
    this.variable = variable;
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkVariableName(newName);
    result.merge(super.checkNewElementName(newName));
    return result;
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() throws CoreException {
    DartVariableDeclaration result = null;
    DartElement[] topLevelElements = variable.getCompilationUnit().getChildren();
    for (DartElement element : topLevelElements) {
      if (element instanceof DartVariableDeclaration
          && Objects.equal(element.getElementName(), getNewElementName())) {
        result = (DartVariableDeclaration) element;
      }
    }
    return result;
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameGlobalVariableRefactoring_name;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(variable);
  }

  @Override
  protected List<SearchMatch> getReferences(final IProgressMonitor pm) throws CoreException {
    return ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        return searchEngine.searchReferences(variable, null, null, pm);
      }
    });
  }
}
