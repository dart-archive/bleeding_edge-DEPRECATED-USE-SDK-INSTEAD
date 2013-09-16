/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation David Schneider, david.schneider@unisys.com - [142500] WTP properties pages fonts
 * don't follow Eclipse preferences
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentproperties.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.css.core.internal.contentproperties.CSSContentProperties;
import org.eclipse.wst.css.core.internal.metamodel.CSSProfile;
import org.eclipse.wst.css.core.internal.metamodel.CSSProfileRegistry;
import org.eclipse.wst.html.core.internal.contentproperties.HTMLContentProperties;
import org.eclipse.wst.html.core.internal.document.HTMLDocumentTypeEntry;
import org.eclipse.wst.html.core.internal.document.HTMLDocumentTypeRegistry;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.Logger;
import org.eclipse.wst.html.ui.internal.editor.IHelpContextIds;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class WebContentSettingsPropertyPage extends PropertyPage {
  private static final String SELECT_NONE = HTMLUIMessages.UI_none;
  private String maxLengthStringInHTMLDocumentTypeRegistry = ""; //$NON-NLS-1$

  private class ComboSelectionListener implements SelectionListener {
    public void widgetDefaultSelected(SelectionEvent e) {
      // do nothing
    }

    public void widgetSelected(SelectionEvent e) {
      int index = fDocumentTypeCombo.getSelectionIndex();
      String doctype = (String) fDocumentTypeIds.get(index);
      updateDoctypeText(index, doctype);
    }
  }

  Combo fDocumentTypeCombo;
  List fDocumentTypeIds;
  private Text fPublicIdText;
  private Text fSystemIdText;
  private Combo fProfileCombo;
  private List fProfileIds;
  private SelectionListener fListener;

  public WebContentSettingsPropertyPage() {
    super();
    setDescription(HTMLUIMessages.WebContentSettingsPropertyPage_0);
  }

  private Composite createComposite(Composite parent, int numColumns) {
    Composite composite = new Composite(parent, SWT.NULL);

    // GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = numColumns;
    composite.setLayout(layout);

    // GridData
    GridData data = new GridData(GridData.FILL, GridData.FILL, true, false);
    data.horizontalIndent = 0;
    composite.setLayoutData(data);

    return composite;
  }

  protected Control createContents(Composite parent) {
    Composite propertyPage = createComposite(parent, 2);

    createDoctypeContents(propertyPage);
    createCSSProfileContents(propertyPage);

    populateValues();
    initializeValues();
    computeMaxWidthHint();

    fListener = new ComboSelectionListener();
    fDocumentTypeCombo.addSelectionListener(fListener);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(propertyPage,
        IHelpContextIds.WEB_CONTENT_SETTINGS_HELPID);
    Dialog.applyDialogFont(parent);
    return propertyPage;
  }

  private void createCSSProfileContents(Composite parent) {
    // CSS Profile
    Label languageLabel = new Label(parent, SWT.NONE);
    languageLabel.setText(HTMLUIMessages.UI_CSS_profile___2);
    fProfileCombo = new Combo(parent, SWT.READ_ONLY);
    GridData data = new GridData(GridData.FILL, GridData.FILL, true, false);
    data.horizontalIndent = 0;
    fProfileCombo.setLayoutData(data);
  }

  private void createDoctypeContents(Composite parent) {
    // create description of implicit DOCTYPE
    Text doctypeLabel = new Text(parent, SWT.READ_ONLY);
    doctypeLabel.setText(HTMLUIMessages.UI_Description_of_role_of_following_DOCTYPE);
    GridData data = new GridData(GridData.FILL, GridData.FILL, true, false);
    data.horizontalIndent = 0;
    data.horizontalSpan = 2;
    doctypeLabel.setLayoutData(data);

    // document type
    Label languageLabel = new Label(parent, SWT.NONE);
    languageLabel.setText(HTMLUIMessages.UI_Default_HTML_DOCTYPE_ID___1);
    fDocumentTypeCombo = new Combo(parent, SWT.READ_ONLY);
    data = new GridData(GridData.FILL, GridData.FILL, true, false);
    data.horizontalIndent = 0;
    fDocumentTypeCombo.setLayoutData(data);

    // public ID
    Label publicIdLabel = new Label(parent, SWT.NONE);
    publicIdLabel.setText(HTMLUIMessages.UI_Public_ID);
    fPublicIdText = new Text(parent, SWT.READ_ONLY | SWT.BORDER);
    data = new GridData(GridData.FILL, GridData.FILL, true, false);
    data.horizontalIndent = 0;
    fPublicIdText.setLayoutData(data);

    // system ID
    Label systemIdLabel = new Label(parent, SWT.NONE);
    systemIdLabel.setText(HTMLUIMessages.UI_System_ID);
    fSystemIdText = new Text(parent, SWT.READ_ONLY | SWT.BORDER);
    data = new GridData(GridData.FILL, GridData.FILL, true, false);
    data.horizontalIndent = 0;
    fSystemIdText.setLayoutData(data);

    // create separator
    Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.horizontalSpan = 2;
    data.verticalSpan = 8;
    label.setLayoutData(data);

  }

  /**
   * Get the resource this properties page is for
   * 
   * @return IResource for this properties page or null if there is no IResource
   */
  private IResource getResource() {
    IResource resource = null;
    IAdaptable adaptable = getElement();
    if (adaptable instanceof IResource) {
      resource = (IResource) adaptable;
    } else if (adaptable != null) {
      Object o = adaptable.getAdapter(IResource.class);
      if (o instanceof IResource) {
        resource = (IResource) o;
      }
    }
    return resource;
  }

  private String getSystemIdFrom(String publicId) {
    if (publicId == null || publicId.length() == 0)
      return null;
    HTMLDocumentTypeRegistry reg = HTMLDocumentTypeRegistry.getInstance();
    Enumeration e = reg.getEntries();
    while (e.hasMoreElements()) {
      HTMLDocumentTypeEntry entry = (HTMLDocumentTypeEntry) e.nextElement();
      if (entry.getPublicId().equals(publicId))
        return entry.getSystemId();
    }
    return null;
  }

  private void initializeValues() {
    initializeDoctypeValues();
    initializeCSSProfileValues();
  }

  private void initializeCSSProfileValues() {
    int index = 0;
    String profile = CSSContentProperties.getProperty(CSSContentProperties.CSS_PROFILE,
        getResource(), false);
    if (profile != null && profile.length() > 0) {
      /*
       * If item is already part of combo, select it. Otherwise, select none.
       */
      index = fProfileIds.indexOf(profile);
    }
    index = index >= 0 ? index : 0;
    fProfileCombo.select(index);
  }

  private void initializeDoctypeValues() {
    int index = 0;
    String doctype = HTMLContentProperties.getProperty(HTMLContentProperties.DOCUMENT_TYPE,
        getResource(), false);
    if (doctype != null) {
      /*
       * If item is already part of combo, select it. Otherwise, select none.
       */
      index = fDocumentTypeIds.indexOf(doctype);
    }

    // set combobox
    index = index >= 0 ? index : 0;
    fDocumentTypeCombo.select(index);

    updateDoctypeText(index, doctype);
  }

  void updateDoctypeText(int index, String doctype) {
    if (index > 0) {
      // set public/system id text
      fPublicIdText.setText(doctype);
      String systemId = getSystemIdFrom(doctype);
      if (systemId != null)
        fSystemIdText.setText(systemId);
      else
        fSystemIdText.setText(""); //$NON-NLS-1$
    } else {
      // set public/system id text
      fPublicIdText.setText(""); //$NON-NLS-1$
      fSystemIdText.setText(""); //$NON-NLS-1$
    }
  }

  private void populateValues() {
    populateDoctypeValues();
    populateCSSProfileValues();
  }

  private void populateCSSProfileValues() {
    fProfileIds = new ArrayList();
    // add none first
    fProfileCombo.add(SELECT_NONE);
    fProfileIds.add(null);

    CSSProfileRegistry reg = CSSProfileRegistry.getInstance();
    Iterator i = reg.getProfiles();
    while (i.hasNext()) {
      CSSProfile profile = (CSSProfile) i.next();
      String id = profile.getProfileID();
      String name = profile.getProfileName();
      fProfileCombo.add(name);
      fProfileIds.add(id);
    }
  }

  private void populateDoctypeValues() {
    fDocumentTypeIds = new ArrayList();
    // add none first
    fDocumentTypeCombo.add(SELECT_NONE);
    fDocumentTypeIds.add(null);

    HTMLDocumentTypeRegistry reg = HTMLDocumentTypeRegistry.getInstance();
    Enumeration e = reg.getEntries();
    while (e.hasMoreElements()) {
      HTMLDocumentTypeEntry entry = (HTMLDocumentTypeEntry) e.nextElement();
      String publicId = entry.getPublicId();
      String displayName = entry.getDisplayName();
      displayName = displayName != null ? displayName : publicId;

      fDocumentTypeCombo.add(displayName);
      fDocumentTypeIds.add(publicId);

      if (displayName.length() > maxLengthStringInHTMLDocumentTypeRegistry.length()) {
        maxLengthStringInHTMLDocumentTypeRegistry = displayName;
      }

      if (entry.getSystemId() == null)
        continue; // if HTML entry

      if (entry.getSystemId().length() > maxLengthStringInHTMLDocumentTypeRegistry.length())
        maxLengthStringInHTMLDocumentTypeRegistry = entry.getSystemId();
    }
  }

  private void computeMaxWidthHint() {
    // maxLengthString was set populateDoctypeValues was called
    String maxLengthString = maxLengthStringInHTMLDocumentTypeRegistry;
    String backup = fSystemIdText.getText();
    fSystemIdText.setText(maxLengthString);
    int maxWidthHint = fSystemIdText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    fSystemIdText.setText(backup);

    if (fDocumentTypeCombo.getLayoutData() != null)
      ((GridData) fDocumentTypeCombo.getLayoutData()).widthHint = maxWidthHint;
    if (fPublicIdText.getLayoutData() != null)
      ((GridData) fPublicIdText.getLayoutData()).widthHint = maxWidthHint;
    if (fSystemIdText.getLayoutData() != null)
      ((GridData) fSystemIdText.getLayoutData()).widthHint = maxWidthHint;
    if (fProfileCombo.getLayoutData() != null)
      ((GridData) fProfileCombo.getLayoutData()).widthHint = maxWidthHint;
  }

  private void performCSSProfileDefaults() {
    int index = fProfileCombo.indexOf(SELECT_NONE);
    if (index > -1)
      fProfileCombo.select(index);

    super.performDefaults();
  }

  private boolean performCSSProfileOk() {
    int index = fProfileCombo.getSelectionIndex();
    if (index > -1) {
      String id = (String) fProfileIds.get(index);
      if (id == null || id.length() == 0 || id.equalsIgnoreCase(SELECT_NONE)) {
        // if none, use null
        id = null;
      }
      try {
        CSSContentProperties.setProperty(CSSContentProperties.CSS_PROFILE, getResource(), id);
      } catch (CoreException e) {
        // maybe in future, let user know there was a problem saving
        // file
        Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
        return false;
      }
    }
    return true;
  }

  protected void performDefaults() {
    super.performDefaults();

    performDoctypeDefaults();
    performCSSProfileDefaults();
  }

  private void performDoctypeDefaults() {
    fPublicIdText.setText("");//$NON-NLS-1$
    fSystemIdText.setText(""); //$NON-NLS-1$
    int index = fDocumentTypeCombo.indexOf(SELECT_NONE);
    if (index > -1)
      fDocumentTypeCombo.select(index);
  }

  private boolean performDoctypeOk() {
    int index = fDocumentTypeCombo.getSelectionIndex();
    if (index > -1) {
      String id = (String) fDocumentTypeIds.get(index);
      if (id == null || id.equalsIgnoreCase(SELECT_NONE)) {
        // if none, use null
        id = null;
      }
      try {
        HTMLContentProperties.setProperty(HTMLContentProperties.DOCUMENT_TYPE, getResource(), id);
      } catch (CoreException e) {
        // maybe in future, let user know there was a problem saving
        // file
        Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
        return false;
      }
    }
    return true;
  }

  public boolean performOk() {
    boolean doctype = performDoctypeOk();
    boolean cssprofile = performCSSProfileOk();
    if (doctype || cssprofile) {
      // touch to mark for build-driven revalidation
      IResource resource = getResource();
      if (resource != null) {
        try {
          resource.accept(new IResourceVisitor() {

            public boolean visit(IResource resource) throws CoreException {
              try {
                resource.touch(null);
              } catch (CoreException e) {
                return false;
              }
              return true;
            }
          }, IResource.DEPTH_INFINITE, false);
        } catch (CoreException e) {
          Logger.logException(e);
        }
      }
    }

    return super.performOk();
  }
}
