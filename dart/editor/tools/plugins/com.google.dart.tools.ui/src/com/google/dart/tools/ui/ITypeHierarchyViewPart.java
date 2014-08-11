/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.model.DartElement;

import org.eclipse.ui.IViewPart;

/**
 * The standard type hierarchy view presents a type hierarchy for a given input class or interface.
 * Visually, this view consists of a pair of viewers, one showing the type hierarchy, the other
 * showing the members of the type selected in the first.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 * 
 * @see DartUI#ID_TYPE_HIERARCHY
 */
public interface ITypeHierarchyViewPart extends IViewPart {

  /**
   * Constant used for the vertical view layout.
   */
  public static final int VIEW_LAYOUT_VERTICAL = 0;

  /**
   * Constant used for the horizontal view layout.
   */
  public static final int VIEW_LAYOUT_HORIZONTAL = 1;

  /**
   * Constant used for the single view layout (no members view)
   */
  public static final int VIEW_LAYOUT_SINGLE = 2;

  /**
   * Constant used for the automatic view layout.
   */
  public static final int VIEW_LAYOUT_AUTOMATIC = 3;

  /**
   * Constant used for the 'classic' type hierarchy mode.
   */
  public static final int HIERARCHY_MODE_CLASSIC = 2;

  /**
   * Constant used for the super types hierarchy mode.
   */
  public static final int HIERARCHY_MODE_SUPERTYPES = 0;

  /**
   * Constant used for the sub types hierarchy mode.
   */
  public static final int HIERARCHY_MODE_SUBTYPES = 1;

  /**
   * Returns the currently configured hierarchy mode. Possible modes are
   * {@link #HIERARCHY_MODE_SUBTYPES}, {@link #HIERARCHY_MODE_SUPERTYPES} and
   * {@link #HIERARCHY_MODE_CLASSIC} but clients should also be able to handle yet unknown modes.
   * 
   * @return The hierarchy mode currently set
   */
  public int getHierarchyMode();

  /**
   * Returns the input element of this type hierarchy view.
   * 
   * @return the input element, or <code>null</code> if no input element is set
   * @see #setInputElement(DartElement)
   */
  public DartElement getInputElement();

  /**
   * Returns the currently configured view layout. Possible layouts are
   * {@link #VIEW_LAYOUT_VERTICAL}, {@link #VIEW_LAYOUT_HORIZONTAL} {@link #VIEW_LAYOUT_SINGLE} and
   * {@link #VIEW_LAYOUT_AUTOMATIC} but clients should also be able to handle yet unknown layout.
   * 
   * @return The layout currently set
   */
  public int getViewLayout();

  /**
   * Returns whether this type hierarchy view's selection automatically tracks the active editor.
   * 
   * @return <code>true</code> if linking is enabled, <code>false</code> if not
   */
  public boolean isLinkingEnabled();

  /**
   * If set, type names are shown with the parent container's name.
   * 
   * @return returns if type names are shown with the parent container's name.
   */
  public boolean isQualifiedTypeNamesEnabled();

  /**
   * If set, the lock mode is enabled.
   * 
   * @return returns if the lock mode is enabled.
   */
  public boolean isShowMembersInHierarchy();

  /**
   * Sets the hierarchy mode. Valid modes are {@link #HIERARCHY_MODE_SUBTYPES},
   * {@link #HIERARCHY_MODE_SUPERTYPES} and {@link #HIERARCHY_MODE_CLASSIC}.
   * 
   * @param mode The hierarchy mode to set
   */
  public void setHierarchyMode(int mode);

  /**
   * Sets the input element of this type hierarchy view. The following input types are possible
   * <code>IMember</code> (types, methods, fields..), <code>IPackageFragment</code>,
   * <code>IPackageFragmentRoot</code> and <code>DartProject</code>.
   * 
   * @param element the input element of this type hierarchy view, or <code>null</code> to clear any
   *          input
   */
  public void setInputElement(DartElement element);

  /**
   * Sets whether this type hierarchy view's selection automatically tracks the active editor.
   * 
   * @param enabled <code>true</code> to enable, <code>false</code> to disable
   */
  public void setLinkingEnabled(boolean enabled);

  /**
   * Sets the view layout. Valid inputs are {@link #VIEW_LAYOUT_VERTICAL},
   * {@link #VIEW_LAYOUT_HORIZONTAL} {@link #VIEW_LAYOUT_SINGLE} and {@link #VIEW_LAYOUT_AUTOMATIC}.
   * 
   * @param layout The layout to set
   */
  public void setViewLayout(int layout);

  /**
   * Locks the the members view and shows the selected members in the hierarchy.
   * 
   * @param enabled If set, the members view will be locked and the selected members are shown in
   *          the hierarchy.
   */
  public void showMembersInHierarchy(boolean enabled);

  /**
   * Specifies if type names are shown with the parent container's name.
   * 
   * @param enabled if enabled, the hierarchy will also show the type container names
   */
  public void showQualifiedTypeNames(boolean enabled);

}
