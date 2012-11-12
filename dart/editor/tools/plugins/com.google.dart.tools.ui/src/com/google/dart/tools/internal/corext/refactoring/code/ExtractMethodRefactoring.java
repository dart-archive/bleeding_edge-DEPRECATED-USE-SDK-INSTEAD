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
import com.google.common.collect.Sets;
import com.google.dart.compiler.ast.ASTNodes;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartClassMember;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartInitializer;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.engine.scanner.Token;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTopLevelProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeMemberProcessor;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.internal.text.Selection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
   * Basically just replaces "renamed" with "created" in all messages.
   */
  private static RefactoringStatus convertRenameToCreateStatus(RefactoringStatus renameStatus) {
    RefactoringStatus result = new RefactoringStatus();
    for (RefactoringStatusEntry entry : renameStatus.getEntries()) {
      String msg = entry.getMessage();
      msg = RenameAnalyzeUtil.convertRenameMessageToCreateMessage(msg);
      result.addEntry(
          entry.getSeverity(),
          msg,
          entry.getContext(),
          entry.getPluginId(),
          entry.getCode());
    }
    return result;
  }

  /**
   * @return the "normalized" version of the given source, which is built form tokens, so ignores
   *         all comments and spaces.
   */
  private static String getNormalizedSource(String s) {
    List<Token> selectionTokens = TokenUtils.getTokens(s);
    return StringUtils.join(selectionTokens, TOKEN_SEPARATOR);
  }

  /**
   * @return <code>true</code> if given {@link DartNode} is left hand side of assignment, or
   *         declaration of the variable.
   */
  private static boolean isLeftHandOfAssignment(DartIdentifier node) {
    if (ASTNodes.inSetterContext(node)) {
      return true;
    }
    return PropertyDescriptorHelper.getLocationInParent(node) == PropertyDescriptorHelper.DART_VARIABLE_NAME;
  }

  private final CompilationUnit unit;
  private int selectionStart;
  private int selectionLength;
  private SourceRange selectionRange;

  private CompilationUnitChange change;
  private ExtractUtils utils;
  private DartUnit unitNode;
  private final List<ParameterInfo> parameters = Lists.newArrayList();

  private final Set<String> usedNames = Sets.newHashSet();
  private VariableElement returnVariable;
  private ExtractMethodAnalyzer selectionAnalyzer;
  private DartClassMember<?> parentMember;
  private DartExpression selectionExpression;

  private List<DartStatement> selectionStatements;
  private final Map<String, List<SourceRange>> selectionParametersToRanges = Maps.newHashMap();
  private final List<Occurrence> occurrences = Lists.newArrayList();
  private boolean staticContext;

  private String methodName;

  private boolean replaceAllOccurrences = true;

  private static final String EMPTY = ""; //$NON-NLS-1$

  public ExtractMethodRefactoring(CompilationUnit unit, int selectionStart, int selectionLength) {
    this.unit = unit;
    this.selectionStart = selectionStart;
    this.selectionLength = selectionLength;
    this.selectionRange = SourceRangeFactory.forStartLength(selectionStart, selectionLength);
    this.methodName = "extracted"; //$NON-NLS-1$
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.ExtractMethodRefactoring_checking_new_name, 3);
    pm.subTask(EMPTY);

    RefactoringStatus result = checkMethodName();
    pm.worked(1);

    result.merge(checkParameterNames());
    pm.worked(1);

    result.merge(checkPossibleConflicts(new SubProgressMonitor(pm, 1)));

    pm.done();
    return result;
  }

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
      if (result.hasFatalError()) {
        return result;
      }
      // prepare parts
      result.merge(initializeParameters());
      initializeOccurrences();
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  /**
   * Checks if the new method name is a valid method name. This method doesn't check if a method
   * with the same name already exists in the hierarchy. This check is done in
   * {@link #checkPossibleConflicts()} since it is expensive.
   */
  public RefactoringStatus checkMethodName() {
    return Checks.checkMethodName(methodName);
  }

  /**
   * Checks if the parameter names are valid.
   */
  public RefactoringStatus checkParameterNames() {
    RefactoringStatus result = new RefactoringStatus();
    for (ParameterInfo parameter : parameters) {
      result.merge(Checks.checkParameter(parameter.getNewName()));
      for (ParameterInfo other : parameters) {
        if (parameter != other && StringUtils.equals(other.getNewName(), parameter.getNewName())) {
          result.addError(Messages.format(
              RefactoringCoreMessages.ExtractMethodRefactoring_error_sameParameter,
              other.getNewName()));
          return result;
        }
      }
      if (parameter.isRenamed() && usedNames.contains(parameter.getNewName())) {
        result.addError(Messages.format(
            RefactoringCoreMessages.ExtractMethodRefactoring_error_nameInUse,
            parameter.getNewName()));
        return result;
      }
    }
    return result;
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException {
    pm.beginTask("", 1 + occurrences.size()); //$NON-NLS-1$
    try {
      // configure Change
      {
        change = new CompilationUnitChange(unit.getElementName(), unit);
        change.setEdit(new MultiTextEdit());
        change.setKeepPreviewEdits(true);
      }
      // replace occurrences with method invocation
      for (Occurrence occurence : occurrences) {
        pm.worked(1);
        SourceRange range = occurence.range;
        // may be replacement of duplicates disabled
        if (!replaceAllOccurrences && !occurence.isSelection) {
          continue;
        }
        // prepare invocation source
        String invocationSource;
        {
          StringBuilder sb = new StringBuilder();
          // may be returns value
          if (returnVariable != null) {
            String varTypeName = ExtractUtils.getTypeSource(returnVariable.getType());
            String originalName = returnVariable.getName();
            String occurrenceName = occurence.parameterOldToOccurrenceName.get(originalName);
            if (varTypeName.equals("dynamic")) {
              sb.append("var ");
            } else {
              sb.append(varTypeName);
              sb.append(" ");
            }
            sb.append(occurrenceName);
            sb.append(" = ");
          }
          // invocation itself
          sb.append(methodName);
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
          // statements as extracted with their ";", so add new one after invocation
          if (selectionStatements != null) {
            invocationSource += ";";
          }
        }
        // add replace edit
        TextEdit edit = new ReplaceEdit(range.getOffset(), range.getLength(), invocationSource);
        change.addEdit(edit);
        String msg = Messages.format(occurence.isSelection
            ? RefactoringCoreMessages.ExtractMethodRefactoring_substitute_with_call
            : RefactoringCoreMessages.ExtractMethodRefactoring_duplicates_single, methodName);
        change.addTextEditGroup(new TextEditGroup(msg, edit));
      }
      // add method declaration
      {
        // prepare environment
        String prefix = utils.getNodePrefix(parentMember);
        String eol = utils.getEndOfLine();
        // prepare annotations
        String annotations = "";
        {
          // may be "static"
          if (staticContext) {
            annotations = "static ";
          }
        }
        // prepare declaration source
        String declarationSource = null;
        {
          String returnExpressionSource = getMethodBodySource();
          // expression
          if (selectionExpression != null) {
            // add return type
            String returnTypeName = ExtractUtils.getTypeSource(selectionExpression);
            if (returnTypeName != null && !returnTypeName.equals("dynamic")) {
              annotations += returnTypeName + " ";
            }
            // just return expression
            declarationSource = annotations + getSignature() + " => " + returnExpressionSource
                + ";";
          }
          // statements
          if (selectionStatements != null) {
            if (returnVariable != null) {
              String returnTypeName = ExtractUtils.getTypeSource(returnVariable.getType());
              if (returnTypeName != null && !returnTypeName.equals("dynamic")) {
                annotations += returnTypeName + " ";
              }
            } else {
              annotations += "void ";
            }
            declarationSource = annotations + getSignature() + " {" + eol;
            declarationSource += returnExpressionSource;
            if (returnVariable != null) {
              declarationSource += prefix + "  return " + returnVariable.getName() + ";" + eol;
            }
            declarationSource += prefix + "}";
          }
        }
        // insert declaration
        if (declarationSource != null) {
          int offset = parentMember.getSourceInfo().getEnd();
          TextEdit edit = new ReplaceEdit(offset, 0, eol + eol + prefix + declarationSource);
          change.addEdit(edit);
          change.addTextEditGroup(new TextEditGroup(Messages.format(selectionExpression != null
              ? RefactoringCoreMessages.ExtractMethodRefactoring_add_method_expression
              : RefactoringCoreMessages.ExtractMethodRefactoring_add_method, methodName), edit));
        }
      }
      pm.worked(1);
      // done
      return change;
    } finally {
      pm.done();
    }
  }

  @Override
  public String getName() {
    return RefactoringCoreMessages.ExtractMethodRefactoring_name;
  }

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

  /**
   * @return the signature of the extracted method
   */
  public String getSignature() {
    return getSignature(methodName);
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
        if (!"dynamic".equals(typeSource) && !"".equals(typeSource)) {
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

  public CompilationUnit getUnit() {
    return unit;
  }

  /**
   * Sets the method name to be used for the extracted method.
   */
  public void setMethodName(String name) {
    methodName = name;
  }

  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    this.replaceAllOccurrences = replaceAllOccurrences;
  }

  /**
   * Checks if created method will shadow or will be shadowed by other elements.
   */
  private RefactoringStatus checkPossibleConflicts(IProgressMonitor pm) throws CoreException {
    List<SearchMatch> references = Lists.newArrayList();
    // top-level function
    if (parentMember.getParent() instanceof DartUnit) {
      RefactoringStatus conflictsStatus = RenameTopLevelProcessor.analyzePossibleConflicts(
          unit.getLibrary(),
          DartElement.FUNCTION,
          false,
          references,
          methodName);
      if (!conflictsStatus.isOK()) {
        return convertRenameToCreateStatus(conflictsStatus);
      }
    }
    // method of class
    if (parentMember.getParent() instanceof DartClass) {
      ClassElement enclosingClassElement = (ClassElement) parentMember.getParent().getElement();
      Type enclosingType = (Type) BindingUtils.getDartElement(enclosingClassElement);
      RefactoringStatus conflictsStatus = RenameTypeMemberProcessor.analyzePossibleConflicts(
          DartElement.FUNCTION,
          enclosingType,
          methodName,
          references,
          methodName,
          pm);
      if (!conflictsStatus.isOK()) {
        return convertRenameToCreateStatus(conflictsStatus);
      }
    }
    // OK
    return new RefactoringStatus();
  }

  /**
   * Checks if {@link #selectionRange} selects {@link DartExpression} or set of
   * {@link DartStatement}s which can be extracted, and location in AST allows extracting.
   */
  private RefactoringStatus checkSelection(IProgressMonitor pm) throws CoreException {
    Selection selection = Selection.createFromStartLength(selectionStart, selectionLength);
    selectionAnalyzer = new ExtractMethodAnalyzer(unit, selection);
    unitNode.accept(selectionAnalyzer);
    // may be fatal error
    {
      RefactoringStatus status = selectionAnalyzer.getStatus();
      if (status.hasFatalError()) {
        return status;
      }
    }
    // update selection
    selectionLength = selectionAnalyzer.getSelectionExclusiveEnd() - selectionStart;
    selectionRange = SourceRangeFactory.forStartLength(selectionStart, selectionLength);
    // check selected nodes
    DartNode[] selectedNodes = selectionAnalyzer.getSelectedNodes();
    if (selectedNodes.length > 0) {
      parentMember = ASTNodes.getParent(
          selectionAnalyzer.getLastCoveringNode(),
          DartClassMember.class);
      // single expression selected
      if (selectedNodes.length == 1
          && !utils.rangeIncludesNonWhitespaceOutsideNode(
              selectionRange,
              selectionAnalyzer.getFirstSelectedNode())) {
        DartNode selectedNode = selectionAnalyzer.getFirstSelectedNode();
        if (selectedNode instanceof DartExpression) {
          selectionExpression = (DartExpression) selectedNode;
          return new RefactoringStatus();
        }
      }
      // statements selected
      {
        List<DartStatement> selectedStatements = Lists.newArrayList();
        for (DartNode selectedNode : selectedNodes) {
          if (selectedNode instanceof DartStatement) {
            selectedStatements.add((DartStatement) selectedNode);
          }
        }
        if (selectedStatements.size() == selectedNodes.length) {
          selectionStatements = selectedStatements;
          return new RefactoringStatus();
        }
      }
    }
    // invalid selection
    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractMethodAnalyzer_single_expression_or_set);
  }

  /**
   * @return the selected {@link DartExpression} source, with applying new parameter names.
   */
  private String getMethodBodySource() {
    String source = utils.getText(selectionStart, selectionLength);
    // prepare ReplaceEdit operators to replace variables with parameters
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
    source = ExtractUtils.applyReplaceEdits(source, replaceEdits);
    // change indentation
    if (selectionStatements != null) {
      String eol = utils.getEndOfLine();
      String selectionIndent = utils.getNodePrefix(selectionStatements.get(0));
      String targetIndent = utils.getNodePrefix(parentMember) + "  ";
      String[] lines = StringUtils.splitPreserveAllTokens(source, eol);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        line = targetIndent + StringUtils.removeStart(line, selectionIndent);
        sb.append(line);
        sb.append(eol);
      }
      source = sb.toString();
      source = StringUtils.stripEnd(source, null);
      source += eol;
    }
    // done
    return source;
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
          VariableElement variableElement = ASTNodes.getVariableOrParameterElement(node);
          if (variableElement != null) {
            String originalName = variableElement.getName();
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
    // prepare selection
    SourcePattern selectionPattern = getSourcePattern(selectionRange);
    final String selectionSource = getNormalizedSource(selectionPattern.patternSource);
    final Map<String, String> patternToSelectionName = HashBiMap.create(
        selectionPattern.originalToPatternNames).inverse();
    // prepare context and enclosing parent - class or unit
    DartNode enclosingMemberParent;
    {
      DartNode coveringNode = selectionAnalyzer.getLastCoveringNode();
      DartClassMember<?> parentMember = ASTNodes.getAncestor(coveringNode, DartClassMember.class);
      enclosingMemberParent = parentMember.getParent();
    }
    // visit nodes which will able to access extracted method
    enclosingMemberParent.accept(new ASTVisitor<Void>() {
      boolean forceStatic = false;

      @Override
      public Void visitBlock(DartBlock node) {
        if (selectionStatements != null) {
          List<DartStatement> blockStatements = node.getStatements();
          int beginStatementIndex = 0;
          int selectionCount = selectionStatements.size();
          while (beginStatementIndex + selectionCount <= blockStatements.size()) {
            SourceRange nodeRange = SourceRangeFactory.forStartEnd(
                blockStatements.get(beginStatementIndex),
                blockStatements.get(beginStatementIndex + selectionCount - 1));
            boolean found = tryToFindOccurrence(nodeRange);
            // next statement
            if (found) {
              beginStatementIndex += selectionCount;
            } else {
              beginStatementIndex++;
            }
          }
        }
        return super.visitBlock(node);
      }

      @Override
      public Void visitExpression(DartExpression node) {
        if (selectionExpression != null && node.getClass() == selectionExpression.getClass()) {
          SourceRange nodeRange = SourceRangeFactory.create(node);
          tryToFindOccurrence(nodeRange);
        }
        return super.visitExpression(node);
      }

      @Override
      public Void visitInitializer(DartInitializer node) {
        forceStatic = true;
        try {
          return super.visitInitializer(node);
        } finally {
          forceStatic = false;
        }
      }

      @Override
      public Void visitMethodDefinition(DartMethodDefinition node) {
        forceStatic = node.getModifiers().isStatic();
        try {
          return super.visitMethodDefinition(node);
        } finally {
          forceStatic = false;
        }
      }

      /**
       * Checks if given {@link SourceRange} matched selection source and adds {@link Occurrence}.
       */
      private boolean tryToFindOccurrence(SourceRange nodeRange) {
        // prepare normalized node source
        SourcePattern nodePattern = getSourcePattern(nodeRange);
        String nodeSource = getNormalizedSource(nodePattern.patternSource);
        // if matches normalized node source, then add as occurrence
        if (nodeSource.equals(selectionSource)) {
          Occurrence occurrence = new Occurrence(nodeRange, SourceRangeUtils.intersects(
              selectionRange,
              nodeRange));
          occurrences.add(occurrence);
          // prepare mapping of parameter names to the occurrence variables
          for (Entry<String, String> entry : nodePattern.originalToPatternNames.entrySet()) {
            String patternName = entry.getValue();
            String originalName = entry.getKey();
            String selectionName = patternToSelectionName.get(patternName);
            occurrence.parameterOldToOccurrenceName.put(selectionName, originalName);
          }
          // update static
          if (forceStatic) {
            staticContext |= true;
          }
          // we have match
          return true;
        }
        // no match
        return false;
      }
    });
  }

  /**
   * Fills {@link #parameters} with information about used variables, which should be turned into
   * parameters.
   */
  private RefactoringStatus initializeParameters() {
    RefactoringStatus result = new RefactoringStatus();
    final List<VariableElement> assignedUsedVariables = Lists.newArrayList();
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitIdentifier(DartIdentifier node) {
        SourceRange nodeRange = SourceRangeFactory.create(node);
        if (SourceRangeUtils.covers(selectionRange, nodeRange)) {
          // analyze local variable
          VariableElement variableElement = ASTNodes.getVariableOrParameterElement(node);
          if (variableElement != null) {
            // if declared outside, add parameter
            if (!isDeclaredInSelection(variableElement)) {
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
            // remember, if assigned and used after seleciton
            if (isLeftHandOfAssignment(node) && isUsedAfterSelection(variableElement)) {
              if (!assignedUsedVariables.contains(variableElement)) {
                assignedUsedVariables.add(variableElement);
              }
            }
          }
          // remember declaration names
          if (ASTNodes.isNameOfDeclaration(node)) {
            usedNames.add(node.getName());
          }
        }
        return null;
      }

    });
    // may be single variable to return
    if (assignedUsedVariables.size() == 1) {
      returnVariable = assignedUsedVariables.get(0);
    }
    // fatal, if multiple variables assigned and used after selection
    if (assignedUsedVariables.size() > 1) {
      StringBuilder sb = new StringBuilder();
      for (VariableElement variable : assignedUsedVariables) {
        sb.append(variable.getName());
        sb.append("\n");
      }
      result.addFatalError(Messages.format(
          RefactoringCoreMessages.ExtractMethodAnalyzer_assignments_to_local,
          sb.toString().trim()));
    }
    // done
    return result;
  }

  /**
   * @return <code>true</code> if the given {@link VariableElement} is declared inside of
   *         {@link #selectionRange}.
   */
  private boolean isDeclaredInSelection(VariableElement element) {
    return SourceRangeUtils.contains(selectionRange, element.getNameLocation().getOffset());
  }

  /**
   * @return <code>true</code> if the given {@link VariableElement} is referenced after the
   *         {@link #selectionRange}.
   */
  private boolean isUsedAfterSelection(final VariableElement element) {
    final AtomicBoolean result = new AtomicBoolean();
    parentMember.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitIdentifier(DartIdentifier node) {
        VariableElement nodeElement = ASTNodes.getVariableElement(node);
        if (nodeElement == element) {
          int nodeOffset = node.getSourceInfo().getOffset();
          if (nodeOffset > selectionStart + selectionLength) {
            result.set(true);
          }
        }
        return null;
      }
    });
    return result.get();
  }
}
