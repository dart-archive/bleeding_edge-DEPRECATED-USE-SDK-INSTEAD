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

package com.google.dart.tools.core.internal.util;

import org.eclipse.core.resources.IFile;

import java.io.File;

/**
 * This class exists to de-couple users of the ResourceUtil class from dartc dependencies.
 */
public class ResourceUtil2 {

  /**
   * Return the file resource associated with the given file, or <code>null</code> if the file does
   * not correspond to an existing file resource.
   * 
   * @param file the file representing the file resource to be returned
   * @return the file resource associated with the given file
   */
  public static IFile getFile(File file) {
    return ResourceUtil.getFile(file);
  }

  private ResourceUtil2() {

  }

}
