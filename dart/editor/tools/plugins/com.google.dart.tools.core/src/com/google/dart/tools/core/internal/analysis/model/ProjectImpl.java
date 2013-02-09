package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.builder.AbstractDeltaListener;
import com.google.dart.tools.core.internal.builder.DeltaProcessor;
import com.google.dart.tools.core.internal.builder.ResourceDeltaEvent;
import com.google.dart.tools.core.model.DartSdkManager;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Represents an Eclipse project that has a Dart nature
 */
public class ProjectImpl implements Project {

  public static class AnalysisContextFactory {
    public AnalysisContext createContext() {
      return AnalysisEngine.getInstance().createAnalysisContext();
    }
  }

  /**
   * The Eclipse project associated with this Dart project (not {@code null})
   */
  private final IProject resource;

  /**
   * The project resource location on disk or {@code null} if it has not yet been initialized.
   * 
   * @see #getResourceLocation()
   */
  private IPath resourceLocation;

  /**
   * The factory used to create new {@link AnalysisContext} (not {@code null})
   */
  private final AnalysisContextFactory factory;

  /**
   * A sparse mapping of {@link IResource#getFullPath()} to {@link PubFolder} or {@code null} if not
   * yet initialized. Call {@link #initialize()} before accessing this field.
   */
  private HashMap<IPath, PubFolder> pubFolders;

  /**
   * The default analysis context for this project or {@code null} if not yet initialized. Call
   * {@link #initialize()} before accessing this field.
   */
  private AnalysisContext defaultContext;

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
    this(resource, new AnalysisContextFactory());
  }

  /**
   * Construct a new instance representing a Dart project
   * 
   * @param resource the Eclipse project associated with this Dart project (not {@code null})
   */
  public ProjectImpl(IProject resource, AnalysisContextFactory factory) {
    if (resource == null | factory == null) {
      throw new IllegalArgumentException();
    }
    this.resource = resource;
    this.factory = factory;
  }

  @Override
  public void discardContextsIn(IContainer container) {
    if (pubFolders == null) {
      return;
    }

    // Remove contained pub folders
    IPath path = container.getFullPath();
    Iterator<Entry<IPath, PubFolder>> iter = pubFolders.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<IPath, PubFolder> entry = iter.next();
      IPath key = entry.getKey();
      if (path.equals(key) || path.isPrefixOf(key)) {
        entry.getValue().getContext().discard();
        iter.remove();
      }
    }
  }

  @Override
  public AnalysisContext getContext(IContainer container) {
    PubFolder pubFolder = getPubFolder(container);
    return pubFolder != null ? pubFolder.getContext() : defaultContext;
  }

  @Override
  public AnalysisContext getDefaultContext() {
    initialize();
    return defaultContext;
  }

  @Override
  public PubFolder getPubFolder(IContainer container) {
    if (!container.getProject().equals(resource)) {
      throw new IllegalArgumentException();
    }
    initialize();
    return getPubFolder(container.getFullPath());
  }

  @Override
  public PubFolder[] getPubFolders() {
    initialize();
    Collection<PubFolder> allFolders = pubFolders.values();
    return allFolders.toArray(new PubFolder[allFolders.size()]);
  }

  @Override
  public IProject getResource() {
    return resource;
  }

  @Override
  public IResource getResourceFor(Source source) {
    IPath path = new Path(source.getFullName());
    IPath projPath = getResourceLocation();
    if (projPath.isPrefixOf(path)) {
      IPath relPath = path.removeFirstSegments(projPath.segmentCount());
      return resource.getFile(relPath);
    }
    // TODO (danrubel): Handle mapped subfolders
    return null;
  }

  @Override
  public void pubspecAdded(IContainer container) {
    if (pubFolders == null) {
      return;
    }

    // If there is already a pubFolder, then ignore the addition
    if (getPubFolder(container.getFullPath()) != null) {
      return;
    }

    // Create and cache a new pub folder
    PubFolderImpl pubFolder = new PubFolderImpl(container, createContext(container));
    pubFolders.put(container.getFullPath(), pubFolder);

    // Merge any overlapping pub folders
    for (Iterator<Entry<IPath, PubFolder>> iter = pubFolders.entrySet().iterator(); iter.hasNext();) {
      Entry<IPath, PubFolder> entry = iter.next();
      PubFolder parent = getParentPubFolder(entry.getKey());
      if (parent != null) {
        parent.getContext().mergeAnalysisContext(entry.getValue().getContext());
        iter.remove();
      }
    }
  }

  @Override
  public void pubspecRemoved(IContainer container) {
    if (pubFolders == null) {
      return;
    }

    // If no pubFolder defined for this container, then ignore
    PubFolder pubFolder = pubFolders.remove(container.getFullPath());
    if (pubFolder == null) {
      return;
    }

    // Merge the context into the default context
    if (defaultContext != pubFolder.getContext()) {
      defaultContext.mergeAnalysisContext(pubFolder.getContext());
    } else {
      defaultContext.clearResolution();
    }

    // Traverse container to find pubspec files that were overshadowed by the one just removed
    createPubFolders(container);
  }

  /**
   * Answer the {@link AnalysisContext} for the specified container, creating one if necessary. This
   * assumes that {@link #defaultContext} has already been set by {@link #initialize()}.
   * 
   * @param container the container with sources to be analyzed (not {@code null})
   * @return the context (not {@code null})
   */
  private AnalysisContext createContext(IContainer container) {
    if (container.equals(resource)) {
      return defaultContext;
    }
    AnalysisContext context;
    IPath location = container.getLocation();
    if (location != null) {
      SourceFactory defaultFactory = defaultContext.getSourceFactory();
      SourceContainer sourceContainer = defaultFactory.forDirectory(location.toFile());
      context = defaultContext.extractAnalysisContext(sourceContainer);
    } else {
      logNoLocation(container);
      context = factory.createContext();
    }
    return initContext(context, container);
  }

  /**
   * Create pub folders for any pubspec files found within the specified container
   * 
   * @param container the container (not {@code null})
   */
  private void createPubFolders(IContainer container) {
    DeltaProcessor processor = new DeltaProcessor(this);
    processor.addDeltaListener(new AbstractDeltaListener() {
      @Override
      public void pubspecAdded(ResourceDeltaEvent event) {
        IContainer container = event.getResource().getParent();
        IPath path = container.getFullPath();
        // Pub folders do not nest, so don't create a folder if a parent folder already exists
        if (getParentPubFolder(path) == null) {
          pubFolders.put(path, new PubFolderImpl(container, createContext(container)));
        }
      }
    });
    try {
      processor.traverse(container);
    } catch (CoreException e) {
      DartCore.logError("Failed to build pub folder mapping", e);
    }
    // Remove nested pub folders
    Iterator<Entry<IPath, PubFolder>> iter = pubFolders.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<IPath, PubFolder> entry = iter.next();
      PubFolder parent = getParentPubFolder(entry.getKey());
      if (parent != null) {
        parent.getContext().mergeAnalysisContext(entry.getValue().getContext());
        iter.remove();
      }
    }
  }

  /**
   * Find the {@link PubFolder} defined for an ancestor. This method assumes that
   * {@link #initialize()} has already been called.
   * 
   * @param path the resource's full path
   * @return the containing pub folder or {@code null} if none
   */
  private PubFolder getParentPubFolder(IPath path) {
    return path.segmentCount() > 1 ? getPubFolder(path.removeLastSegments(1)) : null;
  }

  /**
   * Answer the {@link PubFolder} for the specified resource path. This method assumes that
   * {@link #initialize()} has already been called.
   * 
   * @param path the resource full path (not {@code null})
   * @return the folder or {@code null} if none is found
   */
  private PubFolder getPubFolder(IPath path) {
    if (pubFolders.size() == 0) {
      return null;
    }
    for (int count = path.segmentCount() - 1; count >= 0; count--) {
      PubFolder pubFolder = pubFolders.get(path.removeLastSegments(count));
      if (pubFolder != null) {
        return pubFolder;
      }
    }
    return null;
  }

  /**
   * Answer the Eclipse project's resource location or {@code null} if it could not be determined.
   */
  private IPath getResourceLocation() {
    if (resourceLocation == null) {
      resourceLocation = resource.getLocation();
    }
    return resourceLocation;
  }

  /**
   * Initialize the context for analyzing sources in the specified directory
   * 
   * @param context the context to be initialized (not {@code null})
   * @param container the container with sources to be analyzed
   * @return the context (not {@code null})
   */
  private AnalysisContext initContext(AnalysisContext context, IContainer container) {
    DartUriResolver dartResolver = getDartUriResolver();

    FileUriResolver fileResolver = new FileUriResolver();

    SourceFactory factory;
    IPath location = container.getLocation();
    if (location != null) {
      File packagesDir = new File(location.toFile(), PACKAGES_DIRECTORY_NAME);
      PackageUriResolver pkgResolver = new PackageUriResolver(packagesDir);
      factory = new SourceFactory(dartResolver, pkgResolver, fileResolver);
    } else {
      logNoLocation(container);
      factory = new SourceFactory(dartResolver, fileResolver);
    }

    context.setSourceFactory(factory);
    return context;
  }

  /**
   * Initialize the {@link #pubFolders} map if it has not already been initialized.
   */
  private void initialize() {
    if (pubFolders != null) {
      return;
    }
    pubFolders = new HashMap<IPath, PubFolder>();
    defaultContext = initContext(factory.createContext(), resource);
    createPubFolders(resource);
  }

  private void logNoLocation(IContainer container) {
    DartCore.logInformation("No location for " + container);
  }
}
