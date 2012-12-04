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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.builder.CleanVisitor;
import com.google.dart.tools.core.model.DartSdkManager;

import static com.google.dart.tools.core.DartCore.DART_PROBLEM_MARKER_TYPE;
import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.isDartLikeFileName;
import static com.google.dart.tools.core.DartCore.logError;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import static org.eclipse.core.resources.IResource.DEPTH_INFINITE;
import static org.eclipse.core.resources.IResource.FILE;
import static org.eclipse.core.resources.IResource.FOLDER;
import static org.eclipse.core.resources.IResource.PROJECT;
import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.CHANGED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

import java.util.HashMap;

/**
 * Performs source analysis using instances of {@link AnalysisContext}.
 * {@link AnalysisServerParticipant} should be disabled when this participant is enabled.
 * 
 * @see DartCoreDebug#ENABLE_NEW_ANALYSIS
 */
public class AnalysisEngineParticipant implements BuildParticipant {

  /**
   * Visitor for creating or updating a context
   */
  private class ContextVisitor implements BuildVisitor {

    private AnalysisContext context;
    private IContainer container;

    /**
     * Traverse the specified resource delta and update the existing context.
     * 
     * @param context the context (not {@code null})
     * @param delta the resource delta describing the changes (not {@code null})
     * @param monitor the progress monitor (not {@code null})
     * @return {@code true} if the delta was processed, or {@code false} if the context was
     *         discarded and no analysis performed.
     */
    public boolean traverse(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
      this.container = (IContainer) delta.getResource();

      // If the container was removed, then discard the context
      if (delta.getKind() == REMOVED) {
        context = contexts.remove(container);
        if (context != null) {
          context.discard();
        }
        return false;
      }

      // If this resource was just added or there was no existing context,
      // then traverse it like a new context
      context = contexts.get(container);
      if (delta.getKind() == ADDED || context == null) {
        new NewContextVisitor().createAndTraverse(container, monitor);
        return true;
      }

      // Process any changes to the "packages" directory first
      for (IResourceDelta childDelta : delta.getAffectedChildren()) {
        IResource resource = childDelta.getResource();

        // Process the "packages" directory using the PackagesVisitor
        if (resource.getType() == FOLDER && resource.getName().equals(PACKAGES_DIRECTORY_NAME)) {
          if (new PackagesVisitor().traverse(context, childDelta, monitor)) {
            continue;
          }

          // If the "packages" directory was removed for the project, then re-resolve
          if (container.getType() == PROJECT) {
            context.filesDeleted(resource.getLocation().toFile());
            context.clearResolution();
            break;
          }

          // If the "packages" directory was removed from a folder, then discard the context
          contexts.remove(container);
          context.discard();
          return false;
        }
      }

      // Traverse all resources except hidden resources and resources in the "packages" directory
      new BuildEvent(container, delta, monitor).traverse(this, false);

      // TODO (danrubel): resolve each source in the context

      return true;
    }

    @Override
    public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
      IResource resource = delta.getResource();

      // Parse changed *.dart files and discard those removed
      if (resource.getType() == FILE) {
        if (isDartLikeFileName(resource.getName())) {
          if (delta.getKind() == CHANGED) {
            parse(context, resource, true);
          } else {
            IPath location = resource.getLocation();
            if (location != null) {
              context.sourceDeleted(context.getSourceFactory().forFile(location.toFile()));
            }
          }
        }
        return false;
      }

      // If this folder contains a "packages" directory, then traverse it as a separate context
      IContainer container = (IContainer) resource;
      IFolder packages = container.getFolder(new Path(PACKAGES_DIRECTORY_NAME));
      if (packages.exists()) {
        if (new ContextVisitor().traverse(delta, monitor)) {
          return false;
        }
      }

      // If there is a context associated with this folder, then discard it
      AnalysisContext childContext = contexts.remove(container);
      if (childContext != null) {
        childContext.discard();
      }

      // Recursively visit all changes
      return true;
    }

    @Override
    public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {

      // Parse added *.dart files
      if (proxy.getType() == FILE) {
        if (isDartLikeFileName(proxy.getName())) {
          parse(context, proxy.requestResource(), false);
        }
        return false;
      }

      // Traverse and parse all newly added resources
      IContainer folder = (IContainer) proxy.requestResource();
      new NewContextVisitor().traverseAndParse(context, container, folder, monitor);
      return false;
    }
  }

  /**
   * Visitor for creating a context and analyzing all elements in that context
   */
  private class NewContextVisitor implements CleanVisitor {

    private IContainer container;
    private AnalysisContext context;

    /**
     * Create a new context for the specified container then traverse and analyze the resources
     * 
     * @param container the container (project or folder, not {@code null})
     * @param monitor the progress monitor (not {@code null})
     */
    public void createAndTraverse(IContainer container, IProgressMonitor monitor)
        throws CoreException {

      DartSdkManager sdkManager = com.google.dart.tools.core.model.DartSdkManager.getManager();
      DartSdk sdk = new DartSdk(sdkManager.getSdk().getDirectory());
      DartUriResolver dartResolver = new DartUriResolver(sdk);

      FileUriResolver fileResolver = new FileUriResolver();

      SourceFactory factory;
      IFolder packages = container.getFolder(new Path(PACKAGES_DIRECTORY_NAME));
      if (packages.exists()) {
        PackageUriResolver pkgResolver = new PackageUriResolver(container.getLocation().toFile());
        factory = new SourceFactory(dartResolver, pkgResolver, fileResolver);
      } else {
        factory = new SourceFactory(dartResolver, fileResolver);
      }

      context = contexts.remove(container);
      if (context != null) {
        context.discard();
      }
      context = createContext(container);
      context.setSourceFactory(factory);
      contexts.put(container, context);

      // Traverse all resources except hidden resources and resources in the "packages" directory
      traverseAndParse(context, container, container, monitor);

      // TODO (danrubel): resolve each source in the context
    }

    /**
     * Traverse and parse all resources in the specified folder
     * 
     * @param context the context in which the parse should occur (not {@code null})
     * @param container the context container (not {@code null})
     * @param folder the folder to traverse (may be the same as the container, but not {@code null})
     * @param monitor the progress monitor (not {@code null})
     */
    public void traverseAndParse(AnalysisContext context, IContainer container, IContainer folder,
        IProgressMonitor monitor) throws CoreException {
      this.context = context;
      this.container = container;
      new CleanEvent(folder, monitor).traverse(this, false);
    }

    /**
     * Recursively visit and parse all resources
     */
    @Override
    public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {

      if (proxy.getType() != FILE) {

        // If this is a folder in the receiver's container and has a "packages" directory
        // then create a new context to analyze the content of that container
        IContainer folder = (IContainer) proxy.requestResource();
        if (!folder.equals(container)) {
          IFolder packages = folder.getFolder(new Path(PACKAGES_DIRECTORY_NAME));
          if (packages.exists()) {
            new NewContextVisitor().createAndTraverse(folder, monitor);
            return false;
          }
        }

        // Recursively visit all resources
        return true;
      }

      // Parse any *.dart files found
      if (isDartLikeFileName(proxy.getName())) {
        IResource resource = proxy.requestResource();
        parse(context, resource, false);
      }
      return false;
    }
  }

  /**
   * Visitor for updating an existing context given changes in the "packages" directory
   * 
   * @see PackagesVisitor#traverse(AnalysisContext, IResourceDelta, IProgressMonitor)
   */
  private class PackagesVisitor implements BuildVisitor {
    private AnalysisContext context;
    private SourceFactory factory;

    /**
     * Traverse the specified packages resource delta and update the given context.
     * 
     * @param context the context to be updated (not {@code null})
     * @param delta the delta to be traversed (not {@code null})
     * @param monitor the progress monitor (not {@code null})
     * @return {@code true} if the context was updated, or {@code false} if the packages directory
     *         was removed and the context was not updated
     */
    public boolean traverse(AnalysisContext context, IResourceDelta delta, IProgressMonitor monitor)
        throws CoreException {
      IContainer container = (IContainer) delta.getResource();

      // Sanity check
      if (!container.getName().equals(PACKAGES_DIRECTORY_NAME)) {
        throw new RuntimeException("Expected packages folder");
      }

      // If the packages directory was added, then the context should be reresolved
      if (delta.getKind() == ADDED) {
        context.clearResolution();
        return true;
      }

      // If the packages directory was removed, then the context should be reresolved or discarded
      if (delta.getKind() == REMOVED) {
        return false;
      }

      // Visit all changes in the packages directory
      this.context = context;
      this.factory = context.getSourceFactory();
      new BuildEvent(container, delta, monitor).traverse(this, false);
      return true;
    }

    @Override
    public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
      IResource resource = delta.getResource();

      // Notify the context of anything that was removed
      if (delta.getKind() == REMOVED) {
        IPath location = resource.getLocation();
        if (location != null) {
          if (resource.getType() != FILE) {
            context.filesDeleted(location.toFile());
          } else if (isDartLikeFileName(resource.getName())) {
            context.sourceDeleted(factory.forFile(location.toFile()));
          }
        }
        return false;
      }

      // Recursively visit changes
      if (resource.getType() != FILE) {
        return true;
      }

      // Notify the context about any *.dart files that changed
      if (isDartLikeFileName(resource.getName())) {
        IPath location = resource.getLocation();
        if (location != null) {
          context.sourceChanged(factory.forFile(location.toFile()));
        }
      }
      return false;
    }

    /**
     * If anything was added, then re-resolve the entire context
     */
    @Override
    public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
      if (proxy.getType() != FILE || isDartLikeFileName(proxy.getName())) {
        context.clearResolution();
      }
      return false;
    }
  }

  /**
   * Map of application container (directory containing a pubspec.yaml and a "packages" directory)
   * to the context used to analyze that application.
   */
  private final HashMap<IContainer, AnalysisContext> contexts = new HashMap<IContainer, AnalysisContext>();

  private final boolean enabled;

  public AnalysisEngineParticipant() {
    this(DartCoreDebug.ENABLE_NEW_ANALYSIS);
  }

  public AnalysisEngineParticipant(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Traverse and analyze resources
   */
  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {

    // This participant and AnalysisServerParticipant are mutually exclusive
    if (!enabled) {
      return;
    }

    event.traverse(new BuildVisitor() {

      @Override
      public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
        new ContextVisitor().traverse(delta, monitor);
        return false;
      }

      @Override
      public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
        new NewContextVisitor().createAndTraverse((IContainer) proxy.requestResource(), monitor);
        return false;
      }
    }, false);
  }

  /**
   * Discard all markers and cached analysis
   */
  @Override
  public void clean(CleanEvent event, IProgressMonitor monitor) throws CoreException {

    // This participant and AnalysisServerParticipant are mutually exclusive
    if (!enabled) {
      return;
    }

    for (AnalysisContext context : contexts.values()) {
      context.discard();
    }
    contexts.clear();
    event.getProject().deleteMarkers(DART_PROBLEM_MARKER_TYPE, true, DEPTH_INFINITE);
  }

  /**
   * Create a new {@link AnalysisContext}. This method is overridden during testing to create a mock
   * context rather than a context that would actually perform analysis.
   * 
   * @param container the container (not {@code null}). This parameter is not used here, but exists
   *          for testing to assert that the context is created for the correct container.
   * @return the context (not {@code null})
   */
  protected AnalysisContext createContext(IContainer container) {
    return AnalysisEngine.getInstance().createAnalysisContext();
  }

  /**
   * Create an error marker for the specified resource. This method is overridden during testing to
   * record which markers were requested rather than actually creating markers.
   * 
   * @param resource the resource (not {@code null})
   * @param error the error (not {@code null})
   */
  protected void createMarker(IResource resource, AnalysisError error) {

    ErrorCode errorCode = error.getErrorCode();

    int severity;
    switch (errorCode.getErrorSeverity()) {
      case ERROR:
        severity = IMarker.SEVERITY_ERROR;
        break;
      case WARNING:
        severity = IMarker.SEVERITY_WARNING;
        break;
      default:
        severity = IMarker.SEVERITY_INFO;
        break;
    }

    int offset = error.getOffset();
    int length = error.getLength();
    // TODO (danrubel): calculate line number
    int lineNumber = 1;
    String errorMessage = error.getMessage();
    String errorCodeMessage = errorCode.getMessage();

    try {
      IMarker marker = resource.createMarker(DART_PROBLEM_MARKER_TYPE);
      marker.setAttribute(IMarker.SEVERITY, severity);
      marker.setAttribute(IMarker.CHAR_START, offset);
      marker.setAttribute(IMarker.CHAR_END, offset + length);
      marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
      marker.setAttribute("errorCode", errorCodeMessage);
      marker.setAttribute(IMarker.MESSAGE, errorMessage);
    } catch (CoreException e) {
      logError("Failed to create marker for " + resource + "\n   at " + offset + " message: "
          + errorMessage, e);
    }
  }

  /**
   * Parse the specified resource in the given context
   * 
   * @param context the context (not {@code null})
   * @param resource the resource (not {@code null} and must be a *.dart file)
   * @param changed {@code true} if the source has changed
   */
  protected void parse(AnalysisContext context, final IResource resource, boolean changed) {
    if (!resource.exists()) {
      return;
    }
    IPath location = resource.getLocation();
    if (location == null) {
      return;
    }
    try {
      Source source = context.getSourceFactory().forFile(location.toFile());
      if (changed) {
        context.sourceChanged(source);
      }
      context.parse(source, new AnalysisErrorListener() {
        @Override
        public void onError(AnalysisError error) {
          // TODO (danrubel): replace error listener with method that gets syntactic errors
          createMarker(resource, error);
        }
      });
    } catch (AnalysisException e) {
      // TODO (danrubel): create a marker and log an exception
    }
  }
}
