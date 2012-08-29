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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.MatchQuality;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * {@link DartRenameProcessor} for {@link TypeMember}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public abstract class RenameTypeMemberProcessor extends DartRenameProcessor {

  /**
   * Analyzes possible conflicts when {@link DartElement} is renamed (or created) to the given
   * "newName" - will it shadow some elements, or will it be shadowed by other elements.
   */
  public static RefactoringStatus analyzePossibleConflicts(int elementType, Type enclosingType,
      String oldName, List<SearchMatch> references, String newName, IProgressMonitor pm)
      throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    String elementTypeName = RenameAnalyzeUtil.getElementTypeName(elementType);
    // prepare types
    List<Type> subTypes = RenameAnalyzeUtil.getSubTypes(enclosingType);
    Iterable<Type> enclosingAndSubTypes = Iterables.concat(ImmutableSet.of(enclosingType), subTypes);
    // analyze top-level elements
    pm.subTask("Analyze top-level elements");
    {
      CompilationUnitElement topLevelElement = RenameAnalyzeUtil.getTopLevelElementNamed(
          enclosingType,
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
                  RenameAnalyzeUtil.getElementTypeName(topLevelElement), newName,
                  BasicElementLabels.getPathLabel(resourcePath, false),
                  BasicElementLabels.getPathLabel(libraryPath, false), elementTypeName});
          result.addWarning(message, DartStatusContext.create(topLevelElement));
        }
        // TypeMember shadows top-level element usage in enclosing type
        {
          List<SearchMatch> refs = RenameAnalyzeUtil.getReferences(topLevelElement, null);
          for (SearchMatch ref : refs) {
            if (SourceRangeUtils.intersects(ref.getSourceRange(), enclosingType.getSourceRange())) {
              String message = Messages.format(
                  RefactoringCoreMessages.RenameProcessor_topLevelUsage_shadowedBy_element,
                  new Object[] {
                      RenameAnalyzeUtil.getElementTypeName(topLevelElement), newName,
                      BasicElementLabels.getPathLabel(resourcePath, false),
                      BasicElementLabels.getPathLabel(libraryPath, false), elementTypeName});
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
                      elementTypeName, enclosingType.getElementName(), oldName,
                      RenameAnalyzeUtil.getElementTypeName(topLevelElement), newName,
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
                    superType.getElementName(), newName,
                    BasicElementLabels.getPathLabel(resourcePath, false), elementTypeName});
            result.addWarning(message, DartStatusContext.create(superTypeMember));
          }
          // add error for using hidden super-type TypeMember
          {
            List<SearchMatch> refs = RenameAnalyzeUtil.getReferences(superTypeMember, null);
            for (SearchMatch ref : refs) {
              for (Type subType : enclosingAndSubTypes) {
                if (SourceRangeUtils.intersects(ref.getSourceRange(), subType.getSourceRange())) {
                  IPath resourcePath = superType.getResource().getFullPath();
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameProcessor_typeMemberUsage_shadowedBy_element,
                      new Object[] {
                          RenameAnalyzeUtil.getElementTypeName(superTypeMember),
                          superType.getElementName(), newName,
                          BasicElementLabels.getPathLabel(resourcePath, false), elementTypeName});
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
      boolean isEnclosingType = subType == enclosingType;
      IPath resourcePath = subType.getPath();
      // TypeParameter shadowed by Renamed in enclosing type
      if (isEnclosingType) {
        DartTypeParameter[] typeParameters = subType.getTypeParameters();
        for (DartTypeParameter parameter : typeParameters) {
          if (Objects.equal(parameter.getElementName(), newName)) {
            // add warning for shadowing TypeParameter declaration
            {
              String message = Messages.format(
                  RefactoringCoreMessages.RenameProcessor_typeMemberDecl_shadowedBy_element,
                  new Object[] {
                      RenameAnalyzeUtil.getElementTypeName(parameter), subType.getElementName(),
                      newName, resourcePath, elementTypeName,});
              result.addWarning(message, DartStatusContext.create(parameter));
            }
            // add error for shadowing TypeParameter usage
            List<SourceRange> parameterReferences = RenameAnalyzeUtil.getReferences(parameter);
            for (SourceRange parameterReference : parameterReferences) {
              if (SourceRangeUtils.intersects(parameterReference, subType.getSourceRange())) {
                String message = Messages.format(
                    RefactoringCoreMessages.RenameProcessor_typeMemberUsage_shadowedBy_element,
                    new Object[] {
                        RenameAnalyzeUtil.getElementTypeName(parameter), subType.getElementName(),
                        newName, resourcePath, elementTypeName,});
                result.addError(
                    message,
                    DartStatusContext.create(subType.getCompilationUnit(), parameterReference));
              }
            }
          }
        }
      }
      // TypeParameter shadows Renamed in sub-types
      if (!isEnclosingType) {
        DartTypeParameter[] typeParameters = subType.getTypeParameters();
        for (DartTypeParameter parameter : typeParameters) {
          // add warning for shadowing member declaration
          {
            String message = Messages.format(
                RefactoringCoreMessages.RenameProcessor_elementDecl_shadowedBy_typeMember,
                new Object[] {
                    elementTypeName, RenameAnalyzeUtil.getElementTypeName(parameter),
                    subType.getElementName(), newName, resourcePath,});
            result.addWarning(message, DartStatusContext.create(parameter));
          }
          // add error for shadowing member usage
          for (SearchMatch reference : references) {
            if (SourceRangeUtils.intersects(reference.getSourceRange(), subType.getSourceRange())) {
              String message = Messages.format(
                  RefactoringCoreMessages.RenameProcessor_elementUsage_shadowedBy_typeMember,
                  new Object[] {
                      elementTypeName, RenameAnalyzeUtil.getElementTypeName(parameter),
                      subType.getElementName(), newName, resourcePath,});
              result.addError(message, DartStatusContext.create(reference));
            }
          }
        }
      }
      // check for declared members of sub-types
      if (!Objects.equal(subType, enclosingType)) {
        // analyze TypeMember children
        TypeMember[] subTypeMembers = subType.getExistingMembers(newName);
        for (TypeMember subTypeMember : subTypeMembers) {
          // add warning for hiding Renamed declaration
          {
            String message = Messages.format(
                RefactoringCoreMessages.RenameProcessor_elementDecl_shadowedBy_typeMember,
                new Object[] {
                    elementTypeName, RenameAnalyzeUtil.getElementTypeName(subTypeMember),
                    subType.getElementName(), newName,
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
                    RefactoringCoreMessages.RenameProcessor_elementUsage_shadowedBy_typeMember,
                    new Object[] {
                        elementTypeName, RenameAnalyzeUtil.getElementTypeName(subTypeMember),
                        subType.getElementName(), newName,
                        BasicElementLabels.getPathLabel(resourcePath, false)});
                result.addError(message, DartStatusContext.create(ref));
              }
            }
          }
        }
      }
      // check for local variables
      for (Method method : subType.getMethods()) {
        List<FunctionLocalElement> localVariables = RenameAnalyzeUtil.getFunctionLocalElements(method);
        for (FunctionLocalElement variable : localVariables) {
          if (variable.getElementName().equals(newName)) {
            CompilationUnitElement variableElement = variable.getElement();
            // add warning for hiding Renamed declaration
            {
              String message = Messages.format(
                  RefactoringCoreMessages.RenameProcessor_elementDecl_shadowedBy_variable_inMethod,
                  new Object[] {
                      elementTypeName, RenameAnalyzeUtil.getElementTypeName(variableElement),
                      subType.getElementName(), method.getElementName(),
                      BasicElementLabels.getPathLabel(resourcePath, false)});
              result.addWarning(message, DartStatusContext.create(variableElement));
            }
            // add error for hiding Renamed usage
            for (SearchMatch match : references) {
              if (SourceRangeUtils.intersects(match.getSourceRange(), variable.getVisibleRange())) {
                String message = Messages.format(
                    RefactoringCoreMessages.RenameProcessor_elementUsage_shadowedBy_variable_inMethod,
                    new Object[] {
                        elementTypeName, RenameAnalyzeUtil.getElementTypeName(variableElement),
                        subType.getElementName(), method.getElementName(),
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
  }

  protected final TypeMember member;
  protected final String oldName;
  private final TextChangeManager changeManager = new TextChangeManager(true);

  private List<SearchMatch> declarations;

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
    RefactoringStatus result = Checks.checkIfCuBroken(member);
    result.merge(RenameAnalyzeUtil.checkLocalElement(member));
    return result;
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();

    if (Checks.isAlreadyNamed(member, newName)) {
      result.addFatalError(
          RefactoringCoreMessages.RenameProcessor_another_name,
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
            RefactoringCoreMessages.RenameProcessor_enclosing_type_member_already_defined,
            new Object[] {
                enclosingType.getElementName(),
                BasicElementLabels.getPathLabel(resourcePath, false), newName});
        result.addError(message, DartStatusContext.create(existingMember));
      }
    }

    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameProcessor_checking, 1);
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
  protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException {
    try {
      pm.beginTask("", 19); //$NON-NLS-1$
      pm.setTaskName(RefactoringCoreMessages.RenameProcessor_checking);
      RefactoringStatus result = new RefactoringStatus();
      // check new name
      result.merge(checkNewElementName(getNewElementName()));
      pm.worked(1);
      // prepare references
      pm.setTaskName(RefactoringCoreMessages.RenameProcessor_searching);
      prepareReferences(new SubProgressMonitor(pm, 3));
      pm.setTaskName(RefactoringCoreMessages.RenameProcessor_checking);
      // analyze affected units (such as warn about existing compilation errors)
      result.merge(RenameAnalyzeUtil.checkReferencesSource(references, oldName));
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

  protected String getNewNameSource() {
    return getNewElementName();
  }

  private void addDeclarationUpdates(IProgressMonitor pm) throws CoreException {
    String editName = RefactoringCoreMessages.RenameProcessor_update_declaration;
    addUpdates(pm, editName, declarations);
  }

  private void addReferenceUpdates(IProgressMonitor pm) throws CoreException {
    String editName = RefactoringCoreMessages.RenameProcessor_update_reference;
    addUpdates(pm, editName, references);
  }

  private void addTextEdit(CompilationUnit unit, String groupName, TextEdit textEdit) {
    if (unit.getResource() != null) {
      TextChange change = changeManager.get(unit);
      TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
    }
  }

  private void addUpdates(IProgressMonitor pm, String editName, List<SearchMatch> matches)
      throws CoreException {
    pm.beginTask("", matches.size()); //$NON-NLS-1$
    for (SearchMatch match : matches) {
      CompilationUnit cu = match.getElement().getAncestor(CompilationUnit.class);
      SourceRange matchRange = match.getSourceRange();
      addTextEdit(cu, editName, createTextChange(matchRange));
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
    RefactoringStatus result = new RefactoringStatus();
    // add error if will become private
    result.merge(RenameAnalyzeUtil.checkBecomePrivate(oldName, newName, member, references));
    pm.worked(1);
    // analyze conflicts
    int elementType = member.getElementType();
    Type enclosingType = member.getAncestor(Type.class);
    result.merge(analyzePossibleConflicts(
        elementType,
        enclosingType,
        oldName,
        references,
        newName,
        pm));
    pm.worked(2);
    // done
    pm.done();
    return result;
  }

  private void createChanges(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.RenameProcessor_checking, 12);
    changeManager.clear();
    // update declaration
    addDeclarationUpdates(new SubProgressMonitor(pm, 2));
    // update references
    addReferenceUpdates(new SubProgressMonitor(pm, 10));
    pm.done();
  }

  private TextEdit createTextChange(SourceRange sourceRange) {
    return new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), getNewNameSource());
  }

  private void prepareReferences(IProgressMonitor pm) throws CoreException {
    String name = member.getElementName();
    // prepare types which have member with required name
    Set<Type> renameTypes;
    {
      renameTypes = Sets.newHashSet();
      Set<Type> checkedTypes = Sets.newHashSet();
      LinkedList<Type> checkTypes = Lists.newLinkedList();
      checkTypes.add(member.getDeclaringType());
      while (!checkTypes.isEmpty()) {
        Type type = checkTypes.removeFirst();
        // may be already checked
        if (checkedTypes.contains(type)) {
          continue;
        }
        checkedTypes.add(type);
        // if has member with required name, then may be its super-types and sub-types too
        if (type.getExistingMembers(name).length != 0) {
          renameTypes.add(type);
          checkTypes.addAll(RenameAnalyzeUtil.getSuperTypes(type));
          checkTypes.addAll(RenameAnalyzeUtil.getSubTypes(type));
        }
      }
    }
    // prepare all declarations and references to members
    declarations = Lists.newArrayList();
    references = Lists.newArrayList();
    for (Type type : renameTypes) {
      for (TypeMember typeMember : type.getExistingMembers(name)) {
        declarations.add(new SearchMatch(MatchQuality.EXACT, type, typeMember.getNameRange()));
        references.addAll(RenameAnalyzeUtil.getReferences(typeMember, null));
      }
    }
    // done
    pm.done();
  }
}
