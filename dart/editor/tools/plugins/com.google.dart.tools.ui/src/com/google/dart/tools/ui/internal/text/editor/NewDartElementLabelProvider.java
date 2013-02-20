/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;
import com.google.dart.tools.ui.internal.viewsupport.NewDartElementImageProvider;
import com.google.dart.tools.ui.internal.viewsupport.StorageLabelProvider;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;

/**
 * Standard label provider for Dart elements. Use this class when you want to present Dart elements
 * in a viewer.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * <b>NOTE:</b> this will replace {@link DartElementLabelProvider}.
 */
public class NewDartElementLabelProvider extends LabelProvider implements IStyledLabelProvider {

  /**
   * Flag (bit mask) indicating that the label should include overlay icons for element type and
   * modifiers.
   */
  public final static int SHOW_OVERLAY_ICONS = 0x010;

  /**
   * Flag (bit mask) indicating that method label include parameter types.
   */
  public final static int SHOW_PARAMETERS = 0x002;

  /**
   * Flag (bit mask) indicating that compilation units, types, declarations and members should be
   * rendered qualified. The qualification is appended.
   */
  public final static int SHOW_POST_QUALIFIED = 0x800;

  /**
   * Flag (bit mask) indicating that compilation units, types, declarations and members should be
   * rendered qualified.
   */
  public final static int SHOW_QUALIFIED = 0x400;

  /**
   * Flag (bit mask) indicating that methods labels include the method return type (appended).
   */
  public final static int SHOW_RETURN_TYPE = 0x001;

  /**
   * Flag (bit mask) indicating that the label should include the name of the package fragment root
   * (appended).
   */
  public final static int SHOW_ROOT = 0x040;

  /**
   * Flag (bit mask) indicating that the label should show the icons with no space reserved for
   * overlays.
   */
  public final static int SHOW_SMALL_ICONS = 0x100;

  /**
   * Flag (bit mask) indicating that a field label should include the declared type.
   */
  public final static int SHOW_TYPE = 0x020;

  /**
   * Constant (value <code>0</code>) indicating that the label should show the basic images only.
   */
  public final static int SHOW_BASICS = 0x000;

  /**
   * Constant indicating the default label rendering. Currently the default is equivalent to
   * <code>SHOW_PARAMETERS | SHOW_OVERLAY_ICONS</code>.
   */
  public final static int SHOW_DEFAULT = new Integer(SHOW_PARAMETERS | SHOW_OVERLAY_ICONS).intValue();

  private long textFlags;
  private int imageFlags;
  private final int providedFlags;

  private StorageLabelProvider storageLabelProvider;
  private NewDartElementImageProvider imageLabelProvider;

  private ArrayList<ILabelDecorator> labelDecorators;

  /**
   * Creates a new label provider with <code>SHOW_DEFAULT</code> flag.
   * 
   * @see #SHOW_DEFAULT
   */
  public NewDartElementLabelProvider() {
    this(SHOW_DEFAULT);
  }

  /**
   * Creates a new label provider.
   * 
   * @param flags the initial options; a bitwise OR of <code>SHOW_* </code> constants
   */
  public NewDartElementLabelProvider(int flags) {
    this.providedFlags = flags;
    imageLabelProvider = new NewDartElementImageProvider();
    storageLabelProvider = new StorageLabelProvider();
    initImageProviderFlags();
    initTextProviderFlags();
  }

  /**
   * Adds a decorator to the label provider
   * 
   * @param decorator the decorator to add
   */
  public void addLabelDecorator(ILabelDecorator decorator) {
    if (labelDecorators == null) {
      labelDecorators = new ArrayList<ILabelDecorator>(2);
    }
    labelDecorators.add(decorator);
  }

  @Override
  public void dispose() {

    super.dispose();

    if (labelDecorators != null) {
      for (ILabelDecorator decorator : labelDecorators) {
        decorator.dispose();
      }
      labelDecorators = null;
    }

    imageLabelProvider.dispose();
    storageLabelProvider.dispose();
  }

  @Override
  public Image getImage(Object element) {

    Image result = imageLabelProvider.getImageLabel(element, imageFlags);
    if (result == null && element instanceof IStorage) {
      result = storageLabelProvider.getImage(element);
    }

    return decorateImage(result, element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    return new StyledString(getText(element));
  }

  @Override
  public String getText(Object element) {
    String result = DartElementLabels.getTextLabel(element, textFlags);
    return decorateText(result, element);
  }

  protected Image decorateImage(Image image, Object element) {
    if (labelDecorators != null && image != null) {
      for (ILabelDecorator decorator : labelDecorators) {
        image = decorator.decorateImage(image, element);
      }
    }
    return image;
  }

  protected String decorateText(String text, Object element) {
    if (labelDecorators != null && text != null && text.length() > 0) {
      for (ILabelDecorator decorator : labelDecorators) {
        String decorated = decorator.decorateText(text, element);
        if (decorated != null) {
          text = decorated;
        }
      }
    }
    return text;
  }

  private boolean getFlag(int flag) {
    return (providedFlags & flag) != 0;
  }

  private void initImageProviderFlags() {
    imageFlags = 0;
    if (getFlag(SHOW_OVERLAY_ICONS)) {
      imageFlags |= DartElementImageProvider.OVERLAY_ICONS;
    }
    if (getFlag(SHOW_SMALL_ICONS)) {
      imageFlags |= DartElementImageProvider.SMALL_ICONS;
    }
  }

  private void initTextProviderFlags() {
    textFlags = DartElementLabels.T_TYPE_PARAMETERS;
    if (getFlag(SHOW_RETURN_TYPE)) {
      textFlags |= DartElementLabels.M_APP_RETURNTYPE;
    }
    if (getFlag(SHOW_PARAMETERS)) {
      textFlags |= DartElementLabels.M_PARAMETER_TYPES;
    }
    if (getFlag(SHOW_TYPE)) {
      textFlags |= DartElementLabels.F_APP_TYPE_SIGNATURE;
    }
    if (getFlag(SHOW_ROOT)) {
      textFlags |= DartElementLabels.APPEND_ROOT_PATH;
    }
    if (getFlag(SHOW_QUALIFIED)) {
      textFlags |= (DartElementLabels.F_FULLY_QUALIFIED | DartElementLabels.M_FULLY_QUALIFIED
          | DartElementLabels.I_FULLY_QUALIFIED | DartElementLabels.T_FULLY_QUALIFIED
          | DartElementLabels.D_QUALIFIED | DartElementLabels.CF_QUALIFIED | DartElementLabels.CU_QUALIFIED);
    }
    if (getFlag(SHOW_POST_QUALIFIED)) {
      textFlags |= (DartElementLabels.F_POST_QUALIFIED | DartElementLabels.M_POST_QUALIFIED
          | DartElementLabels.I_POST_QUALIFIED | DartElementLabels.T_POST_QUALIFIED
          | DartElementLabels.D_POST_QUALIFIED | DartElementLabels.CF_POST_QUALIFIED | DartElementLabels.CU_POST_QUALIFIED);
    }
  }

}
