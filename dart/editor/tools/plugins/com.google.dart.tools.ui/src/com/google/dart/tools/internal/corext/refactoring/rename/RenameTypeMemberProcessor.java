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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.List;
import java.util.Set;

/**
 * {@link DartRenameProcessor} for {@link TypeMember}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public abstract class RenameTypeMemberProcessor extends DartRenameProcessor {

  private static void addTextEdit(TextChange change, String groupName, TextEdit textEdit) {
    TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
  }

  protected final TypeMember member;
  private final String oldName;
  private final TextChangeManager changeManager = new TextChangeManager(true);

  private List<SearchMatch> references;

  /**
   * @param member the {@link TypeMember} to rename, not <code>null</code>.
   */
  public RenameTypeMemberProcessor(TypeMember member) {
    this.member = member;
    oldName = member.getElementName();
    setNewElementName(oldName);
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    return Checks.checkIfCuBroken(member);
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();

    if (Checks.isAlreadyNamed(member, newName)) {
      result.addFatalError(
          RefactoringCoreMessages.RenameRefactoring_another_name,
          DartStatusContext.create(member));
      return result;
    }

    // type can not have two members with same name
    {
      Type enclosingType = member.getDeclaringType();
      TypeMember[] existingMembers = enclosingType.getExistingMembers(newName);
      for (TypeMember existingMember : existingMembers) {
        IPath resourcePath = enclosingType.getResource().getFullPath();
        String message = Messages.format(
            RefactoringCoreMessages.RenameRefactoring_enclosing_type_member_already_defined,
            new Object[] {
                enclosingType.getElementName(),
                BasicElementLabels.getPathLabel(resourcePath, false),
                newName});
        result.addError(message, DartStatusContext.create(existingMember));
      }
    }

    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameRefactoring_checking, 1);
    try {
      return new CompositeChange(getProcessorName(), changeManager.getAllChanges());
    } finally {
      monitor.done();
    }
  }

  @Override
  public final String getCurrentElementName() {
    return member.getElementName();
  }

  @Override
  public Object[] getElements() {
    return new Object[] {member};
  }

  @Override
  public int getSaveMode() {
    return RefactoringSaveHelper.SAVE_ALL;
  }

  @Override
  protected RefactoringStatus doCheckFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException {
    try {
      pm.beginTask("", 19); //$NON-NLS-1$
      pm.setTaskName(RefactoringCoreMessages.RenameRefactoring_checking);
      RefactoringStatus result = new RefactoringStatus();
      // check new name
      result.merge(checkNewElementName(getNewElementName()));
      pm.worked(1);
      // prepare references
      pm.setTaskName(RefactoringCoreMessages.RenameRefactoring_searching);
      prepareReferences(new SubProgressMonitor(pm, 3));
      pm.setTaskName(RefactoringCoreMessages.RenameRefactoring_checking);
      // analyze affected units (such as warn about existing compilation errors)
      result.merge(analyzeAffectedCompilationUnits());
      // check for possible conflicts
      result.merge(analyzePossibleConflicts(new SubProgressMonitor(pm, 10)));
      if (result.hasFatalError()) {
        return result;
      }
      // OK, create changes
      createChanges(new SubProgressMonitor(pm, 5));
      return result;
    } finally {
      pm.done();
    }
  }

  private void addDeclarationUpdate() throws CoreException {
    SourceRange nameRange = member.getNameRange();
    CompilationUnit cu = member.getCompilationUnit();
    String editName = RefactoringCoreMessages.RenameRefactoring_update_declaration;
    addTextEdit(changeManager.get(cu), editName, createTextChange(nameRange));
  }

  private void addReferenceUpdates(IProgressMonitor pm) throws DartModelException {
    pm.beginTask("", references.size()); //$NON-NLS-1$
    String editName = RefactoringCoreMessages.RenameRefactoring_update_reference;
    for (SearchMatch match : references) {
      CompilationUnit cu = match.getElement().getAncestor(CompilationUnit.class);
      SourceRange matchRange = match.getSourceRange();
      addTextEdit(changeManager.get(cu), editName, createTextChange(matchRange));
      pm.worked(1);
    }
  }

  private RefactoringStatus analyzeAffectedCompilationUnits() throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    result.merge(Checks.checkCompileErrorsInAffectedFiles(references));
    return result;
  }

  private RefactoringStatus analyzePossibleConflicts(IProgressMonitor pm) throws CoreException {
    pm.beginTask("Analyze possible conflicts", 3);
    try {
      RefactoringStatus result = new RefactoringStatus();
      String newName = getNewElementName();
      Type enclosingType = member.getAncestor(Type.class);
      List<Type> subTypes = RenameAnalyzeUtil.getSubTypes(enclosingType);
      Iterable<Type> enclosingAndSubTypes = Iterables.concat(
          ImmutableSet.of(enclosingType),
          subTypes);
      // analyze top-level elements
      pm.subTask("Analyze top-level elements");
      {
        CompilationUnitElement topLevelElement = RenameAnalyzeUtil.getTopLevelElementNamed(
            Sets.<DartLibrary>newHashSet(),
            member,
            newName);
        if (topLevelElement != null) {
          DartLibrary shadowLibrary = topLevelElement.getAncestor(DartLibrary.class);
          IPath libraryPath = shadowLibrary.getResource().getFullPath();
          IPath resourcePath = topLevelElement.getResource().getFullPath();
          // add warning for shadowing top-level declaration
          {
            String message = Messages.format(
                RefactoringCoreMessages.RenameProcessor_topLevelDecl_shadowedBy_element,
                new Object[] {
                    RenameAnalyzeUtil.getElementTypeName(topLevelElement),
                    newName,
                    BasicElementLabels.getPathLabel(resourcePath, false),
                    BasicElementLabels.getPathLabel(libraryPath, false),
                    RenameAnalyzeUtil.getElementTypeName(member)});
            result.addWarning(message, DartStatusContext.create(topLevelElement));
          }
          // TypeMember shadows top-level element usage in enclosing type
          {
            List<SearchMatch> refs = RenameAnalyzeUtil.getReferences(topLevelElement);
            for (SearchMatch ref : refs) {
              if (SourceRangeUtils.intersects(ref.getSourceRange(), enclosingType.getSourceRange())) {
                String message = Messages.format(
                    RefactoringCoreMessages.RenameProcessor_topLevelUsage_shadowedBy_element,
                    new Object[] {
                        RenameAnalyzeUtil.getElementTypeName(topLevelElement),
                        newName,
                        BasicElementLabels.getPathLabel(resourcePath, false),
                        BasicElementLabels.getPathLabel(libraryPath, false),
                        RenameAnalyzeUtil.getElementTypeName(member)});
                result.addError(message, DartStatusContext.create(ref));
              }
            }
          }
          // top-level element shadows TypeMember usage in sub-type
          // http://code.google.com/p/dart/issues/detail?id=1180
          for (Type subType : subTypes) {
            for (SearchMatch ref : references) {
              if (SourceRangeUtils.intersects(ref.getSourceRange(), subType.getSourceRange())) {
                String message = Messages.format(
                    RefactoringCoreMessages.RenameProcessor_typeMemberUsage_shadowedBy_topLevel,
                    new Object[] {
                        RenameAnalyzeUtil.getElementTypeName(member),
                        enclosingType.getElementName(),
                        member.getElementName(),
                        RenameAnalyzeUtil.getElementTypeName(topLevelElement),
                        newName,
                        BasicElementLabels.getPathLabel(resourcePath, false),
                        BasicElementLabels.getPathLabel(libraryPath, false)});
                result.addError(message, DartStatusContext.create(ref));
              }
            }
          }
        }
      }
      // analyze supertypes
      pm.subTask("Analyze supertypes");
      {
        Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(enclosingType);
        for (Type superType : superTypes) {
          TypeMember[] superTypeMembers = superType.getExistingMembers(newName);
          for (TypeMember superTypeMember : superTypeMembers) {
            // add warning for hiding super-type TypeMember
            {
              IPath resourcePath = superType.getResource().getFullPath();
              String message = Messages.format(
                  RefactoringCoreMessages.RenameProcessor_typeMemberDecl_shadowedBy_element,
                  new Object[] {
                      RenameAnalyzeUtil.getElementTypeName(superTypeMember),
                      superType.getElementName(),
                      newName,
                      BasicElementLabels.getPathLabel(resourcePath, false),
                      RenameAnalyzeUtil.getElementTypeName(member)});
              result.addWarning(message, DartStatusContext.create(superTypeMember));
            }
            // add error for using hidden super-type TypeMember
            {
              List<SearchMatch> refs = RenameAnalyzeUtil.getReferences(superTypeMember);
              for (SearchMatch ref : refs) {
                for (Type subType : enclosingAndSubTypes) {
                  if (SourceRangeUtils.intersects(ref.getSourceRange(), subType.getSourceRange())) {
                    IPath resourcePath = superType.getResource().getFullPath();
                    String message = Messages.format(
                        RefactoringCoreMessages.RenameProcessor_typeMemberUsage_shadowedBy_element,
                        new Object[] {
                            RenameAnalyzeUtil.getElementTypeName(superTypeMember),
                            superType.getElementName(),
                            newName,
                            BasicElementLabels.getPathLabel(resourcePath, false),
                            RenameAnalyzeUtil.getElementTypeName(member)});
                    result.addError(message, DartStatusContext.create(ref));
                  }
                }
              }
            }
          }
        }
      }
      pm.worked(1);
      // analyze [sub-]type members
      pm.subTask("Analyze subtypes");
      for (Type subType : enclosingAndSubTypes) {
        // check for declared members of sub-types
        if (!Objects.equal(subType, enclosingType)) {
          TypeMember[] subTypeMembers = subType.getExistingMembers(newName);
          for (TypeMember subTypeMember : subTypeMembers) {
            IPath resourcePath = subType.getResource().getFullPath();
            // add warning for hiding Renamed declaration
            {
              String message = Messages.format(
                  RefactoringCoreMessages.RenameTopRefactoring_elementDecl_shadowedBy_typeMember,
                  new Object[] {
                      RenameAnalyzeUtil.getElementTypeName(member),
                      RenameAnalyzeUtil.getElementTypeName(subTypeMember),
                      subType.getElementName(),
                      newName,
                      BasicElementLabels.getPathLabel(resourcePath, false)});
              result.addWarning(message, DartStatusContext.create(subTypeMember));
            }
            // add error for hiding Renamed usage
            {
              List<Type> subTypes2 = RenameAnalyzeUtil.getSubTypes(subType);
              subTypes2.add(subType);
              for (SearchMatch ref : references) {
                Type refEnclosingType = ref.getElement().getAncestor(Type.class);
                if (subTypes2.contains(refEnclosingType)) {
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameTopRefactoring_elementUsage_shadowedBy_typeMember,
                      new Object[] {
                          RenameAnalyzeUtil.getElementTypeName(member),
                          RenameAnalyzeUtil.getElementTypeName(subTypeMember),
                          subType.getElementName(),
                          newName,
                          BasicElementLabels.getPathLabel(resourcePath, false)});
                  result.addError(message, DartStatusContext.create(ref));
                }
              }
            }
          }
        }
        // check for local variables
        for (Method method : subType.getMethods()) {
          DartVariableDeclaration[] localVariables = method.getLocalVariables();
          for (DartVariableDeclaration variable : localVariables) {
            if (variable.getElementName().equals(newName)) {
              IPath resourcePath = subType.getResource().getFullPath();
              // add warning for hiding Renamed declaration
              {
                String message = Messages.format(
                    RefactoringCoreMessages.RenameTopRefactoring_elementDecl_shadowedBy_variable_inMethod,
                    new Object[] {
                        RenameAnalyzeUtil.getElementTypeName(member),
                        subType.getElementName(),
                        method.getElementName(),
                        BasicElementLabels.getPathLabel(resourcePath, false)});
                result.addWarning(message, DartStatusContext.create(variable));
              }
              // add error for hiding Renamed usage
              for (SearchMatch match : references) {
                if (SourceRangeUtils.intersects(match.getSourceRange(), variable.getVisibleRange())) {
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameTopRefactoring_elementUsage_shadowedBy_variable_inMethod,
                      new Object[] {
                          RenameAnalyzeUtil.getElementTypeName(member),
                          subType.getElementName(),
                          method.getElementName(),
                          BasicElementLabels.getPathLabel(resourcePath, false)});
                  result.addError(message, DartStatusContext.create(match));
                }
              }
            }
          }
        }
      }
      pm.worked(1);

      // OK
      return result;
    } finally {
      pm.done();
    }
  }

  private void createChanges(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.RenameRefactoring_checking, 10);
    changeManager.clear();
    // update declaration
    addDeclarationUpdate();
    pm.worked(1);
    // update references
    addReferenceUpdates(new SubProgressMonitor(pm, 9));
    pm.done();
  }

  private TextEdit createTextChange(SourceRange sourceRange) {
    return new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), getNewElementName());
  }

  private void prepareReferences(final IProgressMonitor pm) throws CoreException {
    references = RenameAnalyzeUtil.getReferences(member);
  }
}
