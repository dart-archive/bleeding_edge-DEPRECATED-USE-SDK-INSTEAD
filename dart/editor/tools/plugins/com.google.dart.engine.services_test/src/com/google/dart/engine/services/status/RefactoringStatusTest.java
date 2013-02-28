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

package com.google.dart.engine.services.status;

import com.google.dart.engine.services.internal.correction.AbstractDartTest;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

public class RefactoringStatusTest extends AbstractDartTest {

  public void test_addError() throws Exception {
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    // initial state
    assertSame(RefactoringStatusSeverity.OK, refactoringStatus.getSeverity());
    // add ERROR
    refactoringStatus.addError("msg");
    assertSame(RefactoringStatusSeverity.ERROR, refactoringStatus.getSeverity());
    assertFalse(refactoringStatus.isOK());
    assertFalse(refactoringStatus.hasFatalError());
    assertTrue(refactoringStatus.hasError());
    assertTrue(refactoringStatus.hasWarning());
    assertTrue(refactoringStatus.hasInfo());
    // entries
    List<RefactoringStatusEntry> entries = refactoringStatus.getEntries();
    assertThat(entries).hasSize(1);
    assertEquals("msg", entries.get(0).getMessage());
    // toString()
    assertEquals("<ERROR\n\tERROR: msg\n>", refactoringStatus.toString());
  }

  public void test_addFatalError_withContext() throws Exception {
    RefactoringStatusContext context = mock(RefactoringStatusContext.class);
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    // add FATAL
    refactoringStatus.addFatalError("fatal-msg", context);
    assertFalse(refactoringStatus.isOK());
    assertTrue(refactoringStatus.hasFatalError());
    assertTrue(refactoringStatus.hasError());
    assertTrue(refactoringStatus.hasWarning());
    assertTrue(refactoringStatus.hasInfo());
    // toString()
    {
      String str = refactoringStatus.toString();
      assertThat(str).startsWith(
          "<FATAL\n\tFATAL: fatal-msg; Context: Mock for RefactoringStatusContext");
    }
    // add WARNING, resulting severity is still FATAL
    refactoringStatus.addWarning("warning");
    assertTrue(refactoringStatus.hasFatalError());
  }

  public void test_addFatalError_withoutContext() throws Exception {
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    // add FATAL
    refactoringStatus.addFatalError("fatal-msg");
    assertFalse(refactoringStatus.isOK());
    assertTrue(refactoringStatus.hasFatalError());
    assertTrue(refactoringStatus.hasError());
    assertTrue(refactoringStatus.hasWarning());
    assertTrue(refactoringStatus.hasInfo());
    // toString()
    assertEquals("<FATAL\n\tFATAL: fatal-msg\n>", refactoringStatus.toString());
  }

  public void test_addWarning() throws Exception {
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    // add WARNING
    refactoringStatus.addWarning("msg");
    assertFalse(refactoringStatus.isOK());
    assertFalse(refactoringStatus.hasFatalError());
    assertFalse(refactoringStatus.hasError());
    assertTrue(refactoringStatus.hasWarning());
    assertTrue(refactoringStatus.hasInfo());
    // toString()
    assertEquals("<WARNING\n\tWARNING: msg\n>", refactoringStatus.toString());
  }

  public void test_createErrorStatus() throws Exception {
    RefactoringStatus refactoringStatus = RefactoringStatus.createErrorStatus("error-msg");
    assertTrue(refactoringStatus.hasError());
    assertEquals("error-msg", refactoringStatus.getMessage());
  }

  public void test_createFatalErrorStatus() throws Exception {
    RefactoringStatus refactoringStatus = RefactoringStatus.createFatalErrorStatus("fatal-msg");
    assertTrue(refactoringStatus.hasFatalError());
    assertEquals("fatal-msg", refactoringStatus.getMessage());
  }

  public void test_createFatalErrorStatus_withContext() throws Exception {
    RefactoringStatusContext context = mock(RefactoringStatusContext.class);
    RefactoringStatus refactoringStatus = RefactoringStatus.createFatalErrorStatus(
        "fatal-msg",
        context);
    assertTrue(refactoringStatus.hasFatalError());
    assertEquals("fatal-msg", refactoringStatus.getMessage());
    assertSame(context, refactoringStatus.getEntries().get(0).getContext());
  }

  public void test_createWarningStatus() throws Exception {
    RefactoringStatus refactoringStatus = RefactoringStatus.createWarningStatus("warning-msg");
    assertTrue(refactoringStatus.hasWarning());
    assertEquals("warning-msg", refactoringStatus.getMessage());
  }

  public void test_escalateErrorToFatal() throws Exception {
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    refactoringStatus.addError("msg");
    // status
    assertSame(RefactoringStatusSeverity.ERROR, refactoringStatus.getSeverity());
    // escalated
    RefactoringStatus escalated = refactoringStatus.escalateErrorToFatal();
    assertSame(RefactoringStatusSeverity.FATAL, escalated.getSeverity());
    assertEquals("<FATAL\n\tFATAL: msg\n>", escalated.toString());
  }

  public void test_getEntryWithHighestSeverity() throws Exception {
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    // no entries
    assertSame(null, refactoringStatus.getEntryWithHighestSeverity());
    assertSame(null, refactoringStatus.getMessage());
    // add entries
    refactoringStatus.addError("msgError");
    refactoringStatus.addWarning("msgWarning");
    refactoringStatus.addFatalError("msgFatalError");
    // get entry
    {
      RefactoringStatusEntry entry = refactoringStatus.getEntryWithHighestSeverity();
      assertSame(RefactoringStatusSeverity.FATAL, entry.getSeverity());
      assertEquals("msgFatalError", entry.getMessage());
    }
    // get message
    assertEquals("msgFatalError", refactoringStatus.getMessage());
  }

  public void test_merge_Error_withWarning() throws Exception {
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    refactoringStatus.addError("err");
    assertSame(RefactoringStatusSeverity.ERROR, refactoringStatus.getSeverity());
    // merge with OK
    {
      RefactoringStatus other = new RefactoringStatus();
      other.addWarning("warn");
      refactoringStatus.merge(other);
    }
    assertSame(RefactoringStatusSeverity.ERROR, refactoringStatus.getSeverity());
  }

  public void test_merge_Warning_null() throws Exception {
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    refactoringStatus.addWarning("warn");
    assertSame(RefactoringStatusSeverity.WARNING, refactoringStatus.getSeverity());
    // merge with "null"
    refactoringStatus.merge(null);
    assertSame(RefactoringStatusSeverity.WARNING, refactoringStatus.getSeverity());
  }

  public void test_merge_Warning_withError() throws Exception {
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    refactoringStatus.addWarning("warn");
    assertSame(RefactoringStatusSeverity.WARNING, refactoringStatus.getSeverity());
    // merge with ERROR
    {
      RefactoringStatus other = new RefactoringStatus();
      other.addError("err");
      refactoringStatus.merge(other);
    }
    assertSame(RefactoringStatusSeverity.ERROR, refactoringStatus.getSeverity());
  }

  public void test_new() throws Exception {
    RefactoringStatus refactoringStatus = new RefactoringStatus();
    // OK initially
    assertTrue(refactoringStatus.isOK());
    assertFalse(refactoringStatus.hasFatalError());
    assertFalse(refactoringStatus.hasError());
    assertFalse(refactoringStatus.hasWarning());
    assertFalse(refactoringStatus.hasInfo());
    // toString()
    assertEquals("<OK>", refactoringStatus.toString());
  }
}
