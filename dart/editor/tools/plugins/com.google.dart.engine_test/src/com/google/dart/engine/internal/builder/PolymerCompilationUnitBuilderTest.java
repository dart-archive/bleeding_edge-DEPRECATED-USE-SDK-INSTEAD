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

import com.google.dart.engine.internal.html.polymer.PolymerTest;

public class PolymerCompilationUnitBuilderTest extends PolymerTest {
  public void test_badAnnotation_noArguments() throws Exception {
    addTagDartSource(createSource(//
        "class MyAnnotation {}",
        "@MyAnnotation",
        "class MyElement {",
        "}",
        ""));
    resolveTagDart();
    assertNull(tagDartElement);
  }

  public void test_badAnnotation_notConstructor() throws Exception {
    addTagDartSource(createSource(//
        "@NoSuchAnnotation()",
        "class MyElement {",
        "}",
        ""));
    resolveTagDart();
    assertNull(tagDartElement);
  }

  public void test_customTag() throws Exception {
    addTagDartSource(createSource(//
        "import 'polymer.dart';",
        "",
        "@CustomTag('my-element') // marker",
        "class MyElement {",
        "}",
        ""));
    resolveTagDart();
    assertNotNull(tagDartElement);
    assertEquals("my-element", tagDartElement.getName());
    assertEquals(findTagDartOffset("my-element') // marker"), tagDartElement.getNameOffset());
  }
}
