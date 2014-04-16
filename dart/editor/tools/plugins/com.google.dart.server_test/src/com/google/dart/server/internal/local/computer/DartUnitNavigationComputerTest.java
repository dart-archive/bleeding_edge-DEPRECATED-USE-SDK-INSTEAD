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

import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.source.Source;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.internal.local.AbstractLocalServerTest;
import com.google.dart.server.internal.local.asserts.NavigationRegionsAssert;

public class DartUnitNavigationComputerTest extends AbstractLocalServerTest {
  public void test_identifier_resolved() throws Exception {
    String contextId = createContext("test");
    Source source = addSource(contextId, "/test.dart", makeSource(//
        "main() {",
        "  int vvv = 123;",
        "  print(vvv);",
        "}"));
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.NAVIGATION, TestListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
    validator.hasRegion(source, "main()", 4).isIn(source, "main() {").hasLength(4);
    validator.hasRegion(source, "int vvv", 3).isInSdk().hasLength(3);
    validator.hasRegion(source, "vvv)", 3).isIn(source, "vvv = 123").hasLength(3);
  }

  public void test_identifier_unresolved() throws Exception {
    String contextId = createContext("test");
    Source source = addSource(contextId, "/test.dart", makeSource(//
        "main() {",
        "  int vvv = 123;",
        "  print(noo);",
        "}"));
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.NAVIGATION, TestListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
    validator.isNotEmpty();
    assertNull(validator.findRegion(source, "noo", 3));
  }

  public void test_operator_int() throws Exception {
    String contextId = createContext("test");
    Source source = addSource(contextId, "/test.dart", makeSource(//
        "main() {",
        "  var v = 0;",
        "  v - 1;",
        "  v + 2;",
        "  -v; // unary",
        "  --v;",
        "  ++v;",
        "  v--; // mm",
        "  v++; // pp",
        "  v -= 3;",
        "  v += 4;",
        "  v *= 5;",
        "  v /= 6;",
        "}"));
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.NAVIGATION, TestListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
    validator.hasRegion(source, "- 1;", 1).isInSdk().hasLength(1);
    validator.hasRegion(source, "+ 2;", 1).isInSdk().hasLength(1);
    validator.hasRegion(source, "-v; // unary", 1).isInSdk().hasLength(1);
    validator.hasRegion(source, "--v;", 2).isInSdk().hasLength(1);
    validator.hasRegion(source, "++v;", 2).isInSdk().hasLength(1);
    validator.hasRegion(source, "--; // mm", 2).isInSdk().hasLength(1);
    validator.hasRegion(source, "++; // pp", 2).isInSdk().hasLength(1);
    validator.hasRegion(source, "-=", 2).isInSdk().hasLength(1);
    validator.hasRegion(source, "+=", 2).isInSdk().hasLength(1);
    validator.hasRegion(source, "*=", 2).isInSdk().hasLength(1);
    validator.hasRegion(source, "/=", 2).isInSdk().hasLength(1);
  }

  public void test_operator_list() throws Exception {
    String contextId = createContext("test");
    Source source = addSource(contextId, "/test.dart", makeSource(//
        "main() {",
        "  var v = [1, 2, 3];",
        "  v[0]; // []",
        "  v[1] = 1; // []=",
        "  v[2] += 2;",
        "}"));
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.NAVIGATION, TestListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
    validator.hasRegion(source, "]; // []", 1).isInSdk().hasLength(2);
    validator.hasRegion(source, "] = 1; // []=", 1).isInSdk().hasLength(3);
    validator.hasRegion(source, "] += 2;", 1).isInSdk().hasLength(3);
    validator.hasRegion(source, "+= 2;", 2).isInSdk().hasLength(1);
  }

  public void test_operator_map() throws Exception {
    String contextId = createContext("test");
    Source source = addSource(contextId, "/test.dart", makeSource(//
        "main() {",
        "  var v = {0: 0, 1 : 10, 2: 20};",
        "  v[0]; // []",
        "  v[1] = 1; // []=",
        "  v[2] += 2;",
        "}"));
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.NAVIGATION, TestListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    NavigationRegionsAssert validator = serverListener.assertNavigationRegions(contextId, source);
    validator.hasRegion(source, "]; // []", 1).isInSdk().hasLength(2);
    validator.hasRegion(source, "] = 1; // []=", 1).isInSdk().hasLength(3);
    validator.hasRegion(source, "] += 2;", 1).isInSdk().hasLength(3);
    validator.hasRegion(source, "+= 2;", 2).isInSdk().hasLength(1);
  }
}
