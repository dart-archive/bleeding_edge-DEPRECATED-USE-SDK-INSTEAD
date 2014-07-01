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

import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.TypeParameterTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.fieldElement;
import static com.google.dart.engine.element.ElementFactory.getObject;
import static com.google.dart.engine.element.ElementFactory.getterElement;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.namedParameter;
import static com.google.dart.engine.element.ElementFactory.requiredParameter;

/**
 * Instances of the class {@code TestTypeProvider} implement a type provider that can be used by
 * tests without creating the element model for the core library.
 */
public class TestTypeProvider implements TypeProvider {
  /**
   * The type representing the built-in type 'bool'.
   */
  private InterfaceType boolType;

  /**
   * The type representing the type 'bottom'.
   */
  private Type bottomType;

  /**
   * The type representing the built-in type 'double'.
   */
  private InterfaceType doubleType;

  /**
   * The type representing the built-in type 'deprecated'.
   */
  private InterfaceType deprecatedType;

  /**
   * The type representing the built-in type 'dynamic'.
   */
  private Type dynamicType;

  /**
   * The type representing the built-in type 'Function'.
   */
  private InterfaceType functionType;

  /**
   * The type representing the built-in type 'int'.
   */
  private InterfaceType intType;

  /**
   * The type representing the built-in type 'Iterable'.
   */
  private InterfaceType iterableType;

  /**
   * The type representing the built-in type 'Iterator'.
   */
  private InterfaceType iteratorType;

  /**
   * The type representing the built-in type 'List'.
   */
  private InterfaceType listType;

  /**
   * The type representing the built-in type 'Map'.
   */
  private InterfaceType mapType;

  /**
   * The type representing the built-in type 'Null'.
   */
  private InterfaceType nullType;

  /**
   * The type representing the built-in type 'num'.
   */
  private InterfaceType numType;

  /**
   * The type representing the built-in type 'Object'.
   */
  private InterfaceType objectType;

  /**
   * The type representing the built-in type 'StackTrace'.
   */
  private InterfaceType stackTraceType;

  /**
   * The type representing the built-in type 'String'.
   */
  private InterfaceType stringType;

  /**
   * The type representing the built-in type 'Symbol'.
   */
  private InterfaceType symbolType;

  /**
   * The type representing the built-in type 'Type'.
   */
  private InterfaceType typeType;

  /**
   * Initialize a newly created type provider to provide stand-ins for the types defined in the core
   * library.
   */
  public TestTypeProvider() {
    super();
  }

  @Override
  public InterfaceType getBoolType() {
    if (boolType == null) {
      ClassElementImpl boolElement = classElement("bool");
      boolType = boolElement.getType();
      ConstructorElementImpl fromEnvironment = constructorElement(
          boolElement,
          "fromEnvironment",
          true);
      fromEnvironment.setParameters(new ParameterElement[] {
          requiredParameter("name", getStringType()), namedParameter("defaultValue", boolType)});
      fromEnvironment.setFactory(true);
      boolElement.setConstructors(new ConstructorElement[] {fromEnvironment});
    }
    return boolType;
  }

  @Override
  public Type getBottomType() {
    if (bottomType == null) {
      bottomType = BottomTypeImpl.getInstance();
    }
    return bottomType;
  }

  @Override
  public InterfaceType getDeprecatedType() {
    if (deprecatedType == null) {
      ClassElementImpl deprecatedElement = classElement("Deprecated");
      deprecatedElement.setConstructors(new ConstructorElement[] {constructorElement(
          deprecatedElement,
          null,
          true,
          getStringType())});
      deprecatedType = deprecatedElement.getType();
    }
    return deprecatedType;
  }

  @Override
  public InterfaceType getDoubleType() {
    if (doubleType == null) {
      initializeNumericTypes();
    }
    return doubleType;
  }

  @Override
  public Type getDynamicType() {
    if (dynamicType == null) {
      dynamicType = DynamicTypeImpl.getInstance();
    }
    return dynamicType;
  }

  @Override
  public InterfaceType getFunctionType() {
    if (functionType == null) {
      functionType = classElement("Function").getType();
    }
    return functionType;
  }

  @Override
  public InterfaceType getIntType() {
    if (intType == null) {
      initializeNumericTypes();
    }
    return intType;
  }

  public InterfaceType getIterableType() {
    if (iterableType == null) {
      ClassElementImpl iterableElement = classElement("Iterable", "E");
      iterableType = iterableElement.getType();
      Type eType = iterableElement.getTypeParameters()[0].getType();
      iterableElement.setAccessors(new PropertyAccessorElement[] {//
          getterElement("iterator", false, getIteratorType().substitute(new Type[] {eType})),
          getterElement("last", false, eType),});
      propagateTypeArguments(iterableElement);
    }
    return iterableType;
  }

  public InterfaceType getIteratorType() {
    if (iteratorType == null) {
      ClassElementImpl iteratorElement = classElement("Iterator", "E");
      iteratorType = iteratorElement.getType();
      Type eType = iteratorElement.getTypeParameters()[0].getType();
      iteratorElement.setAccessors(new PropertyAccessorElement[] {//
      getterElement("current", false, eType),});
      propagateTypeArguments(iteratorElement);
    }
    return iteratorType;
  }

  @Override
  public InterfaceType getListType() {
    if (listType == null) {
      ClassElementImpl listElement = classElement("List", "E");
      listElement.setConstructors(new ConstructorElement[] {constructorElement(listElement, null)});
      listType = listElement.getType();
      Type eType = listElement.getTypeParameters()[0].getType();
      InterfaceType iterableType = getIterableType().substitute(new Type[] {eType});
      listElement.setInterfaces(new InterfaceType[] {iterableType});
      listElement.setAccessors(new PropertyAccessorElement[] {getterElement(
          "length",
          false,
          getIntType())});
      listElement.setMethods(new MethodElement[] {
          methodElement("[]", eType, getIntType()),
          methodElement("[]=", VoidTypeImpl.getInstance(), getIntType(), eType),
          methodElement("add", VoidTypeImpl.getInstance(), eType)});
      propagateTypeArguments(listElement);
    }
    return listType;
  }

  @Override
  public InterfaceType getMapType() {
    if (mapType == null) {
      ClassElementImpl mapElement = classElement("Map", "K", "V");
      mapType = mapElement.getType();
      Type kType = mapElement.getTypeParameters()[0].getType();
      Type vType = mapElement.getTypeParameters()[1].getType();
      mapElement.setAccessors(new PropertyAccessorElement[] {getterElement(
          "length",
          false,
          getIntType())});
      mapElement.setMethods(new MethodElement[] {
          methodElement("[]", vType, getObjectType()),
          methodElement("[]=", VoidTypeImpl.getInstance(), kType, vType)});
      propagateTypeArguments(mapElement);
    }
    return mapType;
  }

  @Override
  public InterfaceType getNullType() {
    if (nullType == null) {
      nullType = classElement("Null").getType();
    }
    return nullType;
  }

  @Override
  public InterfaceType getNumType() {
    if (numType == null) {
      initializeNumericTypes();
    }
    return numType;
  }

  @Override
  public InterfaceType getObjectType() {
    if (objectType == null) {
      ClassElementImpl objectElement = getObject();
      objectType = objectElement.getType();
      objectElement.setConstructors(new ConstructorElement[] {constructorElement(
          objectElement,
          null)});
      objectElement.setMethods(new MethodElement[] {
          methodElement("toString", getStringType()),
          methodElement("==", getBoolType(), objectType),
          methodElement("noSuchMethod", getDynamicType(), getDynamicType())});
      objectElement.setAccessors(new PropertyAccessorElement[] {
          getterElement("hashCode", false, getIntType()),
          getterElement("runtimeType", false, getTypeType())});
    }
    return objectType;
  }

  @Override
  public InterfaceType getStackTraceType() {
    if (stackTraceType == null) {
      stackTraceType = classElement("StackTrace").getType();
    }
    return stackTraceType;
  }

  @Override
  public InterfaceType getStringType() {
    if (stringType == null) {
      stringType = classElement("String").getType();
      ClassElementImpl stringElement = (ClassElementImpl) stringType.getElement();
      stringElement.setAccessors(new PropertyAccessorElement[] {//
          getterElement("isEmpty", false, getBoolType()),
          getterElement("length", false, getIntType()),
          getterElement("codeUnits", false, getListType().substitute(new Type[] {getIntType()}))});
      stringElement.setMethods(new MethodElement[] {
          methodElement("+", stringType, stringType), methodElement("toLowerCase", stringType),
          methodElement("toUpperCase", stringType)});
      ConstructorElementImpl fromEnvironment = constructorElement(
          stringElement,
          "fromEnvironment",
          true);
      fromEnvironment.setParameters(new ParameterElement[] {
          requiredParameter("name", getStringType()), namedParameter("defaultValue", stringType)});
      fromEnvironment.setFactory(true);
      stringElement.setConstructors(new ConstructorElement[] {fromEnvironment});
    }
    return stringType;
  }

  @Override
  public InterfaceType getSymbolType() {
    if (symbolType == null) {
      ClassElementImpl symbolClass = classElement("Symbol");
      ConstructorElementImpl constructor = constructorElement(
          symbolClass,
          null,
          true,
          getStringType());
      constructor.setFactory(true);
      symbolClass.setConstructors(new ConstructorElement[] {constructor});
      symbolType = symbolClass.getType();
    }
    return symbolType;
  }

  @Override
  public InterfaceType getTypeType() {
    if (typeType == null) {
      typeType = classElement("Type").getType();
    }
    return typeType;
  }

  /**
   * Initialize the numeric types. They are created as a group so that we can (a) create the right
   * hierarchy and (b) add members to them.
   */
  private void initializeNumericTypes() {
    //
    // Create the type hierarchy.
    //
    ClassElementImpl numElement = classElement("num");
    numType = numElement.getType();

    ClassElementImpl intElement = classElement("int", numType);
    intType = intElement.getType();

    ClassElementImpl doubleElement = classElement("double", numType);
    doubleType = doubleElement.getType();
    //
    // Force the referenced types to be cached.
    //
    getObjectType();
    getBoolType();
    getStringType();
    //
    // Add the methods.
    //
    numElement.setMethods(new MethodElement[] {
        methodElement("+", numType, numType), methodElement("-", numType, numType),
        methodElement("*", numType, numType), methodElement("%", numType, numType),
        methodElement("/", doubleType, numType), methodElement("~/", numType, numType),
        methodElement("-", numType), methodElement("remainder", numType, numType),
        methodElement("<", boolType, numType), methodElement("<=", boolType, numType),
        methodElement(">", boolType, numType), methodElement(">=", boolType, numType),
        methodElement("==", boolType, objectType), methodElement("isNaN", boolType),
        methodElement("isNegative", boolType), methodElement("isInfinite", boolType),
        methodElement("abs", numType), methodElement("floor", numType),
        methodElement("ceil", numType), methodElement("round", numType),
        methodElement("truncate", numType), methodElement("toInt", intType),
        methodElement("toDouble", doubleType),
        methodElement("toStringAsFixed", stringType, intType),
        methodElement("toStringAsExponential", stringType, intType),
        methodElement("toStringAsPrecision", stringType, intType),
        methodElement("toRadixString", stringType, intType),});
    intElement.setMethods(new MethodElement[] {
        methodElement("&", intType, intType),
        methodElement("|", intType, intType),
        methodElement("^", intType, intType),
        methodElement("~", intType),
        methodElement("<<", intType, intType),
        methodElement(">>", intType, intType),
//      getterElement("isEven", boolType),
//      getterElement("isOdd", boolType),
        methodElement("-", intType), methodElement("abs", intType),
        methodElement("round", intType), methodElement("floor", intType),
        methodElement("ceil", intType), methodElement("truncate", intType),
        methodElement("toString", stringType),
//      methodElement(/*external static*/ "parse", intType, stringType),
    });
    ConstructorElementImpl fromEnvironment = constructorElement(intElement, "fromEnvironment", true);
    fromEnvironment.setParameters(new ParameterElement[] {
        requiredParameter("name", getStringType()), namedParameter("defaultValue", intType)});
    fromEnvironment.setFactory(true);
    intElement.setConstructors(new ConstructorElement[] {fromEnvironment});
    FieldElement[] fields = new FieldElement[] {fieldElement("NAN", true, false, true, doubleType), // 0.0 / 0.0
        fieldElement("INFINITY", true, false, true, doubleType), // 1.0 / 0.0
        fieldElement("NEGATIVE_INFINITY", true, false, true, doubleType), // -INFINITY
        fieldElement("MIN_POSITIVE", true, false, true, doubleType), // 5e-324
        fieldElement("MAX_FINITE", true, false, true, doubleType), // 1.7976931348623157e+308;
    };
    doubleElement.setFields(fields);
    int fieldCount = fields.length;
    PropertyAccessorElement[] accessors = new PropertyAccessorElement[fieldCount];
    for (int i = 0; i < fieldCount; i++) {
      accessors[i] = fields[i].getGetter();
    }
    doubleElement.setAccessors(accessors);
    doubleElement.setMethods(new MethodElement[] {
        methodElement("remainder", doubleType, numType), methodElement("+", doubleType, numType),
        methodElement("-", doubleType, numType), methodElement("*", doubleType, numType),
        methodElement("%", doubleType, numType), methodElement("/", doubleType, numType),
        methodElement("~/", doubleType, numType), methodElement("-", doubleType),
        methodElement("abs", doubleType), methodElement("round", doubleType),
        methodElement("floor", doubleType), methodElement("ceil", doubleType),
        methodElement("truncate", doubleType), methodElement("toString", stringType),
//      methodElement(/*external static*/ "parse", doubleType, stringType),
    });
  }

  /**
   * Given a class element representing a class with type parameters, propagate those type
   * parameters to all of the accessors, methods and constructors defined for the class.
   * 
   * @param classElement the element representing the class with type parameters
   */
  private void propagateTypeArguments(ClassElementImpl classElement) {
    Type[] typeArguments = TypeParameterTypeImpl.getTypes(classElement.getTypeParameters());
    for (PropertyAccessorElement accessor : classElement.getAccessors()) {
      FunctionTypeImpl functionType = (FunctionTypeImpl) accessor.getType();
      functionType.setTypeArguments(typeArguments);
    }
    for (MethodElement method : classElement.getMethods()) {
      FunctionTypeImpl functionType = (FunctionTypeImpl) method.getType();
      functionType.setTypeArguments(typeArguments);
    }
    for (ConstructorElement constructor : classElement.getConstructors()) {
      FunctionTypeImpl functionType = (FunctionTypeImpl) constructor.getType();
      functionType.setTypeArguments(typeArguments);
    }
  }
}
