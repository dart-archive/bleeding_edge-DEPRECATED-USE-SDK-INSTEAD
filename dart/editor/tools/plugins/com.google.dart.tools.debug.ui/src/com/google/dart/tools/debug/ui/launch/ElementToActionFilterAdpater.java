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
package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.core.model.HTMLFile;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IActionFilter;

/**
 * A filter for the Run in Browser context menu contribution in the Libraries view
 */
public class ElementToActionFilterAdpater implements IActionFilter {

  private static ElementToActionFilterAdpater INSTANCE = new ElementToActionFilterAdpater();

  public static ElementToActionFilterAdpater getInstance() {

    return INSTANCE;
  }

  private ElementToActionFilterAdpater() {

  }

  @Override
  public boolean testAttribute(Object target, String name, String value) {

    IAdaptable element = (IAdaptable) target;
    if (element instanceof HTMLFile) {
      return true;
    }
    return false;
  }

}
