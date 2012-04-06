package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.dom.ASTNodes;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.participants.DartProcessors;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.ResourceUtil;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.List;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameLocalVariableProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameLocalVariableProcessor"; //$NON-NLS-1$

  private final DartVariableDeclaration variable;
  private final CompilationUnit unit;

  private final String oldName;
  private String newName;
  private DartUnit unitNode;
  private DartNode variableNode;
  private VariableElement variableElement;
  private CompilationUnitChange change;

  /**
   * Creates a new rename local variable processor.
   * 
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
      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTempRefactoring_must_select_local);
    }
    // OK
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws DartModelException {
    RefactoringStatus result = Checks.checkVariableName(newName);
    if (!Checks.startsWithLowerCase(newName)) {
      result.addWarning(RefactoringCoreMessages.RenameTempRefactoring_lowercase);
    }
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
    return RefactoringCoreMessages.RenameTempRefactoring_rename;
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
  protected RenameModifications computeRenameModifications() throws CoreException {
    RenameModifications result = new RenameModifications();
    result.rename(variable, new RenameArguments(getNewElementName(), true));
    return result;
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
      // check for possible conflicts
      result.merge(analyzePossibleConflicts(new SubProgressMonitor(pm, 10)));
      if (result.hasFatalError()) {
        return result;
      }
      // OK, create changes
      createEdits();
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  protected final String[] getAffectedProjectNatures() throws CoreException {
    return DartProcessors.computeAffectedNatures(variable);
  }

  @Override
  protected IFile[] getChangedFiles() throws CoreException {
    return new IFile[] {ResourceUtil.getFile(unit)};
  }

  private RefactoringStatus analyzePossibleConflicts(IProgressMonitor pm) throws CoreException {
    pm.beginTask("Analyze possible conflicts", 3);
    try {
      RefactoringStatus result = new RefactoringStatus();
      Type enclosingType = variable.getAncestor(Type.class);
      // analyze variables in same function
      {
        DartFunction enclosingFunction = variable.getAncestor(DartFunction.class);
        if (enclosingFunction != null) {
          DartVariableDeclaration[] localVariables = enclosingFunction.getLocalVariables();
          for (DartVariableDeclaration otherVariable : localVariables) {
            if (Objects.equal(otherVariable.getElementName(), newName)
                && SourceRangeUtils.intersects(
                    otherVariable.getVisibleRange(),
                    variable.getVisibleRange())) {
              String message = Messages.format(
                  RefactoringCoreMessages.RenameLocalVariableProcessor_shadow_variable,
                  newName);
              result.addFatalError(message, DartStatusContext.create(otherVariable));
              return result;
            }
          }
        }
      }
      // analyze supertypes
      pm.subTask("Analyze supertypes");
      RenameAnalyzeUtil.checkShadow_superType_member(
          result,
          enclosingType,
          newName,
          RefactoringCoreMessages.RenameLocalVariableProcessor_shadow_superType_member);
      pm.worked(1);
      if (result.hasFatalError()) {
        return result;
      }
      // analyze top-level elements
      pm.subTask("Analyze top-level elements");
      {
        CompilationUnitElement topLevelElement = RenameAnalyzeUtil.getTopLevelElementNamed(
            Sets.<DartLibrary>newHashSet(),
            variable,
            newName);
        if (topLevelElement != null) {
          DartLibrary shadowLibrary = topLevelElement.getAncestor(DartLibrary.class);
          IPath libraryPath = shadowLibrary.getResource().getFullPath();
          IPath resourcePath = topLevelElement.getResource().getFullPath();
          String message = Messages.format(
              RefactoringCoreMessages.RenameLocalVariableProcessor_shadow_topLevel,
              new Object[] {
                  BasicElementLabels.getPathLabel(resourcePath, false),
                  BasicElementLabels.getPathLabel(libraryPath, false),
                  newName});
          result.addFatalError(message, DartStatusContext.create(topLevelElement));
        }
      }
      // OK
      return result;
    } finally {
      pm.done();
    }
  }

  private void createEdits() {
    List<TextEdit> allRenameEdits = getAllRenameEdits();

    change = new CompilationUnitChange(RefactoringCoreMessages.RenameTempRefactoring_rename, unit);
    MultiTextEdit rootEdit = new MultiTextEdit();
    change.setEdit(rootEdit);
    change.setKeepPreviewEdits(true);

    for (TextEdit edit : allRenameEdits) {
      rootEdit.addChild(edit);
      change.addTextEditGroup(new TextEditGroup(
          RefactoringCoreMessages.RenameTempRefactoring_changeName,
          edit));
    }
  }

  private TextEdit createRenameEdit(int offset) {
    return new ReplaceEdit(offset, oldName.length(), newName);
  }

  private List<TextEdit> getAllRenameEdits() {
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
