/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.TypeMember;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyCompilationUnit;

import junit.framework.TestCase;

public class DartTypeImplTest extends TestCase {
  public void test_DartTypeImpl_getExistingMembers_field() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    DartTypeImpl type = (DartTypeImpl) unit.getType("SimpleMoney");
    TypeMember[] members = type.getExistingMembers("currency");
    assertNotNull(members);
    assertEquals(1, members.length);
    assertTrue(members[0] instanceof Field);
  }

  public void test_DartTypeImpl_getExistingMembers_method() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    DartTypeImpl type = (DartTypeImpl) unit.getType("SimpleMoney");
    TypeMember[] members = type.getExistingMembers("addSimpleMoney");
    assertNotNull(members);
    assertEquals(1, members.length);
    assertTrue(members[0] instanceof Method);
  }

  public void test_DartTypeImpl_getSupertypeNames() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    DartTypeImpl type = (DartTypeImpl) unit.getType("SimpleMoney");
    String[] names = type.getSupertypeNames();
    assertNotNull(names);
    assertEquals(1, names.length);
    assertEquals("Money", names[0]);
  }
}
