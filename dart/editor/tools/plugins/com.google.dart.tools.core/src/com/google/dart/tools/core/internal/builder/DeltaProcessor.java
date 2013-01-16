package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.Project;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;
import static com.google.dart.tools.core.DartCore.isDartLikeFileName;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;

import static org.eclipse.core.resources.IResource.FILE;
import static org.eclipse.core.resources.IResource.PROJECT;
import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.CHANGED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

/**
 * Updates a {@link Project} based upon traversing {@link IResource} and {@link IResourceDelta}.
 */
public class DeltaProcessor {

  final Project project;
  final ProjectUpdater updater;
  AnalysisContext context;

  /**
   * Construct a new instance for updating the specified project.
   * 
   * @param project the project being updated (not {@code null})
   * @param updater the object used to update the specified project (not {@code null})
   */
  public DeltaProcessor(Project project, ProjectUpdater updater) {
    this.project = project;
    this.updater = updater;
  }

  /**
   * Traverse the specified resources and update the associated {@link Project}. Contexts will be
   * notified of new sources via {@link AnalysisContext#sourceAvailable(Source)}, and optionally
   * notified of changed sources via {@link AnalysisContext#sourceChanged(Source)}.
   * 
   * @param resource the container of resources to be recursively traversed
   */
  public void traverse(IContainer resource) throws CoreException {
    processResources(resource);
  }

  /**
   * Traverse the specified changes and update the associated {@link Project}. Contexts will be
   * notified of changed sources via {@link AnalysisContext#sourceChanged(Source)}, and optionally
   * notified of new sources via {@link AnalysisContext#sourceAvailable(Source)}.
   * 
   * @param delta the delta describing the resource changes
   */
  public void traverse(IResourceDelta delta) throws CoreException {
    processDelta(delta);
  }

  /**
   * Traverse the specified changes and update the associated {@link Project}.
   * 
   * @param delta the delta describing the resource changes
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

          if (name.equals(PUBSPEC_FILE_NAME)) {
            switch (delta.getKind()) {
              case ADDED:
                updater.pubspecAdded((IFile) resource);
                break;
              case CHANGED:
                updater.pubspecChanged((IFile) resource);
                break;
              case REMOVED:
                updater.pubspecRemoved((IFile) resource);
                break;
              default:
                break;
            }
          }

          // Notify context of Dart source that have been added, changed, or removed
          if (isDartLikeFileName(name)) {
            switch (delta.getKind()) {
              case ADDED:
                updater.sourceAdded((IFile) resource);
                break;
              case CHANGED:
                updater.sourceChanged((IFile) resource);
                break;
              case REMOVED:
                updater.sourceRemoved((IFile) resource);
                break;
              default:
                break;
            }
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

        switch (delta.getKind()) {
          case ADDED:
            processResources(resource);
            return false;
          case REMOVED:
            updater.containerRemoved((IContainer) resource);
            return false;
          default:
            break;
        }

        // Cache the context and traverse child resource changes
        return setContextFor((IContainer) resource);
      }
    });
  }

  /**
   * Process changes in the "packages" directory
   * 
   * @param delta the delta describing the resource changes (not {@code null})
   */
  protected void processPackagesDelta(IResourceDelta delta) throws CoreException {

    // Only process the top level "packages" folder in any given context
    IResource packagesContainer = delta.getResource();
    IContainer parent = packagesContainer.getParent();
    if (parent.getType() != PROJECT) {
      AnalysisContext parentContext = project.getContext(parent.getParent());
      if (context == parentContext) {
        return;
      }
    }

    switch (delta.getKind()) {
      case ADDED:
        for (IResource child : ((IContainer) packagesContainer).members()) {
          processPackagesResources(child);
        }
        return;
      case REMOVED:
        updater.packageSourcesRemoved((IContainer) packagesContainer);
        return;
      default:
        break;
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
              switch (delta.getKind()) {
                case ADDED:
                  updater.packageSourceAdded((IFile) resource);
                  break;
                case CHANGED:
                  updater.packageSourceChanged((IFile) resource);
                  break;
                case REMOVED:
                  updater.packageSourceRemoved((IFile) resource);
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
            case REMOVED:
              updater.packageSourcesRemoved((IContainer) resource);
              return false;
            default:
              break;
          }

          return true;
        }
      });
    }
  }

  /**
   * Process added resources in the "packages" directory
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
   * Traverse the specified resources and update the associated {@link Project}.
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
        updater.packageSource(proxy);
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

    // Notify context of any Dart source files that have been added
    if (proxy.getType() == FILE) {
      if (name.equals(PUBSPEC_FILE_NAME)) {
        updater.pubspec(proxy);
      } else if (isDartLikeFileName(name)) {
        updater.source(proxy);
      }
      return false;
    }

    // Cache the context and traverse child resource changes
    return setContextFor((IContainer) proxy.requestResource());
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
    if (context == nextContext) {
      return true;
    }
    context = nextContext;
    updater.visitContext(container, context);
    return true;
  }
}
