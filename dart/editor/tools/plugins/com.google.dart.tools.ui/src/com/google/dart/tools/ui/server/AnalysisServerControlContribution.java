package com.google.dart.tools.ui.server;

import com.google.dart.server.AnalysisServerStatusListener;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.themes.Fonts;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class AnalysisServerControlContribution implements AnalysisServerStatusListener {

  private Control control;

  private CLabel label;

  public AnalysisServerControlContribution(WorkbenchWindowControlContribution controlContribution) {

  }

  public Control createControl(Composite parent) {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      control = createLabel(parent);
      DartCore.getAnalysisServer().addStatusListener(this);
    } else {
      control = new Composite(parent, SWT.NONE);
    }
    return control;
  }

  @Override
  public void isAliveServer(boolean isAlive) {
    if (!isAlive) {
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          updateLabelText("Analysis Server Inactive");
        }
      });

    }
  }

  private Control createLabel(Composite parent) {

    Label spacer = new Label(parent, SWT.NONE);
    GridDataFactory.fillDefaults().hint(2, 0).applyTo(spacer);

    label = new CLabel(parent, SWT.NONE);
    label.setAlignment(SWT.CENTER);
    label.setFont(Fonts.getBoldFont(label.getFont()));
    int fgColor = SWT.COLOR_WHITE;
    int bgColor = SWT.COLOR_DARK_RED;
    Display display = label.getDisplay();
    label.setBackground(display.getSystemColor(bgColor));
    label.setForeground(display.getSystemColor(fgColor));
    updateLabelText("   Analysis Server Active   ");
    label.setToolTipText("Analysis Server status");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
    label.setVisible(false);
    return label;
  }

  private void updateLabelText(String message) {
    if (label != null && !label.isDisposed()) {
      label.setText(message);
      label.setVisible(true);
    }
  }
}
