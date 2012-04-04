package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ErrorSeverity;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.base.DartStringStatusContext;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextChange;

import java.util.List;
import java.util.Set;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
class RenameAnalyzeUtil {

  private static final WorkingCopyOwner WORKING_COPY_OWNER = new WorkingCopyOwner() {
  };

//  private static class ProblemNodeFinder {
//
//    private static class NameNodeVisitor extends ASTVisitor {
//
//      private Collection<IRegion> fRanges;
//      private Collection<DartIdentifier> fProblemNodes;
//      private String fKey;
//
//      public NameNodeVisitor(TextEdit[] edits, TextChange change, String key) {
//        Assert.isNotNull(edits);
//        Assert.isNotNull(key);
//
//        fRanges = new HashSet<IRegion>(Arrays.asList(RefactoringAnalyzeUtil.getNewRanges(edits,
//            change)));
//        fProblemNodes = new ArrayList<DartIdentifier>(0);
//        fKey = key;
//      }
//
//      public DartIdentifier[] getProblemNodes() {
//        return fProblemNodes.toArray(new DartIdentifier[fProblemNodes.size()]);
//      }
//
//      //----- visit methods
//
//      @Override
//      public boolean visit(DartIdentifier node) {
//        DartVariable decl = getDartVariable(node);
//        if (decl == null) {
//          return super.visit(node);
//        }
//
//        IVariableBinding binding = decl.resolveBinding();
//        if (binding == null) {
//          return super.visit(node);
//        }
//
//        boolean keysEqual = fKey.equals(binding.getKey());
//        boolean rangeInSet = fRanges.contains(new Region(node.getStartPosition(), node.getLength()));
//
//        if (keysEqual && !rangeInSet) {
//          fProblemNodes.add(node);
//        }
//
//        if (!keysEqual && rangeInSet) {
//          fProblemNodes.add(node);
//        }
//
//        /*
//         * if (!keyEquals && !rangeInSet) ok, different local variable.
//         * 
//         * if (keyEquals && rangeInSet) ok, renamed local variable & has been renamed.
//         */
//
//        return super.visit(node);
//      }
//    }
//
//    public static DartIdentifier[] getProblemNodes(DartNode methodNode, DartVariable variableNode,
//        TextEdit[] edits, TextChange change) {
//      String key = variableNode.resolveBinding().getKey();
//      NameNodeVisitor visitor = new NameNodeVisitor(edits, change, key);
//      methodNode.accept(visitor);
//      return visitor.getProblemNodes();
//    }
//
//    private ProblemNodeFinder() {
//      //static
//    }
//  }

  /**
   * This method analyzes a set of local variable renames inside one {@link CompilationUnit}. It
   * checks whether any new compile errors have been introduced by the rename.
   * 
   * @return a {@link RefactoringStatus} containing errors if compile errors or wrongly renamed
   *         nodes are found.
   */
  public static RefactoringStatus analyzeLocalRenames(TextChange change, CompilationUnit oldUnit)
      throws CoreException {
    RefactoringStatus result = new RefactoringStatus();

    // prepare new compilation errors
    String newSource = change.getPreviewContent(null);
    List<DartCompilationError> newErrors = Lists.newArrayList();
    CompilationUnit newUnit = oldUnit.getWorkingCopy(WORKING_COPY_OWNER, null);
    try {
      newUnit.getBuffer().setContents(newSource);
      DartCompilerUtilities.resolveUnit(newUnit, newErrors);
    } finally {
      newUnit.discardWorkingCopy();
    }

    // check for introduced compilation errors
    for (DartCompilationError compilationError : newErrors) {
      ErrorSeverity errorSeverity = compilationError.getErrorCode().getErrorSeverity();
      if (errorSeverity == ErrorSeverity.ERROR) {
        DartStringStatusContext statusContext = new DartStringStatusContext(
            newSource,
            SourceRangeFactory.create(compilationError));
        result.addEntry(new RefactoringStatusEntry(
            RefactoringStatus.ERROR,
            compilationError.getMessage(),
            statusContext));
      }
    }
    return result;
  }

//  static RefactoringStatus analyzeRenameChanges(TextChangeManager manager,
//      SearchResultGroup[] oldOccurrences, SearchResultGroup[] newOccurrences) {
//    RefactoringStatus result = new RefactoringStatus();
//    for (int i = 0; i < oldOccurrences.length; i++) {
//      SearchResultGroup oldGroup = oldOccurrences[i];
//      SearchMatch[] oldSearchResults = oldGroup.getSearchResults();
//      DartUnit cunit = oldGroup.getCompilationUnit();
//      if (cunit == null) {
//        continue;
//      }
//      for (int j = 0; j < oldSearchResults.length; j++) {
//        SearchMatch oldSearchResult = oldSearchResults[j];
//        if (!RenameAnalyzeUtil.existsInNewOccurrences(oldSearchResult, newOccurrences, manager)) {
//          addShadowsError(cunit, oldSearchResult, result);
//        }
//      }
//    }
//    return result;
//  }
//
//  //TODO: Currently filters out declarations (MethodDeclarationMatch, FieldDeclarationMatch).
//  //Long term solution: only pass reference search results in.
//  static RefactoringStatus analyzeRenameChanges2(TextChangeManager manager,
//      SearchResultGroup[] oldReferences, SearchResultGroup[] newReferences, String newElementName) {
//    RefactoringStatus result = new RefactoringStatus();
//
//    HashMap<DartUnit, SearchMatch[]> cuToNewResults = new HashMap<DartUnit, SearchMatch[]>(
//        newReferences.length);
//    for (int i1 = 0; i1 < newReferences.length; i1++) {
//      DartUnit cu = newReferences[i1].getCompilationUnit();
//      if (cu != null) {
//        cuToNewResults.put(cu.getPrimary(), newReferences[i1].getSearchResults());
//      }
//    }
//
//    for (int i = 0; i < oldReferences.length; i++) {
//      SearchResultGroup oldGroup = oldReferences[i];
//      SearchMatch[] oldMatches = oldGroup.getSearchResults();
//      DartUnit cu = oldGroup.getCompilationUnit();
//      if (cu == null) {
//        continue;
//      }
//
//      SearchMatch[] newSearchMatches = cuToNewResults.remove(cu);
//      if (newSearchMatches == null) {
//        for (int j = 0; j < oldMatches.length; j++) {
//          SearchMatch oldMatch = oldMatches[j];
//          addShadowsError(cu, oldMatch, result);
//        }
//      } else {
//        analyzeChanges(cu, manager.get(cu), oldMatches, newSearchMatches, newElementName, result);
//      }
//    }
//
//    for (Iterator<Entry<DartUnit, SearchMatch[]>> iter = cuToNewResults.entrySet().iterator(); iter.hasNext();) {
//      Entry<DartUnit, SearchMatch[]> entry = iter.next();
//      DartUnit cu = entry.getKey();
//      SearchMatch[] newSearchMatches = entry.getValue();
//      for (int i = 0; i < newSearchMatches.length; i++) {
//        SearchMatch newMatch = newSearchMatches[i];
//        addReferenceShadowedError(cu, newMatch, newElementName, result);
//      }
//    }
//    return result;
//  }

  /**
   * @return all direct and indirect subtypes of the given {@link Type}.
   */
  public static List<Type> getSubTypes(final Type type) throws CoreException {
    List<Type> subTypes = Lists.newArrayList();
    // find direct references
    List<SearchMatch> matches = ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        return searchEngine.searchSubtypes(type, null, null, null);
      }
    });
    // add references from Types, find indirect subtypes
    for (SearchMatch match : matches) {
      if (match.getElement() instanceof Type) {
        Type subType = (Type) match.getElement();
        subTypes.add(subType);
        subTypes.addAll(getSubTypes(subType));
      }
    }
    // done
    return subTypes;
  }

  /**
   * @return all direct and indirect supertypes of the given {@link Type}.
   */
  public static Set<Type> getSuperTypes(final Type type) throws CoreException {
    Set<Type> superTypes = Sets.newHashSet();
    // find direct supertypes
    List<SearchMatch> matches = ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        return searchEngine.searchSupertypes(type, null, null, null);
      }
    });
    // add references from Types, find indirect supertypes
    for (SearchMatch match : matches) {
      if (match.getElement() instanceof Type) {
        Type superType = (Type) match.getElement();
        if (superTypes.add(superType)) {
          superTypes.addAll(getSuperTypes(superType));
        }
      }
    }
    // done
    return superTypes;
  }

  /**
   * @return the first top-level {@link DartElement} in the enclosing {@link DartLibrary} or any
   *         {@link DartLibrary} imported by it, which has given name. May be <code>null</code>.
   */
  public static DartElement getTopLevelElementNamed(
      Set<DartLibrary> visitedLibraries,
      DartElement reference,
      String name) throws DartModelException {
    DartLibrary library = reference.getAncestor(DartLibrary.class);
    if (library != null && !visitedLibraries.contains(library)) {
      visitedLibraries.add(library);
      // search in units of this library
      for (CompilationUnit unit : library.getCompilationUnits()) {
        DartElement[] children = unit.getChildren();
        for (DartElement element : children) {
          if (Objects.equal(element.getElementName(), name)) {
            return element;
          }
        }
      }
      // search in imported libraries
      for (DartLibrary importedLibrary : library.getImportedLibraries()) {
        DartElement element = getTopLevelElementNamed(visitedLibraries, importedLibrary, name);
        if (element != null) {
          return element;
        }
      }
    }
    // not found
    return null;
  }

//  static List<CompilationUnit> createWorkingCopies(List<CompilationUnit> oldUnits,
//      TextChangeManager manager) throws CoreException {
//    List<CompilationUnit> newUnits = Lists.newArrayList();
//    for (CompilationUnit oldUnit : oldUnits) {
//      newUnits.add(createWorkingCopy(oldUnit, manager));
//    }
//    return newUnits;
//  }
//
//  static CompilationUnit createWorkingCopy(CompilationUnit oldUnit, TextChangeManager manager)
//      throws CoreException {
//    CompilationUnit newUnit = oldUnit.getWorkingCopy(WORKING_COPY_OWNER, null);
//    String newSource = manager.get(oldUnit).getPreviewContent(null);
//    newUnit.getBuffer().setContents(newSource);
//    newWc.reconcile(DartUnit.NO_AST, false, owner, pm);
//    return newUnit;
//  }

//
//  static DartUnit findWorkingCopyForCu(DartUnit[] newWorkingCopies, DartUnit cu) {
//    DartUnit original = cu == null ? null : cu.getPrimary();
//    for (int i = 0; i < newWorkingCopies.length; i++) {
//      if (newWorkingCopies[i].getPrimary().equals(original)) {
//        return newWorkingCopies[i];
//      }
//    }
//    return null;
//  }
//
//  private static void addReferenceShadowedError(DartUnit cu, SearchMatch newMatch,
//      String newElementName, RefactoringStatus result) {
//    //Found a new match with no corresponding old match.
//    //-> The new match is a reference which was pointing to another element,
//    //but that other element has been shadowed
//
//    //TODO: should not have to filter declarations:
//    if (newMatch instanceof MethodDeclarationMatch || newMatch instanceof FieldDeclarationMatch) {
//      return;
//    }
//    SourceRange range = getOldSourceRange(newMatch);
//    RefactoringStatusContext context = JavaStatusContext.create(cu, range);
//    String message = Messages.format(
//        RefactoringCoreMessages.RenameAnalyzeUtil_reference_shadowed,
//        new String[] {
//            BasicElementLabels.getFileName(cu),
//            BasicElementLabels.getJavaElementName(newElementName)});
//    result.addError(message, context);
//  }
//
//  private static void addShadowsError(DartUnit cu, SearchMatch oldMatch, RefactoringStatus result) {
//    // Old match not found in new matches -> reference has been shadowed
//
//    //TODO: should not have to filter declarations:
//    if (oldMatch instanceof MethodDeclarationMatch || oldMatch instanceof FieldDeclarationMatch) {
//      return;
//    }
//    SourceRange range = new SourceRange(oldMatch.getOffset(), oldMatch.getLength());
//    RefactoringStatusContext context = JavaStatusContext.create(cu, range);
//    String message = Messages.format(RefactoringCoreMessages.RenameAnalyzeUtil_shadows,
//        BasicElementLabels.getFileName(cu));
//    result.addError(message, context);
//  }
//
//  private static void analyzeChanges(DartUnit cu, TextChange change, SearchMatch[] oldMatches,
//      SearchMatch[] newMatches, String newElementName, RefactoringStatus result) {
//    Map<Integer, SearchMatch> updatedOldOffsets = getUpdatedChangeOffsets(change, oldMatches);
//    for (int i = 0; i < newMatches.length; i++) {
//      SearchMatch newMatch = newMatches[i];
//      Integer offsetInNew = new Integer(newMatch.getSourceRange().getOffset());
//      SearchMatch oldMatch = updatedOldOffsets.remove(offsetInNew);
//      if (oldMatch == null) {
//        addReferenceShadowedError(cu, newMatch, newElementName, result);
//      }
//    }
//    for (Iterator<SearchMatch> iter = updatedOldOffsets.values().iterator(); iter.hasNext();) {
//      // remaining old matches are not found any more -> they have been shadowed
//      SearchMatch oldMatch = iter.next();
//      addShadowsError(cu, oldMatch, result);
//    }
//  }
//
//  private static RefactoringStatus analyzeCompileErrors(String newCuSource, DartUnit newCUNode,
//      DartUnit oldCUNode) {
//    RefactoringStatus result = new RefactoringStatus();
//    IProblem[] newProblems = RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode,
//        oldCUNode);
//    for (int i = 0; i < newProblems.length; i++) {
//      IProblem problem = newProblems[i];
//      if (problem.isError()) {
//        result.addEntry(new RefactoringStatusEntry(problem.isError() ? RefactoringStatus.ERROR
//            : RefactoringStatus.WARNING, problem.getMessage(), new JavaStringStatusContext(
//            newCuSource, SourceRangeFactory.create(problem))));
//      }
//    }
//    return result;
//  }
//
////--- find missing changes in BOTH directions
//
//  private static IRegion createTextRange(SearchMatch searchResult) {
//    return new Region(searchResult.getOffset(), searchResult.getLength());
//  }
//
//  private static boolean existsInNewOccurrences(SearchMatch searchResult,
//      SearchResultGroup[] newOccurrences, TextChangeManager manager) {
//    SearchResultGroup newGroup = findOccurrenceGroup(searchResult.getResource(), newOccurrences);
//    if (newGroup == null) {
//      return false;
//    }
//
//    IRegion oldEditRange = getCorrespondingEditChangeRange(searchResult, manager);
//    if (oldEditRange == null) {
//      return false;
//    }
//
//    SearchMatch[] newSearchResults = newGroup.getSearchResults();
//    int oldRangeOffset = oldEditRange.getOffset();
//    for (int i = 0; i < newSearchResults.length; i++) {
//      if (newSearchResults[i].getOffset() == oldRangeOffset) {
//        return true;
//      }
//    }
//    return false;
//  }
//
//  private static SearchResultGroup findOccurrenceGroup(IResource resource,
//      SearchResultGroup[] newOccurrences) {
//    for (int i = 0; i < newOccurrences.length; i++) {
//      if (newOccurrences[i].getResource().equals(resource)) {
//        return newOccurrences[i];
//      }
//    }
//    return null;
//  }
//
//  private static IRegion getCorrespondingEditChangeRange(SearchMatch searchResult,
//      TextChangeManager manager) {
//    TextChange change = getTextChange(searchResult, manager);
//    if (change == null) {
//      return null;
//    }
//
//    IRegion oldMatchRange = createTextRange(searchResult);
//    TextEditChangeGroup[] editChanges = change.getTextEditChangeGroups();
//    for (int i = 0; i < editChanges.length; i++) {
//      if (oldMatchRange.equals(editChanges[i].getRegion())) {
//        return TextEdit.getCoverage(change.getPreviewEdits(editChanges[i].getTextEdits()));
//      }
//    }
//    return null;
//  }
//
//  private static DartVariable getDartVariable(Name node) {
//    IBinding binding = node.resolveBinding();
//    if (binding == null && node.getParent() instanceof DartVariable) {
//      return (DartVariable) node.getParent();
//    }
//
//    if (binding != null && binding.getKind() == IBinding.VARIABLE) {
//      DartUnit cu = (DartUnit) ASTNodes.getParent(node, DartUnit.class);
//      return ASTNodes.findDartVariable((IVariableBinding) binding, cu);
//    }
//    return null;
//  }
//
//  /**
//   * @param change
//   * @return Map &lt;Integer oldOffset, Integer updatedOffset&gt;
//   */
//  private static Map<Integer, Integer> getEditChangeOffsetUpdates(TextChange change) {
//    TextEditChangeGroup[] editChanges = change.getTextEditChangeGroups();
//    Map<Integer, Integer> offsetUpdates = new HashMap<Integer, Integer>(editChanges.length);
//    for (int i = 0; i < editChanges.length; i++) {
//      TextEditChangeGroup editChange = editChanges[i];
//      IRegion oldRegion = editChange.getRegion();
//      if (oldRegion == null) {
//        continue;
//      }
//      IRegion updatedRegion = TextEdit.getCoverage(change.getPreviewEdits(editChange.getTextEdits()));
//      if (updatedRegion == null) {
//        continue;
//      }
//
//      offsetUpdates.put(new Integer(oldRegion.getOffset()), new Integer(updatedRegion.getOffset()));
//    }
//    return offsetUpdates;
//  }
//
//  private static DartNode getEnclosingBlockOrMethod(TextEdit declarationEdit, TextChange change,
//      DartUnit newCUNode) {
//    DartNode enclosing = RefactoringAnalyzeUtil.getBlock(declarationEdit, change, newCUNode);
//    if (enclosing == null) {
//      enclosing = RefactoringAnalyzeUtil.getMethodDeclaration(declarationEdit, change, newCUNode);
//    }
//    return enclosing;
//  }
//
//  private static SourceRange getOldSourceRange(SearchMatch newMatch) {
//    // cannot transfom offset in preview to offset in original -> just show enclosing method
//    IJavaElement newMatchElement = (IJavaElement) newMatch.getElement();
//    IJavaElement primaryElement = newMatchElement.getPrimaryElement();
//    SourceRange range = null;
//    if (primaryElement.exists() && primaryElement instanceof SourceReference) {
//      try {
//        range = ((SourceReference) primaryElement).getSourceRange();
//      } catch (DartModelException e) {
//        // can live without source range
//      }
//    }
//    return range;
//  }
//
//  private static TextChange getTextChange(SearchMatch searchResult, TextChangeManager manager) {
//    DartUnit cu = SearchUtils.getCompilationUnit(searchResult);
//    if (cu == null) {
//      return null;
//    }
//    return manager.get(cu);
//  }
//
//  /**
//   * @param change
//   * @param oldMatches
//   * @return Map &lt;Integer updatedOffset, SearchMatch oldMatch&gt;
//   */
//  private static Map<Integer, SearchMatch> getUpdatedChangeOffsets(TextChange change,
//      SearchMatch[] oldMatches) {
//    Map<Integer, SearchMatch> updatedOffsets = new HashMap<Integer, SearchMatch>();
//    Map<Integer, Integer> oldToUpdatedOffsets = getEditChangeOffsetUpdates(change);
//    for (int i = 0; i < oldMatches.length; i++) {
//      SearchMatch oldMatch = oldMatches[i];
//      Integer updatedOffset = oldToUpdatedOffsets.get(new Integer(oldMatch.getOffset()));
//      if (updatedOffset == null) {
//        updatedOffset = new Integer(-1); //match not updated
//      }
//      updatedOffsets.put(updatedOffset, oldMatch);
//    }
//    return updatedOffsets;
//  }

  private RenameAnalyzeUtil() {
    //no instance
  }
}
