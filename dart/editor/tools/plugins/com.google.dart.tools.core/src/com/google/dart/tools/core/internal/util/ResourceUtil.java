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
package com.google.dart.tools.core.internal.util;

import com.google.dart.compiler.Source;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.tools.core.utilities.net.URIUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import java.io.File;
import java.net.URI;

/**
 * Utilities for mapping {@link Source} to {@link File} to {@link IFile}
 */
public class ResourceUtil {
  public static IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

  /**
   * Answer the file for the specified Dart source or <code>null</code> if none
   */
  public static File getFile(Source source) {
    if (source == null) {
      return null;
    }
    try {
      URI uri = URIUtilities.safelyResolveDartUri(source.getUri());
      if (uri.isAbsolute() && "file".equals(uri.getScheme())) {
        return new File(uri);
      }
      String relativePath = uri.getPath();
      return new File(new File(".").getAbsoluteFile(), relativePath);
    } catch (IllegalArgumentException ex) {
      // Thrown if the url was not a legal file url.
      return null;
    }
  }

  /**
   * Answer the Eclipse resource associated with the specified file or <code>null</code> if none
   */
  public static IFile getResource(File file) {
    IFile[] resources = ResourceUtil.getResources(file);
    if (resources != null) {
      for (IFile iFile : resources) {
        if (iFile.exists()) {
          return iFile;
        }
      }
    }
    return null;
  }

  /**
   * Answer the Eclipse resource associated with the specified source or <code>null</code> if none
   */
  public static IFile getResource(Source source) {
    return getResource(getFile(source));
  }

  /**
   * Return the resource associated with the given URI, or <code>null</code> if the URI does not
   * correspond to an existing resource.
   * 
   * @param uri the URI representing the resource to be returned
   * @return the resource associated with the given URI
   */
  public static IFile getResource(URI uri) {
    IFile[] resourceFiles = ResourceUtil.getResources(uri);
    if (resourceFiles != null) {
      for (IFile iFile : resourceFiles) {
        if (iFile.exists()) {
          return iFile;
        }
      }
    }
    return null;
  }

  /**
   * Answer the Eclipse resources associated with the specified file or <code>null</code> if none
   */
  public static IFile[] getResources(File file) {
    if (file == null) {
      return null;
    }
    URI fileURI = file.getAbsoluteFile().toURI();
    return root.findFilesForLocationURI(fileURI);
  }

  /**
   * Answer the Eclipse resources associated with the Dart source or <code>null</code> if none
   */
  public static IFile[] getResources(Source source) {
    return getResources(getFile(source));
  }

  /**
   * Answer the Eclipse resources associated with the specified URI or <code>null</code> if none
   */
  public static IFile[] getResources(URI uri) {
    if (SystemLibraryManager.isDartUri(uri)) {
      return null;
    }
    return root.findFilesForLocationURI(uri);
  }

  // No instances
  private ResourceUtil() {
  }
}
