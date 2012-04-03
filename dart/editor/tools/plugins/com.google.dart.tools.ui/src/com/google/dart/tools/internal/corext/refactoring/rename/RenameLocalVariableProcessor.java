package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.dom.ASTNodes;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.participants.DartProcessors;
import com.google.dart.tools.internal.corext.refactoring.util.ResourceUtil;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.List;

public class RenameLocalVariableProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameLocalVariableProcessor"; //$NON-NLS-1$

  private final DartVariableDeclaration fLocalVariable;
  private final CompilationUnit fCu;

  private String fCurrentName;
  private String fNewName;
  private DartUnit fCompilationUnitNode;
  private DartNode fVariableNode;
  private VariableElement fVariableElement;
  private CompilationUnitChange fChange;

  /**
   * Creates a new rename local variable processor.
   * 
   * @param localVariable the local variable, or <code>null</code> if invoked by scripting
   */
  public RenameLocalVariableProcessor(DartVariableDeclaration localVariable) {
    fLocalVariable = localVariable;
    fCu = localVariable != null ? localVariable.getAncestor(CompilationUnit.class) : null;
    fNewName = ""; //$NON-NLS-1$
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    initAST();
    // We should be able to find variable Node and Element.
    if (fVariableElement == null) {
      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTempRefactoring_must_select_local);
    }
    // TODO(scheglov) I don't understand reason for this check. Yes, we need to check that local
    // variable was selected. But may be not this way.
//    if (fTempDeclarationNode == null || fTempDeclarationNode.resolveBinding() == null) {
//      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTempRefactoring_must_select_local);
//    }
    // TODO(scheglov) Dart has no "initializers". Hm... But Dart has functions.
//    if (!Checks.isDeclaredIn(fTempDeclarationNode, DartMethodDefinition.class)
//        && !Checks.isDeclaredIn(fTempDeclarationNode, Initializer.class)) {
//      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.RenameTempRefactoring_only_in_methods_and_initializers);
//    }
    // OK
    fCurrentName = fVariableElement.getName();
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
      return fChange;
    } finally {
      monitor.done();
    }
  }

  @Override
  public String getCurrentElementName() {
    return fCurrentName;
  }

  @Override
  public Object[] getElements() {
    return new Object[] {fLocalVariable};
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
    return fNewName;
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
    return RefactoringAvailabilityTester.isRenameAvailable(fLocalVariable);
  }

  @Override
  public void setNewElementName(String newName) {
    Assert.isNotNull(newName);
    fNewName = newName;
  }

  @Override
  protected RenameModifications computeRenameModifications() throws CoreException {
    RenameModifications result = new RenameModifications();
    result.rename(fLocalVariable, new RenameArguments(getNewElementName(), true));
    return result;
  }

  @Override
  protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException, OperationCanceledException {
    try {
      pm.beginTask("", 1); //$NON-NLS-1$
      // Check new name.
      RefactoringStatus result = checkNewElementName(fNewName);
      if (result.hasFatalError()) {
        return result;
      }
      // Prepare changes.
      createEdits();
      // Check that no changes don't cause problems.
      result.merge(RenameAnalyzeUtil.analyzeLocalRenames(fChange, fCu));
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  protected final String[] getAffectedProjectNatures() throws CoreException {
    return DartProcessors.computeAffectedNatures(fLocalVariable);
  }

  @Override
  protected IFile[] getChangedFiles() throws CoreException {
    return new IFile[] {ResourceUtil.getFile(fCu)};
  }

  private void createEdits() {
    List<TextEdit> allRenameEdits = getAllRenameEdits();

    fChange = new CompilationUnitChange(RefactoringCoreMessages.RenameTempRefactoring_rename, fCu);
    MultiTextEdit rootEdit = new MultiTextEdit();
    fChange.setEdit(rootEdit);
    fChange.setKeepPreviewEdits(true);

    for (TextEdit edit : allRenameEdits) {
      rootEdit.addChild(edit);
      fChange.addTextEditGroup(new TextEditGroup(
          RefactoringCoreMessages.RenameTempRefactoring_changeName,
          edit));
    }
  }

  private TextEdit createRenameEdit(int offset) {
    return new ReplaceEdit(offset, fCurrentName.length(), fNewName);
  }

  private List<TextEdit> getAllRenameEdits() {
    final List<TextEdit> edits = Lists.newArrayList();
    DartNode enclosingMethod = ASTNodes.getParent(fVariableNode, DartMethodDefinition.class);
    enclosingMethod.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitIdentifier(DartIdentifier node) {
        if (node.getElement() == fVariableElement) {
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
    fCompilationUnitNode = DartCompilerUtilities.resolveUnit(fCu);
    // Prepare variable Node.
    SourceRange sourceRange = fLocalVariable.getNameRange();
    fVariableNode = NodeFinder.perform(fCompilationUnitNode, sourceRange);
    if (fVariableNode == null) {
      return;
    }
    // prepare variable Element.
    if (fVariableNode.getElement() instanceof VariableElement) {
      fVariableElement = (VariableElement) fVariableNode.getElement();
    }
  }
}
