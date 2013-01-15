package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.model.DartSdkManager;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Represents an Eclipse project that has a Dart nature
 */
public class ProjectImpl implements Project {

  /**
   * The Eclipse project associated with this Dart project (not {@code null})
   */
  private final IProject resource;

  /**
   * A mapping of container to context used to analyze Dart source in that container. The content of
   * this map is lazily built as clients request the context for a particular container. A
   * container/context pair is added for each container for which a context is requested, and
   * because a single context is used to analyze an entire directory tree, a particular context may
   * appear multiple times in the map.
   */
  private final HashMap<IContainer, AnalysisContext> contexts = new HashMap<IContainer, AnalysisContext>();

  /**
   * The shared dart URI resolver for the Dart SDK or {@code null} if not initialized yet.
   * Synchronize against {@link #lock} when accessing this field. See {@link #getDartUriResolver()}
   */
  private static DartUriResolver dartResolver;

  /**
   * Lock object for use when accessing {@link #dartResolver}
   */
  private static Object lock = new Object();

  /**
   * Answer the dart URI resolver for the SDK
   * 
   * @return the resolver (not {@code null})
   */
  private static DartUriResolver getDartUriResolver() {
    synchronized (lock) {
      if (dartResolver == null) {
        DartSdkManager sdkManager = com.google.dart.tools.core.model.DartSdkManager.getManager();
        DartSdk sdk = new DartSdk(sdkManager.getSdk().getDirectory());
        dartResolver = new DartUriResolver(sdk);
      }
      return dartResolver;
    }
  }

  /**
   * Construct a new instance representing a Dart project
   * 
   * @param resource the Eclipse project associated with this Dart project (not {@code null})
   */
  public ProjectImpl(IProject resource) {
    if (resource == null) {
      throw new IllegalArgumentException();
    }
    this.resource = resource;
  }

  @Override
  public void discardContextsIn(IContainer container) {
    HashSet<AnalysisContext> toDiscard = new HashSet<AnalysisContext>();
    HashSet<AnalysisContext> toSave = new HashSet<AnalysisContext>();

    // Determine which contexts should be saved, and which should be discarded
    // Since a single context may appear multiple times in the map (see field comment)
    // any given context may be part of both lists
    Iterator<Entry<IContainer, AnalysisContext>> iter = contexts.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<IContainer, AnalysisContext> entry = iter.next();
      if (equalsOrContains(container, entry.getKey())) {
        toDiscard.add(entry.getValue());
        iter.remove();
      } else {
        toSave.add(entry.getValue());
      }
    }

    // Discard only those contexts which are no longer in the map
    for (AnalysisContext context : toDiscard) {
      if (!toSave.contains(context)) {
        context.discard();
      }
    }
  }

  @Override
  public AnalysisContext getContext(IContainer container) {

    // Answer the cached context if there is one
    AnalysisContext context = contexts.get(container);
    if (context != null) {
      return context;
    }

    if (!container.getProject().equals(resource)) {
      throw new IllegalArgumentException();
    }
    IPath location = container.getLocation();
    if (location == null) {
      logNoLocation(container);
      return null;
    }

    // Create a context for analyzing sources in the project
    if (container.equals(resource)) {
      context = createDefaultContext();
    } else {
      AnalysisContext parentContext = getContext(container.getParent());
      if (parentContext == null) {
        return null;
      }

      // If the folder contains a pubspec, then create a new context for analyzing sources
      if (((IFolder) container).getFile(DartCore.PUBSPEC_FILE_NAME).exists()) {
        SourceFactory factory = parentContext.getSourceFactory();
        context = parentContext.extractAnalysisContext(factory.forDirectory(location.toFile()));
      }

      // If no pubspec file, then return the parent context
      else {
        contexts.put(container, parentContext);
        return parentContext;
      }
    }

    // Initialize, cache, and return the new context
    initContext(context, location.toFile());
    contexts.put(container, context);
    return context;
  }

  /**
   * Answer the cached context for the specified container, but do not create a context or retrieve
   * the parent context if a context is not already associated with this container. This differs
   * from {@link #getContext(IContainer)} which will create a context or retrieve the parent context
   * as appropriate if a context is not already associated with this container.
   * 
   * @param container the container (not {@code null})
   * @return the associated context or {@code null} if none.
   */
  public AnalysisContext getExistingContext(IContainer container) {
    return contexts.get(container);
  }

  @Override
  public IProject getResource() {
    return resource;
  }

  @Override
  public void pubspecAdded(IContainer container) {

    // If top level context pubspec added, then clear the current resolution
    if (resource.equals(container)) {
      AnalysisContext context = contexts.get(resource);
      if (context != null) {
        context.clearResolution();
      }
      return;
    }

    AnalysisContext context = contexts.get(container);
    // If a context is not cached, then nothing to be updated
    if (context == null) {
      return;
    }
    IPath location = container.getLocation();
    if (location == null) {
      logNoLocation(container);
      return;
    }
    AnalysisContext parentContext = contexts.get(container.getParent());
    // If a sub context has already been created, then nothing to update
    if (context != parentContext) {
      return;
    }
    // Extract the child context from the parent
    SourceFactory factory = parentContext.getSourceFactory();
    SourceContainer sourceContainer = factory.forDirectory(location.toFile());
    context = parentContext.extractAnalysisContext(sourceContainer);
    initContext(context, location.toFile());
    for (Entry<IContainer, AnalysisContext> entry : contexts.entrySet()) {
      if (equalsOrContains(container, entry.getKey()) && entry.getValue() == parentContext) {
        entry.setValue(context);
      }
    }
  }

  @Override
  public void pubspecRemoved(IContainer container) {

    // If top level context pubspec removed, then clear the current resolution
    if (resource.equals(container)) {
      AnalysisContext context = contexts.get(resource);
      if (context != null) {
        context.clearResolution();
      }
      return;
    }

    // Merge the child context into the parent
    AnalysisContext context = contexts.get(container);
    if (context == null) {
      return;
    }
    AnalysisContext parentContext = contexts.get(container.getParent());
    if (context == parentContext) {
      return;
    }
    parentContext.mergeAnalysisContext(context);
    for (Entry<IContainer, AnalysisContext> entry : contexts.entrySet()) {
      if (entry.getValue() == context) {
        entry.setValue(parentContext);
      }
    }
  }

  /**
   * Create a new default context for this project. This method is overridden during testing to
   * create a mock context rather than a context that would actually perform analysis.
   * 
   * @return the context (not {@code null})
   */
  protected AnalysisContext createDefaultContext() {
    return AnalysisEngine.getInstance().createAnalysisContext();
  }

  /**
   * Answer {@code true} if the container equals or contains the specified resource.
   * 
   * @param directory the directory (not {@code null}, absolute file)
   * @param file the file (not {@code null}, absolute file)
   */
  private boolean equalsOrContains(IContainer container, IResource resource) {
    IPath dirPath = container.getFullPath();
    IPath filePath = resource.getFullPath();
    return dirPath.equals(filePath) || dirPath.isPrefixOf(filePath);
  }

  /**
   * Initialize the context for analyzing sources in the specified directory
   * 
   * @param context the context to be initialized (not {@code null})
   * @param directory the directory to be analyzed
   */
  private void initContext(AnalysisContext context, File directory) {
    DartUriResolver dartResolver = getDartUriResolver();

    File packagesDir = new File(directory, PACKAGES_DIRECTORY_NAME);
    PackageUriResolver pkgResolver = new PackageUriResolver(packagesDir);

    FileUriResolver fileResolver = new FileUriResolver();

    SourceFactory factory = new SourceFactory(dartResolver, pkgResolver, fileResolver);

    // Create and cache the context
    context.setSourceFactory(factory);
  }

  private void logNoLocation(IContainer container) {
    DartCore.logInformation("No location for " + container);
  }
}
