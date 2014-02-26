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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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

  private static boolean isWorkspaceClosed() {
    try {
      ResourcesPlugin.getWorkspace();
      return false;
    } catch (IllegalStateException ex) {
      return true;
    }
  }

  protected ProjectManager projectManager;

  protected LightweightModel() {
    projectManager = DartCore.getProjectManager();

    AnalysisWorker.addListener(new AnalysisListener() {
      @Override
      public void complete(AnalysisEvent event) {
        recalculateFor(event);
      }

      @Override
      public void resolved(ResolvedEvent event) {

      }

      @Override
      public void resolvedHtml(ResolvedHtmlEvent event) {

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
   * Return the {@link PubFolder} containing the specified resource.
   * <p>
   * This is a pass-through call to the ProjectManager implementation.
   */
  public PubFolder getPubFolder(IResource resource) {
    return projectManager.getPubFolder(resource);
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
    try {
      String value = file.getPersistentProperty(SOURCE_KIND);

      if (value != null) {
        try {
          return SourceKind.valueOf(value);
        } catch (IllegalArgumentException ex) {

        }
      }
    } catch (CoreException e) {
      DartCore.logError(e);
    }

    return SourceKind.UNKNOWN;
  }

  /**
   * Return whether the given file is the defining compilation unit of a library that can be run in
   * a browser.
   */
  public boolean isClientLibrary(IFile file) {
    try {
      return "true".equals(file.getPersistentProperty(CLIENT_LIBRARY));
    } catch (CoreException e) {
      return false;
    }
  }

  /**
   * Return whether the given file is the defining compilation unit of a library that can be run on
   * the server.
   */
  public boolean isServerLibrary(IFile file) {
    try {
      return "true".equals(file.getPersistentProperty(SERVER_LIBRARY));
    } catch (CoreException e) {
      return false;
    }
  }

  protected void recalculateFor(AnalysisEvent event) {
    final ResourceMap resourceMap = event.getContextManager().getResourceMap(event.getContext());
    final AnalysisContext context = event.getContext();

    // The SDK context does not have a resource map associated with it.
    if (resourceMap == null) {
      return;
    }

    DartCore.logInformation("Analysis complete for " + resourceMap.getResource().getName());

    final IResourceVisitor visitor = new IResourceVisitor() {
      @Override
      public boolean visit(IResource resource) throws CoreException {
        if (isPackagesFolder(resource)) {
          return false;
        }

        // We're currently only providing information about Dart files to clients.
        if (resource instanceof IFile && DartCore.isDartLikeFileName(resource.getName())
            && DartCore.isAnalyzed(resource)) {
          IFile file = (IFile) resource;
          Source source = resourceMap.getSource(file);

          // Set the library name.
          String libraryName = getLibraryName(source, resourceMap);
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

          // Set the html file property.
          Source[] htmlSources = source == null ? null : context.getHtmlFilesReferencing(source);
          if (htmlSources == null || htmlSources.length == 0) {
            setFileProperty(file, HTML_FILE, (String) null);
          } else {
            setFileProperty(file, HTML_FILE, resourceMap.getResource(htmlSources[0]));
          }

          // Set the containing library property.
          Source[] containingSources = source == null ? null
              : context.getLibrariesContaining(source);
          if (containingSources == null || containingSources.length == 0) {
            setFileProperty(file, CONTAINING_LIBRARY, (String) null);
          } else {
            setFileProperty(file, CONTAINING_LIBRARY, resourceMap.getResource(containingSources[0]));
          }
        }

        return true;
      }
    };

    // Do this in a resource operation, to batch up change events.
    IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        IContainer container = resourceMap.getResource();
        try {
          container.accept(visitor);
        } catch (CoreException ce) {
          // This can throw if we're traversing an IContainer that is not up-to-date wrt the file
          // system. I.e., if a file has been deleted on disk but the resources system does not yet
          // know about it. If this happens, we refresh the container and re-visit it.
          container.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
          if (container.exists()) {
            container.accept(visitor);
          }
        }
      }
    };

    try {
      ResourcesPlugin.getWorkspace().run(
          workspaceRunnable,
          resourceMap.getResource(),
          IWorkspace.AVOID_UPDATE,
          new NullProgressMonitor());
    } catch (Throwable e) {
      if (isWorkspaceClosed()) {
        return;
      }
      DartCore.logError(e);
    }
  }

  /**
   * Update the file's property to the new value in the most efficient way possible.
   */
  protected void setFileProperty(IFile file, QualifiedName property, boolean newValue)
      throws CoreException {
    boolean currentValue = "true".equals(file.getPersistentProperty(property));

    if (currentValue != newValue) {
      file.setPersistentProperty(property, Boolean.toString(newValue));
    }
  }

  /**
   * Update the file's property to the new value in the most efficient way possible.
   */
  protected void setFileProperty(IFile file, QualifiedName property, IFile newValue)
      throws CoreException {
    setFileProperty(file, property, newValue.getFullPath().toString());
  }

  /**
   * Update the file's property to the new value in the most efficient way possible.
   */
  protected void setFileProperty(IFile file, QualifiedName property, String newValue)
      throws CoreException {
    String currentValue = file.getPersistentProperty(property);

    if (currentValue != newValue) {
      if (currentValue == null) {
        file.setPersistentProperty(property, newValue);
      } else if (!currentValue.equals(newValue)) {
        file.setPersistentProperty(property, newValue);
      }
    }
  }

  private String getLibraryName(Source source, ResourceMap resourceMap) {
    if (source == null) {
      return null;
    }

    LibraryElement element = resourceMap.getContext().getLibraryElement(source);

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

  private IFile getResource(IFile file, QualifiedName qualifiedName) {
    try {
      String value = file.getPersistentProperty(qualifiedName);

      if (value != null) {
        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(value);

        if (resource instanceof IFile) {
          return (IFile) resource;
        }
      }
    } catch (CoreException e) {
      DartCore.logError(e);
    }

    return null;
  }

  private boolean isPackagesFolder(IResource resource) {
    if (resource instanceof IContainer && resource.getName().equals("packages")) {
      return true;
    }

    return false;
  }
}
