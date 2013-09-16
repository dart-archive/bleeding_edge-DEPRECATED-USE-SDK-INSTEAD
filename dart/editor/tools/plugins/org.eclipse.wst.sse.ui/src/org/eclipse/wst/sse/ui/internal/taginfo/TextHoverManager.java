/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.taginfo;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.preferences.EditorPreferenceNames;

import java.util.HashMap;

/**
 * Manages text hovers for Structured Text editors
 */
public class TextHoverManager {
  /**
   * Contains description of a text hover
   */
  public class TextHoverDescriptor {
    private String fDescription;
    private boolean fEnabled;
    private String fId;
    private String fLabel;
    private String fModifierString;

    /**
     * @param id
     * @param label
     * @param desc
     */
    public TextHoverDescriptor(String id, String label, String desc) {
      fId = id;
      fLabel = label;
      fDescription = desc;
    }

    /**
     * @param id
     * @param label
     * @param desc
     * @param enabled
     * @param modifierString
     */
    public TextHoverDescriptor(String id, String label, String desc, boolean enabled,
        String modifierString) {
      fId = id;
      fLabel = label;
      fDescription = desc;
      fEnabled = enabled;
      fModifierString = modifierString;
    }

    /**
     * @return Returns the fDescription.
     */
    public String getDescription() {
      return fDescription;
    }

    /**
     * @return Returns the fId.
     */
    public String getId() {
      return fId;
    }

    /**
     * @return Returns the fLabel
     */
    public String getLabel() {
      return fLabel;
    }

    /**
     * @return Returns the fModifierString.
     */
    public String getModifierString() {
      return fModifierString;
    }

    /**
     * @return Returns the fEnabled.
     */
    public boolean isEnabled() {
      return fEnabled;
    }

    /**
     * @param enabled The fEnabled to set.
     */
    public void setEnabled(boolean enabled) {
      fEnabled = enabled;
    }

    /**
     * @param modifierString The fModifierString to set.
     */
    public void setModifierString(String modifierString) {
      fModifierString = modifierString;
    }
  }

  public static final String ANNOTATION_HOVER = "annotationHover"; //$NON-NLS-1$

  // list of different types of Source editor hovers
  public static final String COMBINATION_HOVER = "combinationHover"; //$NON-NLS-1$
  // hover descriptions are in .properties file with the key in the form of
  // "[id]_desc"
  private static final String DESCRIPTION_KEY = "_desc"; //$NON-NLS-1$
  public static final String DOCUMENTATION_HOVER = "documentationHover"; //$NON-NLS-1$
  public static final String HOVER_ATTRIBUTE_SEPARATOR = "|"; //$NON-NLS-1$
  public static final String HOVER_SEPARATOR = ";"; //$NON-NLS-1$

  // hover labels are in .properties file with the key in the form of
  // "[id]_label"
  private static final String LABEL_KEY = "_label"; //$NON-NLS-1$

  public static final String NO_MODIFIER = "0"; //$NON-NLS-1$
  public static final String PROBLEM_HOVER = "problemHover"; //$NON-NLS-1$
  public static final String[] TEXT_HOVER_IDS = new String[] {
      COMBINATION_HOVER, PROBLEM_HOVER, DOCUMENTATION_HOVER, ANNOTATION_HOVER};
  /**
   * Current list of Structured Text editor text hovers
   */
  private TextHoverDescriptor[] fTextHovers;

  public TextHoverManager() {
    super();
  }

  /**
   * Create a best match hover with the give text hover as the documentation hover
   * 
   * @param infoHover
   * @return ITextHover
   * @deprecated as of WTP 3.0 M3
   */
  public ITextHover createBestMatchHover(ITextHover infoHover) {
    return new BestMatchHover(infoHover);
  }

  /**
   * Generate a list of text hover descriptors from the given delimited string
   * 
   * @param textHoverStrings
   * @return
   */
  public TextHoverDescriptor[] generateTextHoverDescriptors(String textHoverStrings) {
    StringTokenizer st = new StringTokenizer(textHoverStrings, HOVER_SEPARATOR);

    // read from preference and load id-descriptor mapping to a hash table
    HashMap idToModifier = new HashMap(st.countTokens());
    while (st.hasMoreTokens()) {
      String textHoverString = st.nextToken();
      StringTokenizer st2 = new StringTokenizer(textHoverString, HOVER_ATTRIBUTE_SEPARATOR);
      if (st2.countTokens() == 3) {
        String id = st2.nextToken();
        boolean enabled = Boolean.valueOf(st2.nextToken()).booleanValue();
        String modifierString = st2.nextToken();
        if (modifierString.equals(NO_MODIFIER))
          modifierString = ""; //$NON-NLS-1$

        String label = null;
        String description = null;
        try {
          label = SSEUIMessages.getResourceBundle().getString(id + LABEL_KEY);
          description = SSEUIMessages.getResourceBundle().getString(id + DESCRIPTION_KEY);
        } catch (Exception e) {
          Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
        }
        TextHoverDescriptor descriptor = new TextHoverDescriptor(id, label, description, enabled,
            modifierString);
        // should check to see if ids appear more than once
        idToModifier.put(id, descriptor);
      }
    }

    // go through all defined text hovers and match with their preference
    TextHoverDescriptor[] descriptors = new TextHoverDescriptor[TEXT_HOVER_IDS.length];
    for (int i = 0; i < TEXT_HOVER_IDS.length; i++) {
      TextHoverDescriptor desc = (TextHoverDescriptor) idToModifier.get(TEXT_HOVER_IDS[i]);
      if (desc != null) {
        descriptors[i] = desc;
      } else {
        String label = null;
        String description = null;
        try {
          label = SSEUIMessages.getResourceBundle().getString(TEXT_HOVER_IDS[i] + LABEL_KEY);
          description = SSEUIMessages.getResourceBundle().getString(
              TEXT_HOVER_IDS[i] + DESCRIPTION_KEY);
        } catch (Exception e) {
          Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
        }
        descriptors[i] = new TextHoverDescriptor(TEXT_HOVER_IDS[i], label, description);
      }
    }
    return descriptors;
  }

  private IPreferenceStore getPreferenceStore() {
    return SSEUIPlugin.getDefault().getPreferenceStore();
  }

  /**
   * Returns the text hovers for Structured Text editor. If fTextHover has not been initialied, it
   * will be initialized.
   * 
   * @return Returns the fTextHovers.
   */
  public TextHoverDescriptor[] getTextHovers() {
    if (fTextHovers == null) {
      String textHoverStrings = getPreferenceStore().getString(
          EditorPreferenceNames.EDITOR_TEXT_HOVER_MODIFIERS);
      fTextHovers = generateTextHoverDescriptors(textHoverStrings);
    }
    return fTextHovers;
  }

  /**
   * Sets fTextHovers to null so that next time getTextHovers is called, fTextHovers will be
   * populated with the latest preferences.
   */
  public void resetTextHovers() {
    fTextHovers = null;
  }
}
