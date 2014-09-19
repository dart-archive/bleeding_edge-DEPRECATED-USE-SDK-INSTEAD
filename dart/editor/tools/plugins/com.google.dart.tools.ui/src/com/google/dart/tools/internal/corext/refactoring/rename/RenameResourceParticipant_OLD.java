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
package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.util.DartElementUtil;
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
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.List;

/**
 * {@link RenameParticipant} for updating resource references in Dart libraries.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameResourceParticipant_OLD extends RenameParticipant {
  private final TextChangeManager changeManager = new TextChangeManager();
  private IFile file;

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
      throws OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return null;
  }

  @Override
  public Change createPreChange(IProgressMonitor pm) {
    RenameArguments arguments = getArguments();
    // update references
    if (arguments.getUpdateReferences()) {
      // prepare unit element
      CompilationUnitElement unitElement = DartElementUtil.getCompilationUnitElement(file);
      if (unitElement == null) {
        return null;
      }
      // prepare references
      SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();
      List<SearchMatch> references = searchEngine.searchReferences(unitElement, null, null);
      // update references
      String newName = arguments.getNewName();
      for (SearchMatch match : references) {
        addReferenceUpdate(match, newName);
      }
    }
    // return as single CompositeChange
    TextChange[] changes = changeManager.getAllChanges();
    if (changes.length != 0) {
      return new CompositeChange(getName(), changes);
    } else {
      return null;
    }
  }

  @Override
  public String getName() {
    return RefactoringMessages.RenameResourceParticipant_name;
  }

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof IFile) {
      file = (IFile) element;
      return true;
    }
    return false;
  }

  private void addReferenceUpdate(SearchMatch match, String newName) {
    Source source = match.getElement().getSource();
    // prepare "old name" range
    SourceRange matchRange = match.getSourceRange();
    int end = matchRange.getEnd() - "'".length();
    int begin = end - file.getName().length();
    // add TextEdit to rename "old name" with "new name"
    TextEdit edit = new ReplaceEdit(begin, end - begin, newName);
    addTextEdit(source, RefactoringCoreMessages.RenameProcessor_update_reference, edit);
  }

  private void addTextEdit(Source source, String groupName, TextEdit textEdit) {
    TextChange change = changeManager.get(source);
    TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
  }
}
