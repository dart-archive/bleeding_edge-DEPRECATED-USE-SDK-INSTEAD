package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.participants.DartProcessors;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.ResourceUtil;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.List;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameFieldProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameFieldProcessor"; //$NON-NLS-1$

  private final Field field;
  private final String oldName;
  private final TextChangeManager changeManager = new TextChangeManager(true);
  private List<SearchMatch> references;

  /**
   * Creates a new rename field processor.
   * 
   * @param field the {@link Field} to rename, not <code>null</code>.
   */
  public RenameFieldProcessor(Field field) {
    this.field = field;
    oldName = field.getElementName();
    setNewElementName(oldName);
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    return Checks.checkIfCuBroken(field);
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkFieldName(newName, field);

    if (!field.isStatic() && !Checks.startsWithLowerCase(newName)) {
      result.addWarning(RefactoringCoreMessages.RenameFieldRefactoring_should_start_lowercase);
    }

    if (Checks.isAlreadyNamed(field, newName)) {
      result.addError(
          RefactoringCoreMessages.RenameFieldRefactoring_another_name,
          DartStatusContext.create(field));
      return result;
    }

    if (field.getDeclaringType().getField(newName).exists()) {
      result.addError(
          RefactoringCoreMessages.RenameFieldRefactoring_field_already_defined,
          DartStatusContext.create(field.getDeclaringType().getField(newName)));
      return result;
    }

    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameFieldRefactoring_checking, 1);
    try {
      return new CompositeChange(getProcessorName(), changeManager.getAllChanges());
    } finally {
      monitor.done();
    }
  }

  @Override
  public final String getCurrentElementName() {
    return field.getElementName();
  }

  @Override
  public Object[] getElements() {
    return new Object[] {field};
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return field.getDeclaringType().getField(getNewElementName());
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameFieldRefactoring_name;
  }

  @Override
  public int getSaveMode() {
    return RefactoringSaveHelper.SAVE_ALL;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameFieldAvailable(field);
  }

  @Override
  protected RenameModifications computeRenameModifications() throws CoreException {
    RenameModifications result = new RenameModifications();
    result.rename(field, new RenameArguments(getNewElementName(), true));
    return result;
  }

  @Override
  protected RefactoringStatus doCheckFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException {
    try {
      pm.beginTask("", 19); //$NON-NLS-1$
      pm.setTaskName(RefactoringCoreMessages.RenameFieldRefactoring_checking);
      RefactoringStatus result = new RefactoringStatus();
      // check new name
      result.merge(checkNewElementName(getNewElementName()));
      pm.worked(1);
      // prepare references
      pm.setTaskName(RefactoringCoreMessages.RenameFieldRefactoring_searching);
      references = getReferences(new SubProgressMonitor(pm, 3));
      pm.setTaskName(RefactoringCoreMessages.RenameFieldRefactoring_checking);
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

  @Override
  protected String[] getAffectedProjectNatures() throws CoreException {
    return DartProcessors.computeAffectedNatures(field);
  }

  @Override
  protected IFile[] getChangedFiles() {
    return ResourceUtil.getFiles(changeManager.getAllCompilationUnits());
  }

  private void addDeclarationUpdate() throws CoreException {
    SourceRange nameRange = field.getNameRange();
    TextEdit textEdit = new ReplaceEdit(
        nameRange.getOffset(),
        nameRange.getLength(),
        getNewElementName());
    CompilationUnit cu = field.getCompilationUnit();
    String groupName = RefactoringCoreMessages.RenameFieldRefactoring_Update_field_declaration;
    addTextEdit(changeManager.get(cu), groupName, textEdit);
  }

  private void addReferenceUpdates(IProgressMonitor pm) {
    pm.beginTask("", references.size()); //$NON-NLS-1$
    String editName = RefactoringCoreMessages.RenameFieldRefactoring_Update_field_reference;
    for (SearchMatch searchMatch : references) {
      CompilationUnit cu = searchMatch.getElement().getAncestor(CompilationUnit.class);
      addTextEdit(changeManager.get(cu), editName, createTextChange(searchMatch));
      pm.worked(1);
    }
  }

  private void addTextEdit(TextChange change, String groupName, TextEdit textEdit) {
    TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
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
      Type enclosingType = field.getAncestor(Type.class);
      String newName = getNewElementName();
      // analyze top-level elements
      pm.subTask("Analyze top-level elements");
      RenameAnalyzeUtil.checkShadow_topLevel(
          result,
          references,
          newName,
          RefactoringCoreMessages.RenameFieldRefactoring_shadow_topLevel);
      pm.worked(1);
      if (result.hasFatalError()) {
        return result;
      }
      // analyze supertypes
      pm.subTask("Analyze supertypes");
      RenameAnalyzeUtil.checkShadow_superType_member(
          result,
          enclosingType,
          newName,
          RefactoringCoreMessages.RenameFieldRefactoring_shadow_superType_member);
      pm.worked(1);
      if (result.hasFatalError()) {
        return result;
      }
      // analyze subtypes
      pm.subTask("Analyze subtypes");
      RenameAnalyzeUtil.checkShadow_subType(
          result,
          enclosingType,
          newName,
          RefactoringCoreMessages.RenameFieldRefactoring_shadow_subType_member,
          RefactoringCoreMessages.RenameFieldRefactoring_shadow_subType_parameter,
          RefactoringCoreMessages.RenameFieldRefactoring_shadow_subType_variable);
      pm.worked(1);

      // OK
      return result;
    } finally {
      pm.done();
    }
  }

  private void createChanges(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.RenameFieldRefactoring_checking, 10);
    changeManager.clear();
    // update declaration
    addDeclarationUpdate();
    pm.worked(1);
    // update references
    addReferenceUpdates(new SubProgressMonitor(pm, 9));
    pm.done();
  }

  private TextEdit createTextChange(SearchMatch match) {
    SourceRange sourceRange = match.getSourceRange();
    return new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), getNewElementName());
  }

  private List<SearchMatch> getReferences(final IProgressMonitor pm) throws CoreException {
    return ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        return searchEngine.searchReferences(field, null, null, pm);
      }
    });
  }
}
