package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
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

import java.util.List;
import java.util.Set;

/**
 * {@link DartRenameProcessor} for {@link Type}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameTypeProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameTypeProcessor"; //$NON-NLS-1$

  private static void addTextEdit(TextChange change, String groupName, TextEdit textEdit) {
    TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
  }

  private final Type type;
  private final String oldName;
  private final TextChangeManager changeManager = new TextChangeManager(true);

  private List<SearchMatch> references;

  /**
   * @param type the {@link Type} to rename, not <code>null</code>.
   */
  public RenameTypeProcessor(Type type) {
    this.type = type;
    oldName = type.getElementName();
    setNewElementName(oldName);
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    return Checks.checkIfCuBroken(type);
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkTypeName(newName);

    if (Checks.isAlreadyNamed(type, newName)) {
      result.addError(
          RefactoringCoreMessages.RenameRefactoring_another_name,
          DartStatusContext.create(type));
      return result;
    }

    // type can not have two members with same name
    // TODO(scheglov) may be in unit
//    {
//      Type enclosingType = field.getDeclaringType();
//      TypeMember[] existingMembers = enclosingType.getExistingMembers(newName);
//      if (existingMembers.length != 0) {
//        IPath resourcePath = enclosingType.getResource().getFullPath();
//        result.addError(Messages.format(
//            RefactoringCoreMessages.RenameRefactoring_enclosing_type_member_already_defined,
//            new Object[] {
//                enclosingType.getElementName(),
//                BasicElementLabels.getPathLabel(resourcePath, false),
//                newName}), DartStatusContext.create(existingMembers[0]));
//        return result;
//      }
//    }

    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameRefactoring_checking, 1);
    try {
      return new CompositeChange(getProcessorName(), changeManager.getAllChanges());
    } finally {
      monitor.done();
    }
  }

  @Override
  public final String getCurrentElementName() {
    return type.getElementName();
  }

  @Override
  public Object[] getElements() {
    return new Object[] {type};
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return type.getCompilationUnit().getType(getNewElementName());
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameTypeRefactoring_name;
  }

  @Override
  public int getSaveMode() {
    return RefactoringSaveHelper.SAVE_ALL;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(type);
  }

  @Override
  protected RefactoringStatus doCheckFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException {
    try {
      pm.beginTask("", 19); //$NON-NLS-1$
      pm.setTaskName(RefactoringCoreMessages.RenameRefactoring_checking);
      RefactoringStatus result = new RefactoringStatus();
      // check new name
      result.merge(checkNewElementName(getNewElementName()));
      pm.worked(1);
      // prepare references
      pm.setTaskName(RefactoringCoreMessages.RenameRefactoring_searching);
      prepareReferences(new SubProgressMonitor(pm, 3));
      pm.setTaskName(RefactoringCoreMessages.RenameRefactoring_checking);
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

  private void addDeclarationUpdate() throws CoreException {
    SourceRange nameRange = type.getNameRange();
    CompilationUnit cu = type.getCompilationUnit();
    String editName = RefactoringCoreMessages.RenameRefactoring_update_declaration;
    addTextEdit(changeManager.get(cu), editName, createTextChange(nameRange));
  }

  private void addReferenceUpdates(IProgressMonitor pm) throws DartModelException {
    pm.beginTask("", references.size()); //$NON-NLS-1$
    String editName = RefactoringCoreMessages.RenameRefactoring_update_reference;
    for (SearchMatch searchMatch : references) {
      SourceRange matchRange = searchMatch.getSourceRange();
      if (matchRange.getOffset() != type.getSourceRange().getOffset()) {
        CompilationUnit cu = searchMatch.getElement().getAncestor(CompilationUnit.class);
        addTextEdit(changeManager.get(cu), editName, createTextChange(matchRange));
      }
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
    try {
      RefactoringStatus result = new RefactoringStatus();
      String newName = getNewElementName();
      // prepare libraries with references
      Set<DartLibrary> libraries = Sets.newHashSet();
      for (SearchMatch reference : references) {
        DartLibrary library = reference.getElement().getAncestor(DartLibrary.class);
        libraries.add(library);
      }
      // visit libraries with references
      for (DartLibrary library : libraries) {
        // visit units of library
        for (CompilationUnit unit : library.getCompilationUnitsInScope()) {
          // visit top-level children on unit
          for (DartElement unitElement : unit.getChildren()) {
            // may be conflict with existing top-level element
            if (unitElement instanceof CompilationUnitElement
                && Objects.equal(unitElement.getElementName(), newName)) {
              IPath libraryPath = library.getResource().getFullPath();
              IPath resourcePath = unitElement.getResource().getFullPath();
              String message = Messages.format(
                  RefactoringCoreMessages.RenameTopRefactoring_shadow_topLevel,
                  new Object[] {
                      BasicElementLabels.getPathLabel(resourcePath, false),
                      BasicElementLabels.getPathLabel(libraryPath, false),
                      RenameAnalyzeUtil.getElementTypeName(unitElement),
                      newName});
              result.addFatalError(
                  message,
                  DartStatusContext.create((CompilationUnitElement) unitElement));
              return result;
            }
            // analyze Type
            if (unitElement instanceof Type) {
              Type type = (Type) unitElement;
              // visit type members
              List<TypeMember> typeMembers = RenameAnalyzeUtil.getTypeMembers(type);
              for (TypeMember typeMember : typeMembers) {
                // may be conflict with existing TypeMember
                if (Objects.equal(typeMember.getElementName(), newName)) {
                  IPath resourcePath = unitElement.getResource().getFullPath();
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameTopRefactoring_shadow_typeMember,
                      new Object[] {
                          type.getElementName(),
                          BasicElementLabels.getPathLabel(resourcePath, false),
                          RenameAnalyzeUtil.getElementTypeName(typeMember),
                          newName,
                          RenameAnalyzeUtil.getElementTypeName(type)});
                  result.addWarning(message, DartStatusContext.create(typeMember));
                  return result;
                }
                // analyze Method
                if (typeMember instanceof Method) {
                  Method method = (Method) typeMember;
                  // visit local variables (and parameters)
                  for (DartVariableDeclaration variable : method.getLocalVariables()) {
                    if (Objects.equal(variable.getElementName(), newName)) {
                      IPath resourcePath = unitElement.getResource().getFullPath();
                      String message = Messages.format(
                          RefactoringCoreMessages.RenameTopRefactoring_shadow_variable_inMethod,
                          new Object[] {
                              type.getElementName(),
                              method.getElementName(),
                              BasicElementLabels.getPathLabel(resourcePath, false),
                              RenameAnalyzeUtil.getElementTypeName(variable),
                              newName,
                              RenameAnalyzeUtil.getElementTypeName(type)});
                      result.addWarning(message, DartStatusContext.create(variable));
                      return result;
                    }
                  }
                }
              }
            }
            // analyze Function
            if (unitElement instanceof DartFunction) {
              DartFunction function = (DartFunction) unitElement;
              // visit local variables (and parameters)
              for (DartVariableDeclaration variable : function.getLocalVariables()) {
                if (Objects.equal(variable.getElementName(), newName)) {
                  IPath resourcePath = unitElement.getResource().getFullPath();
                  String message = Messages.format(
                      RefactoringCoreMessages.RenameTopRefactoring_shadow_variable_inFunction,
                      new Object[] {
                          function.getElementName(),
                          BasicElementLabels.getPathLabel(resourcePath, false),
                          RenameAnalyzeUtil.getElementTypeName(variable),
                          newName,
                          RenameAnalyzeUtil.getElementTypeName(type)});
                  result.addWarning(message, DartStatusContext.create(variable));
                  return result;
                }
              }
            }
          }
        }
      }
//      // analyze top-level elements
//      pm.subTask("Analyze top-level elements");
//      RenameAnalyzeUtil.checkShadow_topLevel(
//          result,
//          references,
//          newName,
//          RefactoringCoreMessages.RenameRefactoring_shadow_topLevel);
//      pm.worked(1);
//      if (result.hasFatalError()) {
//        return result;
//      }
//      // analyze supertypes
//      pm.subTask("Analyze supertypes");
//      RenameAnalyzeUtil.checkShadow_superType_member(
//          result,
//          type,
//          newName,
//          RefactoringCoreMessages.RenameRefactoring_shadow_superType_member);
//      pm.worked(1);
//      if (result.hasFatalError()) {
//        return result;
//      }
//      // analyze subtypes
//      pm.subTask("Analyze subtypes");
//      RenameAnalyzeUtil.checkShadow_subType(
//          result,
//          type,
//          newName,
//          RefactoringCoreMessages.RenameRefactoring_shadow_subType_member,
//          RefactoringCoreMessages.RenameRefactoring_shadow_subType_parameter,
//          RefactoringCoreMessages.RenameRefactoring_shadow_subType_variable);
//      pm.worked(1);
      // OK
      return result;
    } finally {
      pm.done();
    }
  }

  private void createChanges(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.RenameRefactoring_checking, 10);
    changeManager.clear();
    // update declaration
    addDeclarationUpdate();
    pm.worked(1);
    // update references
    addReferenceUpdates(new SubProgressMonitor(pm, 9));
    pm.done();
  }

  private TextEdit createTextChange(SourceRange sourceRange) {
    return new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), getNewElementName());
  }

  private void prepareReferences(final IProgressMonitor pm) throws CoreException {
    references = ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        return searchEngine.searchReferences(type, null, null, pm);
      }
    });
  }
}
