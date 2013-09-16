/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentproperties.ui;

import org.eclipse.wst.css.core.internal.metamodel.CSSProfile;
import org.eclipse.wst.css.core.internal.metamodel.CSSProfileRegistry;
import org.eclipse.wst.html.core.internal.document.HTMLDocumentTypeEntry;
import org.eclipse.wst.html.core.internal.document.HTMLDocumentTypeRegistry;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.sse.core.internal.encoding.CommonCharsetNames;
import org.eclipse.wst.sse.ui.internal.contentproperties.ui.ComboList;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * @deprecated This class only contains helper methods that you should actually implement yourself.
 */
public final class ContentSettingsRegistry {

  private static final String NONE = HTMLUIMessages.UI_none;

  public static String maxLengthStringInHTMLDocumentTypeRegistry = ""; //$NON-NLS-1$
  public static String maxLengthStringInCharacterCodeRegistry = ""; //$NON-NLS-1$

  private ContentSettingsRegistry() {
  }

  public static void setHTMLDocumentTypeRegistryInto(ComboList combo) {
    combo.add(NONE, ""); //$NON-NLS-1$
    HTMLDocumentTypeRegistry reg = HTMLDocumentTypeRegistry.getInstance();
    Enumeration e = reg.getEntries();
    while (e.hasMoreElements()) {
      HTMLDocumentTypeEntry entry = (HTMLDocumentTypeEntry) e.nextElement();
      String publicId = entry.getPublicId();
      String displayName = entry.getDisplayName();
      if (displayName != null) {
        combo.add(displayName, publicId);
        if (displayName.length() > maxLengthStringInHTMLDocumentTypeRegistry.length())
          maxLengthStringInHTMLDocumentTypeRegistry = displayName;
      } else
        combo.add(publicId, publicId);
      if (publicId.length() > maxLengthStringInHTMLDocumentTypeRegistry.length())
        maxLengthStringInHTMLDocumentTypeRegistry = publicId;
      if (entry.getSystemId() == null)
        continue; // if HTML entry
      if (entry.getSystemId().length() > maxLengthStringInHTMLDocumentTypeRegistry.length())
        maxLengthStringInHTMLDocumentTypeRegistry = entry.getSystemId();
    }

    combo.sortByKey(1);
  }

  public static void setCSSMetaModelRegistryInto(ComboList combo) {
    combo.add(NONE, ""); //$NON-NLS-1$
    CSSProfileRegistry reg = CSSProfileRegistry.getInstance();
    Iterator i = reg.getProfiles();
    while (i.hasNext()) {
      CSSProfile profile = (CSSProfile) i.next();
      String id = profile.getProfileID();
      String name = profile.getProfileName();
      combo.add(name, id);
    }
    combo.sortByKey(1);
  }

  public static void setDeviceProfileRegistryInto(ComboList combo) {
    combo.add(NONE, ""); //$NON-NLS-1$
    DeviceProfileEntryProvider reg = DeviceProfileEntryProviderBuilder.getEntryProvider();
    if (reg == null) {
      return;
    }
    Iterator profiles = reg.getDeviceProfileEntries();
    if (profiles == null) {
      reg.release();
      return;
    }
    DeviceProfileEntry entry;
    while (profiles.hasNext()) {
      entry = (DeviceProfileEntry) profiles.next();
      combo.add(entry.getEntryName(), entry.getEntryId());
      entry.release();
    }
    reg.release();
    combo.sortByKey(1);
  }

  public static String getSystemIdFrom(String publicId) {
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

  public static void setContentTypeInto(ComboList combo) {
    String[] type = {"", //$NON-NLS-1$
        "application/xhtml+xml", //$NON-NLS-1$
        "application/xml", //$NON-NLS-1$
        "text/html", //$NON-NLS-1$
        "text/xml",}; //$NON-NLS-1$
    String[] displayName = {NONE, "application/xhtml+xml", //$NON-NLS-1$
    //$NON-NLS-1$
        "application/xml", //$NON-NLS-1$
        "text/html", //$NON-NLS-1$
        "text/xml",}; //$NON-NLS-1$
    for (int i = 0; i < type.length; i++) {
      if (displayName[i] != null && displayName[i].length() != 0)
        combo.add(displayName[i], type[i]);
      else
        combo.add(type[i], type[i]);
    }

  }

  public static void setCharacterCodeInto(ComboList combo) {
    combo.add(NONE, ""); //$NON-NLS-1$
    String max = ""; //$NON-NLS-1$
    // CommonCharsetNames encoding = new CommonCharsetNames();
    String[] charCode = CommonCharsetNames.getCommonCharsetNames();
    for (int i = 0; i < charCode.length; i++) {
      String displayName = CommonCharsetNames.getDisplayString(charCode[i]);
      if (displayName != null && displayName.length() != 0) {
        combo.add(displayName, charCode[i]);
        int n_byte = displayName.getBytes().length;
        if (max.getBytes().length < n_byte)
          max = displayName;
      } else
        combo.add(charCode[i], charCode[i]);
    }
    /*
     * charCode = encoding.getSupportedJavaEncodings(); for(int i=0;i<charCode.length;i++){ String
     * displayName = encoding.getDisplayString(charCode[i]); if (displayName!=null &&
     * displayName.length()!=0) combo.add(displayName,charCode[i]); else
     * combo.add(charCode[i],charCode[i]); }
     */
    // combo.sortByKey(1);
    maxLengthStringInCharacterCodeRegistry = max;
  }

  public static void setLanguageInto(ComboList combo) {
    String[] lang = {"", //$NON-NLS-1$
        "java", //$NON-NLS-1$
        "javascript",}; //$NON-NLS-1$
    String[] displayName = {NONE, "java", //$NON-NLS-1$
    //$NON-NLS-1$
        "javascript",}; //$NON-NLS-1$
    for (int i = 0; i < lang.length; i++) {
      if (displayName[i] != null && displayName[i].length() != 0)
        combo.add(displayName[i], lang[i]);
      else
        combo.add(lang[i], lang[i]);
    }
  }

}
