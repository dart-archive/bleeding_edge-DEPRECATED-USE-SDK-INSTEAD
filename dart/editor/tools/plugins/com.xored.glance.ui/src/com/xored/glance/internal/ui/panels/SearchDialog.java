/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.panels;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuri Strot
 */
public class SearchDialog extends PopupDialog {

  private Composite titleArea;

  protected Text titleText;

  private Font infoFont;

  private Label info;

  private Label progress;

  private Control separator;

  private static final GridDataFactory LAYOUTDATA_GRAB_BOTH = GridDataFactory.fillDefaults().grab(
      true,
      true);

  private static final GridLayoutFactory POPUP_LAYOUT_FACTORY = GridLayoutFactory.fillDefaults().margins(
      POPUP_MARGINWIDTH,
      POPUP_MARGINHEIGHT).spacing(POPUP_HORIZONTALSPACING, POPUP_VERTICALSPACING);

  protected static final String HELP_TEXT = "Enter search text";

  public SearchDialog(final Shell parent) {
    super(parent, SWT.RESIZE, true, false, false, true, false, null, null);
  }

  protected void applyBackgroundColor(final Color color) {
    applyBackgroundColor(color, titleArea);
  }

  protected void applyColors(final Composite composite) {
    applyForegroundColor(getForeground(), composite);
    applyBackgroundColor(getBackground(), composite);
  }

  protected void applyFonts(final Composite composite) {
    Dialog.applyDialogFont(composite);

    if (info != null) {
      final Font font = info.getFont();
      final FontData[] fontDatas = font.getFontData();
      for (int i = 0; i < fontDatas.length; i++) {
        fontDatas[i].setHeight(fontDatas[i].getHeight() * 9 / 10);
      }
      infoFont = new Font(info.getDisplay(), fontDatas);
      info.setFont(infoFont);
    }
  }

  @Override
  protected void configureShell(final Shell shell) {
    super.configureShell(shell);
    shell.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(final DisposeEvent e) {
        handleClose();
      }
    });
  }

  @Override
  protected Control createContents(final Composite parent) {
    final Composite composite = new Composite(parent, SWT.NONE);
    POPUP_LAYOUT_FACTORY.applyTo(composite);
    LAYOUTDATA_GRAB_BOTH.applyTo(composite);

    titleArea = (Composite) createTitleMenuArea(composite);
    separator = createHorizontalSeparator(composite);
    createInfoTextArea(composite);

    applyColors(composite);
    applyFonts(composite);
    return composite;
  }

  @Override
  protected Control createInfoTextArea(final Composite parent) {
    final Composite composite = new Composite(parent, SWT.NONE);
    final GridLayout layout = new GridLayout(3, false);
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    progress = new Label(composite, SWT.LEFT);
    // Status label
    info = new Label(composite, SWT.RIGHT);

    info.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    progress.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    // factory.applyTo(info);
    // factory.applyTo(progress);
    final Color color = parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
    info.setForeground(color);
    progress.setForeground(color);
    info.setText(HELP_TEXT);
    return composite;
  }

  @Override
  protected List<Control> getBackgroundColorExclusions() {
    final List<Control> list = copyControls(super.getBackgroundColorExclusions());
    if (separator != null) {
      list.add(separator);
    }
    return list;
  }

  @Override
  protected List<Control> getForegroundColorExclusions() {
    final List<Control> list = copyControls(super.getForegroundColorExclusions());
    if (info != null) {
      list.add(info);
    }
    if (separator != null) {
      list.add(separator);
    }
    return list;
  }

  protected void handleClose() {
    if (infoFont != null && !infoFont.isDisposed()) {
      infoFont.dispose();
    }
    infoFont = null;
  }

  @Override
  protected void setInfoText(final String text) {
    info.setText(text);
  }

  private List<Control> copyControls(List<?> list) {
    List<Control> result = new ArrayList<Control>(list.size());
    for (Control control : result) {
      result.add(control);
    }
    return result;
  }

  /**
   * Create a horizontal separator for the given parent.
   * 
   * @param parent The parent composite.
   * @return The Control representing the horizontal separator.
   */
  private Control createHorizontalSeparator(final Composite parent) {
    final Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(separator);
    return separator;
  }

}
