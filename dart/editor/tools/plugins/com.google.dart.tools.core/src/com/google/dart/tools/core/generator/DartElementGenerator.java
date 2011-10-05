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
package com.google.dart.tools.core.generator;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Subclasses of <code>DartElementGenerator</code> are used to validate inputs and create new Dart
 * elements after validation.
 */
public class DartElementGenerator {
  protected final IWorkspace workspace = ResourcesPlugin.getWorkspace();

  private String name = "";

  /**
   * Answer the name of the project to be created
   * 
   * @return the new project's name (not <code>null</code>)
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the project to be created
   * 
   * @param name the new project's name
   */
  public void setName(String name) {
    this.name = name != null ? name.trim() : ""; //$NON-NLS-1$
  }

  /**
   * Construct a new Error {@link Status} with the specified message
   * 
   * @param message the message
   * @return a {@link Status} (not <code>null</code>)
   */
  protected Status error(String message) {
    return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, message);
  }

  /**
   * Answer the file name without the file extension, by stripping off characters up to and
   * including the last dot '.' if one exists.
   * 
   * @param fileName the file name (not <code>null</code>)
   * @return the file name without the extension (not <code>null</code>)
   */
  protected String stripFileExtension(String fileName) {
    int index = fileName.lastIndexOf('.');
    while (index == fileName.length() - 1 && index >= 0) {
      fileName = fileName.substring(0, index);
      index = fileName.lastIndexOf('.');
    }
    if (index >= 0) {
      fileName = fileName.substring(index + 1);
    }
    return fileName;
  }
}
