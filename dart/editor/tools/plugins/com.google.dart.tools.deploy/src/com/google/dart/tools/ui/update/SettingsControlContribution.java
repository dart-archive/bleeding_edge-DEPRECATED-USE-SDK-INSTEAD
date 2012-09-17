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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

/**
 * Contributes the Settings/Update button control to the main dart editor toolbar.
 */
public class SettingsControlContribution extends UpdateAdapter implements DisposeListener {

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

  private ToolItem settingsButton;

  private UpdateState updateState = UpdateState.UNKNOWN;

  public SettingsControlContribution(WorkbenchWindowControlContribution controlContribution) {
    this.controlContribution = controlContribution;
  }

  public void createControl(Composite composite) {
    ToolBar toolBar = new ToolBar(composite, SWT.NONE);
    settingsButton = new ToolItem(toolBar, SWT.DROP_DOWN | SWT.NO_TRIM);
    settingsButton.setImage(updateState.getButtonImage());
    settingsButton.setToolTipText(updateState.getTooltipText());
    settingsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateState.performAction(controlContribution.getWorkbenchWindow());
      }
    });

    if (Util.isLinux()) {
      GridDataFactory.fillDefaults().indent(0, 1).grab(true, true).applyTo(toolBar);
    }

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

  private void refreshButton() {
    asyncExec(new Runnable() {
      @Override
      public void run() {
        if (!settingsButton.isDisposed()) {
          settingsButton.setImage(updateState.getButtonImage());
          settingsButton.setToolTipText(updateState.getTooltipText());
        }
      }
    });
  }
}
