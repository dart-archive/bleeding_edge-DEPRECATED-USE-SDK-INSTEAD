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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.EnumDeclaration;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.internal.resolver.TestTypeProvider;

import static com.google.dart.engine.ast.AstFactory.enumDeclaration;

public class EnumMemberBuilderTest extends EngineTestCase {
  public void test_visitEnumDeclaration_multiple() {
    String firstName = "ONE";
    String secondName = "TWO";
    String thirdName = "THREE";
    EnumDeclaration enumDeclaration = enumDeclaration("E", firstName, secondName, thirdName);

    ClassElement enumElement = buildElement(enumDeclaration);
    FieldElement[] fields = enumElement.getFields();
    assertLength(6, fields);

    FieldElement constant = fields[3];
    assertNotNull(constant);
    assertEquals(firstName, constant.getName());
    assertTrue(constant.isStatic());

    constant = fields[4];
    assertNotNull(constant);
    assertEquals(secondName, constant.getName());
    assertTrue(constant.isStatic());

    constant = fields[5];
    assertNotNull(constant);
    assertEquals(thirdName, constant.getName());
    assertTrue(constant.isStatic());
  }

  public void test_visitEnumDeclaration_single() {
    String firstName = "ONE";
    EnumDeclaration enumDeclaration = enumDeclaration("E", firstName);

    ClassElement enumElement = buildElement(enumDeclaration);
    FieldElement[] fields = enumElement.getFields();
    assertLength(4, fields);

    FieldElement field = fields[0];
    assertNotNull(field);
    assertEquals("index", field.getName());
    assertFalse(field.isStatic());
    assertTrue(field.isSynthetic());

    field = fields[1];
    assertNotNull(field);
    assertEquals("_name", field.getName());
    assertFalse(field.isStatic());
    assertTrue(field.isSynthetic());

    field = fields[2];
    assertNotNull(field);
    assertEquals("values", field.getName());
    assertTrue(field.isStatic());
    assertTrue(field.isSynthetic());

    FieldElement constant = fields[3];
    assertNotNull(constant);
    assertEquals(firstName, constant.getName());
    assertTrue(constant.isStatic());
  }

  private ClassElement buildElement(EnumDeclaration enumDeclaration) {
    ElementHolder holder = new ElementHolder();
    ElementBuilder elementBuilder = new ElementBuilder(holder);
    enumDeclaration.accept(elementBuilder);

    EnumMemberBuilder memberBuilder = new EnumMemberBuilder(new TestTypeProvider());
    enumDeclaration.accept(memberBuilder);

    ClassElement[] enums = holder.getEnums();
    assertLength(1, enums);
    return enums[0];
  }
}
