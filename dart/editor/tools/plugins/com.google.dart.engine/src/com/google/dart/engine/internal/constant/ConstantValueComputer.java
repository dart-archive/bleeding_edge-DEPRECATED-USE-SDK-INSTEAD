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
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.object.GenericState;
import com.google.dart.engine.internal.object.SymbolState;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.utilities.collection.DirectedGraph;
import com.google.dart.engine.utilities.dart.ParameterKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
  protected TypeProvider typeProvider;

  /**
   * The object used to find constant variables and constant constructor invocations in the
   * compilation units that were added.
   */
  private ConstantFinder constantFinder = new ConstantFinder();

  /**
   * A graph in which the nodes are the constants, and the edges are from each constant to the other
   * constants that are referenced by it.
   */
  protected DirectedGraph<AstNode> referenceGraph = new DirectedGraph<AstNode>();

  /**
   * A table mapping constant variables to the declarations of those variables.
   */
  private HashMap<VariableElement, VariableDeclaration> variableDeclarationMap;

  /**
   * A table mapping constant constructors to the declarations of those constructors.
   */
  private HashMap<ConstructorElement, ConstructorDeclaration> constructorDeclarationMap;

  /**
   * A collection of constant constructor invocations.
   */
  private ArrayList<InstanceCreationExpression> constructorInvocations;

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
    constructorDeclarationMap = constantFinder.getConstructorMap();
    constructorInvocations = constantFinder.getConstructorInvocations();
    for (Map.Entry<VariableElement, VariableDeclaration> entry : variableDeclarationMap.entrySet()) {
      VariableDeclaration declaration = entry.getValue();
      ReferenceFinder referenceFinder = new ReferenceFinder(
          declaration,
          referenceGraph,
          variableDeclarationMap,
          constructorDeclarationMap);
      referenceGraph.addNode(declaration);
      declaration.getInitializer().accept(referenceFinder);
    }
    for (InstanceCreationExpression expression : constructorInvocations) {
      ConstructorElement constructor = expression.getStaticElement();
      referenceGraph.addNode(expression);
      ConstructorDeclaration declaration = constructorDeclarationMap.get(constructor);
      // An instance creation expression depends both on the constructor and the arguments passed
      // to it.
      ReferenceFinder referenceFinder = new ReferenceFinder(
          expression,
          referenceGraph,
          variableDeclarationMap,
          constructorDeclarationMap);
      if (declaration != null) {
        declaration.accept(referenceFinder);
      }
      expression.getArgumentList().accept(referenceFinder);
    }
    beforeGraphWalk();
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
   * This method is called just before computing the constant value associated with an AST node.
   * Unit tests will override this method to introduce additional error checking.
   */
  protected void beforeComputeValue(AstNode constNode) {
  }

  /**
   * This method is called just before walking through [referenceGraph] to compute constant values.
   * Unit tests will override this method to introduce additional error checking.
   */
  protected void beforeGraphWalk() {
  }

  /**
   * Create the ConstantVisitor used to evaluate constants. Unit tests will override this method to
   * introduce additional error checking.
   */
  protected ConstantVisitor createConstantVisitor() {
    return new ConstantVisitor(typeProvider);
  }

  /**
   * Compute a value for the given constant.
   * 
   * @param constNode the constant for which a value is to be computed
   */
  private void computeValueFor(AstNode constNode) {
    beforeComputeValue(constNode);
    EvaluationResultImpl result;
    Element element;
    if (constNode instanceof VariableDeclaration) {
      VariableDeclaration declaration = (VariableDeclaration) constNode;
      element = declaration.getElement();
      result = declaration.getInitializer().accept(createConstantVisitor());
      ((VariableElementImpl) element).setEvaluationResult(result);
    } else if (constNode instanceof InstanceCreationExpression) {
      InstanceCreationExpression expression = (InstanceCreationExpression) constNode;
      ConstructorElement constructor = expression.getStaticElement();
      if (constructor == null) {
        // Couldn't resolve the constructor so we can't compute a value.  No problem--the error
        // has already been reported.
        return;
      }
      element = constructor;
      ConstantVisitor constantVisitor = createConstantVisitor();
      result = evaluateInstanceCreationExpression(expression, constructor, constantVisitor);
      expression.setEvaluationResult(result);
    } else {
      // Should not happen.
      AnalysisEngine.getInstance().getLogger().logError(
          "Constant value computer trying to compute the value of a node which is neither a "
              + "VariableDeclaration nor an InstanceCreationExpression");
      return;
    }
  }

  private EvaluationResultImpl evaluateInstanceCreationExpression(
      InstanceCreationExpression expression, ConstructorElement constructor,
      ConstantVisitor constantVisitor) {
    NodeList<Expression> arguments = expression.getArgumentList().getArguments();
    int argumentCount = arguments.size();
    DartObjectImpl[] argumentValues = new DartObjectImpl[argumentCount];
    HashMap<String, DartObjectImpl> namedArgumentValues = new HashMap<String, DartObjectImpl>();
    for (int i = 0; i < argumentCount; i++) {
      Expression argument = arguments.get(i);
      if (argument instanceof NamedExpression) {
        NamedExpression namedExpression = (NamedExpression) argument;
        String name = namedExpression.getName().getLabel().getName();
        namedArgumentValues.put(name, constantVisitor.valueOf(namedExpression.getExpression()));
        argumentValues[i] = constantVisitor.getNull();
      } else {
        argumentValues[i] = constantVisitor.valueOf(argument);
      }
    }
    HashSet<ConstructorElement> constructorsVisited = new HashSet<ConstructorElement>();
    InterfaceType definingClass = (InterfaceType) constructor.getReturnType();
    while (constructor.isFactory()) {
      if (definingClass.getElement().getLibrary().isDartCore()) {
        String className = definingClass.getName();
        if (className.equals("Symbol") && argumentCount == 1) {
          String argumentValue = argumentValues[0].getStringValue();
          if (argumentValue != null) {
            return constantVisitor.valid(definingClass, new SymbolState(argumentValue));
          }
        }
      }
      constructorsVisited.add(constructor);
      ConstructorElement redirectedConstructor = constructor.getRedirectedConstructor();
      if (redirectedConstructor == null) {
        // This can happen if constructor is an external factory constructor.  Since there is no
        // constructor to delegate to, we currently can't evaluate the constant.
        // TODO(paulberry): if the constructor is one of {bool,int,String}.fromEnvironment(),
        // we may be able to infer the value based on -D flags provided to the analyzer (see
        // dartbug.com/17234).
        return constantVisitor.validWithUnknownValue(definingClass);
      }
      if (!redirectedConstructor.isConst()) {
        // Delegating to a non-const constructor--this is not allowed (and
        // is checked elsewhere--see [ErrorVerifier.checkForRedirectToNonConstConstructor()]).
        // So if we encounter it just consider it an unknown value to suppress further errors.
        return constantVisitor.validWithUnknownValue(definingClass);
      }
      if (constructorsVisited.contains(redirectedConstructor)) {
        // Cycle in redirecting factory constructors--this is not allowed
        // and is checked elsewhere--see [ErrorVerifier.checkForRecursiveFactoryRedirect()]).
        // So if we encounter it just consider it an unknown value to suppress further errors.
        return constantVisitor.validWithUnknownValue(definingClass);
      }
      constructor = redirectedConstructor;
      definingClass = (InterfaceType) constructor.getReturnType();
    }
    HashMap<String, DartObjectImpl> fieldMap = new HashMap<String, DartObjectImpl>();
    ParameterElement[] parameters = constructor.getParameters();
    int parameterCount = parameters.length;
    for (int i = 0; i < parameterCount; i++) {
      ParameterElement parameter = parameters[i];
      if (parameter.isInitializingFormal()) {
        FieldElement field = ((FieldFormalParameterElement) parameter).getField();
        if (field != null) {
          String fieldName = field.getName();
          if (parameter.getParameterKind() == ParameterKind.NAMED) {
            DartObjectImpl argumentValue = namedArgumentValues.get(parameter.getName());
            if (argumentValue != null) {
              fieldMap.put(fieldName, argumentValue);
            }
          } else if (i < argumentCount) {
            fieldMap.put(fieldName, argumentValues[i]);
            // Otherwise, the parameter is assumed to be an optional positional parameter for which
            // no value was provided.
          }
        }
      }
    }
    // TODO(brianwilkerson) This doesn't handle fields initialized in an initializer. We should be
    // able to handle fields initialized by the superclass' constructor fairly easily, but other
    // initializers will be harder.
    return constantVisitor.valid(definingClass, new GenericState(fieldMap));
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
