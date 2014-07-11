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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartIgnoreEvent;
import com.google.dart.tools.core.model.DartIgnoreListener;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class DartIgnoreManagerTest extends TestCase {

  private final class CountingMockIgnoreFile extends DartIgnoreFile {
    private boolean loaded = false;
    private int storeCount = 0;

    private CountingMockIgnoreFile(File file) {
      super(file);
    }

    public void assertStore(int expected) {
      assertEquals(expected, storeCount);
    }

    @Override
    public void initFile() throws IOException {
      // do not write to disk
    }

    @Override
    public DartIgnoreFile load() throws IOException {
      if (loaded) {
        fail("should only call load once");
      }
      loaded = true;
      for (String pattern : initialContent) {
        add(pattern);
      }
      return this;
    }

    @Override
    public DartIgnoreFile store() throws IOException {
      // do not write to disk
      storeCount++;
      return storage;
    }
  }

  private static final File FILE = new File("/my/project/src/does-not-exist");
  private static final String FILE_PATH = FILE.getAbsolutePath();
  private static final String NORMALIZED_PATH = FILE_PATH.replace(File.separatorChar, '/');

  private CountingMockIgnoreFile storage;
  private DartIgnoreManager manager;
  private DartIgnoreListener listener;
  private String[] initialContent = new String[] {};

  public void test_addToIgnores_File() throws Exception {
    assertTrue(manager.addToIgnores(FILE));
    verifyAdd(NORMALIZED_PATH);
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
    assertFalse(manager.addToIgnores(FILE));
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
  }

  public void test_addToIgnores_File_null() throws Exception {
    assertFalse(manager.addToIgnores((File) null));
    assertTrue(storage.getPatterns().isEmpty());
    verify(listener, times(0)).ignoresChanged(any(DartIgnoreEvent.class));
  }

  public void test_addToIgnores_IResource() throws Exception {
    final IResource res = mockResource(FILE_PATH);
    assertTrue(manager.addToIgnores(res));
    String path = res.getLocation().toPortableString();
    verifyAdd(path);
    verify(res).deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
    assertFalse(manager.addToIgnores(res));
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
  }

  public void test_addToIgnores_IResource_null() throws Exception {
    assertFalse(manager.addToIgnores((IResource) null));
    assertEquals(0, storage.getPatterns().size());
    storage.assertStore(0);
    verify(listener, times(0)).ignoresChanged(new DartIgnoreEvent(new String[] {}, new String[] {}));
  }

  public void test_addToIgnores_IResource_null_location() throws Exception {
    final IResource res = mockResource(null);
    assertFalse(manager.addToIgnores(res));
    assertEquals(0, storage.getPatterns().size());
    storage.assertStore(0);
    verify(listener, times(0)).ignoresChanged(new DartIgnoreEvent(new String[] {}, new String[] {}));
  }

  public void test_addToIgnores_String() throws Exception {
    assertTrue(manager.addToIgnores(NORMALIZED_PATH));
    verifyAdd(NORMALIZED_PATH);
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
    assertFalse(manager.addToIgnores(NORMALIZED_PATH));
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
  }

  public void test_addToIgnores_String_null() throws Exception {
    assertFalse(manager.addToIgnores((String) null));
    assertTrue(storage.getPatterns().isEmpty());
    verify(listener, times(0)).ignoresChanged(new DartIgnoreEvent(new String[] {}, new String[] {}));
  }

  public void test_default_ignores() throws Exception {
    final String[] paths = {"/some/build", // pub build directory
        "/some/out.js.info.html" // dart2js info file
    };
    final String buildFile = "/some/build/file/path";

    // Assert specific files/directories are not analyzed by default
    assertTrue(
        "Expect " + NORMALIZED_PATH + " to be analyzed by default",
        manager.isAnalyzed(NORMALIZED_PATH));
    for (String eachPath : paths) {
      assertFalse("Expect " + eachPath + " to be ignored by default", manager.isAnalyzed(eachPath));
    }
    assertFalse("Expect " + buildFile + " to be ignored by default", manager.isAnalyzed(buildFile));
    assertTrue(manager.isAnalyzed("/some/build0"));
    assertTrue(manager.isAnalyzed("/some/build0/foo"));
    assertTrue(manager.isAnalyzed("/some/abuild"));
    assertTrue(manager.isAnalyzed("/some/abuild/foo"));

    //
    // Ideally files that are ignored by default can be analyzed if the user so chooses.
    // This commented out code tests that ideal situation.
    //

//    // Assert that they can be analyzed if marked as such
//    for (String eachPath : paths) {
//      assertTrue("Expect removed: " + eachPath, manager.removeFromIgnores(eachPath));
//    }
//    assertTrue(manager.isAnalyzed(NORMALIZED_PATH));
//    for (String eachPath : paths) {
//      assertTrue("Expect " + eachPath + " to be analyzed", manager.isAnalyzed(eachPath));
//    }
//    assertTrue("Expect " + buildFile + " to be analyzed", manager.isAnalyzed(buildFile));
//
//    // Assert that they can be ignored if marked as such
//    for (String eachPath : paths) {
//      assertTrue("Expect added: " + eachPath, manager.addToIgnores(eachPath));
//    }
//    assertTrue(manager.isAnalyzed(NORMALIZED_PATH));
//    for (String eachPath : paths) {
//      assertFalse("Expect " + eachPath + " to be ignored", manager.isAnalyzed(eachPath));
//    }
//    assertFalse("Expect " + buildFile + " to be ignored", manager.isAnalyzed(buildFile));
  }

  public void test_default_ignores_load_legacy() throws Exception {
    // build directory that was (legacy) explicitly excluded but is now implicitly excluded
    final String buildDir = "/some/build";
    initialContent = new String[] {buildDir};
    assertFalse(manager.isAnalyzed(buildDir));

    //
    // Ideally files that are ignored by default can be analyzed if the user so chooses.
    // This commented out code tests that ideal situation.
    //

//    assertTrue(manager.removeFromIgnores(buildDir));
//    assertTrue(manager.isAnalyzed(buildDir));
//    assertTrue(manager.addToIgnores(buildDir));
//    assertFalse(manager.isAnalyzed(buildDir));
  }

  public void test_isAnalyzed_File() throws Exception {
    File file = mock(File.class);
    when(file.exists()).thenReturn(true);
    when(file.getAbsolutePath()).thenReturn(FILE_PATH);
    assertTrue(manager.isAnalyzed(file));
    manager.addToIgnores(file);
    assertFalse(manager.isAnalyzed(file));
  }

  public void test_isAnalyzed_File_does_not_exist() throws Exception {
    File file = mock(File.class);
    when(file.exists()).thenReturn(false);
    when(file.getAbsolutePath()).thenReturn(FILE_PATH);
    assertFalse(manager.isAnalyzed(file));
  }

  public void test_isAnalyzed_File_null() throws Exception {
    assertFalse(manager.isAnalyzed((File) null));
  }

  public void test_isAnalyzed_IResource() throws Exception {
    final IResource res = mockResource(FILE_PATH);
    when(res.exists()).thenReturn(true);
    assertTrue(manager.isAnalyzed(res));
    manager.addToIgnores(res);
    assertFalse(manager.isAnalyzed(res));
  }

  public void test_isAnalyzed_IResource_does_not_exist() throws Exception {
    final IResource res = mockResource(FILE_PATH);
    when(res.exists()).thenReturn(false);
    assertFalse(manager.isAnalyzed(res));
  }

  public void test_isAnalyzed_IResource_null() throws Exception {
    assertFalse(manager.isAnalyzed((IResource) null));
  }

  public void test_isAnalyzed_IResource_null_location() throws Exception {
    final IResource res = mockResource(null);
    when(res.exists()).thenReturn(true);
    assertFalse(manager.isAnalyzed(res));
  }

  public void test_isAnalyzed_Path() throws Exception {
    IPath path = new Path(FILE.getAbsolutePath());
    assertTrue(manager.isAnalyzed(path));
    manager.addToIgnores(path);
    assertFalse(manager.isAnalyzed(path));
  }

  public void test_isAnalyzed_Path_null() throws Exception {
    assertFalse(manager.isAnalyzed((IPath) null));
  }

  public void test_isAnalyzed_String() throws Exception {
    assertTrue(manager.isAnalyzed(NORMALIZED_PATH));
    manager.addToIgnores(NORMALIZED_PATH);
    assertFalse(manager.isAnalyzed(NORMALIZED_PATH));
  }

  public void test_isAnalyzed_String_null() throws Exception {
    assertFalse(manager.isAnalyzed((String) null));
  }

  public void test_isIgnored_File() throws Exception {
    assertFalse(manager.isIgnored(FILE));
    manager.addToIgnores(FILE);
    assertTrue(manager.isIgnored(FILE));
  }

  public void test_isIgnored_File_null() throws Exception {
    assertFalse(manager.isIgnored((File) null));
  }

  public void test_isIgnored_IResource() throws Exception {
    final IResource res = mockResource(FILE_PATH);
    assertFalse(manager.isIgnored(res));
    manager.addToIgnores(res);
    assertTrue(manager.isIgnored(res));
  }

  public void test_isIgnored_IResource_null() throws Exception {
    assertFalse(manager.isIgnored((IResource) null));
  }

  public void test_isIgnored_Path() throws Exception {
    IPath path = new Path(FILE.getAbsolutePath());
    assertFalse(manager.isIgnored(path));
    manager.addToIgnores(path);
    assertTrue(manager.isIgnored(path));
  }

  public void test_isIgnored_Path_null() throws Exception {
    assertFalse(manager.isIgnored((IPath) null));
  }

  public void test_isIgnored_String() throws Exception {
    assertFalse(manager.isIgnored(NORMALIZED_PATH));
    manager.addToIgnores(NORMALIZED_PATH);
    assertTrue(manager.isIgnored(NORMALIZED_PATH));
  }

  public void test_isIgnored_String_null() throws Exception {
    assertFalse(manager.isIgnored((String) null));
  }

  public void test_removeFromIgnores_File() throws Exception {
    initialContent = new String[] {NORMALIZED_PATH};
    assertEquals(Arrays.asList(initialContent), manager.getExclusionPatterns());
    assertTrue(manager.removeFromIgnores(FILE));
    assertEquals(0, manager.getExclusionPatterns().size());
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
    assertFalse(manager.removeFromIgnores(FILE));
    assertEquals(0, manager.getExclusionPatterns().size());
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
  }

  public void test_removeFromIgnores_File_null() throws Exception {
    initialContent = new String[] {NORMALIZED_PATH};
    assertFalse(manager.removeFromIgnores((File) null));
    assertEquals(Arrays.asList(initialContent), manager.getExclusionPatterns());
    verify(listener, times(0)).ignoresChanged(any(DartIgnoreEvent.class));
  }

  public void test_removeFromIgnores_IPath() throws Exception {
    initialContent = new String[] {NORMALIZED_PATH};
    assertEquals(Arrays.asList(initialContent), manager.getExclusionPatterns());
    assertTrue(manager.removeFromIgnores(new Path(FILE.getAbsolutePath())));
    assertEquals(0, manager.getExclusionPatterns().size());
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
    assertFalse(manager.removeFromIgnores(FILE));
    assertEquals(0, manager.getExclusionPatterns().size());
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
  }

  public void test_removeFromIgnores_IPath_null() throws Exception {
    initialContent = new String[] {NORMALIZED_PATH};
    assertFalse(manager.removeFromIgnores((IPath) null));
    assertEquals(Arrays.asList(initialContent), manager.getExclusionPatterns());
    verify(listener, times(0)).ignoresChanged(any(DartIgnoreEvent.class));
  }

  public void test_removeFromIgnores_IResource() throws Exception {
    initialContent = new String[] {NORMALIZED_PATH};
    final IResource res = mockResource(FILE_PATH);
    assertEquals(Arrays.asList(initialContent), manager.getExclusionPatterns());
    assertTrue(manager.removeFromIgnores(res));
    assertEquals(0, manager.getExclusionPatterns().size());
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
    assertFalse(manager.removeFromIgnores(res));
    assertEquals(0, manager.getExclusionPatterns().size());
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
  }

  public void test_removeFromIgnores_IResource_null() throws Exception {
    initialContent = new String[] {NORMALIZED_PATH};
    assertFalse(manager.removeFromIgnores((IResource) null));
    assertEquals(Arrays.asList(initialContent), manager.getExclusionPatterns());
    verify(listener, times(0)).ignoresChanged(any(DartIgnoreEvent.class));
  }

  public void test_removeFromIgnores_IResource_null_location() throws Exception {
    initialContent = new String[] {NORMALIZED_PATH};
    final IResource res = mockResource(null);
    assertFalse(manager.removeFromIgnores(res));
    verify(listener, times(0)).ignoresChanged(any(DartIgnoreEvent.class));
  }

  public void test_removeFromIgnores_String() throws Exception {
    initialContent = new String[] {NORMALIZED_PATH};
    assertEquals(Arrays.asList(initialContent), manager.getExclusionPatterns());
    assertTrue(manager.removeFromIgnores(NORMALIZED_PATH));
    assertEquals(0, manager.getExclusionPatterns().size());
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
    assertFalse(manager.removeFromIgnores(FILE));
    assertEquals(0, manager.getExclusionPatterns().size());
    verify(listener, times(1)).ignoresChanged(any(DartIgnoreEvent.class));
    storage.assertStore(1);
  }

  public void test_removeFromIgnores_String_null() throws Exception {
    initialContent = new String[] {NORMALIZED_PATH};
    assertFalse(manager.removeFromIgnores((String) null));
    assertEquals(Arrays.asList(initialContent), manager.getExclusionPatterns());
    verify(listener, times(0)).ignoresChanged(any(DartIgnoreEvent.class));
  }

  @Override
  protected void setUp() throws Exception {
    storage = new CountingMockIgnoreFile(new File("ignore-test"));
    manager = new DartIgnoreManager(storage);
    listener = mock(DartIgnoreListener.class);
    manager.addListener(listener);
  }

  private IResource mockResource(String absPath) {
    final IResource res = mock(IResource.class);
    when(res.getLocation()).thenReturn(absPath != null ? new Path(absPath) : null);
    return res;
  }

  private void verifyAdd(final String normalizedPath) throws IOException {
    assertTrue(storage.getPatterns().contains(normalizedPath));
  }
}
