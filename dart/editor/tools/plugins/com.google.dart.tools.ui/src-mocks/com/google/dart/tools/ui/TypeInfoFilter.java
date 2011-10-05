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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.ui.dialogs.ITypeInfoFilterExtension;

/**
 * TODO(brianwilkerson): This is a temporary interface, used to resolve compilation errors.
 */
public class TypeInfoFilter {
  public TypeInfoFilter(String string, SearchScope fSearchScope, int fElementKind,
      ITypeInfoFilterExtension fFilterExtension) {
  }

  public String getNamePattern() {
    return null;
  }

  public int getPackageFlags() {
    return 0;
  }

  public String getPackagePattern() {
    return null;
  }

  public int getSearchFlags() {
    return 0;
  }

  public String getText() {
    return null;
  }

  public boolean isCamelCasePattern() {
    return false;
  }

  public boolean isSubFilter(String text) {
    return false;
  }
}
