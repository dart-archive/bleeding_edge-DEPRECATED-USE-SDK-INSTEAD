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
package com.google.dart.engine.resolver;

import com.google.dart.engine.source.Source;

public class NonHintCodeTest extends ResolverTestCase {

  public void test_deadCode_deadCatch_onCatchSubtype() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {}",
        "f() {",
        "  try {} on B catch (e) {} on A catch (e) {} catch (e) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_unusedImport_export() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';",
        "Two two;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "export 'lib2.dart';",
        "class One {}"));
    addSource("/lib2.dart", createSource(//
        "library lib2;",
        "class Two {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_unusedImport_export_infiniteLoop() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';",
        "Two two;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "export 'lib2.dart';",
        "class One {}"));
    addSource("/lib2.dart", createSource(//
        "library lib2;",
        "export 'lib3.dart';",
        "class Two {}"));
    addSource("/lib3.dart", createSource(//
        "library lib3;",
        "export 'lib2.dart';",
        "class Three {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_unusedImport_export2() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';",
        "Three three;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "export 'lib2.dart';",
        "class One {}"));
    addSource("/lib2.dart", createSource(//
        "library lib2;",
        "export 'lib3.dart';",
        "class Two {}"));
    addSource("/lib3.dart", createSource(//
        "library lib3;",
        "class Three {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_unusedImport_prefix_topLevelFunction() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart' hide topLevelFunction;",
        "import 'lib1.dart' as one show topLevelFunction;",
        "class A {",
        "  static void x() {",
        "    One o;",
        "    one.topLevelFunction();",
        "  }",
        "}"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class One {}",
        "topLevelFunction() {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }
}
