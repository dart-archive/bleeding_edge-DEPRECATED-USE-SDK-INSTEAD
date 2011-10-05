/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;

/**
 * DartUILabelProvider that respects settings from the Appearance preference page. Triggers a viewer
 * update when a preference changes.
 */
public class AppearanceAwareLabelProvider extends DartUILabelProvider implements
    IPropertyChangeListener, IPropertyListener {

  public final static long DEFAULT_TEXTFLAGS = DartElementLabels.ROOT_VARIABLE
      | DartElementLabels.T_TYPE_PARAMETERS | DartElementLabels.M_PARAMETER_NAMES
      | DartElementLabels.M_APP_TYPE_PARAMETERS | DartElementLabels.M_APP_RETURNTYPE
      | DartElementLabels.REFERENCED_ROOT_POST_QUALIFIED;
  public final static int DEFAULT_IMAGEFLAGS = DartElementImageProvider.OVERLAY_ICONS;

  private long fTextFlagMask;
  private int fImageFlagMask;

  /**
   * Creates a labelProvider with DEFAULT_TEXTFLAGS and DEFAULT_IMAGEFLAGS
   */
  public AppearanceAwareLabelProvider() {
    this(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS);
  }

  /**
   * Constructor for AppearanceAwareLabelProvider.
   */
  public AppearanceAwareLabelProvider(long textFlags, int imageFlags) {
    super(textFlags, imageFlags);
    initMasks();
    PreferenceConstants.getPreferenceStore().addPropertyChangeListener(this);
    PlatformUI.getWorkbench().getEditorRegistry().addPropertyListener(this);
  }

  /*
   * @see IBaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    PreferenceConstants.getPreferenceStore().removePropertyChangeListener(this);
    PlatformUI.getWorkbench().getEditorRegistry().removePropertyListener(this);
    super.dispose();
  }

  /*
   * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent event) {
    String property = event.getProperty();
    if (property.equals(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE)
        || property.equals(PreferenceConstants.APPEARANCE_METHOD_TYPEPARAMETERS)
        || property.equals(PreferenceConstants.APPEARANCE_CATEGORY)
        || property.equals(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW)
        || property.equals(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES)) {
      initMasks();
      LabelProviderChangedEvent lpEvent = new LabelProviderChangedEvent(this, null); // refresh all
      fireLabelProviderChanged(lpEvent);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IPropertyListener#propertyChanged(java.lang.Object, int)
   */
  @Override
  public void propertyChanged(Object source, int propId) {
    if (propId == IEditorRegistry.PROP_CONTENTS) {
      fireLabelProviderChanged(new LabelProviderChangedEvent(this, null)); // refresh
// all
    }
  }

  /*
   * @see DartUILabelProvider#evaluateImageFlags()
   */
  @Override
  protected int evaluateImageFlags(Object element) {
    return getImageFlags() & fImageFlagMask;
  }

  /*
   * @see DartUILabelProvider#evaluateTextFlags()
   */
  @Override
  protected long evaluateTextFlags(Object element) {
    return getTextFlags() & fTextFlagMask;
  }

  private void initMasks() {
    IPreferenceStore store = PreferenceConstants.getPreferenceStore();
    fTextFlagMask = -1;
    if (!store.getBoolean(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE)) {
      fTextFlagMask ^= DartElementLabels.M_APP_RETURNTYPE;
    }
    if (!store.getBoolean(PreferenceConstants.APPEARANCE_METHOD_TYPEPARAMETERS)) {
      fTextFlagMask ^= DartElementLabels.M_APP_TYPE_PARAMETERS;
    }
    if (!store.getBoolean(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES)) {
      fTextFlagMask ^= DartElementLabels.P_COMPRESSED;
    }
    if (!store.getBoolean(PreferenceConstants.APPEARANCE_CATEGORY)) {
      fTextFlagMask ^= DartElementLabels.ALL_CATEGORY;
    }

    fImageFlagMask = -1;
  }

}
