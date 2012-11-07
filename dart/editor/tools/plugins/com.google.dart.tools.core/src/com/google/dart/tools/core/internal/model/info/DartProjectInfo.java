/*
 * Copyright (c) 2012, the Dart project authors.
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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartProjectImpl;
import com.google.dart.tools.core.model.DartElement;

import org.eclipse.core.resources.IResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Instances of the class <code>DartProjectInfo</code> maintain the cached data shared by all equal
 * projects.
 */
public class DartProjectInfo extends OpenableElementInfo {
  /**
   * A list containing the project-relative paths to all children in the project.
   */
  private List<String> childPaths = null;

  /**
   * Table to store html file to library mapping
   */
  private HashMap<String, List<String>> htmlMapping;

  /**
   * The name of the directory in packages that is the self reference to the "lib" folder in the
   * project
   */
  private String linkedPackageDirName;

  /**
   * Return a list containing the project-relative paths to all children in the project.
   * 
   * @return a list containing the project-relative paths to all children in the project
   */
  public List<String> getChildPaths() {
    return childPaths;
  }

  /**
   * Return the mapping for the html files contained in this project.
   * 
   * @return the table with the html files to library mapping
   */

  public HashMap<String, List<String>> getHtmlMapping() {
    return htmlMapping;
  }

  public DartLibraryImpl[] getLibraries() {
    ArrayList<DartLibraryImpl> libraries = new ArrayList<DartLibraryImpl>();
    for (DartElement child : getChildren()) {
      if (child instanceof DartLibraryImpl) {
        libraries.add((DartLibraryImpl) child);
      }
    }
    return libraries.toArray(new DartLibraryImpl[libraries.size()]);
  }

  public String getLinkedPackageDirName() {
    return linkedPackageDirName;
  }

  public IResource[] getNonDartResources(DartProjectImpl project) {
    DartCore.notYetImplemented();
    return new IResource[0];
  }

  public void resetCaches() {
    DartCore.notYetImplemented();
  }

  /**
   * Set the project-relative paths to all children in the project to the given list.
   * 
   * @param paths a list containing the project-relative paths to all children in the project
   */
  public void setChildPaths(List<String> paths) {
    childPaths = paths;
  }

  /**
   * Set the html file to library mapping table
   * 
   * @param mapping a table with the html file to library mapping
   */
  public void setHtmlMapping(HashMap<String, List<String>> mapping) {
    htmlMapping = mapping;
  }

  /**
   * set the name for the self linked package directory
   */
  public void setLinkedPackageDirName(String packageDirectoryName) {
    this.linkedPackageDirName = packageDirectoryName;
  }

  /**
   * Update the mapping table on changes to the project
   * 
   * @param htmlFileName the name of the html file that changed
   * @param libraries list of library references
   */
  public void updateHtmlMapping(String htmlFileName, List<String> libraries, boolean add) {
    if (htmlMapping == null) {
      return;
    }
    if (add) {
      htmlMapping.put(htmlFileName, libraries);
    } else {
      htmlMapping.remove(htmlFileName);
    }
  }
}
