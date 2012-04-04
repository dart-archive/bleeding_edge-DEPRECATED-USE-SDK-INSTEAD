package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.common.collect.Sets;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
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
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.ResourceUtil;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import java.util.Set;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameFieldProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameFieldProcessor"; //$NON-NLS-1$

  private final Field fField;
  private final String oldName;
  private final TextChangeManager fChangeManager = new TextChangeManager(true);
  private List<SearchMatch> fReferences;

  /**
   * Creates a new rename field processor.
   * 
   * @param field the field, or <code>null</code> if invoked by scripting
   */
  public RenameFieldProcessor(Field field) {
    fField = field;
    oldName = fField.getElementName();
    setNewElementName(oldName);
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    return Checks.checkIfCuBroken(fField);
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkFieldName(newName, fField);

    if (!fField.isStatic() && !Checks.startsWithLowerCase(newName)) {
      result.addWarning(RefactoringCoreMessages.RenameFieldRefactoring_should_start_lowercase);
    }

    if (Checks.isAlreadyNamed(fField, newName)) {
      result.addError(
          RefactoringCoreMessages.RenameFieldRefactoring_another_name,
          DartStatusContext.create(fField));
      return result;
    }

    if (fField.getDeclaringType().getField(newName).exists()) {
      result.addError(
          RefactoringCoreMessages.RenameFieldRefactoring_field_already_defined,
          DartStatusContext.create(fField.getDeclaringType().getField(newName)));
      return result;
    }

    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameFieldRefactoring_checking, 1);
    try {
      return new CompositeChange(getProcessorName(), fChangeManager.getAllChanges());
    } finally {
      monitor.done();
    }
  }

  @Override
  public final String getCurrentElementName() {
    return fField.getElementName();
  }

  @Override
  public Object[] getElements() {
    return new Object[] {fField};
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return fField.getDeclaringType().getField(getNewElementName());
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
    return RefactoringAvailabilityTester.isRenameFieldAvailable(fField);
  }

  @Override
  protected RenameModifications computeRenameModifications() throws CoreException {
    RenameModifications result = new RenameModifications();
    result.rename(fField, new RenameArguments(getNewElementName(), true));
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
      fReferences = getReferences(new SubProgressMonitor(pm, 3));
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
    return DartProcessors.computeAffectedNatures(fField);
  }

  @Override
  protected IFile[] getChangedFiles() {
    return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
  }

  private void addDeclarationUpdate() throws CoreException {
    SourceRange nameRange = fField.getNameRange();
    TextEdit textEdit = new ReplaceEdit(
        nameRange.getOffset(),
        nameRange.getLength(),
        getNewElementName());
    CompilationUnit cu = fField.getCompilationUnit();
    String groupName = RefactoringCoreMessages.RenameFieldRefactoring_Update_field_declaration;
    addTextEdit(fChangeManager.get(cu), groupName, textEdit);
  }

  private void addReferenceUpdates(IProgressMonitor pm) {
    pm.beginTask("", fReferences.size()); //$NON-NLS-1$
    String editName = RefactoringCoreMessages.RenameFieldRefactoring_Update_field_reference;
    for (SearchMatch searchMatch : fReferences) {
      CompilationUnit cu = searchMatch.getElement().getAncestor(CompilationUnit.class);
      addTextEdit(fChangeManager.get(cu), editName, createTextChange(searchMatch));
      pm.worked(1);
    }
  }

  private void addTextEdit(TextChange change, String groupName, TextEdit textEdit) {
    TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
  }

  private RefactoringStatus analyzeAffectedCompilationUnits() throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    result.merge(Checks.checkCompileErrorsInAffectedFiles(fReferences));
    return result;
  }

  private RefactoringStatus analyzePossibleConflicts(IProgressMonitor pm) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    Type enclosingType = fField.getAncestor(Type.class);
    String newName = getNewElementName();
    // analyze top-level elements
    {
      Set<DartLibrary> visitedLibraries = Sets.newHashSet();
      for (SearchMatch searchMatch : fReferences) {
        DartElement shadowElement = RenameAnalyzeUtil.getTopLevelElementNamed(
            visitedLibraries,
            searchMatch.getElement(),
            newName);
        if (shadowElement != null) {
          DartLibrary shadowLibrary = shadowElement.getAncestor(DartLibrary.class);
          IPath libraryPath = shadowLibrary.getResource().getFullPath();
          IPath resourcePath = shadowElement.getResource().getFullPath();
          result.addFatalError(Messages.format(
              RefactoringCoreMessages.RenameFieldRefactoring_shadow_topLevel_willShadow,
              new Object[] {
                  BasicElementLabels.getPathLabel(resourcePath, false),
                  BasicElementLabels.getPathLabel(libraryPath, false),
                  newName}));
          return result;
        }
      }
    }
    // analyze supertypes
    Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(enclosingType);
    for (Type type : superTypes) {
      if (type.getExistingMembers(newName).length != 0) {
        IPath resourcePath = type.getResource().getFullPath();
        result.addFatalError(Messages.format(
            RefactoringCoreMessages.RenameFieldRefactoring_shadow_superTypeMember_willBeShadowed,
            new Object[] {
                type.getElementName(),
                BasicElementLabels.getPathLabel(resourcePath, false),
                newName}));
        return result;
      }
    }
    // analyze subtypes
    List<Type> subTypes = RenameAnalyzeUtil.getSubTypes(enclosingType);
    for (Type type : subTypes) {
      // check for declared members
      if (type.getExistingMembers(newName).length != 0) {
        IPath resourcePath = type.getResource().getFullPath();
        result.addFatalError(Messages.format(
            RefactoringCoreMessages.RenameFieldRefactoring_shadow_subTypeMember_willShadow,
            new Object[] {
                type.getElementName(),
                BasicElementLabels.getPathLabel(resourcePath, false),
                newName}));
        return result;
      }
      // check for local variables
      for (Method method : type.getMethods()) {
        DartVariableDeclaration[] localVariables = method.getLocalVariables();
        for (DartVariableDeclaration variable : localVariables) {
          if (variable.getElementName().equals(newName)) {
            IPath resourcePath = type.getResource().getFullPath();
            result.addFatalError(Messages.format(
                variable.isParameter()
                    ? RefactoringCoreMessages.RenameFieldRefactoring_shadow_subTypeParameter_willShadow
                    : RefactoringCoreMessages.RenameFieldRefactoring_shadow_subTypeVariable_willShadow,
                new Object[] {
                    type.getElementName(),
                    method.getElementName(),
                    BasicElementLabels.getPathLabel(resourcePath, false),
                    newName}));
            return result;
          }
        }
      }
    }
    return result;
  }

  private void createChanges(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.RenameFieldRefactoring_checking, 10);
    fChangeManager.clear();
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
        return searchEngine.searchReferences(fField, null, null, pm);
      }
    });
  }
}
