/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring David Schneider,
 * david.schneider@unisys.com - [142500] WTP properties pages fonts don't follow Eclipse preferences
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.PageBook;
import org.eclipse.wst.common.ui.internal.viewers.SelectSingleFileView;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

public class SelectFileOrXMLCatalogIdPanel extends Composite implements SelectionListener {

  /**
   * TODO: Change the name of this interface; "Listener" is used by SWT.
   */
  public interface Listener {
    void completionStateChanged();
  }

  protected class MySelectSingleFileView extends SelectSingleFileView implements
      SelectSingleFileView.Listener {
    protected Control control;

    public MySelectSingleFileView(Composite parent) {
      super(null, true);
      // String[] ext = {".dtd"};
      // addFilterExtensions(ext);
      control = createControl(parent);
      control.setLayoutData(new GridData(GridData.FILL_BOTH));
      MySelectSingleFileView.this.setListener(this);
    }

    public Control getControl() {
      return control;
    }

    public void setControlComplete(boolean isComplete) {
      updateCompletionStateChange();
    }

    public void setVisibleHelper(boolean isVisible) {
      super.setVisibleHelper(isVisible);
    }
  }

  protected Listener listener;
  protected PageBook pageBook;

  protected Button[] radioButton;
  protected MySelectSingleFileView selectSingleFileView;
  protected SelectXMLCatalogIdPanel selectXMLCatalogIdPanel;

  public SelectFileOrXMLCatalogIdPanel(Composite parent) {
    super(parent, SWT.NONE);

    // container group
    setLayout(new GridLayout());
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = 400;
    gd.widthHint = 400;
    setLayoutData(gd);

    radioButton = new Button[2];
    radioButton[0] = new Button(this, SWT.RADIO);
    radioButton[0].setText(XMLUIMessages._UI_RADIO_BUTTON_SELECT_FROM_WORKSPACE);
    radioButton[0].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    radioButton[0].setSelection(true);
    radioButton[0].addSelectionListener(this);

    radioButton[1] = new Button(this, SWT.RADIO);
    radioButton[1].setText(XMLUIMessages._UI_RADIO_BUTTON_SELECT_FROM_CATALOG);
    radioButton[1].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    radioButton[1].addSelectionListener(this);

    pageBook = new PageBook(this, SWT.NONE);
    pageBook.setLayoutData(new GridData(GridData.FILL_BOTH));

    selectSingleFileView = new MySelectSingleFileView(pageBook);

    ICatalog xmlCatalog = XMLCorePlugin.getDefault().getDefaultXMLCatalog();
    selectXMLCatalogIdPanel = new SelectXMLCatalogIdPanel(pageBook, xmlCatalog);
    selectXMLCatalogIdPanel.getTableViewer().addSelectionChangedListener(
        new ISelectionChangedListener() {
          public void selectionChanged(SelectionChangedEvent event) {
            updateCompletionStateChange();
          }
        });
    Dialog.applyDialogFont(parent);
    pageBook.showPage(selectSingleFileView.getControl());

  }

  public IFile getFile() {
    IFile result = null;
    if (radioButton[0].getSelection()) {
      result = selectSingleFileView.getFile();
    }
    return result;
  }

  public ICatalogEntry getXMLCatalogEntry() {
    ICatalogEntry result = null;
    if (radioButton[1].getSelection()) {
      result = selectXMLCatalogIdPanel.getXMLCatalogEntry();
    }
    return result;
  }

  public String getXMLCatalogId() {
    String result = null;
    if (radioButton[1].getSelection()) {
      result = selectXMLCatalogIdPanel.getId();
    }
    return result;
  }

  public String getXMLCatalogURI() {
    String result = null;
    if (radioButton[1].getSelection()) {
      result = selectXMLCatalogIdPanel.getURI();
    }
    return result;
  }

  public void setCatalogEntryType(int catalogEntryType) {
    selectXMLCatalogIdPanel.setCatalogEntryType(catalogEntryType);
  }

  public void setFilterExtensions(String[] filterExtensions) {
    selectSingleFileView.resetFilters();
    selectSingleFileView.addFilterExtensions(filterExtensions);

    selectXMLCatalogIdPanel.getTableViewer().setFilterExtensions(filterExtensions);
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setVisibleHelper(boolean isVisible) {
    selectSingleFileView.setVisibleHelper(isVisible);
  }

  public void updateCompletionStateChange() {
    if (listener != null) {
      listener.completionStateChanged();
    }
  }

  public void widgetDefaultSelected(SelectionEvent e) {
  }

  public void widgetSelected(SelectionEvent e) {
    if (e.widget == radioButton[0]) {
      pageBook.showPage(selectSingleFileView.getControl());
    } else {
      pageBook.showPage(selectXMLCatalogIdPanel);
    }
    updateCompletionStateChange();
  }
}
