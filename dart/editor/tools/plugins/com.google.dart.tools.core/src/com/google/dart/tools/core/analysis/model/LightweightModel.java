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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;

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
public class LightweightModel {
  private static final QualifiedName SOURCE_KIND = new QualifiedName(
      DartCore.PLUGIN_ID,
      "sourceKind");

  private static final QualifiedName CLIENT_LIBRARY = new QualifiedName(
      DartCore.PLUGIN_ID,
      "clientLibrary");

  private static final QualifiedName SERVER_LIBRARY = new QualifiedName(
      DartCore.PLUGIN_ID,
      "serverLibrary");

  private static final QualifiedName HTML_FILE = new QualifiedName(DartCore.PLUGIN_ID, "htmlFile");

  private static final QualifiedName CONTAINING_LIBRARY = new QualifiedName(
      DartCore.PLUGIN_ID,
      "containingLibrary");

  private static LightweightModel model;

  /**
   * @return the singleton instance of the LightweightModel
   */
  public static LightweightModel getModel() {
    if (model == null) {
      model = new LightweightModel();
    }

    return model;
  }

  /**
   * Initialize the LightweightModel. This will create the singleton instance and start listening
   * for analysis changes.
   */
  public static void init() {
    getModel();
  }

  protected LightweightModel() {
    AnalysisWorker.addListener(new AnalysisListener() {
      @Override
      public void complete(AnalysisEvent event) {
      }

      @Override
      public void resolved(ResolvedEvent event) {
        IResource resource = event.getResource();
        if (resource != null) {
          AnalysisContext context = event.getContext();
          ResourceMap resourceMap = event.getResourceMap();
          Source source = event.getSource();
          // in tests some information may be missing
          if (context == null || resourceMap == null || source == null) {
            return;
          }
          // OK, update the Source
          try {
            recalculateForResource(context, resourceMap, source, resource);
          } catch (CoreException e) {
            DartCore.logInformation("Exception updating: " + source, e);
          }
        }
      }

      @Override
      public void resolvedHtml(ResolvedHtmlEvent event) {
        IResource htmlResource = event.getResource();
        if (htmlResource instanceof IFile) {
          IFile htmlFile = (IFile) htmlResource;
          AnalysisContext context = event.getContext();
          ResourceMap resourceMap = event.getResourceMap();
          Source htmlSource = event.getSource();
          // in tests some information may be missing
          if (context == null || resourceMap == null || htmlSource == null) {
            return;
          }
          // OK, process the change
          try {
            Source[] librarySources = context.getLibrariesReferencedFromHtml(htmlSource);
            for (Source librarySource : librarySources) {
              IFile libraryFile = resourceMap.getResource(librarySource);
              setFileProperty(libraryFile, HTML_FILE, htmlFile);
            }
          } catch (CoreException e) {
            DartCore.logInformation("Exception updating: " + htmlSource);
          }
        }
      }
    });
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
   * Return the source kind for the given file.
   */
  public SourceKind getSourceKind(IFile file) {
    String value = getPersistentProperty(file, SOURCE_KIND);
    if (value != null) {
      try {
        return SourceKind.valueOf(value);
      } catch (IllegalArgumentException ex) {

      }
    }
    return SourceKind.UNKNOWN;
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
   * Update the file's property to the new value in the most efficient way possible.
   */
  protected void setFileProperty(IFile file, QualifiedName property, boolean newValue)
      throws CoreException {
    boolean currentValue = "true".equals(getPersistentProperty(file, property));

    if (currentValue != newValue) {
      file.setPersistentProperty(property, Boolean.toString(newValue));
    }
  }

  /**
   * Update the file's property to the new value in the most efficient way possible.
   */
  protected void setFileProperty(IFile file, QualifiedName property, IFile newValue)
      throws CoreException {
    String value = newValue != null ? newValue.getFullPath().toString() : null;
    setFileProperty(file, property, value);
  }

  /**
   * Update the file's property to the new value in the most efficient way possible.
   */
  protected void setFileProperty(IFile file, QualifiedName property, String newValue)
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

  private String getLibraryName(Source source, AnalysisContext context) {
    if (source == null) {
      return null;
    }

    LibraryElement element = context.getLibraryElement(source);

    if (element == null) {
      return null;
    }

    String name = element.getDisplayName();

    if (name != null && name.isEmpty()) {
      return null;
    } else {
      return name;
    }
  }

  /**
   * Returns the value of the persistent property of the given {@link IFile}, identified by the
   * given key, or {@code null} if the {@link IFile} does not exists, has no such property or any
   * exception happens.
   */
  private String getPersistentProperty(IFile file, QualifiedName name) {
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

  private IFile getResource(IFile file, QualifiedName qualifiedName) {
    String value = getPersistentProperty(file, qualifiedName);
    if (value != null) {
      IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(value);
      if (resource instanceof IFile) {
        return (IFile) resource;
      }
    }
    return null;
  }

  private boolean isPackagesFolder(IResource resource) {
    if (resource instanceof IContainer && resource.getName().equals("packages")) {
      return true;
    }

    return false;
  }

  private void recalculateForResource(AnalysisContext context, ResourceMap resourceMap,
      Source source, IResource resource) throws CoreException {

    // Check existence before setting persistent properties
    if (!resource.exists()) {
      DartCore.logInformation(getClass().getSimpleName()
          + "#recalculateForResource cannot update persistent properties on non-existant resource: "
          + resource);
      return;
    }
    IFile file = (IFile) resource;

    // Set the library name.
    String libraryName = getLibraryName(source, context);
    setFileProperty(file, DartCore.LIBRARY_NAME, libraryName);

    // Set the source kind.
    SourceKind kind = (source == null ? SourceKind.UNKNOWN : context.getKindOf(source));
    setFileProperty(file, SOURCE_KIND, kind.name());

    // Set the client library property.
    boolean clientLaunchable = source == null ? false : context.isClientLibrary(source);
    setFileProperty(file, CLIENT_LIBRARY, clientLaunchable);

    // Set the server library property.
    boolean serverLaunchable = source == null ? false : context.isServerLibrary(source);
    setFileProperty(file, SERVER_LIBRARY, serverLaunchable);

    // Set the HTML file property.
    Source[] htmlSources = source == null ? null : context.getHtmlFilesReferencing(source);
    if (htmlSources == null || htmlSources.length == 0) {
      setFileProperty(file, HTML_FILE, (String) null);
    } else {
      setFileProperty(file, HTML_FILE, resourceMap.getResource(htmlSources[0]));
    }

    // Set the containing library property.
    Source[] containingSources = source == null ? null : context.getLibrariesContaining(source);
    if (containingSources == null || containingSources.length == 0) {
      setFileProperty(file, CONTAINING_LIBRARY, (String) null);
    } else {
      setFileProperty(file, CONTAINING_LIBRARY, resourceMap.getResource(containingSources[0]));
    }
  }
}
