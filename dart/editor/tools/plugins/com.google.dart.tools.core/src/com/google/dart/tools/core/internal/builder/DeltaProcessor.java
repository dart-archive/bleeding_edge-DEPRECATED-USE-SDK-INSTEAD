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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;
import static com.google.dart.tools.core.DartCore.isDartLikeFileName;

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

/**
 * {@code DeltaProcessor} traverses both {@link IResource} hierarchies and {@link IResourceDelta}s.
 * As Dart related resources are encountered, registered {@link DeltaListener}s are notified.
 */
public class DeltaProcessor {

  private class Event implements SourceDeltaEvent, SourceContainerDeltaEvent {

    private IResource resource;
    private Source source;
    private IResourceProxy proxy;
    private SourceContainer sourceContainer;

    @Override
    public AnalysisContext getContext() {
      return context;
    }

    @Override
    public Project getProject() {
      return project;
    }

    @Override
    public IResource getResource() {
      if (resource == null) {
        resource = proxy.requestResource();
      }
      return resource;
    }

    @Override
    public Source getSource() {
      if (source == null) {
        IPath location = getResource().getLocation();
        if (location != null) {
          source = context.getSourceFactory().forFile(location.toFile());
        } else {
          logNoLocation(getResource());
        }
      }
      return source;
    }

    @Override
    public SourceContainer getSourceContainer() {
      if (sourceContainer == null) {
        IPath location = getResource().getLocation();
        if (location != null) {
          sourceContainer = context.getSourceFactory().forDirectory(location.toFile());
        } else {
          logNoLocation(getResource());
        }
      }
      return sourceContainer;
    }

    @Override
    public boolean isTopContainerInContext() {
      return DeltaProcessor.this.isTopContainerInContext((IContainer) getResource());
    }

    void setProxy(IResourceProxy proxy) {
      this.proxy = proxy;
      this.resource = null;
      this.source = null;
    }

    void setResource(IResource resource) {
      this.proxy = null;
      this.resource = resource;
      this.source = null;
    }
  }

  private final Project project;
  private DeltaListener listener;
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
            event.setResource(resource);
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
          if (isDartLikeFileName(name)) {
            event.setResource(resource);
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
          if (setContextFor(resource.getParent())) {
            processPackagesDelta(delta);
          }
          return false;
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
            event.setResource(resource);
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
      case ADDED:
        for (IResource child : ((IContainer) packagesContainer).members()) {
          processPackagesResources(child);
        }
        return;
      case CHANGED:
        // Fall through to traverse child resource changes
        break;
      case REMOVED:
        event.setResource(packagesContainer);
        listener.packageSourceContainerRemoved(event);
        return;
      default:
        return;
    }

    for (IResourceDelta childDelta : delta.getAffectedChildren()) {
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
              event.setResource(resource);
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
              processPackagesResources(resource);
              return false;
            case CHANGED:
              // Recursively visit changed resources
              return true;
            case REMOVED:
              event.setResource(resource);
              listener.packageSourceContainerRemoved(event);
              return false;
            default:
              return false;
          }
        }
      });
    }
  }

  /**
   * Traverse added resources in the "packages" directory
   * 
   * @param resource the added resource (not {@code null})
   */
  protected void processPackagesResources(IResource resource) throws CoreException {
    resource.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        return visitPackagesProxy(proxy, proxy.getName());
      }
    }, 0);
  }

  /**
   * Traverse the specified resources
   * 
   * @param resource the resources to be recursively traversed
   */
  protected void processResources(IResource resource) throws CoreException {
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
   */
  protected boolean visitPackagesProxy(IResourceProxy proxy, String name) {
    // Ignore hidden resources and nested packages directories
    if (name.startsWith(".") || name.equals(PACKAGES_DIRECTORY_NAME)) {
      return false;
    }

    // Notify context of any Dart source files that have been added
    if (proxy.getType() == FILE) {
      if (isDartLikeFileName(name)) {
        event.setProxy(proxy);
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
        event.setProxy(proxy);
        listener.pubspecAdded(event);
        return false;
      }

      // Notify listener of new source files
      if (isDartLikeFileName(name)) {
        event.setProxy(proxy);
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
    AnalysisContext nextContext = project.getContext(container);
    if (nextContext == null) {
      return false;
    }
    context = nextContext;
    return true;
  }
}
