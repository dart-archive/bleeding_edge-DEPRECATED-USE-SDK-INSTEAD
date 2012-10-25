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
package com.google.dart.tools.ui.dialogs;

import com.google.dart.tools.ui.themes.Fonts;
import com.google.dart.tools.update.core.Revision;
import com.google.dart.tools.update.core.UpdateAdapter;
import com.google.dart.tools.update.core.UpdateCore;
import com.google.dart.tools.update.core.UpdateManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * Contributes an update status label and update action button to the {@link AboutDartDialog}.
 * TODO(pquitslund): this implementation and UX is provisional and under active development
 */
public class UpdateStatusControl extends UpdateAdapter implements DisposeListener {

  private class UpdateAction extends Action {
    public UpdateAction(String text) {
      super(text);
    }

    public void runWithSelectionEvent(SelectionEvent event) {
      run();
    };
  }

  private CLabel updateStatusLabel;
  private Button updateStatusButton;

  private UpdateAction updateAction;

  private Font regularFont;
  private Font italicFont;

  private UpdateAction applyUpdateAction = new UpdateAction("Apply Update...") {
    @Override
    public void run() {
      UpdateCore.getUpdateManager().scheduleInstall();
    }
  };

  private UpdateAction downloadUpdateAction = new UpdateAction("Download Update...") {
    @Override
    public void run() {
      UpdateCore.getUpdateManager().scheduleDownload(latestAvailableRevision);
    }

    @Override
    public void runWithSelectionEvent(SelectionEvent event) {
      if (event.stateMask == SWT.SHIFT) {
        Revision revision = promptForRevision(latestAvailableRevision);
        if (revision == null) {
          return;
        }
        latestAvailableRevision = revision;
      }
      super.runWithSelectionEvent(event);
    };
  };

  private UpdateAction checkFordUpdatesAction = new UpdateAction("Check for Update...") {
    @Override
    public void run() {
      UpdateCore.getUpdateManager().scheduleUpdateCheck();
    }
  };

  private Revision latestAvailableRevision;

  private final Color backgroundColor;
  private final Point margin;

  private final boolean isCentered;

  public UpdateStatusControl(Composite parent, Color backgroundColor, Point margin,
      boolean isCentered) {

    this.backgroundColor = backgroundColor;
    this.margin = margin;
    this.isCentered = isCentered;

    createControl(parent);
    cacheFonts();

    setStatus("Checking for updates...", italicFont);
    setActionDisabled(checkFordUpdatesAction);

    UpdateManager updateManager = UpdateCore.getUpdateManager();

    updateManager.addListener(this);

    if (updateManager.isDownloadingUpdate()) {
      downloadStarted();
    } else {
      updateManager.scheduleUpdateCheck();
    }

    parent.addDisposeListener(this);

  }

  @Override
  public void checkComplete() {
    UpdateCore.logInfo("UpdateStatusControl.checkComplete()");
    asyncExec(new Runnable() {
      @Override
      public void run() {
        setStatus("Dart Editor is up to date.", regularFont);
        setActionDisabled(checkFordUpdatesAction);
      }
    });
  }

  @Override
  public void checkFailed(final String message) {
    UpdateCore.logInfo("UpdateStatusControl.checkFailed()");
    asyncExec(new Runnable() {
      @Override
      public void run() {
        setStatus(message, regularFont);
        setActionEnabled(checkFordUpdatesAction);
      }
    });
  }

  @Override
  public void checkStarted() {
    asyncExec(new Runnable() {
      @Override
      public void run() {
        setStatus("Checking for updates...", italicFont);
        setActionDisabled(downloadUpdateAction);
      }
    });
  }

  @Override
  public void downloadCancelled() {
    asyncExec(new Runnable() {
      @Override
      public void run() {
        setStatus("Download failed. Retry?", regularFont);
        setActionEnabled(downloadUpdateAction);
      }
    });
  }

  @Override
  public void downloadComplete() {
    asyncExec(new Runnable() {
      @Override
      public void run() {
        if (latestAvailableRevision != null) {
          setStatus(
              bindLatestAvailableRevision("A new version {0} is ready to install."),
              regularFont);
          setActionEnabled(applyUpdateAction);
        }
      }
    });
  }

  @Override
  public void downloadStarted() {
    asyncExec(new Runnable() {
      @Override
      public void run() {
        setStatus("Downloading update...", italicFont);
        setActionDisabled(downloadUpdateAction);
      }
    });
  }

  @Override
  public void updateAvailable(Revision revision) {
    this.latestAvailableRevision = revision;
    UpdateCore.logInfo("UpdateStatusControl.updateAvailable() => " + latestAvailableRevision);
    asyncExec(new Runnable() {
      @Override
      public void run() {
        setStatus(bindLatestAvailableRevision("A new version {0} is available."), regularFont);
        setActionEnabled(downloadUpdateAction);
      }
    });
  }

  @Override
  public void updateStaged() {
    UpdateCore.logInfo("UpdateStatusControl.updateStaged()");
    asyncExec(new Runnable() {
      @Override
      public void run() {
        setStatus("An update is ready to install", regularFont);
        setActionEnabled(applyUpdateAction);
      }
    });
  }

  @Override
  public void widgetDisposed(DisposeEvent e) {
    UpdateCore.getUpdateManager().removeListener(this);
  }

  private void asyncExec(Runnable runnable) {
    updateStatusLabel.getDisplay().asyncExec(runnable);
  }

  private String bindLatestAvailableRevision(String msg) {
    return NLS.bind(msg, "[" + latestAvailableRevision + "]");
  }

  private void cacheFonts() {
    regularFont = updateStatusLabel.getFont();
    italicFont = Fonts.getItalicFont(regularFont);
  }

  private void createControl(Composite parent) {

    Composite comp = new Composite(parent, SWT.NONE);

    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(comp);
    GridLayoutFactory.fillDefaults().numColumns(isCentered ? 1 : 2).margins(margin.x, margin.y).applyTo(
        comp);

    updateStatusLabel = new CLabel(comp, SWT.NONE);
    if (backgroundColor != null) {
      comp.setBackground(backgroundColor);
      updateStatusLabel.setBackground(backgroundColor);
    }

    GridDataFactory.fillDefaults().align(isCentered ? SWT.CENTER : SWT.FILL, SWT.CENTER).grab(
        true,
        false).applyTo(updateStatusLabel);

    updateStatusButton = new Button(comp, SWT.PUSH);
    updateStatusButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
    GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).indent(0, 3).applyTo(
        updateStatusButton);

    updateStatusButton.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        performAction(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        performAction(e);
      }
    });
  }

  private void performAction(SelectionEvent e) {
    if (updateAction != null) {
      updateAction.runWithSelectionEvent(e);
    }
  }

  private Revision promptForRevision(Revision originalRevision) {
    InputDialog inputDialog = new InputDialog(
        null,
        "Download Revision",
        "FOR DEVELOPMENT USE ONLY" + System.getProperty("line.separator")
            + "    Enter the revision to be downloaded and installed",
        originalRevision.toString(),
        new IInputValidator() {
          @Override
          public String isValid(String newText) {
            if (newText == null || newText.length() == 0) {
              return "Enter a revision # or click Cancel";
            }
            try {
              Integer.valueOf(newText);
            } catch (NumberFormatException e) {
              return "Invalid revision #";
            }
            return null;
          }
        });
    if (inputDialog.open() != Window.OK) {
      return null;
    }
    return new Revision(Integer.valueOf(inputDialog.getValue()));
  }

  private void setAction(UpdateAction action, boolean enabled) {
    this.updateAction = action;
    updateStatusButton.setText(action.getText());
    updateStatusButton.setEnabled(enabled);
    updateStatusButton.getParent().layout();
  }

  private void setActionDisabled(UpdateAction action) {
    setAction(action, false);
  }

  private void setActionEnabled(UpdateAction action) {
    setAction(action, true);
  }

  private void setStatus(String text, Font font) {
    updateStatusLabel.setText(text);
    updateStatusLabel.setFont(font);
    updateStatusLabel.update();
  }

}
