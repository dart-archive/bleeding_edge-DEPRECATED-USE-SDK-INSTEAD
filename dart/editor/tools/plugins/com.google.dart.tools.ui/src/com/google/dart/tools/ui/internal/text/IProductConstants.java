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
package com.google.dart.tools.ui.internal.text;

/**
 * These constants define the set of properties that this plug-in expects to be available via
 * <code>ProductProperties#getProperty(String)</code>. The status of this interface and the
 * facilities offered is highly provisional. Productization support will be reviewed and possibly
 * modified in future releases.
 * 
 * @see org.eclipse.core.runtime.IProduct#getProperty(String)
 */

public interface IProductConstants {
  /**
   * The "explorer" view to use when creating the JavaScript perspective
   */
  public static final String PERSPECTIVE_EXPLORER_VIEW = "idPerspectiveHierarchyView"; //$NON-NLS-1$

}
