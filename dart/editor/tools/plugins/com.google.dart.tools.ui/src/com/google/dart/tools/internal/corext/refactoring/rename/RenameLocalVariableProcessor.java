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
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil.LocalAnalyzePackage;
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

  //the following fields are set or modified after the construction
  private String fCurrentName;
  private String fNewName;
  private DartUnit fCompilationUnitNode;
  private DartNode fVariableNode;
  private VariableElement fVariableElement;
  private CompilationUnitChange fChange;

//  private boolean fIsComposite;
//  private GroupCategorySet fCategorySet;
//  private TextChangeManager fChangeManager;
  private RenameAnalyzeUtil.LocalAnalyzePackage fLocalAnalyzePackage;

//  public RenameLocalVariableProcessor(DartRefactoringArguments arguments, RefactoringStatus status) {
//    this(null);
//    RefactoringStatus initializeStatus = initialize(arguments);
//    status.merge(initializeStatus);
//  }

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

  /**
   * Creates a new rename local variable processor.
   * <p>
   * This constructor is only used by <code>RenameTypeProcessor</code>.
   * </p>
   * 
   * @param localVariable the local variable
   * @param manager the change manager
   * @param node the compilation unit node
   * @param categorySet the group category set
   */
//  RenameLocalVariableProcessor(DartVariableDeclaration localVariable, TextChangeManager manager,
//      DartUnit node, GroupCategorySet categorySet) {
//    this(localVariable);
//    fChangeManager = manager;
//    fCategorySet = categorySet;
//    fCompilationUnitNode = node;
//    fIsComposite = true;
//  }

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
    initNames();
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws DartModelException {
    RefactoringStatus result = Checks.checkVariableName(newName);
    if (!Checks.startsWithLowerCase(newName)) {
//      if (fIsComposite) {
//        final String nameOfParent = DartElementLabels.getElementLabel(fLocalVariable.getParent(),
//            DartElementLabels.ALL_DEFAULT);
//        final String nameOfType = DartElementLabels.getElementLabel(
//            fLocalVariable.getAncestor(Type.class), DartElementLabels.ALL_DEFAULT);
//        result.addWarning(Messages.format(RefactoringCoreMessages.RenameTempRefactoring_lowercase2,
//            new String[] {BasicElementLabels.getDartElementName(newName), nameOfParent, nameOfType}));
//      } else
      {
        result.addWarning(RefactoringCoreMessages.RenameTempRefactoring_lowercase);
      }
    }
    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameTypeProcessor_creating_changes, 1);
    try {
      // TODO(scheglov) I think that we can remove this feature in Editor at all
//      RenameDartElementDescriptor descriptor = createRefactoringDescriptor();
//      fChange.setDescriptor(new RefactoringChangeDescriptor(descriptor));
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

//  public RenameAnalyzeUtil.LocalAnalyzePackage getLocalAnalyzePackage() {
//    return fLocalAnalyzePackage;
//  }

  @Override
  public Object getNewElement() {
    return null; //cannot create an DartVariableDeclaration
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

      RefactoringStatus result = checkNewElementName(fNewName);
      if (result.hasFatalError()) {
        return result;
      }
      createEdits();
//      if (!fIsComposite)
      {
        LocalAnalyzePackage[] localAnalyzePackages = new RenameAnalyzeUtil.LocalAnalyzePackage[] {fLocalAnalyzePackage};
        result.merge(RenameAnalyzeUtil.analyzeLocalRenames(
            localAnalyzePackages,
            fChange,
            fCu,
            fCompilationUnitNode,
            true));
      }
      return result;
    } finally {
      pm.done();
//      if (fIsComposite) {
//        // end of life cycle for this processor
//        fChange = null;
//        fCompilationUnitNode = null;
//        fVariableElement = null;
//      }
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
    TextEdit declarationEdit = createRenameEdit(fVariableElement.getSourceInfo().getOffset());
    List<TextEdit> allRenameEdits = getAllRenameEdits(declarationEdit);

//    TextEdit[] allUnparentedRenameEdits = new TextEdit[allRenameEdits.length];
//    TextEdit unparentedDeclarationEdit = null;

    fChange = new CompilationUnitChange(RefactoringCoreMessages.RenameTempRefactoring_rename, fCu);
    MultiTextEdit rootEdit = new MultiTextEdit();
    fChange.setEdit(rootEdit);
    fChange.setKeepPreviewEdits(true);

    for (TextEdit edit : allRenameEdits) {
//      if (fIsComposite) {
//        // Add a copy of the text edit (text edit may only have one
//        // parent) to keep problem reporting code clean
//        TextChangeCompatibility.addTextEdit(fChangeManager.get(fCu),
//            RefactoringCoreMessages.RenameTempRefactoring_changeName, allRenameEdits[i].copy(),
//            fCategorySet);
//
//        // Add a separate copy for problem reporting
//        allUnparentedRenameEdits[i] = allRenameEdits[i].copy();
//        if (allRenameEdits[i].equals(declarationEdit)) {
//          unparentedDeclarationEdit = allUnparentedRenameEdits[i];
//        }
//      }
      rootEdit.addChild(edit);
      fChange.addTextEditGroup(new TextEditGroup(
          RefactoringCoreMessages.RenameTempRefactoring_changeName, edit));
    }

    // store information for analysis
//    if (fIsComposite) {
//      fLocalAnalyzePackage = new RenameAnalyzeUtil.LocalAnalyzePackage(unparentedDeclarationEdit,
//          allUnparentedRenameEdits);
//    } else
    {
      fLocalAnalyzePackage = new RenameAnalyzeUtil.LocalAnalyzePackage(declarationEdit,
          allRenameEdits);
    }
  }

//  private RenameDartElementDescriptor createRefactoringDescriptor() {
//    String project = null;
//    DartProject DartProject = fCu.getDartProject();
//    if (DartProject != null) {
//      project = DartProject.getElementName();
//    }
//    final String header = Messages.format(
//        RefactoringCoreMessages.RenameLocalVariableProcessor_descriptor_description,
//        new String[] {
//            BasicElementLabels.getDartElementName(fCurrentName),
//            DartElementLabels.getElementLabel(fLocalVariable.getParent(),
//                DartElementLabels.ALL_FULLY_QUALIFIED),
//            BasicElementLabels.getDartElementName(fNewName)});
//    final String description = Messages.format(
//        RefactoringCoreMessages.RenameLocalVariableProcessor_descriptor_description_short,
//        BasicElementLabels.getDartElementName(fCurrentName));
//    final String comment = new RefactoringDescriptorComment(project, this, header).asString();
//    final RenameDartElementDescriptor descriptor = RefactoringSignatureDescriptorFactory.createRenameDartElementDescriptor(IDartRefactorings.RENAME_LOCAL_VARIABLE);
//    descriptor.setProject(project);
//    descriptor.setDescription(description);
//    descriptor.setComment(comment);
//    descriptor.setFlags(RefactoringDescriptor.NONE);
//    descriptor.setJavaElement(fLocalVariable);
//    descriptor.setNewName(getNewElementName());
//    descriptor.setUpdateReferences(fUpdateReferences);
//    return descriptor;
//  }

  private TextEdit createRenameEdit(int offset) {
    return new ReplaceEdit(offset, fCurrentName.length(), fNewName);
  }

  private List<TextEdit> getAllRenameEdits(TextEdit declarationEdit) {
    final List<TextEdit> edits = Lists.newArrayList(declarationEdit);
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
//    TempOccurrenceAnalyzer fTempAnalyzer = new TempOccurrenceAnalyzer(fTempDeclarationNode);
//    fTempAnalyzer.perform();
//    int[] referenceOffsets = fTempAnalyzer.getReferenceAndDartdocOffsets();
//
//    TextEdit[] allRenameEdits = new TextEdit[referenceOffsets.length + 1];
//    for (int i = 0; i < referenceOffsets.length; i++) {
//      allRenameEdits[i] = createRenameEdit(referenceOffsets[i]);
//    }
//    allRenameEdits[referenceOffsets.length] = declarationEdit;
//    return allRenameEdits;
  }

  private void initAST() throws DartModelException {
//    if (!fIsComposite)
    {
      fCompilationUnitNode = DartCompilerUtilities.resolveUnit(fCu);
    }
    // Prepare variable Node.
    SourceRange sourceRange = fLocalVariable.getNameRange();
    fVariableNode = NodeFinder.perform(fCompilationUnitNode, sourceRange);
    if (fVariableNode == null) {
      return;
    }
    // Prepare variable Element.
    // TODO(scheglov) Fix DartC to set Element for DartIdentifier
    if (fVariableNode.getElement() == null) {
      fVariableNode = fVariableNode.getParent();
    }
    if (fVariableNode.getElement() instanceof VariableElement) {
      fVariableElement = (VariableElement) fVariableNode.getElement();
    }
//    if (variableReferenceNode.getParent() instanceof DartVariable) {
//      fTempDeclarationNode = (DartVariable) variableReferenceNode.getParent();
//    }
  }

//  private RefactoringStatus initialize(DartRefactoringArguments extended) {
//    final String handle = extended.getAttribute(DartRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
//    if (handle != null) {
//      final DartElement element = DartRefactoringDescriptorUtil.handleToElement(
//          extended.getProject(), handle, false);
//      if (element != null && element.exists()) {
//        if (element.getElementType() == DartElement.COMPILATION_UNIT) {
//          fCu = (CompilationUnit) element;
//        } else if (element.getElementType() == DartElement.VARIABLE) {
//          fLocalVariable = (DartVariableDeclaration) element;
//          fCu = fLocalVariable.getAncestor(CompilationUnit.class);
//          if (fCu == null) {
//            return DartRefactoringDescriptorUtil.createInputFatalStatus(element,
//                getProcessorName(), IDartRefactorings.RENAME_LOCAL_VARIABLE);
//          }
//        } else {
//          return DartRefactoringDescriptorUtil.createInputFatalStatus(element, getProcessorName(),
//              IDartRefactorings.RENAME_LOCAL_VARIABLE);
//        }
//      } else {
//        return DartRefactoringDescriptorUtil.createInputFatalStatus(element, getProcessorName(),
//            IDartRefactorings.RENAME_LOCAL_VARIABLE);
//      }
//    } else {
//      return RefactoringStatus.createFatalErrorStatus(Messages.format(
//          RefactoringCoreMessages.InitializableRefactoring_argument_not_exist,
//          DartRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
//    }
//    final String name = extended.getAttribute(DartRefactoringDescriptorUtil.ATTRIBUTE_NAME);
//    if (name != null && !"".equals(name)) {
//      setNewElementName(name);
//    } else {
//      return RefactoringStatus.createFatalErrorStatus(Messages.format(
//          RefactoringCoreMessages.InitializableRefactoring_argument_not_exist,
//          DartRefactoringDescriptorUtil.ATTRIBUTE_NAME));
//    }
//    if (fCu != null && fLocalVariable == null) {
//      final String selection = extended.getAttribute(DartRefactoringDescriptorUtil.ATTRIBUTE_SELECTION);
//      if (selection != null) {
//        int offset = -1;
//        int length = -1;
//        final StringTokenizer tokenizer = new StringTokenizer(selection);
//        if (tokenizer.hasMoreTokens()) {
//          offset = Integer.valueOf(tokenizer.nextToken()).intValue();
//        }
//        if (tokenizer.hasMoreTokens()) {
//          length = Integer.valueOf(tokenizer.nextToken()).intValue();
//        }
//        if (offset >= 0 && length >= 0) {
//          try {
//            final DartElement[] elements = fCu.codeSelect(offset, length);
//            if (elements != null) {
//              for (int index = 0; index < elements.length; index++) {
//                final DartElement element = elements[index];
//                if (element instanceof DartVariableDeclaration) {
//                  fLocalVariable = (DartVariableDeclaration) element;
//                }
//              }
//            }
//            if (fLocalVariable == null) {
//              return DartRefactoringDescriptorUtil.createInputFatalStatus(null, getProcessorName(),
//                  IDartRefactorings.RENAME_LOCAL_VARIABLE);
//            }
//          } catch (DartModelException exception) {
//            DartToolsPlugin.log(exception);
//          }
//        } else {
//          return RefactoringStatus.createFatalErrorStatus(Messages.format(
//              RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[] {
//                  selection, DartRefactoringDescriptorUtil.ATTRIBUTE_SELECTION}));
//        }
//      } else {
//        return RefactoringStatus.createFatalErrorStatus(Messages.format(
//            RefactoringCoreMessages.InitializableRefactoring_argument_not_exist,
//            DartRefactoringDescriptorUtil.ATTRIBUTE_SELECTION));
//      }
//    }
//    final String references = extended.getAttribute(DartRefactoringDescriptorUtil.ATTRIBUTE_REFERENCES);
//    if (references != null) {
//      fUpdateReferences = Boolean.valueOf(references).booleanValue();
//    } else {
//      return RefactoringStatus.createFatalErrorStatus(Messages.format(
//          RefactoringCoreMessages.InitializableRefactoring_argument_not_exist,
//          DartRefactoringDescriptorUtil.ATTRIBUTE_REFERENCES));
//    }
//    return new RefactoringStatus();
//  }

  private void initNames() {
    fCurrentName = fVariableElement.getName();
  }
}
