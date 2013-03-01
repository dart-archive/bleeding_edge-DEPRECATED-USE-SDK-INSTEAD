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
import com.google.dart.engine.services.internal.refactoring.RefactoringImpl;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusEntry;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

/**
 * Test for {@link RefactoringImpl}.
 */
public class RefactoringImplTest extends AbstractDartTest {
  private final ProgressMonitor pm = mock(ProgressMonitor.class);

  public void test_checkAllConditions_checkInitial_checkFinal() throws Exception {
    Refactoring refactoring = mock(RefactoringImpl.class);
    when(refactoring.checkAllConditions(any(ProgressMonitor.class))).thenCallRealMethod();
    // initial 
    RefactoringStatus initialStatus = new RefactoringStatus();
    initialStatus.addWarning("warn-1");
    when(refactoring.checkInitialConditions(any(ProgressMonitor.class))).thenReturn(initialStatus);
    // initial 
    RefactoringStatus finalStatus = new RefactoringStatus();
    finalStatus.addWarning("warn-2");
    when(refactoring.checkFinalConditions(any(ProgressMonitor.class))).thenReturn(finalStatus);
    // check all conditions
    RefactoringStatus result = refactoring.checkAllConditions(pm);
    assertTrue(result.hasWarning());
    List<RefactoringStatusEntry> entries = result.getEntries();
    assertThat(entries).hasSize(2);
    assertEquals("warn-1", entries.get(0).getMessage());
    assertEquals("warn-2", entries.get(1).getMessage());
  }

  public void test_checkAllConditions_fatalInitial() throws Exception {
    Refactoring refactoring = mock(RefactoringImpl.class);
    when(refactoring.checkAllConditions(any(ProgressMonitor.class))).thenCallRealMethod();
    // initial 
    RefactoringStatus initialStatus = new RefactoringStatus();
    initialStatus.addFatalError("fatal-msg");
    when(refactoring.checkInitialConditions(any(ProgressMonitor.class))).thenReturn(initialStatus);
    // check all conditions
    RefactoringStatus result = refactoring.checkAllConditions(pm);
    assertTrue(result.hasFatalError());
    assertEquals("fatal-msg", result.getMessage());
    verify(refactoring, times(0)).checkFinalConditions(any(ProgressMonitor.class));
  }

  public void test_checkAllConditions_isCancelled() throws Exception {
    Refactoring refactoring = mock(RefactoringImpl.class);
    when(refactoring.checkAllConditions(any(ProgressMonitor.class))).thenCallRealMethod();
    // make monitor as cancelled
    when(pm.isCanceled()).thenReturn(true);
    // check all conditions
    try {
      refactoring.checkAllConditions(pm);
      fail();
    } catch (OperationCanceledException e) {
    }
    verify(refactoring).checkInitialConditions(any(ProgressMonitor.class));
    verify(refactoring, times(0)).checkFinalConditions(any(ProgressMonitor.class));
  }

  public void test_checkAllConditions_nullProgressMonitor() throws Exception {
    Refactoring refactoring = mock(RefactoringImpl.class);
    when(refactoring.checkAllConditions(any(ProgressMonitor.class))).thenCallRealMethod();
    // no NPE
    refactoring.checkAllConditions(null);
  }
}
