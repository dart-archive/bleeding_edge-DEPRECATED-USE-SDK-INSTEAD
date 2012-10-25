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

import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_CLASS_MEMBER_NAME;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.getLocationInParent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.compiler.ast.ASTNodes;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartThisExpression;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.Elements;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.rename.FunctionLocalElement;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.util.DartModelUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.ReplaceEdit;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Inlines a method in a compilation unit based on a text selection range.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class InlineMethodRefactoring extends Refactoring {
  public enum Mode {
    INLINE_ALL,
    INLINE_SINGLE;
  }

  private static class ParameterOccurrence {
    final int parentPrecedence;
    final SourceRange range;

    public ParameterOccurrence(int parentPrecedence, SourceRange range) {
      this.parentPrecedence = parentPrecedence;
      this.range = range;
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
      range = SourceRangeFactory.fromBase(range, baseRange);
      instanceFieldQualifiers.add(range);
    }

    public void addParameterOccurrence(int index, SourceRange range, int precedence) {
      if (index != -1) {
        List<ParameterOccurrence> occurrences = parameters.get(index);
        if (occurrences == null) {
          occurrences = Lists.newArrayList();
          parameters.put(index, occurrences);
        }
        range = SourceRangeFactory.fromBase(range, baseRange);
        occurrences.add(new ParameterOccurrence(precedence, range));
      }
    }

    public void addStaticFieldQualifier(String className, SourceRange range) {
      List<SourceRange> ranges = staticFieldQualifiers.get(className);
      if (ranges == null) {
        ranges = Lists.newArrayList();
        staticFieldQualifiers.put(className, ranges);
      }
      range = SourceRangeFactory.fromBase(range, baseRange);
      ranges.add(range);
    }

    public void addVariable(VariableElement element, SourceRange range) {
      List<SourceRange> ranges = variables.get(element);
      if (ranges == null) {
        ranges = Lists.newArrayList();
        variables.put(element, ranges);
      }
      range = SourceRangeFactory.fromBase(range, baseRange);
      ranges.add(range);
    }
  }

  private static ReplaceEdit createReplaceEdit(SourceRange range, String text) {
    return new ReplaceEdit(range.getOffset(), range.getLength(), text);
  }

  /**
   * @return {@link #getExpressionPrecedence(DartNode)} for parent node.
   */
  private static int getExpressionParentPrecedence(DartNode node) {
    DartNode parent = node.getParent();
    return getExpressionPrecedence(parent);
  }

  /**
   * @return the precedence of the operator, may be <code>1000</code> if not binary expression.
   */
  private static int getExpressionPrecedence(DartNode node) {
    if (node instanceof DartBinaryExpression) {
      DartBinaryExpression binary = (DartBinaryExpression) node;
      return binary.getOperator().getPrecedence();
    }
    return 1000;
  }

  private final DartFunction method;
  private final CompilationUnit methodUnit;
  private final CompilationUnit selectionUnit;
  private final int selectionOffset;

  private final TextChangeManager changeManager = new TextChangeManager(true);
  private boolean deleteSource;
  private Mode initialMode;
  private Mode currentMode;
  private ExtractUtils methodUtils;
  private DartMethodDefinition methodNode;
  private ClassElement methodClassElement;
  private SourcePart methodExpressionPart;
  private SourcePart methodStatementsPart;

  public InlineMethodRefactoring(DartFunction method, CompilationUnit selectionUnit,
      int selectionOffset) {
    this.method = method;
    this.methodUnit = method.getAncestor(CompilationUnit.class);
    this.selectionUnit = selectionUnit;
    this.selectionOffset = selectionOffset;
  }

  public boolean canEnableDeleteSource() {
    return !methodUnit.isReadOnly();
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    // begin task
    pm.beginTask(RefactoringCoreMessages.InlineMethodRefactoring_processing, 3);
    pm.subTask(StringUtils.EMPTY);
    // find references
    List<SearchMatch> references;
    {
      SubProgressMonitor pm2 = new SubProgressMonitor(pm, 1);
      references = RenameAnalyzeUtil.getReferences(method, pm2);
    }
    // replace all references
    for (SearchMatch reference : references) {
      CompilationUnit refUnit = reference.getElement().getAncestor(CompilationUnit.class);
      TextChange refChange = changeManager.get(refUnit);
      ExtractUtils utils = new ExtractUtils(refUnit);
      // prepare invocation
      DartNode coveringNode = NodeFinder.find(
          utils.getUnitNode(),
          reference.getSourceRange().getOffset(),
          0).getCoveringNode();
      DartInvocation invocation = ASTNodes.getAncestor(coveringNode, DartInvocation.class);
      // we need invocation
      if (invocation != null) {
        // we don't support cascade
        if (invocation instanceof DartMethodInvocation
            && ((DartMethodInvocation) invocation).isCascade()) {
          result.addFatalError(
              RefactoringCoreMessages.InlineMethodRefactoring_cascadeInvocation,
              DartStatusContext.create(refUnit, invocation));
        }
        // prepare environment
        DartStatement invocationStatement = ASTNodes.getAncestor(invocation, DartStatement.class);
        SourceRange invocationLineRange = utils.getLinesRange(ImmutableList.of(invocationStatement));
        String refPrefix = utils.getNodePrefix(invocationStatement);
        // may be only single place should be inlined
        SourceRange invocationRange = SourceRangeFactory.create(invocation);
        if (currentMode == Mode.INLINE_SINGLE) {
          if (!SourceRangeUtils.contains(invocationRange, selectionOffset)) {
            continue;
          }
        }
        // insert non-return statements
        if (methodStatementsPart != null) {
          // prepare statements source for invocation
          String source = getMethodSourceForInvocation(methodStatementsPart, utils, invocation);
          source = utils.getIndentSource(source, methodStatementsPart.prefix, refPrefix);
          // do insert
          SourceRange range = SourceRangeFactory.forStartLength(invocationLineRange, 0);
          TextChangeCompatibility.addTextEdit(
              refChange,
              RefactoringCoreMessages.InlineMethodRefactoring_replace_references,
              new ReplaceEdit(range.getOffset(), range.getLength(), source));
        }
        // replace invocation with return expression
        if (methodExpressionPart != null) {
          // prepare expression source for invocation
          String source = getMethodSourceForInvocation(methodExpressionPart, utils, invocation);
          // do replace
          SourceRange sourceRange = invocationRange;
          TextChangeCompatibility.addTextEdit(
              refChange,
              RefactoringCoreMessages.InlineMethodRefactoring_replace_references,
              new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), source));
        } else {
          TextChangeCompatibility.addTextEdit(
              refChange,
              RefactoringCoreMessages.InlineMethodRefactoring_replace_references,
              new ReplaceEdit(invocationLineRange.getOffset(), invocationLineRange.getLength(), ""));
        }
      }
    }
    // delete method
    if (deleteSource && currentMode == Mode.INLINE_ALL) {
      SourceInfo methodSI = methodNode.getSourceInfo();
      int lineThisIndex = methodUtils.getLineThisIndex(methodSI.getOffset());
      int lineNextIndex = methodUtils.getLineNextIndex(methodSI.getEnd());
      TextChange change = changeManager.get(methodUnit);
      TextChangeCompatibility.addTextEdit(
          change,
          RefactoringCoreMessages.InlineMethodRefactoring_remove_method,
          new ReplaceEdit(lineThisIndex, lineNextIndex - lineThisIndex, ""));
    }

    pm.done();
    return result;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask("", 4); //$NON-NLS-1$
      final RefactoringStatus result = new RefactoringStatus();
      // prepare mode
      {
        DartUnit selectionUnitNode = DartCompilerUtilities.resolveUnit(selectionUnit);
        DartNode node = NodeFinder.perform(selectionUnitNode, selectionOffset, 0);
        boolean methodNameSelected = getLocationInParent(node) == DART_CLASS_MEMBER_NAME;
        initialMode = currentMode = methodNameSelected ? Mode.INLINE_ALL : Mode.INLINE_SINGLE;
      }
      // prepare "methodX" information
      {
        methodUtils = new ExtractUtils(methodUnit);
        DartNode methodNameNode = NodeFinder.find(
            methodUtils.getUnitNode(),
            method.getNameRange().getOffset(),
            0).getCoveringNode();
        methodNode = ASTNodes.getAncestor(methodNameNode, DartMethodDefinition.class);
        if (methodNode.getElement().getEnclosingElement() instanceof ClassElement) {
          methodClassElement = (ClassElement) methodNode.getElement().getEnclosingElement();
        }
        // analyze method body
        DartBlock body = methodNode.getFunction().getBody();
        List<DartStatement> statements = body.getStatements();
        if (statements.size() >= 1) {
          DartStatement lastStatement = statements.get(statements.size() - 1);
          // "return" statement requires special handling
          if (lastStatement instanceof DartReturnStatement) {
            DartExpression methodExpression = ((DartReturnStatement) lastStatement).getValue();
            SourceRange methodExpressionRange = SourceRangeFactory.create(methodExpression);
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
        // check if we support such body
        body.accept(new ASTVisitor<Void>() {
          private int numReturns = 0;

          @Override
          public Void visitReturnStatement(DartReturnStatement node) {
            numReturns++;
            if (numReturns == 2) {
              result.addError(
                  RefactoringCoreMessages.InlineMethodRefactoring_multipleReturns,
                  DartStatusContext.create(methodUnit, node));
            }
            return super.visitReturnStatement(node);
          }
        });
      }
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException {
    pm.beginTask("", 1);
    try {
      return new CompositeChange(getName(), changeManager.getAllChanges());
    } finally {
      pm.done();
    }
  }

  public Mode getInitialMode() {
    return initialMode;
  }

  public DartFunction getMethod() {
    return method;
  }

  @Override
  public String getName() {
    return RefactoringCoreMessages.InlineMethodRefactoring_name;
  }

  public RefactoringStatus setCurrentMode(Mode mode) throws DartModelException {
    currentMode = mode;
    return new RefactoringStatus();
  }

  public void setDeleteSource(boolean delete) {
    this.deleteSource = delete;
  }

  private SourcePart createSourcePart(final SourceRange sourceRange) throws DartModelException {
    final SourcePart result;
    {
      String source = getMethodUnitSource(sourceRange);
      String prefix = ExtractUtils.getLinesPrefix(source);
      result = new SourcePart(sourceRange, source, prefix);
    }
    // remember parameters and variables occurrences
    methodUtils.getUnitNode().accept(new ASTVisitor<Void>() {
      @Override
      public Void visitIdentifier(DartIdentifier node) {
        addInstanceFieldQualifier(node);
        addParameter(node);
        addVariable(node);
        return null;
      }

      @Override
      public Void visitNode(DartNode node) {
        SourceRange nodeRange = SourceRangeFactory.create(node);
        if (!SourceRangeUtils.intersects(sourceRange, nodeRange)) {
          return null;
        }
        return super.visitNode(node);
      }

      private void addInstanceFieldQualifier(DartIdentifier node) {
        FieldElement fieldElement = ASTNodes.getFieldElement(node);
        if (isMethodClassField(fieldElement)) {
          if (Elements.isStaticField(fieldElement)) {
            String className = fieldElement.getEnclosingElement().getName();
            if (ASTNodes.getNodeQualifier(node) == null) {
              SourceRange qualifierRange = SourceRangeFactory.forStartLength(node, 0);
              result.addStaticFieldQualifier(className, qualifierRange);
            }
          } else {
            SourceRange qualifierRange;
            DartThisExpression qualifier = ASTNodes.getThisQualifier(node);
            if (qualifier != null) {
              qualifierRange = SourceRangeFactory.forStartStart(qualifier, node);
            } else {
              qualifierRange = SourceRangeFactory.forStartLength(node, 0);
            }
            result.addInstanceFieldQualifier(qualifierRange);
          }
        }
      }

      private void addParameter(DartIdentifier node) {
        VariableElement parameterElement = ASTNodes.getParameterElement(node);
        if (parameterElement != null) {
          int parameterIndex = ASTNodes.getParameterIndex(parameterElement);
          if (parameterIndex != -1) {
            SourceRange nodeRange = SourceRangeFactory.create(node);
            int parentPrecedence = getExpressionParentPrecedence(node);
            result.addParameterOccurrence(parameterIndex, nodeRange, parentPrecedence);
          }
        }
      }

      private void addVariable(DartIdentifier node) {
        VariableElement variableElement = ASTNodes.getVariableElement(node);
        if (variableElement != null) {
          SourceRange nodeRange = SourceRangeFactory.create(node);
          result.addVariable(variableElement, nodeRange);
        }
      }
    });
    // done
    return result;
  }

  /**
   * @return the source which should replace given {@link DartInvocation}.
   */
  private String getMethodSourceForInvocation(SourcePart part, ExtractUtils utils,
      DartInvocation invocation) throws DartModelException {
    // prepare arguments
    List<DartExpression> arguments = invocation.getArguments();
    // prepare edits to replace parameters with arguments
    List<ReplaceEdit> edits = Lists.newArrayList();
    for (Entry<Integer, List<ParameterOccurrence>> entry : part.parameters.entrySet()) {
      int parameterIndex = entry.getKey();
      // prepare argument
      DartExpression argument = arguments.get(parameterIndex);
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
        edits.add(createReplaceEdit(range, occurrenceArgumentSource));
      }
    }
    // replace static field "qualifier" with invocation target
    for (Entry<String, List<SourceRange>> entry : part.staticFieldQualifiers.entrySet()) {
      String className = entry.getKey();
      for (SourceRange range : entry.getValue()) {
        edits.add(createReplaceEdit(range, className + "."));
      }
    }
    // replace instance field "qualifier" with invocation target
    {
      DartExpression targetExpression = invocation.getTarget();
      String targetSource = utils.getText(targetExpression) + ".";
      for (SourceRange qualifierRange : part.instanceFieldQualifiers) {
        edits.add(createReplaceEdit(qualifierRange, targetSource));
      }
    }
    // prepare edits to replace conflicting variables
    Set<String> conflictingNames = getNamesConflictingWithLocal(utils.getUnit(), invocation);
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
          edits.add(createReplaceEdit(range, uniqueName));
        }
      }
    }
    // prepare source with applied arguments
    return ExtractUtils.applyReplaceEdits(part.source, edits);
  }

  /**
   * @return the part of source from {@link #methodUnit}.
   */
  private String getMethodUnitSource(SourceRange range) throws DartModelException {
    int start = range.getOffset();
    int end = start + range.getLength();
    return methodUnit.getSource().substring(start, end);
  }

  /**
   * @return the names which will shadow or will be shadowed by any declaration at "node".
   */
  private Set<String> getNamesConflictingWithLocal(CompilationUnit unit, DartNode node)
      throws DartModelException {
    Set<String> result = Sets.newHashSet();
    // prepare offsets
    int offset = node.getSourceInfo().getOffset();
    SourceRange offsetRange;
    {
      DartBlock block = ASTNodes.getAncestor(node, DartBlock.class);
      int endOffset = block.getSourceInfo().getEnd();
      offsetRange = SourceRangeFactory.forStartEnd(offset, endOffset);
    }
    // local variables and functions
    {
      DartFunction function = DartModelUtil.getElementContaining(unit, DartFunction.class, offset);
      if (function != null) {
        for (FunctionLocalElement element : RenameAnalyzeUtil.getFunctionLocalElements(function)) {
          SourceRange variableRange = element.getVisibleRange();
          if (SourceRangeUtils.intersects(variableRange, offsetRange)) {
            result.add(element.getElementName());
          }
        }
      }
    }
    // fields
    {
      Type enclosingType = DartModelUtil.getElementContaining(unit, Type.class, offset);
      if (enclosingType != null) {
        Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(enclosingType);
        Set<Type> types = Sets.newHashSet(superTypes);
        types.add(enclosingType);
        for (Type type : types) {
          for (DartElement typeChild : type.getChildren()) {
            result.add(typeChild.getElementName());
          }
        }
      }
    }
    // done
    return result;
  }

  /**
   * @return <code>true</code> if given {@link FieldElement} is a field of
   *         {@link #methodClassElement} or one of its super-classes.
   */
  private boolean isMethodClassField(FieldElement field) {
    return methodClassElement != null && field != null
        && Elements.hasClassMember(methodClassElement, field.getName());
  }

}
