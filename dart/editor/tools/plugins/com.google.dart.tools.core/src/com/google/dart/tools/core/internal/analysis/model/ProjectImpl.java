/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.ExplicitPackageUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.UriResolver;
import com.google.dart.tools.core.CmdLineOptions;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.FileInfo;
import com.google.dart.tools.core.analysis.model.IFileInfo;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.analysis.model.ResourceMap;
import com.google.dart.tools.core.internal.builder.DeltaAdapter;
import com.google.dart.tools.core.internal.builder.DeltaProcessor;
import com.google.dart.tools.core.internal.builder.ResourceDeltaEvent;
import com.google.dart.tools.core.utilities.io.FileUtilities;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import static org.eclipse.core.resources.IResource.PROJECT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Represents an Eclipse project that has a Dart nature.
 * 
 * @coverage dart.tools.core.model
 */
public class ProjectImpl extends ContextManagerImpl implements Project {

  public static class AnalysisContextFactory {
    public AnalysisContext createContext() {
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        throw new IllegalStateException("This method cannot be used with Analysis Server enabled.");
      }
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
        ArrayList<File> files = new ArrayList<File>();
        String[] paths = setting.split(File.pathSeparator);
        for (String path : paths) {
          files.add(new File(path));
        }
        return files.toArray(new File[files.size()]);
      }

      File packOverrideDir = options.getPackageOverrideDirectory();
      if (packOverrideDir != null) {
        File root = new File(packOverrideDir, container.getLocation().toOSString());
        return new File[] {new File(root, DartCore.PACKAGES_DIRECTORY_NAME)};
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
   * The ID of default analysis context for this project or {@code null} if not yet initialized.
   * Synchronize against {@link #pubFolders} and call {@link #initialize()} before accessing this
   * field.
   */
  private String defaultContextId;

  /**
   * The default resource map for this project (not {@code null}). This resource map is only used if
   * no pubspec or package root is defined.
   */
  private ResourceMap defaultResourceMap;

  /**
   * The index which is updated when contexts are discarded (not {@code null}).
   */
  private final Index index;

  /**
   * The default package resolver used for the project
   */
  private UriResolver defaultPackageResolver;

  /**
   * Construct a new instance representing a Dart project
   * 
   * @param resource the Eclipse project associated with this Dart project (not {@code null})
   * @param sdk the Dart SDK to use when initializing contexts (not {@code null})
   * @param sdkContextId the identifier of the Dart SDK context (not {@code null})
   */
  public ProjectImpl(IProject resource, DartSdk sdk, String sdkContextId) {
    this(
        resource,
        sdk,
        sdkContextId,
        DartCore.getProjectManager().getIndex(),
        new AnalysisContextFactory());
  }

  /**
   * Construct a new instance representing a Dart project
   * 
   * @param resource the Eclipse project associated with this Dart project (not {@code null})
   * @param sdk the Dart SDK to use when initializing contexts (not {@code null})
   * @param sdkContextId the identifier of the Dart SDK context (not {@code null})
   * @param index the index to be updated when contexts are discarded (not {@code null})
   * @param factory the factory used to construct new analysis contexts (not {@code null})
   */
  public ProjectImpl(IProject resource, DartSdk sdk, String sdkContextId, Index index,
      AnalysisContextFactory factory) {
    super(sdk, sdkContextId);
    if (resource == null | factory == null | sdk == null) {
      throw new IllegalArgumentException();
    }
    this.projectResource = resource;
    this.index = index;
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
          if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
            // TODO(scheglov) restore or remove for the new API
//            String contextId = entry.getValue().getContextId();
//            DartCore.getAnalysisServer().deleteContext(contextId);
          } else {
            AnalysisContext context = entry.getValue().getContext();
            stopWorkers(context);
            context.dispose();
            index.removeContext(context);
          }
          iter.remove();
        }
      }

      // Reset the state if discarding the entire project
      if (projectResource.equals(container)) {
        if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
          // TODO(scheglov) restore or remove for the new API
//          DartCore.getAnalysisServer().deleteContext(defaultContextId);
//          defaultContextId = null;
        } else {
          stopWorkers(defaultContext);
          defaultContext.dispose();
          index.removeContext(defaultContext);
          defaultContext = null;
        }
        defaultResourceMap = null;
      }
    }
  }

  @Override
  public PubFolder[] getContainedPubFolders(IContainer container) {
    if (!getResource().equals(container.getProject())) {
      throw new IllegalArgumentException();
    }
    ArrayList<PubFolder> result = new ArrayList<PubFolder>();
    IPath prefix = container.getFullPath();
    synchronized (pubFolders) {
      initialize();
      for (Entry<IPath, PubFolder> entry : pubFolders.entrySet()) {
        IPath pubPath = entry.getKey();
        if (prefix.segmentCount() < pubPath.segmentCount() && prefix.isPrefixOf(pubPath)) {
          result.add(entry.getValue());
        }
      }
    }
    return result.toArray(new PubFolder[result.size()]);
  }

  @Override
  public AnalysisContext getContext(IResource resource) {
    synchronized (pubFolders) {
      PubFolder pubFolder = getPubFolder(resource);
      return pubFolder != null ? pubFolder.getContext() : defaultContext;
    }
  }

  @Override
  public String getContextId(IResource resource) {
    synchronized (pubFolders) {
      PubFolder pubFolder = getPubFolder(resource);
      return pubFolder != null ? pubFolder.getContextId() : defaultContextId;
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
  public String getDefaultContextId() {
    synchronized (pubFolders) {
      initialize();
      return defaultContextId;
    }
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
    String pathName = source.getFullName();
    String pathNameFS = FileUtilities.getFileSystemCase(pathName);
    IPath path = new Path(pathNameFS);
    IPath projPath = getResourceLocation();
    if (projPath != null && projPath.isPrefixOf(path)) {
      IPath relPath = path.removeFirstSegments(projPath.segmentCount());
      return projectResource.getFile(relPath);
    }
    // TODO (danrubel): Handle mapped subfolders
    return null;
  }

  @Override
  public ResourceMap getResourceMap(AnalysisContext context) {
    synchronized (pubFolders) {
      for (PubFolder pubFolder : getPubFolders()) {
        AnalysisContext folderContext = pubFolder.getContext();
        if (folderContext == context) {
          return pubFolder;
        }
        if (folderContext instanceof InstrumentedAnalysisContextImpl
            && ((InstrumentedAnalysisContextImpl) folderContext).getBasis() == context) {
          return pubFolder;
        }
      }
    }

    if (defaultContext == context) {
      return defaultResourceMap;
    }
    if (defaultContext instanceof InstrumentedAnalysisContextImpl
        && ((InstrumentedAnalysisContextImpl) defaultContext).getBasis() == context) {
      return defaultResourceMap;
    }
    return null;
  }

  @Override
  public ResourceMap getResourceMap(IResource resource) {
    synchronized (pubFolders) {
      PubFolder pubFolder = getPubFolder(resource);
      return pubFolder != null ? pubFolder : defaultResourceMap;
    }
  }

  @Override
  public ResourceMap getResourceMap(String contextId) {
    synchronized (pubFolders) {
      for (PubFolder pubFolder : getPubFolders()) {
        String folderContextId = pubFolder.getContextId();
        if (folderContextId.equals(contextId)) {
          return pubFolder;
        }
      }
    }
    if (defaultContextId.equals(contextId)) {
      return defaultResourceMap;
    }
    return null;
  }

  @Override
  public boolean isContextInProject(AnalysisContext context) {
    return getResourceMap(context) != null;
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
      DartSdk sdk = getSdk();
      UriResolver pkgResolver = getPackageUriResolver(container, sdk, true);
      PubFolderImpl pubFolder;
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        pubFolder = new PubFolderImpl(
            container,
            null,
            createContextId(container, sdk),
            sdk,
            pkgResolver);
      } else {
        pubFolder = new PubFolderImpl(
            container,
            createContext(container, sdk),
            null,
            sdk,
            pkgResolver);
      }
      pubFolders.put(container.getFullPath(), pubFolder);

      // If this is the project, then adjust the context source factory
      if (container.getType() == PROJECT) {
        initContext(
            defaultContext,
            projectResource,
            sdk,
            getPackageUriResolver(projectResource, sdk, true));
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
        initContext(
            defaultContext,
            projectResource,
            getSdk(),
            getPackageUriResolver(container, getSdk(), false));
      }

      // Traverse container to find pubspec files that were overshadowed by the one just removed
      createPubFolders(container);
    }
  }

  @Override
  public String resolvePathToPackage(String path) {

    PubFolder folder = getPubFolder(new Path(path));
    if (folder != null) {
      return folder.resolvePathToPackage(path);
    } else {
      if (defaultPackageResolver instanceof ExplicitPackageUriResolver) {
        return ((ExplicitPackageUriResolver) defaultPackageResolver).resolvePathToPackage(path);
      }
    }
    return null;
  }

  @Override
  public IFileInfo resolveUriToFileInfo(IResource relativeTo, String uri) {

    AnalysisContext context = getContext(relativeTo);
    Source source = context.getSourceFactory().forUri(uri);

    ResourceMap map = getResourceMap(relativeTo);
    if (source != null && map != null) {
      IFile resource = map.getResource(source);
      if (resource != null) {
        // linked files do not resolve to canonical paths. So find if it is
        // a self referenced package and get the resource in lib instead.
        if (DartCore.isWindows()) {
          int index = 0;
          IPath path = resource.getFullPath();
          while (index < path.segmentCount()
              && !path.segment(index).equals(PACKAGES_DIRECTORY_NAME)) {
            index++;
          }
          if (index != path.segmentCount()) {
            String packageName = path.segment(index + 1);
            if (packageName.equals(DartCore.getSelfLinkedPackageName(resource))) {
              // src/app/packages/app/mylib.dart => /src/app/lib/mylib.dart
              IPath newPath = path.uptoSegment(index).append("lib").append(
                  path.removeFirstSegments(index + 2));
              IResource libResource = ResourcesPlugin.getWorkspace().getRoot().findMember(newPath);
              if (libResource != null) {
                return new FileInfo(libResource.getLocation().toFile(), (IFile) libResource);
              }

            } else {
              // check if the package is open in the workspace, if so return resource from that 
              // project instead of read only packages resource
              // src/app/packages/mypackage/mylib.dart => /src/mypackage/lib/mylib.dart
              String projectPath = getPubFolderPath(packageName);
              if (projectPath != null) {
                IPath newPath = new Path(projectPath).append("lib").append(
                    path.removeFirstSegments(index + 2));
                IResource libResource = ResourcesPlugin.getWorkspace().getRoot().findMember(newPath);
                if (libResource != null) {
                  return new FileInfo(libResource.getLocation().toFile(), (IFile) libResource);
                }
              }
            }
          }
        }

        return new FileInfo(new File(source.getFullName()), resource);
      }
    }
    if (source != null) {
      return new FileInfo(new File(source.getFullName()));
    }
    return null;
  }

  @Override
  public void setAngularAnalysisOption(boolean enable) {
    for (AnalysisContext context : getAnalysisContexts()) {
      AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
      options.setAnalyzeAngular(enable);
      context.setAnalysisOptions(options);
    }
  }

  @Override
  public void setDart2JSHintOption(boolean enableDart2JSHints) {
    for (AnalysisContext context : getAnalysisContexts()) {
      AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
      options.setDart2jsHint(enableDart2JSHints);
      context.setAnalysisOptions(options);
    }
  }

  @Override
  public void setHintOption(boolean enableHint) {
    for (AnalysisContext context : getAnalysisContexts()) {
      AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
      options.setHint(enableHint);
      context.setAnalysisOptions(options);
    }
  }

  @Override
  public String toString() {
    String name;
    try {
      name = getResource().getName();
    } catch (Exception e) {
      name = "unknown: " + e.getMessage();
    }
    return getClass().getSimpleName() + "[" + name + "]@" + Integer.toHexString(hashCode());
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
    return initContext(context, container, sdk, getPackageUriResolver(container, sdk, true));
  }

  /**
   * Answer the ID of a context for the specified container, creating one if necessary. Must
   * synchronize against {@link #pubFolders} before calling this method and either call
   * {@link #initialize()} or check that {@link #isInitialized()} returns {@code true}.
   * 
   * @param container the container with sources to be analyzed (not {@code null})
   * @param sdk the Dart SDK to use when initializing the context (not {@code null})
   * @return the ID of context (not {@code null})
   */
  private String createContextId(IContainer container, DartSdk sdk) {
    if (container.equals(projectResource)) {
      return defaultContextId;
    }
    // TODO(scheglov) Analysis Server
    throw new UnsupportedOperationException();
//    AnalysisContext context;
//    IPath location = container.getLocation();
//    if (location != null) {
//      // TODO(scheglov)
//      SourceContainer sourceContainer = new DirectoryBasedSourceContainer(location.toFile());
//      context = defaultContext.extractContext(sourceContainer);
//    } else {
//      logNoLocation(container);
//      context = factory.createContext();
//    }
//    return initContext(context, container, sdk, getPackageUriResolver(container, sdk, true));
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
          DartSdk sdk = getSdk();
          UriResolver pkgResolver = getPackageUriResolver(container, sdk, true);
          if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
            pubFolders.put(path, new PubFolderImpl(
                container,
                null,
                createContextId(container, sdk),
                sdk,
                pkgResolver));
          } else {
            pubFolders.put(path, new PubFolderImpl(
                container,
                createContext(container, sdk),
                null,
                sdk,
                pkgResolver));
          }
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
   * Find the most appropriate package resolver for the given container, based on presence of
   * pubspec, package roots etc.
   * 
   * @return UriResolver used to resolve package: uris.
   */
  private UriResolver getPackageUriResolver(IContainer container, DartSdk sdk, boolean hasPubspec) {
    File[] packageRoots = factory.getPackageRoots(container);
    if (packageRoots.length > 0) {
      return new PackageUriResolver(packageRoots);
    }
    if (hasPubspec && !DartCoreDebug.NO_PUB_PACKAGES) {
      IPath location = container.getLocation();
      if (location != null) {
        File[] packagesDirs = new File[] {new File(location.toFile(), PACKAGES_DIRECTORY_NAME)};
        return new PackageUriResolver(packagesDirs);
      }
    }
    if (sdk instanceof DirectoryBasedDartSdk) {
      IPath location = container.getLocation();
      if (location != null) {
        return new InstrumentedExplicitPackageUriResolver(
            (DirectoryBasedDartSdk) sdk,
            location.toFile());
      }
    }
    return null;
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
   * Check if there is a project in the workspace for the given package name, if present return the
   * project path.
   * 
   * @param packageName, the package to look for
   * @return the path to project or {@code null}
   */
  private String getPubFolderPath(String packageName) {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      PubFolder[] folders = DartCore.getProjectManager().getProject(project).getPubFolders();
      for (PubFolder pubFolder : folders) {
        try {
          if (pubFolder.getPubspec().getName().equals(packageName)) {
            return pubFolder.getResource().getFullPath().toString();
          }
        } catch (CoreException e) {
          DartCore.logError(e);
        } catch (IOException e) {
          DartCore.logError(e);
        }
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
   * @param hasPubspec {@code true} if the container has a pubspec file
   * @return the context (not {@code null})
   */
  private AnalysisContext initContext(AnalysisContext context, IContainer container, DartSdk sdk,
      UriResolver pkgResolver) {
    DartUriResolver dartResolver = new DartUriResolver(sdk);

    FileUriResolver fileResolver = new FileUriResolver();

    SourceFactory sourceFactory;

    if (pkgResolver != null) {
      sourceFactory = new SourceFactory(dartResolver, pkgResolver, fileResolver);
    } else {
      logNoLocation(container);
      sourceFactory = new SourceFactory(dartResolver, fileResolver);
    }

    sourceFactory.setLocalSourcePredicate(new WorkspaceLocalSourcePredicate(container));

    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    options.setHint(DartCore.getPlugin().isHintsEnabled());
    options.setDart2jsHint(DartCore.getPlugin().isHintsDart2JSEnabled());
    options.setAnalyzeAngular(DartCore.getPlugin().isAngularAnalysisEnabled());
    options.setIncremental(DartCoreDebug.EXPERIMENTAL);

    context.setSourceFactory(sourceFactory);
    context.setAnalysisOptions(options);

    if (DartCoreDebug.NO_PUB_PACKAGES && pkgResolver instanceof ExplicitPackageUriResolver) {
      //TODO (danrubel): Add package sources to context so that it is properly indexed
//      ChangeSet changeSet = new ChangeSet();
//      for (File dir : ((ExplicitPackageUriResolver) pkgResolver).getPackageDirectories()) {
//        // traverse package directory tree
//        changeSet.addedSource(new FileBasedSource(file));
//      }
//      context.applyChanges(changeSet);
    }

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
    defaultPackageResolver = getPackageUriResolver(projectResource, getSdk(), hasPubspec);
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      // TODO(scheglov) restore or remove for the new API
      defaultContextId = "not-a-real-context";
//      String sdkPath = ((DirectoryBasedDartSdk) getSdk()).getDirectory().getAbsolutePath();
//      // TODO(scheglov) Analysis Server: get packages map from Pub
//      Map<String, String> packageMap;
//      {
//        packageMap = Maps.newHashMap();
//        File projectDir = projectResource.getLocation().toFile();
//        File packagesDir = new File(projectDir, "packages");
//        File[] listFiles = packagesDir.listFiles();
//        if (listFiles != null) {
//          for (File packageFile : listFiles) {
//            try {
//              packageMap.put(packageFile.getName(), packageFile.getCanonicalPath());
//            } catch (IOException e) {
//            }
//          }
//        }
//      }
//      AnalysisServer analysisServer = DartCore.getAnalysisServer();
//      defaultContextId = analysisServer.createContext(
//          projectResource.getName(),
//          sdkPath,
//          packageMap);
//      analysisServer.getFixableErrorCodes(defaultContextId, new FixableErrorCodesConsumer() {
//        @Override
//        public void computed(ErrorCode[] errorCodes) {
//          ((AnalysisServerDataImpl) DartCore.getAnalysisServerData()).internalSetFixableErrorCodes(
//              defaultContextId,
//              errorCodes);
//        }
//      });
//      analysisServer.subscribe(
//          defaultContextId,
//          ImmutableMap.of(NotificationKind.ERRORS, SourceSet.EXPLICITLY_ADDED));
//      defaultResourceMap = new SimpleResourceMapImpl(
//          projectResource,
//          defaultContext,
//          defaultContextId);
    } else {
      defaultContext = initContext(
          factory.createContext(),
          projectResource,
          getSdk(),
          defaultPackageResolver);
      defaultResourceMap = new SimpleResourceMapImpl(
          projectResource,
          defaultContext,
          defaultContextId);
    }
    createPubFolders(projectResource);
  }

  /**
   * Answer true if {@link #defaultContext} and {@link #pubFolders} have been initialized. Must
   * synchronize against {@link #pubFolders} before calling this method.
   * 
   * @return {@code true} if initialized, else {@code false}
   */
  private boolean isInitialized() {
    return defaultContext != null || defaultContextId != null;
  }

  private void logNoLocation(IContainer container) {
    DartCore.logInformation("No location for " + container);
  }
}
