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
import com.google.dart.tools.core.model.DartFunction;
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
 * {@link DartRenameProcessor} for {@link DartFunction}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameFunctionProcessor extends RenameTopLevelProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameFunctionProcessor"; //$NON-NLS-1$

  private final DartFunction function;

  /**
   * @param function the {@link DartFunction} to rename, not <code>null</code>.
   */
  public RenameFunctionProcessor(DartFunction function) {
    super(function);
    this.function = function;
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkFunctionName(newName);
    result.merge(super.checkNewElementName(newName));
    return result;
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() throws CoreException {
    DartFunction result = null;
    DartElement[] topLevelElements = function.getCompilationUnit().getChildren();
    for (DartElement element : topLevelElements) {
      if (element instanceof DartFunction
          && Objects.equal(element.getElementName(), getNewElementName())) {
        result = (DartFunction) element;
      }
    }
    return result;
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameFunctionRefactoring_name;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(function);
  }

  @Override
  protected List<SearchMatch> getReferences(final IProgressMonitor pm) throws CoreException {
    return ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        return searchEngine.searchReferences(function, null, null, pm);
      }
    });
  }
}
