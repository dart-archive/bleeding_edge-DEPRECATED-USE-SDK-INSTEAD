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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.internal.corext.dom.ASTNodes;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;

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

//  private static class AAA {
//    int zzz = 5;
//
//    void bar() {
//      AAA aaa = new AAA();
//      int c = foo(1, 2);
//    }
//
//    int foo(int a, int b) {
//      return zzz + a + b;
//    }
//  }

  private static class ParameterOccurrence {
    final int parentOperatorPrecedence;
    final SourceRange range;

    public ParameterOccurrence(int parentOperatorPrecedence, SourceRange range) {
      this.parentOperatorPrecedence = parentOperatorPrecedence;
      this.range = range;
    }
  }

  private static final String EMPTY = ""; //$NON-NLS-1$

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
  private Mode fInitialMode;
  private Mode fCurrentMode;
  private ExtractUtils methodUtils;
  private DartMethodDefinition methodNode;
  private Map<Integer, List<ParameterOccurrence>> methodParameterRanges = Maps.newHashMap();
  private DartExpression methodExpression;

  private String methodExpressionSource;

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
    pm.beginTask(RefactoringCoreMessages.InlineMethodRefactoring_processing, 3);
    pm.subTask(EMPTY);

    RefactoringStatus result = new RefactoringStatus();

    // replace all references
    List<SearchMatch> references = RenameAnalyzeUtil.getReferences(method, new SubProgressMonitor(
        pm,
        1));
    for (SearchMatch reference : references) {
      CompilationUnit refUnit = reference.getElement().getAncestor(CompilationUnit.class);
      ExtractUtils utils = new ExtractUtils(refUnit);
      // prepare invocation
      DartNode coveringNode = NodeFinder.find(
          utils.getUnitNode(),
          reference.getSourceRange().getOffset(),
          0).getCoveringNode();
      DartInvocation invocationNode = ASTNodes.getAncestor(coveringNode, DartInvocation.class);
      // we need invocation
      if (invocationNode != null) {
        // may be only single place should be inlined
        SourceRange invocationRange = SourceRangeFactory.create(invocationNode);
        if (fCurrentMode == Mode.INLINE_SINGLE) {
          if (!SourceRangeUtils.contains(invocationRange, selectionOffset)) {
            continue;
          }
        }
        // prepare arguments
        List<DartExpression> arguments = invocationNode.getArguments();
        // replace invocation with inlined expression XXX
        List<ReplaceEdit> edits = Lists.newArrayList();
        for (Entry<Integer, List<ParameterOccurrence>> entry : methodParameterRanges.entrySet()) {
          int parameterIndex = entry.getKey();
          // prepare argument
          DartExpression argument = arguments.get(parameterIndex);
          int argumentPrecedence = getExpressionPrecedence(argument);
          String argumentSource = utils.getText(argument);
          // replace all occurrences of this argument
          for (ParameterOccurrence occurrence : entry.getValue()) {
            SourceRange range = occurrence.range;
            // prepare argument source to apply at this occurrence
            String occurrenceArgumentSource;
            if (argumentPrecedence > occurrence.parentOperatorPrecedence) {
              occurrenceArgumentSource = argumentSource;
            } else {
              occurrenceArgumentSource = "(" + argumentSource + ")";
            }
            // do replace
            edits.add(new ReplaceEdit(
                range.getOffset(),
                range.getLength(),
                occurrenceArgumentSource));
          }
        }
        // prepare source with applied arguments
        String source = ExtractUtils.applyReplaceEdits(methodExpressionSource, edits);
        // replace this reference
        TextChange change = changeManager.get(refUnit);
        SourceRange sourceRange = invocationRange;
        TextChangeCompatibility.addTextEdit(
            change,
            RefactoringCoreMessages.InlineMethodRefactoring_replace_references,
            new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), source));
      }
    }
    // delete source
    if (deleteSource) {
      SourceInfo methodSI = methodNode.getSourceInfo();
      int lineThisIndex = methodUtils.getLineThisIndex(methodSI.getOffset());
      int lineNextIndex = methodUtils.getLineNextIndex(methodSI.getEnd());
      TextChange change = changeManager.get(methodUnit);
      TextChangeCompatibility.addTextEdit(
          change,
          RefactoringCoreMessages.InlineMethodRefactoring_remove_method,
          new ReplaceEdit(lineThisIndex, lineNextIndex - lineThisIndex, ""));
    }

    // XXX
//    RefactoringStatus result = checkMethodName();
//    pm.worked(1);
//
//    result.merge(checkParameterNames());
//    pm.worked(1);
//
//    result.merge(checkPossibleConflicts(new SubProgressMonitor(pm, 1)));

    pm.done();
    return result;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask("", 4); //$NON-NLS-1$
      RefactoringStatus result = new RefactoringStatus();
      // prepare mode
      {
        DartUnit selectionUnitNode = DartCompilerUtilities.resolveUnit(selectionUnit);
        DartNode node = NodeFinder.perform(selectionUnitNode, selectionOffset, 0);
        boolean methodNameSelected = getLocationInParent(node) == DART_CLASS_MEMBER_NAME;
        fInitialMode = fCurrentMode = methodNameSelected ? Mode.INLINE_ALL : Mode.INLINE_SINGLE;
      }
      // XXX
      {
        methodUtils = new ExtractUtils(methodUnit);
        DartNode methodNameNode = NodeFinder.find(
            methodUtils.getUnitNode(),
            method.getNameRange().getOffset(),
            0).getCoveringNode();
        methodNode = ASTNodes.getAncestor(methodNameNode, DartMethodDefinition.class);
        if (methodNode == null) {
          return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineMethodRefactoring_cannotFindMethodDeclaration);
        }
        // TODO(scheglov) only "return expression;" support right now
        {
          DartBlock body = methodNode.getFunction().getBody();
          List<DartStatement> statements = body.getStatements();
          if (statements.size() != 1 || !(statements.get(0) instanceof DartReturnStatement)) {
            return RefactoringStatus.createFatalErrorStatus("Only methods with single 'return' are supported right now.");
          }
          methodExpression = ((DartReturnStatement) statements.get(0)).getValue();
          methodExpressionSource = methodUnit.getSource().substring(
              methodExpression.getSourceInfo().getOffset(),
              methodExpression.getSourceInfo().getEnd());
          methodExpression.accept(new ASTVisitor<Void>() {
            @Override
            public Void visitIdentifier(DartIdentifier node) {
              VariableElement parameterElement = ASTNodes.getParameterElement(node);
              if (parameterElement != null) {
                int parameterIndex = ASTNodes.getParameterIndex(parameterElement);
                if (parameterIndex != -1) {
                  List<ParameterOccurrence> occurrences = methodParameterRanges.get(parameterIndex);
                  if (occurrences == null) {
                    occurrences = Lists.newArrayList();
                    methodParameterRanges.put(parameterIndex, occurrences);
                  }
                  int methodExpressionOffset = methodExpression.getSourceInfo().getOffset();
                  SourceRange nodeRange = SourceRangeFactory.fromBase(node, methodExpressionOffset);
                  occurrences.add(new ParameterOccurrence(
                      getExpressionParentPrecedence(node),
                      nodeRange));
                }
              }
              return null;
            }
          });
        }
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
    return fInitialMode;
  }

  public DartFunction getMethod() {
    return method;
  }

  @Override
  public String getName() {
    return RefactoringCoreMessages.InlineMethodRefactoring_name;
  }

  public RefactoringStatus setCurrentMode(Mode mode) throws DartModelException {
    if (fCurrentMode == mode) {
      return new RefactoringStatus();
    }
    // TODO(scheglov)
//    Assert.isTrue(getInitialMode() == Mode.INLINE_SINGLE);
    fCurrentMode = mode;
//    if (mode == Mode.INLINE_SINGLE) {
//      if (fInitialNode instanceof MethodInvocation)
//        fTargetProvider= TargetProvider.create((ICompilationUnit) fInitialTypeRoot, (MethodInvocation)fInitialNode);
//      else if (fInitialNode instanceof SuperMethodInvocation)
//        fTargetProvider= TargetProvider.create((ICompilationUnit) fInitialTypeRoot, (SuperMethodInvocation)fInitialNode);
//      else if (fInitialNode instanceof ConstructorInvocation)
//        fTargetProvider= TargetProvider.create((ICompilationUnit) fInitialTypeRoot, (ConstructorInvocation)fInitialNode);
//      else
//        throw new IllegalStateException(String.valueOf(fInitialNode));
//    } else {
//      fTargetProvider= TargetProvider.create(fSourceProvider.getDeclaration());
//    }
//    return fTargetProvider.checkActivation();
    return new RefactoringStatus();
  }

  public void setDeleteSource(boolean delete) {
    this.deleteSource = delete;
  }

}
