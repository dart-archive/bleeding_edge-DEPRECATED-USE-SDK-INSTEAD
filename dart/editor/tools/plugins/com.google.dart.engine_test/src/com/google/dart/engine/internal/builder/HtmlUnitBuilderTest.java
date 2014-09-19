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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.EmbeddedHtmlScriptElementImpl;
import com.google.dart.engine.internal.element.ExternalHtmlScriptElementImpl;
import com.google.dart.engine.internal.element.HtmlElementImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

public class HtmlUnitBuilderTest extends EngineTestCase {

  private class ExpectedLibrary {
    private final ExpectedVariable[] expectedVariables;

    public ExpectedLibrary(ExpectedVariable[] expectedVariables) {
      this.expectedVariables = expectedVariables;
    }

    private void validate(int scriptIndex, EmbeddedHtmlScriptElementImpl script) {
      LibraryElement library = script.getScriptLibrary();
      assertNotNull("script " + scriptIndex, library);
      assertSame("script " + scriptIndex, context, script.getContext());
      CompilationUnitElement unit = library.getDefiningCompilationUnit();
      assertNotNull("script " + scriptIndex, unit);
      TopLevelVariableElement[] variables = unit.getTopLevelVariables();
      assertLength(expectedVariables.length, variables);
      for (int index = 0; index < variables.length; index++) {
        expectedVariables[index].validate(scriptIndex, variables[index]);
      }
      assertSame("script " + scriptIndex, script, library.getEnclosingElement());
    }
  }

  private class ExpectedScript {
    private final String expectedExternalScriptName;
    private final ExpectedLibrary expectedLibrary;

    public ExpectedScript(ExpectedLibrary expectedLibrary) {
      this.expectedExternalScriptName = null;
      this.expectedLibrary = expectedLibrary;
    }

    ExpectedScript(String expectedExternalScriptPath) {
      this.expectedExternalScriptName = expectedExternalScriptPath;
      this.expectedLibrary = null;
    }

    void validate(int scriptIndex, HtmlScriptElement script) {
      if (expectedLibrary != null) {
        validateEmbedded(scriptIndex, script);
      } else {
        validateExternal(scriptIndex, script);
      }
    }

    void validateEmbedded(int scriptIndex, HtmlScriptElement script) {
      if (!(script instanceof EmbeddedHtmlScriptElementImpl)) {
        fail("Expected script " + scriptIndex + " to be embedded, but found "
            + (script != null ? script.getClass() : "null"));
      }
      EmbeddedHtmlScriptElementImpl embeddedScript = (EmbeddedHtmlScriptElementImpl) script;
      expectedLibrary.validate(scriptIndex, embeddedScript);
    }

    void validateExternal(int scriptIndex, HtmlScriptElement script) {
      if (!(script instanceof ExternalHtmlScriptElementImpl)) {
        fail("Expected script " + scriptIndex + " to be external with src="
            + expectedExternalScriptName + " but found "
            + (script != null ? script.getClass() : "null"));
      }
      ExternalHtmlScriptElementImpl externalScript = (ExternalHtmlScriptElementImpl) script;
      Source scriptSource = externalScript.getScriptSource();
      if (expectedExternalScriptName == null) {
        assertNull("script " + scriptIndex, scriptSource);
      } else {
        assertNotNull("script " + scriptIndex, scriptSource);
        String actualExternalScriptName = scriptSource.getShortName();
        assertEquals("script " + scriptIndex, expectedExternalScriptName, actualExternalScriptName);
      }
    }
  }

  private class ExpectedVariable {
    private final String expectedName;

    public ExpectedVariable(String expectedName) {
      this.expectedName = expectedName;
    }

    public void validate(int scriptIndex, TopLevelVariableElement variable) {
      assertNotNull("script " + scriptIndex, variable);
      assertEquals("script " + scriptIndex, expectedName, variable.getName());
    }
  }

  private AnalysisContextImpl context;

  public void test_embedded_script() throws Exception {
    HtmlElementImpl element = build(createSource(//
        "<html>",
        "<script type=\"application/dart\">foo=2;</script>",
        "</html>"));
    validate(element, s(l(v("foo"))));
  }

  public void test_embedded_script_no_content() throws Exception {
    HtmlElementImpl element = build(createSource(//
        "<html>",
        "<script type=\"application/dart\"></script>",
        "</html>"));
    validate(element, s(l()));
  }

  public void test_external_script() throws Exception {
    HtmlElementImpl element = build(createSource(//
        "<html>",
        "<script type=\"application/dart\" src=\"other.dart\"/>",
        "</html>"));
    validate(element, s("other.dart"));
  }

  public void test_external_script_no_source() throws Exception {
    HtmlElementImpl element = build(createSource(//
        "<html>",
        "<script type=\"application/dart\"/>",
        "</html>"));
    validate(element, s((String) null));
  }

  public void test_external_script_with_content() throws Exception {
    HtmlElementImpl element = build(createSource(//
        "<html>",
        "<script type=\"application/dart\" src=\"other.dart\">blat=2;</script>",
        "</html>"));
    validate(element, s("other.dart"));
  }

  public void test_no_scripts() throws Exception {
    HtmlElementImpl element = build(createSource(//
        "<!DOCTYPE html>",
        "<html><p></p></html>"));
    validate(element);
  }

  public void test_two_dart_scripts() throws Exception {
    HtmlElementImpl element = build(createSource(//
        "<html>",
        "<script type=\"application/dart\">bar=2;</script>",
        "<script type=\"application/dart\" src=\"other.dart\"/>",
        "<script src=\"dart.js\"/>",
        "</html>"));
    validate(element, s(l(v("bar"))), s("other.dart"));
  }

  @Override
  protected void setUp() throws Exception {
    context = AnalysisContextFactory.contextWithCore();
  }

  @Override
  protected void tearDown() throws Exception {
    context = null;
    super.tearDown();
  }

  ExpectedLibrary l(ExpectedVariable... expectedVariables) {
    return new ExpectedLibrary(expectedVariables);
  }

  private HtmlElementImpl build(String contents) throws Exception {
    TestSource source = new TestSource(createFile("/test.html"), contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    context.applyChanges(changeSet);

    HtmlUnitBuilder builder = new HtmlUnitBuilder(context);
    return builder.buildHtmlElement(
        source,
        context.getModificationStamp(source),
        context.parseHtmlUnit(source));
  }

  private ExpectedScript s(ExpectedLibrary expectedLibrary) {
    return new ExpectedScript(expectedLibrary);
  }

  private ExpectedScript s(String scriptSourcePath) {
    return new ExpectedScript(scriptSourcePath);
  }

  private ExpectedVariable v(String varName) {
    return new ExpectedVariable(varName);
  }

  private void validate(HtmlElementImpl element, ExpectedScript... expectedScripts) {
    assertSame(context, element.getContext());
    HtmlScriptElement[] scripts = element.getScripts();
    assertNotNull(scripts);
    assertLength(expectedScripts.length, scripts);
    for (int scriptIndex = 0; scriptIndex < scripts.length; scriptIndex++) {
      expectedScripts[scriptIndex].validate(scriptIndex, scripts[scriptIndex]);
    }
  }
}
