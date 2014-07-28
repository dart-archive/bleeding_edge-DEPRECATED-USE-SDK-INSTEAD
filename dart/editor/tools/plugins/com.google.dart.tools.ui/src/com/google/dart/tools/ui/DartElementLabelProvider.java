/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui;

import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;
import com.google.dart.tools.ui.internal.viewsupport.StorageLabelProvider;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * Standard label provider for JavaScript elements. Use this class when you want to present the
 * JavaScript elements in a viewer.
 * <p>
 * The implementation also handles non-JavaScript elements by forwarding the requests to the
 * <code>IWorkbenchAdapter</code> of the element.
 * </p>
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class DartElementLabelProvider extends LabelProvider implements IStyledLabelProvider {

  /**
   * Flag (bit mask) indicating that methods labels include the method return type (appended).
   */
  public final static int SHOW_RETURN_TYPE = 0x001;

  /**
   * Flag (bit mask) indicating that method label include parameter types.
   */
  public final static int SHOW_PARAMETERS = 0x002;

  /**
   * Flag (bit mask) indicating that the label of a member should include the container. For
   * example, include the name of the type enclosing a field.
   * 
   * @deprecated Use SHOW_QUALIFIED or SHOW_ROOT instead
   */
  @Deprecated
  public final static int SHOW_CONTAINER = 0x004;

  /**
   * Flag (bit mask) indicating that the label of a type should be fully qualified. For example,
   * include the fully qualified name of the type enclosing a type.
   * 
   * @deprecated Use SHOW_QUALIFIED instead
   */
  @Deprecated
  public final static int SHOW_CONTAINER_QUALIFICATION = 0x008;

  /**
   * Flag (bit mask) indicating that the label should include overlay icons for element type and
   * modifiers.
   */
  public final static int SHOW_OVERLAY_ICONS = 0x010;

  /**
   * Flag (bit mask) indicating that a field label should include the declared type.
   */
  public final static int SHOW_TYPE = 0x020;

  /**
   * Flag (bit mask) indicating that the label should include the name of the package fragment root
   * (appended).
   */
  public final static int SHOW_ROOT = 0x040;

  /**
   * Flag (bit mask) indicating that the label qualification of a type should be shown after the
   * name.
   * 
   * @deprecated SHOW_POST_QUALIFIED instead
   */
  @Deprecated
  public final static int SHOW_POSTIFIX_QUALIFICATION = 0x080;

  /**
   * Flag (bit mask) indicating that the label should show the icons with no space reserved for
   * overlays.
   */
  public final static int SHOW_SMALL_ICONS = 0x100;

  /**
   * Flag (bit mask) indicating that the package fragment roots from class path variables should be
   * rendered with the variable in the name
   */
  public final static int SHOW_VARIABLE = 0x200;

  /**
   * Flag (bit mask) indicating that compilation units, class files, types, declarations and members
   * should be rendered qualified. Examples: <code>java.lang.String</code>,
   * <code>java.util.Vector.size()</code>
   */
  public final static int SHOW_QUALIFIED = 0x400;

  /**
   * Flag (bit mask) indicating that compilation units, class files, types, declarations and members
   * should be rendered qualified.The qualification is appended. Examples:
   * <code>String - java.lang</code>, <code>size() - java.util.Vector</code>
   */
  public final static int SHOW_POST_QUALIFIED = 0x800;

  /**
   * Constant (value <code>0</code>) indicating that the label should show the basic images only.
   */
  public final static int SHOW_BASICS = 0x000;

  /**
   * Constant indicating the default label rendering. Currently the default is equivalent to
   * <code>SHOW_PARAMETERS | SHOW_OVERLAY_ICONS</code>.
   */
  public final static int SHOW_DEFAULT = new Integer(SHOW_PARAMETERS | SHOW_OVERLAY_ICONS).intValue();

  private DartElementImageProvider fImageLabelProvider;

  private StorageLabelProvider fStorageLabelProvider;
  private int fFlags;
  private int fImageFlags;
  private long fTextFlags;

  /**
   * Creates a new label provider with <code>SHOW_DEFAULT</code> flag.
   * 
   * @see #SHOW_DEFAULT
   */
  public DartElementLabelProvider() {
    this(SHOW_DEFAULT);
  }

  /**
   * Creates a new label provider.
   * 
   * @param flags the initial options; a bitwise OR of <code>SHOW_* </code> constants
   */
  public DartElementLabelProvider(int flags) {
    fImageLabelProvider = new DartElementImageProvider();
    fStorageLabelProvider = new StorageLabelProvider();
    fFlags = flags;
    updateImageProviderFlags();
    updateTextProviderFlags();
  }

  @Override
  public void dispose() {
    fStorageLabelProvider.dispose();
    fImageLabelProvider.dispose();
  }

  @Override
  public Image getImage(Object element) {
    Image result = fImageLabelProvider.getImageLabel(element, fImageFlags);
    if (result != null) {
      return result;
    }

    if (element instanceof IStorage) {
      return fStorageLabelProvider.getImage(element);
    }

    return result;
  }

  @Override
  public StyledString getStyledText(Object element) {
    return new StyledString(getText(element));
  }

  @Override
  public String getText(Object element) {
    String text = DartElementLabels.getTextLabel(element, fTextFlags);
    if (text.length() > 0) {
      return text;
    }

    if (element instanceof IStorage) {
      return fStorageLabelProvider.getText(element);
    }

    return text;
  }

  /**
   * Turns off the rendering options specified in the given flags.
   * 
   * @param flags the initial options; a bitwise OR of <code>SHOW_* </code> constants
   */
  public void turnOff(int flags) {
    fFlags &= (~flags);
    updateImageProviderFlags();
    updateTextProviderFlags();
  }

  /**
   * Turns on the rendering options specified in the given flags.
   * 
   * @param flags the options; a bitwise OR of <code>SHOW_* </code> constants
   */
  public void turnOn(int flags) {
    fFlags |= flags;
    updateImageProviderFlags();
    updateTextProviderFlags();
  }

  private boolean getFlag(int flag) {
    return (fFlags & flag) != 0;
  }

  private void updateImageProviderFlags() {
    fImageFlags = 0;
    if (getFlag(SHOW_OVERLAY_ICONS)) {
      fImageFlags |= DartElementImageProvider.OVERLAY_ICONS;
    }
    if (getFlag(SHOW_SMALL_ICONS)) {
      fImageFlags |= DartElementImageProvider.SMALL_ICONS;
    }
  }

  private void updateTextProviderFlags() {
    fTextFlags = DartElementLabels.T_TYPE_PARAMETERS;
    if (getFlag(SHOW_RETURN_TYPE)) {
      fTextFlags |= DartElementLabels.M_APP_RETURNTYPE;
    }
    if (getFlag(SHOW_PARAMETERS)) {
      fTextFlags |= DartElementLabels.M_PARAMETER_TYPES;
    }
    if (getFlag(SHOW_CONTAINER)) {
      fTextFlags |= DartElementLabels.P_POST_QUALIFIED | DartElementLabels.T_POST_QUALIFIED
          | DartElementLabels.CF_POST_QUALIFIED | DartElementLabels.CU_POST_QUALIFIED
          | DartElementLabels.M_POST_QUALIFIED | DartElementLabels.F_POST_QUALIFIED;
    }
    if (getFlag(SHOW_POSTIFIX_QUALIFICATION)) {
      fTextFlags |= (DartElementLabels.T_POST_QUALIFIED | DartElementLabels.CF_POST_QUALIFIED | DartElementLabels.CU_POST_QUALIFIED);
    } else if (getFlag(SHOW_CONTAINER_QUALIFICATION)) {
      fTextFlags |= (DartElementLabels.T_FULLY_QUALIFIED | DartElementLabels.CF_QUALIFIED | DartElementLabels.CU_QUALIFIED);
    }
    if (getFlag(SHOW_TYPE)) {
      fTextFlags |= DartElementLabels.F_APP_TYPE_SIGNATURE;
    }
    if (getFlag(SHOW_ROOT)) {
      fTextFlags |= DartElementLabels.APPEND_ROOT_PATH;
    }
    if (getFlag(SHOW_VARIABLE)) {
      fTextFlags |= DartElementLabels.ROOT_VARIABLE;
    }
    if (getFlag(SHOW_QUALIFIED)) {
      fTextFlags |= (DartElementLabels.F_FULLY_QUALIFIED | DartElementLabels.M_FULLY_QUALIFIED
          | DartElementLabels.I_FULLY_QUALIFIED | DartElementLabels.T_FULLY_QUALIFIED
          | DartElementLabels.D_QUALIFIED | DartElementLabels.CF_QUALIFIED | DartElementLabels.CU_QUALIFIED);
    }
    if (getFlag(SHOW_POST_QUALIFIED)) {
      fTextFlags |= (DartElementLabels.F_POST_QUALIFIED | DartElementLabels.M_POST_QUALIFIED
          | DartElementLabels.I_POST_QUALIFIED | DartElementLabels.T_POST_QUALIFIED
          | DartElementLabels.D_POST_QUALIFIED | DartElementLabels.CF_POST_QUALIFIED | DartElementLabels.CU_POST_QUALIFIED);
    }
  }
}
