/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.services.internal.refactoring;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.internal.util.TokenUtils;
import com.google.dart.engine.services.refactoring.ExtractMethodRefactoring;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.ParameterInfo;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.SubProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of {@link ExtractMethodRefactoring}.
 */
public class ExtractMethodRefactoringImpl extends RefactoringImpl implements
    ExtractMethodRefactoring {
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
    List<Token> selectionTokens = TokenUtils.getTokens(s);
    return StringUtils.join(selectionTokens, TOKEN_SEPARATOR);
  }

  /**
   * @return <code>true</code> if given {@link DartNode} is left hand side of assignment, or
   *         declaration of the variable.
   */
  private static boolean isLeftHandOfAssignment(SimpleIdentifier node) {
    if (node.inSetterContext()) {
      return true;
    }
    return node.getParent() instanceof VariableDeclaration
        && ((VariableDeclaration) node.getParent()).getName() == node;
  }

  private final AssistContext context;

  private final SourceRange selectionRange;
  private final CompilationUnit unitNode;
  private final CorrectionUtils utils;

  private String methodName;
  private boolean replaceAllOccurrences = true;

  private ExtractMethodAnalyzer selectionAnalyzer;
  private final Set<String> usedNames = Sets.newHashSet();

  private VariableElement returnVariable;
  private final List<ParameterInfo> parameters = Lists.newArrayList();
  private ASTNode parentMember;
  private Expression selectionExpression;

  private List<Statement> selectionStatements;
  private final Map<String, List<SourceRange>> selectionParametersToRanges = Maps.newHashMap();

  private final List<Occurrence> occurrences = Lists.newArrayList();
  private boolean staticContext;

  public ExtractMethodRefactoringImpl(AssistContext context) throws Exception {
    this.context = context;
    this.selectionRange = context.getSelectionRange();
    this.unitNode = context.getCompilationUnit();
    this.utils = new CorrectionUtils(unitNode);
  }

  @Override
  public RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking final conditions", 3);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // name
      result.merge(NamingConventions.validateMethodName(methodName).escalateErrorToFatal());
      pm.worked(1);
      // parameters
      result.merge(checkParameterNames());
      pm.worked(1);
      // conflicts
      result.merge(checkPossibleConflicts(new SubProgressMonitor(pm, 1)));
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking initial conditions", 2);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // selection
      result.merge(checkSelection());
      if (result.hasFatalError()) {
        return result;
      }
      pm.worked(1);
      // prepare parts
      result.merge(initializeParameters());
      initializeOccurrences();
      pm.worked(1);
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public RefactoringStatus checkMethodName() {
    return NamingConventions.validateMethodName(methodName);
  }

  @Override
  public RefactoringStatus checkParameterNames() {
    RefactoringStatus result = new RefactoringStatus();
    for (ParameterInfo parameter : parameters) {
      result.merge(NamingConventions.validateParameterName(parameter.getNewName()));
      for (ParameterInfo other : parameters) {
        if (parameter != other && StringUtils.equals(other.getNewName(), parameter.getNewName())) {
          result.addError(MessageFormat.format(
              "Parameter ''{0}'' already exists",
              other.getNewName()));
          return result;
        }
      }
      if (parameter.isRenamed() && usedNames.contains(parameter.getNewName())) {
        result.addError(MessageFormat.format(
            "''{0}'' is already used as a name in the selected code",
            parameter.getNewName()));
        return result;
      }
    }
    return result;
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Creating change", 1 + occurrences.size());
    try {
      SourceChange change = new SourceChange(getRefactoringName(), context.getSource());
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
            String varTypeName = utils.getTypeSource(returnVariable.getType());
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
        Edit edit = new Edit(range, invocationSource);
        String msg = MessageFormat.format(occurence.isSelection
            ? "Substitute statements with call to ''{0}''"
            : "Replace duplicate code fragment with call to ''{0}''", methodName);
        change.addEdit(msg, edit);
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
            String returnTypeName = utils.getTypeSource(selectionExpression);
            if (returnTypeName != null && !returnTypeName.equals("dynamic")) {
              annotations += returnTypeName + " ";
            }
            // just return expression
            declarationSource = annotations + getSignature(methodName) + " => "
                + returnExpressionSource + ";";
          }
          // statements
          if (selectionStatements != null) {
            if (returnVariable != null) {
              String returnTypeName = utils.getTypeSource(returnVariable.getType());
              if (returnTypeName != null && !returnTypeName.equals("dynamic")) {
                annotations += returnTypeName + " ";
              }
            } else {
              annotations += "void ";
            }
            declarationSource = annotations + getSignature(methodName) + " {" + eol;
            declarationSource += returnExpressionSource;
            if (returnVariable != null) {
              declarationSource += prefix + "  return " + returnVariable.getName() + ";" + eol;
            }
            declarationSource += prefix + "}";
          }
        }
        // insert declaration
        if (declarationSource != null) {
          int offset = parentMember.getEnd();
          Edit edit = new Edit(offset, 0, eol + eol + prefix + declarationSource);
          String msg = MessageFormat.format(selectionExpression != null
              ? "Create new method ''{0}'' from selected expression"
              : "Create new method ''{0}'' from selected statements", methodName);
          change.addEdit(msg, edit);
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
  public int getNumberOfDuplicates() {
    return occurrences.size() - 1;
  }

  @Override
  public List<ParameterInfo> getParameters() {
    return parameters;
  }

  @Override
  public String getRefactoringName() {
    ASTNode coveringNode = context.getCoveringNode();
    if (coveringNode != null && coveringNode.getAncestor(ClassDeclaration.class) != null) {
      return "Extract Method";
    }
    return "Extract Function";
  }

  @Override
  public boolean getReplaceAllOccurrences() {
    return replaceAllOccurrences;
  }

  @Override
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

  @Override
  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  @Override
  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    this.replaceAllOccurrences = replaceAllOccurrences;
  }

  /**
   * Checks if created method will shadow or will be shadowed by other elements.
   */
  private RefactoringStatus checkPossibleConflicts(ProgressMonitor pm) throws Exception {
    final RefactoringStatus result = new RefactoringStatus();
    // top-level function
    if (parentMember.getParent() instanceof CompilationUnit) {
      CompilationUnit unit = (CompilationUnit) parentMember.getParent();
      RenameUnitMemberValidator validator = new RenameUnitMemberValidator(
          context.getSearchEngine(),
          unit.getElement(),
          ElementKind.FUNCTION,
          methodName);
      result.merge(validator.validate(pm, false));
    }
    // method of class
    if (parentMember.getParent() instanceof ClassDeclaration) {
      ClassDeclaration classDeclaration = (ClassDeclaration) parentMember.getParent();
      final ClassElement classElement = classDeclaration.getElement();
      RenameClassMemberValidator validator = new RenameClassMemberValidator(
          context.getSearchEngine(),
          ElementKind.METHOD,
          classElement,
          null,
          methodName);
      result.merge(validator.validate(pm, false));
    }
    // OK
    return result;
  }

  /**
   * Checks if {@link #selectionRange} selects {@link Expression} which can be extracted, and
   * location of this {@link DartExpression} in AST allows extracting.
   */
  private RefactoringStatus checkSelection() {
    selectionAnalyzer = new ExtractMethodAnalyzer(utils, selectionRange);
    unitNode.accept(selectionAnalyzer);
    // may be fatal error
    {
      RefactoringStatus status = selectionAnalyzer.getStatus();
      if (status.hasFatalError()) {
        return status;
      }
    }
    // check selected nodes
    List<ASTNode> selectedNodes = selectionAnalyzer.getSelectedNodes();
    if (!selectedNodes.isEmpty()) {
      parentMember = CorrectionUtils.getEnclosingExecutableNode(selectionAnalyzer.getCoveringNode());
      // single expression selected
      if (selectedNodes.size() == 1
          && !utils.selectionIncludesNonWhitespaceOutsideNode(
              selectionRange,
              selectionAnalyzer.getFirstSelectedNode())) {
        ASTNode selectedNode = selectionAnalyzer.getFirstSelectedNode();
        if (selectedNode instanceof Expression) {
          selectionExpression = (Expression) selectedNode;
          return new RefactoringStatus();
        }
      }
      // statements selected
      {
        List<Statement> selectedStatements = Lists.newArrayList();
        for (ASTNode selectedNode : selectedNodes) {
          if (selectedNode instanceof Statement) {
            selectedStatements.add((Statement) selectedNode);
          }
        }
        if (selectedStatements.size() == selectedNodes.size()) {
          selectionStatements = selectedStatements;
          return new RefactoringStatus();
        }
      }
    }
    // invalid selection
    return RefactoringStatus.createFatalErrorStatus("Can only extract a single expression or a set of statements.");
  }

  /**
   * @return the selected {@link DartExpression} source, with applying new parameter names.
   */
  private String getMethodBodySource() {
    String source = utils.getText(selectionRange);
    // prepare ReplaceEdit operations to replace variables with parameters
    List<Edit> replaceEdits = Lists.newArrayList();
    for (Entry<String, List<SourceRange>> entry : selectionParametersToRanges.entrySet()) {
      String name = entry.getKey();
      for (ParameterInfo parameter : parameters) {
        if (StringUtils.equals(name, parameter.getOldName())) {
          for (SourceRange range : entry.getValue()) {
            replaceEdits.add(new Edit(
                range.getOffset() - selectionRange.getOffset(),
                range.getLength(),
                parameter.getNewName()));
          }
        }
      }
    }
    // apply replacements
    source = CorrectionUtils.applyReplaceEdits(source, replaceEdits);
    // change indentation
    if (selectionStatements != null) {
      String selectionIndent = utils.getNodePrefix(selectionStatements.get(0));
      String targetIndent = utils.getNodePrefix(parentMember) + "  ";
      source = utils.getIndentSource(source, selectionIndent, targetIndent);
    }
    // done
    return source;
  }

  private SourcePattern getSourcePattern(final SourceRange partRange) {
    String originalSource = utils.getText(partRange.getOffset(), partRange.getLength());
    final SourcePattern pattern = new SourcePattern();
    final List<Edit> replaceEdits = Lists.newArrayList();
    unitNode.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        SourceRange nodeRange = SourceRangeFactory.rangeNode(node);
        if (partRange.covers(nodeRange)) {
          VariableElement variableElement = CorrectionUtils.getLocalOrParameterVariableElement(node);
          if (variableElement != null) {
            String originalName = variableElement.getName();
            String patternName = pattern.originalToPatternNames.get(originalName);
            if (patternName == null) {
              patternName = "__dartEditorVariable" + pattern.originalToPatternNames.size();
              pattern.originalToPatternNames.put(originalName, patternName);
            }
            replaceEdits.add(new Edit(
                nodeRange.getOffset() - partRange.getOffset(),
                nodeRange.getLength(),
                patternName));
          }
        }
        return null;
      }
    });
    pattern.patternSource = CorrectionUtils.applyReplaceEdits(originalSource, replaceEdits);
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
    ASTNode enclosingMemberParent;
    {
      ASTNode coveringNode = selectionAnalyzer.getCoveringNode();
      ASTNode parentMember = CorrectionUtils.getEnclosingExecutableNode(coveringNode);
      enclosingMemberParent = parentMember.getParent();
    }
    // visit nodes which will able to access extracted method
    enclosingMemberParent.accept(new GeneralizingASTVisitor<Void>() {
      boolean forceStatic = false;

      @Override
      public Void visitBlock(Block node) {
        if (selectionStatements != null) {
          List<Statement> blockStatements = node.getStatements();
          int beginStatementIndex = 0;
          int selectionCount = selectionStatements.size();
          while (beginStatementIndex + selectionCount <= blockStatements.size()) {
            SourceRange nodeRange = SourceRangeFactory.rangeStartEnd(
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
      public Void visitConstructorInitializer(ConstructorInitializer node) {
        forceStatic = true;
        try {
          return super.visitConstructorInitializer(node);
        } finally {
          forceStatic = false;
        }
      }

      @Override
      public Void visitExpression(Expression node) {
        if (selectionExpression != null && node.getClass() == selectionExpression.getClass()) {
          SourceRange nodeRange = SourceRangeFactory.rangeNode(node);
          tryToFindOccurrence(nodeRange);
        }
        return super.visitExpression(node);
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        forceStatic = node.isStatic();
        try {
          return super.visitMethodDeclaration(node);
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
          Occurrence occurrence = new Occurrence(nodeRange, selectionRange.intersects(nodeRange));
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
    unitNode.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        SourceRange nodeRange = SourceRangeFactory.rangeNode(node);
        if (selectionRange.covers(nodeRange)) {
          // analyze local variable
          VariableElement variableElement = CorrectionUtils.getLocalOrParameterVariableElement(node);
          if (variableElement != null) {
            // if declared outside, add parameter
            if (!isDeclaredInSelection(variableElement)) {
              String variableName = variableElement.getName();
              // add parameter
              if (!selectionParametersToRanges.containsKey(variableName)) {
                Type paraType = variableElement.getType();
                String paraTypeName = utils.getTypeSource(paraType);
                parameters.add(new ParameterInfoImpl(paraTypeName, variableName));
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
            // remember, if assigned and used after selection
            if (isLeftHandOfAssignment(node) && isUsedAfterSelection(variableElement)) {
              if (!assignedUsedVariables.contains(variableElement)) {
                assignedUsedVariables.add(variableElement);
              }
            }
          }
          // remember declaration names
          if (node.inDeclarationContext()) {
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
      result.addFatalError(MessageFormat.format("Ambiguous return value: "
          + "Selected block contains more than one assignment to local variables. "
          + "Affected variables are:\\n\\n{0}", sb.toString().trim()));
    }
    // done
    return result;
  }

  /**
   * @return <code>true</code> if the given {@link VariableElement} is declared inside of
   *         {@link #selectionRange}.
   */
  private boolean isDeclaredInSelection(VariableElement element) {
    return selectionRange.contains(element.getNameOffset());
  }

  /**
   * @return <code>true</code> if the given {@link VariableElement} is referenced after the
   *         {@link #selectionRange}.
   */
  private boolean isUsedAfterSelection(final VariableElement element) {
    final AtomicBoolean result = new AtomicBoolean();
    parentMember.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        VariableElement nodeElement = CorrectionUtils.getLocalVariableElement(node);
        if (nodeElement == element) {
          int nodeOffset = node.getOffset();
          if (nodeOffset > selectionRange.getEnd()) {
            result.set(true);
          }
        }
        return null;
      }
    });
    return result.get();
  }
}
