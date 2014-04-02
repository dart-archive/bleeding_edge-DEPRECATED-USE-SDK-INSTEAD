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
package com.google.dart.engine.internal.html.polymer;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextHelper;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.polymer.PolymerTagDartElement;
import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;
import com.google.dart.engine.source.Source;

import static org.fest.assertions.Assertions.assertThat;

abstract public class PolymerTest extends EngineTestCase {
  /**
   * @return the offset of given <code>search</code> string in <code>contents</code>. Fails test if
   *         not found.
   */
  protected static int findOffset(String contents, String search) {
    int offset = contents.indexOf(search);
    assertThat(offset).describedAs(contents).isNotEqualTo(-1);
    return offset;
  }

  protected final AnalysisContextHelper contextHelper = new AnalysisContextHelper();

  protected AnalysisContext context;
  protected Source tagDartSource;
  protected String tagDartContents;
  protected CompilationUnitElement tagDartUnitElement;
  protected PolymerTagDartElement tagDartElement;
  protected PolymerTagHtmlElement tagHtmlElement;

  protected final void addTagDartSource(String contents) {
    tagDartContents = contents;
    tagDartSource = contextHelper.addSource("/my-element.dart", contents);
  }

  /**
   * @return the offset of given <code>search</code> string in {@link #tagDartContents}. Fails test
   *         if not found.
   */
  protected final int findTagDartOffset(String search) {
    return findOffset(tagDartContents, search);
  }

  protected final void resolveTagDart() throws Exception {
    contextHelper.runTasks();
    tagDartUnitElement = contextHelper.getDefiningUnitElement(tagDartSource);
    // try to find PolymerTagDartElement
    for (ClassElement classElement : tagDartUnitElement.getTypes()) {
      for (ToolkitObjectElement toolkitObject : classElement.getToolkitObjects()) {
        if (toolkitObject instanceof PolymerTagDartElement) {
          tagDartElement = (PolymerTagDartElement) toolkitObject;
        }
      }
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    configureForPolymer(contextHelper);
    context = contextHelper.context;
  }

  private void configureForPolymer(AnalysisContextHelper contextHelper) {
    contextHelper.addSource(
        "/polymer.dart",
        createSource(
            "library polymer;",
            "",
            "class CustomTag {",
            "  final String tagName;",
            "  const CustomTag(this.tagName);",
            "}",
            ""));
  }
}
