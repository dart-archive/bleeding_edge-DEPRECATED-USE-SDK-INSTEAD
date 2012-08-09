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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
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

import org.eclipse.core.resources.IResource;
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

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * {@link DartRenameProcessor} for top-level {@link DartElement}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public abstract class RenameTopLevelProcessor extends DartRenameProcessor {

  /**
   * Analyzes possible conflicts when {@link DartElement} is renamed (or created) to the given
   * "newName" - will it shadow some elements, or will it be shadowed by other elements.
   */
  public static RefactoringStatus analyzePossibleConflicts(DartLibrary elementParentLibrary,
      int elementType, boolean elementIsImport, List<SearchMatch> references, String newName)
      throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    String elementTypeName = RenameAnalyzeUtil.getElementTypeName(elementType);
    // prepare libraries with references
    Set<DartLibrary> libraries = Sets.newHashSet();
    libraries.add(elementParentLibrary);
    for (SearchMatch reference : references) {
      DartLibrary library = reference.getElement().getAncestor(DartLibrary.class);
      libraries.add(library);
    }
    // may be declaring library has top-level element with same name
    {
      CompilationUnitElement unitElement = RenameAnalyzeUtil.getTopLevelElementNamed(
          elementParentLibrary,
          newName);
      if (unitElement != null) {
        boolean ignorePrefixConflict = elementIsImport && unitElement instanceof DartImport;
        if (!ignorePrefixConflict) {
          reportTopLevelAlreadyDeclared(result, newName, elementParentLibrary, unitElement);
        }
      }
    }
    // may be conflict with existing top-level element, imported using same prefix
    for (SearchMatch reference : references) {
      String referencePrefix = reference.getImportPrefix();
      DartLibrary referenceLibrary = reference.getElement().getAncestor(DartLibrary.class);
      for (DartImport imprt : referenceLibrary.getImports()) {
        if (StringUtils.equals(referencePrefix, imprt.getPrefix())) {
          Set<String> importNames = RenameAnalyzeUtil.getImportedTopLevelNames(imprt);
          if (importNames.contains(newName)) {
            DartLibrary importLibrary = imprt.getLibrary();
            CompilationUnit importLibraryUnit = importLibrary.getDefiningCompilationUnit();
            IPath importLibraryPath = importLibraryUnit.getResource().getFullPath();
            String message = Messages.format(
                RefactoringCoreMessages.RenameTopProcessor_shadow_topLevel2,
                new Object[] {
                    RenameAnalyzeUtil.getElementTypeName(elementType),
                    getResourcePathLabel(importLibraryPath)});
            result.addError(message, DartStatusContext.create(reference));
          }
        }
      }
    }
    // visit libraries with references
    for (DartLibrary library : libraries) {
      // visit units of library
      for (CompilationUnit unit : library.getCompilationUnitsInScope()) {
        // visit top-level children of unit
        for (DartElement unitElement : unit.getChildren()) {
          // analyze Type
          if (unitElement instanceof Type) {
            Type type = (Type) unitElement;
            // analyze type parameters
            for (DartTypeParameter typeParameter : type.getTypeParameters()) {
              if (Objects.equal(typeParameter.getElementName(), newName)) {
                IPath resourcePath = getElementPath(unitElement);
                // warning for shadowing top-level declaration
                {
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameProcessor_elementDecl_shadowedBy_typeMember,
                      new Object[] {
                          elementTypeName, RenameAnalyzeUtil.getElementTypeName(typeParameter),
                          type.getElementName(), typeParameter.getElementName(),
                          getResourcePathLabel(resourcePath),});
                  result.addWarning(message, DartStatusContext.create(typeParameter));
                }
                // error for shadowing top-level usage
                for (SearchMatch match : references) {
                  if (SourceRangeUtils.contains(
                      type.getSourceRange(),
                      match.getSourceRange().getOffset())) {
                    String message = Messages.format(
                        RefactoringCoreMessages.RenameProcessor_elementUsage_shadowedBy_typeMember,
                        new Object[] {
                            elementTypeName, RenameAnalyzeUtil.getElementTypeName(typeParameter),
                            type.getElementName(), typeParameter.getElementName(),
                            getResourcePathLabel(resourcePath),});
                    result.addError(message, DartStatusContext.create(match));
                  }
                }
              }
            }
            // visit type members
            List<TypeMember> typeMembers = RenameAnalyzeUtil.getTypeMembers(type);
            for (TypeMember typeMember : typeMembers) {
              // may be conflict with existing TypeMember
              if (Objects.equal(typeMember.getElementName(), newName)) {
                // add warning if TypeMember shadows top-level declaration
                {
                  IPath resourcePath = getElementPath(unitElement);
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameProcessor_elementDecl_shadowedBy_typeMember,
                      new Object[] {
                          elementTypeName, RenameAnalyzeUtil.getElementTypeName(typeMember),
                          type.getElementName(), typeMember.getElementName(),
                          getResourcePathLabel(resourcePath),});
                  result.addWarning(message, DartStatusContext.create(typeMember));
                }
                // add error for shadowing usage
                {
                  List<SearchMatch> memberRefs = RenameAnalyzeUtil.getReferences(typeMember, null);
                  for (SearchMatch memberRef : memberRefs) {
                    DartElement enclosingRefElement = memberRef.getElement();
                    if (enclosingRefElement != null) {
                      Type enclosingRefType = enclosingRefElement.getAncestor(Type.class);
                      // TypeMember of this type shadows top-level element usage
                      if (Objects.equal(enclosingRefType, type)) {
                        IPath resourcePath = getElementPath(unitElement);
                        String message = Messages.format(
                            RefactoringCoreMessages.RenameProcessor_elementUsage_shadowedBy_typeMember,
                            new Object[] {
                                elementTypeName, RenameAnalyzeUtil.getElementTypeName(typeMember),
                                type.getElementName(), typeMember.getElementName(),
                                getResourcePathLabel(resourcePath),});
                        result.addError(message, DartStatusContext.create(memberRef));
                      }
                      // top-level element shadows TypeMember usage in sub-class
                      // http://code.google.com/p/dart/issues/detail?id=1180
                      if (!memberRef.isQualified()) {
                        if (RenameAnalyzeUtil.isTypeHierarchy(enclosingRefType, type)) {
                          IPath resourcePath = getElementPath(unitElement);
                          String message = Messages.format(
                              RefactoringCoreMessages.RenameProcessor_typeMemberUsage_shadowedBy_element,
                              new Object[] {
                                  RenameAnalyzeUtil.getElementTypeName(typeMember),
                                  type.getElementName(), typeMember.getElementName(),
                                  getResourcePathLabel(resourcePath), elementTypeName});
                          result.addError(message, DartStatusContext.create(memberRef));
                        }
                      }
                    }
                  }
                }
                // done
                return result;
              }
              // analyze Method
              if (typeMember instanceof Method) {
                Method method = (Method) typeMember;
                // visit local variables (and parameters)
                List<FunctionLocalElement> localVariables = RenameAnalyzeUtil.getFunctionLocalElements(method);
                for (FunctionLocalElement variable : localVariables) {
                  if (Objects.equal(variable.getElementName(), newName)) {
                    IPath resourcePath = getElementPath(unitElement);
                    CompilationUnitElement variableElement = variable.getElement();
                    // warning for shadowing declaration
                    {
                      String message = Messages.format(
                          RefactoringCoreMessages.RenameProcessor_elementDecl_shadowedBy_variable_inMethod,
                          new Object[] {
                              elementTypeName,
                              RenameAnalyzeUtil.getElementTypeName(variableElement),
                              type.getElementName(), method.getElementName(),
                              getResourcePathLabel(resourcePath),});
                      result.addWarning(message, DartStatusContext.create(variableElement));
                    }
                    // error for shadowing usage
                    for (SearchMatch match : references) {
                      if (SourceRangeUtils.contains(
                          variable.getVisibleRange(),
                          match.getSourceRange().getOffset())) {
                        String message = Messages.format(
                            RefactoringCoreMessages.RenameProcessor_elementUsage_shadowedBy_variable_inMethod,
                            new Object[] {
                                elementTypeName,
                                RenameAnalyzeUtil.getElementTypeName(variableElement),
                                type.getElementName(), method.getElementName(),
                                getResourcePathLabel(resourcePath),});
                        result.addError(message, DartStatusContext.create(match));
                      }
                    }
                  }
                }
              }
            }
          }
          // analyze Function
          if (unitElement instanceof DartFunction) {
            DartFunction function = (DartFunction) unitElement;
            // visit local variables (and parameters)
            List<FunctionLocalElement> localVariables = RenameAnalyzeUtil.getFunctionLocalElements(function);
            for (FunctionLocalElement variable : localVariables) {
              if (Objects.equal(variable.getElementName(), newName)) {
                IPath resourcePath = getElementPath(unitElement);
                CompilationUnitElement variableElement = variable.getElement();
                // warning for shadowing declaration
                {
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameProcessor_elementDecl_shadowedBy_variable_inFunction,
                      new Object[] {
                          elementTypeName, RenameAnalyzeUtil.getElementTypeName(variableElement),
                          function.getElementName(), getResourcePathLabel(resourcePath),});
                  result.addWarning(message, DartStatusContext.create(variableElement));
                }
                // error for shadowing usage
                for (SearchMatch match : references) {
                  if (SourceRangeUtils.contains(
                      variable.getVisibleRange(),
                      match.getSourceRange().getOffset())) {
                    String message = Messages.format(
                        RefactoringCoreMessages.RenameProcessor_elementUsage_shadowedBy_variable_inFunction,
                        new Object[] {
                            elementTypeName, RenameAnalyzeUtil.getElementTypeName(variableElement),
                            function.getElementName(), getResourcePathLabel(resourcePath),});
                    result.addError(message, DartStatusContext.create(match));
                  }
                }
              }
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * @return the actual {@link IPath} of underlying {@link IResource} or <code>null</code>.
   */
  private static IPath getElementPath(DartElement element) {
    IResource resource = element.getResource();
    if (resource != null) {
      return resource.getFullPath();
    } else {
      return null;
    }
  }

  /**
   * @return the label of a path, "unknown" if <code>null</code> given.
   */
  private static String getResourcePathLabel(IPath resourcePath) {
    if (resourcePath != null) {
      return BasicElementLabels.getPathLabel(resourcePath, false);
    }
    return "<unknown>";
  }

  private static void reportTopLevelAlreadyDeclared(RefactoringStatus result, String newName,
      DartLibrary library, CompilationUnitElement existingElement) {
    IPath libraryPath = library.getResource().getFullPath();
    IPath resourcePath = existingElement.getResource().getFullPath();
    String message = Messages.format(
        RefactoringCoreMessages.RenameTopProcessor_shadow_topLevel,
        new Object[] {
            getResourcePathLabel(resourcePath), getResourcePathLabel(libraryPath),
            RenameAnalyzeUtil.getElementTypeName(existingElement), newName});
    result.addError(message, DartStatusContext.create(existingElement));
  }

  private final CompilationUnitElement element;

  private final SourceReference elementSourceReference;
  protected final String oldName;

  private final TextChangeManager changeManager = new TextChangeManager(true);

  private List<SearchMatch> references;

  /**
   * @param element the {@link CompilationUnitElement} to rename, should also implement
   *          {@link SourceReference}, not <code>null</code>.
   */
  public RenameTopLevelProcessor(CompilationUnitElement element) {
    this.element = element;
    this.elementSourceReference = (SourceReference) element;
    oldName = element.getElementName();
    setNewElementName(StringUtils.defaultString(oldName));
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    RefactoringStatus result = Checks.checkIfCuBroken(element);
    result.merge(RenameAnalyzeUtil.checkLocalElement(element));
    return result;
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();

    if (Checks.isAlreadyNamed(element, newName)) {
      result.addFatalError(
          RefactoringCoreMessages.RenameProcessor_another_name,
          DartStatusContext.create(element));
      return result;
    }

    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameProcessor_checking, 1);
    try {
      List<Change> changesList = Lists.<Change> newArrayList();
      // add unit changes
      Change[] renameUnitChanges = changeManager.getAllChanges();
      Collections.addAll(changesList, renameUnitChanges);
      // additional changes
      changesList.addAll(contributeAdditionalChanges());
      // wrap into CompositeChange
      Change[] changesArray = changesList.toArray(new Change[changesList.size()]);
      return new CompositeChange(getProcessorName(), changesArray);
    } finally {
      monitor.done();
    }
  }

  @Override
  public final String getCurrentElementName() {
    return element.getElementName();
  }

  @Override
  public Object[] getElements() {
    return new Object[] {element};
  }

  @Override
  public int getSaveMode() {
    return RefactoringSaveHelper.SAVE_ALL;
  }

  protected void addDeclarationUpdate() throws CoreException {
    SourceRange nameRange = elementSourceReference.getNameRange();
    CompilationUnit cu = element.getCompilationUnit();
    String editName = RefactoringCoreMessages.RenameProcessor_update_declaration;
    addTextEdit(cu, editName, createTextChange(nameRange));
  }

  protected void addReferenceUpdate(SearchMatch match) {
    String editName = RefactoringCoreMessages.RenameProcessor_update_reference;
    CompilationUnit cu = match.getElement().getAncestor(CompilationUnit.class);
    SourceRange matchRange = match.getSourceRange();
    addTextEdit(cu, editName, createTextChange(matchRange));
  }

  protected void addReferenceUpdates(IProgressMonitor pm) throws DartModelException {
    pm.beginTask("", references.size()); //$NON-NLS-1$
    for (SearchMatch match : references) {
      addReferenceUpdate(match);
      pm.worked(1);
    }
  }

  protected final void addTextEdit(CompilationUnit unit, String groupName, TextEdit textEdit) {
    if (unit.getResource() != null) {
      TextChange change = changeManager.get(unit);
      TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
    }
  }

  /**
   * @return additional {@link Change} to apply during rename, may be empty {@link List}.
   */
  protected List<Change> contributeAdditionalChanges() {
    return ImmutableList.of();
  }

  protected final TextEdit createTextChange(SourceRange sourceRange) {
    return new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), getNewElementName());
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
      if (result.hasFatalError()) {
        return result;
      }
      // prepare references
      pm.setTaskName(RefactoringCoreMessages.RenameProcessor_searching);
      references = RenameAnalyzeUtil.getReferences(element, new SubProgressMonitor(pm, 3));
      pm.setTaskName(RefactoringCoreMessages.RenameProcessor_checking);
      // analyze affected units (such as warn about existing compilation errors)
      result.merge(RenameAnalyzeUtil.checkReferencesSource(references, oldName));
      result.merge(analyzeAffectedCompilationUnits());
      // check for possible conflicts
      result.merge(analyzePossibleConflicts(new SubProgressMonitor(pm, 10)));
      // OK, create changes
      createChanges(new SubProgressMonitor(pm, 5));
      return result;
    } finally {
      pm.done();
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
    // check for making private
    result.merge(RenameAnalyzeUtil.checkBecomePrivate(oldName, newName, element, references));
    pm.worked(1);
    // analyze conflicts
    result.merge(analyzePossibleConflicts(
        element.getAncestor(DartLibrary.class),
        element.getElementType(),
        element instanceof DartImport,
        references,
        newName));
    pm.worked(2);
    // done
    pm.done();
    return result;
  }

  private void createChanges(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.RenameProcessor_checking, 10);
    changeManager.clear();
    // update declaration
    addDeclarationUpdate();
    pm.worked(1);
    // update references
    addReferenceUpdates(new SubProgressMonitor(pm, 9));
    pm.done();
  }
}
