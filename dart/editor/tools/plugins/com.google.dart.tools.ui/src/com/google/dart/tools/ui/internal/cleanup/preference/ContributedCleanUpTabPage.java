/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.cleanup.ICleanUpConfigurationUI;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.Map;

/**
 * @since 3.5
 */
public class ContributedCleanUpTabPage extends CleanUpTabPage {

  private final ICleanUpConfigurationUI fContribution;

  public ContributedCleanUpTabPage(ICleanUpConfigurationUI contribution) {
    fContribution = contribution;
  }

  /*
   * @see org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage#getCleanUpCount()
   */
  @Override
  public int getCleanUpCount() {
    final int[] result = new int[] {0};
    SafeRunner.run(new ISafeRunnable() {
      @Override
      public void handleException(Throwable exception) {
        ContributedCleanUpTabPage.this.handleException(exception);
      }

      @Override
      public void run() throws Exception {
        result[0] = fContribution.getCleanUpCount();
      }
    });
    return result[0];
  }

  /*
   * @see org.eclipse.jdt.internal.ui.preferences.cleanup.ICleanUpTabPage#getPreview()
   */
  @Override
  public String getPreview() {
    final String[] result = new String[] {""}; //$NON-NLS-1$
    SafeRunner.run(new ISafeRunnable() {
      @Override
      public void handleException(Throwable exception) {
        ContributedCleanUpTabPage.this.handleException(exception);
      }

      @Override
      public void run() throws Exception {
        result[0] = fContribution.getPreview();
      }
    });
    return result[0];
  }

  /*
   * @see org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage#getSelectedCleanUpCount()
   */
  @Override
  public int getSelectedCleanUpCount() {
    final int[] result = new int[] {0};
    SafeRunner.run(new ISafeRunnable() {
      @Override
      public void handleException(Throwable exception) {
        ContributedCleanUpTabPage.this.handleException(exception);
      }

      @Override
      public void run() throws Exception {
        int count = fContribution.getSelectedCleanUpCount();
        Assert.isTrue(count >= 0 && count <= getCleanUpCount());
        result[0] = count;
      }
    });
    return result[0];
  }

  /*
   * @see
   * org.eclipse.jdt.internal.ui.preferences.cleanup.ICleanUpTabPage#setOptions(org.eclipse.jdt.
   * internal.ui.fix.CleanUpOptions)
   */
  @Override
  public void setOptions(CleanUpOptions options) {
  }

  /*
   * @see
   * org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage#setWorkingValues(java.util.Map)
   */
  @Override
  public void setWorkingValues(Map<String, String> workingValues) {
    super.setWorkingValues(workingValues);

    final CleanUpOptions options = new CleanUpOptions(workingValues) {
      /*
       * @see org.eclipse.jdt.internal.ui.fix.CleanUpOptions#setOption(java.lang.String,
       * java.lang.String)
       */
      @Override
      public void setOption(String key, String value) {
        super.setOption(key, value);

        doUpdatePreview();
        notifyValuesModified();
      }
    };
    SafeRunner.run(new ISafeRunnable() {
      @Override
      public void handleException(Throwable exception) {
        ContributedCleanUpTabPage.this.handleException(exception);
      }

      @Override
      public void run() throws Exception {
        fContribution.setOptions(options);
      }
    });
  }

  /*
   * @see
   * org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage#doCreatePreferences(org
   * .eclipse.swt.widgets.Composite, int)
   */
  @Override
  protected void doCreatePreferences(Composite composite, int numColumns) {
    final Composite parent = new Composite(composite, SWT.NONE);
    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    layoutData.horizontalSpan = numColumns;
    parent.setLayoutData(layoutData);
    parent.setLayout(new GridLayout(1, false));

    SafeRunner.run(new ISafeRunnable() {
      @Override
      public void handleException(Throwable exception) {
        ContributedCleanUpTabPage.this.handleException(exception);

        Label label = new Label(parent, SWT.NONE);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        label.setText(CleanUpMessages.ContributedCleanUpTabPage_ErrorPage_message);
      }

      @Override
      public void run() throws Exception {
        fContribution.createContents(parent);
      }
    });
  }

  private void handleException(Throwable exception) {
    DartToolsPlugin.log(exception);
  }

}
