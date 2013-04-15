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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
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
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
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

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeFromBase;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartStart;

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
    private final RefactoringStatus result;
    private final SourceChange refChange;
    private final CorrectionUtils refUtils;
    private final ASTNode node;
    private final SourceRange refLineRange;
    private final String refPrefix;

    ReferenceProcessor(RefactoringStatus result, SearchMatch reference) throws Exception {
      this.result = result;
      // prepare SourceChange to update
      Element refElement = reference.getElement();
      Source refSource = refElement.getSource();
      refChange = changeManager.get(refSource);
      // prepare CorrectionUtils
      CompilationUnit refUnit = CorrectionUtils.getResolvedUnit(refElement);
      refUtils = new CorrectionUtils(refUnit);
      // prepare node and environment
      node = refUtils.findNode(reference.getSourceRange().getOffset(), ASTNode.class);
      Statement refStatement = node.getAncestor(Statement.class);
      refLineRange = refUtils.getLinesRange(ImmutableList.of(refStatement));
      refPrefix = refUtils.getNodePrefix(refStatement);
    }

    void process() throws Exception {
      ASTNode nodeParent = node.getParent();
      // may be only single place should be inlined
      if (currentMode == Mode.INLINE_SINGLE) {
        SourceRange parentRange = rangeNode(nodeParent);
        if (!parentRange.contains(context.getSelectionOffset())) {
          return;
        }
      }
      // may be invocation of inline method
      if (nodeParent instanceof MethodInvocation) {
        MethodInvocation invocation = (MethodInvocation) nodeParent;
        Expression target = invocation.getTarget();
        List<Expression> arguments = invocation.getArgumentList().getArguments();
        inlineMethodBody(invocation, invocation.isCascaded(), target, arguments);
      } else {
        // cannot inline reference to method: var v = new A().method;
        if (methodElement instanceof MethodElement) {
          result.addFatalError(
              "Cannot inline class method reference.",
              RefactoringStatusContext.create(node));
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
          inlineMethodBody(nodeParent, cascade, target, arguments);
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
        SourceRange range = rangeNode(node);
        Edit edit = new Edit(range, source);
        refChange.addEdit("Replace all references to method with statements", edit);
      }
    }

    private void inlineMethodBody(ASTNode methodUsage, boolean cascaded, Expression target,
        List<Expression> arguments) {
      // we don't support cascade
      if (cascaded) {
        result.addError(
            "Cannot inline cascade invocation.",
            RefactoringStatusContext.create(methodUsage));
      }
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
        refChange.addEdit("Replace all references to method with statements", edit);
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
        // do replace
        SourceRange methodUsageRange = rangeNode(methodUsage);
        Edit edit = new Edit(methodUsageRange, source);
        refChange.addEdit("Replace all references to method with statements", edit);
      } else {
        Edit edit = new Edit(refLineRange, "");
        refChange.addEdit("Replace all references to method with statements", edit);
      }
    }
  }

  /**
   * Information about part of the source in {@link #methodUnit}.
   */
  private static class SourcePart {
    private final SourceRange baseRange;
    final String source;
    final String prefix;
    final Map<Integer, List<ParameterOccurrence>> parameters = Maps.newHashMap();
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

    public void addParameterOccurrence(int index, SourceRange range, int precedence) {
      if (index != -1) {
        List<ParameterOccurrence> occurrences = parameters.get(index);
        if (occurrences == null) {
          occurrences = Lists.newArrayList();
          parameters.put(index, occurrences);
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
   * @return {@link #getExpressionPrecedence(ASTNode)} for parent node.
   */
  private static int getExpressionParentPrecedence(ASTNode node) {
    ASTNode parent = node.getParent();
    return getExpressionPrecedence(parent);
  }

  /**
   * @return the precedence of the operator, may be <code>1000</code> if not binary expression.
   */
  private static int getExpressionPrecedence(ASTNode node) {
    if (node instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) node;
      return binary.getOperator().getType().getPrecedence();
    }
    return 1000;
  }

  private final AssistContext context;
  private final SourceChangeManager changeManager = new SourceChangeManager();
  private Mode initialMode;

  private Mode currentMode;
  private boolean deleteSource;
  private ExecutableElement methodElement;
  private CompilationUnit methodUnit;
  private CorrectionUtils methodUtils;
  private ASTNode methodNode;
  private FormalParameterList methodParameters;
  private FunctionBody methodBody;
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
      // find references
      List<SearchMatch> references = context.getSearchEngine().searchReferences(
          methodElement,
          null,
          null);
      // replace all references
      for (SearchMatch reference : references) {
        new ReferenceProcessor(result, reference).process();
      }
      // delete method
      if (deleteSource && currentMode == Mode.INLINE_ALL) {
        SourceRange methodRange = rangeNode(methodNode);
        SourceRange linesRange = methodUtils.getLinesRange(methodRange);
        SourceChange change = changeManager.get(methodElement.getSource());
        change.addEdit("Remove method declaration", new Edit(linesRange, ""));
      }
    } finally {
      pm.done();
    }
    return result;
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking initial conditions", 2);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // prepare method information
      result.merge(prepareMethod());
      if (result.hasFatalError()) {
        return result;
      }
      pm.worked(1);
      // analyze method body
      result.merge(prepareMethodParts());
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
      CompositeChange compositeChange = new CompositeChange(getRefactoringName());
      compositeChange.add(changeManager.getChanges());
      return compositeChange;
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
  public void setCurrentMode(Mode currentMode) {
    this.currentMode = currentMode;
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
    methodUnit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitNode(ASTNode node) {
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
          ASTNode qualifier = CorrectionUtils.getNodeQualifier(node);
          if (qualifier == null || qualifier instanceof ThisExpression) {
            if (accessor.isStatic()) {
              String className = accessor.getEnclosingElement().getName();
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
        if (parameterElement != null) {
          int parameterIndex = CorrectionUtils.getParameterIndex(parameterElement);
          if (parameterIndex != -1) {
            SourceRange nodeRange = rangeNode(node);
            int parentPrecedence = getExpressionParentPrecedence(node);
            result.addParameterOccurrence(parameterIndex, nodeRange, parentPrecedence);
          }
        }
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
      ASTNode contextNode, Expression targetExpression, List<Expression> arguments) {
    // prepare edits to replace parameters with arguments
    List<Edit> edits = Lists.newArrayList();
    for (Entry<Integer, List<ParameterOccurrence>> entry : part.parameters.entrySet()) {
      int parameterIndex = entry.getKey();
      // prepare argument
      Expression argument = arguments.get(parameterIndex);
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
      String originalName = entry.getKey().getName();
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

  /**
   * @return the names which will shadow or will be shadowed by any declaration at "node".
   */
  private Set<String> getNamesConflictingWithLocal(CompilationUnit unit, ASTNode node) {
    final Set<String> result = Sets.newHashSet();
    // prepare offsets
    int offset = node.getOffset();
    final SourceRange offsetRange;
    {
      Block block = node.getAncestor(Block.class);
      int endOffset = block.getEnd();
      offsetRange = rangeStartEnd(offset, endOffset);
    }
    // local variables and functions
    {
      ExecutableElement enclosingExecutable = CorrectionUtils.getEnclosingExecutableElement(node);
      if (enclosingExecutable != null) {
        enclosingExecutable.accept(new GeneralizingElementVisitor<Void>() {
          @Override
          public Void visitLocalElement(LocalElement element) {
            SourceRange elementRange = element.getVisibleRange();
            if (elementRange != null && elementRange.intersects(offsetRange)) {
              result.add(element.getName());
            }
            return super.visitLocalElement(element);
          }
        });
      }
    }
    // fields
    {
      ClassDeclaration enclosingClassNode = node.getAncestor(ClassDeclaration.class);
      if (enclosingClassNode != null) {
        ClassElement enclosingClassElement = enclosingClassNode.getElement();
        if (enclosingClassElement != null) {
          Set<ClassElement> elements = Sets.newHashSet(enclosingClassElement);
          elements.addAll(HierarchyUtils.getSuperClasses(enclosingClassElement));
          for (ClassElement classElement : elements) {
            List<Element> classMembers = CorrectionUtils.getChildren(classElement);
            for (Element classMemberElement : classMembers) {
              result.add(classMemberElement.getName());
            }
          }
        }
      }
    }
    // done
    return result;
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
    ASTNode selectedNode = context.getCoveringNode();
    if (!(selectedNode instanceof SimpleIdentifier)) {
      return RefactoringStatus.createFatalErrorStatus("Method declaration or reference must be selected to activate this refactoring.");
    }
    SimpleIdentifier selectedIdentifier = (SimpleIdentifier) selectedNode;
    // prepare selected ExecutableElement
    Element selectedElement = selectedIdentifier.getElement();
    if (!(selectedElement instanceof ExecutableElement)) {
      return RefactoringStatus.createFatalErrorStatus("Method declaration or reference must be selected to activate this refactoring.");
    }
    methodElement = (ExecutableElement) selectedElement;
    methodUnit = CorrectionUtils.getResolvedUnit(selectedElement);
    methodUtils = new CorrectionUtils(methodUnit);
    if (selectedElement instanceof MethodElement
        || selectedElement instanceof PropertyAccessorElement) {
      MethodDeclaration methodDeclaration = methodUtils.findNode(
          methodElement.getNameOffset(),
          MethodDeclaration.class);
      methodNode = methodDeclaration;
      methodParameters = methodDeclaration.getParameters();
      methodBody = methodDeclaration.getBody();
      // prepare mode
      boolean isDeclaration = methodDeclaration.getName() == selectedNode;
      initialMode = currentMode = isDeclaration ? Mode.INLINE_ALL : Mode.INLINE_SINGLE;
    }
    if (selectedElement instanceof FunctionElement) {
      FunctionDeclaration functionDeclaration = methodUtils.findNode(
          methodElement.getNameOffset(),
          FunctionDeclaration.class);
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
      Expression methodExpression = body.getExpression();
      SourceRange methodExpressionRange = rangeNode(methodExpression);
      methodExpressionPart = createSourcePart(methodExpressionRange);
    } else if (methodBody instanceof BlockFunctionBody) {
      Block body = ((BlockFunctionBody) methodBody).getBlock();
      List<Statement> statements = body.getStatements();
      if (statements.size() >= 1) {
        Statement lastStatement = statements.get(statements.size() - 1);
        // "return" statement requires special handling
        if (lastStatement instanceof ReturnStatement) {
          Expression methodExpression = ((ReturnStatement) lastStatement).getExpression();
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
      body.accept(new RecursiveASTVisitor<Void>() {
        private int numReturns = 0;

        @Override
        public Void visitReturnStatement(ReturnStatement node) {
          numReturns++;
          if (numReturns == 2) {
            result.addError("Ambiguous return value.", RefactoringStatusContext.create(node));
          }
          return super.visitReturnStatement(node);
        }
      });
    } else {
      return RefactoringStatus.createFatalErrorStatus("Cannot inline method without body.");
    }
    return result;
  }

}
