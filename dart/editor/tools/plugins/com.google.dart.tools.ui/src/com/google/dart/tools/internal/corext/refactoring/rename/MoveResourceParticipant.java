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
import com.google.dart.compiler.util.apache.FilenameUtils;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartPart;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.net.URIUtilities;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.net.URI;
import java.util.List;

/**
 * {@link MoveParticipant} for updating resource references in Dart libraries.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class MoveResourceParticipant extends MoveParticipant {

  private final TextChangeManager changeManager = new TextChangeManager(true);

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
  public Change createPreChange(final IProgressMonitor pm) throws CoreException,
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
    return RefactoringMessages.MoveResourceParticipant_name;
  }

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof IFile) {
      file = (IFile) element;
      return true;
    }
    return false;
  }

  private void addReferenceUpdate(SearchMatch match, URI destUri) throws Exception {
    CompilationUnit cu = match.getElement().getAncestor(CompilationUnit.class);
    // prepare name prefix
    String namePrefix;
    {
      URI sourceUri = cu.getResource().getParent().getLocationURI();
      namePrefix = sourceUri.relativize(destUri).toString();
      if (namePrefix.length() != 0) {
        namePrefix += "/";
      }
      namePrefix = FilenameUtils.separatorsToUnix(namePrefix);
    }
    // prepare "old name" range
    SourceRange matchRange = match.getSourceRange();
    int begin = matchRange.getOffset() + "'".length();
    int end = SourceRangeUtils.getEnd(matchRange) - "'".length() - file.getName().length();
    // add TextEdit to rename "old name" with "new name"
    TextEdit edit = new ReplaceEdit(begin, end - begin, namePrefix);
    addTextEdit(cu, RefactoringCoreMessages.RenameProcessor_update_reference, edit);
  }

  private void addTextEdit(CompilationUnit unit, String groupName, TextEdit textEdit) {
    if (unit.getResource() != null) {
      TextChange change = getTextChange(unit);
      if (change == null) {
        change = changeManager.get(unit);
      }
      TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
    }
  }

  /**
   * Updates URI of "targetUnit" referenced from "sourceUnit".
   */
  private void addUnitUriTextEdit(CompilationUnit sourceUnit, URI sourceUri, SourceRange uriRange,
      CompilationUnit targetUnit) {
    IResource targetResource = targetUnit.getResource();
    if (targetResource != null) {
      URI targetUri = targetResource.getLocationURI();
      if (URIUtilities.isFileUri(sourceUri) && URIUtilities.isFileUri(targetUri)) {
        URI relative = URIUtilities.relativize(sourceUri, targetUri);
        String relativeStr = FilenameUtils.separatorsToUnix(relative.toString());
        String relativeSource = "'" + relativeStr + "'";
        ReplaceEdit textEdit = new ReplaceEdit(
            uriRange.getOffset(),
            uriRange.getLength(),
            relativeSource);
        String msg = RefactoringCoreMessages.RenameProcessor_update_reference;
        addTextEdit(sourceUnit, msg, textEdit);
      }
    }
  }

  /**
   * Implementation of {@link #createChange(IProgressMonitor)} which can throw any exception.
   */
  private Change createChangeEx(IProgressMonitor pm) throws Exception {
    MoveArguments arguments = getArguments();
    // update references
    Object destination = arguments.getDestination();
    if (arguments.getUpdateReferences() && destination instanceof IContainer) {
      IContainer destContainer = (IContainer) destination;
      URI destURI = ((IContainer) destination).getLocationURI();
      // update references to moving unit
      SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
      List<SearchMatch> references = searchEngine.searchReferences(file, null, null, pm);
      for (SearchMatch match : references) {
        addReferenceUpdate(match, destURI);
      }
      // if moved Unit is defining library, updates references from it to its components
      {
        DartElement fileElement = DartCore.create(file);
        if (fileElement instanceof CompilationUnit) {
          CompilationUnit unit = (CompilationUnit) fileElement;
          DartLibrary library = unit.getLibrary();
          if (library != null && Objects.equal(library.getDefiningCompilationUnit(), unit)) {
            URI newUnitUri = destContainer.getFile(new Path("no-matter")).getLocationURI();
            // "import"
            for (DartImport imp : library.getImports()) {
              SourceRange uriRange = imp.getUriRange();
              CompilationUnit impUnit = imp.getLibrary().getDefiningCompilationUnit();
              addUnitUriTextEdit(unit, newUnitUri, uriRange, impUnit);
            }
            // "part"
            for (DartPart part : library.getParts()) {
              SourceRange uriRange = part.getUriRange();
              CompilationUnit partUnit = part.getUnit();
              addUnitUriTextEdit(unit, newUnitUri, uriRange, partUnit);
            }
          }
        }
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
}
