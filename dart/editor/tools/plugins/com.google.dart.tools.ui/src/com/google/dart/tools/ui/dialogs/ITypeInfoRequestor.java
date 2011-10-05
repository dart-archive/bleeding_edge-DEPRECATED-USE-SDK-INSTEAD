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

/**
 * An interfaces to give access to the type presented in type selection dialogs like the open type
 * dialog.
 * <p>
 * Please note that <code>ITypeInfoRequestor</code> objects <strong>don't </strong> have value
 * semantic. The state of the object might change over time especially since objects are reused for
 * different call backs.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface ITypeInfoRequestor {

  /**
   * Returns a dot separated string of the enclosing types or an empty string if the type is a top
   * level type.
   * 
   * @return a dot separated string of the enclosing types
   */
  public String getEnclosingName();

  /**
   * Returns the type's modifiers. The modifiers can be inspected using the class
   * {@link org.eclipse.wst.jsdt.core.Flags}.
   * 
   * @return the type's modifiers
   */
  public int getModifiers();

  /**
   * Returns the package name.
   * 
   * @return the info's package name.
   */
  public String getPackageName();

  /**
   * Returns the type name.
   * 
   * @return the info's type name.
   */
  public String getTypeName();
}
