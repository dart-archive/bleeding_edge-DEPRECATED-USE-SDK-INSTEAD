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

import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.getObject;
import static com.google.dart.engine.element.ElementFactory.methodElement;

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
   * The type representing the built-in type 'dynamic'.
   */
  private Type dynamicType;

  /**
   * The type representing the built-in type 'int'.
   */
  private InterfaceType intType;

  /**
   * The type representing the built-in type 'List'.
   */
  private InterfaceType listType;

  /**
   * The type representing the built-in type 'Map'.
   */
  private InterfaceType mapType;

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

  /**
   * Return the type representing the built-in type 'bool'.
   * 
   * @return the type representing the built-in type 'bool'
   */
  @Override
  public InterfaceType getBoolType() {
    if (boolType == null) {
      boolType = classElement("bool").getType();
    }
    return boolType;
  }

  /**
   * Return the type representing the type 'bottom'.
   * 
   * @return the type representing the type 'bottom'
   */
  @Override
  public Type getBottomType() {
    if (bottomType == null) {
      bottomType = BottomTypeImpl.getInstance();
    }
    return bottomType;
  }

  /**
   * Return the type representing the built-in type 'double'.
   * 
   * @return the type representing the built-in type 'double'
   */
  @Override
  public InterfaceType getDoubleType() {
    if (doubleType == null) {
      initializeNumericTypes();
    }
    return doubleType;
  }

  /**
   * Return the type representing the built-in type 'dynamic'.
   * 
   * @return the type representing the built-in type 'dynamic'
   */
  @Override
  public Type getDynamicType() {
    if (dynamicType == null) {
      dynamicType = DynamicTypeImpl.getInstance();
    }
    return dynamicType;
  }

  /**
   * Return the type representing the built-in type 'int'.
   * 
   * @return the type representing the built-in type 'int'
   */
  @Override
  public InterfaceType getIntType() {
    if (intType == null) {
      initializeNumericTypes();
    }
    return intType;
  }

  /**
   * Return the type representing the built-in type 'List'.
   * 
   * @return the type representing the built-in type 'List'
   */
  @Override
  public InterfaceType getListType() {
    if (listType == null) {
      listType = classElement("List", "E").getType();
    }
    return listType;
  }

  /**
   * Return the type representing the built-in type 'Map'.
   * 
   * @return the type representing the built-in type 'Map'
   */
  @Override
  public InterfaceType getMapType() {
    if (mapType == null) {
      mapType = classElement("Map", "K", "V").getType();
    }
    return mapType;
  }

  /**
   * Return the type representing the built-in type 'double'.
   * 
   * @return the type representing the built-in type 'double'
   */
  public InterfaceType getNumType() {
    if (numType == null) {
      initializeNumericTypes();
    }
    return numType;
  }

  /**
   * Return the type representing the built-in type 'Object'.
   * 
   * @return the type representing the built-in type 'Object'
   */
  @Override
  public InterfaceType getObjectType() {
    if (objectType == null) {
      objectType = getObject().getType();
    }
    return objectType;
  }

  /**
   * Return the type representing the built-in type 'StackTrace'.
   * 
   * @return the type representing the built-in type 'StackTrace'
   */
  @Override
  public InterfaceType getStackTraceType() {
    if (stackTraceType == null) {
      stackTraceType = classElement("StackTrace").getType();
    }
    return stackTraceType;
  }

  /**
   * Return the type representing the built-in type 'String'.
   * 
   * @return the type representing the built-in type 'String'
   */
  @Override
  public InterfaceType getStringType() {
    if (stringType == null) {
      stringType = classElement("String").getType();
    }
    return stringType;
  }

  /**
   * Return the type representing the built-in type 'Type'.
   * 
   * @return the type representing the built-in type 'Type'
   */
  @Override
  public InterfaceType getTypeType() {
    if (typeType == null) {
      typeType = classElement("Type").getType();
    }
    return typeType;
  }

  private void initializeNumericTypes() {
    //
    // Create the type hierarchy.
    //
    ClassElementImpl numElement = (ClassElementImpl) classElement("num");
    numType = numElement.getType();

    ClassElementImpl intElement = (ClassElementImpl) classElement("int", numType);
    intType = intElement.getType();

    ClassElementImpl doubleElement = (ClassElementImpl) classElement("double", numType);
    doubleType = doubleElement.getType();
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
        methodElement("isNaN", boolType), methodElement("isNegative", boolType),
        methodElement("isInfinite", boolType), methodElement("abs", numType),
        methodElement("floor", numType), methodElement("ceil", numType),
        methodElement("round", numType), methodElement("truncate", numType),
        methodElement("toInt", intType), methodElement("toDouble", doubleType),
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
    doubleElement.setFields(new FieldElement[] {
//      static const double NAN = 0.0 / 0.0;
//      static const double INFINITY = 1.0 / 0.0;
//      static const double NEGATIVE_INFINITY = -INFINITY;
//      static const double MIN_POSITIVE = 5e-324;
//      static const double MAX_FINITE = 1.7976931348623157e+308;
    });
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
}
