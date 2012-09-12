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
import com.google.dart.compiler.ast.ASTNodes;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.SourceRange;
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
 * {@link DartRenameProcessor} for {@link DartVariableDeclaration}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameLocalVariableProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameLocalVariableProcessor"; //$NON-NLS-1$

  public static RefactoringStatus analyzePossibleConflicts(FunctionLocalElement variable,
      String newName, IProgressMonitor pm) throws CoreException {
    CompilationUnitElement variableElement = variable.getElement();
    pm.beginTask("Analyze possible conflicts", 3);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // analyze variables in same function
      {
        DartFunction enclosingFunction = variableElement.getAncestor(DartFunction.class);
        if (enclosingFunction != null) {
          List<FunctionLocalElement> otherVariables = RenameAnalyzeUtil.getFunctionLocalElements(enclosingFunction);
          for (FunctionLocalElement otherVariable : otherVariables) {
            if (Objects.equal(otherVariable.getElementName(), newName)
                && SourceRangeUtils.intersects(
                    otherVariable.getVisibleRange(),
                    variable.getVisibleRange())) {
              CompilationUnitElement otherElement = otherVariable.getElement();
              String message = Messages.format(
                  RefactoringCoreMessages.RenameLocalVariableProcessor_shadow_variable,
                  new Object[] {RenameAnalyzeUtil.getElementTypeName(otherElement), newName});
              result.addError(message, DartStatusContext.create(otherElement));
              return result;
            }
          }
        }
      }
      // analyze supertypes
      pm.subTask("Analyze supertypes");
      {
        Type enclosingType = variableElement.getAncestor(Type.class);
        if (enclosingType != null) {
          Set<Type> enclosingAndSuperTypes = RenameAnalyzeUtil.getSuperTypes(enclosingType);
          enclosingAndSuperTypes.add(enclosingType);
          for (Type superType : enclosingAndSuperTypes) {
            IPath resourcePath = superType.getResource().getFullPath();
            // TypeParameter shadowed by variable
            if (superType == enclosingType) {
              DartTypeParameter[] typeParameters = superType.getTypeParameters();
              for (DartTypeParameter parameter : typeParameters) {
                if (Objects.equal(parameter.getElementName(), newName)) {
                  // add warning for shadowing TypeParameter declaration
                  {
                    String message = Messages.format(
                        RefactoringCoreMessages.RenameProcessor_typeMemberDecl_shadowedBy_element,
                        new Object[] {
                            RenameAnalyzeUtil.getElementTypeName(parameter),
                            superType.getElementName(), newName, resourcePath,
                            RenameAnalyzeUtil.getElementTypeName(variableElement),});
                    result.addWarning(message, DartStatusContext.create(parameter));
                  }
                  // add error for shadowing TypeParameter usage
                  List<SourceRange> parameterReferences = RenameAnalyzeUtil.getReferences(parameter);
                  for (SourceRange parameterReference : parameterReferences) {
                    if (parameterReference.getOffset() != parameter.getNameRange().getOffset()
                        && SourceRangeUtils.intersects(
                            parameterReference,
                            superType.getSourceRange())) {
                      String message = Messages.format(
                          RefactoringCoreMessages.RenameProcessor_typeMemberUsage_shadowedBy_element,
                          new Object[] {
                              RenameAnalyzeUtil.getElementTypeName(parameter),
                              superType.getElementName(), newName, resourcePath,
                              RenameAnalyzeUtil.getElementTypeName(variableElement),});
                      result.addError(message, DartStatusContext.create(
                          superType.getCompilationUnit(),
                          parameterReference));
                    }
                  }
                }
              }
            }
            // analyze type members
            TypeMember[] superMembers = superType.getExistingMembers(newName);
            for (TypeMember superMember : superMembers) {
              // add warning for shadowing member declaration
              {
                String message = Messages.format(
                    RefactoringCoreMessages.RenameProcessor_typeMemberDecl_shadowedBy_element,
                    new Object[] {
                        RenameAnalyzeUtil.getElementTypeName(superMember),
                        superType.getElementName(), superMember.getElementName(), resourcePath,
                        RenameAnalyzeUtil.getElementTypeName(variableElement)});
                result.addWarning(message, DartStatusContext.create(superMember));
              }
              // add error for shadowing member usage
              List<SearchMatch> memberRefs = RenameAnalyzeUtil.getReferences(superMember, null);
              for (SearchMatch memberRef : memberRefs) {
                if (SourceRangeUtils.intersects(
                    memberRef.getSourceRange(),
                    variable.getVisibleRange())) {
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameProcessor_typeMemberUsage_shadowedBy_element,
                      new Object[] {
                          RenameAnalyzeUtil.getElementTypeName(superMember),
                          superType.getElementName(), superMember.getElementName(), resourcePath,
                          RenameAnalyzeUtil.getElementTypeName(variableElement)});
                  result.addError(message, DartStatusContext.create(memberRef));
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
            variableElement,
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
                    RenameAnalyzeUtil.getElementTypeName(topLevelElement), newName,
                    BasicElementLabels.getPathLabel(resourcePath, false),
                    BasicElementLabels.getPathLabel(libraryPath, false),
                    RenameAnalyzeUtil.getElementTypeName(variableElement)});
            result.addWarning(message, DartStatusContext.create(topLevelElement));
          }
          // add error for shadowing element usage
          List<SearchMatch> refs = RenameAnalyzeUtil.getReferences(topLevelElement, null);
          for (SearchMatch ref : refs) {
            if (SourceRangeUtils.intersects(ref.getSourceRange(), variable.getVisibleRange())) {
              String message = Messages.format(
                  RefactoringCoreMessages.RenameProcessor_topLevelUsage_shadowedBy_element,
                  new Object[] {
                      RenameAnalyzeUtil.getElementTypeName(topLevelElement), newName,
                      BasicElementLabels.getPathLabel(resourcePath, false),
                      BasicElementLabels.getPathLabel(libraryPath, false),
                      RenameAnalyzeUtil.getElementTypeName(variableElement)});
              result.addError(message, DartStatusContext.create(ref));
            }
          }
        }
      }
      // OK
      return result;
    } finally {
      pm.done();
    }
  }

  private final DartVariableDeclaration variable;

  private final CompilationUnit unit;
  private final String oldName;
  private String newName;
  private DartUnit unitNode;
  private DartNode variableNode;
  private VariableElement variableElement;

  private CompilationUnitChange change;

  /**
   * @param variable the local variable, not <code>null</code>.
   */
  public RenameLocalVariableProcessor(DartVariableDeclaration variable) {
    this.variable = variable;
    this.unit = variable.getAncestor(CompilationUnit.class);
    this.oldName = variable.getElementName();
    this.newName = oldName;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    initAST();
    // we should be able to find variable Node and Element
    if (variableElement == null) {
      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameLocalVariableProcessor_must_be_selected);
    }
    // OK
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws DartModelException {
    RefactoringStatus result = Checks.checkVariableName(newName);
    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameTypeProcessor_creating_changes, 1);
    try {
      return change;
    } finally {
      monitor.done();
    }
  }

  @Override
  public String getCurrentElementName() {
    return oldName;
  }

  @Override
  public Object[] getElements() {
    return new Object[] {variable};
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return null;
  }

  @Override
  public String getNewElementName() {
    return newName;
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameLocalVariableProcessor_name;
  }

  @Override
  public int getSaveMode() {
    return RefactoringSaveHelper.SAVE_NOTHING;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(variable);
  }

  @Override
  public void setNewElementName(String newName) {
    Assert.isNotNull(newName);
    this.newName = newName;
  }

  @Override
  protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException, OperationCanceledException {
    try {
      pm.beginTask("", 11); //$NON-NLS-1$
      // check new name
      RefactoringStatus result = checkNewElementName(newName);
      if (result.hasFatalError()) {
        return result;
      }
      pm.worked(1);
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
    return analyzePossibleConflicts(new FunctionLocalElement(variable), newName, pm);
  }

  private void createEdits() throws CoreException {
    List<TextEdit> allRenameEdits = Lists.newArrayList();
    allRenameEdits.addAll(getLocalRenameEdits());
    allRenameEdits.addAll(getNamedParameterRenameEdits());

    change = new CompilationUnitChange(
        RefactoringCoreMessages.RenameLocalVariableProcessor_name,
        unit);
    MultiTextEdit rootEdit = new MultiTextEdit();
    change.setEdit(rootEdit);
    change.setKeepPreviewEdits(true);

    for (TextEdit edit : allRenameEdits) {
      rootEdit.addChild(edit);
      change.addTextEditGroup(new TextEditGroup(
          RefactoringCoreMessages.RenameLocalVariableProcessor_update_reference,
          edit));
    }
  }

  private TextEdit createRenameEdit(int offset) {
    return new ReplaceEdit(offset, oldName.length(), newName);
  }

  private List<TextEdit> getLocalRenameEdits() {
    final List<TextEdit> edits = Lists.newArrayList();
    DartNode enclosingMethod = ASTNodes.getParent(variableNode, DartMethodDefinition.class);
    enclosingMethod.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitIdentifier(DartIdentifier node) {
        if (node.getElement() == variableElement) {
          int offset = node.getSourceInfo().getOffset();
          TextEdit edit = createRenameEdit(offset);
          edits.add(edit);
        }
        return null;
      }
    });
    return edits;
  }

  private List<TextEdit> getNamedParameterRenameEdits() throws CoreException {
    List<TextEdit> edits = Lists.newArrayList();
    if (variable.isParameter()) {
      List<SearchMatch> references = RenameAnalyzeUtil.getReferences(variable, null);
      for (SearchMatch match : references) {
        int offset = match.getSourceRange().getOffset();
        TextEdit edit = createRenameEdit(offset);
        edits.add(edit);
      }
    }
    return edits;
  }

  private void initAST() throws DartModelException {
    unitNode = DartCompilerUtilities.resolveUnit(unit);
    // Prepare variable Node.
    SourceRange sourceRange = variable.getNameRange();
    variableNode = NodeFinder.perform(unitNode, sourceRange);
    if (variableNode == null) {
      return;
    }
    // prepare variable Element.
    if (variableNode.getElement() instanceof VariableElement) {
      variableElement = (VariableElement) variableNode.getElement();
    }
  }
}
