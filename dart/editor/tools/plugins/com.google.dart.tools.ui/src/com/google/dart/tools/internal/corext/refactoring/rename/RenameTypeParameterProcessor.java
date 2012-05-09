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
import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.resolver.TypeVariableElement;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.List;
import java.util.Set;

/**
 * {@link DartRenameProcessor} for {@link DartTypeParameter}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameTypeParameterProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameTypeParameterProcessor"; //$NON-NLS-1$

  private final DartTypeParameter parameter;
  private final CompilationUnit unit;
  private final String oldName;

  private String newName;
  private DartUnit unitNode;
  private DartNode parameterNode;
  private TypeVariableElement parameterElement;
  private final List<SourceRange> references = Lists.newArrayList();
  private CompilationUnitChange change;

  /**
   * @param parameter the {@link DartTypeParameter} to rename, not <code>null</code>.
   */
  public RenameTypeParameterProcessor(DartTypeParameter parameter) {
    this.parameter = parameter;
    this.unit = parameter.getAncestor(CompilationUnit.class);
    oldName = parameter.getElementName();
    newName = oldName;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    initAST();
    // we should be able to find Node and Element
    if (parameterElement == null) {
      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTypeParameterProcessor_must_be_selected);
    }
    // OK
    return Checks.checkIfCuBroken(parameter);
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkTypeParameterName(newName);

    if (Checks.isAlreadyNamed(parameter, newName)) {
      result.addFatalError(
          RefactoringCoreMessages.RenameProcessor_another_name,
          DartStatusContext.create(parameter));
      return result;
    }

    // can not have two type parameters with same name
    for (DartTypeParameter typeParameter : getTypeParameters()) {
      if (Objects.equal(typeParameter.getElementName(), newName)) {
        String message = Messages.format(
            RefactoringCoreMessages.RenameTypeParameterProcessor_typeParameter_already_defined,
            new Object[] {newName});
        result.addError(message, DartStatusContext.create(typeParameter));
      }
    }

    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameProcessor_checking, 1);
    try {
      return change;
    } finally {
      monitor.done();
    }
  }

  @Override
  public final String getCurrentElementName() {
    return parameter.getElementName();
  }

  @Override
  public Object[] getElements() {
    return new Object[] {parameter};
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() throws CoreException {
    DartTypeParameter newParameter = null;
    for (DartTypeParameter parameter : getTypeParameters()) {
      if (parameter.getElementName().equals(newName)) {
        newParameter = parameter;
      }
    }
    return newParameter;
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameTypeParameterProcessor_name;
  }

  @Override
  public int getSaveMode() {
    return RefactoringSaveHelper.SAVE_ALL;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(parameter);
  }

  @Override
  public void setNewElementName(String newName) {
    Assert.isNotNull(newName);
    this.newName = newName;
  }

  @Override
  protected RefactoringStatus doCheckFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException, OperationCanceledException {
    try {
      pm.beginTask("", 12); //$NON-NLS-1$
      // check new name
      RefactoringStatus result = checkNewElementName(newName);
      if (result.hasFatalError()) {
        return result;
      }
      pm.worked(1);
      // prepare references
      prepareReferences();
      pm.worked(1);
      // analyze affected units (such as warn about existing compilation errors)
      Checks.checkCompileErrorsInAffectedFile(result, unit.getUnderlyingResource());
      // check for possible conflicts
      result.merge(analyzePossibleConflicts(new SubProgressMonitor(pm, 10)));
      // OK, create changes
      createEdits();
      return result;
    } finally {
      pm.done();
    }
  }

  private RefactoringStatus analyzePossibleConflicts(IProgressMonitor pm) throws CoreException {
    pm.beginTask("Analyze possible conflicts", 3);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // prepare parameter visibility SourceRange
      SourceRange parameterSourceRange;
      DartElement parentElement = parameter;
      {
        while (!(parentElement.getParent() instanceof CompilationUnit)) {
          parentElement = parentElement.getParent();
        }
        parameterSourceRange = ((SourceReference) parentElement).getSourceRange();
      }
      // analyze supertypes
      pm.subTask("Analyze supertypes");
      {
        Type enclosingType = parameter.getAncestor(Type.class);
        if (enclosingType != null) {
          Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(enclosingType);
          superTypes.add(enclosingType);
          for (Type superType : superTypes) {
            boolean isEnclosingType = superType.equals(enclosingType);
            TypeMember[] superMembers = superType.getExistingMembers(newName);
            for (TypeMember superMember : superMembers) {
              IPath resourcePath = superMember.getResource().getFullPath();
              // enclosing TypeMember shadows TypeParameter
              if (isEnclosingType) {
                // add warning for shadowing declaration
                {
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameProcessor_elementDecl_shadowedBy_typeMember,
                      new Object[] {
                          RenameAnalyzeUtil.getElementTypeName(parameter),
                          RenameAnalyzeUtil.getElementTypeName(superMember),
                          superType.getElementName(),
                          superMember.getElementName(),
                          resourcePath,});
                  result.addWarning(message, DartStatusContext.create(superMember));
                }
                // add errors for shadowing usage
                for (SourceRange reference : references) {
                  if (reference.getOffset() != parameter.getNameRange().getOffset()) {
                    String message = Messages.format(
                        RefactoringCoreMessages.RenameProcessor_elementUsage_shadowedBy_typeMember,
                        new Object[] {
                            RenameAnalyzeUtil.getElementTypeName(parameter),
                            RenameAnalyzeUtil.getElementTypeName(superMember),
                            superType.getElementName(),
                            superMember.getElementName(),
                            resourcePath,});
                    result.addError(message, DartStatusContext.create(unit, reference));
                  }
                }
              }
              // TypeParameter shadows Super members
              if (!isEnclosingType) {
                // add warning for shadowing member declaration
                {
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameProcessor_typeMemberDecl_shadowedBy_element,
                      new Object[] {
                          RenameAnalyzeUtil.getElementTypeName(superMember),
                          superType.getElementName(),
                          superMember.getElementName(),
                          resourcePath,
                          RenameAnalyzeUtil.getElementTypeName(parameter)});
                  result.addWarning(message, DartStatusContext.create(superMember));
                }
                // add error for shadowing member usage
                {
                  List<SearchMatch> memberRefs = RenameAnalyzeUtil.getReferences(superMember, null);
                  for (SearchMatch memberRef : memberRefs) {
                    if (SourceRangeUtils.intersects(
                        memberRef.getSourceRange(),
                        parameterSourceRange)) {
                      String message = Messages.format(
                          RefactoringCoreMessages.RenameProcessor_typeMemberUsage_shadowedBy_element,
                          new Object[] {
                              RenameAnalyzeUtil.getElementTypeName(superMember),
                              superType.getElementName(),
                              superMember.getElementName(),
                              resourcePath,
                              RenameAnalyzeUtil.getElementTypeName(parameter)});
                      result.addError(message, DartStatusContext.create(memberRef));
                    }
                  }
                }
              }
            }
            // analyze variables
            for (Method method : superType.getMethods()) {
              List<FunctionLocalElement> localVariables = RenameAnalyzeUtil.getFunctionLocalElements(method);
              for (FunctionLocalElement variable : localVariables) {
                if (variable.getElementName().equals(newName)) {
                  IPath resourcePath = method.getResource().getFullPath();
                  CompilationUnitElement variableElement = variable.getElement();
                  // add warning for hiding TypeParameter declaration
                  {
                    String message = Messages.format(
                        RefactoringCoreMessages.RenameProcessor_elementDecl_shadowedBy_variable_inMethod,
                        new Object[] {
                            RenameAnalyzeUtil.getElementTypeName(parameter),
                            RenameAnalyzeUtil.getElementTypeName(variableElement),
                            enclosingType.getElementName(),
                            method.getElementName(),
                            BasicElementLabels.getPathLabel(resourcePath, false)});
                    result.addWarning(message, DartStatusContext.create(variableElement));
                  }
                  // add error for hiding TypeParameter usage
                  for (SourceRange reference : references) {
                    if (SourceRangeUtils.intersects(reference, variable.getVisibleRange())) {
                      String message = Messages.format(
                          RefactoringCoreMessages.RenameProcessor_elementUsage_shadowedBy_variable_inMethod,
                          new Object[] {
                              RenameAnalyzeUtil.getElementTypeName(parameter),
                              RenameAnalyzeUtil.getElementTypeName(variableElement),
                              enclosingType.getElementName(),
                              method.getElementName(),
                              BasicElementLabels.getPathLabel(resourcePath, false)});
                      result.addError(message, DartStatusContext.create(unit, reference));
                    }
                  }
                }
              }
            }
          }
        }
      }
      pm.worked(1);
      // analyze top-level elements
      pm.subTask("Analyze top-level elements");
      {
        CompilationUnitElement topLevelElement = RenameAnalyzeUtil.getTopLevelElementNamed(
            parameter,
            newName);
        if (topLevelElement != null) {
          DartLibrary shadowLibrary = topLevelElement.getAncestor(DartLibrary.class);
          IPath libraryPath = shadowLibrary.getResource().getFullPath();
          IPath resourcePath = topLevelElement.getResource().getFullPath();
          // add warning for shadowing element declaration
          {
            String message = Messages.format(
                RefactoringCoreMessages.RenameProcessor_topLevelDecl_shadowedBy_element,
                new Object[] {
                    RenameAnalyzeUtil.getElementTypeName(topLevelElement),
                    newName,
                    BasicElementLabels.getPathLabel(resourcePath, false),
                    BasicElementLabels.getPathLabel(libraryPath, false),
                    RenameAnalyzeUtil.getElementTypeName(parameter)});
            result.addWarning(message, DartStatusContext.create(topLevelElement));
          }
          // add error for shadowing element usage
          List<SearchMatch> refs = RenameAnalyzeUtil.getReferences(topLevelElement, null);
          for (SearchMatch ref : refs) {
            if (SourceRangeUtils.intersects(ref.getSourceRange(), parameterSourceRange)) {
              String message = Messages.format(
                  RefactoringCoreMessages.RenameProcessor_topLevelUsage_shadowedBy_element,
                  new Object[] {
                      RenameAnalyzeUtil.getElementTypeName(topLevelElement),
                      newName,
                      BasicElementLabels.getPathLabel(resourcePath, false),
                      BasicElementLabels.getPathLabel(libraryPath, false),
                      RenameAnalyzeUtil.getElementTypeName(parameter)});
              result.addError(message, DartStatusContext.create(ref));
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

  private void createEdits() {
    change = new CompilationUnitChange(
        RefactoringCoreMessages.RenameTypeParameterProcessor_name,
        unit);
    MultiTextEdit rootEdit = new MultiTextEdit();
    change.setEdit(rootEdit);
    change.setKeepPreviewEdits(true);

    for (SourceRange reference : references) {
      int offset = reference.getOffset();
      int length = reference.getLength();
      TextEdit edit = new ReplaceEdit(offset, length, newName);
      change.addEdit(edit);
      String editName = offset == parameterNode.getSourceInfo().getOffset()
          ? RefactoringCoreMessages.RenameProcessor_update_declaration
          : RefactoringCoreMessages.RenameProcessor_update_reference;
      change.addTextEditGroup(new TextEditGroup(editName, edit));
    }
  }

  private DartTypeParameter[] getTypeParameters() throws DartModelException {
    if (parameter.getParent() instanceof Type) {
      return ((Type) parameter.getParent()).getTypeParameters();
    } else {
      return ((DartFunctionTypeAlias) parameter.getParent()).getTypeParameters();
    }
  }

  private void initAST() throws DartModelException {
    unitNode = DartCompilerUtilities.resolveUnit(unit);
    // prepare Node
    SourceRange sourceRange = parameter.getNameRange();
    parameterNode = NodeFinder.perform(unitNode, sourceRange);
    if (parameterNode == null) {
      return;
    }
    // prepare Element
    if (parameterNode.getElement() instanceof TypeVariableElement) {
      parameterElement = (TypeVariableElement) parameterNode.getElement();
    }
  }

  private void prepareReferences() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitIdentifier(DartIdentifier node) {
        if (node.getElement() == parameterElement) {
          SourceInfo sourceInfo = node.getSourceInfo();
          int offset = sourceInfo.getOffset();
          int length = sourceInfo.getLength();
          references.add(new SourceRangeImpl(offset, length));
        }
        return null;
      }
    });
  }
}
