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
package com.google.dart.tools.internal.corext.refactoring.code;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartClassMember;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.resolver.ElementKind;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.engine.scanner.Token;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.internal.corext.dom.ASTNodes;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.internal.text.Selection;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Extracts a method in a compilation unit based on a text selection range.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExtractMethodRefactoring extends Refactoring {
  /**
   * Description of the single occurrence of the selected expression or set of statements.
   */
  private static class Occurrence {
    final SourceRange range;
    final boolean isSelection;
    final Map<String, String> parameterOldToOccurrenceName = Maps.newHashMap();

    public Occurrence(SourceRange range, boolean isSelection) {
      this.range = range;
      this.isSelection = isSelection;
    }
  }

  /**
   * Generalized version of some source, in which references to the specific variables are replaced
   * with pattern variables, with back mapping from pattern to original variable names.
   */
  private static class SourcePattern {
    final Map<String, String> originalToPatternNames = Maps.newHashMap();
    String patternSource;
  }

  private static final String TOKEN_SEPARATOR = "\uFFFF";

  /**
   * @return the "normalized" version of the given source, which is built form tokens, so ignores
   *         all comments and spaces.
   */
  private static String getNormalizedSource(String s) {
    List<Token> selectionTokens = ExtractUtils.tokenizeSource(s);
    return StringUtils.join(selectionTokens, TOKEN_SEPARATOR);
  }

  //  private static class UsedNamesCollector extends ASTVisitor {
//    public static Set<String> perform(ASTNode[] nodes) {
//      UsedNamesCollector collector = new UsedNamesCollector();
//      for (int i = 0; i < nodes.length; i++) {
//        nodes[i].accept(collector);
//      }
//      return collector.result;
//    }
//
//    private Set<String> result = new HashSet<String>();
//    private Set<SimpleName> fIgnore = new HashSet<SimpleName>();
//
//    @Override
//    public void endVisit(FieldAccess node) {
//      fIgnore.remove(node.getName());
//    }
//
//    @Override
//    public void endVisit(MethodInvocation node) {
//      fIgnore.remove(node.getName());
//    }
//
//    @Override
//    public void endVisit(QualifiedName node) {
//      fIgnore.remove(node.getName());
//    }
//
//    @Override
//    public boolean visit(AnnotationTypeDeclaration node) {
//      return visitType(node);
//    }
//
//    @Override
//    public boolean visit(EnumDeclaration node) {
//      return visitType(node);
//    }
//
//    @Override
//    public boolean visit(FieldAccess node) {
//      Expression exp = node.getExpression();
//      if (exp != null) {
//        fIgnore.add(node.getName());
//      }
//      return true;
//    }
//
//    @Override
//    public boolean visit(MethodInvocation node) {
//      Expression exp = node.getExpression();
//      if (exp != null) {
//        fIgnore.add(node.getName());
//      }
//      return true;
//    }
//
//    @Override
//    public boolean visit(QualifiedName node) {
//      fIgnore.add(node.getName());
//      return true;
//    }
//
//    @Override
//    public boolean visit(SimpleName node) {
//      if (!fIgnore.contains(node)) {
//        result.add(node.getIdentifier());
//      }
//      return true;
//    }
//
//    @Override
//    public boolean visit(TypeDeclaration node) {
//      return visitType(node);
//    }
//
//    private boolean visitType(AbstractTypeDeclaration node) {
//      result.add(node.getName().getIdentifier());
//      // don't dive into type declaration since they open a new
//      // context.
//      return false;
//    }
//  }
//
//  private static final String ATTRIBUTE_VISIBILITY = "visibility"; //$NON-NLS-1$
//  private static final String ATTRIBUTE_DESTINATION = "destination"; //$NON-NLS-1$
//  private static final String ATTRIBUTE_COMMENTS = "comments"; //$NON-NLS-1$
//  private static final String ATTRIBUTE_REPLACE = "replace"; //$NON-NLS-1$
//
//  private static final String ATTRIBUTE_EXCEPTIONS = "exceptions"; //$NON-NLS-1$
  private final CompilationUnit unit;
  private final int selectionStart;
  private final int selectionLength;

  private final SourceRange selectionRange;
  private final CompilationUnitChange change;
  private ExtractUtils utils;
  private DartUnit unitNode;
  private final List<ParameterInfo> parameters = Lists.newArrayList();

  private SelectionAnalyzer selectionAnalyzer;
  private final Map<String, List<SourceRange>> selectionParametersToRanges = Maps.newHashMap();
  private DartExpression selectionExpression;

  private final List<Occurrence> occurrences = Lists.newArrayList();
  private String fMethodName;

  private boolean replaceAllOccurrences = true;

  //  private AST fAST;
//  private ASTRewrite fRewriter;
//  private ExtractMethodAnalyzer fAnalyzer;
//  private int fVisibility;
//  private boolean fThrowRuntimeExceptions;
//  private List<ParameterInfo> fParameterInfos;
//  private Set<String> fUsedNames;
//  private boolean fGenerateJavadoc;
//  private boolean fReplaceDuplicates;
//  private SnippetFinder.Match[] fDuplicates;
//  private int fDestinationIndex = 0;
//  // either of type TypeDeclaration or AnonymousClassDeclaration
//  private ASTNode fDestination;
//  // either of type TypeDeclaration or AnonymousClassDeclaration
//  private ASTNode[] fDestinations;
//
//  private LinkedProposalModel fLinkedProposalModel;
//
  private static final String EMPTY = ""; //$NON-NLS-1$
//  private static final String KEY_TYPE = "type"; //$NON-NLS-1$
//
//  private static final String KEY_NAME = "name"; //$NON-NLS-1$

  public ExtractMethodRefactoring(CompilationUnit unit, int selectionStart, int selectionLength) {
    this.unit = unit;
    this.selectionStart = selectionStart;
    this.selectionLength = selectionLength;
    this.selectionRange = SourceRangeFactory.forStartLength(selectionStart, selectionLength);
    this.fMethodName = "extracted"; //$NON-NLS-1$
    {
      this.change = new CompilationUnitChange(unit.getElementName(), unit);
      change.setEdit(new MultiTextEdit());
      change.setKeepPreviewEdits(true);
    }
  }

//
//  /**
//   * Creates a new extract method refactoring
//   * 
//   * @param astRoot the AST root of an AST created from a compilation unit
//   * @param selectionStart start
//   * @param selectionLength length
//   */
//  public ExtractMethodRefactoring(DartUnit astRoot, int selectionStart, int selectionLength) {
//    this((CompilationUnit) astRoot.getTypeRoot(), selectionStart, selectionLength);
//    fRoot = astRoot;
//  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.ExtractMethodRefactoring_checking_new_name, 2);
    pm.subTask(EMPTY);

    RefactoringStatus result = checkMethodName();
    // TODO(scheglov)
//    result.merge(checkParameterNames());
//    result.merge(checkVarargOrder());
//    pm.worked(1);
//    if (pm.isCanceled()) {
//      throw new OperationCanceledException();
//    }
//
//    BodyDeclaration node = fAnalyzer.getEnclosingBodyDeclaration();
//    if (node != null) {
//      fAnalyzer.checkInput(result, fMethodName, fDestination);
//      pm.worked(1);
//    }
    pm.done();
    return result;
  }

  /**
   * Checks if the refactoring can be activated. Activation typically means, if a corresponding menu
   * entry can be added to the UI.
   * 
   * @param pm a progress monitor to report progress during activation checking.
   * @return the refactoring status describing the result of the activation check.
   * @throws CoreException if checking fails
   */
  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask("", 4); //$NON-NLS-1$
      RefactoringStatus result = new RefactoringStatus();
      // prepare AST
      utils = new ExtractUtils(unit);
      unitNode = utils.getUnitNode();
      pm.worked(1);
      // check selection
      result.merge(checkSelection(new SubProgressMonitor(pm, 3)));
      // prepare elements
      initializeParameters();
      initializeOccurrences();
      // done
      return result;
    } finally {
      pm.done();
    }
//    RefactoringStatus result = new RefactoringStatus();
//    pm.beginTask("", 100); //$NON-NLS-1$
//
//    if (fSelectionStart < 0 || fSelectionLength == 0) {
//      return mergeTextSelectionStatus(result);
//    }
//
//    IFile[] changedFiles = ResourceUtil.getFiles(new CompilationUnit[] {fCUnit});
//    result.merge(Checks.validateModifiesFiles(changedFiles, getValidationContext()));
//    if (result.hasFatalError()) {
//      return result;
//    }
//    result.merge(ResourceChangeChecker.checkFilesToBeChanged(changedFiles, new SubProgressMonitor(
//        pm,
//        1)));
//
//    if (fRoot == null) {
//      fRoot = RefactoringASTParser.parseWithASTProvider(
//          fCUnit,
//          true,
//          new SubProgressMonitor(pm, 99));
//    }
//    fImportRewriter = StubUtility.createImportRewrite(fRoot, true);
//
//    fAST = fRoot.getAST();
//    fRoot.accept(createVisitor());
//
//    fSelectionStart = fAnalyzer.getSelection().getOffset();
//    fSelectionLength = fAnalyzer.getSelection().getLength();
//
//    result.merge(fAnalyzer.checkInitialConditions(fImportRewriter));
//    if (result.hasFatalError()) {
//      return result;
//    }
//    if (fVisibility == -1) {
//      setVisibility(Modifier.PRIVATE);
//    }
//    initializeParameterInfos();
//    initializeUsedNames();
//    initializeDuplicates();
//    initializeDestinations();
//    return result;
  }

  /**
   * Checks if the new method name is a valid method name. This method doesn't check if a method
   * with the same name already exists in the hierarchy. This check is done in
   * <code>checkInput</code> since it is expensive.
   * 
   * @return validation status
   */
  public RefactoringStatus checkMethodName() {
    return Checks.checkMethodName(fMethodName);
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException {
    // TODO(scheglov)
//    if (fMethodName == null) {
//      return null;
//    }
    pm.beginTask("", 2); //$NON-NLS-1$
    try {
      // add method declaration
      {
        // prepare environment
        DartClassMember<?> parentMember = ASTNodes.getParent(
            selectionExpression,
            DartClassMember.class);
        String prefix = utils.getNodePrefix(parentMember);
        String eol = utils.getEndOfLine();
        // prepare annotations
        String annotations = "";
        {
          // may be "static"
          if (parentMember.getModifiers().isStatic()) {
            annotations = "static ";
          }
          // add return type
          String returnTypeName = ExtractUtils.getTypeSource(selectionExpression);
          if (returnTypeName != null && !returnTypeName.equals("Dynamic")) {
            annotations += returnTypeName + " ";
          }
        }
        // prepare declaration source
        String returnExpressionSource = getReturnExpressionSource();
        String declarationSource = annotations + getSignature() + " => " + returnExpressionSource
            + ";";
        // insert declaration
        TextEdit edit = new ReplaceEdit(parentMember.getSourceInfo().getEnd(), 0, eol + prefix
            + declarationSource);
        change.addEdit(edit);
        change.addTextEditGroup(new TextEditGroup(Messages.format(
            RefactoringCoreMessages.ExtractMethodRefactoring_add_method,
            fMethodName), edit));
      }
      // replace occurrences with method invocation
      for (Occurrence occurence : occurrences) {
        SourceRange range = occurence.range;
        // may be replacement of duplicates disabled
        if (!replaceAllOccurrences && !occurence.isSelection) {
          continue;
        }
        // prepare invocation source
        String invocationSource;
        {
          StringBuilder sb = new StringBuilder();
          sb.append(fMethodName);
          sb.append("(");
          boolean firstParameter = true;
          for (ParameterInfo parameter : parameters) {
            // may be comma
            if (firstParameter) {
              firstParameter = false;
            } else {
              sb.append(", ");
            }
            // argument name
            {
              String parameterOldName = parameter.getOldName();
              String argumentName = occurence.parameterOldToOccurrenceName.get(parameterOldName);
              sb.append(argumentName);
            }
          }
          sb.append(")");
          invocationSource = sb.toString();
        }
        // add replace edit
        TextEdit edit = new ReplaceEdit(range.getOffset(), range.getLength(), invocationSource);
        change.addEdit(edit);
        String msg = Messages.format(occurence.isSelection
            ? RefactoringCoreMessages.ExtractMethodRefactoring_substitute_with_call
            : RefactoringCoreMessages.ExtractMethodRefactoring_duplicates_single, fMethodName);
        change.addTextEditGroup(new TextEditGroup(msg, edit));
      }
      // done
      return change;
      // XXX
//      fAnalyzer.aboutToCreateChange();
//      BodyDeclaration declaration = fAnalyzer.getEnclosingBodyDeclaration();
//      fRewriter = ASTRewrite.create(declaration.getAST());
//
//      final CompilationUnitChange result = new CompilationUnitChange(
//          RefactoringCoreMessages.ExtractMethodRefactoring_change_name,
//          fCUnit);
//      result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);
//      result.setDescriptor(new RefactoringChangeDescriptor(getRefactoringDescriptor()));
//
//      MultiTextEdit root = new MultiTextEdit();
//      result.setEdit(root);
//
//      ASTNode[] selectedNodes = fAnalyzer.getSelectedNodes();
//      fRewriter.setTargetSourceRangeComputer(new SelectionAwareSourceRangeComputer(
//          selectedNodes,
//          fCUnit.getBuffer(),
//          fSelectionStart,
//          fSelectionLength));
//
//      TextEditGroup substituteDesc = new TextEditGroup(Messages.format(
//          RefactoringCoreMessages.ExtractMethodRefactoring_substitute_with_call,
//          BasicElementLabels.getJavaElementName(fMethodName)));
//      result.addTextEditGroup(substituteDesc);
//
//      MethodDeclaration mm = createNewMethod(
//          selectedNodes,
//          fCUnit.findRecommendedLineSeparator(),
//          substituteDesc);
//
//      if (fLinkedProposalModel != null) {
//        LinkedProposalPositionGroup typeGroup = fLinkedProposalModel.getPositionGroup(
//            KEY_TYPE,
//            true);
//        typeGroup.addPosition(fRewriter.track(mm.getReturnType2()), false);
//
//        ITypeBinding typeBinding = fAnalyzer.getReturnTypeBinding();
//        if (typeBinding != null) {
//          ITypeBinding[] relaxingTypes = ASTResolving.getNarrowingTypes(fAST, typeBinding);
//          for (int i = 0; i < relaxingTypes.length; i++) {
//            typeGroup.addProposal(relaxingTypes[i], fCUnit, relaxingTypes.length - i);
//          }
//        }
//
//        LinkedProposalPositionGroup nameGroup = fLinkedProposalModel.getPositionGroup(
//            KEY_NAME,
//            true);
//        nameGroup.addPosition(fRewriter.track(mm.getName()), false);
//
//        ModifierCorrectionSubProcessor.installLinkedVisibilityProposals(
//            fLinkedProposalModel,
//            fRewriter,
//            mm.modifiers(),
//            false);
//      }
//
//      TextEditGroup insertDesc = new TextEditGroup(Messages.format(
//          RefactoringCoreMessages.ExtractMethodRefactoring_add_method,
//          BasicElementLabels.getJavaElementName(fMethodName)));
//      result.addTextEditGroup(insertDesc);
//
//      if (fDestination == fDestinations[0]) {
//        ChildListPropertyDescriptor desc = (ChildListPropertyDescriptor) declaration.getLocationInParent();
//        ListRewrite container = fRewriter.getListRewrite(declaration.getParent(), desc);
//        container.insertAfter(mm, declaration, insertDesc);
//      } else {
//        BodyDeclarationRewrite container = BodyDeclarationRewrite.create(fRewriter, fDestination);
//        container.insert(mm, insertDesc);
//      }
//
//      replaceDuplicates(result, mm.getModifiers());
//      replaceBranches(result);
//
//      if (fImportRewriter.hasRecordedChanges()) {
//        TextEdit edit = fImportRewriter.rewriteImports(null);
//        root.addChild(edit);
//        result.addTextEditGroup(new TextEditGroup(
//            RefactoringCoreMessages.ExtractMethodRefactoring_organize_imports,
//            new TextEdit[] {edit}));
//      }
//      root.addChild(fRewriter.rewriteAST());
//      return result;
    } finally {
      pm.done();
    }

  }

  @Override
  public String getName() {
    return RefactoringCoreMessages.ExtractMethodRefactoring_name;
  }

//  /**
//   * Checks if the parameter names are valid.
//   * 
//   * @return validation status
//   */
//  public RefactoringStatus checkParameterNames() {
//    RefactoringStatus result = new RefactoringStatus();
//    for (Iterator<ParameterInfo> iter = fParameterInfos.iterator(); iter.hasNext();) {
//      ParameterInfo parameter = iter.next();
//      result.merge(Checks.checkIdentifier(parameter.getNewName(), fCUnit));
//      for (Iterator<ParameterInfo> others = fParameterInfos.iterator(); others.hasNext();) {
//        ParameterInfo other = others.next();
//        if (parameter != other && other.getNewName().equals(parameter.getNewName())) {
//          result.addError(Messages.format(
//              RefactoringCoreMessages.ExtractMethodRefactoring_error_sameParameter,
//              BasicElementLabels.getJavaElementName(other.getNewName())));
//          return result;
//        }
//      }
//      if (parameter.isRenamed() && fUsedNames.contains(parameter.getNewName())) {
//        result.addError(Messages.format(
//            RefactoringCoreMessages.ExtractMethodRefactoring_error_nameInUse,
//            BasicElementLabels.getJavaElementName(parameter.getNewName())));
//        return result;
//      }
//    }
//    return result;
//  }
//
//  /**
//   * Checks if varargs are ordered correctly.
//   * 
//   * @return validation status
//   */
//  public RefactoringStatus checkVarargOrder() {
//    for (Iterator<ParameterInfo> iter = fParameterInfos.iterator(); iter.hasNext();) {
//      ParameterInfo info = iter.next();
//      if (info.isOldVarargs() && iter.hasNext()) {
//        return RefactoringStatus.createFatalErrorStatus(Messages.format(
//            RefactoringCoreMessages.ExtractMethodRefactoring_error_vararg_ordering,
//            BasicElementLabels.getJavaElementName(info.getOldName())));
//      }
//    }
//    return new RefactoringStatus();
//  }

  /**
   * @return the number of other occurrences of the same source as selection (but not including
   *         selection itself).
   */
  public int getNumberOfDuplicates() {
    return occurrences.size() - 1;
  }

  public List<ParameterInfo> getParameters() {
    return parameters;
  }

  public boolean getReplaceAllOccurrences() {
    return replaceAllOccurrences;
  }

//
//  public CompilationUnit getCompilationUnit() {
//    return fCUnit;
//  }
//
//  public ASTNode[] getDestinations() {
//    return fDestinations;
//  }
//
//  public boolean getGenerateJavadoc() {
//    return fGenerateJavadoc;
//  }
//
//  /**
//   * Returns the method name to be used for the extracted method.
//   * 
//   * @return the method name to be used for the extracted method.
//   */
//  public String getMethodName() {
//    return fMethodName;
//  }

//
//  /**
//   * Returns the number of duplicate code snippets found.
//   * 
//   * @return the number of duplicate code fragments
//   */
//  public int getNumberOfDuplicates() {
//    if (fDuplicates == null) {
//      return 0;
//    }
//    int result = 0;
//    for (int i = 0; i < fDuplicates.length; i++) {
//      if (!fDuplicates[i].isMethodBody()) {
//        result++;
//      }
//    }
//    return result;
//  }
//
//  /**
//   * Returns the parameter infos.
//   * 
//   * @return a list of parameter infos.
//   */
//  public List<ParameterInfo> getParameterInfos() {
//    return fParameterInfos;
//  }
//
//  public boolean getReplaceDuplicates() {
//    return fReplaceDuplicates;
//  }

  /**
   * @return the signature of the extracted method
   */
  public String getSignature() {
    return getSignature(fMethodName);
  }

  /**
   * @param methodName the method name used for the new method
   * @return the signature of the extracted method
   */
  public String getSignature(String methodName) {
    StringBuilder sb = new StringBuilder();
    sb.append(methodName);
    sb.append("(");
    // add all parameters
    boolean firstParameter = true;
    for (ParameterInfo parameter : parameters) {
      // may be comma
      if (firstParameter) {
        firstParameter = false;
      } else {
        sb.append(", ");
      }
      // type
      {
        String typeSource = parameter.getNewTypeName();
        if (!"Dynamic".equals(typeSource)) {
          sb.append(typeSource);
          sb.append(" ");
        }
      }
      // name
      sb.append(parameter.getNewName());
    }
    // done
    sb.append(")");
    return sb.toString();
  }

//  /**
//   * Returns the names already in use in the selected statements/expressions.
//   * 
//   * @return names already in use.
//   */
//  public Set<String> getUsedNames() {
//    return fUsedNames;
//  }
//
//  /**
//   * Returns the visibility of the new method.
//   * 
//   * @return the visibility of the new method
//   */
//  public int getVisibility() {
//    return fVisibility;
//  }
//
//  public void setDestination(int index) {
//    fDestination = fDestinations[index];
//    fDestinationIndex = index;
//  }
//
//  public void setGenerateJavadoc(boolean generate) {
//    fGenerateJavadoc = generate;
//  }
//
//  public void setLinkedProposalModel(LinkedProposalModel linkedProposalModel) {
//    fLinkedProposalModel = linkedProposalModel;
//  }

  /**
   * Sets the method name to be used for the extracted method.
   */
  public void setMethodName(String name) {
    fMethodName = name;
  }

  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    this.replaceAllOccurrences = replaceAllOccurrences;
  }

//  /**
//   * Sets whether the new method signature throws runtime exceptions.
//   * 
//   * @param throwRuntimeExceptions flag indicating if the new method throws runtime exceptions
//   */
//  public void setThrowRuntimeExceptions(boolean throwRuntimeExceptions) {
//    fThrowRuntimeExceptions = throwRuntimeExceptions;
//  }
//
//  /**
//   * Sets the visibility of the new method.
//   * 
//   * @param visibility the visibility of the new method. Valid values are "public", "protected", "",
//   *          and "private"
//   */
//  public void setVisibility(int visibility) {
//    fVisibility = visibility;
//  }
//
//  private ITypeBinding[] computeLocalTypeVariables() {
//    List<ITypeBinding> result = new ArrayList<ITypeBinding>(
//        Arrays.asList(fAnalyzer.getTypeVariables()));
//    for (int i = 0; i < fParameterInfos.size(); i++) {
//      ParameterInfo info = fParameterInfos.get(i);
//      processVariable(result, info.getOldBinding());
//    }
//    IVariableBinding[] methodLocals = fAnalyzer.getMethodLocals();
//    for (int i = 0; i < methodLocals.length; i++) {
//      processVariable(result, methodLocals[i]);
//    }
//    return result.toArray(new ITypeBinding[result.size()]);
//  }
//
//  private ASTNode[] createCallNodes(SnippetFinder.Match duplicate, int modifiers) {
//    List<ASTNode> result = new ArrayList<ASTNode>(2);
//
//    IVariableBinding[] locals = fAnalyzer.getCallerLocals();
//    for (int i = 0; i < locals.length; i++) {
//      result.add(createDeclaration(locals[i], null));
//    }
//
//    MethodInvocation invocation = fAST.newMethodInvocation();
//    invocation.setName(fAST.newSimpleName(fMethodName));
//    ASTNode typeNode = ASTResolving.findParentType(fAnalyzer.getEnclosingBodyDeclaration());
//    RefactoringStatus status = new RefactoringStatus();
//    while (fDestination != typeNode) {
//      fAnalyzer.checkInput(status, fMethodName, typeNode);
//      if (!status.isOK()) {
//        SimpleName destinationTypeName = fAST.newSimpleName(ASTNodes.getEnclosingType(fDestination).getName());
//        if ((modifiers & Modifier.STATIC) == 0) {
//          ThisExpression thisExpression = fAST.newThisExpression();
//          thisExpression.setQualifier(destinationTypeName);
//          invocation.setExpression(thisExpression);
//        } else {
//          invocation.setExpression(destinationTypeName);
//        }
//        break;
//      }
//      typeNode = typeNode.getParent();
//    }
//
//    List<Expression> arguments = invocation.arguments();
//    for (int i = 0; i < fParameterInfos.size(); i++) {
//      ParameterInfo parameter = fParameterInfos.get(i);
//      arguments.add(ASTNodeFactory.newName(fAST, getMappedName(duplicate, parameter)));
//    }
//    if (fLinkedProposalModel != null) {
//      LinkedProposalPositionGroup nameGroup = fLinkedProposalModel.getPositionGroup(KEY_NAME, true);
//      nameGroup.addPosition(fRewriter.track(invocation.getName()), false);
//    }
//
//    ASTNode call;
//    int returnKind = fAnalyzer.getReturnKind();
//    switch (returnKind) {
//      case ExtractMethodAnalyzer.ACCESS_TO_LOCAL:
//        IVariableBinding binding = fAnalyzer.getReturnLocal();
//        if (binding != null) {
//          VariableDeclarationStatement decl = createDeclaration(
//              getMappedBinding(duplicate, binding),
//              invocation);
//          call = decl;
//        } else {
//          Assignment assignment = fAST.newAssignment();
//          assignment.setLeftHandSide(ASTNodeFactory.newName(
//              fAST,
//              getMappedBinding(duplicate, fAnalyzer.getReturnValue()).getName()));
//          assignment.setRightHandSide(invocation);
//          call = assignment;
//        }
//        break;
//      case ExtractMethodAnalyzer.RETURN_STATEMENT_VALUE:
//        ReturnStatement rs = fAST.newReturnStatement();
//        rs.setExpression(invocation);
//        call = rs;
//        break;
//      default:
//        call = invocation;
//    }
//
//    if (call instanceof Expression && !fAnalyzer.isExpressionSelected()) {
//      call = fAST.newExpressionStatement((Expression) call);
//    }
//    result.add(call);
//
//    // We have a void return statement. The code looks like
//    // extracted();
//    // return;
//    if (returnKind == ExtractMethodAnalyzer.RETURN_STATEMENT_VOID
//        && !fAnalyzer.isLastStatementSelected()) {
//      result.add(fAST.newReturnStatement());
//    }
//    return result.toArray(new ASTNode[result.size()]);
//  }
//
//  //---- Helper methods ------------------------------------------------------------------------
//
//  private VariableDeclarationStatement createDeclaration(IVariableBinding binding,
//      Expression intilizer) {
//    VariableDeclaration original = ASTNodes.findVariableDeclaration(
//        binding,
//        fAnalyzer.getEnclosingBodyDeclaration());
//    VariableDeclarationFragment fragment = fAST.newVariableDeclarationFragment();
//    fragment.setName((SimpleName) ASTNode.copySubtree(fAST, original.getName()));
//    fragment.setInitializer(intilizer);
//    VariableDeclarationStatement result = fAST.newVariableDeclarationStatement(fragment);
//    result.modifiers().addAll(ASTNode.copySubtrees(fAST, ASTNodes.getModifiers(original)));
//    result.setType(ASTNodeFactory.newType(
//        fAST,
//        original,
//        fImportRewriter,
//        new ContextSensitiveImportRewriteContext(original, fImportRewriter)));
//    return result;
//  }
//
//  private Block createMethodBody(ASTNode[] selectedNodes, TextEditGroup substitute, int modifiers) {
//    Block result = fAST.newBlock();
//    ListRewrite statements = fRewriter.getListRewrite(result, Block.STATEMENTS_PROPERTY);
//
//    // Locals that are not passed as an arguments since the extracted method only
//    // writes to them
//    IVariableBinding[] methodLocals = fAnalyzer.getMethodLocals();
//    for (int i = 0; i < methodLocals.length; i++) {
//      if (methodLocals[i] != null) {
//        result.statements().add(createDeclaration(methodLocals[i], null));
//      }
//    }
//
//    for (Iterator<ParameterInfo> iter = fParameterInfos.iterator(); iter.hasNext();) {
//      ParameterInfo parameter = iter.next();
//      if (parameter.isRenamed()) {
//        for (int n = 0; n < selectedNodes.length; n++) {
//          SimpleName[] oldNames = LinkedNodeFinder.findByBinding(
//              selectedNodes[n],
//              parameter.getOldBinding());
//          for (int i = 0; i < oldNames.length; i++) {
//            fRewriter.replace(oldNames[i], fAST.newSimpleName(parameter.getNewName()), null);
//          }
//        }
//      }
//    }
//
//    boolean extractsExpression = fAnalyzer.isExpressionSelected();
//    ASTNode[] callNodes = createCallNodes(null, modifiers);
//    ASTNode replacementNode;
//    if (callNodes.length == 1) {
//      replacementNode = callNodes[0];
//    } else {
//      replacementNode = fRewriter.createGroupNode(callNodes);
//    }
//    if (extractsExpression) {
//      // if we have an expression then only one node is selected.
//      ITypeBinding binding = fAnalyzer.getExpressionBinding();
//      if (binding != null && (!binding.isPrimitive() || !"void".equals(binding.getName()))) { //$NON-NLS-1$
//        ReturnStatement rs = fAST.newReturnStatement();
//        rs.setExpression((Expression) fRewriter.createMoveTarget(selectedNodes[0] instanceof ParenthesizedExpression
//            ? ((ParenthesizedExpression) selectedNodes[0]).getExpression() : selectedNodes[0]));
//        statements.insertLast(rs, null);
//      } else {
//        ExpressionStatement st = fAST.newExpressionStatement((Expression) fRewriter.createMoveTarget(selectedNodes[0]));
//        statements.insertLast(st, null);
//      }
//      fRewriter.replace(selectedNodes[0].getParent() instanceof ParenthesizedExpression
//          ? selectedNodes[0].getParent() : selectedNodes[0], replacementNode, substitute);
//    } else {
//      if (selectedNodes.length == 1) {
//        statements.insertLast(fRewriter.createMoveTarget(selectedNodes[0]), substitute);
//        fRewriter.replace(selectedNodes[0], replacementNode, substitute);
//      } else {
//        ListRewrite source = fRewriter.getListRewrite(
//            selectedNodes[0].getParent(),
//            (ChildListPropertyDescriptor) selectedNodes[0].getLocationInParent());
//        ASTNode toMove = source.createMoveTarget(
//            selectedNodes[0],
//            selectedNodes[selectedNodes.length - 1],
//            replacementNode,
//            substitute);
//        statements.insertLast(toMove, substitute);
//      }
//      IVariableBinding returnValue = fAnalyzer.getReturnValue();
//      if (returnValue != null) {
//        ReturnStatement rs = fAST.newReturnStatement();
//        rs.setExpression(fAST.newSimpleName(getName(returnValue)));
//        statements.insertLast(rs, null);
//      }
//    }
//    return result;
//  }
//
//  private MethodDeclaration createNewMethod(ASTNode[] selectedNodes, String lineDelimiter,
//      TextEditGroup substitute) throws CoreException {
//    MethodDeclaration result = createNewMethodDeclaration();
//    result.setBody(createMethodBody(selectedNodes, substitute, result.getModifiers()));
//    if (fGenerateJavadoc) {
//      AbstractTypeDeclaration enclosingType = (AbstractTypeDeclaration) ASTNodes.getParent(
//          fAnalyzer.getEnclosingBodyDeclaration(),
//          AbstractTypeDeclaration.class);
//      String string = CodeGeneration.getMethodComment(
//          fCUnit,
//          enclosingType.getName().getIdentifier(),
//          result,
//          null,
//          lineDelimiter);
//      if (string != null) {
//        Javadoc javadoc = (Javadoc) fRewriter.createStringPlaceholder(string, ASTNode.JAVADOC);
//        result.setJavadoc(javadoc);
//      }
//    }
//    return result;
//  }
//
//  private MethodDeclaration createNewMethodDeclaration() {
//    MethodDeclaration result = fAST.newMethodDeclaration();
//
//    int modifiers = fVisibility;
//    ASTNode enclosingBodyDeclaration = fAnalyzer.getEnclosingBodyDeclaration();
//    while (enclosingBodyDeclaration != null && enclosingBodyDeclaration.getParent() != fDestination) {
//      enclosingBodyDeclaration = enclosingBodyDeclaration.getParent();
//    }
//    if (enclosingBodyDeclaration instanceof BodyDeclaration) { // should always be the case
//      int enclosingModifiers = ((BodyDeclaration) enclosingBodyDeclaration).getModifiers();
//      boolean shouldBeStatic = Modifier.isStatic(enclosingModifiers)
//          || enclosingBodyDeclaration instanceof EnumDeclaration || fAnalyzer.getForceStatic();
//      if (shouldBeStatic) {
//        modifiers |= Modifier.STATIC;
//      }
//    }
//
//    ITypeBinding[] typeVariables = computeLocalTypeVariables();
//    List<TypeParameter> typeParameters = result.typeParameters();
//    for (int i = 0; i < typeVariables.length; i++) {
//      TypeParameter parameter = fAST.newTypeParameter();
//      parameter.setName(fAST.newSimpleName(typeVariables[i].getName()));
//      typeParameters.add(parameter);
//    }
//
//    result.modifiers().addAll(ASTNodeFactory.newModifiers(fAST, modifiers));
//    result.setReturnType2((Type) ASTNode.copySubtree(fAST, fAnalyzer.getReturnType()));
//    result.setName(fAST.newSimpleName(fMethodName));
//
//    ImportRewriteContext context = new ContextSensitiveImportRewriteContext(
//        enclosingBodyDeclaration,
//        fImportRewriter);
//
//    List<SingleVariableDeclaration> parameters = result.parameters();
//    for (int i = 0; i < fParameterInfos.size(); i++) {
//      ParameterInfo info = fParameterInfos.get(i);
//      VariableDeclaration infoDecl = getVariableDeclaration(info);
//      SingleVariableDeclaration parameter = fAST.newSingleVariableDeclaration();
//      parameter.modifiers().addAll(
//          ASTNodeFactory.newModifiers(fAST, ASTNodes.getModifiers(infoDecl)));
//      parameter.setType(ASTNodeFactory.newType(fAST, infoDecl, fImportRewriter, context));
//      parameter.setName(fAST.newSimpleName(info.getNewName()));
//      parameter.setVarargs(info.isNewVarargs());
//      parameters.add(parameter);
//    }
//
//    List<Name> exceptions = result.thrownExceptions();
//    ITypeBinding[] exceptionTypes = fAnalyzer.getExceptions(fThrowRuntimeExceptions);
//    for (int i = 0; i < exceptionTypes.length; i++) {
//      ITypeBinding exceptionType = exceptionTypes[i];
//      exceptions.add(ASTNodeFactory.newName(fAST, fImportRewriter.addImport(exceptionType, context)));
//    }
//    return result;
//  }
//
//  private ASTVisitor createVisitor() throws CoreException {
//    fAnalyzer = new ExtractMethodAnalyzer(fCUnit, Selection.createFromStartLength(
//        fSelectionStart,
//        fSelectionLength));
//    return fAnalyzer;
//  }
//
//  private IVariableBinding getMappedBinding(SnippetFinder.Match duplicate, IVariableBinding org) {
//    if (duplicate == null) {
//      return org;
//    }
//    return duplicate.getMappedBinding(org);
//  }
//
//  private String getMappedName(SnippetFinder.Match duplicate, ParameterInfo paramter) {
//    if (duplicate == null) {
//      return paramter.getOldName();
//    }
//    return duplicate.getMappedName(paramter.getOldBinding()).getIdentifier();
//  }
//
//  //---- Code generation -----------------------------------------------------------------------
//
//  private String getName(IVariableBinding binding) {
//    for (Iterator<ParameterInfo> iter = fParameterInfos.iterator(); iter.hasNext();) {
//      ParameterInfo info = iter.next();
//      if (Bindings.equals(binding, info.getOldBinding())) {
//        return info.getNewName();
//      }
//    }
//    return binding.getName();
//  }
//
//  private ASTNode getNextParent(ASTNode node) {
//    do {
//      node = node.getParent();
//    } while (node != null
//        && !(node instanceof AbstractTypeDeclaration || node instanceof AnonymousClassDeclaration));
//    return node;
//  }
//
//  private ExtractMethodDescriptor getRefactoringDescriptor() {
//    final Map<String, String> arguments = new HashMap<String, String>();
//    String project = null;
//    IJavaProject javaProject = fCUnit.getJavaProject();
//    if (javaProject != null) {
//      project = javaProject.getElementName();
//    }
//    ITypeBinding type = null;
//    if (fDestination instanceof AbstractTypeDeclaration) {
//      final AbstractTypeDeclaration decl = (AbstractTypeDeclaration) fDestination;
//      type = decl.resolveBinding();
//    } else if (fDestination instanceof AnonymousClassDeclaration) {
//      final AnonymousClassDeclaration decl = (AnonymousClassDeclaration) fDestination;
//      type = decl.resolveBinding();
//    }
//    IMethodBinding method = null;
//    final BodyDeclaration enclosing = fAnalyzer.getEnclosingBodyDeclaration();
//    if (enclosing instanceof MethodDeclaration) {
//      final MethodDeclaration node = (MethodDeclaration) enclosing;
//      method = node.resolveBinding();
//    }
//    final int flags = RefactoringDescriptor.STRUCTURAL_CHANGE
//        | JavaRefactoringDescriptor.JAR_REFACTORING
//        | JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
//    final String description = Messages.format(
//        RefactoringCoreMessages.ExtractMethodRefactoring_descriptor_description_short,
//        BasicElementLabels.getJavaElementName(fMethodName));
//    final String label = method != null ? BindingLabelProvider.getBindingLabel(
//        method,
//        JavaElementLabels.ALL_FULLY_QUALIFIED) : '{' + JavaElementLabels.ELLIPSIS_STRING + '}';
//    final String header = Messages.format(
//        RefactoringCoreMessages.ExtractMethodRefactoring_descriptor_description,
//        new String[] {
//            BasicElementLabels.getJavaElementName(getSignature()), label,
//            BindingLabelProvider.getBindingLabel(type, JavaElementLabels.ALL_FULLY_QUALIFIED)});
//    final JDTRefactoringDescriptorComment comment = new JDTRefactoringDescriptorComment(
//        project,
//        this,
//        header);
//    comment.addSetting(Messages.format(
//        RefactoringCoreMessages.ExtractMethodRefactoring_name_pattern,
//        BasicElementLabels.getJavaElementName(fMethodName)));
//    comment.addSetting(Messages.format(
//        RefactoringCoreMessages.ExtractMethodRefactoring_destination_pattern,
//        BindingLabelProvider.getBindingLabel(type, JavaElementLabels.ALL_FULLY_QUALIFIED)));
//    String visibility = JdtFlags.getVisibilityString(fVisibility);
//    if ("".equals(visibility)) {
//      visibility = RefactoringCoreMessages.ExtractMethodRefactoring_default_visibility;
//    }
//    comment.addSetting(Messages.format(
//        RefactoringCoreMessages.ExtractMethodRefactoring_visibility_pattern,
//        visibility));
//    if (fThrowRuntimeExceptions) {
//      comment.addSetting(RefactoringCoreMessages.ExtractMethodRefactoring_declare_thrown_exceptions);
//    }
//    if (fReplaceDuplicates) {
//      comment.addSetting(RefactoringCoreMessages.ExtractMethodRefactoring_replace_occurrences);
//    }
//    if (fGenerateJavadoc) {
//      comment.addSetting(RefactoringCoreMessages.ExtractMethodRefactoring_generate_comment);
//    }
//    final ExtractMethodDescriptor descriptor = RefactoringSignatureDescriptorFactory.createExtractMethodDescriptor(
//        project,
//        description,
//        comment.asString(),
//        arguments,
//        flags);
//    arguments.put(
//        JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT,
//        JavaRefactoringDescriptorUtil.elementToHandle(project, fCUnit));
//    arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME, fMethodName);
//    arguments.put(
//        JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION,
//        new Integer(fSelectionStart).toString() + " " + new Integer(fSelectionLength).toString()); //$NON-NLS-1$
//    arguments.put(ATTRIBUTE_VISIBILITY, new Integer(fVisibility).toString());
//    arguments.put(ATTRIBUTE_DESTINATION, new Integer(fDestinationIndex).toString());
//    arguments.put(ATTRIBUTE_EXCEPTIONS, Boolean.valueOf(fThrowRuntimeExceptions).toString());
//    arguments.put(ATTRIBUTE_COMMENTS, Boolean.valueOf(fGenerateJavadoc).toString());
//    arguments.put(ATTRIBUTE_REPLACE, Boolean.valueOf(fReplaceDuplicates).toString());
//    return descriptor;
//  }
//
//  private String getType(VariableDeclaration declaration, boolean isVarargs) {
//    String type = ASTNodes.asString(ASTNodeFactory.newType(
//        declaration.getAST(),
//        declaration,
//        fImportRewriter,
//        new ContextSensitiveImportRewriteContext(declaration, fImportRewriter)));
//    if (isVarargs) {
//      return type + ParameterInfo.ELLIPSIS;
//    } else {
//      return type;
//    }
//  }
//
//  private VariableDeclaration getVariableDeclaration(ParameterInfo parameter) {
//    return ASTNodes.findVariableDeclaration(
//        parameter.getOldBinding(),
//        fAnalyzer.getEnclosingBodyDeclaration());
//  }
//
//  private void initializeDestinations() {
//    List<ASTNode> result = new ArrayList<ASTNode>();
//    BodyDeclaration decl = fAnalyzer.getEnclosingBodyDeclaration();
//    ASTNode current = getNextParent(decl);
//    result.add(current);
//    if (decl instanceof MethodDeclaration || decl instanceof Initializer
//        || decl instanceof FieldDeclaration) {
//      ITypeBinding binding = ASTNodes.getEnclosingType(current);
//      ASTNode next = getNextParent(current);
//      while (next != null && binding != null && binding.isNested()) {
//        result.add(next);
//        current = next;
//        binding = ASTNodes.getEnclosingType(current);
//        next = getNextParent(next);
//      }
//    }
//    fDestinations = result.toArray(new ASTNode[result.size()]);
//    fDestination = fDestinations[fDestinationIndex];
//  }
//
//  private void initializeDuplicates() {
//    ASTNode start = fAnalyzer.getEnclosingBodyDeclaration();
//    while (!(start instanceof AbstractTypeDeclaration)) {
//      start = start.getParent();
//    }
//
//    fDuplicates = SnippetFinder.perform(start, fAnalyzer.getSelectedNodes());
//    fReplaceDuplicates = fDuplicates.length > 0 && !fAnalyzer.isLiteralNodeSelected();
//  }
//
//  private void initializeParameterInfos() {
//    IVariableBinding[] arguments = fAnalyzer.getArguments();
//    fParameterInfos = new ArrayList<ParameterInfo>(arguments.length);
//    ASTNode root = fAnalyzer.getEnclosingBodyDeclaration();
//    ParameterInfo vararg = null;
//    for (int i = 0; i < arguments.length; i++) {
//      IVariableBinding argument = arguments[i];
//      if (argument == null) {
//        continue;
//      }
//      VariableDeclaration declaration = ASTNodes.findVariableDeclaration(argument, root);
//      boolean isVarargs = declaration instanceof SingleVariableDeclaration
//          ? ((SingleVariableDeclaration) declaration).isVarargs() : false;
//      ParameterInfo info = new ParameterInfo(
//          argument,
//          getType(declaration, isVarargs),
//          argument.getName(),
//          i);
//      if (isVarargs) {
//        vararg = info;
//      } else {
//        fParameterInfos.add(info);
//      }
//    }
//    if (vararg != null) {
//      fParameterInfos.add(vararg);
//    }
//  }
//
//  private void initializeUsedNames() {
//    fUsedNames = UsedNamesCollector.perform(fAnalyzer.getSelectedNodes());
//    for (Iterator<ParameterInfo> iter = fParameterInfos.iterator(); iter.hasNext();) {
//      ParameterInfo parameter = iter.next();
//      fUsedNames.remove(parameter.getOldName());
//    }
//  }
//
//  private boolean isDestinationReachable(MethodDeclaration methodDeclaration) {
//    ASTNode start = methodDeclaration;
//    while (start != null && start != fDestination) {
//      start = start.getParent();
//    }
//    return start == fDestination;
//  }
//
//  private RefactoringStatus mergeTextSelectionStatus(RefactoringStatus status) {
//    status.addFatalError(RefactoringCoreMessages.ExtractMethodRefactoring_no_set_of_statements);
//    return status;
//  }
//
//  private void processVariable(List<ITypeBinding> result, IVariableBinding variable) {
//    if (variable == null) {
//      return;
//    }
//    ITypeBinding binding = variable.getType();
//    if (binding != null && binding.isParameterizedType()) {
//      ITypeBinding[] typeArgs = binding.getTypeArguments();
//      for (int args = 0; args < typeArgs.length; args++) {
//        ITypeBinding arg = typeArgs[args];
//        if (arg.isTypeVariable() && !result.contains(arg)) {
//          ASTNode decl = fRoot.findDeclaringNode(arg);
//          if (decl != null && decl.getParent() instanceof MethodDeclaration) {
//            result.add(arg);
//          }
//        }
//      }
//    }
//  }
//
//  private void replaceBranches(final CompilationUnitChange result) {
//    ASTNode[] selectedNodes = fAnalyzer.getSelectedNodes();
//    for (int i = 0; i < selectedNodes.length; i++) {
//      ASTNode astNode = selectedNodes[i];
//      astNode.accept(new ASTVisitor() {
//        private LinkedList<String> fOpenLoopLabels = new LinkedList<String>();
//
//        @Override
//        public void endVisit(ContinueStatement node) {
//          final SimpleName label = node.getLabel();
//          if (fOpenLoopLabels.isEmpty()
//              || (label != null && !fOpenLoopLabels.contains(label.getIdentifier()))) {
//            TextEditGroup description = new TextEditGroup(
//                RefactoringCoreMessages.ExtractMethodRefactoring_replace_continue);
//            result.addTextEditGroup(description);
//
//            ReturnStatement rs = fAST.newReturnStatement();
//            IVariableBinding returnValue = fAnalyzer.getReturnValue();
//            if (returnValue != null) {
//              rs.setExpression(fAST.newSimpleName(getName(returnValue)));
//            }
//
//            fRewriter.replace(node, rs, description);
//          }
//        }
//
//        @Override
//        public void endVisit(DoStatement node) {
//          fOpenLoopLabels.removeLast();
//        }
//
//        @Override
//        public void endVisit(EnhancedForStatement node) {
//          fOpenLoopLabels.removeLast();
//        }
//
//        @Override
//        public void endVisit(ForStatement node) {
//          fOpenLoopLabels.removeLast();
//        }
//
//        @Override
//        public void endVisit(WhileStatement node) {
//          fOpenLoopLabels.removeLast();
//        }
//
//        @Override
//        public boolean visit(DoStatement node) {
//          registerLoopLabel(node);
//          return super.visit(node);
//        }
//
//        @Override
//        public boolean visit(EnhancedForStatement node) {
//          registerLoopLabel(node);
//          return super.visit(node);
//        }
//
//        @Override
//        public boolean visit(ForStatement node) {
//          registerLoopLabel(node);
//          return super.visit(node);
//        }
//
//        @Override
//        public boolean visit(WhileStatement node) {
//          registerLoopLabel(node);
//          return super.visit(node);
//        }
//
//        private void registerLoopLabel(Statement node) {
//          String identifier;
//          if (node.getParent() instanceof LabeledStatement) {
//            LabeledStatement labeledStatement = (LabeledStatement) node.getParent();
//            identifier = labeledStatement.getLabel().getIdentifier();
//          } else {
//            identifier = null;
//          }
//          fOpenLoopLabels.add(identifier);
//        }
//      });
//    }
//  }
//
//  private void replaceDuplicates(CompilationUnitChange result, int modifiers) {
//    int numberOf = getNumberOfDuplicates();
//    if (numberOf == 0 || !fReplaceDuplicates) {
//      return;
//    }
//    String label = null;
//    if (numberOf == 1) {
//      label = Messages.format(
//          RefactoringCoreMessages.ExtractMethodRefactoring_duplicates_single,
//          BasicElementLabels.getJavaElementName(fMethodName));
//    } else {
//      label = Messages.format(
//          RefactoringCoreMessages.ExtractMethodRefactoring_duplicates_multi,
//          BasicElementLabels.getJavaElementName(fMethodName));
//    }
//
//    TextEditGroup description = new TextEditGroup(label);
//    result.addTextEditGroup(description);
//
//    for (int d = 0; d < fDuplicates.length; d++) {
//      SnippetFinder.Match duplicate = fDuplicates[d];
//      if (!duplicate.isMethodBody()) {
//        if (isDestinationReachable(duplicate.getEnclosingMethod())) {
//          ASTNode[] callNodes = createCallNodes(duplicate, modifiers);
//          ASTNode[] duplicateNodes = duplicate.getNodes();
//          for (int i = 0; i < duplicateNodes.length; i++) {
//            ASTNode parent = duplicateNodes[i].getParent();
//            if (parent instanceof ParenthesizedExpression) {
//              duplicateNodes[i] = parent;
//            }
//          }
//          new StatementRewrite(fRewriter, duplicateNodes).replace(callNodes, description);
//        }
//      }
//    }
//  }

  /**
   * Checks if {@link #selectionRange} selects {@link DartExpression} or set of
   * {@link DartStatement}s which can be extracted, and location in AST allows extracting.
   */
  private RefactoringStatus checkSelection(IProgressMonitor pm) throws DartModelException {
    Selection selection = Selection.createFromStartLength(
        selectionRange.getOffset(),
        selectionRange.getLength());
    selectionAnalyzer = new SelectionAnalyzer(selection, false);
    unitNode.accept(selectionAnalyzer);
    // single expression selected
    if (selectionAnalyzer.getSelectedNodes().length == 1
        && !utils.rangeIncludesNonWhitespaceOutsideNode(
            selectionRange,
            selectionAnalyzer.getFirstSelectedNode())) {
      DartNode selectedNode = selectionAnalyzer.getFirstSelectedNode();
      if (selectedNode instanceof DartExpression) {
        selectionExpression = (DartExpression) selectedNode;
        return new RefactoringStatus();
      }
    }
    // invalid selection
    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractMethodAnalyzer_single_expression_or_set);
  }

  /**
   * @return the selected {@link DartExpression} source, with applying new parameter names.
   */
  private String getReturnExpressionSource() {
    String source = utils.getText(selectionStart, selectionLength);
    // prepare ReplaceEdit operators to apply
    List<ReplaceEdit> replaceEdits = Lists.newArrayList();
    for (Entry<String, List<SourceRange>> entry : selectionParametersToRanges.entrySet()) {
      String name = entry.getKey();
      for (ParameterInfo parameter : parameters) {
        if (StringUtils.equals(name, parameter.getOldName())) {
          for (SourceRange range : entry.getValue()) {
            replaceEdits.add(new ReplaceEdit(
                range.getOffset() - selectionStart,
                range.getLength(),
                parameter.getNewName()));
          }
        }
      }
    }
    // apply replacements
    return ExtractUtils.applyReplaceEdits(source, replaceEdits);
  }

  private SourcePattern getSourcePattern(final SourceRange partRange) {
    String originalSource = utils.getText(partRange.getOffset(), partRange.getLength());
    final SourcePattern pattern = new SourcePattern();
    final List<ReplaceEdit> replaceEdits = Lists.newArrayList();
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitIdentifier(DartIdentifier node) {
        SourceRange nodeRange = SourceRangeFactory.create(node);
        if (SourceRangeUtils.covers(partRange, nodeRange)) {
          if (ElementKind.of(node.getElement()) == ElementKind.VARIABLE) {
            String originalName = ((VariableElement) node.getElement()).getName();
            String patternName = pattern.originalToPatternNames.get(originalName);
            if (patternName == null) {
              patternName = "__dartEditorVariable" + pattern.originalToPatternNames.size();
              pattern.originalToPatternNames.put(originalName, patternName);
            }
            replaceEdits.add(new ReplaceEdit(
                nodeRange.getOffset() - partRange.getOffset(),
                nodeRange.getLength(),
                patternName));
          }
        }
        return null;
      }
    });
    pattern.patternSource = ExtractUtils.applyReplaceEdits(originalSource, replaceEdits);
    return pattern;
  }

  /**
   * Fills {@link #occurrences} field.
   */
  private void initializeOccurrences() {
    if (selectionExpression != null) {
      // prepare selection
      SourcePattern selectionPattern = getSourcePattern(selectionRange);
      final String selectionSource = getNormalizedSource(selectionPattern.patternSource);
      final Map<String, String> patternToSelectionName = HashBiMap.create(
          selectionPattern.originalToPatternNames).inverse();
      // prepare context and enclosing parent - class or unit
      final boolean staticContext;
      DartNode enclosingMemberParent;
      {
        DartNode coveringNode = selectionAnalyzer.getLastCoveringNode();
        DartClassMember<?> parentMember = ASTNodes.getAncestor(coveringNode, DartClassMember.class);
        staticContext = parentMember.getModifiers().isStatic();
        enclosingMemberParent = parentMember.getParent();
      }
      // visit nodes which will able to access extracted method
      enclosingMemberParent.accept(new ASTVisitor<Void>() {
        @Override
        public Void visitClassMember(DartClassMember<?> node) {
          if (staticContext || !node.getModifiers().isStatic()) {
            return super.visitClassMember(node);
          }
          return null;
        }

        @Override
        public Void visitExpression(DartExpression node) {
          if (node.getClass() == selectionExpression.getClass()) {
            SourceRange nodeRange = SourceRangeFactory.create(node);
            // prepare normalized node source
            SourcePattern nodePattern = getSourcePattern(nodeRange);
            String nodeSource = getNormalizedSource(nodePattern.patternSource);
            // if matches normalized node source, then add as occurrence
            if (nodeSource.equals(selectionSource)) {
              Occurrence occurrence = new Occurrence(nodeRange, node == selectionExpression);
              occurrences.add(occurrence);
              // prepare mapping of parameter names to the occurence variables
              for (Entry<String, String> entry : nodePattern.originalToPatternNames.entrySet()) {
                String patternName = entry.getValue();
                String originalName = entry.getKey();
                String selectionName = patternToSelectionName.get(patternName);
                occurrence.parameterOldToOccurrenceName.put(selectionName, originalName);
              }
            }
          }
          return super.visitExpression(node);
        }
      });
    }
  }

  /**
   * Fills {@link #parameters} with information about used variables, which should be turned into
   * parameters.
   */
  private void initializeParameters() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitIdentifier(DartIdentifier node) {
        if (ElementKind.of(node.getElement()) == ElementKind.VARIABLE) {
          SourceRange nodeRange = SourceRangeFactory.create(node);
          if (SourceRangeUtils.covers(selectionRange, nodeRange)) {
            VariableElement variableElement = (VariableElement) node.getElement();
            String variableName = variableElement.getName();
            // add parameter
            if (!selectionParametersToRanges.containsKey(variableName)) {
              parameters.add(new ParameterInfo(variableElement));
            }
            // add reference to parameter
            {
              List<SourceRange> ranges = selectionParametersToRanges.get(variableName);
              if (ranges == null) {
                ranges = Lists.newArrayList();
                selectionParametersToRanges.put(variableName, ranges);
              }
              ranges.add(nodeRange);
            }
          }
        }
        return null;
      }
    });
  }
}
