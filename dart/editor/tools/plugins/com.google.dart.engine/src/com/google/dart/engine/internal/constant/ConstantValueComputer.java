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
 * Instances of the class {@code ConstantValueComputer} compute the values of constant variables in
 * one or more compilation units. The expected usage pattern is for the compilation units to be
 * added to this computer using the method {@link #add(CompilationUnit)} and then for the method
 * {@link #computeValues()} to be invoked exactly once. Any use of an instance after invoking the
 * method {@link #computeValues()} will result in unpredictable behavior.
 */
public class ConstantValueComputer {
  /**
   * The type provider used to access the known types.
   */
  private TypeProvider typeProvider;

  /**
   * The object used to find constant variables in the compilation units that were added.
   */
  private ConstantFinder constantFinder = new ConstantFinder();

  /**
   * A graph in which the nodes are the constant variables and the edges are from each variable to
   * the other constant variables that are referenced in the head's initializer.
   */
  private DirectedGraph<VariableElement> referenceGraph = new DirectedGraph<VariableElement>();

  /**
   * A table mapping constant variables to the declarations of those variables.
   */
  private HashMap<VariableElement, VariableDeclaration> declarationMap;

  /**
   * Initialize a newly created constant value computer.
   * 
   * @param typeProvider the type provider used to access known types
   */
  public ConstantValueComputer(TypeProvider typeProvider) {
    this.typeProvider = typeProvider;
  }

  /**
   * Add the constant variables in the given compilation unit to the list of constant variables
   * whose value needs to be computed.
   * 
   * @param unit the compilation unit defining the constant variables to be added
   */
  public void add(CompilationUnit unit) {
    unit.accept(constantFinder);
  }

  /**
   * Compute values for all of the constant variables in the compilation units that were added.
   */
  public void computeValues() {
    declarationMap = constantFinder.getVariableMap();
    for (Map.Entry<VariableElement, VariableDeclaration> entry : declarationMap.entrySet()) {
      VariableElement element = entry.getKey();
      ReferenceFinder referenceFinder = new ReferenceFinder(element, referenceGraph);
      referenceGraph.addNode(element);
      entry.getValue().getInitializer().accept(referenceFinder);
    }
    while (!referenceGraph.isEmpty()) {
      VariableElement element = referenceGraph.removeSink();
      while (element != null) {
        computeValueFor(element);
        element = referenceGraph.removeSink();
      }
      if (!referenceGraph.isEmpty()) {
        List<VariableElement> variablesInCycle = referenceGraph.findCycle();
        if (variablesInCycle == null) {
          //
          // This should not happen. Either the graph should be empty, or there should be at least
          // one sink, or there should be a cycle. If this does happen we exit to prevent an
          // infinite loop.
          //
          AnalysisEngine.getInstance().getLogger().logError(
              "Exiting constant value computer with " + referenceGraph.getNodeCount()
                  + " variables that are neither sinks nor in a cycle");
          return;
        }
        for (VariableElement variable : variablesInCycle) {
          generateCycleError(variablesInCycle, variable);
        }
        referenceGraph.removeAllNodes(variablesInCycle);
      }
    }
  }

  /**
   * Compute a value for the given variable.
   * 
   * @param variable the variable for which a value is to be computed
   */
  private void computeValueFor(VariableElement variable) {
    VariableDeclaration declaration = declarationMap.get(variable);
    if (declaration == null) {
      //
      // The declaration will be null when the variable was added to the graph as a result of being
      // referenced by another variable but is not defined in the compilation units that were added
      // to this computer. In such cases, the variable should already have a value associated with
      // it, but we don't bother to check because there's nothing we can do about it at this point.
      //
      return;
    }
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
  }

  /**
   * Generate an error indicating that the given variable is not a valid compile-time constant
   * because it references at least one of the variables in the given cycle, each of which directly
   * or indirectly references the variable.
   * 
   * @param variablesInCycle the variables in the cycle that includes the given variable
   * @param variable the variable that is not a valid compile-time constant
   */
  private void generateCycleError(List<VariableElement> variablesInCycle, VariableElement variable) {
    // TODO(brianwilkerson) Implement this.
  }
}
