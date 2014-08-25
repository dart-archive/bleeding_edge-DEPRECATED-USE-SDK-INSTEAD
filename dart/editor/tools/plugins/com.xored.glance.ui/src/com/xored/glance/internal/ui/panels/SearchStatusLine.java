/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui.panels;

import com.xored.glance.ui.panels.SearchPanel;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.utils.UIUtils;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.IStatusField;
import org.eclipse.ui.texteditor.IStatusFieldExtension;

/**
 * @author Yuri Strot
 */
@SuppressWarnings("restriction")
public class SearchStatusLine extends SearchPanel {

  private class SearchItem extends ContributionItem implements IStatusField, IStatusFieldExtension {

    @Override
    public void dispose() {
      try {
        fireClose();
      } catch (SWTException ex) {
        // ignore it -- window closing
      }
    }

    @Override
    public void fill(Composite parent) {
      Label separator = new Label(parent, SWT.SEPARATOR);
      createContent(parent);
      setLayoutData(separator);
    }

    public SearchStatusLine getSearchPanel() {
      return SearchStatusLine.this;
    }

    @Override
    public void setErrorImage(Image image) {
      setImage(image);
    }

    @Override
    public void setErrorText(String text) {
      setText(text);
    }

    @Override
    public void setImage(Image image) {
    }

    @Override
    public void setText(String text) {
    }

    @Override
    public void setToolTipText(String string) {
      setText(string);
    }

  }

  private static final String DEFAULT_MATCH_LABEL = "no matches";

  public static SearchStatusLine getSearchLine(IWorkbenchWindow window) {
    IStatusLineManager manager = getManager(window);
    if (manager != null) {
      IContributionItem[] items = manager.getItems();
      for (IContributionItem item : items) {
        if (item instanceof SearchItem) {
          return ((SearchItem) item).getSearchPanel();
        }
      }
    }
    return new SearchStatusLine(window);
  }

  public static IWorkbenchWindow getWindow(Control control) {
    Shell shell = control.getShell();
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      if (shell.equals(window.getShell())) {
        return window;
      }
    }
    return null;
  }

  private static IStatusLineManager getManager(IWorkbenchWindow window) {
    if (window != null) {
      WorkbenchWindow ww = (WorkbenchWindow) window;
      return ww.getActionBars().getStatusLineManager();
    }
    return null;
  }

  private int matchCount;
  private String matchText = DEFAULT_MATCH_LABEL;
  private CLabel matchLabel;
  private int matchIndex;
  private SearchItem item;
  private final IWorkbenchWindow window;

  private SearchStatusLine(IWorkbenchWindow window) {
    this.window = window;
    init();
  }

  @Override
  public void allFound(final Match[] matches) {
    super.allFound(matches);
    matchCount = matches.length;
    updateInfo();
  }

  @Override
  public void clearStatus() {
    matchCount = matchIndex = 0;
    updateInfo();
  }

  @Override
  public void closePanel() {
    if (item != null) {
      fireClose();
      IStatusLineManager manager = getManager();
      if (manager != null) {
        manager.remove(item);
        manager.update(false);
      }
      item = null;
    }
  }

  @Override
  public void createContent(Composite parent) {
    super.createContent(parent);
    StatusLineLayoutData data = new StatusLineLayoutData();
    data.widthHint = getPreferedWidth();
    data.heightHint = getPreferredHeight();
    getControl().setLayoutData(data);
    createMatchLabel(parent);
  }

  public IWorkbenchWindow getWindow() {
    return window;
  }

  @Override
  public boolean isApplicable(Control control) {
    return window.equals(getWindow(control));
  }

  @Override
  public void setMatchIndex(int index) {
    matchIndex = index;
    updateInfo();
  }

  @Override
  protected Control createText(Composite parent, int style) {
    Control textControl = super.createText(parent, style);
    textControl.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        setKeyFilter(false);
        updateSelection();
      }

      @Override
      public void focusLost(FocusEvent e) {
        setKeyFilter(true);
      }
    });
    textControl.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        setKeyFilter(true);
      }
    });
    return textControl;
  }

  protected void setKeyFilter(boolean enabled) {
    IBindingService service = (IBindingService) PlatformUI.getWorkbench().getService(
        IBindingService.class);
    if (service != null) {
      service.setKeyFilterEnabled(enabled);
    }
  }

  @Override
  protected void textEmpty() {
    super.textEmpty();
    matchText = DEFAULT_MATCH_LABEL;
    matchLabel.setText(matchText);
  }

  private void createMatchLabel(Composite parent) {
    Label separator = new Label(parent, SWT.SEPARATOR);
    setLayoutData(separator);
    matchLabel = new CLabel(parent, SWT.SHADOW_NONE);
    matchLabel.setData("name", "searchStatus");
    StatusLineLayoutData data = new StatusLineLayoutData();
    data.widthHint = getTextWidth(parent, 10) + 15;
    data.heightHint = getPreferredHeight();
    matchLabel.setLayoutData(data);
    matchLabel.setText(matchText);
  }

  private IStatusLineManager getManager() {
    return getManager(window);
  }

  private void init() {
    item = new SearchItem();
    IStatusLineManager manager = getManager();
    if (manager != null) {
      manager.remove(item);
      manager.appendToGroup(StatusLineManager.BEGIN_GROUP, item);
      manager.update(true);
    }
  }

  private void setLayoutData(Label separator) {
    StatusLineLayoutData data = new StatusLineLayoutData();
    data.heightHint = getPreferredHeight();
    separator.setLayoutData(data);
  }

  private void updateInfo() {
    StringBuffer buffer = new StringBuffer();

    if (matchCount == 0) {
      buffer.append(DEFAULT_MATCH_LABEL);
    } else {
      if (matchIndex > 0) {
        buffer.append(matchIndex);
        buffer.append(" / ");
      }
      buffer.append(matchCount);
    }

    matchText = buffer.toString();
    UIUtils.asyncExec(matchLabel, new Runnable() {

      @Override
      public void run() {
        matchLabel.setText(matchText);
      }
    });
  }

}
