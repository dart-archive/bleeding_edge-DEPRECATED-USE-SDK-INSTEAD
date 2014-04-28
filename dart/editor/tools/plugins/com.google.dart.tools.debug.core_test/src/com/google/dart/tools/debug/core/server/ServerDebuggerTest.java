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
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

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

  public void testVerifyStackConditions() throws Exception {
    BreakpointLatch latch = new BreakpointLatch(1);
    createBreakpoint(testFile, 20);
    latch.await();

    vm.connect(testFile.getLocation().toFile().getAbsolutePath());

    assertNotNull(vm.getDebugTarget());

    assertEquals("1", vm.readLine());
    assertEquals("2", vm.readLine());
    assertEquals("3", vm.readLine());
    assertPaused();

    // assert stack conditions
    IStackFrame frame = getTopStackFrame();
    assertHasLocal(frame, "this");
    assertHasLocal(frame, "localVar");

    // assert that there is a frameId, and the top frame ID is 0
    ServerDebugStackFrame serverFrame = (ServerDebugStackFrame) frame;
    assertEquals(0, serverFrame.getVmFrame().getFrameId());

    // assert that expression evaluation works
    assertExpressionEval(serverFrame, "globalVar", "1");
    assertExpressionEval(serverFrame, "staticVar", "2");
    assertExpressionEval(serverFrame, "instanceVar", "3");
    assertExpressionEval(serverFrame, "localVar", "4");

    resume();

    vm.waitForExit(3000);
  }

  @Override
  protected void assertPaused() throws DebugException {
    long start = System.currentTimeMillis();

    while (System.currentTimeMillis() - start < 1000) {
      IThread thread = getCurrentThread();

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

  @Override
  protected void createBreakpoint(final IFile file, final int line) throws CoreException,
      InterruptedException {
    DartBreakpoint bp = new DartBreakpoint(file, line);

    DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(bp);
  }

  @Override
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

  @Override
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

  private void assertExpressionEval(ServerDebugStackFrame frame, String expression, String expected)
      throws InterruptedException, DebugException {
    ExpressionListenerLatch latch = new ExpressionListenerLatch();
    frame.evaluateExpression(expression, latch);
    latch.await();

    if (latch.getResult().hasErrors()) {
      assertFalse(
          "error evaluating expression: " + latch.getResult().getErrorMessages()[0],
          latch.getResult().hasErrors());
    }

    assertEquals(expected, latch.getResult().getValue().getValueString());
  }

  private void assertHasLocal(IStackFrame frame, String variableName) throws DebugException {
    IVariable[] vars = frame.getVariables();

    for (IVariable variable : vars) {
      if (variable.getName().equals(variableName)) {
        return;
      }
    }

    fail("the current frame does not contain the variable: " + variableName);
  }

  private IFile createFile(PlainTestProject project, String parentPath, String fileName)
      throws Exception {
    return project.setFileContent(
        parentPath + "/" + fileName,
        this.getClass().getResourceAsStream("/data/scripts/" + fileName));
  }

  private IThread getCurrentThread() throws DebugException {
    return vm.getDebugTarget().getThreads()[0];
  }

  private IStackFrame getTopStackFrame() throws DebugException {
    return getCurrentThread().getTopStackFrame();
  }

}
