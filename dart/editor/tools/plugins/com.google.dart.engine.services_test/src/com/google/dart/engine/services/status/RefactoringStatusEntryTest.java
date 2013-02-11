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

public class RefactoringStatusEntryTest extends AbstractDartTest {

  public void test_new_withContext() throws Exception {
    RefactoringStatusContext context = mock(RefactoringStatusContext.class);
    RefactoringStatusEntry entry = new RefactoringStatusEntry(
        RefactoringStatusSeverity.ERROR,
        "my message",
        context);
    // access
    assertSame(RefactoringStatusSeverity.ERROR, entry.getSeverity());
    assertEquals("my message", entry.getMessage());
    assertSame(context, entry.getContext());
    // toString()
    {
      String str = entry.toString();
      assertThat(str).startsWith("ERROR: my message; Context: Mock for RefactoringStatusContext");
    }
  }

  public void test_new_withoutContext() throws Exception {
    RefactoringStatusEntry entry = new RefactoringStatusEntry(
        RefactoringStatusSeverity.ERROR,
        "my message");
    // access
    assertSame(RefactoringStatusSeverity.ERROR, entry.getSeverity());
    assertEquals("my message", entry.getMessage());
    assertSame(null, entry.getContext());
    // isX
    assertFalse(entry.isFatalError());
    assertTrue(entry.isError());
    assertFalse(entry.isWarning());
    assertFalse(entry.isInfo());
    // toString()
    assertEquals("ERROR: my message", entry.toString());
  }
}
