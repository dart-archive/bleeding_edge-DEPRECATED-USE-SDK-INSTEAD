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
package com.google.dart.tools.ui.dialogs;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

/**
 * The class provides API to extend type selection dialogs like the open type dialog.
 * <p>
 * The class should be subclassed by clients wishing to extend the type selection dialog.
 * </p>
 * 
 * @see org.eclipse.wst.jsdt.ui.JavaScriptUI#createTypeDialog(org.eclipse.swt.widgets.Shell,
 *      org.eclipse.jface.operation.IRunnableContext,
 *      org.eclipse.wst.jsdt.core.search.IJavaScriptSearchScope, int, boolean, String,
 *      TypeSelectionExtension) Provisional API: This class/interface is part of an interim API that
 *      is still under development and expected to change significantly before reaching stability.
 *      It is being made available at this early stage to solicit feedback from pioneering adopters
 *      on the understanding that any code that uses this API will almost certainly be broken
 *      (repeatedly) as the API evolves.
 */
public abstract class TypeSelectionExtension {

  private ITypeSelectionComponent fComponent;

  /**
   * Creates the content area which the extensions contributes to the type selection dialog. The
   * area will be presented between the table showing the list of types and the optional status
   * line.
   * 
   * @param parent the parent of the additional content area
   * @return the additional content area or <code>null</code> if no additional content area is
   *         required
   */
  public Control createContentArea(Composite parent) {
    return null;
  }

  /**
   * Returns the filter extension or <code>null</code> if no additional filtering is required.
   * 
   * @return the additional filter extension
   */
  public ITypeInfoFilterExtension getFilterExtension() {
    return null;
  }

  /**
   * Returns an image provider or <code>null</code> if the standard images should be used.
   * 
   * @return the image provider
   */
  public ITypeInfoImageProvider getImageProvider() {
    return null;
  }

  /**
   * Returns the selection validator or <code>null</code> if selection validation is not required.
   * The elements passed to the selection validator are of type
   * {@link org.eclipse.wst.jsdt.core.IType}.
   * 
   * @return the selection validator or <code>null</code>
   */
  public ISelectionStatusValidator getSelectionValidator() {
    return null;
  }

  /**
   * Returns the type selection dialog or <code>null</code> if the extension has not been
   * initialized yet.
   * 
   * @return the type selection dialog or <code>null</code>
   */
  public final ITypeSelectionComponent getTypeSelectionComponent() {
    return fComponent;
  }

  /**
   * Initializes the type dialog extension with the given type dialog
   * 
   * @param component the type dialog hosting this extension
   */
  public final void initialize(ITypeSelectionComponent component) {
    fComponent = component;
  }
}
