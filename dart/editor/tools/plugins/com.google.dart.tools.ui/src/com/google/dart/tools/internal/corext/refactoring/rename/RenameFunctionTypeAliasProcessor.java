package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.common.base.Objects;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.List;

/**
 * {@link DartRenameProcessor} for {@link DartFunctionTypeAlias}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameFunctionTypeAliasProcessor extends RenameTopLevelProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameFunctionTypeAliasProcessor"; //$NON-NLS-1$

  private final DartFunctionTypeAlias type;

  /**
   * @param type the {@link DartFunctionTypeAlias} to rename, not <code>null</code>.
   */
  public RenameFunctionTypeAliasProcessor(DartFunctionTypeAlias type) {
    super(type);
    this.type = type;
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkFunctionTypeAliasName(newName);
    result.merge(super.checkNewElementName(newName));
    return result;
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() throws CoreException {
    DartFunctionTypeAlias result = null;
    DartElement[] topLevelElements = type.getCompilationUnit().getChildren();
    for (DartElement element : topLevelElements) {
      if (element instanceof DartFunctionTypeAlias
          && Objects.equal(element.getElementName(), getNewElementName())) {
        result = (DartFunctionTypeAlias) element;
      }
    }
    return result;
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameFunctionTypeAliasRefactoring_name;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(type);
  }

  @Override
  protected List<SearchMatch> getReferences(final IProgressMonitor pm) throws CoreException {
    return ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        return searchEngine.searchReferences(type, null, null, pm);
      }
    });
  }
}
