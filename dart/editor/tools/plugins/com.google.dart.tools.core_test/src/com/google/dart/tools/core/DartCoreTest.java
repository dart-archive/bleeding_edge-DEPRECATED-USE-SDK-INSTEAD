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
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class DartCoreTest extends TestCase {
  public void test_DartCore_create_file_notNull() {
    IFile file = new MockFile(new MockProject());
    DartElement element = DartCore.create(file);
    assertNull(element);
    // assertNotNull(element);
    // assertEquals(file, element.getResource());
  }

  public void test_DartCore_create_file_null() {
    DartElement element = DartCore.create((IFile) null);
    assertNull(element);
  }

  public void test_DartCore_create_folder_notNull() {
    IFolder folder = new MockFolder();
    DartElement element = DartCore.create(folder);
    assertNull(element);
    // assertNotNull(element);
    // assertEquals(file, element.getResource());
  }

  public void test_DartCore_create_folder_null() {
    DartElement element = DartCore.create((IFolder) null);
    assertNull(element);
  }

  public void test_DartCore_create_model_notNull() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    DartModel model = DartCore.create(workspace.getRoot());
    assertNotNull(model);
    assertEquals(workspace, model.getWorkspace());
  }

  public void test_DartCore_create_model_null() {
    DartModel model = DartCore.create((IWorkspaceRoot) null);
    assertNull(model);
  }

  public void test_DartCore_create_project_notNull() {
    IProject project = new MockProject();
    DartProject dartProject = DartCore.create(project);
    assertNotNull(dartProject);
    assertEquals(project, dartProject.getProject());
  }

  public void test_DartCore_create_project_null() {
    DartProject dartProject = DartCore.create((IProject) null);
    assertNull(dartProject);
  }

  public void test_DartCore_create_string_empty() {
    DartElement element = DartCore.create("");
    assertTrue(element instanceof DartModel);
  }

  public void test_DartCore_create_string_null() {
    DartElement element = DartCore.create((String) null);
    assertNull(element);
  }

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
    assertTrue(DartCore.isCSSLikeFileName("name.css"));
    assertTrue(DartCore.isCSSLikeFileName("name.CSS"));
    assertTrue(DartCore.isCSSLikeFileName("name.cSS"));
    assertFalse(DartCore.isCSSLikeFileName("name.cs"));
    assertFalse(DartCore.isCSSLikeFileName("namecss"));
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
    assertTrue(DartCore.isHTMLLikeFileName("name.html"));
    assertTrue(DartCore.isHTMLLikeFileName("name.HTML"));
    assertTrue(DartCore.isHTMLLikeFileName("name.hTML"));
    assertFalse(DartCore.isHTMLLikeFileName("name.ht"));
    assertFalse(DartCore.isHTMLLikeFileName("namehtml"));
    // again for "htm"
    assertTrue(DartCore.isHTMLLikeFileName("name.htm"));
    assertTrue(DartCore.isHTMLLikeFileName("name.HTM"));
    assertTrue(DartCore.isHTMLLikeFileName("name.hTM"));
    assertFalse(DartCore.isHTMLLikeFileName("namehtm"));
  }

  public void test_DartCore_isJSLikeFileName() {
    assertTrue(DartCore.isJSLikeFileName("name.js"));
    assertTrue(DartCore.isJSLikeFileName("name.JS"));
    assertTrue(DartCore.isJSLikeFileName("name.jS"));
    assertFalse(DartCore.isJSLikeFileName("name.j"));
    assertFalse(DartCore.isJSLikeFileName("namejs"));
  }

  public void test_DartCore_isTXTLikeFileName() {
    assertTrue(DartCore.isTXTLikeFileName("name.txt"));
    assertTrue(DartCore.isTXTLikeFileName("name.TXT"));
    assertTrue(DartCore.isTXTLikeFileName("name.tXt"));
    assertFalse(DartCore.isTXTLikeFileName("name.tx"));
    assertFalse(DartCore.isTXTLikeFileName("nametxt"));
  }

  @SuppressWarnings("unchecked")
  private ArrayList<String> getExclusionPatterns() throws Exception {
    Method method = DartIgnoreManager.class.getDeclaredMethod("getExclusionPatterns");
    method.setAccessible(true);
    return (ArrayList<String>) method.invoke(DartIgnoreManager.getInstance());
  }
}
