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
package com.google.dart.core;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.mock.ui.IIncludePathEntry;

/**
 * TODO(brianwilkerson): This is a temporary interface, used to resolve compilation errors.
 */
public interface IPackageFragmentRoot extends DartElement {
  String K_SOURCE = null;

  DartProject getJavaScriptProject();

  IIncludePathEntry getRawIncludepathEntry();

  boolean isArchive();

  boolean isExternal();
}
