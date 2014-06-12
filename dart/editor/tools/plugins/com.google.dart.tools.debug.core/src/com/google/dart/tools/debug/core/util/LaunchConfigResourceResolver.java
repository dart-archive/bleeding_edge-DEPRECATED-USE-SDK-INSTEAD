/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.debug.core.util;

import com.google.dart.tools.core.utilities.net.URIUtilities;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import java.io.File;
import java.net.URI;

/**
 * A resource resolver for use with url based Dartium launches.
 */
public class LaunchConfigResourceResolver implements IResourceResolver {
  private DartLaunchConfigWrapper wrapper;

  public LaunchConfigResourceResolver(DartLaunchConfigWrapper wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public String getUrlForFile(File file) {
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());

    if (files.length > 0) {
      return getUrlForResource(files[0]);
    } else {
      return null;
    }
  }

  @Override
  public String getUrlForResource(IResource resource) {
    String relPath = calcRelPath(getSourceContainer(), resource);

    if (relPath == null) {
      return null;
    }

    String url = getUrl();

    int index = url.lastIndexOf('/');

    if (index != -1) {
      url = url.substring(0, index + 1);
    }

    url += URIUtilities.uriEncode(relPath);

    return url;
  }

  @Override
  public String getUrlRegexForResource(IResource resource) {
    IContainer container = getSourceContainer();

    String relPath = calcRelPath(container, resource);

    if (relPath != null) {
      return URIUtilities.uriEncode(relPath);
    }

    // Check for package or self-reference links.
    if (container.getProject().equals(resource.getProject())) {
      String path = resource.getFullPath().toString();

      if (path.contains("/packages/") || path.contains("/lib/")) {
        if (path.startsWith("/")) {
          path = path.substring(1);
        }

        return URIUtilities.uriEncode(path);
      }
    }
    // resource is not in project
    return resource.getFullPath().toString();
  }

  @Override
  public IResource resolveUrl(String url) {
    try {
      URI uri = new URI(url);
      String filePath = uri.getPath();
      IContainer sourceDirectory = getSourceContainer();

      if (sourceDirectory == null) {
        return null;
      }

      IResource resource = sourceDirectory.findMember(filePath);

      if (resource.exists()) {
        return resource;
      }

      return null;
    } catch (Throwable t) {
      return null;
    }
  }

  private String calcRelPath(IContainer container, IResource resource) {
    if (container == null) {
      return null;
    }

    String containerPath = container.getFullPath().toString();
    String resourcePath = resource.getFullPath().toString();

    if (resourcePath.startsWith(containerPath)) {
      String relPath = resourcePath.substring(containerPath.length());
      if (relPath.startsWith("-")) {
        relPath = relPath.substring(relPath.indexOf("/"));
      }
      if (relPath.startsWith("/")) {
        return relPath.substring(1);
      } else {
        return relPath;
      }
    } else {
      return null;
    }
  }

  private IContainer getSourceContainer() {
    // TODO(devoncarew): remove this if/else logic once most launch configurations have moved over
    // to using source directories.

    IContainer container = wrapper.getSourceDirectory();

    if (container != null) {
      return container;
    } else {
      return wrapper.getProject();
    }
  }

  private String getUrl() {
    return wrapper.getUrl();
  }
}
