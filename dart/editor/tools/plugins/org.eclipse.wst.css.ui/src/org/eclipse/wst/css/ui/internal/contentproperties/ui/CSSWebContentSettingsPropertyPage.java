/*****************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation David Schneider, david.schneider@unisys.com - [142500] WTP properties pages fonts
 * don't follow Eclipse preferences
 ****************************************************************************/
package org.eclipse.wst.css.ui.internal.contentproperties.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.css.core.internal.contentproperties.CSSContentProperties;
import org.eclipse.wst.css.core.internal.metamodel.CSSProfile;
import org.eclipse.wst.css.core.internal.metamodel.CSSProfileRegistry;
import org.eclipse.wst.css.ui.internal.CSSUIMessages;
import org.eclipse.wst.css.ui.internal.Logger;
import org.eclipse.wst.css.ui.internal.editor.IHelpContextIds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CSSWebContentSettingsPropertyPage extends PropertyPage {
  private static final String SELECT_NONE = CSSUIMessages.UI_none;

  private Combo fProfileCombo;
  private List fProfileIds;

  public CSSWebContentSettingsPropertyPage() {
    super();
    setDescription(CSSUIMessages.CSSContentSettingsPropertyPage_0);
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

    // CSS Profile control
    Text languageLabel = new Text(propertyPage, SWT.READ_ONLY);
    languageLabel.setText(CSSUIMessages.CSSContentSettingsPropertyPage_1);
    fProfileCombo = new Combo(propertyPage, SWT.READ_ONLY);
    GridData data = new GridData(GridData.FILL, GridData.FILL, true, false);
    data.horizontalIndent = 0;
    fProfileCombo.setLayoutData(data);

    populateValues();
    initializeValues();

    PlatformUI.getWorkbench().getHelpSystem().setHelp(propertyPage,
        IHelpContextIds.CSS_CONTENT_SETTINGS_HELPID);
    Dialog.applyDialogFont(parent);
    return propertyPage;
  }

  /**
   * Get the resource this properties page is for
   * 
   * @return IResource for this properties page or null if there is no IResource
   */
  private IResource getResource() {
    IResource resource = null;
    if (getElement() instanceof IResource) {
      resource = (IResource) getElement();
    }
    return resource;
  }

  private void initializeValues() {
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

  private void populateValues() {
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

  protected void performDefaults() {
    int index = fProfileCombo.indexOf(SELECT_NONE);
    if (index > -1)
      fProfileCombo.select(index);

    super.performDefaults();
  }

  public boolean performOk() {
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
      }
    }
    return super.performOk();
  }
}
