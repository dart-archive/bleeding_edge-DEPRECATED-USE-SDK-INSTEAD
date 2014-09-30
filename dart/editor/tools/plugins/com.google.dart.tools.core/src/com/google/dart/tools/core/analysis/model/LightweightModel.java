/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.core.analysis.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight version of the core model. This version can be queried quickly and is guaranteed to
 * have reasonably up-to-date information. As the latest data is available from the analysis engine,
 * this model is updated.
 */
public abstract class LightweightModel {
  static final QualifiedName CLIENT_LIBRARY = new QualifiedName(DartCore.PLUGIN_ID, "clientLibrary");

  static final QualifiedName SERVER_LIBRARY = new QualifiedName(DartCore.PLUGIN_ID, "serverLibrary");

  static final QualifiedName HTML_FILE = new QualifiedName(DartCore.PLUGIN_ID, "htmlFile");

  static final QualifiedName CONTAINING_LIBRARY = new QualifiedName(
      DartCore.PLUGIN_ID,
      "containingLibrary");

  private static LightweightModel model;

  /**
   * Returns the singleton instance of the {@link LightweightModel}
   */
  public static LightweightModel getModel() {
    if (model == null) {
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        model = new LightweightModel_NEW();
      } else {
        model = new LightweightModel_OLD();
      }
    }
    return model;
  }

  /**
   * Initialize the {@link LightweightModel}.
   */
  public static void init() {
    getModel();
  }

  /**
   * Return the containing library of this file. This can be null if there is not containing
   * library. If there are more then one containing libraries, this method returns a single
   * containing library.
   */
  public IFile getContainingLibrary(IFile file) {
    return getResource(file, CONTAINING_LIBRARY);
  }

  /**
   * Return the html file which has a reference to the library represented by the given source.
   */
  public IFile getHtmlFileForLibrary(IFile file) {
    return getResource(file, HTML_FILE);
  }

  /**
   * Return the list of html launch targets that exist in the given container.
   */
  public List<IFile> getHtmlLaunchTargets(IContainer container) {
    final List<IFile> files = new ArrayList<IFile>();

    try {
      container.accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (isPackagesFolder(resource)) {
            return false;
          }

          if (resource instanceof IFile) {
            IFile file = (IFile) resource;

            IFile htmlFile = getHtmlFileForLibrary(file);

            if (htmlFile != null) {
              files.add(htmlFile);
            }
          }

          return true;
        }
      });
    } catch (CoreException e) {
      DartCore.logError(e);
    }

    return files;
  }

  /**
   * Return the list of server launch targets that exist in the given container.
   */
  public List<IFile> getServerLaunchTargets(IContainer container) {
    final List<IFile> files = new ArrayList<IFile>();

    try {
      container.accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (isPackagesFolder(resource)) {
            return false;
          }

          if (resource instanceof IFile) {
            IFile file = (IFile) resource;

            if (isServerLibrary(file)) {
              files.add(file);
            }
          }

          return true;
        }
      });
    } catch (CoreException e) {
      DartCore.logError(e);
    }

    return files;
  }

  /**
   * Return whether the given file is the defining compilation unit of a library that can be run in
   * a browser.
   */
  public boolean isClientLibrary(IFile file) {
    String value = getPersistentProperty(file, CLIENT_LIBRARY);
    return "true".equals(value);
  }

  /**
   * Return whether the given file is the defining compilation unit of a library that can be run on
   * the server.
   */
  public boolean isServerLibrary(IFile file) {
    String value = getPersistentProperty(file, SERVER_LIBRARY);
    return "true".equals(value);
  }

  /**
   * Returns the value of the persistent property of the given {@link IFile}, identified by the
   * given key, or {@code null} if the {@link IFile} does not exists, has no such property or any
   * exception happens.
   */
  protected final String getPersistentProperty(IFile file, QualifiedName name) {
    if (file == null) {
      return null;
    }
    if (!file.exists()) {
      return null;
    }
    try {
      return file.getPersistentProperty(name);
    } catch (CoreException e) {
      return null;
    }
  }

  protected final IFile getResource(IFile file, QualifiedName qualifiedName) {
    String value = getPersistentProperty(file, qualifiedName);
    if (value != null) {
      IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(value);
      if (resource instanceof IFile) {
        return (IFile) resource;
      }
    }
    return null;
  }

  protected final boolean isPackagesFolder(IResource resource) {
    if (resource instanceof IContainer && resource.getName().equals("packages")) {
      return true;
    }
    return false;
  }

  /**
   * Update the file's property to the new value in the most efficient way possible.
   */
  protected final void setFileProperty(IFile file, QualifiedName property, boolean newValue)
      throws CoreException {
    boolean currentValue = "true".equals(getPersistentProperty(file, property));
    if (currentValue != newValue) {
      file.setPersistentProperty(property, Boolean.toString(newValue));
    }
  }

  /**
   * Update the file's property to the new value in the most efficient way possible.
   */
  protected final void setFileProperty(IFile file, QualifiedName property, IFile newValue)
      throws CoreException {
    String value = newValue != null ? newValue.getFullPath().toString() : null;
    setFileProperty(file, property, value);
  }

  /**
   * Update the file's property to the new value in the most efficient way possible.
   */
  protected final void setFileProperty(IFile file, QualifiedName property, String newValue)
      throws CoreException {
    String currentValue = getPersistentProperty(file, property);
    if (currentValue != newValue) {
      if (currentValue == null) {
        file.setPersistentProperty(property, newValue);
      } else if (!currentValue.equals(newValue)) {
        file.setPersistentProperty(property, newValue);
      }
    }
  }
}
