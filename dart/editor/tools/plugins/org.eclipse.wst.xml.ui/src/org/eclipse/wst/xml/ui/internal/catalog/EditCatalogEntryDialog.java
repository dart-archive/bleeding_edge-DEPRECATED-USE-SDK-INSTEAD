/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.PageBook;
import org.eclipse.wst.common.ui.internal.dialogs.SelectSingleFileDialog;
import org.eclipse.wst.common.uriresolver.internal.URI;
import org.eclipse.wst.common.uriresolver.internal.util.URIHelper;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogElement;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.core.internal.catalog.provisional.IDelegateCatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.INextCatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.IRewriteEntry;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ISuffixEntry;

public class EditCatalogEntryDialog extends Dialog {
  protected static Image borwseImage = ImageFactory.INSTANCE.getImage("icons/obj16/file_expand.gif"); //$NON-NLS-1$
  protected static Image catalogEntryToolBarImage = ImageFactory.INSTANCE.getImage("icons/etool50/catalogEntry.gif"); //$NON-NLS-1$
  protected static Image nextCatalogToolBarImage = ImageFactory.INSTANCE.getImage("icons/etool50/nextCatalog.gif"); //$NON-NLS-1$
  protected static Image delegateCatalogToolBarImage = ImageFactory.INSTANCE.getImage("icons/etool50/delegateCatalog.gif"); //$NON-NLS-1$
  protected static Image rewriteToolBarImage = ImageFactory.INSTANCE.getImage("icons/etool50/rewrite.gif"); //$NON-NLS-1$
  protected static Image prefixToolBarImage = ImageFactory.INSTANCE.getImage("icons/etool50/prefix.gif"); //$NON-NLS-1$
  protected static Image suffixToolBarImage = ImageFactory.INSTANCE.getImage("icons/etool50/sufix.gif"); //$NON-NLS-1$

  protected class CatalogEntryPage extends CatalogElementPage {

    protected Button browseWorkspaceButton;

    protected Button browseFileSystemButton;

    protected ICatalogEntry catalogEntry;

    protected Button checkboxButton;

    protected Label errorMessageLabel;

    protected Text keyField;

    protected Combo keyTypeCombo;

    protected Text resourceLocationField;

    protected Combo resourceTypeCombo;

    protected Text webAddressField;

    protected String key;

    protected int type;

    public void refresh() {
      computeErrorMessage();
      updateErrorMessageLabel(errorMessageLabel);
      updateOKButtonState();
    }

    protected void computeErrorMessage() {
      errorMessage = null;
      warningMessage = null;

      String fileName = resourceLocationField.getText();
      if (fileName.trim().length() > 0) {
        if ((fileName.indexOf("..") != -1) || (fileName.indexOf("./") != -1) || (fileName.indexOf("/.") != -1) || (fileName.indexOf(".\\") != -1) || (fileName.indexOf("\\.") != -1)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
          errorMessage = XMLCatalogMessages.UI_WARNING_URI_MUST_NOT_HAVE_DOTS;
        }

        String uri = fileName;
        if (!URIHelper.hasProtocol(uri)) {
          URIHelper.isAbsolute(uri);
          uri = (URIHelper.isAbsolute(uri)) ? URIHelper.prependFileProtocol(uri)
              : URIHelper.prependPlatformResourceProtocol(uri);
        }

        if ((errorMessage == null) && !URIHelper.isReadableURI(uri, false)) {
          errorMessage = XMLCatalogMessages.UI_WARNING_URI_NOT_FOUND_COLON + fileName;
        }
      } else {
        // this an error that is not actaully
        // reported ... OK is just disabled
        errorMessage = ""; //$NON-NLS-1$
      }

      // Make sure the key is a fully qualified URI in the cases
      // where the key type is "System ID" or "Schema location"
      if ((keyField.getText().length() > 0) && (getKeyType() == ICatalogEntry.ENTRY_TYPE_SYSTEM)) {
        URI uri = URI.createURI(keyField.getText());
        if (uri.scheme() == null) {
          warningMessage = XMLCatalogMessages.UI_WARNING_SHOULD_BE_FULLY_QUALIFIED_URI;
        }
      }

      if ((errorMessage == null) && checkboxButton.getSelection()
          && (webAddressField.getText().trim().length() == 0)) {
        // this an error that is not actaully
        // reported ... OK is just disabled
        errorMessage = ""; //$NON-NLS-1$
      }

      if ((errorMessage == null) && (keyField.getText().trim().length() == 0)) {
        // this an error that is not actaully
        // reported ... OK is just disabled
        errorMessage = ""; //$NON-NLS-1$
      }
    }

    protected Control createCatalogEntryPanel(Composite parent) {

      ModifyListener modifyListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          if (e.widget == resourceLocationField) {
            if (keyField.getText().length() == 0) {
              String uri = resourceLocationField.getText();
              if (uri.endsWith("xsd") && !URIHelper.hasProtocol(uri)) { //$NON-NLS-1$
                uri = URIHelper.isAbsolute(uri) ? URIHelper.prependFileProtocol(uri)
                    : URIHelper.prependPlatformResourceProtocol(uri);
                String namespaceURI = XMLQuickScan.getTargetNamespaceURIForSchema(uri);
                if (namespaceURI != null) {
                  keyField.setText(namespaceURI);
                }
              }
            }
          }
          updateWidgets(e.widget);
        }
      };

      Composite composite = new Composite(parent, SWT.NONE);
      GridData gd = new GridData(GridData.FILL_BOTH);
      composite.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      composite.setLayout(layout);

      Composite group = new Composite(composite, SWT.NONE);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);

      layout = new GridLayout(2, false);
      group.setLayout(layout);

      Label resourceLocationLabel = new Label(group, SWT.NONE);
      resourceLocationLabel.setText(XMLCatalogMessages.UI_LABEL_LOCATION_COLON);

      resourceLocationField = new Text(group, SWT.SINGLE | SWT.BORDER);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      resourceLocationField.setLayoutData(gd);

      resourceLocationField.setText(getDisplayValue(URIUtils.convertURIToLocation(getEntry().getURI())));

      // WorkbenchHelp.setHelp(resourceLocationField,
      // XMLBuilderContextIds.XMLP_ENTRY_URI);
      resourceLocationField.addModifyListener(modifyListener);

      // WorkbenchHelp.setHelp(browseButton,
      // XMLBuilderContextIds.XMLP_ENTRY_BROWSE);

      Composite browseButtonsComposite = new Composite(group, SWT.NONE);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 2;
      gd.horizontalAlignment = GridData.END;
      browseButtonsComposite.setLayoutData(gd);

      layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.marginBottom = 5;
      browseButtonsComposite.setLayout(layout);

      browseWorkspaceButton = new Button(browseButtonsComposite, SWT.PUSH);
      browseWorkspaceButton.setText(XMLCatalogMessages.UI_BUTTON_MENU_BROWSE_WORKSPACE);
      browseWorkspaceButton.addSelectionListener(new SelectionListener() {

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
          String value = invokeWorkspaceFileSelectionDialog();
          if (value != null) {
            resourceLocationField.setText(value);
          }
        }
      });

      browseFileSystemButton = new Button(browseButtonsComposite, SWT.PUSH);
      browseFileSystemButton.setText(XMLCatalogMessages.UI_BUTTON_MENU_BROWSE_FILE_SYSTEM);
      browseFileSystemButton.addSelectionListener(new SelectionListener() {

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
          String value = invokeFileSelectionDialog();
          if (value != null) {
            resourceLocationField.setText(value);
          }
        }
      });

      // Key Type
      //
      Label keyTypeLabel = new Label(group, SWT.NONE);
      keyTypeLabel.setText(XMLCatalogMessages.UI_KEY_TYPE_COLON);

      keyTypeCombo = new Combo(group, SWT.READ_ONLY);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      keyTypeCombo.setLayoutData(gd);
      updateKeyTypeCombo(getEntry().getEntryType());
      keyTypeCombo.addModifyListener(modifyListener);
      // WorkbenchHelp.setHelp(keyTypeCombo,
      // XMLBuilderContextIds.XMLP_ENTRY_KEY_TYPE);

      // Key
      // 
      Label keyValueLabel = new Label(group, SWT.NONE);
      keyValueLabel.setText(XMLCatalogMessages.UI_LABEL_KEY_COLON);
      keyField = new Text(group, SWT.SINGLE | SWT.BORDER);
      // WorkbenchHelp.setHelp(keyField,
      // XMLBuilderContextIds.XMLP_ENTRY_KEY);
      keyField.setLayoutData(gd);
      keyField.setText(getDisplayValue(getEntry().getKey()));
      keyField.addModifyListener(modifyListener);

      Composite group2 = new Composite(composite, SWT.NONE);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      group2.setLayoutData(gd);

      layout = new GridLayout();
      group2.setLayout(layout);

      // checkbox -- note parent is dialogArea
      //
      checkboxButton = new Button(group2, SWT.CHECK);
      // WorkbenchHelp.setHelp(checkboxButton,
      // XMLBuilderContextIds.XMLP_ENTRY_SPECIFY_ALTERNATIVE);
      checkboxButton.setText(XMLCatalogMessages.UI_LABEL_SPECIFY_ALTERNATIVE_WEB_URL);
      checkboxButton.setLayoutData(new GridData());
      checkboxButton.setSelection(getEntry().getAttributeValue(ICatalogEntry.ATTR_WEB_URL) != null);
      SelectionListener buttonListener = new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent event) {
          // no impl
        }

        public void widgetSelected(SelectionEvent event) {
          if (event.widget == checkboxButton) {
            updateWidgets(checkboxButton);
          }
        }
      };
      checkboxButton.addSelectionListener(buttonListener);

      // Web Address field
      //

      ModifyListener webAddressFieldListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          computeErrorMessage();
          updateErrorMessageLabel(errorMessageLabel);
          updateOKButtonState();
        }
      };

      webAddressField = new Text(group2, SWT.SINGLE | SWT.BORDER);
      // WorkbenchHelp.setHelp(webAddressField,
      // XMLBuilderContextIds.XMLP_ENTRY_WEB_ADDRESS);
      webAddressField.setLayoutData(gd);
      webAddressField.setText(getDisplayValue(getEntry().getAttributeValue(
          ICatalogEntry.ATTR_WEB_URL)));
      webAddressField.setEnabled(false);
      webAddressField.addModifyListener(webAddressFieldListener);

      errorMessageLabel = new Label(group2, SWT.NONE);
      errorMessageLabel.setForeground(color);
      errorMessageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      updateWidgets(null);

      key = getEntry().getKey();
      type = getEntry().getEntryType();

      return composite;
    }

    public Control createControl(Composite parent) {

      fControl = createCatalogEntryPanel(parent);

      return fControl;
    }

    public ICatalogElement getData() {
      return getEntry();
    }

    protected ICatalogEntry getEntry() {
      if (catalogEntry == null) {
        if ((fCatalogElement != null) && (fCatalogElement.getType() == ICatalogElement.TYPE_ENTRY)) {
          catalogEntry = (ICatalogEntry) fCatalogElement;
        } else {
          if (catalog != null) {
            catalogEntry = (ICatalogEntry) catalog.createCatalogElement(ICatalogElement.TYPE_ENTRY);
          }
        }
      }
      return catalogEntry;
    }

    protected int getKeyType() {
      switch (keyTypeCombo.getSelectionIndex()) {
        case 0:
          if ("schema".equals(keyTypeCombo.getData("keyType"))) { //$NON-NLS-1$ //$NON-NLS-2$
            return ICatalogEntry.ENTRY_TYPE_URI; // xsd
            // namespace
            // is URI type
            // key
          }
          return ICatalogEntry.ENTRY_TYPE_PUBLIC;
        case 1:
          return ICatalogEntry.ENTRY_TYPE_SYSTEM;
        case 2:
          return ICatalogEntry.ENTRY_TYPE_URI;
        default:
          return ICatalogEntry.ENTRY_TYPE_PUBLIC;
      }
    }

    public void saveData() {
      if (validateData()) {
        getEntry().setURI(URIUtils.convertLocationToURI(resourceLocationField.getText()));
        getEntry().setKey(keyField.getText());
        getEntry().setEntryType(getKeyType());
        getEntry().setAttributeValue(ICatalogEntry.ATTR_WEB_URL,
            checkboxButton.getSelection() ? webAddressField.getText() : null);
        dataSaved = true;
      } else {
        errorMessage = XMLCatalogMessages.UI_WARNING_DUPLICATE_ENTRY;
        errorMessageLabel.setText(errorMessage);
        updateOKButtonState();
        dataSaved = false;
      }
    }

    /**
     * Validates that the data entered does not conflict with an existing entry in either catalog.
     * 
     * @return True if validated, false otherwise.
     */
    protected boolean validateData() {

      String result = null;
      if (key == null || !key.equals(keyField.getText()) || type != getKeyType()) {
        try {
          switch (getKeyType()) {
            case ICatalogEntry.ENTRY_TYPE_PUBLIC:
              result = catalog.resolvePublic(keyField.getText(), null);
              break;
            case ICatalogEntry.ENTRY_TYPE_SYSTEM:
              result = catalog.resolveSystem(keyField.getText());
              break;
            case ICatalogEntry.ENTRY_TYPE_URI:
              result = catalog.resolveURI(keyField.getText());
              break;
          }
        } catch (Exception e) {
        }
      }

      return (result == null);
    }

    protected void updateKeyTypeCombo(int type) {
      keyTypeCombo.removeAll();
      for (Iterator i = CatalogFileTypeRegistryReader.getXMLCatalogFileTypes().iterator(); i.hasNext();) {
        XMLCatalogFileType theFileType = (XMLCatalogFileType) i.next();
        if (theFileType.extensions != null) {
          for (Iterator j = theFileType.extensions.iterator(); j.hasNext();) {
            String extension = (String) j.next();
            if (resourceLocationField.getText().endsWith(extension)) {
              if ("org.eclipse.wst.xml.core.ui.catalogFileType.xsd".equals(theFileType.id)) { //$NON-NLS-1$
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_XSD_PUBLIC);
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_XSD_SYSTEM);
                keyTypeCombo.setData("keyType", "schema"); //$NON-NLS-1$ //$NON-NLS-2$
              } else if ("org.eclipse.wst.xml.core.ui.catalogFileType.dtd".equals(theFileType.id)) { //$NON-NLS-1$
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_PUBLIC);
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM);
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI);
              } else {
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI);
              }
            }

          }

        }
      }
      if (keyTypeCombo.getItemCount() == 0) {
        keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_PUBLIC);
        keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM);
        keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI);
      }

      switch (type) {
        case ICatalogEntry.ENTRY_TYPE_PUBLIC:
          keyTypeCombo.select(0);
          break;
        case ICatalogEntry.ENTRY_TYPE_SYSTEM:
          keyTypeCombo.select(1);
          break;
        case ICatalogEntry.ENTRY_TYPE_URI: // handle XML Schema,
          // where namespace name is
          // mapped to URI situation
          if ("schema".equals(keyTypeCombo.getData("keyType"))) { //$NON-NLS-1$ //$NON-NLS-2$
            keyTypeCombo.select(0); // namespace name as URI key
            // type
          } else {
            keyTypeCombo.select(2); // URI key type
          }
          break;
        default:
          if (keyTypeCombo.getItemCount() > 0) {
            keyTypeCombo.select(0);
          }
          break;
      }

    }

    protected void updateWebAddressWidgets(int keyType) {
      boolean isPublicKeyType = (keyType == ICatalogEntry.ENTRY_TYPE_PUBLIC);
      checkboxButton.setEnabled(isPublicKeyType);
      webAddressField.setEnabled(isPublicKeyType && checkboxButton.getSelection());
    }

    protected void updateWidgets(Widget widget) {
      if (widget != keyTypeCombo) {
        updateKeyTypeCombo(getKeyType());
      }
      updateWebAddressWidgets(getKeyType());
      computeErrorMessage();
      updateErrorMessageLabel(errorMessageLabel);
      updateOKButtonState();
    }

  }

  protected class SuffixEntryPage extends CatalogElementPage {

    protected Button browseWorkspaceButton;

    protected Button browseFileSystemButton;

    protected ISuffixEntry catalogEntry;

    protected Label errorMessageLabel;

    protected Text suffixField;

    protected Combo keyTypeCombo;

    protected Text resourceLocationField;

    protected Combo resourceTypeCombo;

    protected String key;

    protected int type;

    public void refresh() {
      computeErrorMessage();
      updateErrorMessageLabel(errorMessageLabel);
      updateOKButtonState();
    }

    protected void computeErrorMessage() {
      errorMessage = null;
      warningMessage = null;

      String fileName = resourceLocationField.getText();
      if (fileName.trim().length() > 0) {
        if ((fileName.indexOf("..") != -1) || (fileName.indexOf("./") != -1) || (fileName.indexOf("/.") != -1) || (fileName.indexOf(".\\") != -1) || (fileName.indexOf("\\.") != -1)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
          errorMessage = XMLCatalogMessages.UI_WARNING_URI_MUST_NOT_HAVE_DOTS;
        }

        String uri = fileName;
        if (!URIHelper.hasProtocol(uri)) {
          URIHelper.isAbsolute(uri);
          uri = (URIHelper.isAbsolute(uri)) ? URIHelper.prependFileProtocol(uri)
              : URIHelper.prependPlatformResourceProtocol(uri);
        }

        if ((errorMessage == null) && !URIHelper.isReadableURI(uri, false)) {
          errorMessage = XMLCatalogMessages.UI_WARNING_URI_NOT_FOUND_COLON + fileName;
        }
      } else {
        // this an error that is not actaully
        // reported ... OK is just disabled
        errorMessage = ""; //$NON-NLS-1$
      }

      if ((errorMessage == null) && (suffixField.getText().trim().length() == 0)) {
        // this an error that is not actaully
        // reported ... OK is just disabled
        errorMessage = ""; //$NON-NLS-1$
      }
    }

    protected Control createSuffixEntryPanel(Composite parent) {

      ModifyListener modifyListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          if (e.widget == resourceLocationField) {
            if (suffixField.getText().length() == 0) {
              String uri = resourceLocationField.getText();
              if (uri.endsWith("xsd") && !URIHelper.hasProtocol(uri)) { //$NON-NLS-1$
                uri = URIHelper.isAbsolute(uri) ? URIHelper.prependFileProtocol(uri)
                    : URIHelper.prependPlatformResourceProtocol(uri);
                String namespaceURI = XMLQuickScan.getTargetNamespaceURIForSchema(uri);
                if (namespaceURI != null) {
                  suffixField.setText(namespaceURI);
                }
              }
            }
          }
          updateWidgets(e.widget);
        }
      };

      Composite composite = new Composite(parent, SWT.NONE);
      GridData gd = new GridData(GridData.FILL_BOTH);
      composite.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      composite.setLayout(layout);

      Composite group = new Composite(composite, SWT.NONE);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);

      layout = new GridLayout(2, false);
      group.setLayout(layout);

      Label resourceLocationLabel = new Label(group, SWT.NONE);
      resourceLocationLabel.setText(XMLCatalogMessages.UI_LABEL_LOCATION_COLON);

      resourceLocationField = new Text(group, SWT.SINGLE | SWT.BORDER);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      resourceLocationField.setLayoutData(gd);

      resourceLocationField.setText(getDisplayValue(URIUtils.convertURIToLocation(getEntry().getURI())));
      resourceLocationField.addModifyListener(modifyListener);

      Composite browseButtonsComposite = new Composite(group, SWT.NONE);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 2;
      gd.horizontalAlignment = GridData.END;
      browseButtonsComposite.setLayoutData(gd);

      layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.marginBottom = 5;
      browseButtonsComposite.setLayout(layout);

      browseWorkspaceButton = new Button(browseButtonsComposite, SWT.PUSH);
      browseWorkspaceButton.setText(XMLCatalogMessages.UI_BUTTON_MENU_BROWSE_WORKSPACE);
      browseWorkspaceButton.addSelectionListener(new SelectionListener() {

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
          String value = invokeWorkspaceFileSelectionDialog();
          if (value != null) {
            resourceLocationField.setText(value);
          }
        }
      });

      browseFileSystemButton = new Button(browseButtonsComposite, SWT.PUSH);
      browseFileSystemButton.setText(XMLCatalogMessages.UI_BUTTON_MENU_BROWSE_FILE_SYSTEM);
      browseFileSystemButton.addSelectionListener(new SelectionListener() {

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
          String value = invokeFileSelectionDialog();
          if (value != null) {
            resourceLocationField.setText(value);
          }
        }
      });

      // Key Type
      //
      Label keyTypeLabel = new Label(group, SWT.NONE);
      keyTypeLabel.setText(XMLCatalogMessages.UI_KEY_TYPE_COLON);

      keyTypeCombo = new Combo(group, SWT.READ_ONLY);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      keyTypeCombo.setLayoutData(gd);
      updateKeyTypeCombo(getEntry().getEntryType());
      keyTypeCombo.addModifyListener(modifyListener);

      // Suffix
      // 
      Label suffixValueLabel = new Label(group, SWT.NONE);
      suffixValueLabel.setText(XMLCatalogMessages.UI_LABEL_SUFFIX_COLON);
      suffixField = new Text(group, SWT.SINGLE | SWT.BORDER);

      suffixField.setLayoutData(gd);
      suffixField.setText(getDisplayValue(getEntry().getSuffix()));
      suffixField.addModifyListener(modifyListener);

      errorMessageLabel = new Label(composite, SWT.NONE);
      errorMessageLabel.setForeground(color);
      errorMessageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      updateWidgets(null);

      key = getEntry().getSuffix();
      type = getEntry().getEntryType();

      return composite;
    }

    public Control createControl(Composite parent) {

      fControl = createSuffixEntryPanel(parent);

      return fControl;
    }

    public ICatalogElement getData() {
      return getEntry();
    }

    protected ISuffixEntry getEntry() {
      if (catalogEntry == null) {
        if ((fCatalogElement != null) && (fCatalogElement.getType() == ICatalogElement.TYPE_SUFFIX)) {
          catalogEntry = (ISuffixEntry) fCatalogElement;
        } else {
          if (catalog != null) {
            catalogEntry = (ISuffixEntry) catalog.createCatalogElement(ICatalogElement.TYPE_SUFFIX);
          }
        }
      }
      return catalogEntry;
    }

    protected int getKeyType() {
      switch (keyTypeCombo.getSelectionIndex()) {
        case 0:
          return ISuffixEntry.SUFFIX_TYPE_URI; // xsd namespace is URI type key
        case 1:
          return ISuffixEntry.SUFFIX_TYPE_SYSTEM;
        default:
          return ISuffixEntry.SUFFIX_TYPE_URI;
      }
    }

    public void saveData() {
      if (validateData()) {
        getEntry().setURI(URIUtils.convertLocationToURI(resourceLocationField.getText()));
        getEntry().setSuffix(suffixField.getText());
        getEntry().setEntryType(getKeyType());
        dataSaved = true;
      } else {
        errorMessage = XMLCatalogMessages.UI_WARNING_DUPLICATE_SUFFIX;
        errorMessageLabel.setText(errorMessage);
        updateOKButtonState();
        dataSaved = false;
      }
    }

    protected boolean validateData() {
      ISuffixEntry entry = getEntry();
      String uri = URIUtils.convertLocationToURI(resourceLocationField.getText());
      if (entry.getEntryType() != getKeyType() || !entry.getSuffix().equals(suffixField.getText())
          || !entry.getURI().equals(uri)) {
        ISuffixEntry[] entries = catalog.getSuffixEntries();
        for (int i = 0; i < entries.length; i++) {
          if (entries[i].getSuffix().equals(suffixField.getText())
              && entries[i].getEntryType() == getKeyType())
            return false;
        }
      }
      return true;
    }

    protected void updateKeyTypeCombo(int type) {
      keyTypeCombo.removeAll();
      for (Iterator i = CatalogFileTypeRegistryReader.getXMLCatalogFileTypes().iterator(); i.hasNext();) {
        XMLCatalogFileType theFileType = (XMLCatalogFileType) i.next();
        if (theFileType.extensions != null) {
          for (Iterator j = theFileType.extensions.iterator(); j.hasNext();) {
            String extension = (String) j.next();
            if (resourceLocationField.getText().endsWith(extension)) {
              if ("org.eclipse.wst.xml.core.ui.catalogFileType.xsd".equals(theFileType.id)) { //$NON-NLS-1$
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_XSD_PUBLIC);
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_XSD_SYSTEM);
              } else if ("org.eclipse.wst.xml.core.ui.catalogFileType.dtd".equals(theFileType.id)) { //$NON-NLS-1$
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI);
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM);
              } else {
                keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI);
              }
            }

          }

        }
      }
      if (keyTypeCombo.getItemCount() == 0) {
        keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI);
        keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM);
      }

      switch (type) {
        case ISuffixEntry.SUFFIX_TYPE_URI: // handle XML Schema,
          keyTypeCombo.select(0); // namespace name as URI key
          break;

        case ISuffixEntry.SUFFIX_TYPE_SYSTEM:
          keyTypeCombo.select(1);
          break;

        default:
          if (keyTypeCombo.getItemCount() > 0) {
            keyTypeCombo.select(0);
          }
          break;
      }

    }

    protected void updateWidgets(Widget widget) {
      if (widget != keyTypeCombo) {
        updateKeyTypeCombo(getKeyType());
      }
      computeErrorMessage();
      updateErrorMessageLabel(errorMessageLabel);
      updateOKButtonState();
    }

  }

  protected abstract class CatalogElementPage {

    Control fControl;

    public CatalogElementPage() {
      super();

    }

    public abstract void refresh();

    public abstract Control createControl(Composite parent);

    public Control getControl() {
      return fControl;
    }

    public abstract ICatalogElement getData();

    public abstract void saveData();
  }

  protected class FilterableSelectSingleFileDialog extends SelectSingleFileDialog implements
      SelectionListener {
    protected Combo filterControl;

    public FilterableSelectSingleFileDialog(Shell parentShell) {
      super(parentShell, null, true);
      setFilters(null);
    }

    public void createAndOpen() {
      this.create();
      setBlockOnOpen(true);
      getShell().setText(XMLCatalogMessages.UI_LABEL_FILE_SELECTION);
      this.setTitle(XMLCatalogMessages.UI_LABEL_SELECT_FILE);
      this.setMessage(XMLCatalogMessages.UI_LABEL_CHOOSE_FILE_TO_ADD_TO_CATALOG);
      open();
    }

    public void createFilterControl(Composite composite) {
      Label label = new Label(composite, SWT.NONE);
      label.setText(XMLCatalogMessages.UI_LABEL_SELECT_FILE_FILTER_CONTROL);

      filterControl = new Combo(composite, SWT.READ_ONLY);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      filterControl.setLayoutData(gd);

      filterControl.setText(XMLCatalogMessages.UI_TEXT_SELECT_FILE_FILTER_CONTROL);
      filterControl.add(XMLCatalogMessages.UI_TEXT_SELECT_FILE_FILTER_CONTROL);

      for (Iterator i = CatalogFileTypeRegistryReader.getXMLCatalogFileTypes().iterator(); i.hasNext();) {
        XMLCatalogFileType fileType = (XMLCatalogFileType) i.next();
        if (fileType.description != null) {
          filterControl.add(fileType.description);
        }
      }

      filterControl.select(0);
      filterControl.addSelectionListener(this);
    }

    protected void setFilters(XMLCatalogFileType fileType) {
      if (fileType == null) {
        // compute all the supported file extensions
        List list = new ArrayList();
        for (Iterator i = CatalogFileTypeRegistryReader.getXMLCatalogFileTypes().iterator(); i.hasNext();) {
          XMLCatalogFileType theFileType = (XMLCatalogFileType) i.next();
          if (theFileType.extensions != null) {
            list.addAll(theFileType.extensions);
          }
        }
        // Any files are now supported with Resource URI
        selectSingleFileView.setFilterExtensions(createStringArray(list));
      } else {
        if (fileType.extensions != null) {
          selectSingleFileView.setFilterExtensions(createStringArray(fileType.extensions));
        }
      }
    }

    public void widgetDefaultSelected(SelectionEvent e) {
      // do nothing
    }

    public void widgetSelected(SelectionEvent e) {
      String text = filterControl.getText();
      XMLCatalogFileType fileType = getMatchingFileType(text);
      setFilters(fileType);
    }
  }

  protected abstract class AbstractDelegatePage extends CatalogElementPage {

    protected Button browseWorkspaceButton;

    protected Button browseFileSystemButton;

    protected Text catalogLocationField;

    protected Label errorMessageLabel;

    protected void computeErrorMessage() {
      errorMessage = null;
      String fileName = catalogLocationField.getText();
      if (fileName.trim().length() > 0) {
        if ((fileName.indexOf("..") != -1) || (fileName.indexOf("./") != -1) || (fileName.indexOf("/.") != -1) || (fileName.indexOf(".\\") != -1) || (fileName.indexOf("\\.") != -1)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
          errorMessage = XMLCatalogMessages.UI_WARNING_URI_MUST_NOT_HAVE_DOTS;
        }

        String uri = fileName;
        if (!URIHelper.hasProtocol(uri)) {
          uri = URIHelper.isAbsolute(uri) ? URIHelper.prependFileProtocol(uri)
              : URIHelper.prependPlatformResourceProtocol(uri);
        }

        if ((errorMessage == null) && !URIHelper.isReadableURI(uri, false)) {
          errorMessage = XMLCatalogMessages.UI_WARNING_URI_NOT_FOUND_COLON + fileName;
        }
      } else {
        // this an error that is not actually
        // reported ... OK is just disabled
        errorMessage = ""; //$NON-NLS-1$
      }
    }

    public abstract Control createControl(Composite parent);

    protected Control createNextCatalogPanel(Composite parent, String catalogUriLabel) {
      ModifyListener modifyListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          updateWidgets(e.widget);
        }
      };

      Composite composite = new Composite(parent, SWT.NONE);
      GridData gd = new GridData(GridData.FILL_BOTH);
      composite.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      composite.setLayout(layout);

      Composite group = new Composite(composite, SWT.NONE);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);

      layout = new GridLayout();
      group.setLayout(layout);

      createSpecificFields(group);

      Label resourceLocationLabel = new Label(group, SWT.NONE);
      resourceLocationLabel.setText(catalogUriLabel);

      catalogLocationField = new Text(group, SWT.SINGLE | SWT.BORDER);
      catalogLocationField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      catalogLocationField.setText(URIUtils.convertURIToLocation(getDisplayValue(getCatalogLocation())));
      // WorkbenchHelp.setHelp(resourceLocationField,
      // XMLBuilderContextIds.XMLP_ENTRY_URI);
      catalogLocationField.addModifyListener(modifyListener);

      Composite browseButtonsComposite = new Composite(group, SWT.FLAT);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 2;
      gd.horizontalAlignment = GridData.END;
      browseButtonsComposite.setLayoutData(gd);

      layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.marginBottom = 5;
      browseButtonsComposite.setLayout(layout);

      browseWorkspaceButton = new Button(browseButtonsComposite, SWT.PUSH);
      browseWorkspaceButton.setText(XMLCatalogMessages.UI_BUTTON_MENU_BROWSE_WORKSPACE);
      browseWorkspaceButton.addSelectionListener(new SelectionListener() {

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
          String value = invokeWorkspaceFileSelectionDialog();
          if (value != null) {
            catalogLocationField.setText(value);
          }
        }
      });

      browseFileSystemButton = new Button(browseButtonsComposite, SWT.PUSH);
      browseFileSystemButton.setText(XMLCatalogMessages.UI_BUTTON_MENU_BROWSE_FILE_SYSTEM);
      browseFileSystemButton.addSelectionListener(new SelectionListener() {

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
          String value = invokeFileSelectionDialog();
          if (value != null) {
            catalogLocationField.setText(value);
          }
        }
      });

      errorMessageLabel = new Label(group, SWT.NONE);
      errorMessageLabel.setForeground(color);
      errorMessageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      updateWidgets(null);
      return composite;
    }

    protected void createSpecificFields(Composite group) {
    }

    protected abstract String getCatalogLocation();

    protected void updateWidgets(Widget widget) {
      computeErrorMessage();
      updateErrorMessageLabel(errorMessageLabel);
      updateOKButtonState();
    }
  }

  protected class NextCatalogPage extends AbstractDelegatePage {
    protected INextCatalog nextCatalog;

    public ICatalogElement getData() {
      return getNextCatalog();
    }

    public void refresh() {
      computeErrorMessage();
      updateErrorMessageLabel(errorMessageLabel);
      updateOKButtonState();
    }

    protected INextCatalog getNextCatalog() {
      if (nextCatalog == null) {
        if ((fCatalogElement != null)
            && (fCatalogElement.getType() == ICatalogElement.TYPE_NEXT_CATALOG)) {
          nextCatalog = (INextCatalog) fCatalogElement;
        } else {
          if (catalog != null) {
            nextCatalog = (INextCatalog) catalog.createCatalogElement(ICatalogElement.TYPE_NEXT_CATALOG);
          }
        }
      }
      return nextCatalog;
    }

    public void saveData() {
      getNextCatalog().setCatalogLocation(
          URIUtils.convertLocationToURI(catalogLocationField.getText()));
      dataSaved = true;
    }

    protected String getCatalogLocation() {
      return getNextCatalog().getCatalogLocation();
    }

    public Control createControl(Composite parent) {
      fControl = createNextCatalogPanel(parent, XMLCatalogMessages.UI_LABEL_CATALOG_URI_COLON);
      return fControl;
    }
  }

  protected class DelegateCatalogPage extends AbstractDelegatePage {
    protected IDelegateCatalog delegateCatalog;
    private Text prefixField;
    private Combo keyTypeCombo;

    public void refresh() {
      computeErrorMessage();
      updateErrorMessageLabel(errorMessageLabel);
      updateOKButtonState();
    }

    protected void computeErrorMessage() {
      errorMessage = null;
      String prefix = prefixField.getText();
      if (prefix.length() > 0) {
        // good
      } else {
        errorMessage = "";
      }
    }

    public ICatalogElement getData() {
      return getDelegateCatalog();
    }

    protected void createSpecificFields(Composite group) {

      Composite prefixComposite = new Composite(group, SWT.NONE);

      GridLayout layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.marginBottom = 5;
      prefixComposite.setLayout(layout);

      // Key Type
      //
      Label keyTypeLabel = new Label(prefixComposite, SWT.NONE);
      keyTypeLabel.setText(XMLCatalogMessages.UI_MATCH_KEY_TYPE_COLON);

      keyTypeCombo = new Combo(prefixComposite, SWT.READ_ONLY);
      GridData gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      keyTypeCombo.setLayoutData(gd);
      keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_PUBLIC);
      keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM);
      keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI);
      switch (getDelegateCatalog().getEntryType()) {
        case IDelegateCatalog.DELEGATE_TYPE_PUBLIC:
          keyTypeCombo.select(0);
          break;
        case IDelegateCatalog.DELEGATE_TYPE_SYSTEM:
          keyTypeCombo.select(1);
          break;
        default:
        case IDelegateCatalog.DELEGATE_TYPE_URI:
          keyTypeCombo.select(2);
          break;
      }

      Label prefixLabel = new Label(prefixComposite, SWT.NONE);
      prefixLabel.setText(XMLCatalogMessages.UI_LABEL_START_STRING_COLON);

      prefixField = new Text(prefixComposite, SWT.SINGLE | SWT.BORDER);
      prefixField.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
      prefixField.setText(getDisplayValue(getDelegateCatalog().getStartString()));
      ModifyListener modifyListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          updateWidgets(e.widget);
        }
      };
      prefixField.addModifyListener(modifyListener);
      prefixComposite.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    }

    protected IDelegateCatalog getDelegateCatalog() {
      if (delegateCatalog == null) {
        if ((fCatalogElement != null)
            && (fCatalogElement.getType() == ICatalogElement.TYPE_DELEGATE)) {
          delegateCatalog = (IDelegateCatalog) fCatalogElement;
        } else {
          if (catalog != null) {
            delegateCatalog = (IDelegateCatalog) catalog.createCatalogElement(IDelegateCatalog.DELEGATE_TYPE_URI);
          }
        }
      }
      return delegateCatalog;
    }

    public void saveData() {
      if (validateData()) {
        getDelegateCatalog().setCatalogLocation(
            URIUtils.convertLocationToURI(catalogLocationField.getText()));
        getDelegateCatalog().setStartString(prefixField.getText());
        getDelegateCatalog().setEntryType(getDelegateType());
        dataSaved = true;
      } else {
        errorMessage = XMLCatalogMessages.UI_WARNING_DUPLICATE_DELEGATE;
        errorMessageLabel.setText(errorMessage);
        updateOKButtonState();
        dataSaved = false;
      }
    }

    private int getDelegateType() {
      switch (keyTypeCombo.getSelectionIndex()) {
        case 0:
          return IDelegateCatalog.DELEGATE_TYPE_PUBLIC;

        case 1:
          return IDelegateCatalog.DELEGATE_TYPE_SYSTEM;

        case 2:
        default:
          return IDelegateCatalog.DELEGATE_TYPE_URI;
      }
    }

    protected boolean validateData() {
      IDelegateCatalog entry = getDelegateCatalog();
      String prefix = prefixField.getText();
      if (entry.getEntryType() != getDelegateType() || !prefix.equals(entry.getStartString())) {
        IDelegateCatalog[] entries = catalog.getDelegateCatalogs();
        for (int i = 0; i < entries.length; i++) {
          if (entries[i].getStartString().equals(prefixField)
              && entries[i].getEntryType() == getDelegateType())
            return false;
        }
      }
      return true;
    }

    protected String getCatalogLocation() {
      return getDelegateCatalog().getCatalogLocation();
    }

    public Control createControl(Composite parent) {
      fControl = createNextCatalogPanel(parent,
          XMLCatalogMessages.UI_LABEL_DELEGATE_CATALOG_URI_COLON);
      return fControl;
    }
  }

  protected class RadioItemSelectionChangeListener implements SelectionListener {
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void widgetSelected(SelectionEvent e) {
      Object selection = e.getSource();
      if (selection instanceof Button) {
        Button button = (Button) selection;
        if (button.getSelection()) {
          if (!showPage((CatalogElementPage) button.getData())) {
            // Page flipping wasn't successful
            // handleError();
          }
        }
      }
    }

  }

  protected class RewriteEntryPage extends CatalogElementPage {
    protected IRewriteEntry rewriteEntry;
    private Text startStringField;
    private Text prefixField;
    private Combo keyTypeCombo;

    public void refresh() {
      computeErrorMessage();
      updateErrorMessageLabel(errorMessageLabel);
      updateOKButtonState();
    }

    public ICatalogElement getData() {
      return getRewriteEntry();
    }

    protected Label errorMessageLabel;

    protected void computeErrorMessage() {
      errorMessage = null;

      String start = startStringField.getText();
      String prefix = prefixField.getText();
      if (start.trim().length() > 0 && prefix.trim().length() > 0) {
        // good
      } else {
        // this an error that is not actually
        // reported ... OK is just disabled
        errorMessage = ""; //$NON-NLS-1$
      }
    }

    protected IRewriteEntry getRewriteEntry() {
      if (rewriteEntry == null) {
        if ((fCatalogElement != null)
            && (fCatalogElement.getType() == ICatalogElement.TYPE_REWRITE)) {
          rewriteEntry = (IRewriteEntry) fCatalogElement;
        } else {
          if (catalog != null) {
            rewriteEntry = (IRewriteEntry) catalog.createCatalogElement(IRewriteEntry.REWRITE_TYPE_SYSTEM);
          }
        }
      }
      return rewriteEntry;
    }

    protected void updateWidgets(Widget widget) {
      computeErrorMessage();
      updateErrorMessageLabel(errorMessageLabel);
      updateOKButtonState();
    }

    public void saveData() {
      if (validateData()) {
        getRewriteEntry().setRewritePrefix(prefixField.getText());
        getRewriteEntry().setStartString(startStringField.getText());
        getRewriteEntry().setEntryType(getEntryType());
        dataSaved = true;
      } else {
        errorMessage = XMLCatalogMessages.UI_WARNING_DUPLICATE_REWRITE;
        errorMessageLabel.setText(errorMessage);
        updateOKButtonState();
        dataSaved = false;
      }
    }

    private int getEntryType() {
      switch (keyTypeCombo.getSelectionIndex()) {
        case 0:
          return IRewriteEntry.REWRITE_TYPE_SYSTEM;
        case 1:
        default:
          return IRewriteEntry.REWRITE_TYPE_URI;
      }
    }

    protected boolean validateData() {
      IRewriteEntry entry = getRewriteEntry();
      String startString = startStringField.getText();
      if (entry.getEntryType() != getEntryType() || !entry.getStartString().equals(startString)) {
        IRewriteEntry[] entries = catalog.getRewriteEntries();
        for (int i = 0; i < entries.length; i++) {
          if (entries[i].getStartString().equals(startString)
              && entries[i].getEntryType() == getEntryType())
            return false;
        }
      }
      return true;
    }

    public Control createControl(Composite parent) {

      fControl = createRewriteEntryPanel(parent);

      return fControl;
    }

    public Control createRewriteEntryPanel(Composite parent) {
      ModifyListener modifyListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          updateWidgets(e.widget);
        }
      };

      Composite composite = new Composite(parent, SWT.NONE);
      GridData gd = new GridData(GridData.FILL_BOTH);
      composite.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      composite.setLayout(layout);

      Composite group = new Composite(composite, SWT.NONE);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);

      layout = new GridLayout();
      group.setLayout(layout);

      Composite prefixComposite = new Composite(group, SWT.NONE);

      layout = new GridLayout();
      layout.numColumns = 2;
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.marginBottom = 5;
      prefixComposite.setLayout(layout);

      // Key Type
      //
      Label keyTypeLabel = new Label(prefixComposite, SWT.NONE);
      keyTypeLabel.setText(XMLCatalogMessages.UI_MATCH_KEY_TYPE_COLON);

      keyTypeCombo = new Combo(prefixComposite, SWT.READ_ONLY);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.grabExcessHorizontalSpace = true;
      keyTypeCombo.setLayoutData(gd);
      keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_DTD_SYSTEM);
      keyTypeCombo.add(XMLCatalogMessages.UI_KEY_TYPE_DESCRIPTION_URI);
      switch (getRewriteEntry().getEntryType()) {
        case IDelegateCatalog.DELEGATE_TYPE_SYSTEM:
          keyTypeCombo.select(0);
          break;
        default:
        case IDelegateCatalog.DELEGATE_TYPE_URI:
          keyTypeCombo.select(1);
          break;
      }
      Label startStringLabel = new Label(prefixComposite, SWT.NONE);
      startStringLabel.setText(XMLCatalogMessages.UI_LABEL_START_STRING_COLON);

      startStringField = new Text(prefixComposite, SWT.SINGLE | SWT.BORDER);
      startStringField.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
      startStringField.setText(getDisplayValue(getRewriteEntry().getStartString()));
      startStringField.addModifyListener(modifyListener);

      Label prefixLabel = new Label(prefixComposite, SWT.NONE);
      prefixLabel.setText(XMLCatalogMessages.UI_LABEL_REWRITE_PREFIX_COLON);

      prefixField = new Text(prefixComposite, SWT.SINGLE | SWT.BORDER);
      prefixField.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
      prefixField.setText(getDisplayValue(getRewriteEntry().getRewritePrefix()));
      prefixField.addModifyListener(modifyListener);

      prefixComposite.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

      errorMessageLabel = new Label(group, SWT.NONE);
      errorMessageLabel.setForeground(color);
      errorMessageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      updateWidgets(null);
      return composite;
    }
  }

  protected class ToolBarItemSelectionChangeListener implements SelectionListener {
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void widgetSelected(SelectionEvent e) {
      Object selection = e.getSource();
      if (selection instanceof ToolItem) {
        ToolItem toolItem = (ToolItem) selection;
        ToolBar toolbar = toolItem.getParent();
        if (toolbar != null) {
          ToolItem[] items = toolbar.getItems();
          for (int i = 0; i < items.length; i++) {
            items[i].setSelection(items[i] == toolItem);
          }
        }
        if (!showPage((CatalogElementPage) toolItem.getData())) {
          // Page flipping wasn't successful
          // handleError();
        }
      }
    }
  }

  public static String[] createStringArray(List list) {
    String[] stringArray = new String[list.size()];
    for (int i = 0; i < stringArray.length; i++) {
      stringArray[i] = (String) list.get(i);
    }
    return stringArray;
  }

  public static String removeLeadingSlash(String uri) {
    // remove leading slash from the value to avoid the whole leading
    // slash
    // ambiguity problem
    //       
    if (uri != null) {
      while (uri.startsWith("/") || uri.startsWith("\\")) { //$NON-NLS-1$ //$NON-NLS-2$
        uri = uri.substring(1);
      }
    }
    return uri;
  }

  protected ICatalog catalog;

  protected ICatalogElement fCatalogElement;

  protected String errorMessage;

  protected String warningMessage;

  protected Button okButton;

  protected PageBook pageContainer;

  protected CatalogElementPage selectedPage;

  // protected TreeViewer treeViewer;

  protected ToolBar toolBar;

  protected Composite elementTypeComposite;

  protected Color color;
  protected boolean dataSaved;

  public EditCatalogEntryDialog(Shell parentShell, ICatalog aCatalog) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.catalog = aCatalog;
  }

  public EditCatalogEntryDialog(Shell parentShell, ICatalogElement catalogElement, ICatalog aCatalog) {
    this(parentShell, aCatalog);
    this.fCatalogElement = catalogElement;
    // TODO EB: fix his
    // entry.setURI(URIHelper.removePlatformResourceProtocol(entry.getURI()));
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      selectedPage.saveData();
      if (!dataSaved) {
        // do not exit edit dialog
        return;
      }
    }
    super.buttonPressed(buttonId);
  }

  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    okButton.setEnabled(false);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    updateOKButtonState();
  }

  protected Control createDialogArea(Composite parent) {
    Composite dialogAreaComposite = (Composite) super.createDialogArea(parent);
    color = new Color(dialogAreaComposite.getDisplay(), 200, 0, 0);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    dialogAreaComposite.setLayout(layout);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = 550;
    //gd.heightHint = 250;
    dialogAreaComposite.setLayoutData(gd);
    createMainComponent(dialogAreaComposite);
    return this.dialogArea;
  }

  public boolean close() {
    if (color != null) {
      color.dispose();
    }
    return super.close();
  }

  protected Composite createMainComponent(Composite composite) {
    if (fCatalogElement != null) // "edit" action
    {
      Composite composite1 = new Composite(composite, SWT.NONE);
      GridData data = new GridData(GridData.FILL_BOTH);
      composite1.setLayoutData(data);
      GridLayout layout = new GridLayout();
      composite1.setLayout(layout);

      pageContainer = new PageBook(composite1, SWT.NONE);
      pageContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

      if (fCatalogElement.getType() == ICatalogElement.TYPE_ENTRY) {
        CatalogElementPage entryPage = new CatalogEntryPage();
        entryPage.createControl(pageContainer);
        showPage(entryPage);
      } else if (fCatalogElement.getType() == ICatalogElement.TYPE_NEXT_CATALOG) {
        CatalogElementPage nextCatalogPage = new NextCatalogPage();
        nextCatalogPage.createControl(pageContainer);
        showPage(nextCatalogPage);
      } else if (fCatalogElement.getType() == ICatalogElement.TYPE_DELEGATE) {
        DelegateCatalogPage delegateCatalogPage = new DelegateCatalogPage();
        delegateCatalogPage.createControl(pageContainer);
        showPage(delegateCatalogPage);
      } else if (fCatalogElement.getType() == ICatalogElement.TYPE_SUFFIX) {
        SuffixEntryPage suffixEntryPage = new SuffixEntryPage();
        suffixEntryPage.createControl(pageContainer);
        showPage(suffixEntryPage);
      } else if (fCatalogElement.getType() == ICatalogElement.TYPE_REWRITE) {
        RewriteEntryPage rewriteEntryPage = new RewriteEntryPage();
        rewriteEntryPage.createControl(pageContainer);
        showPage(rewriteEntryPage);
      }

      return composite1;
    }
    return createMainComponentWithToolbar(composite);

  }

  protected Composite createMainComponentWithToolbar(Composite composite) {

    FormLayout formLayout = new FormLayout();
    formLayout.marginHeight = 5;
    formLayout.marginWidth = 5;
    composite.setLayout(formLayout);

    Label label = new Label(composite, SWT.NONE);
    FormData data = new FormData();
    data.top = new FormAttachment(0, 0);
    data.left = new FormAttachment(0, 0);
    data.right = new FormAttachment(100, 0);
    label.setLayoutData(data);

    toolBar = new ToolBar(composite, SWT.BORDER | SWT.FLAT | SWT.VERTICAL);

    data = new FormData();
    data.top = new FormAttachment(label, 0);
    data.left = new FormAttachment(0, 0);
    data.bottom = new FormAttachment(100, 0);
    // data.height = 250;
    // data.width = 50;
    toolBar.setLayoutData(data);

    Composite composite1 = new Composite(composite, SWT.BORDER);
    data = new FormData();
    data.top = new FormAttachment(label, 0);
    data.left = new FormAttachment(toolBar, 0, SWT.DEFAULT);
    data.right = new FormAttachment(100, 0);
    data.bottom = new FormAttachment(100, 0);
    composite1.setLayoutData(data);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite1.setLayout(layout);

    // createPageBookPanel(composite1);
    pageContainer = new PageBook(composite1, SWT.NONE);
    pageContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

    // add pages for each type of catalog element
    createCatalogEntryButton();
    createRewriteButton();
    createSuffixCatalogButton();
    createNextCatalogButton();
    createDelegateCatalogButton();
    if (toolBar.getItemCount() > 0) {
      ToolItem item = toolBar.getItem(0);
      showPage((CatalogElementPage) (item.getData()));
    }
    return composite1;
  }

  protected void createCatalogEntryButton() {
    CatalogElementPage page = new CatalogEntryPage();
    page.createControl(pageContainer);
    ToolItem toolItem = new ToolItem(toolBar, SWT.CHECK);
    toolItem.setImage(catalogEntryToolBarImage);
    toolItem.setText(XMLCatalogMessages.EditCatalogEntryDialog_catalogEntryLabel);
    toolItem.setData(page);
    toolItem.addSelectionListener(new ToolBarItemSelectionChangeListener());
    toolItem.setSelection(true);
  }

  protected void createNextCatalogButton() {
    CatalogElementPage page = new NextCatalogPage();
    page.createControl(pageContainer);
    ToolItem toolItem = new ToolItem(toolBar, SWT.CHECK);
    toolItem.setImage(nextCatalogToolBarImage);
    toolItem.setText(XMLCatalogMessages.EditCatalogEntryDialog_nextCatalogLabel);
    toolItem.setData(page);
    toolItem.addSelectionListener(new ToolBarItemSelectionChangeListener());

  }

  protected void createRewriteButton() {
    CatalogElementPage page = new RewriteEntryPage();
    page.createControl(pageContainer);
    ToolItem toolItem = new ToolItem(toolBar, SWT.CHECK);
    toolItem.setImage(rewriteToolBarImage);
    toolItem.setText(XMLCatalogMessages.EditCatalogEntryDialog_rewriteEntryLabel);
    toolItem.setData(page);
    toolItem.addSelectionListener(new ToolBarItemSelectionChangeListener());
  }

  protected void createDelegateCatalogButton() {
    CatalogElementPage page = new DelegateCatalogPage();
    page.createControl(pageContainer);
    ToolItem toolItem = new ToolItem(toolBar, SWT.CHECK);
    toolItem.setImage(delegateCatalogToolBarImage);
    toolItem.setText(XMLCatalogMessages.EditCatalogEntryDialog_delegateCatalogLabel);
    toolItem.setData(page);
    toolItem.addSelectionListener(new ToolBarItemSelectionChangeListener());
  }

  protected void createSuffixCatalogButton() {
    CatalogElementPage page = new SuffixEntryPage();
    page.createControl(pageContainer);
    ToolItem toolItem = new ToolItem(toolBar, SWT.CHECK);
    toolItem.setImage(suffixToolBarImage);
    toolItem.setText(XMLCatalogMessages.EditCatalogEntryDialog_suffixEntryLabel);
    toolItem.setData(page);
    toolItem.addSelectionListener(new ToolBarItemSelectionChangeListener());
  }

//
//	protected void createRewriteEntryButton() {
//		CatalogElementPage page = new RewriteEntryPage();
//		page.createControl(pageContainer);
//		Button radioButton = new Button(elementTypeComposite, SWT.RADIO);
//		radioButton.setText(XMLCatalogMessages.EditCatalogEntryDialog_rewriteEntryLabel);
//		radioButton.setData(page);
//		radioButton.addSelectionListener(new RadioItemSelectionChangeListener());
//	}
//
//	protected void createSuffixEntryButton() {
//		CatalogElementPage page = new SuffixEntryPage();
//		page.createControl(pageContainer);
//		Button radioButton = new Button(elementTypeComposite, SWT.RADIO);
//		radioButton.setText(XMLCatalogMessages.EditCatalogEntryDialog_suffixEntryLabel);
//		radioButton.setData(page);
//		radioButton.addSelectionListener(new RadioItemSelectionChangeListener());
//	}
//
//	protected void createDelegateCatalogButton() {
//		CatalogElementPage page = new DelegateCatalogPage();
//		page.createControl(pageContainer);
//		Button radioButton = new Button(elementTypeComposite, SWT.RADIO);
//		radioButton.setText(XMLCatalogMessages.EditCatalogEntryDialog_delegateCatalogLabel);
//		radioButton.setData(page);
//		radioButton.addSelectionListener(new RadioItemSelectionChangeListener());
//
//	}

  protected ICatalogElement getCatalogElement() {
    return fCatalogElement;
  }

  protected String getDisplayValue(String string) {
    return string != null ? string : ""; //$NON-NLS-1$
  }

  protected XMLCatalogFileType getMatchingFileType(String description) {
    XMLCatalogFileType fileType = null;
    for (Iterator i = CatalogFileTypeRegistryReader.getXMLCatalogFileTypes().iterator(); i.hasNext();) {
      XMLCatalogFileType theFileType = (XMLCatalogFileType) i.next();
      if ((theFileType.description != null) && theFileType.description.equals(description)) {
        fileType = theFileType;
      }
    }
    return fileType;
  }

  protected boolean showPage(CatalogElementPage page) {
    if (pageContainer.isDisposed()) {
      return false;
    }
    selectedPage = page;
    pageContainer.setVisible(true);
    pageContainer.showPage(selectedPage.getControl());
    fCatalogElement = selectedPage.getData();
    selectedPage.refresh();
    return true;
  }

  protected void updateErrorMessageLabel(Label errorMessageLabel) {
    if (errorMessage != null) {
      errorMessageLabel.setText(errorMessage);
    } else if (warningMessage != null) {
      errorMessageLabel.setText(warningMessage);
    } else {
      errorMessageLabel.setText("");
    }
  }

  protected void updateOKButtonState() {
    if (okButton != null) {
      okButton.setEnabled(errorMessage == null);
    }
  }

  protected Button createBrowseButton(Composite composite) {
    Button browseButton = new Button(composite, SWT.PUSH);
    // browseButton.setText(XMLCatalogMessages.
    // UI_BUTTON_BROWSE"));
    browseButton.setImage(borwseImage);
    Rectangle r = borwseImage.getBounds();
    GridData gd = new GridData();
    int IMAGE_WIDTH_MARGIN = 6;
    int IMAGE_HEIGHT_MARGIN = 6;
    gd.heightHint = r.height + IMAGE_HEIGHT_MARGIN;
    gd.widthHint = r.width + IMAGE_WIDTH_MARGIN;
    browseButton.setLayoutData(gd);

    return browseButton;

  }

  protected Button createWorkspaceBrowseButton(Composite composite) {
    Button browseWorkspaceButton = new Button(composite, SWT.PUSH);
    browseWorkspaceButton.setText(XMLCatalogMessages.UI_BUTTON_MENU_BROWSE_WORKSPACE);
    return browseWorkspaceButton;
  }

  protected Button createFileSystemBrowseButton(Composite composite) {
    Button browseFileSystemButton = new Button(composite, SWT.PUSH);
    browseFileSystemButton.setText(XMLCatalogMessages.UI_BUTTON_MENU_BROWSE_WORKSPACE);
    return browseFileSystemButton;
  }

  String invokeWorkspaceFileSelectionDialog() {
    FilterableSelectSingleFileDialog dialog = new FilterableSelectSingleFileDialog(getShell());
    dialog.createAndOpen();
    IFile file = dialog.getFile();
    String uri = null;
    if (file != null) {
      // remove leading slash from the value to avoid the
      // whole leading slash ambiguity problem
      //                    
      uri = file.getFullPath().toString();
      while (uri.startsWith("/") || uri.startsWith("\\")) { //$NON-NLS-1$ //$NON-NLS-2$
        uri = uri.substring(1);
      }
    }
    return uri;
  }

  String invokeFileSelectionDialog() {
    FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
    return dialog.open();
  }

}
