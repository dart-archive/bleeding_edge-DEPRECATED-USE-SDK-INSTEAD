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
package com.google.dart.tools.ui.feedback;

import com.google.dart.tools.ui.themes.Fonts;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

/**
 * Contributes the "Submit Feedback" button control.
 */
public class FeedbackControlContribution {

  private Control control;
  private CLabel label;
  private boolean inControl;

  private OpenFeedbackDialogAction openFeedbackDialogAction;

  private WorkbenchWindowControlContribution controlContribution;

  public FeedbackControlContribution(WorkbenchWindowControlContribution controlContribution) {
    this.controlContribution = controlContribution;
  }

  public Control createControl(Composite parent) {
    control = createLabel(parent);
    hookupLabelListeners();
    return control;
  }

  protected void handleMouseEnter() {
    inControl = true;
  }

  protected void handleMouseExit() {
    inControl = false;
  }

  protected void handleSelection() {
    if (openFeedbackDialogAction == null) {
      openFeedbackDialogAction = createOpenDialogAction();
    }
    openFeedbackDialogAction.run();
  }

  private Control createLabel(Composite parent) {

    label = new CLabel(parent, SWT.NONE);
    label.setAlignment(SWT.CENTER);
    label.setText(FeedbackMessages.FeedbackButtonControl_Text);
    label.setBackground(label.getDisplay().getSystemColor(SWT.COLOR_GRAY));
    label.setFont(Fonts.getBoldFont(label.getFont()));
    label.setForeground(label.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    label.setToolTipText(FeedbackMessages.FeedbackControlContribution_control_tootip);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

    Label spacer = new Label(parent, SWT.NONE);
    GridDataFactory.fillDefaults().hint(4, 0).applyTo(spacer);

    return label;
  }

  private OpenFeedbackDialogAction createOpenDialogAction() {
    return new OpenFeedbackDialogAction(getWorkbenchWindow()) {
      @Override
      public void run() {
        //if there is a dialog, give it focus, else open one
        Shell shell = getDialogShell();
        if (shell != null) {
          shell.forceFocus();
        } else {
          super.run();
        }
      }
    };
  }

  private IWorkbenchWindow getWorkbenchWindow() {
    return controlContribution.getWorkbenchWindow();
  }

  private void hookupLabelListeners() {
    label.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        if (inControl && e.button == 1) {
          handleSelection();
        }
      }
    });

    label.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        handleMouseEnter();
      }

      @Override
      public void mouseExit(MouseEvent e) {
        handleMouseExit();
      }
    });
  }
}
