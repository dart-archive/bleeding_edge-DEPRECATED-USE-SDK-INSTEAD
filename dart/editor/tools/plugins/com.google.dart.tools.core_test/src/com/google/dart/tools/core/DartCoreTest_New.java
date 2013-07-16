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
package com.google.dart.tools.core;

import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class DartCoreTest_New extends TestCase {

  public void test_DartCore_getDartLikeExtensions() {
    String[] extensions = DartCore.getDartLikeExtensions();
    assertNotNull(extensions);
    assertEquals(1, extensions.length);
    assertEquals("dart", extensions[0]);
  }

  public void test_DartCore_isAnalyzed_false() throws Exception {
    ArrayList<String> patterns = getExclusionPatterns();
    ArrayList<String> savedPatterns = new ArrayList<String>(patterns);
    try {
      final String pattern = "/this/does/not";
      patterns.clear();
      patterns.add(pattern);
      IFile file = new MockFile(new MockProject()) {
        @Override
        public IPath getLocation() {
          return new Path(pattern + "/exist");
        }
      };
      assertFalse(DartCore.isAnalyzed(file));
      assertTrue(DartIgnoreManager.getInstance().isIgnored(file.getLocation().toPortableString()));
    } finally {
      patterns.clear();
      patterns.addAll(savedPatterns);
    }
  }

  public void test_DartCore_isAnalyzed_true() {
    IFile file = new MockFile(new MockProject()) {
      @Override
      public IPath getLocation() {
        return new Path("/this/does/not/exist");
      }
    };
    assertTrue(DartCore.isAnalyzed(file));
    assertFalse(DartIgnoreManager.getInstance().isIgnored(file.getLocation().toPortableString()));
  }

  public void test_DartCore_isCSSLikeFileName() {
    assertTrue(DartCore.isCssLikeFileName("name.css"));
    assertTrue(DartCore.isCssLikeFileName("name.CSS"));
    assertTrue(DartCore.isCssLikeFileName("name.cSS"));
    assertFalse(DartCore.isCssLikeFileName("name.cs"));
    assertFalse(DartCore.isCssLikeFileName("namecss"));
  }

  public void test_DartCore_isDartGeneratedFile() {
    assertTrue(DartCore.isDartGeneratedFile("name.js"));
    assertTrue(DartCore.isDartGeneratedFile("name.JS"));
    assertTrue(DartCore.isDartGeneratedFile("name.jS"));
    assertFalse(DartCore.isDartGeneratedFile("name.j"));
    assertFalse(DartCore.isDartGeneratedFile("namejs"));
  }

  public void test_DartCore_isDartLikeFileName() {
    assertTrue(DartCore.isDartLikeFileName("name.dart"));
    assertTrue(DartCore.isDartLikeFileName("name.DART"));
    assertTrue(DartCore.isDartLikeFileName("name.dART"));
    assertFalse(DartCore.isDartLikeFileName("name.dar"));
    assertFalse(DartCore.isDartLikeFileName("namedart"));
  }

  public void test_DartCore_isHTMLLikeFileName() {
    assertTrue(DartCore.isHtmlLikeFileName("name.html"));
    assertTrue(DartCore.isHtmlLikeFileName("name.HTML"));
    assertTrue(DartCore.isHtmlLikeFileName("name.hTML"));
    assertFalse(DartCore.isHtmlLikeFileName("name.ht"));
    assertFalse(DartCore.isHtmlLikeFileName("namehtml"));
    // again for "htm"
    assertTrue(DartCore.isHtmlLikeFileName("name.htm"));
    assertTrue(DartCore.isHtmlLikeFileName("name.HTM"));
    assertTrue(DartCore.isHtmlLikeFileName("name.hTM"));
    assertFalse(DartCore.isHtmlLikeFileName("namehtm"));
  }

  public void test_DartCore_isJSLikeFileName() {
    assertTrue(DartCore.isJSLikeFileName("name.js"));
    assertTrue(DartCore.isJSLikeFileName("name.JS"));
    assertTrue(DartCore.isJSLikeFileName("name.jS"));
    assertFalse(DartCore.isJSLikeFileName("name.j"));
    assertFalse(DartCore.isJSLikeFileName("namejs"));
  }

  public void test_DartCore_isTXTLikeFileName() {
    assertTrue(DartCore.isTxtLikeFileName("name.txt"));
    assertTrue(DartCore.isTxtLikeFileName("name.TXT"));
    assertTrue(DartCore.isTxtLikeFileName("name.tXt"));
    assertFalse(DartCore.isTxtLikeFileName("name.tx"));
    assertFalse(DartCore.isTxtLikeFileName("nametxt"));
  }

  @SuppressWarnings("unchecked")
  private ArrayList<String> getExclusionPatterns() throws Exception {
    Method method = DartIgnoreManager.class.getDeclaredMethod("getExclusionPatterns");
    method.setAccessible(true);
    return (ArrayList<String>) method.invoke(DartIgnoreManager.getInstance());
  }
}
