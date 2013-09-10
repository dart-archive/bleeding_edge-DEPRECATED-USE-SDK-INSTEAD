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

package com.google.dart.tools.core.html;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class DartHtmlScriptHelperTest extends TestCase {

  public void test_getDartScripts_1() {
    List<Token> expectedScripts = new ArrayList<Token>();

    validateParse("<html><script></script></html>", expectedScripts);
  }

  public void test_getDartScripts_2() {
    List<Token> expectedScripts = new ArrayList<Token>();
    expectedScripts.add(new Token("foo!", 38, 1));

    validateParse("<html><script type=\"application/dart\">foo!</script></html>", expectedScripts);
  }

  public void test_getDartScripts_3() {
    List<Token> expectedScripts = new ArrayList<Token>();
    expectedScripts.add(new Token("foo!", 38, 1));
    expectedScripts.add(new Token("foo foo!", 84, 2));

    validateParse("<html><script type=\"application/dart\">foo!</script>\n"
        + "<script type=\"application/dart\">foo foo!</script></html>", expectedScripts);
  }

  public void test_getNonDartScripts_1() {
    String html = "<html><script type=\"application/dart\">foo!</script>\n"
        + "<script type=\"application/dart\">foo foo!</script></html>";
    List<String> scripts = DartHtmlScriptHelper.getNonDartScripts(html);
    assertEquals(0, scripts.size());
  }

  public void test_getNonDartScripts_2() {
    String html = "<html><script src=\"packages/polymer/boot.js\">foo!</script>\n"
        + "<script type=\"application/dart\">foo foo!</script></html>";
    List<String> scripts = DartHtmlScriptHelper.getNonDartScripts(html);
    assertEquals(1, scripts.size());
    assertEquals("packages/polymer/boot.js", scripts.get(0));
  }

  public void test_getNonDartScripts_3() {
    String html = "<html><script src=\"packages/polymer/boot.js\"></script>\n"
        + "<script type=\"text/javascript\" src=\"packages/browser/interop.js\"></script></html>";
    List<String> scripts = DartHtmlScriptHelper.getNonDartScripts(html);
    assertEquals(2, scripts.size());
    assertEquals("packages/polymer/boot.js", scripts.get(0));
    assertEquals("packages/browser/interop.js", scripts.get(1));
  }

  private void validateParse(String data, List<Token> expectedScripts) {
    List<Token> actualScripts = DartHtmlScriptHelper.getDartScripts(data);

    assertEquals(expectedScripts.size(), actualScripts.size());

    for (int i = 0; i < expectedScripts.size(); i++) {
      Token expected = expectedScripts.get(i);
      Token actual = actualScripts.get(i);

      assertEquals(expected.getValue(), actual.getValue());
      assertEquals(expected.getLocation(), actual.getLocation());
      assertEquals(expected.getLineNumber(), actual.getLineNumber());
    }
  }

}
