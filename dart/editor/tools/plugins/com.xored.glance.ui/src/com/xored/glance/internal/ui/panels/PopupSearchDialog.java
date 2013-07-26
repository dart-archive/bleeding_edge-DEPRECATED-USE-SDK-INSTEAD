/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.panels;

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.xored.glance.ui.panels.SearchPanel;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.utils.UIUtils;

/**
 * @author Yuri Strot
 */
public class PopupSearchDialog extends SearchPanel {

  public PopupSearchDialog(Control target) {
    this.target = target;
    popup = new SearchPopup(target.getShell());
  }

  public int open() {
    return popup.open();
  }

  @Override
  public boolean isApplicable(Control control) {
    return true;
  }

  private int matchCount;

  @Override
  public void allFound(final Match[] matches) {
    super.allFound(matches);
    matchCount = matches.length;
    updateInfo();
  }

  protected void updateInfo() {
    UIUtils.asyncExec(popup.getShell(), new Runnable() {

      @Override
      public void run() {
        StringBuffer buffer = new StringBuffer();
        if (matchCount == 0) {
          buffer.append("No matches");
        } else if (matchCount == 1) {
          buffer.append("1 match");
        } else {
          buffer.append(matchCount);
          buffer.append(" matches");
        }
        buffer.append(" found");

        popup.setInfoText(buffer.toString());
      }
    });
  }

  @Override
  protected void textEmpty() {
    super.textEmpty();
    popup.setInfoText(SearchDialog.HELP_TEXT);
  }

  @Override
  protected Control createText(Composite parent, int style) {
    return super.createText(parent, SWT.NONE);
  }

  @Override
  protected Label createIcon(Composite parent) {
    Label label = super.createIcon(parent);
    new MoveTracker(label) {

      @Override
      protected void handleClick(int x, int y) {
        super.handleClick(x, y);
        location = popup.getShell().getLocation();
      }

      @Override
      protected void handleDrag(int dx, int dy) {
        super.handleDrag(dx, dy);
        popup.getShell().setLocation(location.x + dx, location.y + dy);
      }

      private Point location;

    };
    return label;
  }

  @Override
  protected void setBackground(boolean found) {
    popup.setBackground(found);
  }

  @Override
  public void finished() {
  }

  @Override
  public void closePanel() {
    popup.close();
  }

  @Override
  protected void showSettings() {
    super.showSettings();
    // popup.showDialogMenu();
  }

  private Point getTargetLocation() {
    Shell shell = target.getShell();
    Display display = target.getDisplay();
    Point location = target.getLocation();
    location = display.map(target.getParent(), shell, location);
    return shell.toDisplay(location);
  }

  private final SearchPopup popup;
  private final Control target;

  private class SearchPopup extends SearchDialog {

    /**
     * @param parent
     */
    public SearchPopup(Shell parent) {
      super(parent);
    }

    @Override
    protected Control createTitleMenuArea(Composite parent) {
      PopupSearchDialog.this.createContent(parent);
      Control control = PopupSearchDialog.this.getControl();
      control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      return control;
    }

    @Override
    public void showDialogMenu() {
      super.showDialogMenu();
    }

    @Override
    protected void fillDialogMenu(IMenuManager dialogMenu) {
      PopupSearchDialog.this.fillMenu(dialogMenu);
    }

    @Override
    protected void handleClose() {
      super.handleClose();
      fireClose();
    }

    @Override
    protected Point getInitialSize() {
      return new Point(getPreferedWidth(), super.getInitialSize().y);
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
      Point location = getTargetLocation();
      Point size = target.getSize();
      int x = location.x + size.x / 2 - initialSize.x / 2;
      int y = location.y + size.y;
      Rectangle bounds = target.getMonitor().getBounds();
      if (y + initialSize.y > bounds.y + bounds.height) {
        y = location.y - initialSize.y;
      }
      return new Point(x, y);
    }

    @Override
    protected Control getFocusControl() {
      return PopupSearchDialog.this.title;
    }

    @Override
    protected List<Control> getBackgroundColorExclusions() {
      List<Control> list = super.getBackgroundColorExclusions();
      list.add(PopupSearchDialog.this.title);
      return list;
    }

    public void setBackground(boolean found) {
      Color color = found ? getBackground() : BAD_COLOR;
      applyBackgroundColor(color);
    }

  }

}
