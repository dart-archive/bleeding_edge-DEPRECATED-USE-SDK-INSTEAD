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

package com.google.dart.tools.debug.core.server;

import com.google.dart.tools.core.test.util.PlainTestProject;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IThread;

public class ServerDebuggerTest extends ServerTestCase {
  protected IFile testFile;
  protected IFile libFile;
  protected IFile packageFile;
  protected IFile libEntryFile;
  protected PlainTestProject project;
  protected VMDebugger vm;

  /**
   * Test that a breakpoint in the 'lib' folder works.
   */
  public void testBreakpointLibFolder() throws Exception {
    BreakpointLatch latch = new BreakpointLatch(1);
    createBreakpoint(libFile, 2);
    latch.await();

    vm.connect(libEntryFile.getLocation().toFile().getAbsolutePath());

    assertNotNull(vm.getDebugTarget());

    assertEquals("1", vm.readLine());
    assertPaused();
    resume();
    assertEquals("my_lib", vm.readLine());
    assertEquals("other_lib", vm.readLine());
    assertEquals("2", vm.readLine());

    vm.waitForExit(3000);
  }

  public void testBreakpointPackageFolder() throws Exception {
    BreakpointLatch latch = new BreakpointLatch(1);
    createBreakpoint(packageFile, 2);
    latch.await();

    vm.connect(libEntryFile.getLocation().toFile().getAbsolutePath());

    assertNotNull(vm.getDebugTarget());

    assertEquals("1", vm.readLine());
    assertPaused();
    resume();
    assertEquals("my_lib", vm.readLine());
    assertEquals("other_lib", vm.readLine());
    assertEquals("2", vm.readLine());

    vm.waitForExit(3000);
  }

  public void testBreakpointSimple() throws Exception {
    BreakpointLatch latch = new BreakpointLatch(1);
    createBreakpoint(testFile, 3);
    createBreakpoint(testFile, 4);
    latch.await();

    vm.connect(testFile.getLocation().toFile().getAbsolutePath());

    assertNotNull(vm.getDebugTarget());

    assertEquals("1", vm.readLine());
    assertPaused();
    resume();
    assertEquals("2", vm.readLine());
    assertPaused();
    resume();
    assertEquals("3", vm.readLine());

    vm.waitForExit(3000);
  }

  protected void assertPaused() throws DebugException {
    long start = System.currentTimeMillis();

    while (System.currentTimeMillis() - start < 1000) {
      IThread thread = vm.getDebugTarget().getThreads()[0];

      if (thread.isSuspended()) {
        return;
      }

      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {

      }
    }

    assertTrue(vm.getDebugTarget() + " not paused", false);
  }

  protected void createBreakpoint(final IFile file, final int line) throws CoreException,
      InterruptedException {
    DartBreakpoint bp = new DartBreakpoint(file, line);

    DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(bp);
  }

  protected PlainTestProject createTestProject() throws Exception {
    final String SCRIPT_PATH = "/data/scripts/";

    PlainTestProject project = new PlainTestProject("dbgTest");

    // create a pubspec
    project.setFileContent(
        "pubspec.yaml",
        this.getClass().getResourceAsStream(SCRIPT_PATH + "pubspec.yaml"));

    // create 'lib'
    project.createFolder("lib");
    libFile = createFile(project, "lib", "my_lib.dart");

    // create 'bin'
    project.createFolder("bin");
    testFile = createFile(project, "bin", "test.dart");
    libEntryFile = createFile(project, "bin", "test_lib.dart");

    // create synthetic packages folders
    project.createFolder("packages/foo");
    createFile(project, "packages/foo", "my_lib.dart");

    project.createFolder("packages/other");
    packageFile = createFile(project, "packages/other", "other_lib.dart");

    project.createFolder("bin/packages/foo");
    createFile(project, "bin/packages/foo", "my_lib.dart");

    project.createFolder("bin/packages/other");
    createFile(project, "bin/packages/other", "other_lib.dart");

    TestProject.waitForAutoBuild();

    return project;
  }

  protected void resume() throws DebugException {
    vm.getDebugTarget().getThreads()[0].resume();
  }

  @Override
  protected void setUp() throws Exception {
    project = createTestProject();
    vm = new VMDebugger();
  }

  @Override
  protected void tearDown() throws Exception {
    if (vm != null) {
      vm.dispose();
    }

    if (project != null) {
      project.dispose();
    }
  }

  private IFile createFile(PlainTestProject project, String parentPath, String fileName)
      throws Exception {
    return project.setFileContent(
        parentPath + "/" + fileName,
        this.getClass().getResourceAsStream("/data/scripts/" + fileName));
  }

}
