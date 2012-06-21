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

package com.google.dart.tools.internal.corext.codemanipulation;

import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.problem.DefaultProblem;
import com.google.dart.tools.core.internal.problem.ProblemSeverities;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.problem.Problem;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.StubUtility;
import com.google.dart.tools.ui.internal.text.dart.ImportRewrite;
import com.google.dart.tools.ui.internal.text.editor.ASTProvider;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Organize Imports runnable
 */
public class OrganizeImportsOperation implements IWorkspaceRunnable {

//  private static class TypeReferenceProcessor {

//    private static class UnresolvedTypeData {
////      final SimpleName ref;
//      final int typeKinds;
//      final List<SearchMatch> foundInfos;
//
//      public UnresolvedTypeData(SimpleName ref) {
//        this.ref= ref;
//        this.typeKinds= ASTResolving.getPossibleTypeKinds(ref, true);
//        this.foundInfos= new ArrayList<SearchMatch>(3);
//      }
//
//      public void addInfo(SearchMatch info) {
//        for (int i= this.foundInfos.size() - 1; i >= 0; i--) {
//          TypeNameMatch curr= this.foundInfos.get(i);
//          if (curr.getTypeContainerName().equals(info.getTypeContainerName())) {
//            return; // not added. already contains type with same name
//          }
//        }
//        foundInfos.add(info);
//      }
//    }
//
//    private Set<String> fOldSingleImports;
//    private Set<String> fOldDemandImports;
//
//    private Set<String> fImplicitImports;
//
//    private ImportRewrite fImpStructure;
//
//    private boolean fDoIgnoreLowerCaseNames;
//
//    private IPackageFragment fCurrPackage;
//
//    private ScopeAnalyzer fAnalyzer;
//    private boolean fAllowDefaultPackageImports;
//
//    private Map<String, UnresolvedTypeData> fUnresolvedTypes;
//    private Set<String> fImportsAdded;
//    private TypeNameMatch[][] fOpenChoices;
//    private SourceRange[] fSourceRanges;
//
//
//    public TypeReferenceProcessor(Set<String> oldSingleImports, Set<String> oldDemandImports, CompilationUnit root, ImportRewrite impStructure, boolean ignoreLowerCaseNames) {
//      fOldSingleImports= oldSingleImports;
//      fOldDemandImports= oldDemandImports;
//      fImpStructure= impStructure;
//      fDoIgnoreLowerCaseNames= ignoreLowerCaseNames;
//
//      ICompilationUnit cu= impStructure.getCompilationUnit();
//
//      fImplicitImports= new HashSet<String>(3);
//      fImplicitImports.add(""); //$NON-NLS-1$
//      fImplicitImports.add("java.lang"); //$NON-NLS-1$
//      fImplicitImports.add(cu.getParent().getElementName());
//
//      fAnalyzer= new ScopeAnalyzer(root);
//
//      fCurrPackage= (IPackageFragment) cu.getParent();
//
//      fAllowDefaultPackageImports= cu.getJavaProject().getOption(JavaCore.COMPILER_SOURCE, true).equals(JavaCore.VERSION_1_3);
//
//      fImportsAdded= new HashSet<String>();
//      fUnresolvedTypes= new HashMap<String, UnresolvedTypeData>();
//    }
//
//    private boolean needsImport(ITypeBinding typeBinding, SimpleName ref) {
//      if (!typeBinding.isTopLevel() && !typeBinding.isMember() || typeBinding.isRecovered()) {
//        return false; // no imports for anonymous, local, primitive types or parameters types
//      }
//      int modifiers= typeBinding.getModifiers();
//      if (Modifier.isPrivate(modifiers)) {
//        return false; // imports for privates are not required
//      }
//      ITypeBinding currTypeBinding= Bindings.getBindingOfParentType(ref);
//      if (currTypeBinding == null) {
//        if (ASTNodes.getParent(ref, ASTNode.PACKAGE_DECLARATION) != null) {
//          return true; // reference in package-info.java
//        }
//        return false; // not in a type
//      }
//      if (!Modifier.isPublic(modifiers)) {
//        if (!currTypeBinding.getPackage().getName().equals(typeBinding.getPackage().getName())) {
//          return false; // not visible
//        }
//      }
//
//      ASTNode parent= ref.getParent();
//      while (parent instanceof Type) {
//        parent= parent.getParent();
//      }
//      if (parent instanceof AbstractTypeDeclaration && parent.getParent() instanceof CompilationUnit) {
//        return true;
//      }
//
//      if (typeBinding.isMember()) {
//        if (fAnalyzer.isDeclaredInScope(typeBinding, ref, ScopeAnalyzer.TYPES | ScopeAnalyzer.CHECK_VISIBILITY))
//          return false;
//      }
//      return true;
//    }
//
//
//    /**
//     * Tries to find the given type name and add it to the import structure.
//     * @param ref the name node
//     */
//    public void add(SimpleName ref) {
//      String typeName= ref.getIdentifier();
//
//      if (fImportsAdded.contains(typeName)) {
//        return;
//      }
//
//      IBinding binding= ref.resolveBinding();
//      if (binding != null) {
//        if (binding.getKind() != IBinding.TYPE) {
//          return;
//        }
//        ITypeBinding typeBinding= (ITypeBinding) binding;
//        if (typeBinding.isArray()) {
//          typeBinding= typeBinding.getElementType();
//        }
//        typeBinding= typeBinding.getTypeDeclaration();
//        if (!typeBinding.isRecovered()) {
//          if (needsImport(typeBinding, ref)) {
//            fImpStructure.addImport(typeBinding);
//            fImportsAdded.add(typeName);
//          }
//          return;
//        }
//      } else {
//        if (fDoIgnoreLowerCaseNames && typeName.length() > 0) {
//          char ch= typeName.charAt(0);
//          if (Strings.isLowerCase(ch) && Character.isLetter(ch)) {
//            return;
//          }
//        }
//      }
//      fImportsAdded.add(typeName);
//      fUnresolvedTypes.put(typeName, new UnresolvedTypeData(ref));
//    }
//
//    public boolean process(IProgressMonitor monitor) throws JavaModelException {
//      try {
//        int nUnresolved= fUnresolvedTypes.size();
//        if (nUnresolved == 0) {
//          return false;
//        }
//        char[][] allTypes= new char[nUnresolved][];
//        int i= 0;
//        for (Iterator<String> iter= fUnresolvedTypes.keySet().iterator(); iter.hasNext();) {
//          allTypes[i++]= iter.next().toCharArray();
//        }
//        final ArrayList<TypeNameMatch> typesFound= new ArrayList<TypeNameMatch>();
//        final IJavaProject project= fCurrPackage.getJavaProject();
//        IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { project });
//        TypeNameMatchCollector collector= new TypeNameMatchCollector(typesFound);
//        new SearchEngine().searchAllTypeNames(null, allTypes, scope, collector, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);
//
//        boolean is50OrHigher=   JavaModelUtil.is50OrHigher(project);
//
//        for (i= 0; i < typesFound.size(); i++) {
//          TypeNameMatch curr= typesFound.get(i);
//          UnresolvedTypeData data= fUnresolvedTypes.get(curr.getSimpleTypeName());
//          if (data != null && isVisible(curr) && isOfKind(curr, data.typeKinds, is50OrHigher)) {
//            if (fAllowDefaultPackageImports || curr.getPackageName().length() > 0) {
//              data.addInfo(curr);
//            }
//          }
//        }
//
//        ArrayList<TypeNameMatch[]> openChoices= new ArrayList<TypeNameMatch[]>(nUnresolved);
//        ArrayList<SourceRange> sourceRanges= new ArrayList<SourceRange>(nUnresolved);
//        for (Iterator<UnresolvedTypeData> iter= fUnresolvedTypes.values().iterator(); iter.hasNext();) {
//          UnresolvedTypeData data= iter.next();
//          TypeNameMatch[] openChoice= processTypeInfo(data.foundInfos);
//          if (openChoice != null) {
//            openChoices.add(openChoice);
//            sourceRanges.add(new SourceRange(data.ref.getStartPosition(), data.ref.getLength()));
//          }
//        }
//        if (openChoices.isEmpty()) {
//          return false;
//        }
//        fOpenChoices= openChoices.toArray(new TypeNameMatch[openChoices.size()][]);
//        fSourceRanges= sourceRanges.toArray(new SourceRange[sourceRanges.size()]);
//        return true;
//      } finally {
//        monitor.done();
//      }
//    }
//
//    private TypeNameMatch[] processTypeInfo(List<TypeNameMatch> typeRefsFound) {
//      int nFound= typeRefsFound.size();
//      if (nFound == 0) {
//        // nothing found
//        return null;
//      } else if (nFound == 1) {
//        TypeNameMatch typeRef= typeRefsFound.get(0);
//        fImpStructure.addImport(typeRef.getFullyQualifiedName());
//        return null;
//      } else {
//        String typeToImport= null;
//        boolean ambiguousImports= false;
//
//        // multiple found, use old imports to find an entry
//        for (int i= 0; i < nFound; i++) {
//          TypeNameMatch typeRef= typeRefsFound.get(i);
//          String fullName= typeRef.getFullyQualifiedName();
//          String containerName= typeRef.getTypeContainerName();
//          if (fOldSingleImports.contains(fullName)) {
//            // was single-imported
//            fImpStructure.addImport(fullName);
//            return null;
//          } else if (fOldDemandImports.contains(containerName) || fImplicitImports.contains(containerName)) {
//            if (typeToImport == null) {
//              typeToImport= fullName;
//            } else {  // more than one import-on-demand
//              ambiguousImports= true;
//            }
//          }
//        }
//
//        if (typeToImport != null && !ambiguousImports) {
//          fImpStructure.addImport(typeToImport);
//          return null;
//        }
//        // return the open choices
//        return typeRefsFound.toArray(new TypeNameMatch[nFound]);
//      }
//    }
//
//    private boolean isOfKind(TypeNameMatch curr, int typeKinds, boolean is50OrHigher) {
//      int flags= curr.getModifiers();
//      if (Flags.isAnnotation(flags)) {
//        return is50OrHigher && (typeKinds & SimilarElementsRequestor.ANNOTATIONS) != 0;
//      }
//      if (Flags.isEnum(flags)) {
//        return is50OrHigher && (typeKinds & SimilarElementsRequestor.ENUMS) != 0;
//      }
//      if (Flags.isInterface(flags)) {
//        return (typeKinds & SimilarElementsRequestor.INTERFACES) != 0;
//      }
//      return (typeKinds & SimilarElementsRequestor.CLASSES) != 0;
//    }
//
//    private boolean isVisible(TypeNameMatch curr) {
//      int flags= curr.getModifiers();
//      if (Flags.isPrivate(flags)) {
//        return false;
//      }
//      if (Flags.isPublic(flags) || Flags.isProtected(flags)) {
//        return true;
//      }
//      return curr.getPackageName().equals(fCurrPackage.getElementName());
//    }
//
//    public TypeNameMatch[][] getChoices() {
//      return fOpenChoices;
//    }
//
//    public ISourceRange[] getChoicesSourceRanges() {
//      return fSourceRanges;
//    }
//  }

  private CompilationUnit compilationUnit;
  private Problem parsingError;
  private int numberOfImportsAdded;
  private int numberOfImportsRemoved;
  private DartUnit astRoot;
  private boolean allowSyntaxErrors;
  private boolean doSave;

  DartProject project;

  public OrganizeImportsOperation(
      CompilationUnit cu, DartUnit astRoot, boolean save, boolean allowSyntaxErrors) {

    compilationUnit = cu;
    this.astRoot = astRoot;

    doSave = save;

    this.allowSyntaxErrors = allowSyntaxErrors;

    numberOfImportsAdded = 0;
    numberOfImportsRemoved = 0;

    parsingError = null;
    project = compilationUnit.getDartProject();

  }

  public TextEdit createTextEdit(IProgressMonitor monitor)
      throws CoreException, OperationCanceledException {
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }
    try {
      numberOfImportsAdded = 0;
      numberOfImportsRemoved = 0;

      monitor.beginTask(
          NLS.bind(
              DartUIMessages.OrganizeImportsOperation_description,
              BasicElementLabels.getFileName(compilationUnit)),
          9);

      DartUnit dartUnit = astRoot;
      if (dartUnit == null) {
        dartUnit = DartToolsPlugin.getDefault().getASTProvider().getAST(
            compilationUnit.getPrimaryElement(),
            ASTProvider.WAIT_YES,
            new SubProgressMonitor(monitor, 2));

        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
      } else {
        monitor.worked(2);
      }

      ImportRewrite importsRewrite = StubUtility.createImportRewrite(
          compilationUnit,
          dartUnit,
          false);

      Set<String> oldSingleImports = new HashSet<String>();
      Set<String> oldDemandImports = new HashSet<String>();
      List<String> typeReferences = new ArrayList<String>();
      List<String> staticReferences = new ArrayList<String>();

      if (!collectReferences(compilationUnit, typeReferences, staticReferences, oldSingleImports)) {
        return null;
      }

      monitor.worked(1);

//      TypeReferenceProcessor processor = new TypeReferenceProcessor(
//          oldSingleImports, oldDemandImports, dartUnit, importsRewrite);
//
//      Iterator<SimpleName> refIterator = typeReferences.iterator();
//      while (refIterator.hasNext()) {
//        SimpleName typeRef = refIterator.next();
//        processor.add(typeRef);
//      }
//
//      boolean hasOpenChoices = processor.process(new SubProgressMonitor(monitor, 3));

//
//    if (hasOpenChoices && fChooseImportQuery != null) {
//      TypeNameMatch[][] choices = processor.getChoices();
//      ISourceRange[] ranges = processor.getChoicesSourceRanges();
//      TypeNameMatch[] chosen = fChooseImportQuery.chooseImports(choices, ranges);
//      if (chosen == null) {
//        // cancel pressed by the user
//        throw new OperationCanceledException();
//      }
//      for (int i = 0; i < chosen.length; i++) {
//        TypeNameMatch typeInfo = chosen[i];
//        importsRewrite.addImport(typeInfo.getFullyQualifiedName());
//      }
//    }

      TextEdit result = importsRewrite.rewriteImports(new SubProgressMonitor(monitor, 3));

      determineImportDifferences(importsRewrite, oldSingleImports, oldDemandImports);

      return result;
    } finally {
      monitor.done();
    }
  }

  public int getNumberOfImportsAdded() {
    return numberOfImportsAdded;
  }

  public int getNumberOfImportsRemoved() {
    return numberOfImportsRemoved;
  }

  /**
   * After executing the operation, returns <code>null</code> if the operation has been executed
   * successfully or the range where parsing failed.
   * 
   * @return returns the parse error
   */
  public Problem getParseError() {
    return parsingError;
  }

  /**
   * @return Returns the scheduling rule for this operation
   */
  public ISchedulingRule getScheduleRule() {
    return compilationUnit.getResource();
  }

  @Override
  public void run(IProgressMonitor monitor) throws CoreException {

    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }
    try {
      monitor.beginTask(
          NLS.bind(
              DartUIMessages.OrganizeImportsOperation_description,
              BasicElementLabels.getFileName(compilationUnit)),
          10);

      TextEdit edit = createTextEdit(new SubProgressMonitor(monitor, 9));
      if (edit == null) {
        return;
      }

      DartModelUtil.applyEdit(compilationUnit, edit, doSave, new SubProgressMonitor(monitor, 1));
    } finally {
      monitor.done();
    }

  }

  //find type references in a compilation unit
  private boolean collectReferences(CompilationUnit cu, List<String> typeReferences,
      List<String> staticReferences, Set<String> oldSingleImports) {
    if (!allowSyntaxErrors) {
      IMarker[] problems;
      try {
        problems = cu.getCorrespondingResource()
            .findMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_ZERO);

        for (int i = 0; i < problems.length; i++) {
          IMarker curr = problems[i];
          if (curr.getAttribute(IMarker.SEVERITY) == new Integer(IMarker.SEVERITY_ERROR)) {
            parsingError = new DefaultProblem(
                cu.getElementName().toCharArray(),
                (String) curr.getAttribute(IMarker.MESSAGE),
                0,
                new String[0],
                ProblemSeverities.Error,
                curr.getAttribute(IMarker.CHAR_START, 0),
                curr.getAttribute(IMarker.CHAR_END, 0),
                curr.getAttribute(IMarker.LINE_NUMBER, 0),
                curr.getAttribute(IMarker.CHAR_START, 0));

            return false;
          }
        }
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
      } catch (CoreException e) {
        DartToolsPlugin.log(e);
      }
    }
    List<DartDirective> directives = astRoot.getDirectives();
    for (DartDirective directive : directives) {
      if (directive instanceof DartImportDirective) {
        String id = ((DartImportDirective) directive).getLibraryUri().getValue();
        oldSingleImports.add(id);
      }
    }

    project = compilationUnit.getDartProject();
    //   ImportReferencesCollector.collect(astRoot, project, null, typeReferences, staticReferences);

    return true;

  }

  private void determineImportDifferences(
      ImportRewrite importsStructure, Set<String> oldSingleImports, Set<String> oldDemandImports) {

    ArrayList<String> importsAdded = new ArrayList<String>();
    //   importsAdded.addAll(Arrays.asList(importsStructure.getCreatedImports()));

    Object[] content = oldSingleImports.toArray();
    for (int i = 0; i < content.length; i++) {
      String importName = (String) content[i];
      if (importsAdded.remove(importName)) {
        oldSingleImports.remove(importName);
      }
    }

    numberOfImportsAdded = importsAdded.size();
    numberOfImportsRemoved = oldSingleImports.size() + oldDemandImports.size();
  }
}
