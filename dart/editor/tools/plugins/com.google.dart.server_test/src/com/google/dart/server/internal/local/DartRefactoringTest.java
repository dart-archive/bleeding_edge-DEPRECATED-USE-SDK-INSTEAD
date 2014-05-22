/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local;

import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusEntry;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

public class DartRefactoringTest extends AbstractLocalServerTest {
  /**
   * Asserts that given {@link RefactoringStatus} has expected severity.
   */
  public static void assertRefactoringStatus(RefactoringStatus status,
      RefactoringStatusSeverity expectedSeverity) {
    assertRefactoringStatus(status, expectedSeverity, null, null);
  }

  /**
   * Asserts that given {@link RefactoringStatus} has expected severity and message.
   */
  public static void assertRefactoringStatus(RefactoringStatus status,
      RefactoringStatusSeverity expectedSeverity, String expectedMessage) {
    assertRefactoringStatus(status, expectedSeverity, expectedMessage, null);
  }

  /**
   * Asserts that given {@link RefactoringStatus} has expected severity and message.
   */
  public static void assertRefactoringStatus(RefactoringStatus status,
      RefactoringStatusSeverity expectedSeverity, String expectedMessage,
      SourceRange expectedContextRange) {
    assertNotNull(status);
    assertSame(status.getMessage(), expectedSeverity, status.getSeverity());
    if (expectedSeverity != RefactoringStatusSeverity.OK) {
      RefactoringStatusEntry entry = status.getEntryWithHighestSeverity();
      assertSame(expectedSeverity, entry.getSeverity());
      if (expectedMessage != null) {
        assertEquals(expectedMessage, entry.getMessage());
      }
      if (expectedContextRange != null) {
        assertEquals(expectedContextRange, entry.getContext().getRange());
      }
    }
  }

  /**
   * Asserts that given {@link RefactoringStatus} is OK.
   */
  public static void assertRefactoringStatusOK(RefactoringStatus status) {
    assertRefactoringStatus(status, RefactoringStatusSeverity.OK, null);
  }

  protected String contextId;
  protected String code;
  protected Source source;
  protected int selectionOffset;
  protected int selectionLength;

  protected String refactoring_id;
  protected RefactoringStatus refactoring_status;
  protected Change refactoring_change;

  protected final void createContextWithSingleSource(String code) {
    this.contextId = createContext("test");
    this.code = code;
    this.source = addSource(contextId, "/test.dart", code);
  }

  protected final void setSelectionAtRange(String search) {
    selectionOffset = code.indexOf(search);
    selectionLength = search.length();
  }
}
