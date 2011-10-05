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

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IPageLayout;

public class ProductProperties {

  /**
   * Default values for WTP level product
   */
  public static final String ID_PERSPECTIVE_EXPLORER_VIEW = IPageLayout.ID_PROJECT_EXPLORER;

  /**
   * Return the value for the associated key from the Platform Product registry or return the WTP
   * default for the JavaScript cases.
   * 
   * @param key
   * @return String value of product's property
   */
  public static String getProperty(String key) {
    if (key == null) {
      return null;
    }
    String value = null;
    if (Platform.getProduct() != null) {
      value = Platform.getProduct().getProperty(key);
    }
    if (value == null) {
      if (key.equals(IProductConstants.PERSPECTIVE_EXPLORER_VIEW)) {
        return ID_PERSPECTIVE_EXPLORER_VIEW;
      }
    }
    return value;
  }

}
