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
package com.google.dart.tools.ui.refactoring;

import com.google.common.collect.Lists;
import com.google.dart.tools.ui.internal.refactoring.UserInteractions;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.swt.widgets.Shell;

import java.util.List;

/**
 * Abstract test for any refactoring.
 */
public abstract class RefactoringTest extends AbstractDartTest {

  protected final List<String> openInformationMessages = Lists.newArrayList();
  protected final List<Integer> showStatusSeverities = Lists.newArrayList();

  protected final List<String> showStatusMessages = Lists.newArrayList();

  protected boolean showStatusCancel;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    UserInteractions.openInformation = new UserInteractions.OpenInformation() {
      @Override
      public void open(Shell parent, String title, String message) {
        openInformationMessages.add(message);
      }
    };
    UserInteractions.showStatusDialog = new UserInteractions.ShowStatusDialog() {
      @Override
      public boolean open(RefactoringStatus status, Shell parent, String windowTitle) {
        RefactoringStatusEntry[] entries = status.getEntries();
        for (RefactoringStatusEntry entry : entries) {
          int severity = entry.getSeverity();
          showStatusSeverities.add(severity);
          showStatusMessages.add(entry.getMessage());
        }
        return showStatusCancel;
      }
    };
    showStatusCancel = true;
  }

  @Override
  protected void tearDown() throws Exception {
    UserInteractions.reset();
    super.tearDown();
  }

}
