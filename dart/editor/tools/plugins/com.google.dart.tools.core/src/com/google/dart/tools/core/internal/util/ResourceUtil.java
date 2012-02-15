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
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.net.URIUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

/**
 * Utilities for mapping {@link Source} to {@link File} to {@link IFile}
 */
public class ResourceUtil {
  /**
   * The root of the workspace, cached for efficiency.
   */
  public static IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

  /**
   * A cached table mapping the URI's of file resources to the resource associated with the URI.
   */
  private static HashMap<URI, IFile> resourceMap = null;

  /**
   * The listener used to maintain the resource map when the list of resources has changed.
   */
  private static final IResourceChangeListener listener = new IResourceChangeListener() {
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      //
      // This is overkill, but it has the advantage that it is not error prone. We can consider
      // implementing a more efficient version that would update the state of the map if the
      // performance still isn't good enough.
      //
      synchronized (ResourceUtil.class) {
        resourceMap = null;
      }
    }
  };

  /**
   * Return the file corresponding to the specified Dart source, or <code>null</code> if there is no
   * such file.
   * 
   * @param source the source corresponding to the file to be returned
   * @return the file corresponding to the specified Dart source
   */
  public static File getFile(Source source) {
    if (source == null) {
      return null;
    }

    URI uri = URIUtilities.safelyResolveDartUri(source.getUri());

    try {
      if (uri.isAbsolute() && "file".equals(uri.getScheme())) {
        return new File(uri);
      }
      String relativePath = uri.getPath();
      if (relativePath == null) {
        DartCore.logError("Illegal file URI: " + uri, new Exception());
        return null;
      }
      return new File(new File(".").getAbsoluteFile(), relativePath);
    } catch (IllegalArgumentException ex) {
      DartCore.logError("Illegal file URI: " + uri, ex);
      return null;
    }
  }

  /**
   * Answer the Eclipse resource associated with the specified file or <code>null</code> if none
   */
  public static IFile getResource(File file) {
    return getResource(file.getAbsoluteFile().toURI());
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
    return getResources(file.getAbsoluteFile().toURI());
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
    } else if (!uri.isAbsolute()) {
      DartCore.logError("Cannot get resource associated with non-absolute URI: " + uri,
          new Exception());
      return null;
    }
    IFile file = getResourceMap().get(uri);
    if (file != null) {
      return new IFile[] {file};
    }
    return root.findFilesForLocationURI(uri);
  }

  /**
   * Perform any clean up required when the core plug-in is shutting down.
   */
  public static void shutdown() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
  }

  /**
   * Perform any initialization required when the core plug-in is starting up.
   */
  public static void startup() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
  }

  /**
   * Return a table mapping the URI's of file resources to the resource associated with the URI.
   * 
   * @return a table mapping the URI's of file resources to the resource associated with the URI
   */
  private static HashMap<URI, IFile> getResourceMap() {
    synchronized (ResourceUtil.class) {
      if (resourceMap == null) {
        resourceMap = new HashMap<URI, IFile>();
        try {
          root.accept(new IResourceProxyVisitor() {
            @Override
            public boolean visit(IResourceProxy proxy) {
              if (proxy.getType() == IResource.FILE) {
                IFile file = (IFile) proxy.requestResource();
                resourceMap.put(file.getLocationURI(), file);
              }
              return true;
            }
          }, 0);
        } catch (CoreException exception) {
          DartCore.logError("Could not visit resources", exception);
        }
      }
      return resourceMap;
    }
  }

  // No instances
  private ResourceUtil() {
  }
}
