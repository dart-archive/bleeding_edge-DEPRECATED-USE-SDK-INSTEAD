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
package com.google.dart.tools.ui.internal.cleanup.migration;

import com.google.dart.compiler.ast.ASTNodes;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNamedExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;

import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.List;

/**
 * If method is declared using old "named optional formals" <code>[a = 1]"</code>, and all call
 * sites use "named arguments" <code>f(a: 10)</code>, then convert method declaration to the new 1.0
 * M1 "named formal parameter" <code>{a: 1}</code>.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M1_optionalNamed_CleanUp extends AbstractMigrateCleanUp {
  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitMethodDefinition(final DartMethodDefinition node) {
        ExecutionUtils.runRethrow(new RunnableEx() {
          @Override
          public void run() throws Exception {
            processMethod(node);
          }
        });
        return super.visitMethodDefinition(node);
      }

      private boolean allInvocationsUseNamedArguments(DartMethodDefinition node) throws Exception {
        // find invocations
        List<SearchMatch> references;
        {
          DartFunction element = BindingUtils.getDartElement(unit.getLibrary(), node.getElement());
          references = RenameAnalyzeUtil.getReferences(element, new NullProgressMonitor());
        }
        // if no invocations, then we don't know
        if (references.isEmpty()) {
          return false;
        }
        // check each invocation
        for (SearchMatch reference : references) {
          CompilationUnit refUnit = reference.getElement().getAncestor(CompilationUnit.class);
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
              Object parameterId = argument.getInvocationParameterId();
              if (parameterId instanceof VariableElement) {
                VariableElement parameter = (VariableElement) parameterId;
                // each optional parameter should be used as named
                if (parameter.getModifiers().isOptional()
                    && !(argument instanceof DartNamedExpression)) {
                  return false;
                }
              }
            }
          }
        }
        // OK
        return true;
      }

      private void processMethod(DartMethodDefinition node) throws Exception {
        com.google.dart.compiler.ast.DartFunction function = node.getFunction();
        List<DartParameter> parameters = function.getParameters();
        // check if has optional parameters
        boolean hasOptional = false;
        for (DartParameter parameter : parameters) {
          Modifiers modifiers = parameter.getModifiers();
          hasOptional |= modifiers.isOptional();
        }
        if (!hasOptional) {
          return;
        }
        // check that all invocations use named arguments
        if (!allInvocationsUseNamedArguments(node)) {
          return;
        }
        // convert [] to {}
        int optionalOpen = function.getParametersOptionalOpen();
        int optionalClose = function.getParametersOptionalClose();
        addReplaceEdit(SourceRangeFactory.forStartLength(optionalOpen, 1), "{");
        addReplaceEdit(SourceRangeFactory.forStartLength(optionalClose, 1), "}");
        // convert "name = value" into "name: value"
        for (DartParameter parameter : parameters) {
          Modifiers modifiers = parameter.getModifiers();
          if (modifiers.isOptional() && parameter.getDefaultExpr() != null) {
            addReplaceEdit(
                SourceRangeFactory.forEndStart(parameter.getName(), parameter.getDefaultExpr()),
                ": ");
          }
        }
      }
    });
  }
}
