/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.indexer;

import com.google.dart.indexer.exceptions.IndexTemporarilyNonOperational;
import com.google.dart.indexer.locations.Location;
import com.google.dart.tools.core.internal.indexer.location.TypeLocation;
import com.google.dart.tools.core.internal.model.DartFunctionTypeAliasImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.test.util.TestUtilities;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyCompilationUnit;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.ArrayList;

public class DartIndexerTest extends TestCase {
  public void test_DartIndexer_getAllTypes() throws Exception {
    // Force the project to be loaded
    getMoneyCompilationUnit("money.dart");
    DartIndexerResult result = null;
    // Wait for up to 60 seconds for the indexer to index the world
    long start = System.currentTimeMillis();
    while (true) {
      try {
        result = DartIndexer.getAllTypes();
        break;
      } catch (IndexTemporarilyNonOperational e) {
        long delta = System.currentTimeMillis() - start;
        if (delta > 60000) {
          throw new RuntimeException("Waited " + delta + "ms for indexer", e);
        }
      }
    }
    convertResults(result, -6, Type.class, DartFunctionTypeAliasImpl.class);
  }

  public void test_DartIndexer_getSubtypes_1_none() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("currency.dart");
    Type type = unit.getType("Currency");
    DartIndexerResult result = DartIndexer.getSubtypes(type);
    convertResults(result, 0, Type.class);
  }

  public void test_DartIndexer_getSubtypes_2_none() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("currency.dart");
    Type type = unit.getType("Currency");
    DartIndexerResult result = DartIndexer.getSubtypes(type, new NullProgressMonitor());
    convertResults(result, 0, Type.class);
  }

  public void test_DartIndexer_unpackElementOrNull() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("currency.dart");
    Type type = unit.getType("Currency");
    DartElement element = DartIndexer.unpackElementOrNull(new TypeLocation(
        type,
        type.getNameRange()));
    assertTrue(element instanceof Type);
    assertEquals(type, element);
  }

  public void xtest_DartIndexer_getReferences_field_1() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    Type type = unit.getType("SimpleMoney");
    Field field = type.getField("amount");
    DartIndexerResult result = DartIndexer.getReferences(field);
    convertResults(result, 4, null);
  }

  public void xtest_DartIndexer_getReferences_field_2() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    Type type = unit.getType("SimpleMoney");
    Field field = type.getField("amount");
    DartIndexerResult result = DartIndexer.getReferences(field, new NullProgressMonitor());
    convertResults(result, 4, null);
  }

  public void xtest_DartIndexer_getReferences_method_1() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    Type type = unit.getType("SimpleMoney");
    Method method = type.getMethod("getCurrency", new String[0]);
    DartIndexerResult result = DartIndexer.getReferences(method);
    convertResults(result, 5, null);
  }

  public void xtest_DartIndexer_getReferences_method_2() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    Type type = unit.getType("SimpleMoney");
    Method method = type.getMethod("getCurrency", new String[0]);
    DartIndexerResult result = DartIndexer.getReferences(method, new NullProgressMonitor());
    convertResults(result, 5, null);
  }

  public void xtest_DartIndexer_getReferences_type_1() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    Type type = unit.getType("SimpleMoney");
    DartIndexerResult result = DartIndexer.getReferences(type);
    convertResults(result, 16, null);
  }

  public void xtest_DartIndexer_getReferences_type_2() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    Type type = unit.getType("SimpleMoney");
    DartIndexerResult result = DartIndexer.getReferences(type, new NullProgressMonitor());
    convertResults(result, 16, null);
  }

  public void xtest_DartIndexer_getSubtypes_1_multiple() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("money.dart");
    Type type = unit.getType("Money");
    DartIndexerResult result = DartIndexer.getSubtypes(type);
    convertResults(result, 2, Type.class);
  }

  public void xtest_DartIndexer_getSupertypes_1_none() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("currency.dart");
    Type type = unit.getType("Currency");
    DartIndexerResult result = DartIndexer.getSupertypes(type);
    ArrayList<Type> supertypes = convertResults(result, 1, Type.class);
    assertTrue("Object".equals(supertypes.get(0).getElementName()));
  }

  public void xtest_DartIndexer_getSupertypes_1_one() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    Type type = unit.getType("SimpleMoney");
    DartIndexerResult result = DartIndexer.getSupertypes(type);
    ArrayList<Type> supertypes = convertResults(result, 2, Type.class);
    String firstName = supertypes.get(0).getElementName();
    String secondName = supertypes.get(1).getElementName();
    assertTrue(("Money".equals(firstName) && "Object".equals(secondName))
        || ("Object".equals(firstName) && "Money".equals(secondName)));
  }

  public void xtest_DartIndexer_getSupertypes_2_none() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("complex_money.dart");
    Type type = unit.getType("ComplexMoney");
    DartIndexerResult result = DartIndexer.getSupertypes(type, new NullProgressMonitor());
    ArrayList<Type> supertypes = convertResults(result, 2, Type.class);
    String firstName = supertypes.get(0).getElementName();
    String secondName = supertypes.get(1).getElementName();
    assertTrue(("Money".equals(firstName) && "Object".equals(secondName))
        || ("Object".equals(firstName) && "Money".equals(secondName)));
  }

  private ArrayList<DartElement> convertResults(DartIndexerResult result, int expectedSize,
      Class<? extends DartElement> firstClass, Class<? extends DartElement> secondClass) {
    TestUtilities.assertNoErrors(result);
    Location[] locations = result.getResult();
    if (expectedSize < 0) {
      assertTrue("Not enough results found, expected at least " + expectedSize + " but found "
          + locations.length, locations.length >= expectedSize);
    } else {
      assertEquals("Incorrect number of results", expectedSize, locations.length);
    }
    ArrayList<DartElement> elements = new ArrayList<DartElement>(locations.length);
    for (Location location : locations) {
      DartElement element = DartIndexer.unpackElementOrNull(location);
      if (!(firstClass.isInstance(element) || secondClass.isInstance(element))) {
        fail("Expected element of type " + firstClass.getName() + " or " + secondClass.getName()
            + " but found " + element.getClass().getName());
      }
      elements.add(element);
    }
    return elements;
  }

  @SuppressWarnings("unchecked")
  private <E extends DartElement> ArrayList<E> convertResults(DartIndexerResult result,
      int expectedSize, Class<E> expectedClass) {
    TestUtilities.assertNoErrors(result);
    Location[] locations = result.getResult();
    if (expectedSize < 0) {
      assertTrue("Not enough results found, expected at least " + expectedSize + " but found "
          + locations.length, locations.length >= expectedSize);
    } else {
      assertEquals("Incorrect number of results", expectedSize, locations.length);
    }
    if (expectedClass == null) {
      return null;
    }
    ArrayList<E> elements = new ArrayList<E>(locations.length);
    for (Location location : locations) {
      DartElement element = DartIndexer.unpackElementOrNull(location);
      if (!expectedClass.isInstance(element)) {
        fail("Expected element of type " + expectedClass.getName() + " but found "
            + element.getClass().getName());
      }
      elements.add((E) element);
    }
    return elements;
  }
}
