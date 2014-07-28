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
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.internal.util.TokenUtils;
import com.google.dart.engine.services.refactoring.ExtractMethodRefactoring;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.Parameter;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.SubProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collections;
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
   * @return {@code true} if the given {@link AstNode} has {@link MethodInvocation}.
   */
  private static boolean hasMethodInvocation(AstNode node) {
    final boolean[] result = {false};
    node.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        result[0] = true;
        return null;
      }
    });
    return result[0];
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

  private boolean extractGetter;
  private ExtractMethodAnalyzer selectionAnalyzer;

  private final Set<String> usedNames = Sets.newHashSet();
  private final List<Parameter> parameters = Lists.newArrayList();
  private final Map<String, Parameter> parametersMap = Maps.newHashMap();
  private final Map<String, List<SourceRange>> parameterReferencesMap = Maps.newHashMap();
  private Type returnType;
  private String returnVariableName;
  private AstNode parentMember;
  private Expression selectionExpression;

  private FunctionExpression selectionFunctionExpression;
  private List<Statement> selectionStatements;

  private final List<Occurrence> occurrences = Lists.newArrayList();

  private boolean staticContext;

  public ExtractMethodRefactoringImpl(AssistContext context) throws Exception {
    this.context = context;
    this.selectionRange = context.getSelectionRange();
    this.unitNode = context.getCompilationUnit();
    this.utils = new CorrectionUtils(unitNode);
  }

  @Override
  public boolean canExtractGetter() {
    if (!parameters.isEmpty()) {
      return false;
    }
    if (selectionExpression != null) {
      if (selectionExpression instanceof AssignmentExpression) {
        return false;
      }
    }
    if (selectionStatements != null) {
      return returnType != null;
    }
    return true;
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
      initializeGetter();
      pm.worked(1);
      // closure cannot have parameters
      if (selectionFunctionExpression != null && !parameters.isEmpty()) {
        String message = MessageFormat.format(
            "Cannot extract closure as method, it references {0} external variable(s).",
            parameters.size());
        return RefactoringStatus.createFatalErrorStatus(message);
      }
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
    for (Parameter parameter : parameters) {
      result.merge(NamingConventions.validateParameterName(parameter.getNewName()));
      for (Parameter other : parameters) {
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
        if (selectionFunctionExpression != null) {
          invocationSource = methodName;
        } else {
          StringBuilder sb = new StringBuilder();
          // may be returns value
          if (returnType != null) {
            String returnTypeName = utils.getTypeSource(returnType);
            // single variable assignment / return statement
            if (returnVariableName != null) {
              String occurrenceName = occurence.parameterOldToOccurrenceName.get(returnVariableName);
              // may be declare variable
              if (!parametersMap.containsKey(returnVariableName)) {
                if (returnTypeName.equals("dynamic")) {
                  sb.append("var ");
                } else {
                  sb.append(returnTypeName);
                  sb.append(" ");
                }
              }
              // assign the return value 
              sb.append(occurrenceName);
              sb.append(" = ");
            } else {
              sb.append("return ");
            }
          }
          // invocation itself
          sb.append(methodName);
          if (!extractGetter) {
            sb.append("(");
            boolean firstParameter = true;
            for (Parameter parameter : parameters) {
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
          }
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
        change.addEdit(edit, msg);
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
          // closure
          if (selectionFunctionExpression != null) {
            declarationSource = methodName + returnExpressionSource;
            if (selectionFunctionExpression.getBody() instanceof ExpressionFunctionBody) {
              declarationSource += ";";
            }
          }
          // expression
          if (selectionExpression != null) {
            // add return type
            String returnTypeName = utils.getTypeSource(selectionExpression);
            if (returnTypeName != null && !returnTypeName.equals("dynamic")) {
              annotations += returnTypeName + " ";
            }
            // just return expression
            declarationSource = annotations + getSignature() + " => " + returnExpressionSource
                + ";";
          }
          // statements
          if (selectionStatements != null) {
            if (returnType != null) {
              String returnTypeName = utils.getTypeSource(returnType);
              if (returnTypeName != null && !returnTypeName.equals("dynamic")) {
                annotations += returnTypeName + " ";
              }
            } else {
              annotations += "void ";
            }
            declarationSource = annotations + getSignature() + " {" + eol;
            declarationSource += returnExpressionSource;
            if (returnVariableName != null) {
              declarationSource += prefix + "  return " + returnVariableName + ";" + eol;
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
          change.addEdit(edit, msg);
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
  public boolean getExtractGetter() {
    return extractGetter;
  }

  @Override
  public int getNumberOfOccurrences() {
    return occurrences.size();
  }

  @Override
  public Parameter[] getParameters() {
    return parameters.toArray(new Parameter[parameters.size()]);
  }

  @Override
  public String getRefactoringName() {
    AstNode coveringNode = context.getCoveringNode();
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
  public String getSignature() {
    StringBuilder sb = new StringBuilder();
    if (extractGetter) {
      sb.append("get ");
      sb.append(methodName);
    } else {
      sb.append(methodName);
      sb.append("(");
      // add all parameters
      boolean firstParameter = true;
      for (Parameter parameter : parameters) {
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
      sb.append(")");
    }
    // done
    return sb.toString();
  }

  @Override
  public void setExtractGetter(boolean extractGetter) {
    this.extractGetter = extractGetter;
  }

  @Override
  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  @Override
  public void setParameters(Parameter[] parameters) {
    this.parameters.clear();
    Collections.addAll(this.parameters, parameters);
  }

  @Override
  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    this.replaceAllOccurrences = replaceAllOccurrences;
  }

  /**
   * Adds a new reference to the parameter with the given name.
   */
  private void addParameterReference(String name, SourceRange range) {
    List<SourceRange> references = parameterReferencesMap.get(name);
    if (references == null) {
      references = Lists.newArrayList();
      parameterReferencesMap.put(name, references);
    }
    references.add(range);
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
    List<AstNode> selectedNodes = selectionAnalyzer.getSelectedNodes();
    if (!selectedNodes.isEmpty()) {
      AstNode coveringNode = selectionAnalyzer.getCoveringNode();
      parentMember = CorrectionUtils.getEnclosingClassOrUnitMember(coveringNode);
      // single expression selected
      if (selectedNodes.size() == 1
          && !utils.selectionIncludesNonWhitespaceOutsideNode(
              selectionRange,
              selectionAnalyzer.getFirstSelectedNode())) {
        AstNode selectedNode = selectionAnalyzer.getFirstSelectedNode();
        if (selectedNode instanceof Expression) {
          selectionExpression = (Expression) selectedNode;
          // additional check for closure
          if (selectionExpression instanceof FunctionExpression) {
            selectionFunctionExpression = (FunctionExpression) selectionExpression;
            selectionExpression = null;
          }
          // OK
          return new RefactoringStatus();
        }
      }
      // statements selected
      {
        List<Statement> selectedStatements = Lists.newArrayList();
        for (AstNode selectedNode : selectedNodes) {
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
    for (Parameter parameter : parametersMap.values()) {
      List<SourceRange> ranges = parameterReferencesMap.get(parameter.getOldName());
      if (ranges != null) {
        for (SourceRange range : ranges) {
          replaceEdits.add(new Edit(
              range.getOffset() - selectionRange.getOffset(),
              range.getLength(),
              parameter.getNewName()));
        }
      }
    }
    // apply replacements
    source = CorrectionUtils.applyReplaceEdits(source, replaceEdits);
    // change indentation
    if (selectionFunctionExpression != null) {
      AstNode baseNode = selectionFunctionExpression.getAncestor(Statement.class);
      if (baseNode != null) {
        String baseIndent = utils.getNodePrefix(baseNode);
        String targetIndent = utils.getNodePrefix(parentMember);
        source = utils.getIndentSource(source, baseIndent, targetIndent);
        source = source.trim();
      }
    }
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
    unitNode.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        SourceRange nodeRange = SourceRangeFactory.rangeNode(node);
        if (partRange.covers(nodeRange)) {
          VariableElement variableElement = CorrectionUtils.getLocalOrParameterVariableElement(node);
          if (variableElement != null) {
            // name of the named expression
            if (CorrectionUtils.isNamedExpressionName(node)) {
              return null;
            }
            // continue
            String originalName = variableElement.getDisplayName();
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
   * Initializes {@link #extractGetter} flag.
   */
  private void initializeGetter() {
    extractGetter = false;
    // may be we cannot at all
    if (!canExtractGetter()) {
      return;
    }
    // OK, just expression
    if (selectionExpression != null) {
      extractGetter = !hasMethodInvocation(selectionExpression);
      return;
    }
    // allow code blocks without cycles
    if (selectionStatements != null) {
      extractGetter = true;
      for (Statement statement : selectionStatements) {
        // method is something heavy, so we don't want to extract it as part of getter
        if (hasMethodInvocation(statement)) {
          extractGetter = false;
          return;
        }
        // don't allow cycles
        statement.accept(new RecursiveAstVisitor<Void>() {
          @Override
          public Void visitDoStatement(DoStatement node) {
            extractGetter = false;
            return super.visitDoStatement(node);
          }

          @Override
          public Void visitForEachStatement(ForEachStatement node) {
            extractGetter = false;
            return super.visitForEachStatement(node);
          }

          @Override
          public Void visitForStatement(ForStatement node) {
            extractGetter = false;
            return super.visitForStatement(node);
          }

          @Override
          public Void visitWhileStatement(WhileStatement node) {
            extractGetter = false;
            return super.visitWhileStatement(node);
          }
        });
      }
    }
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
    // prepare an enclosing parent - class or unit
    AstNode enclosingMemberParent = parentMember.getParent();
    // visit nodes which will able to access extracted method
    enclosingMemberParent.accept(new GeneralizingAstVisitor<Void>() {
      boolean forceStatic = false;

      @Override
      public Void visitBlock(Block node) {
        if (selectionStatements != null) {
          visitStatements(node.getStatements());
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
        if (selectionFunctionExpression != null || selectionExpression != null
            && node.getClass() == selectionExpression.getClass()) {
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

      @Override
      public Void visitSwitchMember(SwitchMember node) {
        if (selectionStatements != null) {
          visitStatements(node.getStatements());
        }
        return super.visitSwitchMember(node);
      }

      /**
       * Checks if given {@link SourceRange} matched selection source and adds {@link Occurrence}.
       */
      private boolean tryToFindOccurrence(SourceRange nodeRange) {
        // check if can be extracted
        if (!isExtractable(nodeRange)) {
          return false;
        }
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

      private void visitStatements(List<Statement> statements) {
        int beginStatementIndex = 0;
        int selectionCount = selectionStatements.size();
        while (beginStatementIndex + selectionCount <= statements.size()) {
          SourceRange nodeRange = SourceRangeFactory.rangeStartEnd(
              statements.get(beginStatementIndex),
              statements.get(beginStatementIndex + selectionCount - 1));
          boolean found = tryToFindOccurrence(nodeRange);
          // next statement
          if (found) {
            beginStatementIndex += selectionCount;
          } else {
            beginStatementIndex++;
          }
        }
      }
    });
  }

  /**
   * Prepares information about used variables, which should be turned into parameters.
   */
  private RefactoringStatus initializeParameters() {
    parameters.clear();
    parametersMap.clear();
    parameterReferencesMap.clear();
    RefactoringStatus result = new RefactoringStatus();
    final List<VariableElement> assignedUsedVariables = Lists.newArrayList();
    unitNode.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        SourceRange nodeRange = SourceRangeFactory.rangeNode(node);
        if (selectionRange.covers(nodeRange)) {
          // analyze local variable
          VariableElement variableElement = CorrectionUtils.getLocalOrParameterVariableElement(node);
          if (variableElement != null) {
            // name of the named expression
            if (CorrectionUtils.isNamedExpressionName(node)) {
              return null;
            }
            // if declared outside, add parameter
            if (!isDeclaredInSelection(variableElement)) {
              String variableName = variableElement.getDisplayName();
              // add parameter
              Parameter parameter = parametersMap.get(variableName);
              if (parameter == null) {
                Type parameterType = node.getBestType();
                String parameterTypeName = utils.getTypeSource(parameterType);
                parameter = new ParameterImpl(parameterTypeName, variableName);
                parameters.add(parameter);
                parametersMap.put(variableName, parameter);
              }
              // add reference to parameter
              addParameterReference(variableName, nodeRange);
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
    // may be ends with "return" statement
    if (selectionStatements != null) {
      Statement lastStatement = selectionStatements.get(selectionStatements.size() - 1);
      if (lastStatement instanceof ReturnStatement) {
        Expression expression = ((ReturnStatement) lastStatement).getExpression();
        if (expression != null) {
          returnType = expression.getBestType();
        }
      }
    }
    // may be single variable to return
    if (assignedUsedVariables.size() == 1) {
      // we cannot both return variable and have explicit return statement
      if (returnType != null) {
        result.addFatalError("Ambiguous return value: "
            + "Selected block contains assignment(s) to local variables and return statement.");
        return result;
      }
      // prepare to return an assigned variable
      VariableElement returnVariable = assignedUsedVariables.get(0);
      returnType = returnVariable.getType();
      returnVariableName = returnVariable.getDisplayName();
    }
    // fatal, if multiple variables assigned and used after selection
    if (assignedUsedVariables.size() > 1) {
      StringBuilder sb = new StringBuilder();
      for (VariableElement variable : assignedUsedVariables) {
        sb.append(variable.getDisplayName());
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
   * @return {@code true} if it is OK to extract the node with the given {@link SourceRange}.
   */
  private boolean isExtractable(SourceRange range) {
    ExtractMethodAnalyzer analyzer = new ExtractMethodAnalyzer(utils, range);
    utils.getUnit().accept(analyzer);
    return analyzer.getStatus().isOK();
  }

  /**
   * @return <code>true</code> if the given {@link VariableElement} is referenced after the
   *         {@link #selectionRange}.
   */
  private boolean isUsedAfterSelection(final VariableElement element) {
    final AtomicBoolean result = new AtomicBoolean();
    parentMember.accept(new GeneralizingAstVisitor<Void>() {
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
