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
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.model.DartSdkManager;

import static com.google.dart.tools.core.DartCore.DART_PROBLEM_MARKER_TYPE;
import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Performs source analysis using instances of {@link AnalysisContext}.
 * {@link AnalysisServerParticipant} should be disabled when this participant is enabled.
 * 
 * @see DartCoreDebug#ENABLE_NEW_ANALYSIS
 */
public class AnalysisEngineParticipant implements BuildParticipant, BuildVisitor {

  private static final Path PUBSPEC_PATH = new Path(PUBSPEC_FILE_NAME);

  private final boolean enabled;

  /**
   * Map of application container (directory containing a pubspec.yaml and a "packages" directory)
   * to the context used to analyze that application.
   */
  private final HashMap<IContainer, AnalysisContext> allContexts = new HashMap<IContainer, AnalysisContext>();

  /**
   * The current context in which analysis should occur.
   */
  private AnalysisContext currentContext;

  /**
   * The container associated with the {@link #currentContext}. This field should be updated
   * whenever {@link #currentContext} is updated.
   */
  private IContainer currentContextContainer;

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

    // Create a context for the project if one does not already exist
    if (allContexts.isEmpty()) {
      IProject project = event.getProject();
      AnalysisContext context = initContext(createRootContext(), project);
      if (context == null) {
        DartCore.logInformation("No location for " + project);
        return;
      }
      allContexts.put(project, context);
    }

    // Traverse resources specified by the build event
    event.traverse(this, false);
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

    for (AnalysisContext context : allContexts.values()) {
      context.discard();
    }
    allContexts.clear();
    event.getProject().deleteMarkers(DART_PROBLEM_MARKER_TYPE, true, DEPTH_INFINITE);
  }

  /**
   * Visit resources, updating contexts and sources without performing any analysis
   */
  @Override
  public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
    IResource resource = delta.getResource();
    if (delta.getKind() == CHANGED) {
      if (resource.getType() == FILE) {
        return fileChanged(resource);
      } else {
        return containerChanged((IContainer) resource, delta);
      }
    } else {
      if (resource.getType() == FILE) {
        return fileRemoved(resource);
      } else {
        return containerRemoved((IContainer) resource);
      }
    }
  }

  /**
   * Visit resources, updating contexts and sources without performing any analysis
   */
  @Override
  public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
    if (proxy.getType() == FILE) {
      return fileAdded(proxy);
    } else {
      return containerAdded((IContainer) proxy.requestResource());
    }
  }

  /**
   * Create a new {@link AnalysisContext} for the project associated with this builder. This method
   * is overridden during testing to create a mock context rather than a context that would actually
   * perform analysis.
   * 
   * @return the context not {@code null}
   */
  protected AnalysisContext createRootContext() {
    return AnalysisEngine.getInstance().createAnalysisContext();
  }

  /**
   * Answer all context defined in the recevier. This is used by tests to assert the correct
   * contexts have been created and sources updated
   * 
   * @return a map of container to context (not {@code null}, contains no {@code null}s)
   */
  protected HashMap<IContainer, AnalysisContext> getAllContexts() {
    return allContexts;
  }

  /**
   * For any container with a pubspec file, create a new context if one does not exist. For any
   * container with an associated context but no pubspec file, discard the context. Update the
   * currently cached context based upon the container being visited.
   * 
   * @param container the container (not {@code null})
   * @return {@code true} if the container's content should be visited
   */
  private boolean containerAdded(IContainer container) {

    // If the container is the project or the container has a pubspec file
    // then create the context if it does not already exist
    if (container.getType() == PROJECT || container.getFile(PUBSPEC_PATH).exists()) {
      if (getOrCreateCurrentContext(container) == null) {
        return false;
      }
    }

    // Discard any existing context and update the current context
    else {
      discardExistingContext((IFolder) container);
    }
    return true;
  }

  /**
   * If a pubspec file is added to a container, then create a new context if one does not exist. If
   * a pubspec file is removed from a container, discard the context.
   * 
   * @param container the container (not {@code null})
   * @param delta the resource delta describing the change
   * @return {@code true} if the container's content should be visited
   */
  private boolean containerChanged(IContainer container, IResourceDelta delta) throws CoreException {
    updateCurrentContext(container);
    boolean isFolder = container.getType() == FOLDER;

    for (IResourceDelta childDelta : delta.getAffectedChildren()) {
      String name = childDelta.getResource().getName();

      // Check for pubspec file changes indicating whether a context should be created or discarded
      // The project level context exists regardless of a pubspec file
      if (isFolder && name.equals(PUBSPEC_FILE_NAME)) {

        // If the pubspec file was added, then create a context if it does not already exist
        if (childDelta.getKind() == ADDED) {
          if (getOrCreateCurrentContext(container) == null) {
            return false;
          }
        }

        // If the pubspec file was removed, then discard any existing context
        else if (childDelta.getKind() == REMOVED) {
          discardExistingContext((IFolder) container);
        }
      }

      // Traverse changes in the "packages" directory
      if (name.equals(PACKAGES_DIRECTORY_NAME)) {
        updatePackages(childDelta);
      }
    }

    return true;
  }

  /**
   * Discard any contexts associated with the specified container or any child containers. Discard
   * any sources in the specified container associated with the current context.
   * 
   * @param container the container being removed (not {@code null})
   * @return {@code false} indicating that the container's content should not be visited
   */
  private boolean containerRemoved(IContainer container) {

    // If the project is removed... discard everything
    if (container.getType() == PROJECT) {
      allContexts.clear();
      return false;
    }

    // Remove any existing contexts in the given container
    Iterator<Entry<IContainer, AnalysisContext>> iter = allContexts.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<IContainer, AnalysisContext> entry = iter.next();
      if (equalsOrContains(container, entry.getKey())) {
        entry.getValue().discard();
        iter.remove();
      }
    }

    updateCurrentContext(container);

    // Delete the sources from the current context
    IPath location = container.getLocation();
    if (location == null) {
      DartCore.logInformation("No location for " + container);
      return false;
    }
    currentContext.directoryDeleted(location.toFile());

    return false;
  }

  /**
   * Discard the specified context associated with the specified folder and merge its source into
   * the parent container's context. Ensure that {@link #currentContext} and
   * {@link #currentContextContainer} are set to the context for analyzing the folder's sources.
   * 
   * @param folder the folder (not {@code null})
   */
  private void discardExistingContext(IFolder folder) {
    AnalysisContext context = allContexts.remove(folder);
    updateCurrentContext(folder.getParent());
    if (context != null) {
      currentContext.mergeAnalysisContext(context);
    }
  }

  /**
   * Answer <code>true</code> if the container equals or contains the specified resource.
   * 
   * @param directory the directory (not <code>null</code>, absolute file)
   * @param file the file (not <code>null</code>, absolute file)
   */
  private boolean equalsOrContains(IContainer container, IResource resource) {
    IPath dirPath = container.getFullPath();
    IPath filePath = resource.getFullPath();
    return dirPath.equals(filePath) || dirPath.isPrefixOf(filePath);
  }

  /**
   * If the file is a Dart source file, then determine the context containing the specified file and
   * notify the context that the source is available.
   * 
   * @param proxy the file proxy (not {@code null})
   */
  private boolean fileAdded(IResourceProxy proxy) {
    Source source;
    if (DartCore.isDartLikeFileName(proxy.getName())) {
      IResource resource = proxy.requestResource();
      IPath location = resource.getLocation();
      if (location != null) {
        source = currentContext.getSourceFactory().forFile(location.toFile());
        currentContext.sourceAvailable(source);
      } else {
        DartCore.logInformation("No location for " + resource);
      }
    }
    return false;
  }

  /**
   * If the file is a Dart source file, then determine the context containing the specified file and
   * notify the context that the source has changed.
   * 
   * @param resource the resource (not {@code null})
   */
  private boolean fileChanged(IResource resource) {
    Source source;
    if (DartCore.isDartLikeFileName(resource.getName())) {
      IPath location = resource.getLocation();
      if (location != null) {
        source = currentContext.getSourceFactory().forFile(location.toFile());
        currentContext.sourceChanged(source);
      } else {
        DartCore.logInformation("No location for " + resource);
      }
    }
    return false;
  }

  private boolean fileRemoved(IResource resource) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Get the context for the specified container, creating and initializing one as necessary. If an
   * existing context is found or a new context successfully created, then {@link #currentContext}
   * is set to that context and {@link #currentContextContainer} set to the specified container
   * 
   * @param container the container for which a context should exist
   * @return the context or {@code null} if there was a problem creating it
   */
  private AnalysisContext getOrCreateCurrentContext(IContainer container) {
    AnalysisContext context = allContexts.get(container);
    // The context for the project is created in the build method and thus should not be null here
    if (context == null) {
      updateCurrentContext(container.getParent());
      IPath location = container.getLocation();
      if (location == null) {
        DartCore.logInformation("No location for " + container);
        return null;
      }
      context = initContext(currentContext.extractAnalysisContext(location.toFile()), container);
      if (context == null) {
        return null;
      }
      allContexts.put(container, context);
    }
    currentContext = context;
    currentContextContainer = container;
    return context;
  }

  /**
   * Initialize the specified context for analyzing sources in the specified container.
   * 
   * @param context the context to be initialized (not {@code null})
   * @param container the container of sources (not {@code null})
   * @return the context or {@code null} if it could not be initialized
   */
  private AnalysisContext initContext(AnalysisContext context, IContainer container) {
    IPath location = container.getLocation();
    if (location == null) {
      DartCore.logInformation("No location for " + container);
      return null;
    }
    File applicationDirectory = location.toFile();

    // Create the resolvers and the source factory

    DartSdkManager sdkManager = com.google.dart.tools.core.model.DartSdkManager.getManager();
    DartSdk sdk = new DartSdk(sdkManager.getSdk().getDirectory());
    DartUriResolver dartResolver = new DartUriResolver(sdk);

    FileUriResolver fileResolver = new FileUriResolver();

    File packagesDir = new File(applicationDirectory, PACKAGES_DIRECTORY_NAME);
    PackageUriResolver pkgResolver = new PackageUriResolver(packagesDir);

    SourceFactory factory = new SourceFactory(dartResolver, pkgResolver, fileResolver);

    // Create and cache the context
    context.setSourceFactory(factory);
    return context;
  }

  /**
   * Update {@link #currentContext} to be the context in which sources in the specified container
   * should be analyzed.
   * 
   * @param container the container (not {@code null})
   */
  private void updateCurrentContext(IContainer container) {

    // Determine if the current context applies to the container
    IContainer parent = container.getParent();
    while (parent != null && !parent.equals(currentContextContainer)) {
      parent = parent.getParent();
    }

    // If the current context does not apply, then find the context that does
    if (parent == null) {
      currentContextContainer = container;
      while (true) {
        currentContext = allContexts.get(currentContextContainer);
        if (currentContext != null) {
          break;
        }
        currentContextContainer = currentContextContainer.getParent();
      }
    }
  }

  /**
   * Traverse changes in the "packages" folder and update the {@link #currentContext}.
   * 
   * @param packagesDelta the delta describing the changes (not {@code null})
   */
  private void updatePackages(IResourceDelta packagesDelta) throws CoreException {
    final boolean[] sourcesAdded = {false};
    packagesDelta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) throws CoreException {

        // If a folder or *.dart file is added, clear the resolution
        if (delta.getKind() == ADDED) {
          IResource resource = delta.getResource();
          if (resource.getType() == FOLDER || DartCore.isDartLikeFileName(resource.getName())) {
            sourcesAdded[0] = true;
          }
          return false;
        }

        // If a *.dart file changes, then update the context
        else if (delta.getKind() == CHANGED) {
          IResource resource = delta.getResource();
          if (resource.getType() == FILE && DartCore.isDartLikeFileName(resource.getName())) {
            IPath location = resource.getLocation();
            if (location == null) {
              DartCore.logInformation("No location for " + resource);
              return false;
            }
            Source source = currentContext.getSourceFactory().forFile(location.toFile());
            currentContext.sourceChanged(source);
          }
        }

        // If a folder or *.dart file is removed, then update the context
        if (delta.getKind() == REMOVED) {
          IResource resource = delta.getResource();
          if (resource.getType() == FOLDER) {
            IPath location = resource.getLocation();
            if (location == null) {
              DartCore.logInformation("No location for " + resource);
              return false;
            }
            currentContext.directoryDeleted(location.toFile());
          } else if (DartCore.isDartLikeFileName(resource.getName())) {
            IPath location = resource.getLocation();
            if (location == null) {
              DartCore.logInformation("No location for " + resource);
              return false;
            }
            Source source = currentContext.getSourceFactory().forFile(location.toFile());
            currentContext.sourceDeleted(source);
          }
        }

        return true;
      }
    });
    if (sourcesAdded[0]) {
      currentContext.clearResolution();
    }
  }
}
