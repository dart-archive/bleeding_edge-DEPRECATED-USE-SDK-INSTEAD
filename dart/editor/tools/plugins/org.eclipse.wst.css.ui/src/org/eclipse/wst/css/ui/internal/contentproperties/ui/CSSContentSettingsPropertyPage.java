/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentproperties.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.css.ui.internal.Logger;
import org.eclipse.wst.css.ui.internal.contentproperties.ContentSettingsRegistry;
import org.eclipse.wst.css.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.internal.contentproperties.IContentSettings;
import org.eclipse.wst.sse.ui.internal.contentproperties.ui.ComboListOnPropertyPage;
import org.eclipse.wst.sse.ui.internal.contentproperties.ui.ContentSettingsPropertyPage;

import java.util.Map;

/**
 * @deprecated Use CSSWebContentSettingsPropertyPage instead
 */
public final class CSSContentSettingsPropertyPage extends ContentSettingsPropertyPage {

  private final int N_CSS_PROFILE = 0;

  public CSSContentSettingsPropertyPage() {
    super();
    numberOfCombo = 1;
    numCols = 2;
    numRows = 1;
    combo = new ComboListOnPropertyPage[super.numberOfCombo];

  }

  protected void createCSSComboBox() {
    super.combo[N_CSS_PROFILE] = super.createComboBoxOf(CSS_LABEL);
    ContentSettingsRegistry.setCSSMetaModelRegistryInto(combo[N_CSS_PROFILE]);
    if (combo[N_CSS_PROFILE].getItemCount() <= 0)
      return;
    String initValue = contentSettings.getProperty((IResource) super.getElement(),
        IContentSettings.CSS_PROFILE);
    // when either .contentsettings or element doesn't exist
    // when attribute doesn't exists,getProperty returns empty string.
    if (initValue == null)
      initValue = ""; //$NON-NLS-1$
    // set init selectionItem in Combo
    super.setSelectionItem(combo[N_CSS_PROFILE], initValue);
  }

  protected void createSettingsPageGUI() {

    int type = ((IResource) getElement()).getType();
    switch (type) {
      case IResource.FILE:
        //composite = createComposite(propertyPage,numCols,numRows);
        createCSSComboBox();
        PlatformUI.getWorkbench().getHelpSystem().setHelp(propertyPage,
            IHelpContextIds.CSS_CONTENT_SETTINGS_HELPID);
        break;

      default:
        Logger.log(Logger.WARNING,
            "CSSContentSettingsPropertyPage is instantiated by resource except FILE");//$NON-NLS-1$
        break;
    }

  }

  protected void putSelectedPropertyInto(Map properties, String valueInCombo, int index) {

    switch (index) {
      case N_CSS_PROFILE:
        // css
        properties.put(IContentSettings.CSS_PROFILE, valueInCombo);
        break;
      default:
        Logger.log(Logger.ERROR,
            "Index is out of range in putSelectedPropertyInto() in class CSSContentSettingsPropertyPage");//$NON-NLS-1$
        break;
    }

  }

  protected void deleteNoneProperty(int index) {
    switch (index) {
      case N_CSS_PROFILE:
        // css
        contentSettings.deleteProperty((IResource) super.getElement(), IContentSettings.CSS_PROFILE);
        break;

      default:
        Logger.log(Logger.ERROR,
            "Index is out of range in deleteNoneProperty() in class CSSContentSettingsPropertyPage");//$NON-NLS-1$
        break;
    }
  }

}
