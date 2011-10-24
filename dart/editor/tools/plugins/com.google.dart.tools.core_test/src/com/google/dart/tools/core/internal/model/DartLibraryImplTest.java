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
package com.google.dart.tools.core.internal.model;

import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.dart.compiler.DefaultLibrarySource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.indexer.standard.StandardDriver;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DartLibraryImplTest extends TestCase {
  private static File tempDir = null;

  private static void createTempFile(File file, final String content) throws IOException {
    assertTrue(file.getParentFile().exists());
    Files.copy(new InputSupplier<InputStream>() {
      @Override
      public InputStream getInput() throws IOException {
        return new ByteArrayInputStream(content.getBytes());
      }
    }, file);
    file.deleteOnExit();
  }

  private static File getTempDir() throws IOException {
    if (tempDir == null) {
      tempDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(
          DartLibraryImplTest.class.getSimpleName() + ".tmp").toFile();
      makeTempDir(tempDir);
    }
    return tempDir;
  }

  private static void makeTempDir(File dir) {
    assertFalse(dir.exists());
    assertTrue(dir.mkdir());
    dir.deleteOnExit();
  }

  private DartLibraryImpl dartLibEmpty;
  private DartLibraryImpl dartLib1;
  private DartLibraryImpl dartLib2;
  private DartLibraryImpl dartLib3;
  private DartLibraryImpl dartLib3Alt;
  private DartLibraryImpl dartLibExternal;

  private final Map<DartLibraryImpl, Collection<DartElement>> libraryChildrenWithCachedInfos = new HashMap<DartLibraryImpl, Collection<DartElement>>();

  public void test_DartLibraryImpl_close() throws Exception {
    File libraryFile = TestUtilities.getPluginRelativePath("com.google.dart.tools.core_test",
        new Path("test_data/Geometry/geometry.dart")).toFile();
    final DartLibrary library = DartCore.openLibrary(libraryFile, null);
    assertNotNull(library);
    IProject project = ((DartProject) library.getParent()).getProject();
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        library.delete(monitor);
      }
    }, null);
    assertFalse(project.exists());
  }

  /**
   * Test that two different handles with the same project and file are equal
   */
  public void test_DartLibraryImpl_equals_lib1() throws Exception {
    DartLibraryImpl lib1a = getDartLib1();
    IProject proj = lib1a.getResource().getProject();
    DartProjectImpl dartProj = new DartProjectImpl(getDartModel(), proj);
    DartLibraryImpl lib1b = new DartLibraryImpl(dartProj, proj.getFile("lib1.dart"));
    assertEquals(lib1a, lib1b);
  }

  public void test_DartLibraryImpl_equals_lib1NotLib2() throws Exception {
    if (getDartLib1().equals(getDartLib2())) {
      fail();
    }
  }

  public void test_DartLibraryImpl_equals_lib1NotLibDom() throws Exception {
    if (getDartLib1().equals(getDartLibDom())) {
      fail();
    }
  }

  public void test_DartLibraryImpl_equals_lib1NotLibExternal() throws Exception {
    if (getDartLib1().equals(getDartLibExternal())) {
      fail();
    }
  }

  /**
   * Test that a handle "in the workspace" and a handle "not in the workspace" but pointing to the
   * same file on disk are equal
   */
  public void test_DartLibraryImpl_equals_lib3Alt() throws Exception {
    assertEquals(getDartLib3(), getDartLib3Alt());
  }

  /**
   * Test equals and sanity check that the core library can be accessed
   */
  public void test_DartLibraryImpl_equals_libCore() throws Exception {
    LibrarySource libSrc = getDartLibCore().getLibrarySourceFile();
    assertEquals(getDartLibCore(), new DartLibraryImpl(libSrc));
  }

  /**
   * Test equals and sanity check that the core impl library can be accessed
   */
  public void test_DartLibraryImpl_equals_libCoreImpl() throws Exception {
    LibrarySource libSrc = getDartLibCoreImpl().getLibrarySourceFile();
    assertEquals(getDartLibCoreImpl(), new DartLibraryImpl(libSrc));
  }

  /**
   * Test equals and sanity check that the dom library can be accessed
   */
  public void test_DartLibraryImpl_equals_libDom() throws Exception {
    LibrarySource libSrc = getDartLibDom().getLibrarySourceFile();
    assertEquals(getDartLibDom(), new DartLibraryImpl(libSrc));
  }

  public void test_DartLibraryImpl_equals_libDomNotLibExternal() throws Exception {
    if (getDartLibDom().equals(getDartLibExternal())) {
      fail();
    }
  }

  public void test_DartLibraryImpl_equals_libDomWebkit() throws Exception {
    // TODO (danrubel) uncomment to ensure webkit dom is bundled
    //LibrarySource libSrc = getDartLibDomWebkit().getLibrarySourceFile();
    //assertEquals(getDartLibDomWebkit(), new DartLibraryImpl(libSrc));
  }

  public void test_DartLibraryImpl_equals_libExternal() throws Exception {
    DartLibraryImpl lib = getDartLibExternal();
    File libDir = new File(getTempDir(), "lib5");
    assertEquals(lib, new DartLibraryImpl(new File(libDir, "lib5.dart")));
  }

  public void test_DartLibraryImpl_findType() {
    // TODO Implement this
  }

  public void test_DartLibraryImpl_getChildren_lib1() throws Exception {
    assertDartLib1Children(getDartLib1(), true);
//    getDartLib1().getDartProject().close();
//    try {
//      assertInfoCleared(getDartLib1(), true);
//      assertDartLib1Children(getDartLib1(), false);
//    } finally {
//      // force #getDartLib1() to reopen library
//      dartLib1 = null;
//      getDartLib1();
//    }
//    assertDartLib1Children(getDartLib1(), true);
  }

  public void test_DartLibraryImpl_getChildren_lib2() throws Exception {
    DartElement[] children = getDartLib2().getChildren();
    assertContainsCompUnit(children, "lib2.dart", false, false);
    assertEquals(1, children.length);
  }

  /**
   * Assert that lib3's info has been discarded when project is closed and again when it has been
   * reopened
   */
  public void test_DartLibraryImpl_getChildren_lib3() throws Exception {
    assertDartLib3Children(getDartLib3(), true);
    getDartLib3().getDartProject().close();
    try {
      assertInfoCleared(getDartLib3(), false);
      assertDartLib3Children(getDartLib3(), false);
    } finally {
      // force #getDartLib3() to reopen library
      dartLib3 = null;
      getDartLib3();
    }
    assertDartLib3Children(getDartLib3(), true);
  }

  /**
   * Assert that lib3's info has been discarded when project is closed and again when it has been
   * reopened
   */
  public void test_DartLibraryImpl_getChildren_lib3Alt() throws Exception {
    assertDartLib3Children(getDartLib3Alt(), true);
  }

  public void test_DartLibraryImpl_getChildren_libCore() throws Exception {
    DartElement[] children = getDartLibCore().getChildren();
    assertContainsCompUnit(children, "corelib.dart", false, false);
    assertContainsCompUnit(children, "object.dart", false, false);
    assertContainsCompUnit(children, "list.dart", false, false);
    assertTrue(children.length > 20);
  }

  public void test_DartLibraryImpl_getChildren_libCoreImpl() throws Exception {
    DartElement[] children = getDartLibCoreImpl().getChildren();
    assertContainsCompUnit(children, "corelib_impl.dart", false, false);
    assertContainsCompUnit(children, "regexp.dart", false, false);
    assertContainsCompUnit(children, "array.dart", false, false);
    assertTrue(children.length > 10);
  }

  public void test_DartLibraryImpl_getChildren_libDom() throws Exception {
    DartElement[] children = getDartLibDom().getChildren();
    assertContainsCompUnit(children, "dom.dart", false, false);
    assertContainsCompUnit(children, "Document.dart", false, false);
    assertTrue(children.length > 20);
  }

  public void test_DartLibraryImpl_getChildren_libEmpty() throws Exception {
    DartElement[] children = getDartLibEmpty().getChildren();
    assertContainsCompUnit(children, "empty.dart", false, false);
    assertEquals(1, children.length);
  }

  public void test_DartLibraryImpl_getChildren_libExternal() throws Exception {
    DartElement[] children = getDartLibExternal().getChildren();
    assertContainsCompUnit(children, "lib5.dart", false, false);
    assertContainsCompUnit(children, "ALib5MissingClass.dart", false, false);
    assertEquals(2, children.length);
  }

  public void test_DartLibraryImpl_getChildren_libHtml() throws Exception {
    DartElement[] children = getDartLibHtml().getChildren();
    assertContainsCompUnit(children, "html.dart", false, false);
    assertContainsCompUnit(children, "Text.dart", false, false);
    assertTrue(children.length > 20);
  }

  public void test_DartLibraryImpl_getCompilationUnits_lib1() throws Exception {
    assertEquals(2, getDartLib1().getCompilationUnits().length);
  }

  public void test_DartLibraryImpl_getCompilationUnits_lib2() throws Exception {
    assertEquals(1, getDartLib2().getCompilationUnits().length);
  }

  public void test_DartLibraryImpl_getCompilationUnits_libDom() throws Exception {
    assertTrue(getDartLibDom().getCompilationUnits().length > 20);
  }

  public void test_DartLibraryImpl_getCompilationUnits_libEmpty() throws Exception {
    assertEquals(1, getDartLibEmpty().getCompilationUnits().length);
  }

  public void test_DartLibraryImpl_getCompilationUnits_libExternal() throws Exception {
    assertEquals(2, getDartLibExternal().getCompilationUnits().length);
  }

  public void test_DartLibraryImpl_getCompilationUnits_libHtml() throws Exception {
    assertTrue(getDartLibHtml().getCompilationUnits().length > 20);
  }

  public void test_DartLibraryImpl_getCorrespondingResource() {
    // TODO Implement this
  }

  public void test_DartLibraryImpl_getDisplayName_html() throws Exception {
    assertEquals("dart:html", getDartLibHtml().getDisplayName());
  }

  public void test_DartLibraryImpl_getDisplayName_lib1() throws Exception {
    assertEquals("lib1", getDartLib1().getDisplayName());
  }

  public void test_DartLibraryImpl_getElementName_lib1() throws Exception {
    assertEquals("file:" + getTempDir().getAbsolutePath() + "/lib1/lib1.dart",
        getDartLib1().getElementName());
  }

  public void test_DartLibraryImpl_getElementName_lib2() throws Exception {
    assertEquals("file:" + getTempDir().getAbsolutePath() + "/lib2/lib2.dart",
        getDartLib2().getElementName());
  }

  public void test_DartLibraryImpl_getElementName_lib3() throws Exception {
    assertEquals("file:" + getTempDir().getAbsolutePath() + "/lib3/lib3.dart",
        getDartLib3().getElementName());
  }

  public void test_DartLibraryImpl_getElementName_libDom() throws Exception {
    assertEquals("dart:dom", getDartLibDom().getElementName());
  }

  public void test_DartLibraryImpl_getElementName_libEmpty() throws Exception {
    assertEquals("file:" + getTempDir().getAbsolutePath() + "/empty/empty.dart",
        getDartLibEmpty().getElementName());
  }

  public void test_DartLibraryImpl_getElementName_libExternal() throws Exception {
    assertEquals("file:" + getTempDir().getAbsolutePath() + "/lib5/lib5.dart",
        getDartLibExternal().getElementName());
  }

  public void test_DartLibraryImpl_getElementName_libHtml() throws Exception {
    assertEquals("dart:html", getDartLibHtml().getElementName());
  }

  public void test_DartLibraryImpl_getHandleMemento_lib1() throws Exception {
    assertHandleMemento(getDartLib1());
  }

  public void test_DartLibraryImpl_getHandleMemento_lib2() throws Exception {
    assertHandleMemento(getDartLib2());
  }

  public void test_DartLibraryImpl_getHandleMemento_lib3() throws Exception {
    assertHandleMemento(getDartLib3());
    getDartLib3().getDartProject().close();
    try {
      assertHandleMemento(getDartLib3());
    } finally {
      // force #getDartLib3() to reopen library
      dartLib3 = null;
      getDartLib3();
    }
    assertHandleMemento(getDartLib3());
  }

  public void test_DartLibraryImpl_getHandleMemento_lib3Alt() throws Exception {
    assertHandleMemento(getDartLib3Alt());
    getDartLib3().getDartProject().close();
    try {
      assertHandleMemento(getDartLib3Alt());
    } finally {
      // force #getDartLib3() to reopen library
      dartLib3 = null;
      getDartLib3();
    }
    assertHandleMemento(getDartLib3Alt());
  }

  public void test_DartLibraryImpl_getHandleMemento_libDom() throws Exception {
    assertHandleMemento(getDartLibDom());
  }

  public void test_DartLibraryImpl_getHandleMemento_libExternal() throws Exception {
    assertHandleMemento(getDartLibExternal());
  }

  public void test_DartLibraryImpl_getHandleMemento_libHtml() throws Exception {
    assertHandleMemento(getDartLibHtml());
  }

  public void test_DartLibraryImpl_getImportedLibraries_lib1() throws Exception {
    DartLibrary[] importedLibraries = getDartLib1().getImportedLibraries();
    assertContainsLibImpl(importedLibraries, "dart:dom");
    assertEquals(1, importedLibraries.length);
  }

  public void test_DartLibraryImpl_getImportedLibraries_lib2() throws Exception {
    assertDartLib2ImportedLibraries();
    getDartLib2().getDartProject().close();
    try {
      assertDartLib2ImportedLibraries();
    } finally {
      // force #getDartLib2() to reopen library
      dartLib2 = null;
      getDartLib2();
    }
    assertDartLib2ImportedLibraries();
  }

  public void test_DartLibraryImpl_getImportedLibraries_lib3() throws Exception {
    assertDartLib3ImportedLibraries();
    getDartLib3().getDartProject().close();
    try {
      assertDartLib3ImportedLibraries();
    } finally {
      // force #getDartLib3() to reopen library
      dartLib3 = null;
      getDartLib3();
    }
    assertDartLib3ImportedLibraries();
  }

  public void test_DartLibraryImpl_getImportedLibraries_libDom() throws Exception {
    DartLibrary[] importedLibraries = getDartLibDom().getImportedLibraries();
    assertEquals(0, importedLibraries.length);
  }

  public void test_DartLibraryImpl_getImportedLibraries_libEmpty() throws Exception {
    DartLibrary[] importedLibraries = getDartLibEmpty().getImportedLibraries();
    assertEquals(0, importedLibraries.length);
  }

  public void test_DartLibraryImpl_getImportedLibraries_libExternal() throws Exception {
    DartLibrary[] importedLibraries = getDartLibExternal().getImportedLibraries();
    assertContainsLibImpl(importedLibraries, "/empty/empty.dart");
    assertEquals(1, importedLibraries.length);
  }

  public void test_DartLibraryImpl_isTopLevel() throws Exception {
    assertFalse(getDartLibEmpty().isTopLevel());
  }

  public void test_DartLibraryImpl_isUnreferenced_imported() throws Exception {
    DartLibraryImpl library = getDartLib2();
    DartLibraryImpl importedLibrary = getDartLibEmpty();
    assertTrue(importedLibrary.isUnreferenced());
    library.setTopLevel(true);
    assertFalse(importedLibrary.isUnreferenced());
    library.setTopLevel(false);
    assertTrue(importedLibrary.isUnreferenced());
  }

  public void test_DartLibraryImpl_isUnreferenced_topLevel() throws Exception {
    DartLibraryImpl library = getDartLibEmpty();
    assertTrue(library.isUnreferenced());
    library.setTopLevel(true);
    assertFalse(library.isUnreferenced());
    library.setTopLevel(false);
    assertTrue(library.isUnreferenced());
  }

  public void test_DartLibraryImpl_newLibrarySourceFile() throws Exception {
    DartLibraryImpl library = getDartLibExternal();
    assertTrue(library.getLibrarySourceFile().getUri().toString().startsWith("file:/"));
  }

  public void test_DartLibraryImpl_nonExistantBundledLib() throws Exception {
    String memento = "= {file:/some/path/to/missing_bundled_lib.dart";
    DartLibraryImpl actual = (DartLibraryImpl) DartCore.create(memento);
    assertNotNull(actual);
  }

  public void test_DartLibraryImpl_nonExistantCompUnitAndBundledLib() throws Exception {
    String memento = "= {file:/some/path/to/missing_bundled_lib.dart!src/NonExistingClass.dart";
    ExternalCompilationUnitImpl actual = (ExternalCompilationUnitImpl) DartCore.create(memento);
    assertNotNull(actual);
  }

  public void test_DartLibraryImpl_setTopLevel() throws Exception {
    DartLibraryImpl library = getDartLibEmpty();
    library.setTopLevel(true);
    assertTrue(library.isTopLevel());
    library.setTopLevel(false);
    assertFalse(library.isTopLevel());
  }

  public void xtest_DartLibraryImpl_getImportedLibraries_libHtml() throws Exception {
    DartLibrary[] importedLibraries = getDartLibHtml().getImportedLibraries();
    assertEquals(1, importedLibraries.length);
    DartLibraryImpl hackLib = assertContainsLibImpl(importedLibraries, "dart:bootstrap_hacks");
    importedLibraries = hackLib.getImportedLibraries();
    assertEquals(1, importedLibraries.length);
    DartLibraryImpl domLib = assertContainsLibImpl(importedLibraries, "dart:dom");
    assertEquals(getDartLibDom(), domLib);
  }

  @Override
  protected void setUp() throws Exception {
    // Prevent indexer from interfering with the tests
    StandardDriver.shutdownForTests();
  }

  private CompilationUnitImpl assertContainsCompUnit(DartElement[] elements, String elemPath,
      boolean inWorkspace, boolean exists) throws DartModelException {
    for (DartElement elem : elements) {
      if ((elem instanceof CompilationUnitImpl) && elem.getElementName().endsWith(elemPath)) {
        CompilationUnitImpl unit = (CompilationUnitImpl) elem;
        assertHandleMemento(unit);

        if (!exists) {
          return unit;
        }

        // This call causes info to be cached
        String source = unit.getSource();
        unit.getElementInfo();
        assertInfoCached(unit);

        assertNotNull(source);
        assertTrue(source.length() > 10);
        if (inWorkspace) {
          assertNotNull(unit.getResource());
        } else {
          assertNull(unit.getResource());
        }
        return unit;
      }
    }
    failContains(elements, CompilationUnitImpl.class, elemPath);
    return null;
  }

  private DartLibraryImpl assertContainsLibImpl(DartElement[] elements, String elemPath) {
    String suffix = getClass().getSimpleName() + elemPath;
    for (DartElement elem : elements) {
      if (elem instanceof DartLibraryImpl) {
        String libName = elem.getElementName();
        DartLibraryImpl lib = (DartLibraryImpl) elem;
        LibrarySource libSrc = lib.getLibrarySourceFile();
        if (libSrc instanceof UrlLibrarySource) {
          if (libName.endsWith(elemPath)) {
            return (DartLibraryImpl) elem;
          }
        } else if (libName.endsWith(suffix)) {
          return (DartLibraryImpl) elem;
        }
      }
    }
    failContains(elements, DartLibraryImpl.class, suffix);
    return null;
  }

  private void assertDartLib1Children(DartLibraryImpl lib, boolean inWorkspace) throws Exception {
    DartElement[] children = lib.getChildren();
    assertContainsCompUnit(children, "lib1.dart", inWorkspace, true);
    assertContainsCompUnit(children, "SomeClass.dart", inWorkspace, true);
    assertEquals(2, children.length);
  }

  private void assertDartLib2ImportedLibraries() throws Exception {
    DartLibrary[] importedLibraries = getDartLib2().getImportedLibraries();
    assertContainsLibImpl(importedLibraries, "/lib1/lib1.dart");
    assertContainsLibImpl(importedLibraries, "/lib3/lib3.dart");
    assertContainsLibImpl(importedLibraries, "/lib5/lib5.dart");
    assertContainsLibImpl(importedLibraries, "/empty/empty.dart");
    assertEquals(4, importedLibraries.length);
  }

  private void assertDartLib3Children(DartLibraryImpl lib, boolean inWorkspace)
      throws DartModelException {

    // This call should cache the library info
    DartElement[] children = lib.getChildren();
    assertInfoCached(lib);

    assertContainsCompUnit(children, "lib3.dart", inWorkspace, false);
    assertContainsCompUnit(children, "AnotherMissingClass.dart", inWorkspace, false);
    assertEquals(2, children.length);
  }

  private void assertDartLib3ImportedLibraries() throws Exception {
    DartLibrary[] importedLibraries = getDartLib3().getImportedLibraries();
    assertContainsLibImpl(importedLibraries, "/empty/empty.dart");
    assertEquals(1, importedLibraries.length);
  }

  /**
   * Assert that a handle memento can be generated from the specified "expected" library and that
   * Dart element recreated from the handle memento equals the "expected" library.
   */
  private void assertHandleMemento(DartElementImpl expected) {
    String memento = expected.getHandleMemento();
    assertNotNull(memento);
    DartElement actual = DartCore.create(memento);
    assertEquals(expected, actual);
  }

  /**
   * Record the specified element as having info cached.
   */
  private void assertInfoCached(DartElement elem) {
    DartModelManager manager = DartModelManager.getInstance();
    if (manager.getInfo(elem) == null) {
      fail("Expected info to be cached for " + elem);
    }
    DartElement parent = elem.getParent();
    if (parent instanceof DartLibraryImpl) {
      DartLibraryImpl lib = (DartLibraryImpl) parent;
      Collection<DartElement> children = libraryChildrenWithCachedInfos.get(lib);
      if (children == null) {
        children = new HashSet<DartElement>();
        libraryChildrenWithCachedInfos.put(lib, children);
      }
      children.add(elem);
    }
  }

  /**
   * Assert that the info for the specified library and all children of that library have been
   * cleared.
   * 
   * @param lib the library (not <code>null</code>)
   */
  private void assertInfoCleared(DartLibraryImpl lib, boolean expectChildInfo) {
    DartModelManager manager = DartModelManager.getInstance();
    if (manager.getInfo(lib) != null) {
      fail("Expected info to be cleared for " + lib);
    }
    Collection<DartElement> children = libraryChildrenWithCachedInfos.remove(lib);
    if (expectChildInfo) {
      if (children == null) {
        fail("No child infos were cached");
      }
      for (DartElement child : children) {
        if (child instanceof CompilationUnitImpl) {
          if (manager.getInfo(child) != null) {
            fail("Expected info to be cleared for " + child);
          }
        }
      }
    }
  }

  private void failContains(DartElement[] elements, Class<?> elemClass, String elemPath) {
    StringBuilder message = new StringBuilder(1000);
    message.append("Failed to find instance of \n" + elemClass + " with path " + elemPath + " in [");
    for (DartElement elem : elements) {
      message.append("\n  ");
      if (elem == null) {
        message.append("null");
      } else {
        message.append(elem.getClass().getName() + " named " + elem.getElementName());
      }
    }
    message.append("\n]");
    fail(message.toString());
  }

  private DartLibraryImpl getBundledLib(String bundledLibName) throws Exception {
    SystemLibraryManager libMgr = SystemLibraryManagerProvider.getSystemLibraryManager();
    URI libUri = new URI(bundledLibName);
    return new DartLibraryImpl(new UrlLibrarySource(libUri, libMgr));
  }

  private DartLibraryImpl getDartLib1() throws Exception {
    if (dartLib1 == null) {
      dartLib1 = getOrCreateDartLib("lib1", new DartLibrary[] {getDartLibDom()}, "SomeClass", true);
    }
    return dartLib1;
  }

  private DartLibraryImpl getDartLib2() throws Exception {
    if (dartLib2 == null) {
      dartLib2 = getOrCreateDartLib("lib2", new DartLibrary[] {
          getDartLibEmpty(), getDartLib1(), getDartLib3(), getDartLibExternal()}, null, false);
    }
    return dartLib2;
  }

  private DartLibraryImpl getDartLib3() throws Exception {
    if (dartLib3 == null) {
      dartLib3 = getOrCreateDartLib("lib3", new DartLibrary[] {getDartLibEmpty()},
          "AnotherMissingClass", false);
    }
    return dartLib3;
  }

  private DartLibraryImpl getDartLib3Alt() throws Exception {
    if (dartLib3Alt == null) {
      IProject proj = getDartLib3().getResource().getProject();
      File libFile = proj.getFile("lib3.dart").getLocation().toFile();
      dartLib3Alt = new DartLibraryImpl(libFile);
    }
    return dartLib3Alt;
  }

  private DartLibraryImpl getDartLibCore() throws Exception {
    return getBundledLib("dart:core");
  }

  private DartLibraryImpl getDartLibCoreImpl() throws Exception {
    return (DartLibraryImpl) getDartLibCore().getImportedLibraries()[0];
  }

  private DartLibraryImpl getDartLibDom() throws Exception {
    return getBundledLib("dart:dom");
  }

  private DartLibraryImpl getDartLibEmpty() throws Exception {
    if (dartLibEmpty == null) {
      dartLibEmpty = getOrCreateDartLib("empty", null, null, false);
    }
    return dartLibEmpty;
  }

  private DartLibraryImpl getDartLibExternal() throws Exception {
    if (dartLibExternal == null) {
      File libFile = getOrCreateLibFile("lib5", new DartLibrary[] {getDartLibEmpty()},
          "ALib5MissingClass", false);
      dartLibExternal = new DartLibraryImpl(libFile);
    }
    return dartLibExternal;
  }

  private DartLibraryImpl getDartLibHtml() throws Exception {
    return getBundledLib("dart:html");
  }

  private DartModelImpl getDartModel() {
    return DartModelManager.getInstance().getDartModel();
  }

  private DartLibraryImpl getOrCreateDartLib(String libName, DartLibrary[] importLibs,
      String className, boolean createClass) throws Exception {
    File libFile = getOrCreateLibFile(libName, importLibs, className, createClass);
    return (DartLibraryImpl) DartCore.openLibrary(libFile, new NullProgressMonitor());
  }

  private File getOrCreateLibFile(String libName, DartLibrary[] importLibs, String className,
      boolean createClass) throws IOException {
    File libDir = new File(getTempDir(), libName);
    File libFile = new File(libDir, libName + ".dart");
    if (!libFile.exists()) {
      List<File> importFiles = new ArrayList<File>();
      if (importLibs != null) {
        for (DartLibrary lib : importLibs) {
          String libPath = lib.getElementName();
          if (libPath.startsWith("file:")) {
            libPath = libPath.substring(5);
          }
          importFiles.add(new File(libPath));
        }
      }
      List<File> sourceFiles = new ArrayList<File>();
      if (className != null) {
        sourceFiles.add(new File(libDir, className + ".dart"));
      }
      final String content = DefaultLibrarySource.generateSource(libName, libFile, importFiles,
          sourceFiles, null);
      makeTempDir(libDir);
      createTempFile(libFile, content);
      if (createClass) {
        File classFile = new File(libDir, className + ".dart");
        String classContent = "class " + className + " { }";
        createTempFile(classFile, classContent);
      }
    }
    return libFile;
  }
}
