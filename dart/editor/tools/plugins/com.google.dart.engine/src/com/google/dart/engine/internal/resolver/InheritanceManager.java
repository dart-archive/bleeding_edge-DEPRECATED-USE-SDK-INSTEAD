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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.MultiplyInheritedExecutableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.ExecutableElementImpl;
import com.google.dart.engine.internal.element.MultiplyInheritedMethodElementImpl;
import com.google.dart.engine.internal.element.MultiplyInheritedPropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.member.MethodMember;
import com.google.dart.engine.internal.element.member.PropertyAccessorMember;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.verifier.ErrorVerifier;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Instances of the class {@code InheritanceManager} manage the knowledge of where class members
 * (methods, getters & setters) are inherited from.
 * 
 * @coverage dart.engine.resolver
 */
public class InheritanceManager {

  /**
   * Given some array of {@link ExecutableElement}s, this method creates a synthetic element as
   * described in 8.1.1:
   * <p>
   * Let <i>numberOfPositionals</i>(<i>f</i>) denote the number of positional parameters of a
   * function <i>f</i>, and let <i>numberOfRequiredParams</i>(<i>f</i>) denote the number of
   * required parameters of a function <i>f</i>. Furthermore, let <i>s</i> denote the set of all
   * named parameters of the <i>m<sub>1</sub>, &hellip;, m<sub>k</sub></i>. Then let
   * <ul>
   * <li><i>h = max(numberOfPositionals(m<sub>i</sub>)),</i></li>
   * <li><i>r = min(numberOfRequiredParams(m<sub>i</sub>)), for all <i>i</i>, 1 <= i <= k.</i></li>
   * </ul>
   * Then <i>I</i> has a method named <i>n</i>, with <i>r</i> required parameters of type
   * <b>dynamic</b>, <i>h</i> positional parameters of type <b>dynamic</b>, named parameters
   * <i>s</i> of type <b>dynamic</b> and return type <b>dynamic</b>.
   * <p>
   */
  // TODO (jwren) Associate a propagated type to the synthetic method element using least upper
  // bounds instead of dynamic
  //
  // TODO (collinsn) @jwren's above TODO could maybe be addressed using the union-type method merge
  // code I'm adding: [ElementResolver.computeMergedExecutableElement].
  // The difference is that I don't plan to handle unioning of methods with
  // different shapes very well: I'm just going to fall back to [dynamic] in that case.
  private static ExecutableElement computeMergedExecutableElement(
      ExecutableElement[] elementArrayToMerge) {
    int h = getNumOfPositionalParameters(elementArrayToMerge[0]);
    int r = getNumOfRequiredParameters(elementArrayToMerge[0]);
    Set<String> namedParametersList = new HashSet<String>();
    for (int i = 1; i < elementArrayToMerge.length; i++) {
      ExecutableElement element = elementArrayToMerge[i];
      int numOfPositionalParams = getNumOfPositionalParameters(element);
      if (h < numOfPositionalParams) {
        h = numOfPositionalParams;
      }
      int numOfRequiredParams = getNumOfRequiredParameters(element);
      if (r > numOfRequiredParams) {
        r = numOfRequiredParams;
      }
      namedParametersList.addAll(getNamedParameterNames(element));
    }
    return createSyntheticExecutableElement(
        elementArrayToMerge,
        elementArrayToMerge[0].getDisplayName(),
        r,
        h - r,
        namedParametersList.toArray(new String[namedParametersList.size()]));
  }

  /**
   * Used by {@link #computeMergedExecutableElement(ExecutableElement[])} to actually create the
   * synthetic element.
   * 
   * @param elementArrayToMerge the array used to create the synthetic element
   * @param name the name of the method, getter or setter
   * @param numOfRequiredParameters the number of required parameters
   * @param numOfPositionalParameters the number of positional parameters
   * @param namedParameters the list of {@link String}s that are the named parameters
   * @return the created synthetic element
   */
  private static ExecutableElement createSyntheticExecutableElement(
      ExecutableElement[] elementArrayToMerge, String name, int numOfRequiredParameters,
      int numOfPositionalParameters, String... namedParameters) {
    DynamicTypeImpl dynamicType = DynamicTypeImpl.getInstance();
    SimpleIdentifier nameIdentifier = new SimpleIdentifier(new StringToken(
        TokenType.IDENTIFIER,
        name,
        0));
    ExecutableElementImpl executable;
    if (elementArrayToMerge[0] instanceof MethodElement) {
      MultiplyInheritedMethodElementImpl unionedMethod = new MultiplyInheritedMethodElementImpl(
          nameIdentifier);
      unionedMethod.setInheritedElements(elementArrayToMerge);
      executable = unionedMethod;
    } else {
      MultiplyInheritedPropertyAccessorElementImpl unionedPropertyAccessor = new MultiplyInheritedPropertyAccessorElementImpl(
          nameIdentifier);
      unionedPropertyAccessor.setGetter(((PropertyAccessorElement) elementArrayToMerge[0]).isGetter());
      unionedPropertyAccessor.setSetter(((PropertyAccessorElement) elementArrayToMerge[0]).isSetter());
      unionedPropertyAccessor.setInheritedElements(elementArrayToMerge);
      executable = unionedPropertyAccessor;
    }

    int numOfParameters = numOfRequiredParameters + numOfPositionalParameters
        + namedParameters.length;
    ParameterElement[] parameters = new ParameterElement[numOfParameters];
    int i = 0;
    for (int j = 0; j < numOfRequiredParameters; j++, i++) {
      ParameterElementImpl parameter = new ParameterElementImpl("", 0);
      parameter.setType(dynamicType);
      parameter.setParameterKind(ParameterKind.REQUIRED);
      parameters[i] = parameter;
    }
    for (int k = 0; k < numOfPositionalParameters; k++, i++) {
      ParameterElementImpl parameter = new ParameterElementImpl("", 0);
      parameter.setType(dynamicType);
      parameter.setParameterKind(ParameterKind.POSITIONAL);
      parameters[i] = parameter;
    }
    for (int m = 0; m < namedParameters.length; m++, i++) {
      ParameterElementImpl parameter = new ParameterElementImpl(namedParameters[m], 0);
      parameter.setType(dynamicType);
      parameter.setParameterKind(ParameterKind.NAMED);
      parameters[i] = parameter;
    }
    executable.setReturnType(dynamicType);
    executable.setParameters(parameters);

    FunctionTypeImpl methodType = new FunctionTypeImpl(executable);
    executable.setType(methodType);

    return executable;
  }

  /**
   * Given some {@link ExecutableElement}, return the list of named parameters.
   */
  private static List<String> getNamedParameterNames(ExecutableElement executableElement) {
    ArrayList<String> namedParameterNames = new ArrayList<String>();
    ParameterElement[] parameters = executableElement.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      ParameterElement parameterElement = parameters[i];
      if (parameterElement.getParameterKind() == ParameterKind.NAMED) {
        namedParameterNames.add(parameterElement.getName());
      }
    }
    return namedParameterNames;
  }

  /**
   * Given some {@link ExecutableElement} return the number of parameters of the specified kind.
   */
  private static int getNumOfParameters(ExecutableElement executableElement,
      ParameterKind parameterKind) {
    int parameterCount = 0;
    ParameterElement[] parameters = executableElement.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      ParameterElement parameterElement = parameters[i];
      if (parameterElement.getParameterKind() == parameterKind) {
        parameterCount++;
      }
    }
    return parameterCount;
  }

  /**
   * Given some {@link ExecutableElement} return the number of positional parameters.
   * <p>
   * Note: by positional we mean {@link ParameterKind#REQUIRED} or {@link ParameterKind#POSITIONAL}.
   */
  private static int getNumOfPositionalParameters(ExecutableElement executableElement) {
    // For this portion of the spec "positional" references both the required and the positional
    // parameters.
    return getNumOfParameters(executableElement, ParameterKind.REQUIRED)
        + getNumOfParameters(executableElement, ParameterKind.POSITIONAL);
  }

  /**
   * Given some {@link ExecutableElement} return the number of required parameters.
   */
  private static int getNumOfRequiredParameters(ExecutableElement executableElement) {
    return getNumOfParameters(executableElement, ParameterKind.REQUIRED);
  }

  /**
   * Given some {@link ExecutableElement} returns {@code true} if it is an abstract member of a
   * class.
   * 
   * @param executableElement some {@link ExecutableElement} to evaluate
   * @return {@code true} if the given element is an abstract member of a class
   */
  private static boolean isAbstract(ExecutableElement executableElement) {
    if (executableElement instanceof MethodElement) {
      return ((MethodElement) executableElement).isAbstract();
    } else if (executableElement instanceof PropertyAccessorElement) {
      return ((PropertyAccessorElement) executableElement).isAbstract();
    }
    return false;
  }

  /**
   * The {@link LibraryElement} that is managed by this manager.
   */
  private LibraryElement library;

  /**
   * This is a mapping between each {@link ClassElement} and a map between the {@link String} member
   * names and the associated {@link ExecutableElement} in the mixin and superclass chain.
   */
  private HashMap<ClassElement, MemberMap> classLookup;

  /**
   * This is a mapping between each {@link ClassElement} and a map between the {@link String} member
   * names and the associated {@link ExecutableElement} in the interface set.
   */
  private HashMap<ClassElement, MemberMap> interfaceLookup;

  /**
   * A map between each visited {@link ClassElement} and the set of {@link AnalysisError}s found on
   * the class element.
   */
  private HashMap<ClassElement, HashSet<AnalysisError>> errorsInClassElement = new HashMap<ClassElement, HashSet<AnalysisError>>();

  /**
   * Initialize a newly created inheritance manager.
   * 
   * @param library the library element context that the inheritance mappings are being generated
   */
  public InheritanceManager(LibraryElement library) {
    this.library = library;
    classLookup = new HashMap<ClassElement, MemberMap>();
    interfaceLookup = new HashMap<ClassElement, MemberMap>();
  }

  /**
   * Return the set of {@link AnalysisError}s found on the passed {@link ClassElement}, or
   * {@code null} if there are none.
   * 
   * @param classElt the class element to query
   * @return the set of {@link AnalysisError}s found on the passed {@link ClassElement}, or
   *         {@code null} if there are none
   */
  public HashSet<AnalysisError> getErrors(ClassElement classElt) {
    return errorsInClassElement.get(classElt);
  }

  /**
   * Get and return a mapping between the set of all string names of the members inherited from the
   * passed {@link ClassElement} superclass hierarchy, and the associated {@link ExecutableElement}.
   * 
   * @param classElt the class element to query
   * @return a mapping between the set of all members inherited from the passed {@link ClassElement}
   *         superclass hierarchy, and the associated {@link ExecutableElement}
   */
  public MemberMap getMapOfMembersInheritedFromClasses(ClassElement classElt) {
    return computeClassChainLookupMap(classElt, new HashSet<ClassElement>());
  }

  /**
   * Get and return a mapping between the set of all string names of the members inherited from the
   * passed {@link ClassElement} interface hierarchy, and the associated {@link ExecutableElement}.
   * 
   * @param classElt the class element to query
   * @return a mapping between the set of all string names of the members inherited from the passed
   *         {@link ClassElement} interface hierarchy, and the associated {@link ExecutableElement}.
   */
  public MemberMap getMapOfMembersInheritedFromInterfaces(ClassElement classElt) {
    return computeInterfaceLookupMap(classElt, new HashSet<ClassElement>());
  }

  /**
   * Given some {@link ClassElement class element} and some member name, this returns the
   * {@link ExecutableElement executable element} that the class inherits from the mixins,
   * superclasses or interfaces, that has the member name, if no member is inherited {@code null} is
   * returned.
   * 
   * @param classElt the class element to query
   * @param memberName the name of the executable element to find and return
   * @return the inherited executable element with the member name, or {@code null} if no such
   *         member exists
   */
  public ExecutableElement lookupInheritance(ClassElement classElt, String memberName) {
    if (memberName == null || memberName.isEmpty()) {
      return null;
    }
    ExecutableElement executable = computeClassChainLookupMap(classElt, new HashSet<ClassElement>()).get(
        memberName);
    if (executable == null) {
      return computeInterfaceLookupMap(classElt, new HashSet<ClassElement>()).get(memberName);
    }
    return executable;
  }

  /**
   * Given some {@link ClassElement class element} and some member name, this returns the
   * {@link ExecutableElement executable element} that the class either declares itself, or
   * inherits, that has the member name, if no member is inherited {@code null} is returned.
   * 
   * @param classElt the class element to query
   * @param memberName the name of the executable element to find and return
   * @return the inherited executable element with the member name, or {@code null} if no such
   *         member exists
   */
  public ExecutableElement lookupMember(ClassElement classElt, String memberName) {
    ExecutableElement element = lookupMemberInClass(classElt, memberName);
    if (element != null) {
      return element;
    }
    return lookupInheritance(classElt, memberName);
  }

  /**
   * Given some {@link InterfaceType interface type} and some member name, this returns the
   * {@link FunctionType function type} of the {@link ExecutableElement executable element} that the
   * class either declares itself, or inherits, that has the member name, if no member is inherited
   * {@code null} is returned. The returned {@link FunctionType function type} has all type
   * parameters substituted with corresponding type arguments from the given {@link InterfaceType}.
   * 
   * @param interfaceType the interface type to query
   * @param memberName the name of the executable element to find and return
   * @return the member's function type, or {@code null} if no such member exists
   */
  public FunctionType lookupMemberType(InterfaceType interfaceType, String memberName) {
    ExecutableElement iteratorMember = lookupMember(interfaceType.getElement(), memberName);
    if (iteratorMember == null) {
      return null;
    }
    return substituteTypeArgumentsInMemberFromInheritance(
        iteratorMember.getType(),
        memberName,
        interfaceType);
  }

  /**
   * Determine the set of methods which is overridden by the given class member. If no member is
   * inherited, an empty list is returned. If one of the inherited members is a
   * {@link MultiplyInheritedExecutableElement}, then it is expanded into its constituent inherited
   * elements.
   * 
   * @param classElt the class to query
   * @param memberName the name of the class member to query
   * @return a list of overridden methods
   */
  public ArrayList<ExecutableElement> lookupOverrides(ClassElement classElt, String memberName) {
    ArrayList<ExecutableElement> result = new ArrayList<ExecutableElement>();
    if (memberName == null || memberName.isEmpty()) {
      return result;
    }
    ArrayList<MemberMap> interfaceMaps = gatherInterfaceLookupMaps(
        classElt,
        new HashSet<ClassElement>());
    if (interfaceMaps != null) {
      for (MemberMap interfaceMap : interfaceMaps) {
        ExecutableElement overriddenElement = interfaceMap.get(memberName);
        if (overriddenElement != null) {
          if (overriddenElement instanceof MultiplyInheritedExecutableElement) {
            MultiplyInheritedExecutableElement multiplyInheritedElement = (MultiplyInheritedExecutableElement) overriddenElement;
            for (ExecutableElement element : multiplyInheritedElement.getInheritedElements()) {
              result.add(element);
            }
          } else {
            result.add(overriddenElement);
          }
        }
      }
    }
    return result;
  }

  /**
   * Set the new library element context.
   * 
   * @param library the new library element
   */
  public void setLibraryElement(LibraryElement library) {
    this.library = library;
  }

  /**
   * This method takes some inherited {@link FunctionType}, and resolves all the parameterized types
   * in the function type, dependent on the class in which it is being overridden.
   * 
   * @param baseFunctionType the function type that is being overridden
   * @param memberName the name of the member, this is used to lookup the inheritance path of the
   *          override
   * @param definingType the type that is overriding the member
   * @return the passed function type with any parameterized types substituted
   */
  public FunctionType substituteTypeArgumentsInMemberFromInheritance(FunctionType baseFunctionType,
      String memberName, InterfaceType definingType) {
    // if the baseFunctionType is null, or does not have any parameters, return it.
    if (baseFunctionType == null || baseFunctionType.getTypeArguments().length == 0) {
      return baseFunctionType;
    }

    // First, generate the path from the defining type to the overridden member
    LinkedList<InterfaceType> inheritancePath = new LinkedList<InterfaceType>();
    computeInheritancePath(inheritancePath, definingType, memberName);

    if (inheritancePath == null || inheritancePath.isEmpty()) {
      // TODO(jwren) log analysis engine error
      return baseFunctionType;
    }
    FunctionType functionTypeToReturn = baseFunctionType;
    // loop backward through the list substituting as we go:
    while (!inheritancePath.isEmpty()) {
      InterfaceType lastType = inheritancePath.removeLast();
      Type[] parameterTypes = lastType.getElement().getType().getTypeArguments();
      Type[] argumentTypes = lastType.getTypeArguments();
      functionTypeToReturn = functionTypeToReturn.substitute(argumentTypes, parameterTypes);
    }
    return functionTypeToReturn;
  }

  /**
   * Compute and return a mapping between the set of all string names of the members inherited from
   * the passed {@link ClassElement} superclass hierarchy, and the associated
   * {@link ExecutableElement}.
   * 
   * @param classElt the class element to query
   * @param visitedClasses a set of visited classes passed back into this method when it calls
   *          itself recursively
   * @return a mapping between the set of all string names of the members inherited from the passed
   *         {@link ClassElement} superclass hierarchy, and the associated {@link ExecutableElement}
   */
  private MemberMap computeClassChainLookupMap(ClassElement classElt,
      HashSet<ClassElement> visitedClasses) {
    MemberMap resultMap = classLookup.get(classElt);
    if (resultMap != null) {
      return resultMap;
    } else {
      resultMap = new MemberMap();
    }
    ClassElement superclassElt = null;
    InterfaceType supertype = classElt.getSupertype();
    if (supertype != null) {
      superclassElt = supertype.getElement();
    } else {
      // classElt is Object
      classLookup.put(classElt, resultMap);
      return resultMap;
    }
    if (superclassElt != null) {
      if (!visitedClasses.contains(superclassElt)) {
        visitedClasses.add(superclassElt);
        try {
          resultMap = new MemberMap(computeClassChainLookupMap(superclassElt, visitedClasses));

          //
          // Substitute the super types down the hierarchy.
          //
          substituteTypeParametersDownHierarchy(supertype, resultMap);

          //
          // Include the members from the superclass in the resultMap.
          //
          recordMapWithClassMembers(resultMap, supertype, false);
        } finally {
          visitedClasses.remove(superclassElt);
        }
      } else {
        // This case happens only when the superclass was previously visited and not in the lookup,
        // meaning this is meant to shorten the compute for recursive cases.
        classLookup.put(superclassElt, resultMap);
        return resultMap;
      }
    }

    //
    // Include the members from the mixins in the resultMap
    //
    InterfaceType[] mixins = classElt.getMixins();
    for (int i = mixins.length - 1; i >= 0; i--) {
      ClassElement mixinElement = mixins[i].getElement();

      if (mixinElement != null) {
        if (!visitedClasses.contains(mixinElement)) {
          visitedClasses.add(mixinElement);
          try {
            MemberMap map = new MemberMap(computeClassChainLookupMap(mixinElement, visitedClasses));

            //
            // Substitute the super types down the hierarchy.
            //
            substituteTypeParametersDownHierarchy(mixins[i], map);

            //
            // Include the members from the superclass in the resultMap.
            //
            recordMapWithClassMembers(map, mixins[i], false);

            //
            // Add the members from map into result map.
            //
            for (int j = 0; j < map.getSize(); j++) {
              String key = map.getKey(j);
              ExecutableElement value = map.getValue(j);
              if (key != null) {
                if (resultMap.get(key) == null
                    || (resultMap.get(key) != null && !isAbstract(value))) {
                  resultMap.put(key, value);
                }
              }
            }
          } finally {
            visitedClasses.remove(mixinElement);
          }
        } else {
          // This case happens only when the superclass was previously visited and not in the lookup,
          // meaning this is meant to shorten the compute for recursive cases.
          classLookup.put(mixinElement, resultMap);
          return resultMap;
        }
      }
    }

    classLookup.put(classElt, resultMap);
    return resultMap;
  }

  /**
   * Compute and return the inheritance path given the context of a type and a member that is
   * overridden in the inheritance path (for which the type is in the path).
   * 
   * @param chain the inheritance path that is built up as this method calls itself recursively,
   *          when this method is called an empty {@link LinkedList} should be provided
   * @param currentType the current type in the inheritance path
   * @param memberName the name of the member that is being looked up the inheritance path
   */
  private void computeInheritancePath(LinkedList<InterfaceType> chain, InterfaceType currentType,
      String memberName) {
    // TODO (jwren) create a public version of this method which doesn't require the initial chain
    // to be provided, then provided tests for this functionality in InheritanceManagerTest
    chain.add(currentType);
    ClassElement classElt = currentType.getElement();
    InterfaceType supertype = classElt.getSupertype();
    // Base case- reached Object
    if (supertype == null) {
      // Looked up the chain all the way to Object, return null.
      // This should never happen.
      return;
    }

    // If we are done, return the chain
    // We are not done if this is the first recursive call on this method.
    if (chain.size() != 1) {
      // We are done however if the member is in this classElt
      if (lookupMemberInClass(classElt, memberName) != null) {
        return;
      }
    }

    // Otherwise, determine the next type (up the inheritance graph) to search for our member, start
    // with the mixins, followed by the superclass, and finally the interfaces:

    // Mixins- note that mixins call lookupMemberInClass, not lookupMember
    InterfaceType[] mixins = classElt.getMixins();
    for (int i = mixins.length - 1; i >= 0; i--) {
      ClassElement mixinElement = mixins[i].getElement();
      if (mixinElement != null) {
        ExecutableElement elt = lookupMemberInClass(mixinElement, memberName);
        if (elt != null) {
          // this is equivalent (but faster than) calling this method recursively
          // (return computeInheritancePath(chain, mixins[i], memberName);)
          chain.add(mixins[i]);
          return;
        }
      }
    }

    // Superclass
    ClassElement superclassElt = supertype.getElement();
    if (lookupMember(superclassElt, memberName) != null) {
      computeInheritancePath(chain, supertype, memberName);
      return;
    }

    // Interfaces
    InterfaceType[] interfaces = classElt.getInterfaces();
    for (InterfaceType interfaceType : interfaces) {
      ClassElement interfaceElement = interfaceType.getElement();
      if (interfaceElement != null && lookupMember(interfaceElement, memberName) != null) {
        computeInheritancePath(chain, interfaceType, memberName);
        return;
      }
    }
  }

  /**
   * Compute and return a mapping between the set of all string names of the members inherited from
   * the passed {@link ClassElement} interface hierarchy, and the associated
   * {@link ExecutableElement}.
   * 
   * @param classElt the class element to query
   * @param visitedInterfaces a set of visited classes passed back into this method when it calls
   *          itself recursively
   * @return a mapping between the set of all string names of the members inherited from the passed
   *         {@link ClassElement} interface hierarchy, and the associated {@link ExecutableElement}
   */
  private MemberMap computeInterfaceLookupMap(ClassElement classElt,
      HashSet<ClassElement> visitedInterfaces) {
    MemberMap resultMap = interfaceLookup.get(classElt);
    if (resultMap != null) {
      return resultMap;
    }
    ArrayList<MemberMap> lookupMaps = gatherInterfaceLookupMaps(classElt, visitedInterfaces);
    if (lookupMaps == null) {
      resultMap = new MemberMap();
    } else {
      HashMap<String, ArrayList<ExecutableElement>> unionMap = unionInterfaceLookupMaps(lookupMaps);
      resultMap = resolveInheritanceLookup(classElt, unionMap);
    }
    interfaceLookup.put(classElt, resultMap);
    return resultMap;
  }

  /**
   * Collect a list of interface lookup maps whose elements correspond to all of the classes
   * directly above {@link classElt} in the class hierarchy (the direct superclass if any, all
   * mixins, and all direct superinterfaces). Each item in the list is the interface lookup map
   * returned by {@link computeInterfaceLookupMap} for the corresponding super, except with type
   * parameters appropriately substituted.
   * 
   * @param classElt the class element to query
   * @param visitedInterfaces a set of visited classes passed back into this method when it calls
   *          itself recursively
   * @return {@code null} if there was a problem (such as a loop in the class hierarchy) or if there
   *         are no classes above this one in the class hierarchy. Otherwise, a list of interface
   *         lookup maps.
   */
  private ArrayList<MemberMap> gatherInterfaceLookupMaps(ClassElement classElt,
      HashSet<ClassElement> visitedInterfaces) {
    InterfaceType supertype = classElt.getSupertype();
    ClassElement superclassElement = supertype != null ? supertype.getElement() : null;
    InterfaceType[] mixins = classElt.getMixins();
    InterfaceType[] interfaces = classElt.getInterfaces();

    // Recursively collect the list of mappings from all of the interface types
    ArrayList<MemberMap> lookupMaps = new ArrayList<MemberMap>(interfaces.length + mixins.length
        + 1);

    //
    // Superclass element
    //
    if (superclassElement != null) {
      if (!visitedInterfaces.contains(superclassElement)) {
        try {
          visitedInterfaces.add(superclassElement);

          //
          // Recursively compute the map for the super type.
          //
          MemberMap map = computeInterfaceLookupMap(superclassElement, visitedInterfaces);
          map = new MemberMap(map);

          //
          // Substitute the super type down the hierarchy.
          //
          substituteTypeParametersDownHierarchy(supertype, map);

          //
          // Add any members from the super type into the map as well.
          //
          recordMapWithClassMembers(map, supertype, true);

          lookupMaps.add(map);
        } finally {
          visitedInterfaces.remove(superclassElement);
        }
      } else {
        return null;
      }
    }

    //
    // Mixin elements
    //
    for (int i = mixins.length - 1; i >= 0; i--) {
      InterfaceType mixinType = mixins[i];
      ClassElement mixinElement = mixinType.getElement();
      if (mixinElement != null) {
        if (!visitedInterfaces.contains(mixinElement)) {
          try {
            visitedInterfaces.add(mixinElement);

            //
            // Recursively compute the map for the mixin.
            //
            MemberMap map = computeInterfaceLookupMap(mixinElement, visitedInterfaces);
            map = new MemberMap(map);

            //
            // Substitute the mixin type down the hierarchy.
            //
            substituteTypeParametersDownHierarchy(mixinType, map);

            //
            // Add any members from the mixin type into the map as well.
            //
            recordMapWithClassMembers(map, mixinType, true);

            lookupMaps.add(map);
          } finally {
            visitedInterfaces.remove(mixinElement);
          }
        } else {
          return null;
        }
      }
    }

    //
    // Interface elements
    //
    for (InterfaceType interfaceType : interfaces) {
      ClassElement interfaceElement = interfaceType.getElement();
      if (interfaceElement != null) {
        if (!visitedInterfaces.contains(interfaceElement)) {
          try {
            visitedInterfaces.add(interfaceElement);

            //
            // Recursively compute the map for the interfaces.
            //
            MemberMap map = computeInterfaceLookupMap(interfaceElement, visitedInterfaces);
            map = new MemberMap(map);

            //
            // Substitute the supertypes down the hierarchy
            //
            substituteTypeParametersDownHierarchy(interfaceType, map);

            //
            // And add any members from the interface into the map as well.
            //
            recordMapWithClassMembers(map, interfaceType, true);

            lookupMaps.add(map);
          } finally {
            visitedInterfaces.remove(interfaceElement);
          }
        } else {
          return null;
        }
      }
    }
    if (lookupMaps.size() == 0) {
      return null;
    }
    return lookupMaps;
  }

  /**
   * Given some {@link ClassElement}, this method finds and returns the {@link ExecutableElement} of
   * the passed name in the class element. Static members, members in super types and members not
   * accessible from the current library are not considered.
   * 
   * @param classElt the class element to query
   * @param memberName the name of the member to lookup in the class
   * @return the found {@link ExecutableElement}, or {@code null} if no such member was found
   */
  private ExecutableElement lookupMemberInClass(ClassElement classElt, String memberName) {
    MethodElement[] methods = classElt.getMethods();
    for (MethodElement method : methods) {
      if (memberName.equals(method.getName()) && method.isAccessibleIn(library)
          && !method.isStatic()) {
        return method;
      }
    }
    PropertyAccessorElement[] accessors = classElt.getAccessors();
    for (PropertyAccessorElement accessor : accessors) {
      if (memberName.equals(accessor.getName()) && accessor.isAccessibleIn(library)
          && !accessor.isStatic()) {
        return accessor;
      }
    }
    return null;
  }

  /**
   * Record the passed map with the set of all members (methods, getters and setters) in the type
   * into the passed map.
   * 
   * @param map some non-{@code null} map to put the methods and accessors from the passed
   *          {@link ClassElement} into
   * @param type the type that will be recorded into the passed map
   * @param doIncludeAbstract {@code true} if abstract members will be put into the map
   */
  private void recordMapWithClassMembers(MemberMap map, InterfaceType type,
      boolean doIncludeAbstract) {
    MethodElement[] methods = type.getMethods();
    for (MethodElement method : methods) {
      if (method.isAccessibleIn(library) && !method.isStatic()
          && (doIncludeAbstract || !method.isAbstract())) {
        map.put(method.getName(), method);
      }
    }
    PropertyAccessorElement[] accessors = type.getAccessors();
    for (PropertyAccessorElement accessor : accessors) {
      if (accessor.isAccessibleIn(library) && !accessor.isStatic()
          && (doIncludeAbstract || !accessor.isAbstract())) {
        map.put(accessor.getName(), accessor);
      }
    }
  }

  /**
   * This method is used to report errors on when they are found computing inheritance information.
   * See {@link ErrorVerifier#checkForInconsistentMethodInheritance()} to see where these generated
   * error codes are reported back into the analysis engine.
   * 
   * @param classElt the location of the source for which the exception occurred
   * @param offset the offset of the location of the error
   * @param length the length of the location of the error
   * @param errorCode the error code to be associated with this error
   * @param arguments the arguments used to build the error message
   */
  private void reportError(ClassElement classElt, int offset, int length, ErrorCode errorCode,
      Object... arguments) {
    HashSet<AnalysisError> errorSet = errorsInClassElement.get(classElt);
    if (errorSet == null) {
      errorSet = new HashSet<AnalysisError>();
      errorsInClassElement.put(classElt, errorSet);
    }
    errorSet.add(new AnalysisError(classElt.getSource(), offset, length, errorCode, arguments));
  }

  /**
   * Given the set of methods defined by classes above {@link classElt} in the class hierarchy,
   * apply the appropriate inheritance rules to determine those methods inherited by or overridden
   * by {@link classElt}. Also report static warnings
   * {@link StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE} and
   * {@link StaticWarningCode.INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD} if appropriate.
   * 
   * @param classElt the class element to query.
   * @param unionMap a mapping from method name to the set of unique (in terms of signature) methods
   *          defined in superclasses of {@link classElt}.
   * @return the inheritance lookup map for {@link classElt}.
   */
  private MemberMap resolveInheritanceLookup(ClassElement classElt,
      HashMap<String, ArrayList<ExecutableElement>> unionMap) {
    MemberMap resultMap = new MemberMap();
    for (Entry<String, ArrayList<ExecutableElement>> entry : unionMap.entrySet()) {
      String key = entry.getKey();
      ArrayList<ExecutableElement> list = entry.getValue();
      int numOfEltsWithMatchingNames = list.size();
      if (numOfEltsWithMatchingNames == 1) {
        //
        // Example: class A inherits only 1 method named 'm'.  Since it is the only such method, it
        // is inherited.
        // Another example: class A inherits 2 methods named 'm' from 2 different interfaces, but
        // they both have the same signature, so it is the method inherited.
        //
        resultMap.put(key, list.get(0));
      } else {
        //
        // Then numOfEltsWithMatchingNames > 1, check for the warning cases.
        //
        boolean allMethods = true;
        boolean allSetters = true;
        boolean allGetters = true;
        for (ExecutableElement executableElement : list) {
          if (executableElement instanceof PropertyAccessorElement) {
            allMethods = false;
            if (((PropertyAccessorElement) executableElement).isSetter()) {
              allGetters = false;
            } else {
              allSetters = false;
            }
          } else {
            allGetters = false;
            allSetters = false;
          }
        }

        //
        // If there isn't a mixture of methods with getters, then continue, otherwise create a
        // warning.
        //
        if (allMethods || allGetters || allSetters) {
          //
          // Compute the element whose type is the subtype of all of the other types.
          //
          ExecutableElement[] elements = list.toArray(new ExecutableElement[numOfEltsWithMatchingNames]);
          FunctionType[] executableElementTypes = new FunctionType[numOfEltsWithMatchingNames];
          for (int i = 0; i < numOfEltsWithMatchingNames; i++) {
            executableElementTypes[i] = elements[i].getType();
          }
          ArrayList<Integer> subtypesOfAllOtherTypesIndexes = new ArrayList<Integer>(1);
          for (int i = 0; i < numOfEltsWithMatchingNames; i++) {
            FunctionType subtype = executableElementTypes[i];
            if (subtype == null) {
              continue;
            }
            boolean subtypeOfAllTypes = true;
            for (int j = 0; j < numOfEltsWithMatchingNames && subtypeOfAllTypes; j++) {
              if (i != j) {
                if (!subtype.isSubtypeOf(executableElementTypes[j])) {
                  subtypeOfAllTypes = false;
                  break;
                }
              }
            }
            if (subtypeOfAllTypes) {
              subtypesOfAllOtherTypesIndexes.add(i);
            }
          }

          //
          // The following is split into three cases determined by the number of elements in subtypesOfAllOtherTypes
          //
          if (subtypesOfAllOtherTypesIndexes.size() == 1) {
            //
            // Example: class A inherited only 2 method named 'm'. One has the function type
            // '() -> dynamic' and one has the function type '([int]) -> dynamic'. Since the second
            // method is a subtype of all the others, it is the inherited method.
            // Tests: InheritanceManagerTest.test_getMapOfMembersInheritedFromInterfaces_union_oneSubtype_*
            //
            resultMap.put(key, elements[subtypesOfAllOtherTypesIndexes.get(0)]);
          } else {
            if (subtypesOfAllOtherTypesIndexes.isEmpty()) {
              //
              // Determine if the current class has a method or accessor with the member name, if it
              // does then then this class does not "inherit" from any of the supertypes.
              // See issue 16134.
              //
              boolean classHasMember = false;
              if (allMethods) {
                classHasMember = classElt.getMethod(key) != null;
              } else {
                PropertyAccessorElement[] accessors = classElt.getAccessors();
                for (int i = 0; i < accessors.length; i++) {
                  if (accessors[i].getName().equals(key)) {
                    classHasMember = true;
                  }
                }
              }
              //
              // Example: class A inherited only 2 method named 'm'. One has the function type
              // '() -> int' and one has the function type '() -> String'. Since neither is a subtype
              // of the other, we create a warning, and have this class inherit nothing.
              //
              if (!classHasMember) {
                String firstTwoFuntionTypesStr = executableElementTypes[0].toString() + ", "
                    + executableElementTypes[1].toString();
                reportError(
                    classElt,
                    classElt.getNameOffset(),
                    classElt.getDisplayName().length(),
                    StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE,
                    key,
                    firstTwoFuntionTypesStr);
              }
            } else {
              //
              // Example: class A inherits 2 methods named 'm'. One has the function type
              // '(int) -> dynamic' and one has the function type '(num) -> dynamic'. Since they are
              // both a subtype of the other, a synthetic function '(dynamic) -> dynamic' is
              // inherited.
              // Tests: test_getMapOfMembersInheritedFromInterfaces_union_multipleSubtypes_*
              //
              ExecutableElement[] elementArrayToMerge = new ExecutableElement[subtypesOfAllOtherTypesIndexes.size()];
              for (int i = 0; i < elementArrayToMerge.length; i++) {
                elementArrayToMerge[i] = elements[subtypesOfAllOtherTypesIndexes.get(i)];
              }
              ExecutableElement mergedExecutableElement = computeMergedExecutableElement(elementArrayToMerge);
              resultMap.put(key, mergedExecutableElement);
            }
          }
        } else {
          reportError(
              classElt,
              classElt.getNameOffset(),
              classElt.getDisplayName().length(),
              StaticWarningCode.INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD,
              key);
        }
      }
    }
    return resultMap;
  }

  /**
   * Loop through all of the members in some {@link MemberMap}, performing type parameter
   * substitutions using a passed supertype.
   * 
   * @param superType the supertype to substitute into the members of the {@link MemberMap}
   * @param map the MemberMap to perform the substitutions on
   */
  private void substituteTypeParametersDownHierarchy(InterfaceType superType, MemberMap map) {
    for (int i = 0; i < map.getSize(); i++) {
      ExecutableElement executableElement = map.getValue(i);
      if (executableElement instanceof MethodMember) {
        executableElement = MethodMember.from((MethodMember) executableElement, superType);
        map.setValue(i, executableElement);
      } else if (executableElement instanceof PropertyAccessorMember) {
        executableElement = PropertyAccessorMember.from(
            (PropertyAccessorMember) executableElement,
            superType);
        map.setValue(i, executableElement);
      }
    }
  }

  /**
   * Union all of the {@link lookupMaps} together into a single map, grouping the ExecutableElements
   * into a list where none of the elements are equal where equality is determined by having equal
   * function types. (We also take note too of the kind of the element: ()->int and () -> int may
   * not be equal if one is a getter and the other is a method.)
   * 
   * @param lookupMaps the maps to be unioned together.
   * @return the resulting union map.
   */
  private HashMap<String, ArrayList<ExecutableElement>> unionInterfaceLookupMaps(
      ArrayList<MemberMap> lookupMaps) {
    HashMap<String, ArrayList<ExecutableElement>> unionMap = new HashMap<String, ArrayList<ExecutableElement>>();
    for (MemberMap lookupMap : lookupMaps) {
      int lookupMapSize = lookupMap.getSize();
      for (int i = 0; i < lookupMapSize; i++) {
        // Get the string key, if null, break.
        String key = lookupMap.getKey(i);
        if (key == null) {
          break;
        }

        // Get the list value out of the unionMap
        ArrayList<ExecutableElement> list = unionMap.get(key);

        // If we haven't created such a map for this key yet, do create it and put the list entry
        // into the unionMap.
        if (list == null) {
          list = new ArrayList<ExecutableElement>(4);
          unionMap.put(key, list);
        }

        // Fetch the entry out of this lookupMap
        ExecutableElement newExecutableElementEntry = lookupMap.getValue(i);

        if (list.isEmpty()) {
          // If the list is empty, just the new value
          list.add(newExecutableElementEntry);
        } else {
          // Otherwise, only add the newExecutableElementEntry if it isn't already in the list, this
          // covers situation where a class inherits two methods (or two getters) that are
          // identical.
          boolean alreadyInList = false;
          boolean isMethod1 = newExecutableElementEntry instanceof MethodElement;
          for (ExecutableElement executableElementInList : list) {
            boolean isMethod2 = executableElementInList instanceof MethodElement;
            if (isMethod1 == isMethod2
                && executableElementInList.getType().equals(newExecutableElementEntry.getType())) {
              alreadyInList = true;
              break;
            }
          }
          if (!alreadyInList) {
            list.add(newExecutableElementEntry);
          }
        }
      }
    }
    return unionMap;
  }
}
