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
package com.google.dart.engine.internal.constant;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.collection.DirectedGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class {@code ConstantValueComputer} compute the values of constant variables and
 * constant constructor invocations in one or more compilation units. The expected usage pattern is
 * for the compilation units to be added to this computer using the method
 * {@link #add(CompilationUnit)} and then for the method {@link #computeValues()} to be invoked
 * exactly once. Any use of an instance after invoking the method {@link #computeValues()} will
 * result in unpredictable behavior.
 */
public class ConstantValueComputer {
  /**
   * The type provider used to access the known types.
   */
  private TypeProvider typeProvider;

  /**
   * The object used to find constant variables and constant constructor invocations in the
   * compilation units that were added.
   */
  private ConstantFinder constantFinder = new ConstantFinder();

  /**
   * A graph in which the nodes are the constants, and the edges are from each constant to the other
   * constants that are referenced by it.
   */
  private DirectedGraph<AstNode> referenceGraph = new DirectedGraph<AstNode>();

  /**
   * A table mapping constant variables to the declarations of those variables.
   */
  private HashMap<VariableElement, VariableDeclaration> variableDeclarationMap;

  /**
   * Initialize a newly created constant value computer.
   * 
   * @param typeProvider the type provider used to access known types
   */
  public ConstantValueComputer(TypeProvider typeProvider) {
    this.typeProvider = typeProvider;
  }

  /**
   * Add the constants in the given compilation unit to the list of constants whose value needs to
   * be computed.
   * 
   * @param unit the compilation unit defining the constants to be added
   */
  public void add(CompilationUnit unit) {
    unit.accept(constantFinder);
  }

  /**
   * Compute values for all of the constants in the compilation units that were added.
   */
  public void computeValues() {
    variableDeclarationMap = constantFinder.getVariableMap();
    for (Map.Entry<VariableElement, VariableDeclaration> entry : variableDeclarationMap.entrySet()) {
      VariableDeclaration declaration = entry.getValue();
      ReferenceFinder referenceFinder = new ReferenceFinder(
          declaration,
          referenceGraph,
          variableDeclarationMap);
      referenceGraph.addNode(declaration);
      declaration.getInitializer().accept(referenceFinder);
    }
    // TODO(paulberry): Do the same for constant constructor invocations.
    while (!referenceGraph.isEmpty()) {
      AstNode node = referenceGraph.removeSink();
      while (node != null) {
        computeValueFor(node);
        node = referenceGraph.removeSink();
      }
      if (!referenceGraph.isEmpty()) {
        List<AstNode> constantsInCycle = referenceGraph.findCycle();
        if (constantsInCycle == null) {
          //
          // This should not happen. Either the graph should be empty, or there should be at least
          // one sink, or there should be a cycle. If this does happen we exit to prevent an
          // infinite loop.
          //
          AnalysisEngine.getInstance().getLogger().logError(
              "Exiting constant value computer with " + referenceGraph.getNodeCount()
                  + " constants that are neither sinks nor in a cycle");
          return;
        }
        for (AstNode constant : constantsInCycle) {
          generateCycleError(constantsInCycle, constant);
        }
        referenceGraph.removeAllNodes(constantsInCycle);
      }
    }
  }

  /**
   * Compute a value for the given constant.
   * 
   * @param constNode the constant for which a value is to be computed
   */
  private void computeValueFor(AstNode constNode) {
    if (constNode instanceof VariableDeclaration) {
      VariableDeclaration declaration = (VariableDeclaration) constNode;
      VariableElement variable = declaration.getElement();
      EvaluationResultImpl result = declaration.getInitializer().accept(
          new ConstantVisitor(typeProvider));
      ((VariableElementImpl) variable).setEvaluationResult(result);
      if (result instanceof ErrorResult) {
        ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
        for (ErrorResult.ErrorData data : ((ErrorResult) result).getErrorData()) {
          AstNode node = data.getNode();
          Source source = variable.getAncestor(CompilationUnitElement.class).getSource();
          errors.add(new AnalysisError(
              source,
              node.getOffset(),
              node.getLength(),
              data.getErrorCode()));
        }
      }
    } else if (constNode instanceof InstanceCreationExpression) {
      // TODO(paulberry): compute constant value for constant constructor invocations.
    } else {
      // Should not happen.
      AnalysisEngine.getInstance().getLogger().logError(
          "Constant value computer trying to compute the value of a node which is neither a "
              + "VariableDeclaration nor an InstanceCreationExpression");
    }
  }

  /**
   * Generate an error indicating that the given constant is not a valid compile-time constant
   * because it references at least one of the constants in the given cycle, each of which directly
   * or indirectly references the constant.
   * 
   * @param constantsInCycle the constants in the cycle that includes the given constant
   * @param constant the constant that is not a valid compile-time constant
   */
  private void generateCycleError(List<AstNode> constantsInCycle, AstNode constant) {
    // TODO(brianwilkerson) Implement this.
  }
}
