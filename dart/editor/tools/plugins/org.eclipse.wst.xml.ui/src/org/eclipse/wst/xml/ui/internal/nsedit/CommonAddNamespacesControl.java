/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.nsedit;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.PageBook;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolverPlugin;
import org.eclipse.wst.common.uriresolver.internal.util.URIHelper;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.dialogs.SelectFileOrXMLCatalogIdDialog;

public class CommonAddNamespacesControl extends Composite implements SelectionListener {

  class EditNamespaceControl extends Composite {
    protected Button browseButton;
    Text locationHintField;
    Text prefixField;
    Text uriField;

    // protected NamespaceInfo info;

    public EditNamespaceControl(Composite parent) {
      super(parent, SWT.NONE); // BORDER);
      setLayout(new GridLayout());
      setLayoutData(new GridData(GridData.FILL_BOTH));

      Label label = new Label(this, SWT.NONE);
      label.setText(XMLUIMessages._UI_ENTER_REQ_PREFIX_AND_NAMESPACE);

      Composite composite = new Composite(this, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.numColumns = 3;
      layout.marginWidth = 0;
      layout.verticalSpacing = 1;
      composite.setLayout(layout);

      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.widthHint = 350;
      composite.setLayoutData(gd);

      // row 1
      //
      Label prefixLabel = new Label(composite, SWT.NONE);
      prefixLabel.setText(XMLUIMessages._UI_LABEL_PREFIX_COLON);

      prefixField = new Text(composite, SWT.SINGLE | SWT.BORDER);
      prefixField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      // prefixField.setText(getDisplayValue(info.prefix));
      // prefixField.addModifyListener(modifyListener);
      // prefixField.setEnabled(info.getProperty("prefix-readOnly") ==
      // null);
      new Label(composite, SWT.NONE);

      // row 2
      //
      Label uriLabel = new Label(composite, SWT.NONE);
      uriLabel.setText(XMLUIMessages._UI_LABEL_NAMESPACE_NAME_COLON);

      uriField = new Text(composite, SWT.SINGLE | SWT.BORDER);
      uriField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      // uriField.setText(getDisplayValue(info.uri));
      // uriField.addModifyListener(modifyListener);
      // uriField.setEnabled(info.getProperty("uri-readOnly") == null);

      new Label(composite, SWT.NONE);

      // row 3
      //
      Label locationHintLabel = new Label(composite, SWT.NONE);
      locationHintLabel.setText(XMLUIMessages._UI_LABEL_LOCATION_HINT_COLON);

      locationHintField = new Text(composite, SWT.SINGLE | SWT.BORDER);
      locationHintField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      // locationHintField.setText(getDisplayValue(info.locationHint));
      // locationHintField.addModifyListener(modifyListener);
      // locationHintField.setEnabled(info.getProperty("locationHint-readOnly")
      // == null);

      SelectionListener selectionListener = new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          performBrowse();
        }
      };

      browseButton = new Button(composite, SWT.NONE);
      browseButton.setText(XMLUIMessages._UI_LABEL_BROWSE);
      browseButton.addSelectionListener(selectionListener);
      browseButton.setEnabled(locationHintField.getEnabled());
    }

    protected void performBrowse() {
      String[] extensions = {".xsd"}; //$NON-NLS-1$
      SelectFileOrXMLCatalogIdDialog dialog = new SelectFileOrXMLCatalogIdDialog(getShell(),
          extensions);
      dialog.create();
      dialog.getShell().setText(XMLUIMessages._UI_LABEL_SELECT_FILE);
      dialog.setBlockOnOpen(true);
      dialog.open();

      if (dialog.getReturnCode() == Window.OK) {
        // String grammarURI = null;
        IFile file = dialog.getFile();
        String id = dialog.getId();
        if (file != null) {
          String uri = null;
          if (resourceLocation != null) {
            IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(
                resourceLocation);
            if (resource != null) {
              IPath location = resource.getLocation();
              if (location != null) {
                uri = URIHelper.getRelativeURI(file.getLocation(), location);
              }
            } else {
              uri = URIHelper.getRelativeURI(file.getLocation(), resourceLocation);
            }
            // grammarURI = file.getLocation().toOSString();
          } else {
            uri = file.getLocation().toOSString();
            // grammarURI = uri;
          }
          locationHintField.setText(uri);
        } else if (id != null) {
          locationHintField.setText(id);
          // URIResolver resolver =
          URIResolverPlugin.createResolver();
          // grammarURI = resolver.resolve(null, id, id);
        }

        try {
          // TODO CMDocument document =
          // CMDocumentBuilderRegistry.getInstance().buildCMDocument(grammarURI);
          // List namespaceInfoList =
          // (List)document.getProperty("http://org.eclipse.wst/cm/properties/namespaceInfo");
          // NamespaceInfo info =
          // (NamespaceInfo)namespaceInfoList.get(0);
          // if (uriField.getText().trim().length() == 0 && info.uri
          // != null)
          // {
          // uriField.setText(info.uri);
          // }
          // if (prefixField.getText().trim().length() == 0 &&
          // info.prefix != null)
          // {
          // prefixField.setText(info.prefix);
          // }
        } catch (Exception e) {
        }
      }
    }
  }

  protected Button deleteButton;
  protected Button editButton;
  protected EditNamespaceControl editNamespaceControl;
  protected int heightHint = 250;
  protected List namespaceInfoList = new ArrayList();
  protected Button newButton;
  protected PageBook pageBook;
  protected Button radio1;
  protected Button radio2;
  protected IPath resourceLocation;
  protected Composite tableSection;
  protected CommonNamespaceInfoTable tableViewer;
  protected int widthHint = 500;

  public CommonAddNamespacesControl(Composite parent, int style, IPath resourceLocation) {
    super(parent, style);
    this.resourceLocation = resourceLocation;
    GridData gd = new GridData(GridData.FILL_BOTH);
    if (widthHint != -1) {
      gd.widthHint = widthHint;
    }
    if (heightHint != -1) {
      gd.heightHint = heightHint;
    }
    setLayoutData(gd);
    setLayout(new GridLayout());

    radio1 = new Button(this, SWT.RADIO);
    radio1.setText(XMLUIMessages._UI_SELECT_REGISTERED_NAMESPACES);
    radio1.setSelection(true);
    radio1.addSelectionListener(this);

    radio2 = new Button(this, SWT.RADIO);
    radio2.setText(XMLUIMessages._UI_SPECIFY_NEW_NAMESPACE);
    radio2.addSelectionListener(this);

    Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    // Group namespaceInfoGroup = new Group(this, SWT.NONE);
    // namespaceInfoGroup.setText("Namespace Declarations");
    // //XMLCommonUIPlugin.getInstance().getString("_UI_LABEL_XML_SCHEMA_INFORMATION"));
    // namespaceInfoGroup.setLayout(new GridLayout(2, false));
    // namespaceInfoGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
    pageBook = new PageBook(this, SWT.NONE);
    pageBook.setLayoutData(new GridData(GridData.FILL_BOTH));

    tableSection = new Composite(pageBook, SWT.NONE);
    tableSection.setLayout(new GridLayout());
    Label label = new Label(tableSection, SWT.NONE);
    label.setText(XMLUIMessages._UI_SELECT_NAMESPACE_TO_ADD);

    tableViewer = new CommonNamespaceInfoTable(tableSection, SWT.CHECK, 6);
    editNamespaceControl = new EditNamespaceControl(pageBook);
    pageBook.showPage(tableSection);

    tableViewer.setInput(namespaceInfoList);
  }

  public List getNamespaceInfoList() {
    List list = new ArrayList();
    if (radio1.getSelection()) {
      TableItem[] items = tableViewer.getTable().getItems();
      for (int i = 0; i < items.length; i++) {
        TableItem item = items[i];
        if (item.getChecked()) {
          list.add(item.getData());
        }
      }
    } else {
      NamespaceInfo info = new NamespaceInfo();
      info.prefix = editNamespaceControl.prefixField.getText();
      info.uri = editNamespaceControl.uriField.getText();
      info.locationHint = editNamespaceControl.locationHintField.getText();
      list.add(info);
    }
    return list;
  }

  public void setNamespaceInfoList(List list) {
    namespaceInfoList = list;
    tableViewer.setInput(namespaceInfoList);
  }

  public void widgetDefaultSelected(SelectionEvent e) {
  }

  public void widgetSelected(SelectionEvent e) {
    if (e.widget == radio1) {
      pageBook.showPage(tableSection);
    } else if (e.widget == radio2) {
      pageBook.showPage(editNamespaceControl);
    }
  }
}
