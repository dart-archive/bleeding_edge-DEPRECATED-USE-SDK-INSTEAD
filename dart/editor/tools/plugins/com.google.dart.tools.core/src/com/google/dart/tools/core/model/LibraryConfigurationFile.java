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
 * The interface <code>LibraryConfigurationFile</code> defines the behavior of objects representing
 * a Dart library file.
 */
@Deprecated
public interface LibraryConfigurationFile extends OpenableElement,
    SourceFileElement<LibraryConfigurationFile> {
  /**
   * Return <code>true</code> if this configuration file represents an application configuration
   * file.
   * 
   * @return <code>true</code> if this configuration file represents an application configuration
   *         file
   */
  @Deprecated
  public boolean isApplicationFile();
}
