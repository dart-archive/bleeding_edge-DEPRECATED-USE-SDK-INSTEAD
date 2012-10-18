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
package com.google.dart.tools.ui.update;

import com.google.dart.tools.deploy.Activator;
import com.google.dart.tools.update.core.Revision;
import com.google.dart.tools.update.core.UpdateAdapter;
import com.google.dart.tools.update.core.UpdateCore;
import com.google.dart.tools.update.core.UpdateManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

/**
 * Contributes the Settings/Update button control to the main dart editor toolbar.
 */
public class SettingsControlContribution extends UpdateAdapter implements DisposeListener {

  final class LinuxControl extends SettingsControl<Button> {

    LinuxControl(Composite composite) {
      super(new Button(composite, SWT.NO_FOCUS));
      GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).applyTo(control);
    }

    @Override
    void addSelectionListener(SelectionAdapter listener) {
      control.addSelectionListener(listener);
    }

    @Override
    void setImage(Image image) {
      control.setImage(image);
    }

    @Override
    void setToolTipText(String tooltipText) {
      control.setToolTipText(tooltipText);
    }

  }

  final class StandardControl extends SettingsControl<ToolItem> {

    StandardControl(Composite composite) {
      super(new ToolItem(new ToolBar(composite, SWT.NONE), SWT.DROP_DOWN | SWT.NO_TRIM));
    }

    @Override
    void addSelectionListener(SelectionAdapter listener) {
      control.addSelectionListener(listener);
    }

    @Override
    void setImage(Image image) {
      control.setImage(image);
    }

    @Override
    void setToolTipText(String tooltipText) {
      control.setToolTipText(tooltipText);
    }

  }

  private abstract class SettingsControl<T extends Widget> {

    final T control;

    SettingsControl(T settingsButton) {

      this.control = settingsButton;

      refresh();

      addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateState.performAction(controlContribution.getWorkbenchWindow());
        }
      });

    }

    abstract void addSelectionListener(SelectionAdapter listener);

    boolean isDisposed() {
      return control.isDisposed();
    }

    void refresh() {
      setImage(updateState.getButtonImage());
      setToolTipText(updateState.getTooltipText());
    }

    abstract void setImage(Image image);

    abstract void setToolTipText(String tooltipText);

  }

  private static enum UpdateState {
    UNKNOWN("icons/full/obj16/wrench.gif", "Preferences") {
      @Override
      void performAction(IWorkbenchWindow window) {
        openPreferences(null);
      }
    },
    AVAILABLE("icons/full/obj16/wrench-update.gif", "Update available") {
      @Override
      void performAction(IWorkbenchWindow window) {
        openPreferences(UpdatePreferencePage.PAGE_ID);
      }
    };

    private final String imagePath;
    private final String tooltipText;

    UpdateState(String imagePath, String tooltipText) {
      this.imagePath = imagePath;
      this.tooltipText = tooltipText;
    }

    Image getButtonImage() {
      return Activator.getImage(imagePath);
    }

    String getTooltipText() {
      return tooltipText;
    }

    abstract void performAction(IWorkbenchWindow window);

  }

  private static void asyncExec(Runnable runnable) {
    Display.getDefault().asyncExec(runnable);
  }

  private static void openPreferences(final String preferencePageId) {
    asyncExec(new Runnable() {
      @Override
      public void run() {
        PreferencesUtil.createPreferenceDialogOn(null, preferencePageId, null, null).open();
      }
    });
  }

  private final WorkbenchWindowControlContribution controlContribution;

  private SettingsControl<? extends Widget> settingsButton;

  private UpdateState updateState = UpdateState.UNKNOWN;

  public SettingsControlContribution(WorkbenchWindowControlContribution controlContribution) {
    this.controlContribution = controlContribution;
  }

  public void createControl(Composite composite) {

    settingsButton = createSettingsControl(composite);

    composite.addDisposeListener(this);

    UpdateManager updateManager = UpdateCore.getUpdateManager();
    updateManager.addListener(this);

  }

  @Override
  public void updateAvailable(Revision revision) {
    updateState = UpdateState.AVAILABLE;
    refreshButton();
  }

  @Override
  public void widgetDisposed(DisposeEvent e) {
    UpdateCore.getUpdateManager().removeListener(this);
  }

  private SettingsControl<? extends Widget> createSettingsControl(Composite composite) {
    if (Util.isLinux()) {
      return new LinuxControl(composite);
    }
    return new StandardControl(composite);
  }

  private void refreshButton() {
    asyncExec(new Runnable() {
      @Override
      public void run() {
        if (!settingsButton.isDisposed()) {
          settingsButton.refresh();
        }
      }
    });
  }
}
