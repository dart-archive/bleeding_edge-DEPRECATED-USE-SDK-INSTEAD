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

package com.google.dart.engine.services.refactoring;

import com.google.dart.engine.services.internal.correction.AbstractDartTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SubProgressMonitorTest extends AbstractDartTest {
  private final ProgressMonitor parent = mock(ProgressMonitor.class);

  public void test_beginTask_done() throws Exception {
    SubProgressMonitor monitor = new SubProgressMonitor(parent, 10);
    monitor.beginTask("sub", 5);
    // no interactions yet
    verifyNoMoreInteractions(parent);
    // done - report all other ticks
    monitor.done();
    verify(parent).internalWorked(10.0);
    verifyNoMoreInteractions(parent);
  }

  public void test_beginTask_nested() throws Exception {
    SubProgressMonitor monitor = new SubProgressMonitor(parent, 10);
    monitor.beginTask("sub", 5);
    // nested is ignored, no interactions
    monitor.beginTask("sub-2", 6);
    monitor.worked(4);
    verifyNoMoreInteractions(parent);
    // done nested
    monitor.done();
  }

  public void test_isCancelled() throws Exception {
    SubProgressMonitor monitor = new SubProgressMonitor(parent, 10);
    // not cancelled
    assertFalse(monitor.isCanceled());
    // "cancel" parent
    when(parent.isCanceled()).thenReturn(true);
    assertTrue(monitor.isCanceled());
  }

  public void test_setCancelled() throws Exception {
    SubProgressMonitor monitor = new SubProgressMonitor(parent, 10);
    // do cancel
    monitor.setCanceled();
    verify(parent).setCanceled();
  }

  public void test_subTask() throws Exception {
    SubProgressMonitor monitor = new SubProgressMonitor(parent, 10);
    // begin
    monitor.beginTask("Main task", 10);
    monitor.subTask("sub task");
    verify(parent).subTask("sub task");
    // done, reset sub-task
    monitor.done();
    verify(parent).subTask("");
  }

  public void test_worked() throws Exception {
    SubProgressMonitor monitor = new SubProgressMonitor(parent, 10);
    monitor.beginTask("sub", 5);
    // worked
    monitor.worked(4);
    verify(parent).internalWorked(8.0);
    verifyNoMoreInteractions(parent);
    // done - report all other ticks
    monitor.done();
    verify(parent).internalWorked(2.0);
    verifyNoMoreInteractions(parent);
  }

  public void test_worked_endUp() throws Exception {
    SubProgressMonitor monitor = new SubProgressMonitor(parent, 10);
    monitor.beginTask("sub", 5);
    // work up all ticks
    monitor.worked(4);
    monitor.worked(1);
    verify(parent).internalWorked(8.0);
    verify(parent).internalWorked(2.0);
    // no more ticks
    reset(parent);
    monitor.worked(1);
    verifyNoMoreInteractions(parent);
  }
}
