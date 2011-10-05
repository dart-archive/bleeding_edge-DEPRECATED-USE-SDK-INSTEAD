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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartLibrary;

/**
 * Instances of the class <code>HTMLFileInfo</code> represent the information known about an HTML
 * file that references one or more libraries.
 */
public class HTMLFileInfo extends OpenableElementInfo {
  /**
   * An array containing all of the libraries referenced by this HTML file.
   */
  private DartLibrary[] referencedLibraries = DartLibraryImpl.EMPTY_ARRAY;

  /**
   * Return an array containing all of the libraries referenced by this HTML file.
   * 
   * @return an array containing all of the libraries referenced by this HTML file
   */
  public DartLibrary[] getReferencedLibraries() {
    return referencedLibraries;
  }

  /**
   * Set the libraries referenced by this HTML file to those in the given array.
   * 
   * @param libraries an array containing all of the libraries referenced by this HTML file
   */
  public void setReferencedLibraries(DartLibrary[] libraries) {
    referencedLibraries = libraries;
  }
}
