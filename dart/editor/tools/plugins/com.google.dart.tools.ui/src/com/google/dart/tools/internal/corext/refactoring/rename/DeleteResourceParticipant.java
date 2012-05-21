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

import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.List;

/**
 * {@link DeleteParticipant} for removing resource references in Dart libraries.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class DeleteResourceParticipant extends DeleteParticipant {

  private final TextChangeManager changeManager = new TextChangeManager(true);
  private IFile file;

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
      throws OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(final IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    return ExecutionUtils.runObjectCore(new RunnableObjectEx<Change>() {
      @Override
      public Change runObject() throws Exception {
        return createChangeEx(pm);
      }
    });
  }

  @Override
  public String getName() {
    return RefactoringMessages.DeleteResourceParticipant_name;
  }

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof IFile) {
      file = (IFile) element;
      return true;
    }
    return false;
  }

  private void addReferenceRemove(SearchMatch match) throws Exception {
    CompilationUnit unit = match.getElement().getAncestor(CompilationUnit.class);
    DartUnit unitNode = DartCompilerUtilities.resolveUnit(unit);
    for (DartDirective directive : unitNode.getDirectives()) {
      SourceRange directiveRange = SourceRangeFactory.create(directive);
      if (SourceRangeUtils.intersects(directiveRange, match.getSourceRange())) {
        String source = unit.getSource();
        int begin = directiveRange.getOffset();
        int end = begin + directiveRange.getLength();
        // skip trailing spaces
        while (end < source.length()) {
          char c = source.charAt(end);
          if (c != ' ' && c != '\t') {
            break;
          }
          end++;
        }
        // skip one EOL
        if (end < source.length() && source.charAt(end) == '\r') {
          end++;
        }
        if (end < source.length() && source.charAt(end) == '\n') {
          end++;
        }
        // remove directive
        TextEdit edit = new ReplaceEdit(begin, end - begin, "");
        addTextEdit(unit, RefactoringMessages.DeleteResourceParticipant_remove_reference, edit);
      }
    }
  }

  private void addTextEdit(CompilationUnit unit, String groupName, TextEdit textEdit) {
    if (unit.getResource() != null) {
      TextChange change = changeManager.get(unit);
      TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
    }
  }

  /**
   * Implementation of {@link #createChange(IProgressMonitor)} which can throw any exception.
   */
  private Change createChangeEx(IProgressMonitor pm) throws Exception {
    // remove references
    SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
    List<SearchMatch> references = searchEngine.searchReferences(file, null, null, pm);
    for (SearchMatch match : references) {
      addReferenceRemove(match);
    }
    // return as single CompositeChange
    TextChange[] changes = changeManager.getAllChanges();
    if (changes.length != 0) {
      return new CompositeChange(getName(), changes);
    } else {
      return null;
    }
  }

}
