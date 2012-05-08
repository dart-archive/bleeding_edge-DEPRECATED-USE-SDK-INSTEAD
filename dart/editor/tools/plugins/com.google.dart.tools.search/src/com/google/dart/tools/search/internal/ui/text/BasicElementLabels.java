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
package com.google.dart.tools.search.internal.ui.text;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.TextProcessor;

import java.io.File;

/**
 * A label provider for basic elements like paths. The label provider will make sure that the labels
 * are correctly shown in RTL environments.
 */
public class BasicElementLabels {

  /**
   * Returns the label for a file pattern like '*.java'
   * 
   * @param name the pattern
   * @return the label of the pattern.
   */
  public static String getFilePattern(String name) {
    return markLTR(name, "*.?/\\:."); //$NON-NLS-1$
  }

  /**
   * Returns the label of this resource's parent's path.
   * 
   * @param resource the resource
   * @param isOSPath if <code>true</code>, the path represents an OS path, if <code>false</code> it
   *          is a workspace path.
   * @return the label of the path to be used in the UI.
   */
  public static String getParentPathLabel(File resource, boolean isOSPath) {
    Path parentPath = new Path(resource.getParentFile().getAbsolutePath());
    return getPathLabel(parentPath, isOSPath);
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
    String label;
    if (isOSPath) {
      label = path.toOSString();
    } else {
      label = path.makeRelative().toString();
    }
    return markLTR(label, "/\\:."); //$NON-NLS-1$
  }

  /**
   * Returns a label for a resource name.
   * 
   * @param resource the resource
   * @return the label of the resource name.
   */
  public static String getResourceName(File resource) {
    return markLTR(resource.getName(), ":."); //$NON-NLS-1$
  }

  /**
   * Returns a label for a resource name.
   * 
   * @param resource the resource
   * @return the label of the resource name.
   */
  public static String getResourceName(IResource resource) {
    return markLTR(resource.getName(), ":."); //$NON-NLS-1$
  }

  /**
   * Returns a label for a resource name.
   * 
   * @param resourceName the resource name
   * @return the label of the resource name.
   */
  public static String getResourceName(String resourceName) {
    return markLTR(resourceName, ":."); //$NON-NLS-1$
  }

  /**
   * Returns the label for a URL, URI or URL part. Example is 'http://www.x.xom/s.html#1'
   * 
   * @param name the URL string
   * @return the label of the URL.
   */
  public static String getURLPart(String name) {
    return markLTR(name, ":@?-#/\\:."); //$NON-NLS-1$
  }

  /**
   * Returns a label for a version name. Example is '1.4.1'
   * 
   * @param name the version string
   * @return the version label
   */
  public static String getVersionName(String name) {
    return markLTR(name, ":."); //$NON-NLS-1$
  }

  /**
   * Adds special marks so that that the given string is readable in a BIDI environment.
   * 
   * @param string the string
   * @param delimiters the additional delimiters
   * @return the processed styled string
   */
  private static String markLTR(String string, String delimiters) {
    return TextProcessor.process(string, delimiters);
  }

  private BasicElementLabels() {
  }
}
