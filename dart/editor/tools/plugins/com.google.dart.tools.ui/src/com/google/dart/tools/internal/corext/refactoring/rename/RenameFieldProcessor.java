package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.participants.DartProcessors;
import com.google.dart.tools.internal.corext.refactoring.util.ResourceUtil;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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

public class RenameFieldProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameFieldProcessor"; //$NON-NLS-1$

  private final Field fField;
  private final TextChangeManager fChangeManager = new TextChangeManager(true);
  private List<SearchMatch> fReferences;

  /**
   * Creates a new rename field processor.
   * 
   * @param field the field, or <code>null</code> if invoked by scripting
   */
  public RenameFieldProcessor(Field field) {
    fField = field;
    setNewElementName(fField.getElementName());
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
  protected RefactoringStatus doCheckFinalConditions(IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException {
    try {
      pm.beginTask("", 14); //$NON-NLS-1$
      pm.setTaskName(RefactoringCoreMessages.RenameFieldRefactoring_checking);
      RefactoringStatus result = new RefactoringStatus();

      // check new name
      result.merge(checkNewElementName(getNewElementName()));
      pm.worked(1);

      // prepare references
      pm.setTaskName(RefactoringCoreMessages.RenameFieldRefactoring_searching);
      fReferences = getReferences(new SubProgressMonitor(pm, 3), result);
      pm.setTaskName(RefactoringCoreMessages.RenameFieldRefactoring_checking);

      // analyze affected units (such as warn about existing compilation errors)
      result.merge(analyzeAffectedCompilationUnits());

      // OK, create changes
      result.merge(createChanges(new SubProgressMonitor(pm, 10)));
      if (result.hasFatalError()) {
        return result;
      }

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

  private RefactoringStatus analyzeRenameChanges(IProgressMonitor pm) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    pm.done();
    return result;
    // TODO(scheglov)
//    CompilationUnit[] newWorkingCopies = null;
//    WorkingCopyOwner newWCOwner = new WorkingCopyOwner() { /* must subclass */
//    };
//    try {
//      pm.beginTask("", 2); //$NON-NLS-1$
//      RefactoringStatus result = new RefactoringStatus();
//      List<SearchMatch> oldReferences = fReferences;
//
//      List<CompilationUnit> compilationUnitsToModify = new ArrayList<CompilationUnit>();
//      compilationUnitsToModify.addAll(Arrays.asList(fChangeManager.getAllCompilationUnits()));
//
//      newWorkingCopies = RenameAnalyzeUtil.createNewWorkingCopies(
//          compilationUnitsToModify.toArray(new CompilationUnit[compilationUnitsToModify.size()]),
//          fChangeManager,
//          newWCOwner,
//          new SubProgressMonitor(pm, 1));
//
//      SearchResultGroup[] newReferences = getNewReferences(
//          new SubProgressMonitor(pm, 1),
//          result,
//          newWCOwner,
//          newWorkingCopies);
//      result.merge(RenameAnalyzeUtil.analyzeRenameChanges2(
//          fChangeManager,
//          oldReferences,
//          newReferences,
//          getNewElementName()));
//      return result;
//    } finally {
//      pm.done();
//      if (newWorkingCopies != null) {
//        for (int i = 0; i < newWorkingCopies.length; i++) {
//          newWorkingCopies[i].discardWorkingCopy();
//        }
//      }
//    }
  }

  private RefactoringStatus createChanges(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.RenameFieldRefactoring_checking, 3);
    RefactoringStatus result = new RefactoringStatus();

    fChangeManager.clear();
    addDeclarationUpdate();
    addReferenceUpdates(new SubProgressMonitor(pm, 1));

    result.merge(analyzeRenameChanges(new SubProgressMonitor(pm, 2)));
    if (result.hasFatalError()) {
      return result;
    }

    pm.done();
    return result;
  }

//  private IJavaSearchScope createRefactoringScope() throws CoreException {
//    return RefactoringScopeFactory.create(fField, true, false);
//  }
//
//  private SearchPattern createSearchPattern() {
//    return SearchPatternFactory.createPattern(fField, IJavaSearchConstants.REFERENCES);
//  }

  private TextEdit createTextChange(SearchMatch match) {
    SourceRange sourceRange = match.getSourceRange();
    return new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), getNewElementName());
  }

//  private Field getFieldInWorkingCopy(CompilationUnit newWorkingCopyOfDeclaringCu,
//      String elementName) {
//    Type type = fField.getDeclaringType();
//    Type typeWc = (Type) DartModelUtil.findInCompilationUnit(newWorkingCopyOfDeclaringCu, type);
//    if (typeWc == null) {
//      return null;
//    }
//
//    return typeWc.getField(elementName);
//  }
//
//  private SearchResultGroup[] getNewReferences(IProgressMonitor pm, RefactoringStatus status,
//      WorkingCopyOwner owner, CompilationUnit[] newWorkingCopies) throws CoreException {
//    pm.beginTask("", 2); //$NON-NLS-1$
//    CompilationUnit declaringCuWorkingCopy = RenameAnalyzeUtil.findWorkingCopyForCu(
//        newWorkingCopies,
//        fField.getCompilationUnit());
//    if (declaringCuWorkingCopy == null) {
//      return new SearchResultGroup[0];
//    }
//
//    Field field = getFieldInWorkingCopy(declaringCuWorkingCopy, getNewElementName());
//    if (field == null || !field.exists()) {
//      return new SearchResultGroup[0];
//    }
//
//    CollectingSearchRequestor requestor = null;
//    if (fDelegateUpdating && RefactoringAvailabilityTester.isDelegateCreationAvailable(fField)) {
//      // There will be two new matches inside the delegate (the invocation
//      // and the javadoc) which are OK and must not be reported.
//      final Field oldField = getFieldInWorkingCopy(declaringCuWorkingCopy, getCurrentElementName());
//      requestor = new CollectingSearchRequestor() {
//        @Override
//        public void acceptSearchMatch(SearchMatch match) throws CoreException {
//          if (!oldField.equals(match.getElement())) {
//            super.acceptSearchMatch(match);
//          }
//        }
//      };
//    } else {
//      requestor = new CollectingSearchRequestor();
//    }
//
//    SearchPattern newPattern = SearchPattern.createPattern(field, IJavaSearchConstants.REFERENCES);
//    IJavaSearchScope scope = RefactoringScopeFactory.create(fField, true, true);
//    return RefactoringSearchEngine.search(
//        newPattern,
//        owner,
//        scope,
//        requestor,
//        new SubProgressMonitor(pm, 1),
//        status);
//  }

  private List<SearchMatch> getReferences(IProgressMonitor pm, RefactoringStatus status)
      throws CoreException {
    try {
      return SearchEngineFactory.createSearchEngine().searchReferences(fField, null, null, pm);
    } catch (SearchException e) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.getMessage(), e));
    }
  }
}
