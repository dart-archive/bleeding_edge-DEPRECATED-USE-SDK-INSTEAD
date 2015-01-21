package com.google.dart.tools.ui.server;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

/**
 * Adds a status indicator for the Analysis Server to the toolbar
 */

public class AnalysisServerToolbarControlContribution extends WorkbenchWindowControlContribution {

  private static final int VERTICAL_NUDGE = Util.isLinux() ? 0 : 1;

  private AnalysisServerControlContribution analysisServerStatusButton;

  @Override
  protected Control createControl(Composite parent) {

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(4).margins(4, VERTICAL_NUDGE).spacing(2, 0).applyTo(
        composite);
    analysisServerStatusButton = new AnalysisServerControlContribution(this);
    analysisServerStatusButton.createControl(composite);
    return composite;
  }
}
