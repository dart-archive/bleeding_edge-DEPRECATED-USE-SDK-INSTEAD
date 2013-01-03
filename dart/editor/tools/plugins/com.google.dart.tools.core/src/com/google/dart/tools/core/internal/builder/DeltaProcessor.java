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
import org.eclipse.core.resources.IFile;
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
 * Updates a {@link Project} based upon traversing {@link IResource} and {@link IResourceDelta}.
 */
public class DeltaProcessor {

  private final Project project;
  private boolean notifyAvailable;
  private boolean notifyChanged;
  private AnalysisContext context;

  /**
   * Construct a new instance for updating the specified project.
   * 
   * @param project the project being updated (not {@code null})
   */
  public DeltaProcessor(Project project) {
    this.project = project;
  }

  /**
   * Traverse the specified resources and update the associated {@link Project}. Contexts will be
   * notified of new sources via {@link AnalysisContext#sourceAvailable(Source)}, and optionally
   * notified of changed sources via {@link AnalysisContext#sourceChanged(Source)}.
   * 
   * @param resource the container of resources to be recursively traversed
   * @param notifyChanged {@code true} if the context(s) being updated should be modified of changed
   *          sources via {@link AnalysisContext#sourceChanged(Source)}, or {@code false} if not.
   */
  public void traverse(IContainer resource, boolean notifyChanged) throws CoreException {
    this.notifyAvailable = true;
    this.notifyChanged = notifyChanged;
    processResources(resource);
  }

  /**
   * Traverse the specified changes and update the associated {@link Project}. Contexts will be
   * notified of changed sources via {@link AnalysisContext#sourceChanged(Source)}, and optionally
   * notified of new sources via {@link AnalysisContext#sourceAvailable(Source)}.
   * 
   * @param delta the delta describing the resource changes
   * @param notifyAvailable {@code true} if the context(s) being updated should be notified of new
   *          sources via {@link AnalysisContext#sourceAvailable(Source)}, or {@code false} if not.
   */
  public void traverse(IResourceDelta delta, boolean notifyAvailable) throws CoreException {
    this.notifyAvailable = notifyAvailable;
    this.notifyChanged = true;
    processDelta(delta);
  }

  /**
   * Visit a resource proxy in the "packages" directory. Overridden for scan timings.
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
        sourceChanged((IFile) proxy.requestResource(), false, notifyChanged);
      }
      return false;
    }

    // Recursively visit nested folders
    return true;
  }

  /**
   * Visit a resource proxy. Overridden for scan timings.
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
        project.pubspecAdded(proxy.requestResource().getParent());
      } else if (isDartLikeFileName(name)) {
        sourceChanged((IFile) proxy.requestResource(), notifyAvailable, notifyChanged);
      }
      return false;
    }

    // Cache the context and traverse child resource changes
    context = project.getContext((IContainer) proxy.requestResource());
    return true;
  }

  /**
   * Traverse the specified changes and update the associated {@link Project}.
   * 
   * @param delta the delta describing the resource changes
   */
  private void processDelta(IResourceDelta delta) throws CoreException {
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

          // Notify project when pubspec is added or removed.
          // Pubspec changes will be processed by pubspec build participant
          // and result in a "packages" resource delta.
          if (name.equals(PUBSPEC_FILE_NAME)) {
            switch (delta.getKind()) {
              case ADDED:
                project.pubspecAdded(resource.getParent());
                break;
              case REMOVED:
                project.pubspecRemoved(resource.getParent());
                break;
              default:
                break;
            }
          }

          // Notify context of Dart source that have been added, changed, or removed
          if (isDartLikeFileName(name)) {
            switch (delta.getKind()) {
              case ADDED:
                sourceChanged((IFile) resource, notifyAvailable, notifyChanged);
                break;
              case CHANGED:
                sourceChanged((IFile) resource, false, notifyChanged);
                break;
              case REMOVED:
                sourceDeleted((IFile) resource);
                break;
              default:
                break;
            }
          }

          return false;
        }

        // Process "packages" folder changes separately
        if (name.equals(PACKAGES_DIRECTORY_NAME)) {
          context = project.getContext(resource.getParent());
          processPackagesDelta(delta);
          return false;
        }

        switch (delta.getKind()) {
          case ADDED:
            processResources(resource);
            return false;
          case REMOVED:
            project.containerDeleted((IContainer) resource);
            return false;
          default:
            break;
        }

        // Cache the context and traverse child resource changes
        context = project.getContext((IContainer) resource);
        return true;
      }
    });
  }

  /**
   * Process changes in the "packages" directory
   * 
   * @param delta the delta describing the resource changes (not {@code null})
   */
  private void processPackagesDelta(IResourceDelta delta) throws CoreException {

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
        sourcesDeleted((IContainer) packagesContainer);
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
                case CHANGED:
                  sourceChanged((IFile) resource, false, notifyChanged);
                  break;
                case REMOVED:
                  sourceDeleted((IFile) resource);
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
              sourcesDeleted((IContainer) resource);
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
  private void processPackagesResources(IResource resource) throws CoreException {
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
  private void processResources(IResource resource) throws CoreException {
    resource.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        return visitProxy(proxy, proxy.getName());
      }
    }, 0);
  }

  /**
   * Notify the current context of a source file that has changed
   * 
   * @param resource the resource that has changed (not {@code null})
   * @param available {@code true} if the context should be notified that the associated source is
   *          available via {@link AnalysisContext#sourceAvailable(Source)}
   * @param changed {@code true} if the context should be notified that the associated source has
   *          changed via {@link AnalysisContext#sourceChanged(Source)}
   */
  private void sourceChanged(IFile resource, boolean available, boolean changed) {
    IPath location = resource.getLocation();
    if (location == null) {
      DartCore.logInformation("No location for " + resource);
      return;
    }
    Source source = context.getSourceFactory().forFile(location.toFile());
    if (available) {
      context.sourceAvailable(source);
    }
    if (changed) {
      context.sourceChanged(source);
    }
  }

  /**
   * Notify the current context of a source file that has been deleted
   * 
   * @param resource the resource that has been deleted (not {@code null})
   */
  private void sourceDeleted(IFile resource) {
    IPath location = resource.getLocation();
    if (location == null) {
      DartCore.logInformation("No location for " + resource);
      return;
    }
    Source source = context.getSourceFactory().forFile(location.toFile());
    context.sourceDeleted(source);
  }

  /**
   * Notify the current context of sources that were deleted
   * 
   * @param resource the resource container that was deleted (not {@code null})
   */
  private void sourcesDeleted(IContainer resource) {
    IPath location = resource.getLocation();
    if (location == null) {
      DartCore.logInformation("No location for " + resource);
      return;
    }
    SourceContainer container = context.getSourceFactory().forDirectory(location.toFile());
    context.sourcesDeleted(container);
  }
}
