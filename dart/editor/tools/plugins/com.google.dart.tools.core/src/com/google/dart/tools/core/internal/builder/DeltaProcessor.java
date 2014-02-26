/*
 * Copyright 2013 Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.analysis.model.InvertedSourceContainer;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;
import static com.google.dart.tools.core.DartCore.isDartLikeFileName;
import static com.google.dart.tools.core.DartCore.isHtmlLikeFileName;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import static org.eclipse.core.resources.IResource.FILE;
import static org.eclipse.core.resources.IResource.PROJECT;
import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.CHANGED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

import java.io.File;
import java.io.IOException;

/**
 * {@code DeltaProcessor} traverses both {@link IResource} hierarchies and {@link IResourceDelta}s.
 * As Dart related resources are encountered, registered {@link DeltaListener}s are notified.
 * 
 * @coverage dart.tools.core.builder
 */
public class DeltaProcessor {

  private class Event implements SourceDeltaEvent, SourceContainerDeltaEvent {

    private IResourceProxy proxy;
    private IResource resource;
    private File canonicalPackageDir;
    private IPath fullPackagePath;
    private Source source;
    private SourceContainer sourceContainer;
    private AnalysisContext packagesRemovedFromContext;

    @Override
    public AnalysisContext getContext() {
      return context;
    }

    @Override
    public Project getProject() {
      return project;
    }

    @Override
    public PubFolder getPubFolder() {
      return pubFolder;
    }

    @Override
    public IResource getResource() {
      if (resource == null && proxy != null) {
        resource = proxy.requestResource();
      }
      return resource;
    }

    @Override
    public Source getSource() {
      if (source == null) {
        File file = getResourceFile();
        if (file != null) {
          source = new FileBasedSource(file);
        } else {
          logNoLocation(getResource());
        }
      }
      return source;
    }

    @Override
    public SourceContainer getSourceContainer() {
      if (sourceContainer == null) {
        File file = getResourceFile();
        if (file != null) {
          sourceContainer = new DirectoryBasedSourceContainer(file);
        } else {
          logNoLocation(getResource());
        }
      }
      return sourceContainer;
    }

    @Override
    public boolean isTopContainerInContext() {
      if (resource != null || proxy != null) {
        return DeltaProcessor.this.isTopContainerInContext((IContainer) getResource());
      } else {
        return false;
      }
    }

    /**
     * Answer the file associated with the given resource, ensuring that the file is canonical if it
     * resides in a package.
     * 
     * @return the file or {@code null} if the resource location cannot be determined
     */
    File getResourceFile() {
      if (fullPackagePath != null) {
        IPath fullPath = getResource().getFullPath();
        return new File(canonicalPackageDir, fullPath.makeRelativeTo(fullPackagePath).toString());
      }
      IPath location = getResource().getLocation();
      if (location != null) {
        return location.toFile();
      }
      return null;
    }

    /**
     * Configure the event to represent that a package or the entire "packages" directory was
     * removed. Because the original canonical location is lost (the symlink has been removed), we
     * represent this by an {@link InvertedSourceContainer "inverted" source container} that
     * contains everything *not* currently in the context's root folder or any of the existing
     * canonical package folders. Because this effectively represents all removed packages, we only
     * fire this event at most once per delta.
     * 
     * @return {@code true} if the
     *         {@link DeltaListener#packageSourceContainerRemoved(SourceContainerDeltaEvent) package
     *         removed event} should be fired, or {@code false} if it has already been fired earlier
     *         when processing the current delta.
     */
    boolean setPackagesRemoved() {
      this.proxy = null;
      this.resource = null;
      this.canonicalPackageDir = null;
      this.fullPackagePath = null;
      this.source = null;
      PubFolder pubFolder = getPubFolder();
      this.sourceContainer = pubFolder == null ? null : pubFolder.getInvertedSourceContainer();
      if (packagesRemovedFromContext == context) {
        return false;
      }
      this.packagesRemovedFromContext = context;
      return true;
    }

    /**
     * Configure the event to represent the specified resource
     * 
     * @param proxy the resource proxy (not {@code null})
     * @param canonicalPackageDir the canonical package directory or {@code null} if the resource is
     *          not in a package directory
     * @param fullPackagePath the full Eclipse path of the package directory (
     *          {@link IResource#getFullPath()} or {@link IResourceProxy#requestFullPath()}) or null
     *          if the resource is not in a package directory
     */
    void setProxy(IResourceProxy proxy, File canonicalPackageDir, IPath fullPackagePath) {
      this.proxy = proxy;
      this.resource = null;
      this.canonicalPackageDir = canonicalPackageDir;
      this.fullPackagePath = fullPackagePath;
      this.source = null;
      this.sourceContainer = null;
    }

    /**
     * Configure the event to represent the specified resource
     * 
     * @param resource the resource (not {@code null})
     * @param canonicalPackageDir the canonical package directory or {@code null} if the resource is
     *          not in a package directory
     * @param fullPackagePath the full Eclipse path of the package directory (
     *          {@link IResource#getFullPath()} or {@link IResourceProxy#requestFullPath()}) or null
     *          if the resource is not in a package directory
     */
    void setResource(IResource resource, File canonicalPackageDir, IPath fullPackagePath) {
      this.proxy = null;
      this.resource = resource;
      this.canonicalPackageDir = canonicalPackageDir;
      this.fullPackagePath = fullPackagePath;
      this.source = null;
      this.sourceContainer = null;
    }
  }

  private final Project project;
  private DeltaListener listener;
  private PubFolder pubFolder;
  private AnalysisContext context;
  private Event event;

  /**
   * Construct a new instance for traversing project resources
   * 
   * @param project the project containing the resources being traversed (not {@code null})
   */
  public DeltaProcessor(Project project) {
    this.project = project;
  }

  /**
   * Add a listener interested in receiving change information
   * 
   * @param listener the listener (not {@code null})
   */
  public void addDeltaListener(DeltaListener listener) {
    this.listener = DeltaListenerList.add(this.listener, listener);
  }

  /**
   * Traverse the specified resources in the associated {@link Project}. Listeners will be notified
   * of Dart related resources.
   * 
   * @param resource the container of resources to be recursively traversed (not {@code null})
   */
  public void traverse(IContainer resource) throws CoreException {
    event = new Event();
    processResources(resource);
    event = null;
  }

  /**
   * Traverse the specified resource changes in the associated {@link Project}. Listeners will be
   * notified of Dart related resource changes.
   * 
   * @param delta the delta describing the resource changes (not {@code null})
   */
  public void traverse(IResourceDelta delta) throws CoreException {
    event = new Event();
    processDelta(delta);
    event = null;
  }

  /**
   * Traverse the specified changes.
   * 
   * @param delta the delta describing the resource changes (not {@code null})
   */
  protected void processDelta(IResourceDelta delta) throws CoreException {
    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        String name = resource.getName();

        // Ignore hidden resources
        if (name.startsWith(".")) {
          return false;
        }

        if (resource.getType() == IResource.FILE) {

          // Notify listeners of changes to the pubspec.yaml file
          if (name.equals(PUBSPEC_FILE_NAME)) {
            event.setResource(resource, null, null);
            switch (delta.getKind()) {
              case ADDED:
                listener.pubspecAdded(event);
                break;
              case CHANGED:
                listener.pubspecChanged(event);
                break;
              case REMOVED:
                listener.pubspecRemoved(event);
                break;
              default:
                break;
            }
            return false;
          }

          // Notify context of Dart source that have been added, changed, or removed
          if (isDartLikeFileName(name) || isHtmlLikeFileName(name)) {
            event.setResource(resource, null, null);
            switch (delta.getKind()) {
              case ADDED:
                listener.sourceAdded(event);
                break;
              case CHANGED:
                listener.sourceChanged(event);
                break;
              case REMOVED:
                listener.sourceRemoved(event);
                break;
              default:
                break;
            }
            return false;
          }

          return false;
        }

        // Process "packages" folder changes separately
        if (name.equals(PACKAGES_DIRECTORY_NAME)) {
          boolean hasNewContext = setContextFor(resource.getParent());
          if (pubFolder != null) {
            if (hasNewContext) {
              processPackagesDelta(delta);
            }
            return false;
          }
        }

        // Process folder changes
        switch (delta.getKind()) {
          case ADDED:
            processResources(resource);
            return false;
          case CHANGED:
            // Cache the context and traverse child resource changes
            return setContextFor((IContainer) resource);
          case REMOVED:
            event.setResource(resource, null, null);
            listener.sourceContainerRemoved(event);
            return false;
          default:
            return false;
        }
      }
    });
  }

  /**
   * Traverse changes in the "packages" directory
   * 
   * @param delta the delta describing the resource changes (not {@code null})
   */
  protected void processPackagesDelta(IResourceDelta delta) throws CoreException {

    // Only process the top level "packages" folder in any given context
    IResource packagesContainer = delta.getResource();
    if (!isTopContainerInContext(packagesContainer.getParent())) {
      return;
    }

    switch (delta.getKind()) {

    // If the "packages" directory is added, then traverse each package individually
    // because the canonical locations of each package differ
      case ADDED:
        for (IResource child : ((IContainer) packagesContainer).members()) {
          try {
            processPackagesResources(
                child,
                child.getLocation().toFile().getCanonicalFile(),
                child.getFullPath());
          } catch (IOException e) {
            DartCore.logError(e);
          }
        }

        return;

        // Fall through to traverse child resource changes
      case CHANGED:
        break;

      // If all packages have been removed, then we issue a more general "packages removed" event
      // because the symlinks have been removed and thus the canonical locations lost
      case REMOVED:
        if (event.setPackagesRemoved()) {
          listener.packageSourceContainerRemoved(event);
        }
        return;
      default:
        return;
    }

    // Process each package delta in the "packages" directory
    for (IResourceDelta childDelta : delta.getAffectedChildren()) {

      // If the package has been removed, then we issue a more general "packages removed" event
      // because the symlink has been removed and thus the canonical location lost
      if (childDelta.getKind() == IResourceDelta.REMOVED) {
        IResource resource = childDelta.getResource();
        String name = resource.getName();

        // Ignore hidden resources and nested packages directories
        if (name.startsWith(".") || name.equals(PACKAGES_DIRECTORY_NAME)) {
          continue;
        }

        if (event.setPackagesRemoved()) {
          listener.packageSourceContainerRemoved(event);
        }
        continue;
      }

      // Determine the canonical location of the package
      IPath packageLocation = childDelta.getResource().getLocation();
      if (packageLocation == null) {
        logNoLocation(childDelta.getResource());
        continue;
      }
      File packageDir;
      try {
        packageDir = packageLocation.toFile().getCanonicalFile();
      } catch (IOException e) {
        DartCore.logError(e);
        continue;
      }
      final File canonicalPackageDir = packageDir;
      final IPath fullPackagePath = childDelta.getResource().getFullPath();

      // Traverse the package additions and changes
      childDelta.accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {
          IResource resource = delta.getResource();
          String name = resource.getName();

          // Ignore hidden resources and nested packages directories
          if (name.startsWith(".") || name.equals(PACKAGES_DIRECTORY_NAME)) {
            return false;
          }

          // Notify context of any Dart source files that have been added, changed, or removed
          if (resource.getType() == IResource.FILE) {
            if (isDartLikeFileName(name)) {
              event.setResource(resource, canonicalPackageDir, fullPackagePath);
              switch (delta.getKind()) {
                case ADDED:
                  listener.packageSourceAdded(event);
                  break;
                case CHANGED:
                  listener.packageSourceChanged(event);
                  break;
                case REMOVED:
                  listener.packageSourceRemoved(event);
                  break;
                default:
                  break;
              }
            }
            return false;
          }

          // Notify context of any package folders that were added or removed
          switch (delta.getKind()) {
            case ADDED:
              processPackagesResources(resource, canonicalPackageDir, fullPackagePath);
              return false;
            case CHANGED:
              // Recursively visit changed resources
              return true;
            case REMOVED:
              event.setResource(resource, canonicalPackageDir, fullPackagePath);
              listener.packageSourceContainerRemoved(event);
              return false;
            default:
              return false;
          }
        }
      });
    };
  }

  /**
   * Traverse added resources in the "packages" directory
   * 
   * @param resource the added resource (not {@code null})
   * @param canonicalPackageDir the canonical package directory (not {@code null})
   * @param fullPackagePath the full eclipse resource path of the package (
   *          {@link IResource#getFullPath()} not {@code null})
   */
  protected void processPackagesResources(IResource resource, final File canonicalPackageDir,
      final IPath fullPackagePath) throws CoreException {
    resource.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        return visitPackagesProxy(proxy, proxy.getName(), canonicalPackageDir, fullPackagePath);
      }
    }, 0);
  }

  /**
   * Traverse the specified resources
   * 
   * @param resource the resources to be recursively traversed
   */
  protected void processResources(IResource resource) throws CoreException {
    if (!resource.exists()) {
      return;
    }
    resource.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        return visitProxy(proxy, proxy.getName());
      }
    }, 0);
  }

  /**
   * Visit a resource proxy in the "packages" directory.
   * 
   * @param proxy the resource proxy (not {@code null})
   * @param name the resource name (not {@code null})
   * @param canonicalPackageDir the canonical package directory (not {@code null})
   * @param fullPackagePath the full eclipse resource path of the package (
   *          {@link IResource#getFullPath()} not {@code null})
   */
  protected boolean visitPackagesProxy(IResourceProxy proxy, String name, File canonicalPackageDir,
      IPath fullPackagePath) {
    // Ignore hidden resources and nested packages directories
    if (name.startsWith(".") || name.equals(PACKAGES_DIRECTORY_NAME)) {
      return false;
    }

    // Notify context of any Dart source files that have been added
    if (proxy.getType() == FILE) {
      if (isDartLikeFileName(name)) {
        event.setProxy(proxy, canonicalPackageDir, fullPackagePath);
        listener.packageSourceAdded(event);
      }
      return false;
    }

    // Recursively visit nested folders
    return true;
  }

  /**
   * Visit a resource proxy.
   * 
   * @param proxy the resource proxy (not {@code null})
   * @param name the resource name (not {@code null})
   */
  protected boolean visitProxy(IResourceProxy proxy, String name) {

    // Ignore hidden resources and nested packages directories
    if (name.startsWith(".") || name.equals(PACKAGES_DIRECTORY_NAME)) {
      return false;
    }

    if (proxy.getType() == FILE) {

      // Notify listener of new pubspec.yaml files
      if (name.equals(PUBSPEC_FILE_NAME)) {
        event.setProxy(proxy, null, null);
        listener.pubspecAdded(event);
        return false;
      }

      // Notify listener of new source files
      if (isDartLikeFileName(name) || isHtmlLikeFileName(name)) {
        event.setProxy(proxy, null, null);
        listener.sourceAdded(event);
        return false;
      }

      return false;
    }

    // Cache the context and traverse child resource changes
    return setContextFor((IContainer) proxy.requestResource());
  }

  /**
   * Return {@code true} if this container is a project or contains a pubspec.yaml
   */
  private boolean isTopContainerInContext(IContainer container) {
    if (container.getType() == PROJECT) {
      return true;
    }
    AnalysisContext context = project.getContext(container);
    AnalysisContext parentContext = project.getContext(container.getParent());
    return context != parentContext;
  }

  private void logNoLocation(IResource resource) {
    DartCore.logInformation("No location for " + resource);
  }

  /**
   * Cache the context for the specified container for use in subsequent operations. If the context
   * for the specified container cannot be determined, then the cached context is left unchanged.
   * 
   * @param container the container (not {@code null})
   * @return {@code true} if the context was set, or {@code false} if not
   */
  private boolean setContextFor(IContainer container) {
    PubFolder newPubFolder = project.getPubFolder(container);
    if (container.getType() == PROJECT || pubFolder != newPubFolder) {
      pubFolder = newPubFolder;
      if (pubFolder != null) {
        context = pubFolder.getContext();
        event.setResource(pubFolder.getResource(), null, null);
      } else {
        context = project.getDefaultContext();
        event.setResource(project.getResource(), null, null);
      }
      listener.visitContext(event);
    }
    return true;
  }
}
