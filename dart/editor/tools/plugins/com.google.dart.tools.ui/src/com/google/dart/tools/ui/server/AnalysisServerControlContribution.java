package com.google.dart.tools.ui.server;

import com.google.dart.server.AnalysisServerStatusListener;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.themes.Fonts;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class AnalysisServerControlContribution implements AnalysisServerStatusListener {

  private static String failMessage = "Use \"{0}\" to send feedback about the failure, and relaunch {1} to restart Analysis Server.";

  private Control control;
  private boolean inControl;
  private CLabel label;
  private WorkbenchWindowControlContribution controlContribution;

  public AnalysisServerControlContribution(WorkbenchWindowControlContribution controlContribution) {
    this.controlContribution = controlContribution;
  }

  public Control createControl(Composite parent) {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      control = createLabel(parent);
      DartCore.getAnalysisServer().addStatusListener(this);
      hookupLabelListeners();
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

  protected void handleMouseEnter() {
    inControl = true;
  }

  protected void handleMouseExit() {
    inControl = false;
  }

  protected void handleSelection() {
    String message;
    if (DartCore.isPluginsBuild()) {
      message = NLS.bind(failMessage, "Help > Send feedback about Dart", "Eclipse");
    } else {
      message = NLS.bind(failMessage, "Send Feedback", "Dart Editor");
    }
    MessageDialog.openInformation(getActiveShell(), "Analaysis Server Inactive", message);
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

  private Shell getActiveShell() {
    return controlContribution.getWorkbenchWindow().getShell();
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

  private void updateLabelText(String message) {
    if (label != null && !label.isDisposed()) {
      label.setText(message);
      label.setVisible(true);
    }
  }
}
