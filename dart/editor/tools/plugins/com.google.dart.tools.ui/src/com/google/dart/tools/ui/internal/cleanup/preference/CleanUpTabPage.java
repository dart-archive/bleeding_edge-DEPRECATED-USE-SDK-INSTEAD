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

import com.google.dart.tools.ui.cleanup.ICleanUpConfigurationUI;
import com.google.dart.tools.ui.internal.cleanup.CleanUpConstants;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public abstract class CleanUpTabPage extends ModifyDialogTabPage implements ICleanUpConfigurationUI {

  private Map<String, String> fValues;
  private DartPreview fCleanUpPreview;
  private boolean fIsSaveAction;

  private int fCount;
  private int fSelectedCount;

  public CleanUpTabPage() {
    super();
    fCount = 0;
    setSelectedCleanUpCount(0);
    fIsSaveAction = false;
  }

  @Override
  public int getCleanUpCount() {
    return fCount;
  }

  @Override
  public int getSelectedCleanUpCount() {
    return fSelectedCount;
  }

  /**
   * @return is this tab page shown in the save action dialog
   */
  public boolean isSaveAction() {
    return fIsSaveAction;
  }

  /**
   * @param kind the kind of clean up to configure
   * @see CleanUpConstants#DEFAULT_CLEAN_UP_OPTIONS
   * @see CleanUpConstants#DEFAULT_SAVE_ACTION_OPTIONS
   */
  public void setOptionsKind(int kind) {
    fIsSaveAction = kind == CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setWorkingValues(Map<String, String> workingValues) {
    super.setWorkingValues(workingValues);
    fValues = workingValues;
  }

  @Override
  protected DartPreview doCreateJavaPreview(Composite parent) {
    fCleanUpPreview = new CleanUpPreview(parent, this);
    return fCleanUpPreview;
  }

  @Override
  protected void doUpdatePreview() {
    fCleanUpPreview.setWorkingValues(fValues);
    fCleanUpPreview.update();
  }

  @Override
  protected void initializePage() {
    fCleanUpPreview.update();
  }

  protected void intent(Composite group) {
    Label l = new Label(group, SWT.NONE);
    GridData gd = new GridData();
    gd.widthHint = fPixelConverter.convertWidthInCharsToPixels(4);
    l.setLayoutData(gd);
  }

  protected void registerPreference(final CheckboxPreference preference) {
    fCount++;
    preference.addObserver(new Observer() {
      @Override
      public void update(Observable o, Object arg) {
        if (preference.getChecked()) {
          setSelectedCleanUpCount(fSelectedCount + 1);
        } else {
          setSelectedCleanUpCount(fSelectedCount - 1);
        }
      }
    });
    if (preference.getChecked()) {
      setSelectedCleanUpCount(fSelectedCount + 1);
    }
  }

  protected void registerSlavePreference(final CheckboxPreference master,
      final CheckboxPreference[] slaves) {
    registerSlavePreference(master, slaves, null);
  }

  /**
   * Connects master and slave checkboxes.
   * 
   * @param master the master
   * @param slaves direct slaves of the master
   * @param subSlaves indirect slaves, i.e. a slave is a master of its subSlave). First index into
   *          array is the subSlave's master's index. subSlaves can also be <code>null</code>.
   */
  protected void registerSlavePreference(final CheckboxPreference master,
      final CheckboxPreference[] slaves, final CheckboxPreference[][] subSlaves) {
    if (subSlaves != null) {
      // Need to add subSlave observers before registering slaves below, since java.util.Observable notifies in wrong order!
      for (int i = 0; i < slaves.length; i++) {
        final CheckboxPreference slave = slaves[i];
        for (int j = 0; j < subSlaves[i].length; j++) {
          final CheckboxPreference subSlave = subSlaves[i][j];
          master.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
              boolean enabled = slave.getEnabled() && slave.getChecked();
              subSlave.setEnabled(enabled);
            }
          });
        }
      }
    }

    internalRegisterSlavePreference(master, slaves);
    fCount += slaves.length;

    master.addObserver(new Observer() {
      @Override
      public void update(Observable o, Object arg) {
        boolean masterChecked = master.getChecked();
        for (int i = 0; i < slaves.length; i++) {
          if (slaves[i].getChecked()) {
            setSelectedCleanUpCount(fSelectedCount + (masterChecked ? 1 : -1));
            if (subSlaves != null) {
              for (int j = 0; j < subSlaves[i].length; j++) {
                if (subSlaves[i][j].getChecked()) {
                  setSelectedCleanUpCount(fSelectedCount + (masterChecked ? 1 : -1));
                }
              }
            }
          }
        }
      }
    });

    for (int i = 0; i < slaves.length; i++) {
      final CheckboxPreference slave = slaves[i];
      slave.addObserver(new Observer() {
        @Override
        public void update(Observable o, Object arg) {
          setSelectedCleanUpCount(fSelectedCount + (slave.getChecked() ? 1 : -1));
        }
      });
    }

    if (master.getChecked()) {
      for (int i = 0; i < slaves.length; i++) {
        if (slaves[i].getChecked() && master.getEnabled()) {
          setSelectedCleanUpCount(fSelectedCount + 1);
        }
      }
    }
  }

  protected void registerSlavePreference(final CheckboxPreference master,
      final RadioPreference[] slaves) {
    internalRegisterSlavePreference(master, slaves);
    registerPreference(master);
  }

  private void internalRegisterSlavePreference(final CheckboxPreference master,
      final ButtonPreference[] slaves) {
    master.addObserver(new Observer() {
      @Override
      public void update(Observable o, Object arg) {
        for (int i = 0; i < slaves.length; i++) {
          slaves[i].setEnabled(master.getChecked());
        }
      }
    });

    for (int i = 0; i < slaves.length; i++) {
      slaves[i].setEnabled(master.getChecked());
    }
  }

  private void setSelectedCleanUpCount(int selectedCount) {
    Assert.isLegal(selectedCount >= 0 && selectedCount <= fCount);
    fSelectedCount = selectedCount;
  }

}
