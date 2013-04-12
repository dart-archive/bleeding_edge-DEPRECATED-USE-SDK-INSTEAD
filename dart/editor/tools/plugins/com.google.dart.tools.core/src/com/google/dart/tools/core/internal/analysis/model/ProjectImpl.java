package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.CmdLineOptions;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.builder.DeltaAdapter;
import com.google.dart.tools.core.internal.builder.DeltaProcessor;
import com.google.dart.tools.core.internal.builder.ResourceDeltaEvent;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import static org.eclipse.core.resources.IResource.PROJECT;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Represents an Eclipse project that has a Dart nature
 */
public class ProjectImpl extends ContextManagerImpl implements Project {

  public static class AnalysisContextFactory {
    public AnalysisContext createContext() {
      return AnalysisEngine.getInstance().createAnalysisContext();
    }

    public File[] getPackageRoots(IContainer container) {
      return ProjectImpl.getPackageRoots(
          DartCore.getPlugin(),
          CmdLineOptions.getOptions(),
          container);
    }
  }

  /**
   * Answer the package roots for the specified project with the given options. May return an empty
   * array if no package roots are specified either at the project level or in the command line
   * options.
   * 
   * @param core the instance of DartCore used to access project preferences
   * @param options the command line options (not {@code null})
   * @param container the container for which package roots are to be returned
   * @return the package roots (not {@code null}, contains no {@code null}s)
   */
  public static File[] getPackageRoots(DartCore core, CmdLineOptions options, IContainer container) {
    if (container instanceof IProject) {
      IEclipsePreferences prefs = core.getProjectPreferences((IProject) container);
      String setting = prefs.get(DartCore.PROJECT_PREF_PACKAGE_ROOT, "");
      if (setting != null && setting.length() > 0) {
        return new File[] {new File(setting)};
      }
    }
    return options.getPackageRoots();
  }

  /**
   * The Eclipse project associated with this Dart project (not {@code null})
   */
  private final IProject projectResource;

  /**
   * The project resource location on disk or {@code null} if it has not yet been initialized.
   * 
   * @see #getResourceLocation()
   */
  private IPath projectResourceLocation;

  /**
   * The factory used to create new {@link AnalysisContext} (not {@code null})
   */
  private final AnalysisContextFactory factory;

  /**
   * A sparse mapping of {@link IResource#getFullPath()} to {@link PubFolder}. Synchronize against
   * this field and call {@link #initialize()} before accessing this field.
   */
  private final HashMap<IPath, PubFolder> pubFolders = new HashMap<IPath, PubFolder>();

  /**
   * The default analysis context for this project or {@code null} if not yet initialized.
   * Synchronize against {@link #pubFolders} and call {@link #initialize()} before accessing this
   * field.
   */
  private AnalysisContext defaultContext;

  /**
   * The Dart SDK used when constructing the default context.
   */
  private final DartSdk sdk;

  /**
   * The index which is updated when contexts are discarded (not {@code null}).
   */
  private final Index index;

  /**
   * Construct a new instance representing a Dart project
   * 
   * @param resource the Eclipse project associated with this Dart project (not {@code null})
   * @param sdk the Dart SDK to use when initializing contexts (not {@code null})
   */
  public ProjectImpl(IProject resource, DartSdk sdk) {
    this(resource, sdk, DartCore.getProjectManager().getIndex(), new AnalysisContextFactory());
  }

  /**
   * Construct a new instance representing a Dart project
   * 
   * @param resource the Eclipse project associated with this Dart project (not {@code null})
   * @param sdk the Dart SDK to use when initializing contexts (not {@code null})
   * @param index the index to be updated when contexts are discarded (not {@code null})
   * @param factory the factory used to construct new analysis contexts (not {@code null})
   */
  public ProjectImpl(IProject resource, DartSdk sdk, Index index, AnalysisContextFactory factory) {
    if (resource == null | factory == null | sdk == null) {
      throw new IllegalArgumentException();
    }
    this.projectResource = resource;
    this.index = index;
    this.sdk = sdk;
    this.factory = factory;
  }

  @Override
  public void discardContextsIn(IContainer container) {
    synchronized (pubFolders) {
      if (!isInitialized()) {
        return;
      }

      // Remove contained pub folders
      IPath path = container.getFullPath();
      Iterator<Entry<IPath, PubFolder>> iter = pubFolders.entrySet().iterator();
      while (iter.hasNext()) {
        Entry<IPath, PubFolder> entry = iter.next();
        IPath key = entry.getKey();
        if (path.equals(key) || path.isPrefixOf(key)) {
          AnalysisContext context = entry.getValue().getContext();
          stopWorkers(context);
          index.removeContext(context);
          iter.remove();
        }
      }

      // Reset the state if discarding the entire project
      if (projectResource.equals(container)) {
        stopWorkers(defaultContext);
        index.removeContext(defaultContext);
        defaultContext = null;
      }
    }
  }

  @Override
  public AnalysisContext getContext(IResource resource) {
    synchronized (pubFolders) {
      PubFolder pubFolder = getPubFolder(resource);
      return pubFolder != null ? pubFolder.getContext() : defaultContext;
    }
  }

  @Override
  public AnalysisContext getDefaultContext() {
    synchronized (pubFolders) {
      initialize();
      return defaultContext;
    }
  }

  @Override
  public Source[] getLaunchableClientLibrarySources() {
    AnalysisContext[] contexts;
    synchronized (pubFolders) {
      contexts = getAnalysisContexts();
    }
    List<Source> sources = new ArrayList<Source>();
    for (AnalysisContext context : contexts) {
      sources.addAll(Arrays.asList(context.getLaunchableClientLibrarySources()));
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public Source[] getLaunchableServerLibrarySources() {
    AnalysisContext[] contexts;
    synchronized (pubFolders) {
      initialize();
      contexts = getAnalysisContexts();
    }
    List<Source> sources = new ArrayList<Source>();
    for (AnalysisContext context : contexts) {
      sources.addAll(Arrays.asList(context.getLaunchableServerLibrarySources()));
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public Source[] getLibrarySources() {
    Set<Source> sources = new HashSet<Source>();
    synchronized (pubFolders) {
      for (PubFolder pubfolder : getPubFolders()) {
        sources.addAll(Arrays.asList(pubfolder.getContext().getLibrarySources()));
      }
      sources.addAll(Arrays.asList(getDefaultContext().getLibrarySources()));
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public PubFolder getPubFolder(IResource resource) {
    if (!resource.getProject().equals(projectResource)) {
      throw new IllegalArgumentException();
    }
    synchronized (pubFolders) {
      initialize();
      return getPubFolder(resource.getFullPath());
    }
  }

  @Override
  public PubFolder[] getPubFolders() {
    Collection<PubFolder> allFolders;
    synchronized (pubFolders) {
      initialize();
      allFolders = pubFolders.values();
    }
    return allFolders.toArray(new PubFolder[allFolders.size()]);
  }

  @Override
  public IProject getResource() {
    return projectResource;
  }

  @Override
  public IResource getResource(Source source) {
    // TODO (danrubel): revisit and optimize performance
    if (source == null) {
      return null;
    }
    IPath path = new Path(source.getFullName());
    IPath projPath = getResourceLocation();
    if (projPath.isPrefixOf(path)) {
      IPath relPath = path.removeFirstSegments(projPath.segmentCount());
      return projectResource.getFile(relPath);
    }
    // TODO (danrubel): Handle mapped subfolders
    return null;
  }

  @Override
  public DartSdk getSdk() {
    return sdk;
  }

  @Override
  public void pubspecAdded(IContainer container) {
    synchronized (pubFolders) {
      if (!isInitialized()) {
        return;
      }

      // If there is already a pubFolder, then ignore the addition
      if (getPubFolder(container.getFullPath()) != null) {
        return;
      }

      // Create and cache a new pub folder
      PubFolderImpl pubFolder = new PubFolderImpl(container, createContext(container, sdk), sdk);
      pubFolders.put(container.getFullPath(), pubFolder);

      // If this is the project, then adjust the context source factory
      if (container.getType() == PROJECT) {
        initContext(defaultContext, projectResource, sdk, true);
      }

      // Merge any overlapping pub folders
      for (Iterator<Entry<IPath, PubFolder>> iter = pubFolders.entrySet().iterator(); iter.hasNext();) {
        Entry<IPath, PubFolder> entry = iter.next();
        PubFolder parent = getParentPubFolder(entry.getKey());
        if (parent != null) {
          parent.getContext().mergeContext(entry.getValue().getContext());
          iter.remove();
        }
      }
    }
  }

  @Override
  public void pubspecRemoved(IContainer container) {
    synchronized (pubFolders) {
      if (!isInitialized()) {
        return;
      }

      // If no pubFolder defined for this container, then ignore
      PubFolder pubFolder = pubFolders.remove(container.getFullPath());
      if (pubFolder == null) {
        return;
      }

      // Merge the context into the default context or re-initialize the default context
      AnalysisContext context = pubFolder.getContext();
      if (defaultContext != context) {
        defaultContext.mergeContext(context);
        index.removeContext(context);
      } else {
        initContext(defaultContext, projectResource, sdk, false);
      }

      // Traverse container to find pubspec files that were overshadowed by the one just removed
      createPubFolders(container);
    }
  }

  /**
   * Answer the {@link AnalysisContext} for the specified container, creating one if necessary. Must
   * synchronize against {@link #pubFolders} before calling this method and either call
   * {@link #initialize()} or check that {@link #isInitialized()} returns {@code true}.
   * 
   * @param container the container with sources to be analyzed (not {@code null})
   * @param sdk the Dart SDK to use when initializing the context (not {@code null})
   * @return the context (not {@code null})
   */
  private AnalysisContext createContext(IContainer container, DartSdk sdk) {
    if (container.equals(projectResource)) {
      return defaultContext;
    }
    AnalysisContext context;
    IPath location = container.getLocation();
    if (location != null) {
      SourceContainer sourceContainer = new DirectoryBasedSourceContainer(location.toFile());
      context = defaultContext.extractContext(sourceContainer);
    } else {
      logNoLocation(container);
      context = factory.createContext();
    }
    return initContext(context, container, sdk, true);
  }

  /**
   * Create pub folders for any pubspec files found within the specified container. Must synchronize
   * against {@link #pubFolders} before calling this method and either call {@link #initialize()} or
   * check that {@link #isInitialized()} returns {@code true}.
   * 
   * @param container the container (not {@code null})
   */
  private void createPubFolders(IContainer container) {
    DeltaProcessor processor = new DeltaProcessor(this);
    processor.addDeltaListener(new DeltaAdapter() {
      @Override
      public void pubspecAdded(ResourceDeltaEvent event) {
        IContainer container = event.getResource().getParent();
        IPath path = container.getFullPath();
        // Pub folders do not nest, so don't create a folder if a parent folder already exists
        if (getParentPubFolder(path) == null) {
          pubFolders.put(path, new PubFolderImpl(container, createContext(container, sdk), sdk));
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
        parent.getContext().mergeContext(entry.getValue().getContext());
        iter.remove();
      }
    }
  }

  /**
   * Answer all the {@link AnalysisContext} for this project. Must synchronize against
   * {@link #pubFolders} before calling this method and either call {@link #initialize()} or check
   * that {@link #isInitialized()} returns {@code true}.
   * 
   * @return all the analysis contexts in the project
   */
  private AnalysisContext[] getAnalysisContexts() {
    Set<AnalysisContext> contexts = new HashSet<AnalysisContext>();
    for (PubFolder folder : getPubFolders()) {
      contexts.add(folder.getContext());
    }
    contexts.add(getDefaultContext());
    return contexts.toArray(new AnalysisContext[contexts.size()]);
  }

  /**
   * Find the {@link PubFolder} defined for an ancestor. Must synchronize against
   * {@link #pubFolders} before calling this method and either call {@link #initialize()} or check
   * that {@link #isInitialized()} returns {@code true}.
   * 
   * @param path the resource's full path
   * @return the containing pub folder or {@code null} if none
   */
  private PubFolder getParentPubFolder(IPath path) {
    return path.segmentCount() > 1 ? getPubFolder(path.removeLastSegments(1)) : null;
  }

  /**
   * Answer the {@link PubFolder} for the specified resource path. Must synchronize against
   * {@link #pubFolders} before calling this method and either call {@link #initialize()} or check
   * that {@link #isInitialized()} returns {@code true}.
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
    if (projectResourceLocation == null) {
      projectResourceLocation = projectResource.getLocation();
    }
    return projectResourceLocation;
  }

  /**
   * Initialize the context for analyzing sources in the specified directory. Must synchronize
   * against {@link #pubFolders} before calling this method and either call {@link #initialize()} or
   * check that {@link #isInitialized()} returns {@code true}.
   * 
   * @param context the context to be initialized (not {@code null})
   * @param container the container with sources to be analyzed
   * @param sdk the Dart SDK to use when initializing the context (not {@code null})
   * @param hasPubspec {@code true} if the contaner has a pubspec file
   * @return the context (not {@code null})
   */
  private AnalysisContext initContext(AnalysisContext context, IContainer container, DartSdk sdk,
      boolean hasPubspec) {
    DartUriResolver dartResolver = new DartUriResolver(sdk);

    FileUriResolver fileResolver = new FileUriResolver();

    File[] packageRoots = factory.getPackageRoots(container);
    File[] packagesDirs = null;
    if (hasPubspec) {
      IPath location = container.getLocation();
      if (location != null) {
        packagesDirs = new File[] {new File(location.toFile(), PACKAGES_DIRECTORY_NAME)};
      }
    } else if (packageRoots.length > 0) {
      packagesDirs = packageRoots;
    }
    SourceFactory sourceFactory;
    if (packagesDirs != null) {
      PackageUriResolver pkgResolver = new PackageUriResolver(packagesDirs);
      sourceFactory = new SourceFactory(dartResolver, pkgResolver, fileResolver);
    } else {
      logNoLocation(container);
      sourceFactory = new SourceFactory(dartResolver, fileResolver);
    }

    context.setSourceFactory(sourceFactory);
    return context;
  }

  /**
   * Initialize {@link #pubFolders} and {@link #defaultContext} if not already been initialized.
   * Must synchronize against {@link #pubFolders} before calling this method.
   */
  private void initialize() {
    if (isInitialized()) {
      return;
    }
    boolean hasPubspec = projectResource.getFile(PUBSPEC_FILE_NAME).exists();
    defaultContext = initContext(factory.createContext(), projectResource, sdk, hasPubspec);
    createPubFolders(projectResource);
  }

  /**
   * Answer true if {@link #defaultContext} and {@link #pubFolders} have been initialized. Must
   * synchronize against {@link #pubFolders} before calling this method.
   * 
   * @return {@code true} if initialized, else {@code false}
   */
  private boolean isInitialized() {
    return defaultContext != null;
  }

  private void logNoLocation(IContainer container) {
    DartCore.logInformation("No location for " + container);
  }
}
