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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.HTMLFileInfo;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.test.util.HTMLFactory;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.lang.reflect.Constructor;

public class HTMLFileImplTest extends TestCase {
  public void test_HTMLFileImpl() {
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, createMockFile());
    assertNotNull(htmlFile);
  }

  public void test_HTMLFileImpl_getChildren() throws DartModelException {
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, createMockFile());
    DartElement[] children = htmlFile.getChildren();
    assertNotNull(children);
    assertEquals(0, children.length);
  }

  public void test_HTMLFileImpl_getElementInfo() throws DartModelException {
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, createMockFile());
    DartElementInfo info = htmlFile.getElementInfo();
    assertTrue(info instanceof HTMLFileInfo);
    HTMLFileInfo htmlInfo = (HTMLFileInfo) info;
    DartElement[] children = htmlInfo.getChildren();
    assertNotNull(children);
    assertEquals(0, children.length);
    assertTrue(htmlInfo.isStructureKnown());
  }

  public void test_HTMLFileImpl_getElementName() {
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, createMockFile());
    assertEquals("index.html", htmlFile.getElementName());
  }

  public void test_HTMLFileImpl_getElementType() {
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, createMockFile());
    assertEquals(DartElement.HTML_FILE, htmlFile.getElementType());
  }

  public void test_HTMLFileImpl_getHandleIdentifier() {
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, createMockFile());
    assertEquals("@index.html", htmlFile.getHandleIdentifier());
  }

  public void test_HTMLFileImpl_getReferencedLibraries_libraries() throws DartModelException {
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, createMockFile(HTMLFactory.allScriptTypes()));
    DartLibrary[] libraries = htmlFile.getReferencedLibraries();
    assertNotNull(libraries);
//    assertEquals(1, libraries.length);
  }

  public void test_HTMLFileImpl_getReferencedLibraries_noLibraries() throws DartModelException {
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, createMockFile(HTMLFactory.noScripts()));
    DartLibrary[] libraries = htmlFile.getReferencedLibraries();
    assertNotNull(libraries);
    assertEquals(0, libraries.length);
  }

  public void test_HTMLFileImpl_getUnderlyingResource() throws DartModelException {
    IFile file = createMockFile();
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, file);
    assertEquals(file, htmlFile.getUnderlyingResource());
  }

  public void test_HTMLFileImpl_resource() {
    IFile file = createMockFile();
    HTMLFileImpl htmlFile = createHTMLFileImpl(null, file);
    assertEquals(file, htmlFile.resource());
  }

  private HTMLFileImpl createHTMLFileImpl(DartProjectImpl project, IFile file) {
    try {
      Constructor<HTMLFileImpl> constructor = HTMLFileImpl.class.getDeclaredConstructor(
          DartLibraryImpl.class,
          IFile.class);
      if (constructor == null) {
        return null;
      }
      constructor.setAccessible(true);
      return constructor.newInstance(project, file);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  private IFile createMockFile() {
    return createMockFile("");
  }

  private IFile createMockFile(String contents) {
    String fileName = "index.html";
    MockProject project = new MockProject("someProject");
    MockFile file = new MockFile(project, fileName, contents) {
      @Override
      public IPath getProjectRelativePath() {
        return new Path(getName());
      }
    };
    return file;
  }
}
