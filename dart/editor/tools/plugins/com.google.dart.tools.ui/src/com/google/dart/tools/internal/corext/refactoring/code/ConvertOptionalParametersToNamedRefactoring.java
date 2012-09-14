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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.ASTNodes;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartNamedExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.rename.MemberDeclarationsReferences;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
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
import org.eclipse.text.edits.TextEdit;

import java.util.List;

/**
 * Converts {@link DartFunction} with optional positional parameters style <code>[a = 10]</code> to
 * optional named style <code>{a: 10}</code>.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ConvertOptionalParametersToNamedRefactoring extends Refactoring {

  private static void addReplaceEdit(TextChange change, String name, SourceRange range, String text) {
    TextEdit edit = new ReplaceEdit(range.getOffset(), range.getLength(), text);
    TextChangeCompatibility.addTextEdit(change, name, edit);
  }

  private final DartFunction function;
  private final TextChangeManager changeManager = new TextChangeManager(true);

  public ConvertOptionalParametersToNamedRefactoring(DartFunction function) {
    this.function = function;
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    pm.beginTask(RefactoringCoreMessages.ConvertOptionalParametersToNamedRefactoring_processing, 3);
    pm.subTask(StringUtils.EMPTY);
    try {
      List<DartFunction> declarations;
      List<SearchMatch> references;
      // find references
      SubProgressMonitor pm2 = new SubProgressMonitor(pm, 1);
      if (function instanceof Method) {
        MemberDeclarationsReferences memberInfo = RenameAnalyzeUtil.findDeclarationsReferences(
            (Method) function,
            pm2);
        declarations = Lists.newArrayList();
        for (TypeMember member : memberInfo.declarations) {
          if (member instanceof Method) {
            declarations.add((Method) member);
          }
        }
        references = memberInfo.references;
      } else {
        declarations = ImmutableList.of(function);
        references = RenameAnalyzeUtil.getReferences(function, pm2);
      }
      // update declarations to use {}
      for (DartFunction function : declarations) {
        CompilationUnit unit = function.getCompilationUnit();
        TextChange change = changeManager.get(unit);
        String changeName = RefactoringCoreMessages.ConvertOptionalParametersToNamedRefactoring_update_declaration;
        addReplaceEdit(change, changeName, function.getOptionalParametersOpeningGroupChar(), "{");
        addReplaceEdit(change, changeName, function.getOptionalParametersClosingGroupChar(), "}");
        // replace "p = value" with "p: value"
        for (DartVariableDeclaration parameter : function.getLocalVariables()) {
          if (parameter.isParameter()) {
            SourceRange expressionRange = parameter.getDefaultExpressionRange();
            if (expressionRange != null) {
              addReplaceEdit(
                  change,
                  changeName,
                  SourceRangeFactory.forEndStart(parameter.getNameRange(), expressionRange),
                  ": ");
            }
          }
        }
      }
      pm.worked(1);
      // convert all references
      for (SearchMatch reference : references) {
        CompilationUnit refUnit = reference.getElement().getAncestor(CompilationUnit.class);
        if (!Checks.isAvailable(refUnit)) {
          result.addError(Messages.format(
              RefactoringCoreMessages.ConvertOptionalParametersToNamedRefactoring_externalUnit,
              refUnit));
          continue;
        }
        TextChange refChange = null;
        ExtractUtils utils = new ExtractUtils(refUnit);
        // prepare invocation
        DartNode coveringNode = NodeFinder.find(
            utils.getUnitNode(),
            reference.getSourceRange().getOffset(),
            0).getCoveringNode();
        DartInvocation invocation = ASTNodes.getAncestor(coveringNode, DartInvocation.class);
        // we need invocation
        if (invocation != null) {
          List<DartExpression> arguments = invocation.getArguments();
          for (DartExpression argument : arguments) {
            if (!(argument instanceof DartNamedExpression)) {
              Object parameterId = argument.getInvocationParameterId();
              if (parameterId instanceof VariableElement) {
                VariableElement parameterElement = (VariableElement) parameterId;
                if (parameterElement.getModifiers().isOptional()) {
                  if (refChange == null) {
                    refChange = changeManager.get(refUnit);
                  }
                  addReplaceEdit(
                      refChange,
                      RefactoringCoreMessages.ConvertOptionalParametersToNamedRefactoring_update_invocation,
                      SourceRangeFactory.forStartLength(argument, 0),
                      parameterElement.getName() + ": ");
                }
              }
            }
          }
        }
      }
      pm.worked(1);
    } finally {
      pm.done();
    }
    return result;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    pm.done();
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException {
    pm.done();
    return new CompositeChange(getName(), changeManager.getAllChanges());
  }

  @Override
  public String getName() {
    return RefactoringCoreMessages.ConvertOptionalParametersToNamedRefactoring_name;
  }

}
