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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SymbolLiteral;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.MergeCompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.change.SourceChangeManager;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.refactoring.InlineMethodRefactoring;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getExpressionParentPrecedence;
import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getExpressionPrecedence;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeFromBase;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartStart;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Implementation of {@link InlineMethodRefactoring}.
 */
public class InlineMethodRefactoringImpl extends RefactoringImpl implements InlineMethodRefactoring {
  private static class ParameterOccurrence {
    final int parentPrecedence;
    final SourceRange range;

    public ParameterOccurrence(int parentPrecedence, SourceRange range) {
      this.parentPrecedence = parentPrecedence;
      this.range = range;
    }
  }

  /**
   * Processor for single {@link SearchMatch} reference to {@link #methodElement}.
   */
  private class ReferenceProcessor {
    private final Source refSource;
    private final CorrectionUtils refUtils;
    private final AstNode node;
    private final SourceRange refLineRange;
    private final String refPrefix;
    private boolean argsHaveSideEffect;

    ReferenceProcessor(SearchMatch reference) throws Exception {
      // prepare SourceChange to update
      Element refElement = reference.getElement();
      refSource = refElement.getSource();
      // prepare CorrectionUtils
      CompilationUnit refUnit = refElement.getUnit();
      refUtils = new CorrectionUtils(refUnit);
      // prepare node and environment
      node = refUtils.findNode(reference.getSourceRange().getOffset());
      Statement refStatement = node.getAncestor(Statement.class);
      if (refStatement != null) {
        refLineRange = refUtils.getLinesRange(ImmutableList.of(refStatement));
        refPrefix = refUtils.getNodePrefix(refStatement);
      } else {
        refLineRange = null;
        refPrefix = refUtils.getLinePrefix(node.getOffset());
      }
    }

    public void updateRequiresPreviewFlag() {
      if (!shouldProcess()) {
        return;
      }
      requiresPreview |= argsHaveSideEffect;
    }

    void checkForSideEffects() throws Exception {
      AstNode nodeParent = node.getParent();
      // may be invocation of inline method
      if (nodeParent instanceof MethodInvocation) {
        MethodInvocation invocation = (MethodInvocation) nodeParent;
        List<Expression> arguments = invocation.getArgumentList().getArguments();
        argsHaveSideEffect = hasSideEffect(arguments);
      } else {
        if (methodElement instanceof PropertyAccessorElement) {
          // prepare arguments
          List<Expression> arguments = Lists.newArrayList();
          if (((SimpleIdentifier) node).inSetterContext()) {
            arguments.add(((AssignmentExpression) nodeParent.getParent()).getRightHandSide());
          }
          // check arguments
          argsHaveSideEffect = hasSideEffect(arguments);
        }
      }
    }

    void process(RefactoringStatus status) throws Exception {
      AstNode nodeParent = node.getParent();
      // may be only single place should be inlined
      if (!shouldProcess()) {
        return;
      }
      // may be invocation of inline method
      if (nodeParent instanceof MethodInvocation) {
        MethodInvocation invocation = (MethodInvocation) nodeParent;
        Expression target = invocation.getTarget();
        List<Expression> arguments = invocation.getArgumentList().getArguments();
        inlineMethodInvocation(status, invocation, invocation.isCascaded(), target, arguments);
      } else {
        // cannot inline reference to method: var v = new A().method;
        if (methodElement instanceof MethodElement) {
          status.addFatalError(
              "Cannot inline class method reference.",
              new RefactoringStatusContext(node));
          return;
        }
        // PropertyAccessorElement
        if (methodElement instanceof PropertyAccessorElement) {
          Expression target = null;
          boolean cascade = false;
          // TODO(scheglov) hopefully at some point in future we will have only PropertyAccess
          if (nodeParent instanceof PrefixedIdentifier) {
            PrefixedIdentifier propertyAccess = (PrefixedIdentifier) nodeParent;
            target = propertyAccess.getPrefix();
            cascade = false;
          }
          if (nodeParent instanceof PropertyAccess) {
            PropertyAccess propertyAccess = (PropertyAccess) nodeParent;
            target = propertyAccess.getRealTarget();
            cascade = propertyAccess.isCascaded();
          }
          // prepare arguments
          List<Expression> arguments = Lists.newArrayList();
          if (((SimpleIdentifier) node).inSetterContext()) {
            arguments.add(((AssignmentExpression) nodeParent.getParent()).getRightHandSide());
          }
          // inline body
          inlineMethodInvocation(status, (Expression) nodeParent, cascade, target, arguments);
          return;
        }
        // not invocation, just reference to function
        String source;
        {
          source = methodUtils.getText(rangeStartEnd(
              methodParameters.getLeftParenthesis(),
              methodNode));
          String methodPrefix = methodUtils.getLinePrefix(methodNode.getOffset());
          source = refUtils.getIndentSource(source, methodPrefix, refPrefix);
          source = source.trim();
        }
        // do insert
        SourceChange refChange = safeManager.get(refSource);
        SourceRange range = rangeNode(node);
        Edit edit = new Edit(range, source);
        refChange.addEdit(edit, "Replace all references to method with statements");
      }
    }

    private boolean canInlineBody(AstNode usage) {
      // no statements, usually just expression
      if (methodStatementsPart == null) {
        // empty method, inline as closure
        if (methodExpressionPart == null) {
          return false;
        }
        // OK, just expression
        return true;
      }
      // analyze point of invocation
      AstNode parent = usage.getParent();
      AstNode parent2 = parent.getParent();
      // OK, if statement in block
      if (parent instanceof Statement) {
        return parent2 instanceof Block;
      }
      // may be assignment, in block
      if (parent instanceof AssignmentExpression) {
        AssignmentExpression assignment = (AssignmentExpression) parent;
        // inlining setter
        if (assignment.getLeftHandSide() == usage) {
          return parent2 instanceof Statement && parent2.getParent() instanceof Block;
        }
        // inlining initializer
        return methodExpressionPart != null;
      }
      // may be value for variable initializer, in block
      if (methodExpressionPart != null) {
        if (parent instanceof VariableDeclaration) {
          if (parent2 instanceof VariableDeclarationList) {
            AstNode parent3 = parent2.getParent();
            return parent3 instanceof VariableDeclarationStatement
                && parent3.getParent() instanceof Block;
          }
        }
      }
      // not in block, cannot inline body
      return false;
    }

    private void inlineMethodInvocation(RefactoringStatus status, Expression methodUsage,
        boolean cascaded, Expression target, List<Expression> arguments) {
      // prepare change
      SourceChange refChange;
      if (argsHaveSideEffect) {
        refChange = previewManager.get(refSource);
      } else {
        refChange = safeManager.get(refSource);
      }
      // we don't support cascade
      if (cascaded) {
        status.addError("Cannot inline cascade invocation.", new RefactoringStatusContext(
            methodUsage));
      }
      // can we inline method body into "methodUsage" block?
      if (canInlineBody(methodUsage)) {
        // insert non-return statements
        if (methodStatementsPart != null) {
          // prepare statements source for invocation
          String source = getMethodSourceForInvocation(
              methodStatementsPart,
              refUtils,
              methodUsage,
              target,
              arguments);
          source = refUtils.getIndentSource(source, methodStatementsPart.prefix, refPrefix);
          // do insert
          SourceRange range = rangeStartLength(refLineRange, 0);
          Edit edit = new Edit(range, source);
          refChange.addEdit(edit, "Replace all references to method with statements");
        }
        // replace invocation with return expression
        if (methodExpressionPart != null) {
          // prepare expression source for invocation
          String source = getMethodSourceForInvocation(
              methodExpressionPart,
              refUtils,
              methodUsage,
              target,
              arguments);
          if (getExpressionPrecedence(methodExpression) < getExpressionParentPrecedence(methodUsage)) {
            source = "(" + source + ")";
          }
          // do replace
          SourceRange methodUsageRange = rangeNode(methodUsage);
          Edit edit = new Edit(methodUsageRange, source);
          refChange.addEdit(edit, "Replace all references to method with statements");
        } else {
          Edit edit = new Edit(refLineRange, "");
          refChange.addEdit(edit, "Replace all references to method with statements");
        }
        return;
      }
      // inline as closure invocation
      String source;
      {
        source = methodUtils.getText(rangeStartEnd(
            methodParameters.getLeftParenthesis(),
            methodNode));
        String methodPrefix = methodUtils.getLinePrefix(methodNode.getOffset());
        source = refUtils.getIndentSource(source, methodPrefix, refPrefix);
        source = source.trim();
      }
      // do insert
      SourceRange range = rangeNode(node);
      Edit edit = new Edit(range, source);
      refChange.addEdit(edit, "Replace all references to method with statements");
    }

    private boolean shouldProcess() {
      if (currentMode == Mode.INLINE_SINGLE) {
        SourceRange parentRange = rangeNode(node);
        return parentRange.contains(context.getSelectionOffset());
      }
      return true;
    }
  }

  /**
   * Information about part of the source in {@link #methodUnit}.
   */
  private static class SourcePart {
    private final SourceRange baseRange;
    final String source;
    final String prefix;
    final Map<ParameterElement, List<ParameterOccurrence>> parameters = Maps.newHashMap();
    final Map<VariableElement, List<SourceRange>> variables = Maps.newHashMap();
    final List<SourceRange> instanceFieldQualifiers = Lists.newArrayList();
    final Map<String, List<SourceRange>> staticFieldQualifiers = Maps.newHashMap();

    public SourcePart(SourceRange baseRange, String source, String prefix) {
      this.baseRange = baseRange;
      this.source = source;
      this.prefix = prefix;
    }

    public void addInstanceFieldQualifier(SourceRange range) {
      range = rangeFromBase(range, baseRange);
      instanceFieldQualifiers.add(range);
    }

    public void addParameterOccurrence(ParameterElement parameter, SourceRange range, int precedence) {
      if (parameter != null) {
        List<ParameterOccurrence> occurrences = parameters.get(parameter);
        if (occurrences == null) {
          occurrences = Lists.newArrayList();
          parameters.put(parameter, occurrences);
        }
        range = rangeFromBase(range, baseRange);
        occurrences.add(new ParameterOccurrence(precedence, range));
      }
    }

    public void addStaticFieldQualifier(String className, SourceRange range) {
      List<SourceRange> ranges = staticFieldQualifiers.get(className);
      if (ranges == null) {
        ranges = Lists.newArrayList();
        staticFieldQualifiers.put(className, ranges);
      }
      range = rangeFromBase(range, baseRange);
      ranges.add(range);
    }

    public void addVariable(VariableElement element, SourceRange range) {
      List<SourceRange> ranges = variables.get(element);
      if (ranges == null) {
        ranges = Lists.newArrayList();
        variables.put(element, ranges);
      }
      range = rangeFromBase(range, baseRange);
      ranges.add(range);
    }
  }

  /**
   * If the given {@link AstNode} is a some {@link ClassDeclaration}, returns the
   * {@link ClassElement}. Otherwise returns {@code null}.
   */
  private static ClassElement getEnclosingClassElement(AstNode node) {
    ClassDeclaration enclosingClassNode = node.getAncestor(ClassDeclaration.class);
    if (enclosingClassNode != null) {
      return enclosingClassNode.getElement();
    }
    return null;
  }

  private final AssistContext context;
  private SourceChangeManager safeManager;
  private SourceChangeManager previewManager;
  private Mode initialMode;

  private List<ReferenceProcessor> referenceProcessors = Lists.newArrayList();
  private boolean requiresPreview;
  private Mode currentMode;
  private boolean deleteSource;
  private ExecutableElement methodElement;
  private CompilationUnit methodUnit;
  private CorrectionUtils methodUtils;
  private AstNode methodNode;
  private FormalParameterList methodParameters;

  private FunctionBody methodBody;
  private Expression methodExpression;

  private SourcePart methodExpressionPart;

  private SourcePart methodStatementsPart;

  public InlineMethodRefactoringImpl(AssistContext context) throws Exception {
    this.context = context;
  }

  @Override
  public boolean canDeleteSource() {
    // TODO(scheglov) check that declaration and all references can be updated 
    return true;
  }

  @Override
  public RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking final conditions", 5);
    RefactoringStatus result = new RefactoringStatus();
    try {
      safeManager = new SourceChangeManager();
      previewManager = new SourceChangeManager();
      // prepare changes
      for (ReferenceProcessor processor : referenceProcessors) {
        processor.process(result);
      }
      // delete method
      if (deleteSource && currentMode == Mode.INLINE_ALL) {
        SourceRange methodRange = rangeNode(methodNode);
        SourceRange linesRange = methodUtils.getLinesRange(methodRange);
        SourceChange change = safeManager.get(methodElement.getSource());
        change.addEdit(new Edit(linesRange, ""), "Remove method declaration");
      }
    } finally {
      pm.done();
    }
    return result;
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking initial conditions", 3);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // prepare method information
      result.merge(prepareMethod());
      if (result.hasFatalError()) {
        return result;
      }
      pm.worked(1);
      // may be operator
      if (methodElement.isOperator()) {
        return RefactoringStatus.createFatalErrorStatus("Cannot inline operator.");
      }
      // analyze method body
      result.merge(prepareMethodParts());
      pm.worked(1);
      // process references
      {
        requiresPreview = false;
        // find references
        List<SearchMatch> references = context.getSearchEngine().searchReferences(
            methodElement,
            null,
            null);
        // prepare reference processors
        referenceProcessors.clear();
        for (SearchMatch reference : references) {
          ReferenceProcessor processor = new ReferenceProcessor(reference);
          referenceProcessors.add(processor);
          processor.checkForSideEffects();
        }
        // update preview flag
        updateRequiresPreviewFlag();
      }
      pm.worked(1);
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    try {
      SourceChange[] safeChanges = safeManager.getChanges();
      SourceChange[] previewChanges = previewManager.getChanges();
      if (previewChanges.length == 0) {
        CompositeChange compositeChange = new CompositeChange(getRefactoringName());
        compositeChange.add(safeChanges);
        return compositeChange;
      } else {
        CompositeChange safeChange = new CompositeChange("(Safe changes)");
        CompositeChange previewChange = new CompositeChange("(Has arguments with side-effects)");
        safeChange.add(safeChanges);
        previewChange.add(previewChanges);
        return new MergeCompositeChange(getRefactoringName(), previewChange, safeChange);
      }
    } finally {
      pm.done();
    }
  }

  @Override
  public ExecutableElement getElement() {
    return methodElement;
  }

  @Override
  public Mode getInitialMode() {
    return initialMode;
  }

  @Override
  public String getRefactoringName() {
    if (methodElement instanceof MethodElement) {
      return "Inline Method";
    } else {
      return "Inline Function";
    }
  }

  @Override
  public boolean requiresPreview() {
    return requiresPreview;
  }

  @Override
  public void setCurrentMode(Mode currentMode) {
    this.currentMode = currentMode;
    updateRequiresPreviewFlag();
  }

  @Override
  public void setDeleteSource(boolean delete) {
    this.deleteSource = delete;
  }

  private SourcePart createSourcePart(final SourceRange sourceRange) {
    final SourcePart result;
    {
      String source = methodUtils.getText(sourceRange);
      String prefix = CorrectionUtils.getLinesPrefix(source);
      result = new SourcePart(sourceRange, source, prefix);
    }
    // remember parameters and variables occurrences
    methodUnit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitNode(AstNode node) {
        SourceRange nodeRange = rangeNode(node);
        if (!sourceRange.intersects(nodeRange)) {
          return null;
        }
        return super.visitNode(node);
      }

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        SourceRange nodeRange = rangeNode(node);
        if (sourceRange.covers(nodeRange)) {
          addInstanceFieldQualifier(node);
          addParameter(node);
          addVariable(node);
        }
        return null;
      }

      private void addInstanceFieldQualifier(SimpleIdentifier node) {
        PropertyAccessorElement accessor = CorrectionUtils.getPropertyAccessorElement(node);
        if (isFieldAccessorElement(accessor)) {
          AstNode qualifier = CorrectionUtils.getNodeQualifier(node);
          if (qualifier == null || qualifier instanceof ThisExpression) {
            if (accessor.isStatic()) {
              String className = accessor.getEnclosingElement().getDisplayName();
              if (qualifier == null) {
                SourceRange qualifierRange = rangeStartLength(node, 0);
                result.addStaticFieldQualifier(className, qualifierRange);
              }
            } else {
              SourceRange qualifierRange;
              if (qualifier != null) {
                qualifierRange = rangeStartStart(qualifier, node);
              } else {
                qualifierRange = rangeStartLength(node, 0);
              }
              result.addInstanceFieldQualifier(qualifierRange);
            }
          }
        }
      }

      private void addParameter(SimpleIdentifier node) {
        ParameterElement parameterElement = CorrectionUtils.getParameterElement(node);
        // not parameter
        if (parameterElement == null) {
          return;
        }
        // not parameter of a function being inlined
        if (!ArrayUtils.contains(methodElement.getParameters(), parameterElement)) {
          return;
        }
        // OK, add occurrence
        SourceRange nodeRange = rangeNode(node);
        int parentPrecedence = getExpressionParentPrecedence(node);
        result.addParameterOccurrence(parameterElement, nodeRange, parentPrecedence);
      }

      private void addVariable(SimpleIdentifier node) {
        VariableElement variableElement = CorrectionUtils.getLocalVariableElement(node);
        if (variableElement != null) {
          SourceRange nodeRange = rangeNode(node);
          result.addVariable(variableElement, nodeRange);
        }
      }
    });
    // done
    return result;
  }

  /**
   * @return the source which should replace given invocation with given arguments.
   */
  private String getMethodSourceForInvocation(SourcePart part, CorrectionUtils utils,
      AstNode contextNode, Expression targetExpression, List<Expression> arguments) {
    // prepare edits to replace parameters with arguments
    List<Edit> edits = Lists.newArrayList();
    for (Entry<ParameterElement, List<ParameterOccurrence>> entry : part.parameters.entrySet()) {
      ParameterElement parameter = entry.getKey();
      // prepare argument
      Expression argument = null;
      for (Expression arg : arguments) {
        if (Objects.equal(arg.getBestParameterElement(), parameter)) {
          argument = arg;
          break;
        }
      }
      if (argument instanceof NamedExpression) {
        argument = ((NamedExpression) argument).getExpression();
      }
      int argumentPrecedence = getExpressionPrecedence(argument);
      String argumentSource = utils.getText(argument);
      // replace all occurrences of this parameter
      for (ParameterOccurrence occurrence : entry.getValue()) {
        SourceRange range = occurrence.range;
        // prepare argument source to apply at this occurrence
        String occurrenceArgumentSource;
        if (argumentPrecedence < occurrence.parentPrecedence) {
          occurrenceArgumentSource = "(" + argumentSource + ")";
        } else {
          occurrenceArgumentSource = argumentSource;
        }
        // do replace
        edits.add(new Edit(range, occurrenceArgumentSource));
      }
    }
    // replace static field "qualifier" with invocation target
    for (Entry<String, List<SourceRange>> entry : part.staticFieldQualifiers.entrySet()) {
      String className = entry.getKey();
      for (SourceRange range : entry.getValue()) {
        edits.add(new Edit(range, className + "."));
      }
    }
    // replace instance field "qualifier" with invocation target
    if (targetExpression != null) {
      String targetSource = utils.getText(targetExpression) + ".";
      for (SourceRange qualifierRange : part.instanceFieldQualifiers) {
        edits.add(new Edit(qualifierRange, targetSource));
      }
    }
    // prepare edits to replace conflicting variables
    Set<String> conflictingNames = getNamesConflictingWithLocal(utils.getUnit(), contextNode);
    for (Entry<VariableElement, List<SourceRange>> entry : part.variables.entrySet()) {
      String originalName = entry.getKey().getDisplayName();
      // prepare unique name
      String uniqueName;
      {
        uniqueName = originalName;
        int uniqueIndex = 2;
        while (conflictingNames.contains(uniqueName)) {
          uniqueName = originalName + uniqueIndex;
          uniqueIndex++;
        }
      }
      // update references, if name was change
      if (!StringUtils.equals(uniqueName, originalName)) {
        for (SourceRange range : entry.getValue()) {
          edits.add(new Edit(range, uniqueName));
        }
      }
    }
    // prepare source with applied arguments
    return CorrectionUtils.applyReplaceEdits(part.source, edits);
  }

  private SourceRange getNamesConflictingRange(AstNode node) {
    // may be Block
    Block block = node.getAncestor(Block.class);
    if (block != null) {
      int offset = node.getOffset();
      int endOffset = block.getEnd();
      return rangeStartEnd(offset, endOffset);
    }
    // may be whole executable
    AstNode executableNode = CorrectionUtils.getEnclosingExecutableNode(node);
    if (executableNode != null) {
      return rangeNode(executableNode);
    }
    // not a part of declaration with locals
    return SourceRange.EMPTY;
  }

  /**
   * @return the names which will shadow or will be shadowed by any declaration at "node".
   */
  private Set<String> getNamesConflictingWithLocal(CompilationUnit unit, AstNode node) {
    final Set<String> result = Sets.newHashSet();
    // local variables and functions
    {
      final SourceRange offsetRange = getNamesConflictingRange(node);
      ExecutableElement enclosingExecutable = CorrectionUtils.getEnclosingExecutableElement(node);
      if (enclosingExecutable != null) {
        enclosingExecutable.accept(new GeneralizingElementVisitor<Void>() {
          @Override
          public Void visitLocalElement(LocalElement element) {
            SourceRange elementRange = element.getVisibleRange();
            if (elementRange != null && elementRange.intersects(offsetRange)) {
              result.add(element.getDisplayName());
            }
            return super.visitLocalElement(element);
          }
        });
      }
    }
    // fields
    {
      ClassElement enclosingClassElement = getEnclosingClassElement(node);
      if (enclosingClassElement != null) {
        Set<ClassElement> elements = Sets.newHashSet(enclosingClassElement);
        elements.addAll(HierarchyUtils.getSuperClasses(enclosingClassElement));
        for (ClassElement classElement : elements) {
          List<Element> classMembers = CorrectionUtils.getChildren(classElement);
          for (Element classMemberElement : classMembers) {
            result.add(classMemberElement.getDisplayName());
          }
        }
      }
    }
    // done
    return result;
  }

  /**
   * @return {@code true} if we can prove that the given {@link Expression} has no side effects.
   */
  private boolean hasSideEffect(Expression e) {
    if (e instanceof BooleanLiteral || e instanceof DoubleLiteral || e instanceof IntegerLiteral
        || e instanceof NullLiteral || e instanceof SimpleStringLiteral
        || e instanceof AdjacentStrings || e instanceof SymbolLiteral) {
      return false;
    }
    if (e instanceof ThisExpression) {
      return false;
    }
    if (e instanceof NamedExpression) {
      NamedExpression namedExpression = (NamedExpression) e;
      return hasSideEffect(namedExpression.getExpression());
    }
    if (e instanceof SimpleIdentifier) {
      SimpleIdentifier identifier = (SimpleIdentifier) e;
      Element element = identifier.getBestElement();
      if (element instanceof VariableElement) {
        return false;
      }
      if (element instanceof PropertyAccessorElement) {
        return !element.isSynthetic();
      }
    }
    if (e instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) e;
      return hasSideEffect(binary.getLeftOperand()) || hasSideEffect(binary.getRightOperand());
    }
    return true;
  }

  /**
   * @return {@code true} if we can prove that all of the given {@link Expression} have no side
   *         effects.
   */
  private boolean hasSideEffect(List<Expression> arguments) {
    for (Expression argument : arguments) {
      if (hasSideEffect(argument)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return <code>true</code> if given {@link PropertyAccessorElement} is accessor of some
   *         {@link FieldElement}.
   */
  private boolean isFieldAccessorElement(PropertyAccessorElement accessor) {
    return accessor != null && accessor.getVariable() instanceof FieldElement
        && accessor.getVariable().getEnclosingElement() instanceof ClassElement;
  }

  /**
   * Initializes "method*" fields.
   */
  private RefactoringStatus prepareMethod() throws Exception {
    methodElement = null;
    methodParameters = null;
    methodBody = null;
    // prepare selected SimpleIdentifier
    AstNode selectedNode = context.getCoveringNode();
    if (!(selectedNode instanceof SimpleIdentifier)) {
      return RefactoringStatus.createFatalErrorStatus("Method declaration or reference must be selected to activate this refactoring.");
    }
    SimpleIdentifier selectedIdentifier = (SimpleIdentifier) selectedNode;
    // prepare selected ExecutableElement
    Element selectedElement = selectedIdentifier.getBestElement();
    if (!(selectedElement instanceof ExecutableElement)) {
      return RefactoringStatus.createFatalErrorStatus("Method declaration or reference must be selected to activate this refactoring.");
    }
    methodElement = (ExecutableElement) selectedElement;
    methodUnit = selectedElement.getUnit();
    methodUtils = new CorrectionUtils(methodUnit);
    if (selectedElement instanceof MethodElement
        || selectedElement instanceof PropertyAccessorElement) {
      MethodDeclaration methodDeclaration = (MethodDeclaration) methodElement.getNode();
      methodNode = methodDeclaration;
      methodParameters = methodDeclaration.getParameters();
      methodBody = methodDeclaration.getBody();
      // prepare mode
      boolean isDeclaration = methodDeclaration.getName() == selectedNode;
      initialMode = currentMode = isDeclaration ? Mode.INLINE_ALL : Mode.INLINE_SINGLE;
    }
    if (selectedElement instanceof FunctionElement) {
      FunctionDeclaration functionDeclaration = (FunctionDeclaration) methodElement.getNode();
      methodNode = functionDeclaration;
      methodParameters = functionDeclaration.getFunctionExpression().getParameters();
      methodBody = functionDeclaration.getFunctionExpression().getBody();
      // prepare mode
      boolean isDeclaration = functionDeclaration.getName() == selectedNode;
      initialMode = currentMode = isDeclaration ? Mode.INLINE_ALL : Mode.INLINE_SINGLE;
    }
    // OK
    return new RefactoringStatus();
  }

  /**
   * Analyze {@link #methodBody} to fill {@link #methodExpressionPart} and
   * {@link #methodStatementsPart}.
   */
  private RefactoringStatus prepareMethodParts() {
    final RefactoringStatus result = new RefactoringStatus();
    if (methodBody instanceof ExpressionFunctionBody) {
      ExpressionFunctionBody body = (ExpressionFunctionBody) methodBody;
      methodExpression = body.getExpression();
      SourceRange methodExpressionRange = rangeNode(methodExpression);
      methodExpressionPart = createSourcePart(methodExpressionRange);
    } else if (methodBody instanceof BlockFunctionBody) {
      Block body = ((BlockFunctionBody) methodBody).getBlock();
      List<Statement> statements = body.getStatements();
      if (statements.size() >= 1) {
        Statement lastStatement = statements.get(statements.size() - 1);
        // "return" statement requires special handling
        if (lastStatement instanceof ReturnStatement) {
          methodExpression = ((ReturnStatement) lastStatement).getExpression();
          SourceRange methodExpressionRange = rangeNode(methodExpression);
          methodExpressionPart = createSourcePart(methodExpressionRange);
          // exclude "return" statement from statements
          statements = ImmutableList.copyOf(statements).subList(0, statements.size() - 1);
        }
        // if there are statements, process them
        if (!statements.isEmpty()) {
          SourceRange statementsRange = methodUtils.getLinesRange(statements);
          methodStatementsPart = createSourcePart(statementsRange);
        }
      }
      // check if more than one return
      body.accept(new RecursiveAstVisitor<Void>() {
        private int numReturns = 0;

        @Override
        public Void visitReturnStatement(ReturnStatement node) {
          numReturns++;
          if (numReturns == 2) {
            result.addError("Ambiguous return value.", new RefactoringStatusContext(node));
          }
          return super.visitReturnStatement(node);
        }
      });
    } else {
      return RefactoringStatus.createFatalErrorStatus("Cannot inline method without body.");
    }
    return result;
  }

  private void updateRequiresPreviewFlag() {
    requiresPreview = false;
    for (ReferenceProcessor processor : referenceProcessors) {
      processor.updateRequiresPreviewFlag();
    }
  }

}
