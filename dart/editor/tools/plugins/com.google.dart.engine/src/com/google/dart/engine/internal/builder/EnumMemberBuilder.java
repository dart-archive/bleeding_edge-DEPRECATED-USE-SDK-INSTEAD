/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.ast.EnumConstantDeclaration;
import com.google.dart.engine.ast.EnumDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.internal.constant.ValidResult;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstFieldElementImpl;
import com.google.dart.engine.internal.element.FieldElementImpl;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.object.GenericState;
import com.google.dart.engine.internal.object.IntState;
import com.google.dart.engine.internal.object.StringState;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Instances of the class {@code EnumMemberBuilder} build the members in enum declarations.
 */
public class EnumMemberBuilder extends RecursiveAstVisitor<Void> {
  /**
   * The type provider used to access the types needed to build an element model for enum
   * declarations.
   */
  private TypeProvider typeProvider;

  /**
   * Initialize a newly created enum member builder.
   * 
   * @param typeProvider the type provider used to access the types needed to build an element model
   *          for enum declarations
   */
  public EnumMemberBuilder(TypeProvider typeProvider) {
    this.typeProvider = typeProvider;
  }

  @Override
  public Void visitEnumDeclaration(EnumDeclaration node) {
    //
    // Finish building the enum.
    //
    ClassElementImpl enumElement = (ClassElementImpl) node.getName().getStaticElement();
    InterfaceType enumType = enumElement.getType();
    enumElement.setSupertype(typeProvider.getObjectType());
    //
    // Populate the fields.
    //
    ArrayList<FieldElement> fields = new ArrayList<FieldElement>();
    InterfaceType intType = typeProvider.getIntType();
    InterfaceType stringType = typeProvider.getStringType();

    String indexFieldName = "index";
    FieldElementImpl indexField = new FieldElementImpl(indexFieldName, -1);
    indexField.setFinal(true);
    indexField.setType(intType);
    fields.add(indexField);

    String nameFieldName = "_name";
    FieldElementImpl nameField = new FieldElementImpl(nameFieldName, -1);
    nameField.setFinal(true);
    nameField.setType(stringType);
    fields.add(nameField);

    FieldElementImpl valuesField = new FieldElementImpl("values", -1);
    valuesField.setStatic(true);
    valuesField.setConst(true);
    valuesField.setType(typeProvider.getListType().substitute(new Type[] {enumType}));
    fields.add(valuesField);
    //
    // Build the enum constants.
    //
    NodeList<EnumConstantDeclaration> constants = node.getConstants();
    int constantCount = constants.size();
    for (int i = 0; i < constantCount; i++) {
      SimpleIdentifier constantName = constants.get(i).getName();
      FieldElementImpl constantElement = new ConstFieldElementImpl(constantName);
      constantElement.setStatic(true);
      constantElement.setConst(true);
      constantElement.setType(enumType);
      HashMap<String, DartObjectImpl> fieldMap = new HashMap<String, DartObjectImpl>();
      fieldMap.put(indexFieldName, new DartObjectImpl(intType, new IntState(BigInteger.valueOf(i))));
      fieldMap.put(
          nameFieldName,
          new DartObjectImpl(stringType, new StringState(constantName.getName())));
      DartObjectImpl value = new DartObjectImpl(enumType, new GenericState(fieldMap));
      constantElement.setEvaluationResult(new ValidResult(value));
      fields.add(constantElement);
      constantName.setStaticElement(constantElement);
    }
    //
    // Finish building the enum.
    //
    enumElement.setFields(fields.toArray(new FieldElement[fields.size()]));
    // Client code isn't allowed to invoke the constructor, so we do not model it.
    return super.visitEnumDeclaration(node);
  }
}
