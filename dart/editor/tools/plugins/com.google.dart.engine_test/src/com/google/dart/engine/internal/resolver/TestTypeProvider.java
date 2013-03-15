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
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.fieldElement;
import static com.google.dart.engine.element.ElementFactory.getObject;
import static com.google.dart.engine.element.ElementFactory.getterElement;
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
   * The type representing the built-in type 'Function'.
   */
  private InterfaceType functionType;

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

  @Override
  public InterfaceType getBoolType() {
    if (boolType == null) {
      boolType = classElement("bool").getType();
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

  @Override
  public InterfaceType getListType() {
    if (listType == null) {
      ClassElementImpl listElement = classElement("List", "E");
      listType = listElement.getType();
      Type eType = listElement.getTypeVariables()[0].getType();
      listElement.setAccessors(new PropertyAccessorElement[] {//
      getterElement("last", false, eType), // defined in Iterable
      });
      listElement.setMethods(new MethodElement[] {
          methodElement("[]", eType, intType),
          methodElement("[]=", VoidTypeImpl.getInstance(), intType, eType)});
    }
    return listType;
  }

  @Override
  public InterfaceType getMapType() {
    if (mapType == null) {
      mapType = classElement("Map", "K", "V").getType();
    }
    return mapType;
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
      if (objectElement.getMethods().length == 0) {
        objectElement.setMethods(new MethodElement[] {
            methodElement("toString", getStringType()), methodElement("==", boolType, objectType)});
        objectElement.setAccessors(new PropertyAccessorElement[] {getterElement(
            "hashCode",
            false,
            getIntType())});
      }
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
      stringElement.setAccessors(new PropertyAccessorElement[] {getterElement(
          "length",
          false,
          getIntType())});
    }
    return stringType;
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
}
