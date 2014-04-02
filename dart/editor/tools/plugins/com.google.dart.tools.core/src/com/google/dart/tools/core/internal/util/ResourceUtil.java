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
package com.google.dart.tools.core.internal.util;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

/**
 * Utilities for dealing with resources.
 * 
 * @coverage dart.tools.core
 */
public class ResourceUtil {
  /**
   * The root of the workspace, cached for efficiency.
   */
  public static IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

  /**
   * A cached table mapping the URI's of file resources to the resource associated with the URI.
   */
  private static HashMap<URI, IResource> resourceMap = null;

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

  public static URI getCanonicalUri(URI uri) {
    return CanonicalizationManager.getManager().getCanonicalUri(uri);
  }

  /**
   * Return the file resource associated with the given file, or <code>null</code> if the file does
   * not correspond to an existing file resource.
   * 
   * @param file the file representing the file resource to be returned
   * @return the file resource associated with the given file
   */
  public static IFile getFile(File file) {
    if (file == null) {
      return null;
    }
    return getFile(getCanonicalUri(file));
  }

  /**
   * Return the file associated with the given URI, or <code>null</code> if the URI does not
   * correspond to an existing file.
   * 
   * @param uri the URI representing the file to be returned
   * @return the file associated with the given URI
   */
  public static IFile getFile(URI uri) {
    IResource[] resources = ResourceUtil.getResources(uri);
    if (resources != null) {
      for (IResource resource : resources) {
        if (resource instanceof IFile && resource.exists()) {
          return (IFile) resource;
        }
      }
    }
    return null;
  }

  public static IPath getProjectLocation(IProject project) {
    if (project.getRawLocation() == null) {
      return project.getLocation();
    } else {
      return project.getRawLocation();
    }
  }

  /**
   * Answer the Eclipse resource associated with the specified file or <code>null</code> if none
   */
  public static IResource getResource(File file) {
    if (file == null) {
      return null;
    }
    return getResource(getCanonicalUri(file));
  }

  /**
   * Return the resource associated with the given URI, or <code>null</code> if the URI does not
   * correspond to an existing resource.
   * 
   * @param uri the URI representing the resource to be returned
   * @return the resource associated with the given URI
   */
  public static IResource getResource(URI uri) {
    IResource[] resources = ResourceUtil.getResources(uri);
    if (resources != null) {
      for (IResource resource : resources) {
        if (resource.exists()) {
          return resource;
        }
      }
    }
    return null;
  }

  /**
   * Answer the Eclipse resources associated with the specified file or <code>null</code> if none
   */
  public static IResource[] getResources(File file) {
    if (file == null) {
      return null;
    }
    return getResources(getCanonicalUri(file));
  }

  /**
   * Return the Eclipse resources associated with the given URI, or <code>null</code> if there is no
   * associated resource. The URI must be a canonical URI (a file: URI built from a canonical path).
   * 
   * @return the Eclipse resources associated with the given URI
   */
  public static IResource[] getResources(URI uri) {
    if (uri == null) {
      return null;
    } else if (!uri.isAbsolute()) {
      DartCore.logError(
          "Cannot get resource associated with non-absolute URI: " + uri,
          new Exception());
      return null;
    }
    IResource resource = getResourceMap().get(uri);
    if (resource == null) {
      resource = getResourceMap().get(getCanonicalUri(uri));
    }
    if (resource != null) {
      return new IResource[] {resource};
    }
    return root.findFilesForLocationURI(uri);
  }

  public static boolean isExistingProject(IProject project) {
    if (!project.isOpen()) {
      return false;
    }
    IPath location = getProjectLocation(project);
    if (location == null) {
      return false;
    }
    File file = location.toFile();
    if (file == null) {
      return false;
    }
    return file.exists();
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

  private static URI getCanonicalUri(File file) {
    return CanonicalizationManager.getManager().getCanonicalUri(file);
  }

  private static URI getCanonicalUri(IResource resource) {
    return getCanonicalUri(resource.getLocationURI());
  }

  /**
   * Return a table mapping the URI's of file resources to the resource associated with the URI.
   * 
   * @return a table mapping the URI's of file resources to the resource associated with the URI
   */
  private static HashMap<URI, IResource> getResourceMap() {
    synchronized (ResourceUtil.class) {
      //resourceMap = null;
      if (resourceMap == null) {
        resourceMap = new HashMap<URI, IResource>();
        try {
          root.accept(new IResourceProxyVisitor() {
            @Override
            public boolean visit(IResourceProxy proxy) {
              int type = proxy.getType();
              if (type == IResource.FILE || type == IResource.FOLDER || type == IResource.PROJECT) {
                IResource resource = proxy.requestResource();
                URI resourceUri = getCanonicalUri(resource);
                if (resourceUri != null) {
                  resourceMap.put(resourceUri, resource);
                }
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
