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
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.Set;

/**
 * {@link DartRenameProcessor} for {@link DartImport}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameImportProcessor extends RenameTopLevelProcessor {

  private final DartImport imprt;
  private final DartLibrary enclosingLibrary;
  private final boolean hasPrefix;

  private boolean willHavePrefix;

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameImportProcessor"; //$NON-NLS-1$

  /**
   * @param imprt the {@link DartImport} to rename.
   */
  public RenameImportProcessor(DartImport imprt) {
    super(imprt);
    this.imprt = imprt;
    this.enclosingLibrary = imprt.getAncestor(DartLibrary.class);
    this.hasPrefix = oldName != null;
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    // no name - removing prefix
    willHavePrefix = !StringUtils.isEmpty(newName);
    if (!willHavePrefix) {
      return new RefactoringStatus();
    }
    // validate new name
    RefactoringStatus result = Checks.checkPrefixName(newName);
    result.merge(super.checkNewElementName(newName));
    return result;
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return ExecutionUtils.runObjectIgnore(new RunnableObjectEx<DartImport>() {
      @Override
      public DartImport runObject() throws Exception {
        DartImport[] newImports = enclosingLibrary.getImports();
        DartImport result = null;
        for (DartImport newImport : newImports) {
          if (Objects.equal(newImport.getPrefix(), getNewElementName())
              && Objects.equal(newImport.getLibrary(), imprt.getLibrary())) {
            result = newImport;
          }
        }
        return result;
      }
    },
        null);
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameImportProcessor_name;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(imprt);
  }

  @Override
  protected void addDeclarationUpdate() throws CoreException {
    CompilationUnit cu = imprt.getCompilationUnit();
    String editName = RefactoringCoreMessages.RenameProcessor_update_declaration;
    if (hasPrefix && willHavePrefix) {
      SourceRange nameRange = imprt.getNameRange();
      addTextEdit(cu, editName, createTextChange(nameRange));
    } else if (!hasPrefix) {
      SourceRange uriRange = imprt.getUriRange();
      int uriEnd = SourceRangeUtils.getEnd(uriRange);
      addTextEdit(cu, editName, new ReplaceEdit(uriEnd, 0, " as " + newName));
    } else if (!willHavePrefix) {
      SourceRange uriRange = imprt.getUriRange();
      SourceRange sourceRange = imprt.getSourceRange();
      int uriEnd = SourceRangeUtils.getEnd(uriRange);
      int sourceEnd = SourceRangeUtils.getEnd(sourceRange);
      addTextEdit(cu, editName, new ReplaceEdit(uriEnd, sourceEnd - uriEnd, ";"));
    }
  }

  @Override
  protected void addReferenceUpdate(SearchMatch match) {
    String newPrefix = getNewElementName();
    SourceRange matchRange = match.getSourceRange();
    // prepare TextEdit
    TextEdit textEdit;
    if (willHavePrefix) {
      if (!hasPrefix) {
        newPrefix += ".";
      }
      textEdit = new ReplaceEdit(matchRange.getOffset(), matchRange.getLength(), newPrefix);
    } else {
      textEdit = new ReplaceEdit(matchRange.getOffset(), matchRange.getLength() + 1, "");
    }
    // add TextEdit
    CompilationUnit cu = match.getElement().getAncestor(CompilationUnit.class);
    String editName = RefactoringCoreMessages.RenameProcessor_update_reference;
    addTextEdit(cu, editName, textEdit);
  }

  @Override
  protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    result.merge(analyzeTopLevelConflictsInSamePrefix());
    result.merge(super.doCheckFinalConditions(pm, context));
    return result;
  }

  private RefactoringStatus analyzeTopLevelConflictsInSamePrefix() throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    // if same name, do nothing
    if (Objects.equal(newName, oldName)) {
      return result;
    }
    // analyze other imports
    DartLibrary thisLibrary = imprt.getLibrary();
    Set<String> thisNames = null;
    for (DartImport currentImport : enclosingLibrary.getImports()) {
      // analyze import with same prefix
      if (Objects.equal(currentImport.getPrefix(), newName)) {
        // initialize lazily, only if there is prefix conflict
        if (thisNames == null) {
          thisNames = RenameAnalyzeUtil.getExportedTopLevelNames(thisLibrary);
        }
        // prepare current library names
        DartLibrary currentLibrary = currentImport.getLibrary();
        Set<String> currentNames = RenameAnalyzeUtil.getExportedTopLevelNames(currentLibrary);
        // if has intersection, report error
        SetView<String> intersection = Sets.intersection(currentNames, thisNames);
        if (!intersection.isEmpty()) {
          IPath thisLibraryPath = thisLibrary.getDefiningCompilationUnit().getResource().getFullPath();
          IPath currentLibraryPath = currentLibrary.getDefiningCompilationUnit().getResource().getFullPath();
          String message = Messages.format(
              RefactoringCoreMessages.RenameImportProcessor_duplicateTopLevels_samePrefix,
              new Object[] {
                  newName, BasicElementLabels.getPathLabel(thisLibraryPath, false),
                  BasicElementLabels.getPathLabel(currentLibraryPath, false), intersection});
          result.addError(message, DartStatusContext.create(currentImport));
        }
      }
    }
    // done
    return result;
  }
}
