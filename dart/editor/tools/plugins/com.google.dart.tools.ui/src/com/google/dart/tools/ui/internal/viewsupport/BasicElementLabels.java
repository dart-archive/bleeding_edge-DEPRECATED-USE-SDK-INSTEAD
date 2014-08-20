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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.DartElementLabels;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IWorkingSet;

import java.io.File;

/**
 * A label provider for basic elements like paths.
 */
public class BasicElementLabels {

  /**
   * Returns a label for Dart code snippet used in a label. Example is 'Test test = new Test<?
   * extends List>() { ...}'.
   * 
   * @param string the Dart code snippet
   * @return the label for the Dart code snippet
   */
  public static String getDartCodeString(String string) {
    // TODO(scheglov) inline this no-op method later
    return string;
  }

  /**
   * Returns a label for Dart element name. Example is 'new Test<? extends List>() { ...}'. This
   * method should only be used for simple element names. Use {@link DartElementLabels} to create a
   * label from a Dart element or {@link BindingLabelProvider} for labels of bindings.
   * 
   * @param name the Dart element name.
   * @return the label for the Dart element
   */
  public static String getDartElementName(String name) {
    // TODO(scheglov) inline this no-op method later
    return name;
  }

  /**
   * Returns the label for a file pattern like '*.dart'
   * 
   * @param name the pattern
   * @return the label of the pattern.
   */
  public static String getFilePattern(String name) {
    // TODO(scheglov) inline this no-op method later
    return name;
  }

  /**
   * Returns the label of the path of a file.
   * 
   * @param file the file
   * @return the label of the file path to be used in the UI.
   */
  public static String getPathLabel(File file) {
    // TODO(scheglov) inline this method later
    return file.getAbsolutePath();
  }

  /**
   * Returns the label of a path.
   * 
   * @param path the path
   * @param isOSPath if <code>true</code>, the path represents an OS path, if <code>false</code> it
   *          is a workspace path.
   * @return the label of the path to be used in the UI.
   */
  public static String getPathLabel(IPath path, boolean isOSPath) {
    if (isOSPath) {
      return path.toOSString();
    } else {
      return path.makeRelative().toString();
    }
  }

  /**
   * Returns a label for a resource name.
   * 
   * @param resource the resource
   * @return the label of the resource name.
   */
  public static String getResourceName(IResource resource) {
    // TODO(scheglov) inline this method later
    return resource.getName();
  }

  /**
   * Returns a label for a resource name.
   * 
   * @param resourceName the resource name
   * @return the label of the resource name.
   */
  public static String getResourceName(String resourceName) {
    // TODO(scheglov) inline this method later
    return resourceName;
  }

  /**
   * Returns the label for a URL, URI or URL part. Example is 'http://www.x.xom/s.html#1'
   * 
   * @param name the URL string
   * @return the label of the URL.
   */
  public static String getURLPart(String name) {
    // TODO(scheglov) inline this method later
    return name;
  }

  /**
   * Returns a label for a version name. Example is '1.4.1'
   * 
   * @param name the version string
   * @return the version label
   */
  public static String getVersionName(String name) {
    // TODO(scheglov) inline this method later
    return name;
  }

  /**
   * Returns a label for a working set
   * 
   * @param set the working set
   * @return the label of the working set
   */
  public static String getWorkingSetLabel(IWorkingSet set) {
    // TODO(scheglov) inline this method later
    return set.getLabel();
  }

}
