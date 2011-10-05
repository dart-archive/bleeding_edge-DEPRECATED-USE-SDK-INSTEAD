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
package com.google.dart.tools.core.model;

/**
 * The interface <code>HTMLFile</code> defines the behavior of HTML files contained within a project
 * that reference one or more libraries.
 */
public interface HTMLFile extends DartElement {
  /**
   * Return a possibly empty array containing all of the libraries referenced by this HTML file.
   * 
   * @return an array containing all of the libraries referenced by this HTML file
   * @throws DartModelException if the referenced libraries could not be determined for some reason
   */
  public DartLibrary[] getReferencedLibraries() throws DartModelException;
}
