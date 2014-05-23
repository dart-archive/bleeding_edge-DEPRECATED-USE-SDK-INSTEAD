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

package com.google.dart.server.internal.local.computer;

import com.google.dart.server.internal.local.AbstractLocalServerTest;

// TODO(scheglov) restore or remove for the new API
public class DartUnitNavigationComputerTest extends AbstractLocalServerTest {
  public void test_constructor_named() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "class A {",
//        "  A.named(int p) {}",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    {
//      ElementAssert elementAssert = validator.hasRegion(
//          source,
//          "A.named(int p) {}",
//          "A.named".length());
//      elementAssert.isIn(source, "named(int p) {}").hasLength("named".length());
//    }
//    // no separate regions for "A" and "named"
//    validator.hasNoRegion(source, "A.named(", "A".length());
//    validator.hasNoRegion(source, "named(", "named".length());
//    // validate that we don't forget to resolve parameters
//    {
//      ElementAssert elementAssert = validator.hasRegion(source, "int p) {}", "int".length());
//      elementAssert.isInSdk();
//    }
  }

//  public void test_constructor_unnamed() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "class A {",
//        "  A(int p) {}",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    {
//      ElementAssert elementAssert = validator.hasRegion(source, "A(int p) {}", "A".length());
//      elementAssert.isIn(source, "A(int p) {}").hasLength(0);
//    }
//    // validate that we don't forget to resolve parameters
//    {
//      ElementAssert elementAssert = validator.hasRegion(source, "int p) {}", "int".length());
//      elementAssert.isInSdk();
//    }
//  }
//
//  public void test_fieldFormalParameter() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "class A {",
//        "  int fff = 123;",
//        "  A(this.fff);",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "fff);", 3).isIn(source, "fff = 123;").hasLength(3);
//  }
//
//  public void test_identifier_resolved() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "main() {",
//        "  int vvv = 123;",
//        "  print(vvv);",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "main()", 4).isIn(source, "main() {").hasLength(4);
//    validator.hasRegion(source, "int vvv", 3).isInSdk().hasLength(3);
//    validator.hasRegion(source, "vvv)", 3).isIn(source, "vvv = 123").hasLength(3);
//  }
//
//  public void test_identifier_unresolved() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "main() {",
//        "  int vvv = 123;",
//        "  print(noo);",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.isNotEmpty();
//    assertNull(validator.findRegion(source, "noo", 3));
//  }
//
//  public void test_instanceCreation_named() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "class A {",
//        "  A.named() {}",
//        "}",
//        "",
//        "main() {",
//        "  new A.named();",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "new A.named").isIn(source, "named() {}").hasLength(
//        "named".length());
//  }
//
//  public void test_instanceCreation_unnamed() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "class A {",
//        "  A() {}",
//        "}",
//        "",
//        "main() {",
//        "  new A();",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "new A").isIn(source, "A() {}").hasLength(0);
//  }
//
//  public void test_operator_int() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "main() {",
//        "  var v = 0;",
//        "  v - 1;",
//        "  v + 2;",
//        "  -v; // unary",
//        "  --v;",
//        "  ++v;",
//        "  v--; // mm",
//        "  v++; // pp",
//        "  v -= 3;",
//        "  v += 4;",
//        "  v *= 5;",
//        "  v /= 6;",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "- 1;", 1).isInSdk().hasLength(1);
//    validator.hasRegion(source, "+ 2;", 1).isInSdk().hasLength(1);
//    validator.hasRegion(source, "-v; // unary", 1).isInSdk().hasLength(1);
//    validator.hasRegion(source, "--v;", 2).isInSdk().hasLength(1);
//    validator.hasRegion(source, "++v;", 2).isInSdk().hasLength(1);
//    validator.hasRegion(source, "--; // mm", 2).isInSdk().hasLength(1);
//    validator.hasRegion(source, "++; // pp", 2).isInSdk().hasLength(1);
//    validator.hasRegion(source, "-=", 2).isInSdk().hasLength(1);
//    validator.hasRegion(source, "+=", 2).isInSdk().hasLength(1);
//    validator.hasRegion(source, "*=", 2).isInSdk().hasLength(1);
//    validator.hasRegion(source, "/=", 2).isInSdk().hasLength(1);
//  }
//
//  public void test_operator_list() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "main() {",
//        "  List<int> v = [1, 2, 3];",
//        "  v[0]; // []",
//        "  v[1] = 1; // []=",
//        "  v[2] += 2;",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "]; // []", 1).isInSdk().hasLength(2);
//    validator.hasRegion(source, "] = 1; // []=", 1).isInSdk().hasLength(3);
//    validator.hasRegion(source, "] += 2;", 1).isInSdk().hasLength(3);
//    validator.hasRegion(source, "+= 2;", 2).isInSdk().hasLength(1);
//  }
//
//  public void test_operator_map() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "main() {",
//        "  Map<int, int> v = {0: 0, 1 : 10, 2: 20};",
//        "  v[0]; // []",
//        "  v[1] = 1; // []=",
//        "  v[2] += 2;",
//        "}"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "]; // []", 1).isInSdk().hasLength(2);
//    validator.hasRegion(source, "] = 1; // []=", 1).isInSdk().hasLength(3);
//    validator.hasRegion(source, "] += 2;", 1).isInSdk().hasLength(3);
//    validator.hasRegion(source, "+= 2;", 2).isInSdk().hasLength(1);
//  }
//
//  public void test_partOf() throws Exception {
//    String contextId = createContext("test");
//    Source unitSource = addSource(contextId, "/test-unit.dart", "part of lib;");
//    Source libSource = addSource(contextId, "/test.dart", makeSource(//
//        "library lib;",
//        "part 'test-unit.dart';"));
//    prepareNavigationRegions(contextId, unitSource);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(
//        contextId,
//        unitSource);
//    validator.hasRegion(unitSource, "part of lib").isIn(libSource, "lib;");
//  }
//
//  public void test_string_export() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", "export 'dart:math';");
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "export 'dart:math'").isInSdk().hasLength("dart.math".length());
//  }
//
//  public void test_string_export_unresolvedUri() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", "export 'no.dart';");
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.noRegionAt(source, "export ");
//  }
//
//  public void test_string_import() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", "import 'dart:math' as m;");
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "import 'dart:math'").isInSdk().hasLength("dart.math".length());
//  }
//
//  public void test_string_import_noUri() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", "import ;");
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.noRegionAt(source, "import ");
//  }
//
//  public void test_string_import_unresolvedUri() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", "import 'no.dart';");
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.noRegionAt(source, "import ");
//  }
//
//  public void test_string_part() throws Exception {
//    String contextId = createContext("test");
//    Source unitSource = addSource(contextId, "/test-unit.dart", "path of lib;");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "library lib;",
//        "part 'test-unit.dart';"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.hasRegion(source, "part 'test-unit.dart'").isIn(unitSource, -1).hasLength(0);
//  }
//
//  public void test_string_part_unresolvedUri() throws Exception {
//    String contextId = createContext("test");
//    Source source = addSource(contextId, "/test.dart", makeSource(//
//        "library lib;",
//        "part 'test-unit.dart';"));
//    prepareNavigationRegions(contextId, source);
//    // validate
//    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
//    validator.noRegionAt(source, "part ");
//  }
//
//  private void prepareNavigationRegions(String contextId, Source source) {
//    server.subscribe(
//        contextId,
//        ImmutableMap.of(NotificationKind.NAVIGATION, ListSourceSet.create(source)));
//    server.test_waitForWorkerComplete();
//    serverListener.assertNoServerErrors();
//  }
}
