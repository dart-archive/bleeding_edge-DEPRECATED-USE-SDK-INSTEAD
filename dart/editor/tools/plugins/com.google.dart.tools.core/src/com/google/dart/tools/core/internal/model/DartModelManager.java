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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartResourceDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.indexer.locations.LocationPersitence;
import com.google.dart.indexer.standard.StandardDriver;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.DartPreferenceConstants;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.generator.DartProjectGenerator;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.indexer.location.CompilationUnitLocation;
import com.google.dart.tools.core.internal.indexer.location.FieldLocation;
import com.google.dart.tools.core.internal.indexer.location.FunctionLocation;
import com.google.dart.tools.core.internal.indexer.location.FunctionTypeAliasLocation;
import com.google.dart.tools.core.internal.indexer.location.MethodLocation;
import com.google.dart.tools.core.internal.indexer.location.SyntheticLocationType;
import com.google.dart.tools.core.internal.indexer.location.TypeLocation;
import com.google.dart.tools.core.internal.indexer.location.VariableLocation;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaBuilder;
import com.google.dart.tools.core.internal.model.delta.DeltaProcessingState;
import com.google.dart.tools.core.internal.model.delta.DeltaProcessor;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartProjectInfo;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.LibraryReferenceFinder;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.ElementChangedListener;
import com.google.dart.tools.core.problem.ProblemRequestor;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The unique instance of the class <code>DartModelManager</code> is used to manage the elements in
 * the Dart element model.
 */
public class DartModelManager {
  /**
   * Update the classpath variable cache
   */
  public static class EclipsePreferencesListener implements
      IEclipsePreferences.IPreferenceChangeListener {
    @Override
    public void preferenceChange(IEclipsePreferences.PreferenceChangeEvent event) {
      DartCore.notYetImplemented();
      // String propertyName = event.getKey();
      // if (propertyName.startsWith(DartCore.PLUGIN_ID)) {
      // if (propertyName.startsWith(CP_VARIABLE_PREFERENCES_PREFIX)) {
      // String varName =
      // propertyName.substring(CP_VARIABLE_PREFERENCES_PREFIX.length());
      // DartModelManager manager = getInstance();
      // if (manager.variablesWithInitializer.contains(varName)) {
      // // revert preference value as we will not apply it to DartCore
      // // classpath variable
      // String oldValue = (String) event.getOldValue();
      // if (oldValue == null) {
      // // unexpected old value => remove variable from set
      // manager.variablesWithInitializer.remove(varName);
      // } else {
      // manager.getInstancePreferences().put(varName, oldValue);
      // }
      // } else {
      // String newValue = (String) event.getNewValue();
      // IPath newPath;
      // if (newValue != null
      // && !(newValue = newValue.trim()).equals(CP_ENTRY_IGNORE)) {
      // newPath = new Path(newValue);
      // } else {
      // newPath = null;
      // }
      // try {
      // SetVariablesOperation operation = new SetVariablesOperation(
      // new String[]{varName}, new IPath[]{newPath}, false/*
      // * don't
      // * update
      // * preferences
      // */);
      // operation.runOperation(null/* no progress available */);
      // } catch (DartModelException e) {
      // Util.log(
      // e,
      //                  "Could not set classpath variable " + varName + " to " + newPath); //$NON-NLS-1$ //$NON-NLS-2$
      // }
      // }
      // } else if (propertyName.startsWith(CP_CONTAINER_PREFERENCES_PREFIX)) {
      // recreatePersistedContainer(propertyName,
      // (String) event.getNewValue(), false);
      // } else if
      // (propertyName.equals(DartCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER)
      // || propertyName.equals(DartCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER)
      // || propertyName.equals(DartCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE)
      // ||
      // propertyName.equals(DartCore.CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER)
      // || propertyName.equals(DartCore.CORE_JAVA_BUILD_INVALID_CLASSPATH)
      // ||
      // propertyName.equals(DartCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS)
      // ||
      // propertyName.equals(DartCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS)
      // || propertyName.equals(DartCore.CORE_INCOMPLETE_CLASSPATH)
      // || propertyName.equals(DartCore.CORE_CIRCULAR_CLASSPATH)
      // || propertyName.equals(DartCore.CORE_INCOMPATIBLE_JDK_LEVEL)) {
      // DartModelManager manager = DartModelManager.getInstance();
      // DartModelImpl model = manager.getDartModel();
      // DartProject[] projects;
      // try {
      // projects = model.getDartProjects();
      // for (int i = 0, pl = projects.length; i < pl; i++) {
      // DartProjectImpl dartProject = (DartProjectImpl) projects[i];
      // manager.deltaState.addClasspathValidation(dartProject);
      // try {
      // // need to touch the project to force validation by
      // // DeltaProcessor
      // dartProject.getProject().touch(null);
      // } catch (CoreException e) {
      // // skip
      // }
      // }
      // } catch (DartModelException e) {
      // // skip
      // }
      // } else if (propertyName.startsWith(CP_USERLIBRARY_PREFERENCES_PREFIX))
      // {
      // String libName =
      // propertyName.substring(CP_USERLIBRARY_PREFERENCES_PREFIX.length());
      // UserLibraryManager manager = DartModelManager.getUserLibraryManager();
      // manager.updateUserLibrary(libName, (String) event.getNewValue());
      // }
      // }
      // Reset all project caches (see
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=233568 )
      try {
        DartProject[] projects = DartModelManager.getInstance().getDartModel().getDartProjects();
        for (int i = 0, length = projects.length; i < length; i++) {
          ((DartProjectImpl) projects[i]).resetCaches();
        }
      } catch (DartModelException e) {
        // cannot retrieve Dart projects
      }
    }
  }

  /**
   * Instances of the class <code>LibrarySearchResult</code> encapsulate both a library file and the
   * result of parsing that library file. The class exists so that we can return multiple values
   * from a method in order to improve performance.
   */
  private static class LibrarySearchResult {
    /**
     * The file defining the library.
     */
    private File libraryFile;

    /**
     * The result of parsing the file.
     */
    private DartUnit libraryUnit;

    /**
     * Initialize a newly created search result to represent the library defined by the given file
     * and having the given AST structure.
     * 
     * @param libraryFile the file defining the library
     * @param libraryUnit the result of parsing the file
     */
    public LibrarySearchResult(File libraryFile, DartUnit libraryUnit) {
      this.libraryFile = libraryFile;
      this.libraryUnit = libraryUnit;
    }
  }

  public static void shutdown() {
    if (UniqueInstance != null) {
      UniqueInstance.shutdownImpl();
    }
  }

  /**
   * A flag indicating whether an AbortCompilationUnit should be thrown when the source of a
   * compilation unit cannot be retrieved.
   */
  public ThreadLocal<Boolean> abortOnMissingSource = new ThreadLocal<Boolean>();

  /**
   * The unique instance of the element representing the workspace.
   */
  private final DartModelImpl model;

  /**
   * The cache used to map model elements to the corresponding info objects. Accesses to this field
   * should always be synchronized on the value of this field.
   */
  private DartModelCache infoCache;

  /**
   * Temporary cache of newly opened elements.
   */
  private ThreadLocal<HashMap<DartElement, DartElementInfo>> temporaryCache = new ThreadLocal<HashMap<DartElement, DartElementInfo>>();

  /**
   * Set of elements which are out of sync with their buffers.
   */
  private HashSet<OpenableElementImpl> elementsOutOfSynchWithBuffers = new HashSet<OpenableElementImpl>(
      11);

  /**
   * Table from IProject to PerProjectInfo. NOTE: this object itself is used as a lock to
   * synchronize creation/removal of per project infos.
   */
  private Map<IProject, PerProjectInfo> perProjectInfos = new HashMap<IProject, PerProjectInfo>(5);

  /**
   * Table from WorkingCopyOwner to a table of CompilationUnit (working copy handle) to
   * PerWorkingCopyInfo. NOTE: this object itself is used as a lock to synchronize creation/removal
   * of per working copy infos.
   */
  private Map<WorkingCopyOwner, Map<CompilationUnit, PerWorkingCopyInfo>> perWorkingCopyInfos = new HashMap<WorkingCopyOwner, Map<CompilationUnit, PerWorkingCopyInfo>>(
      5);

  /**
   * Holds the state used for delta processing.
   */
  private DeltaProcessingState deltaState = new DeltaProcessingState();

  private HashSet<String> optionNames = new HashSet<String>(20);

  private HashMap<String, String> optionsCache;

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

  private static final int PREF_INSTANCE = 0;

  private static final int PREF_DEFAULT = 1;

  public static final String RECONCILE_PERF = DartCore.PLUGIN_ID + "/perf/reconcile"; //$NON-NLS-1$

  /**
   * Listener on eclipse preferences changes.
   */
  private EclipsePreferencesListener instancePreferencesListener = new EclipsePreferencesListener();

  /**
   * Listener on eclipse preferences default/instance node changes.
   */
  private IEclipsePreferences.INodeChangeListener instanceNodeListener = new IEclipsePreferences.INodeChangeListener() {
    @Override
    public void added(IEclipsePreferences.NodeChangeEvent event) {
      // do nothing
    }

    @Override
    public void removed(IEclipsePreferences.NodeChangeEvent event) {
      if (event.getChild() == DartModelManager.this.preferencesLookup[PREF_INSTANCE]) {
        DartModelManager.this.preferencesLookup[PREF_INSTANCE] = getInstanceScope().getNode(
            DartCore.PLUGIN_ID);
        DartModelManager.this.preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(new EclipsePreferencesListener());
      }
    }
  };

  private IEclipsePreferences.INodeChangeListener defaultNodeListener = new IEclipsePreferences.INodeChangeListener() {
    @Override
    public void added(IEclipsePreferences.NodeChangeEvent event) {
      // do nothing
    }

    @Override
    public void removed(IEclipsePreferences.NodeChangeEvent event) {
      if (event.getChild() == DartModelManager.this.preferencesLookup[PREF_DEFAULT]) {
        DartModelManager.this.preferencesLookup[PREF_DEFAULT] = getDefaultScope().getNode(
            DartCore.PLUGIN_ID);
      }
    }
  };

  /**
   * Listener on properties changes.
   */
  private IEclipsePreferences.IPreferenceChangeListener propertyListener;

  private IEclipsePreferences.IPreferenceChangeListener resourcesPropertyListener;

  /**
   * The unique instance of this class.
   */
  private static DartModelManager UniqueInstance;

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public synchronized static DartModelManager getInstance() {
    if (UniqueInstance == null) {
      UniqueInstance = new DartModelManager();
      UniqueInstance.startupImpl();
    }

    return UniqueInstance;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private DartModelManager() {
    DartCore.notYetImplemented();
    model = new DartModelImpl();
  }

  /**
   * Add the given listener to the list of objects that are listening for changes to Dart elements.
   * Has no effect if an identical listener is already registered. After completion of this method,
   * the given listener will be registered for exactly the specified events. If they were previously
   * registered for other events, they will be de-registered.
   * <p>
   * Once registered, a listener starts receiving notification of changes to Dart elements in the
   * model. The listener continues to receive notifications until it is replaced or removed.
   * </p>
   * <p>
   * Listeners can listen for several types of event as defined in <code>ElementChangeEvent</code>.
   * Clients are free to register for any number of event types, however if they register for more
   * than one, it is their responsibility to ensure they correctly handle the case where the same
   * Dart element change shows up in multiple notifications. Clients are guaranteed to receive only
   * the events for which they are registered.
   * </p>
   * 
   * @param listener the listener being added
   * @param eventMask the bit-wise OR of all event types of interest to the listener
   */
  public void addElementChangedListener(ElementChangedListener listener, int eventMask) {
    deltaState.addElementChangedListener(listener, eventMask);
  }

  /**
   * Add the given resource to the set of ignored resources.
   * 
   * @param resource the resource to ignore
   * @throws IOException if there was an error accessing the ignore file
   * @throws CoreException if there was an error deleting markers
   */
  public void addToIgnores(IResource resource) throws IOException, CoreException {
    DartIgnoreManager.getInstance().addToIgnores(resource);
  }

  /**
   * The given project is being removed. Remove all containers for this project from the cache.
   */
  public synchronized void containerRemove(DartProject project) {
    DartCore.notYetImplemented();
    // Map initializations = (Map) containerInitializationInProgress.get();
    // if (initializations != null) {
    // initializations.remove(project);
    // }
    // containers.remove(project);
  }

  /**
   * Return the Dart model element associated with the given file, or <code>null</code> if the file
   * does not have a corresponding element in the model.
   * 
   * @param file the file corresponding to the element to be returned
   * @return the Dart model element associated with the given file
   */
  public DartElementImpl create(IFile file) {
    if (file == null) {
      return null;
    }
    DartProjectImpl project = create(file.getProject());
    try {
      for (DartLibrary library : project.getChildrenOfType(DartLibrary.class)) {
        for (DartElement child : library.getChildren()) {
          IResource resource = child.getCorrespondingResource();
          if (resource != null && resource.equals(file)) {
            return (DartElementImpl) child;
          }
        }
      }
    } catch (DartModelException exception) {
      // Cannot access the file through the project structure, fall through to
      // try a different approach.
    }

    IContainer parent = file.getParent();
    if (parent == null) {
      return null;
    }
    return findChild(create(parent), file);
  }

  /**
   * Return the Dart model element associated with the given folder, or <code>null</code> if the
   * folder does not have a corresponding element in the model.
   * 
   * @param folder the folder corresponding to the element to be returned
   * @return the Dart model element associated with the given folder
   */
  public DartElementImpl create(IFolder folder) {
    if (folder == null) {
      return null;
    }
    IContainer parent = folder.getParent();
    if (parent == null) {
      return null;
    }
    return findChild(create(parent), folder);
  }

  /**
   * Return the Dart model element associated with the given project, or <code>null</code> if the
   * project does not have a corresponding element in the model.
   * 
   * @param project the project corresponding to the element to be returned
   * @return the Dart model element associated with the given project
   */
  public DartProjectImpl create(IProject project) {
    if (project == null) {
      return null;
    }
    return new DartProjectImpl(model, project);
  }

  /**
   * Return the Dart model element associated with the given resource, or <code>null</code> if the
   * resource does not have a corresponding element in the model.
   * 
   * @param resource the resource corresponding to the element to be returned
   * @return the Dart model element associated with the given resource
   */
  public DartElementImpl create(IResource resource) {
    if (resource instanceof IWorkspaceRoot) {
      return model;
    } else if (resource instanceof IProject) {
      return create((IProject) resource);
    } else if (resource instanceof IFolder) {
      return create((IFolder) resource);
    } else if (resource instanceof IFile) {
      return create((IFile) resource);
    }
    return null;
  }

  /**
   * Discard the per working copy info for the given working copy (making it a compilation unit) if
   * its use count was 1. Otherwise, just decrement the use count. If the working copy is primary,
   * computes the delta between its state and the original compilation unit and register it. Close
   * the working copy, its buffer and remove it from the shared working copy table. Ignore if no
   * per-working copy info existed. NOTE: it must NOT be synchronized as it may interact with the
   * element info cache (if useCount is decremented to 0), see bug 50667. Returns the new use count
   * (or -1 if it didn't exist).
   */
  public int discardPerWorkingCopyInfo(CompilationUnitImpl workingCopy) throws DartModelException {
    // create the delta builder (this remembers the current content of the
    // working copy)
    // outside the perWorkingCopyInfos lock (see bug 50667)
    DartElementDeltaBuilder deltaBuilder = null;
    if (workingCopy.isPrimary() && workingCopy.hasUnsavedChanges()) {
      deltaBuilder = new DartElementDeltaBuilder(workingCopy);
    }
    PerWorkingCopyInfo info = null;
    synchronized (perWorkingCopyInfos) {
      WorkingCopyOwner owner = workingCopy.getOwner();
      Map<CompilationUnit, PerWorkingCopyInfo> workingCopyToInfos = perWorkingCopyInfos.get(owner);
      if (workingCopyToInfos == null) {
        return -1;
      }
      info = workingCopyToInfos.get(workingCopy);
      if (info == null) {
        return -1;
      }
      info.setUseCount(info.getUseCount() - 1);
      if (info.getUseCount() == 0) {
        // remove per working copy info
        workingCopyToInfos.remove(workingCopy);
        if (workingCopyToInfos.isEmpty()) {
          perWorkingCopyInfos.remove(owner);
        }
      }
    }
    if (info.getUseCount() == 0) { // info cannot be null here (check was done
                                   // above)
      // remove infos + close buffer (since no longer working copy)
      // outside the perWorkingCopyInfos lock (see bug 50667)
      removeInfoAndChildren(workingCopy);
      workingCopy.closeBuffer();

      // compute the delta if needed and register it if there are changes
      if (deltaBuilder != null) {
        deltaBuilder.buildDeltas();
        if (deltaBuilder.delta != null) {
          getDeltaProcessor().registerDartModelDelta(deltaBuilder.delta);
        }
      }
    }
    return info.getUseCount();
  }

  /**
   * Given some directory {@link File}, return the {@link DartLibrary} in that directory, if there
   * is such a Dart library that is also already loaded in the Dart Editor workspace. Otherwise,
   * return <code>null</code>.
   * 
   * @see #findFirstLibraryContaining(File)
   * @param dir some existing directory on disk
   * @return the first found {@link DartLibrary} in the passed {@link File} directory
   */
  public DartLibrary findLibraryInDirectory(File dir) {
    if (dir == null || !dir.exists()) {
      return null;
    }
    // create a FilenameFilter which filters out all non *.dart files
    FilenameFilter onlyDartFileFilter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String fileName) {
        return DartCore.isDartLikeFileName(fileName);
      }
    };
    // use the filter to loop through the set of *.dart files in this directory
    File[] dartFiles = dir.listFiles(onlyDartFileFilter);
    if (dartFiles != null) {
      for (File child : dartFiles) {
        DartUnit libraryUnit = parseLibraryFile(child);
        if (libraryUnit != null) {
          // found a library file, return it if it is loaded into the workspace
          IResource[] children = ResourceUtil.getResources(child);
          if (children.length > 0) {
            DartElement element = DartCore.create(children[0]);
            if (element instanceof CompilationUnit && ((CompilationUnit) element).definesLibrary()) {
              return ((CompilationUnit) element).getLibrary();
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Return the Dart element representing the root of the workspace.
   * 
   * @return the Dart element representing the root of the workspace
   */
  public DartModelImpl getDartModel() {
    return model;
  }

  /**
   * Return a table of all known configurable options with their default values. These options allow
   * to configure the behavior of the underlying components. The client may safely use the result as
   * a template that they can modify and then pass to {@link #setOptions()}</code>.
   * <p>
   * Helper constants have been defined on DartPreferenceConstants for each of the option IDs
   * (categorized in Code assist option ID, Compiler option ID and Core option ID) and some of their
   * acceptable values (categorized in Option value). Some options accept open value sets beyond the
   * documented constant values.
   * <p>
   * Note: each release may add new options.
   * 
   * @return a table of all known configurable options with their default values
   */
  public Hashtable<String, String> getDefaultOptions() {
    DartCore.notYetImplemented();
    return new Hashtable<String, String>();
  }

  /**
   * Get the default eclipse preference for DartCore plugin.
   */
  public IEclipsePreferences getDefaultPreferences() {
    return preferencesLookup[PREF_DEFAULT];
  }

  public DeltaProcessor getDeltaProcessor() {
    return deltaState.getDeltaProcessor();
  }

  /**
   * Return the information associated with the given element. Using this method to access the
   * information will cause it to be marked as being used for purposes of keeping it in the cache.
   * 
   * @param element the element associated with the information to be returned
   * @return the information associated with the given element
   */
  public DartElementInfo getInfo(DartElement element) {
    HashMap<DartElement, DartElementInfo> tempCache = temporaryCache.get();
    if (tempCache != null) {
      DartElementInfo result = tempCache.get(element);
      if (result != null) {
        return result;
      }
    }
    synchronized (infoCache) {
      return infoCache.getInfo(element);
    }
  }

  /**
   * Get the workspace eclipse preference for DartCore plug-in.
   */
  public IEclipsePreferences getInstancePreferences() {
    return preferencesLookup[PREF_INSTANCE];
  }

  /**
   * Utility method for returning one option value only. Equivalent to
   * <code>DartModelManager.getOptions().get(optionName)</code> Note that it may answer
   * <code>null</code> if this option does not exist.
   * <p>
   * Helper constants have been defined on DartPreferenceConstants for each of the option IDs
   * (categorized in Code assist option ID, Compiler option ID and Core option ID) and some of their
   * acceptable values (categorized in Option value). Some options accept open value sets beyond the
   * documented constant values.
   * <p>
   * Note: each release may add new options.
   * 
   * @param optionName the name of the option whose value is to be returned
   * @return the value of a given option
   */
  public String getOption(String optionName) {
    DartCore.notYetImplemented();
    // if (DartCore.CORE_ENCODING.equals(optionName)){
    // return DartCore.getEncoding();
    // }
    String propertyName = optionName;
    if (optionNames.contains(propertyName)) {
      IPreferencesService service = Platform.getPreferencesService();
      String value = service.get(optionName, null, preferencesLookup);
      return value == null ? null : value.trim();
    }
    return null;
  }

  public HashSet<String> getOptionNames() {
    return optionNames;
  }

  /**
   * Return the table of the current options. Initially, all options have their default values, and
   * this method returns a table that includes all known options.
   * <p>
   * Helper constants have been defined on DartPreferenceConstants for each of the option IDs
   * (categorized in Code assist option ID, Compiler option ID and Core option ID) and some of their
   * acceptable values (categorized in Option value). Some options accept open value sets beyond the
   * documented constant values.
   * <p>
   * Note: each release may add new options.
   * <p>
   * Returns a default set of options even if the platform is not running.
   * </p>
   * 
   * @return table of current settings of all options (key type: <code>String</code>; value type:
   *         <code>String</code>)
   */
  public HashMap<String, String> getOptions() {
    // return cached options if already computed
    HashMap<String, String> cachedOptions; // use a local variable to avoid race
                                           // condition (see
                                           // https://bugs.eclipse.org/bugs/show_bug.cgi?id=256329
                                           // )
    if ((cachedOptions = optionsCache) != null) {
      return new HashMap<String, String>(cachedOptions);
    }

    if (!Platform.isRunning()) {
      optionsCache = getDefaultOptionsNoInitialization();
      return new HashMap<String, String>(optionsCache);
    }
    // init
    HashMap<String, String> options = new HashMap<String, String>(10);
    IPreferencesService service = Platform.getPreferencesService();

    // set options using preferences service lookup
    Iterator<String> iterator = optionNames.iterator();
    while (iterator.hasNext()) {
      String propertyName = iterator.next();
      String propertyValue = service.get(propertyName, null, preferencesLookup);
      if (propertyValue != null) {
        options.put(propertyName, propertyValue);
      }
    }

    // // get encoding through resource plugin
    // options.put(DartCore.CORE_ENCODING, DartCore.getEncoding());
    //
    // // backward compatibility
    // addDeprecatedOptions(options);
    // try {
    // final IEclipsePreferences eclipsePreferences =
    // preferencesLookup[PREF_INSTANCE];
    // String[] instanceKeys = eclipsePreferences.keys();
    // for (int i=0, length=instanceKeys.length; i<length; i++) {
    // String optionName = instanceKeys[i];
    // migrateObsoleteOption(options, optionName,
    // eclipsePreferences.get(optionName, null));
    // }
    // } catch (BackingStoreException e) {
    // // skip
    // }
    //
    // Util.fixTaskTags(options);
    // store built map in cache
    optionsCache = new HashMap<String, String>(options);

    // return built map
    return options;
  }

  /**
   * Return the per-project info for the given project. If specified, create the info if the info
   * doesn't exist.
   */
  public PerProjectInfo getPerProjectInfo(IProject project, boolean create) {
    synchronized (perProjectInfos) {
      // use the perProjectInfo collection as its own lock
      PerProjectInfo info = perProjectInfos.get(project);
      if (info == null && create) {
        info = new PerProjectInfo(project);
        perProjectInfos.put(project, info);
      }
      return info;
    }
  }

  /**
   * Return the per-project info for the given project. If the info doesn't exist, check for the
   * project existence and create the info.
   * 
   * @throws DartModelException if the project doesn't exist
   */
  public PerProjectInfo getPerProjectInfoCheckExistence(IProject project) throws DartModelException {
    PerProjectInfo info = getPerProjectInfo(project, false);
    if (info == null) {
      if (!DartProjectNature.hasDartNature(project)) {
        throw ((DartProjectImpl) DartCore.create(project)).newNotPresentException();
      }
      info = getPerProjectInfo(project, true);
    }
    return info;
  }

  /**
   * Return the per-working copy info for the given working copy at the given path. If it doesn't
   * exist and <code>create</code> is <code>true</code>, add a new per-working copy info with the
   * given problem requester. If recordUsage, increment the per-working copy info's use count.
   * Return <code>null</code> if it doesn't exist and is not created.
   */
  public PerWorkingCopyInfo getPerWorkingCopyInfo(CompilationUnitImpl workingCopy, boolean create,
      boolean recordUsage, ProblemRequestor problemRequestor) {
    synchronized (perWorkingCopyInfos) { // use the perWorkingCopyInfo
                                         // collection as its own lock
      WorkingCopyOwner owner = workingCopy.getOwner();
      Map<CompilationUnit, PerWorkingCopyInfo> workingCopyToInfos = perWorkingCopyInfos.get(owner);
      if (workingCopyToInfos == null && create) {
        workingCopyToInfos = new HashMap<CompilationUnit, PerWorkingCopyInfo>();
        perWorkingCopyInfos.put(owner, workingCopyToInfos);
      }
      PerWorkingCopyInfo info = workingCopyToInfos == null ? null
          : (PerWorkingCopyInfo) workingCopyToInfos.get(workingCopy);
      if (info == null && create) {
        info = new PerWorkingCopyInfo(workingCopy, problemRequestor);
        workingCopyToInfos.put(workingCopy, info);
      }
      if (info != null && recordUsage) {
        info.incrementUseCount();
      }
      return info;
    }
  }

  /**
   * Return the temporary cache for newly opened elements for the current thread, creating it if not
   * already created.
   * 
   * @return the temporary cache for newly opened elements for the current thread
   */
  public HashMap<DartElement, DartElementInfo> getTemporaryCache() {
    HashMap<DartElement, DartElementInfo> result = temporaryCache.get();
    if (result == null) {
      result = new HashMap<DartElement, DartElementInfo>();
      temporaryCache.set(result);
    }
    return result;
  }

  /**
   * Return all of the working copies which have the given owner. If the given owner is not the
   * primary owner and the given flag is <code>true</code>, then the working copies of the primary
   * owner will also be added. Return <code>null</code> if there are no working copies that match
   * the specification.
   * 
   * @return all of the working copies which have the given owner
   */
  public CompilationUnit[] getWorkingCopies(WorkingCopyOwner owner, boolean addPrimary) {
    synchronized (perWorkingCopyInfos) {
      CompilationUnit[] primaryWCs = addPrimary && owner != DefaultWorkingCopyOwner.getInstance()
          ? getWorkingCopies(DefaultWorkingCopyOwner.getInstance(), false) : null;
      Map<CompilationUnit, PerWorkingCopyInfo> workingCopyToInfos = perWorkingCopyInfos.get(owner);
      if (workingCopyToInfos == null) {
        return primaryWCs;
      }
      int primaryLength = primaryWCs == null ? 0 : primaryWCs.length;
      // note that size will be > 0 otherwise workingCopyToInfos would be null
      int size = workingCopyToInfos.size();
      CompilationUnit[] result = new CompilationUnit[primaryLength + size];
      int index = 0;
      if (primaryWCs != null) {
        for (int i = 0; i < primaryLength; i++) {
          CompilationUnit primaryWorkingCopy = primaryWCs[i];
          CompilationUnit workingCopy = new CompilationUnitImpl(
              (DartLibraryImpl) primaryWorkingCopy.getParent(),
              (IFile) primaryWorkingCopy.getResource(), owner);
          if (!workingCopyToInfos.containsKey(workingCopy)) {
            result[index++] = primaryWorkingCopy;
          }
        }
        if (index != primaryLength) {
          System.arraycopy(result, 0, result = new CompilationUnit[index + size], 0, index);
        }
      }
      Iterator<PerWorkingCopyInfo> iterator = workingCopyToInfos.values().iterator();
      while (iterator.hasNext()) {
        result[index++] = iterator.next().getWorkingCopy();
      }
      return result;
    }
  }

  /**
   * Return <code>true</code> if there is a temporary cache for the current thread.
   * 
   * @return <code>true</code> if there is a temporary cache for the current thread
   */
  public boolean hasTemporaryCache() {
    return temporaryCache.get() != null;
  }

  /**
   * Initialize preferences lookups for DartCore plug-in.
   */
  public void initializePreferences() {
    // Create lookups
    preferencesLookup[PREF_INSTANCE] = getInstanceScope().getNode(DartCore.PLUGIN_ID);
    preferencesLookup[PREF_DEFAULT] = getDefaultScope().getNode(DartCore.PLUGIN_ID);

    // Listen to instance preferences node removal from parent in order to
    // refresh stored one
    instanceNodeListener = new IEclipsePreferences.INodeChangeListener() {
      @Override
      public void added(IEclipsePreferences.NodeChangeEvent event) {
        // do nothing
      }

      @Override
      public void removed(IEclipsePreferences.NodeChangeEvent event) {
        if (event.getChild() == DartModelManager.this.preferencesLookup[PREF_INSTANCE]) {
          DartModelManager.this.preferencesLookup[PREF_INSTANCE] = getInstanceScope().getNode(
              DartCore.PLUGIN_ID);
          DartModelManager.this.preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(new EclipsePreferencesListener());
        }
      }
    };
    ((IEclipsePreferences) preferencesLookup[PREF_INSTANCE].parent()).addNodeChangeListener(instanceNodeListener);
    preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(instancePreferencesListener = new EclipsePreferencesListener());

    // Listen to default preferences node removal from parent in order to
    // refresh stored one
    defaultNodeListener = new IEclipsePreferences.INodeChangeListener() {
      @Override
      public void added(IEclipsePreferences.NodeChangeEvent event) {
        // do nothing
      }

      @Override
      public void removed(IEclipsePreferences.NodeChangeEvent event) {
        if (event.getChild() == DartModelManager.this.preferencesLookup[PREF_DEFAULT]) {
          DartModelManager.this.preferencesLookup[PREF_DEFAULT] = getDefaultScope().getNode(
              DartCore.PLUGIN_ID);
        }
      }
    };
    ((IEclipsePreferences) preferencesLookup[PREF_DEFAULT].parent()).addNodeChangeListener(defaultNodeListener);
  }

  /**
   * Return <code>true</code> if the given resource should be analyzed. All resources are to be
   * analyzed unless they have been excluded.
   * 
   * @param resource the resource being tested
   * @return <code>true</code> if the given resource should be analyzed
   */
  public boolean isAnalyzed(IResource resource) {
    if (!resource.exists()) {
      return false;
    }
    // TODO(brianwilkerson) Re-implement this once the real semantics have been decided on.
    ArrayList<String> patterns = getExclusionPatterns();
    if (patterns.size() > 0) {
      String path = resource.getLocation().toPortableString();
      for (String pattern : patterns) {
        // TODO(brianwilkerson) Replace this with some form of pattern matching.
        if (path.equals(pattern) || path.startsWith(pattern + "/")) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * If the given file defines a library, open the library and return it. If the library was already
   * open, then this method has no effect but returns the existing library. If the file does not
   * define a library, then look for a library in the same directory as the file or in a parent of
   * that directory that references the file. If such a library can be found, then open that library
   * and return it. Otherwise return <code>null</code>.
   * 
   * @param libraryFile the file defining the library to be opened
   * @param monitor the progress monitor used to provide feedback to the user, or <code>null</code>
   *          if no feedback is desired
   * @return the library defined by the given file
   * @throws DartModelException if the library exists but could not be opened for some reason
   */
  public DartLibrary openLibrary(File libraryFile, IProgressMonitor monitor)
      throws DartModelException {
    if (libraryFile == null || !libraryFile.exists()) {
      return null;
    }
    //
    // Check whether the library that contains the file is already open, and if so return it.
    //
    IResource[] resources = ResourceUtil.getResources(libraryFile);
    if (resources != null && resources.length > 0) {
      DartProject project = DartCore.create(resources[0].getProject());
      if (project != null) {
        DartLibrary[] libraries = project.getDartLibraries();
        if (libraries != null && libraries.length > 0) {
          return libraries[0];
        }
      }
    }
    //
    // Check whether the file defines a library, and if so create the structure for it.
    //
    DartUnit libraryUnit = parseLibraryFile(libraryFile);
    if (libraryUnit != null) {
      return createLibraryProject(libraryFile, libraryUnit, monitor);
    }
    //
    // Otherwise, check whether we can find a file that defines a library that references the file,
    // and if so create the structure for that file.
    //
    LibrarySearchResult result = findFirstLibraryContaining(libraryFile);
    if (result != null) {
      return createLibraryProject(result.libraryFile, result.libraryUnit, monitor);
    }
    return null;
  }

  /**
   * Return the information associated with the given element. Using this method to access the
   * information will <b>not</b> cause it to be marked as being used for purposes of keeping it in
   * the cache.
   * 
   * @param element the element associated with the information to be returned
   * @return the information associated with the given element
   */
  public DartElementInfo peekAtInfo(DartElement element) {
    HashMap<DartElement, DartElementInfo> tempCache = temporaryCache.get();
    if (tempCache != null) {
      DartElementInfo result = tempCache.get(element);
      if (result != null) {
        return result;
      }
    }
    synchronized (infoCache) {
      return infoCache.peekAtInfo(element);
    }
  }

  /**
   * Remove the given listener from the list of objects that are listening for changes to Dart
   * elements. Has no affect if an identical listener is not registered.
   * 
   * @param listener the listener to be removed
   */
  public void removeElementChangedListener(ElementChangedListener listener) {
    deltaState.removeElementChangedListener(listener);
  }

  /**
   * Remove the given resource from the set of ignored resources.
   * 
   * @param resource the resource to (un)ignore
   * @throws IOException if there was an error accessing the ignore file
   */
  public void removeFromIgnores(IResource resource) throws IOException {
    DartIgnoreManager.getInstance().removeFromIgnores(resource);
  }

  /**
   * Remove any information cached for the given element or any children of the element.
   * 
   * @param element the element associated with the information to be removed
   * @throws DartModelException if the information cannot be deleted
   */
  public DartElementInfo removeInfoAndChildren(DartElementImpl element) throws DartModelException {
    synchronized (infoCache) {
      DartElementInfo info = infoCache.peekAtInfo(element);
      if (info != null) {
        element.closing(info);
        closeChildren(info);
        infoCache.removeInfo(element);
      }
      return info;
    }
  }

  /**
   * Remove any information cached for any library in the specified directory hierarchy and any
   * children
   * 
   * @param uri the URI of the directory containing the libraries to be closed.
   */
  public void removeLibraryInfoAndChildren(URI uri) {
    synchronized (infoCache) {
      DartLibraryImpl[] libraries = infoCache.getLibrariesWithPrefix(uri);
      for (DartLibraryImpl element : libraries) {
        try {
          removeInfoAndChildren(element);
        } catch (DartModelException e) {
          DartCore.logError("Failed to close library: " + element.getElementName(), e); //$NON-NLS-1$
          continue;
        }
      }
    }
  }

  public void removePerProjectInfo(DartProjectImpl dartProject, boolean removeExtJarInfo) {
    synchronized (perProjectInfos) {
      // use the perProjectInfo collection as its own lock
      IProject project = dartProject.getProject();
      PerProjectInfo info = perProjectInfos.get(project);
      if (info != null) {
        perProjectInfos.remove(project);
        if (removeExtJarInfo) {
          info.forgetExternalTimestampsAndIndexes();
        }
      }
    }
    DartCore.notYetImplemented();
    // resetClasspathListCache();
  }

  /**
   * Reset project options stored in info cache.
   */
  public void resetProjectOptions(DartProjectImpl dartProject) {
    synchronized (perProjectInfos) {
      // use the perProjectInfo collection as its own lock
      IProject project = dartProject.getProject();
      PerProjectInfo info = perProjectInfos.get(project);
      if (info != null) {
        info.setOptions(null);
      }
    }
  }

  /**
   * Reset project preferences stored in info cache.
   */
  public void resetProjectPreferences(DartProjectImpl dartProject) {
    synchronized (perProjectInfos) {
      // use the perProjectInfo collection as its own lock
      IProject project = dartProject.getProject();
      PerProjectInfo info = perProjectInfos.get(project);
      if (info != null) {
        info.setPreferences(null);
      }
    }
  }

  /**
   * Reset the temporary cache for newly created elements to <code>null</code>.
   */
  public void resetTemporaryCache() {
    temporaryCache.set(null);
  }

  /**
   * Sets the current table of options. All and only the options explicitly included in the given
   * table are remembered; all previous option settings are forgotten, including ones not explicitly
   * mentioned.
   * <p>
   * Helper constants have been defined on DartPreferenceConstants for each of the option IDs
   * (categorized in Code assist option ID, Compiler option ID and Core option ID) and some of their
   * acceptable values (categorized in Option value). Some options accept open value sets beyond the
   * documented constant values.
   * <p>
   * Note: each release may add new options.
   * 
   * @param newOptions the new options, or <code>null</code> to reset all options to their default
   *          values
   */
  public void setOptions(Hashtable<String, String> newOptions) {
    DartCore.notYetImplemented();
  }

  /**
   * Return the set of elements which are out of synch with their buffers.
   * 
   * @return the set of elements which are out of synch with their buffers
   */
  protected HashSet<OpenableElementImpl> getElementsOutOfSynchWithBuffers() {
    return elementsOutOfSynchWithBuffers;
  }

  /*
   * Puts the infos in the given map (keys are DartElements and values are DartElementInfos) in the
   * Dart model cache in an atomic way.
   */
  protected synchronized void putInfos(DartElement openedElement,
      Map<DartElement, DartElementInfo> newElements) {
    synchronized (infoCache) {
      // Remove existing children; they are replaced with the new children contained in newElements.
      DartElementInfo existingInfo = infoCache.peekAtInfo(openedElement);
      closeChildren(existingInfo);
      // Add the new children.
      for (Map.Entry<DartElement, DartElementInfo> entry : newElements.entrySet()) {
        infoCache.putInfo(entry.getKey(), entry.getValue());
      }
      if (openedElement instanceof CompilationUnitImpl) {
        CompilationUnitImpl cu = (CompilationUnitImpl) openedElement;
        DartLibrary lib = cu.getLibrary();
        try {
          if (lib.getDefiningCompilationUnit() == cu) {
            // redefining library
            DartCompilerUtilities.removeCachedLibrary(((DartLibraryImpl) lib).getLibrarySourceFile());
          }
        } catch (DartModelException ex) {
          // ignore it
        }
      }
    }
  }

  private void closeChildren(DartElementInfo info) {
    if (info != null) {
      DartElement[] children = info.getChildren();
      for (int i = 0, size = children.length; i < size; ++i) {
        DartElementImpl child = (DartElementImpl) children[i];
        try {
          child.close();
        } catch (DartModelException e) {
          // ignore
        }
      }
    }
  }

  /**
   * Return <code>true</code> if the given library unit contains either a source or a resource
   * directive that references the target URI.
   * 
   * @param libraryUnit the library unit containing the directives to be searched
   * @param sourceUri the URI of the file containing the library unit
   * @param targetUri the URI of the file that might be referenced by the URI
   * @return <code>true</code> if the library unit references the file
   */
  private boolean containsReference(DartUnit libraryUnit, URI sourceUri, URI targetUri) {
    List<DartDirective> directives = libraryUnit.getDirectives();
    if (directives == null) {
      return false;
    }
    for (DartDirective directive : directives) {
      if (directive instanceof DartSourceDirective) {
        DartSourceDirective source = (DartSourceDirective) directive;
        if (isReference(source.getSourceUri().getValue(), sourceUri, targetUri)) {
          return true;
        }
      } else if (directive instanceof DartResourceDirective) {
        DartResourceDirective resource = (DartResourceDirective) directive;
        if (isReference(resource.getResourceUri().getValue(), sourceUri, targetUri)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Given a file that defines a library that is not yet included in the model, create a project to
   * contain the library, link in the files associated with the library, create the model structure,
   * and return the library defined by the file.
   * 
   * @param libraryFile the file defining the library whose project is to be created
   * @param libraryUnit the result of parsing the library file
   * @param monitor the progress monitor used to provide feedback to the user, or <code>null</code>
   *          if no feedback is desired
   * @return the library defined by the file
   * @throws DartModelException if the project could not be created or populated
   */
  private DartLibrary createLibraryProject(File libraryFile, DartUnit libraryUnit,
      IProgressMonitor monitor) throws DartModelException {
    try {
      //
      // Otherwise, the file defines a library that is not yet represented, so start by creating a
      // project for the library.
      //
      IProject newProject = createProjectForLibrary(libraryUnit, monitor);
      //
      // Create links for all of the files that are part of the library.
      //
      for (File file : getFilesForLibrary(libraryFile, libraryUnit)) {
        if (file.exists()) {
          IProjectUtilities.addLinkToProject(newProject.getProject(), file, monitor);
        }
      }
      //
      // Record the path to the library file so we don't have to parse every .dart file to identify
      // the library associated with the project.
      //
      List<String> paths = new ArrayList<String>(1);
      IResource[] resources = ResourceUtil.getResources(libraryFile);
      if (resources == null || resources.length != 1) {
        throw new DartModelException(new DartModelStatusImpl(IStatus.OK,
            "Too many files representing the library file " + libraryFile.getAbsolutePath()));
      }
      paths.add(resources[0].getProjectRelativePath().toPortableString());
      DartProjectImpl newDartProject = create(newProject);
      // TODO(brianwilkerson) Re-work this so that the info object can be saved in order to avoid
      // having to read the list from disk just after writing it.
      DartProjectInfo projectInfo = (DartProjectInfo) getInfo(newDartProject);
      if (projectInfo == null) {
        projectInfo = new DartProjectInfo();
      }
      newDartProject.setChildPaths(projectInfo, paths);
      //
      // And let the project create the unique library associated with it. (We have to remove the
      // cached info for the project because it has the wrong structure at this point.)
      //
      removeInfoAndChildren(newDartProject);
      DartLibrary[] libraries = newDartProject.getDartLibraries();
      if (libraries == null || libraries.length <= 0) {
        throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
            "No libraries found while opening a new project: " + newProject.getLocation()));
      }
      //
      // Now that the library's project fully exists, make sure that all of the imported
      // libraries exist as projects. This avoids a race condition in which building the library's
      // structure on two different threads causes us to create the same project on two different
      // threads, resulting in errors.
      //
//      for (File file : getImportedLibraryFiles(libraryFile, libraryUnit)) {
//        openLibrary(file, monitor);
//      }
      // 
      // After the resources have been created, start analyzing the library
      //
      return libraries[0];
    } catch (CoreException exception) {
      throw new DartModelException(exception);
    }
  }

  /**
   * Create the project that should contain the library defined by the given library unit.
   * 
   * @param libraryUnit the library unit defining the library to be contained by the created project
   * @param monitor the progress monitor used to provide feedback
   * @return the project that was created
   * @throws CoreException if the project could not be created for some reason
   */
  private IProject createProjectForLibrary(DartUnit libraryUnit, IProgressMonitor monitor)
      throws CoreException {
    DartProjectGenerator generator = new DartProjectGenerator();
    generator.setName(getLibraryName(libraryUnit));
    generator.execute(monitor);
    return generator.getProject();
  }

  /**
   * Return the model element that is a child of the given element that corresponds to the given
   * resource, or <code>null</code> if there is no such element.
   * 
   * @param parent the element whose children are to be searched
   * @param resource the resource corresponding to the element to be returned
   * @return the child of the given element corresponding to the given resource
   */
  private DartElementImpl findChild(DartElementImpl parent, IResource resource) {
    if (parent == null) {
      return null;
    }
    try {
      for (DartElement child : parent.getChildren()) {
        if (resource.equals(child.getCorrespondingResource())) {
          return (DartElementImpl) child;
        }
      }
    } catch (DartModelException exception) {
      // Fall through to return null
    }
    return null;
  }

  /**
   * Starting in the directory containing the given file and proceeding up the chain of parent
   * directories, search for a library file that contains a reference to the given file. Return the
   * first such library file that is found. Return <code>null</code> if no library file is found.
   * 
   * @param file the file referenced by the library file that is returned
   * @return the library file that references the given file
   */
  private LibrarySearchResult findFirstLibraryContaining(File file) {
    if (file == null) {
      return null;
    }
    URI targetUri = file.toURI();
    File dir = file.getParentFile();
    do {
      URI sourceUri = dir.toURI();
      for (File child : dir.listFiles()) {
        DartUnit libraryUnit = parseLibraryFile(child);
        if (libraryUnit != null) {
          if (containsReference(libraryUnit, sourceUri, targetUri)) {
            return new LibrarySearchResult(child, libraryUnit);
          }
        }
      }
      dir = dir.getParentFile();
    } while (dir != null);
    return null;
  }

  /**
   * Return the base name used to compose the project name that should be used when creating a
   * project for the library defined by the given library unit.
   * 
   * @param libraryUnit the library unit defining the library for which a project is to be created
   * @return the base of the project name that should be used for the project to be created
   */
  private String getBaseLibraryName(DartUnit libraryUnit) {
    List<DartDirective> directives = libraryUnit.getDirectives();
    if (directives != null) {
      for (DartDirective directive : directives) {
        if (directive instanceof DartLibraryDirective) {
          DartStringLiteral nameLiteral = ((DartLibraryDirective) directive).getName();
          if (nameLiteral != null) {
            String derivedName = FileUtilities.deriveFileName(nameLiteral.getValue());
            if (derivedName != null && derivedName.length() > 0) {
              return derivedName;
            }
          }
        }
      }
    }
    String fileName = libraryUnit.getSourceName();
    if (fileName.endsWith(Extensions.DOT_DART)) {
      return fileName.substring(0, fileName.length() - Extensions.DOT_DART.length());
    }
    return fileName;
  }

  // Do not modify without modifying getDefaultOptions()
  private HashMap<String, String> getDefaultOptionsNoInitialization() {
    DartCore.notYetImplemented();
    // Map defaultOptionsMap = new CompilerOptions().getMap(); // compiler
    // defaults
    Map<String, String> defaultOptionsMap = new HashMap<String, String>();

    // // Override some compiler defaults
    // defaultOptionsMap.put(DartPreferenceConstants.COMPILER_LOCAL_VARIABLE_ATTR,
    // DartPreferenceConstants.GENERATE);
    // defaultOptionsMap.put(DartPreferenceConstants.COMPILER_CODEGEN_UNUSED_LOCAL,
    // DartPreferenceConstants.PRESERVE);
    // defaultOptionsMap.put(DartPreferenceConstants.COMPILER_TASK_TAGS,
    // DartPreferenceConstants.DEFAULT_TASK_TAGS);
    // defaultOptionsMap.put(DartPreferenceConstants.COMPILER_TASK_PRIORITIES,
    // DartPreferenceConstants.DEFAULT_TASK_PRIORITIES);
    // defaultOptionsMap.put(DartPreferenceConstants.COMPILER_TASK_CASE_SENSITIVE,
    // DartPreferenceConstants.ENABLED);
    // defaultOptionsMap.put(DartPreferenceConstants.COMPILER_DOC_COMMENT_SUPPORT,
    // DartPreferenceConstants.ENABLED);
    // defaultOptionsMap.put(DartPreferenceConstants.COMPILER_PB_FORBIDDEN_REFERENCE,
    // DartPreferenceConstants.ERROR);
    //
    // // Builder settings
    //    defaultOptionsMap.put(DartPreferenceConstants.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, ""); //$NON-NLS-1$
    // defaultOptionsMap.put(DartPreferenceConstants.CORE_JAVA_BUILD_INVALID_CLASSPATH,
    // DartPreferenceConstants.ABORT);
    // defaultOptionsMap.put(DartPreferenceConstants.CORE_JAVA_BUILD_DUPLICATE_RESOURCE,
    // DartPreferenceConstants.WARNING);
    // defaultOptionsMap.put(DartPreferenceConstants.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER,
    // DartPreferenceConstants.CLEAN);
    //
    // // DartCore settings
    // defaultOptionsMap.put(DartPreferenceConstants.CORE_JAVA_BUILD_ORDER,
    // DartPreferenceConstants.IGNORE);
    // defaultOptionsMap.put(DartPreferenceConstants.CORE_INCOMPLETE_CLASSPATH,
    // DartPreferenceConstants.ERROR);
    // defaultOptionsMap.put(DartPreferenceConstants.CORE_CIRCULAR_CLASSPATH,
    // DartPreferenceConstants.ERROR);
    // defaultOptionsMap.put(DartPreferenceConstants.CORE_INCOMPATIBLE_JDK_LEVEL,
    // DartPreferenceConstants.IGNORE);
    // defaultOptionsMap.put(DartPreferenceConstants.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS,
    // DartPreferenceConstants.ENABLED);
    // defaultOptionsMap.put(DartPreferenceConstants.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS,
    // DartPreferenceConstants.ENABLED);

    // Formatter settings
    defaultOptionsMap.putAll(DefaultCodeFormatterConstants.getEclipseDefaultSettings());

    // CodeAssist settings
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_VISIBILITY_CHECK,
        DartPreferenceConstants.DISABLED);
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_DEPRECATION_CHECK,
        DartPreferenceConstants.DISABLED);
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_IMPLICIT_QUALIFICATION,
        DartPreferenceConstants.DISABLED);
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_FIELD_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_STATIC_FIELD_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_STATIC_FINAL_FIELD_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_LOCAL_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_ARGUMENT_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_FIELD_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_STATIC_FIELD_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_STATIC_FINAL_FIELD_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_LOCAL_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_ARGUMENT_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_FORBIDDEN_REFERENCE_CHECK,
        DartPreferenceConstants.ENABLED);
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_DISCOURAGED_REFERENCE_CHECK,
        DartPreferenceConstants.DISABLED);
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_CAMEL_CASE_MATCH,
        DartPreferenceConstants.ENABLED);
//    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_SUGGEST_STATIC_IMPORTS,
//        DartPreferenceConstants.ENABLED);

    // // Time out for parameter names
    //    defaultOptionsMap.put(DartPreferenceConstants.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC, "50"); //$NON-NLS-1$

    return new HashMap<String, String>(defaultOptionsMap);
  }

  private IScopeContext getDefaultScope() {
    return new DefaultScope();
  }

  /**
   * Return a list of exclusion patterns that are to be applied to determine which files are not
   * currently being analyzed.
   * 
   * @return the exclusion patterns used to determine which files are not currently being analyzed
   */
  private ArrayList<String> getExclusionPatterns() {
    return DartIgnoreManager.getInstance().getExclusionPatterns();
  }

  private Set<File> getFilesForLibrary(File libraryFile, DartUnit libraryUnit) {
    Set<File> files = new HashSet<File>();
    if (libraryFile == null || libraryUnit == null) {
      return files;
    }
    files.add(libraryFile);
    File libDirectory = libraryFile.getParentFile();
    URI libraryUri = libDirectory.toURI();
    List<DartDirective> directives = libraryUnit.getDirectives();
    if (directives != null) {
      for (DartDirective directive : directives) {
        try {
          URI uri = null;
          if (directive instanceof DartSourceDirective) {
            DartStringLiteral literal = ((DartSourceDirective) directive).getSourceUri();
            if (literal != null) {
              uri = new URI(literal.getValue());
            }
          } else if (directive instanceof DartResourceDirective) {
            DartStringLiteral literal = ((DartResourceDirective) directive).getResourceUri();
            if (literal != null) {
              uri = new URI(literal.getValue());
            }
          }
          if (uri != null) {
            uri = URIUtil.makeAbsolute(uri, libraryUri);
            files.add(new File(uri));
          }
        } catch (URISyntaxException exception) {
          // If we cannot get the URI for the source or resource, ignore it.
        }
      }
    }
    // add html files
    if (libDirectory.exists() && libDirectory.isDirectory()) {
      File[] htmlFiles = libDirectory.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return DartCore.isHTMLLikeFileName(name);
        }
      });

      if (htmlFiles != null) {
        String libraryName = libraryFile.getName();
        for (File htmlFile : htmlFiles) {
          List<String> libraryNames;
          try {
            libraryNames = LibraryReferenceFinder.findInHTML(FileUtilities.getContents(htmlFile));
            for (String name : libraryNames) {
              if (name.equalsIgnoreCase(libraryName)) {
                files.add(htmlFile);
                break;
              }
            }
          } catch (IOException exception) {
            DartCore.logInformation("Could not read \"" + htmlFile.getAbsolutePath()
                + "\" to find references to \"" + libraryFile.getAbsolutePath() + "\"", exception);
          }
        }
      }
    }
    return files;
  }

  private Set<File> getImportedLibraryFiles(File libraryFile, DartUnit libraryUnit) {
    Set<File> files = new HashSet<File>();
    URI libraryUri = libraryFile.getParentFile().toURI();
    List<DartDirective> directives = libraryUnit.getDirectives();
    if (directives != null) {
      for (DartDirective directive : directives) {
        try {
          if (directive instanceof DartImportDirective) {
            URI uri = new URI(((DartImportDirective) directive).getLibraryUri().getValue());
            if (uri != null && !SystemLibraryManager.isDartUri(uri)) {
              uri = URIUtil.makeAbsolute(uri, libraryUri);
              files.add(new File(uri));
            }
          }
        } catch (URISyntaxException exception) {
          // If we cannot get the URI for the library, ignore it.
        }
      }
    }
    return files;
  }

  private IScopeContext getInstanceScope() {
    return new InstanceScope();
  }

  /**
   * Return the project name that should be used when creating a project for the library defined by
   * the given library unit.
   * 
   * @param libraryUnit the library unit defining the library for which a project is to be created
   * @return the project name that should be used for the project to be created
   */
  private String getLibraryName(DartUnit libraryUnit) {
    String baseName = getBaseLibraryName(libraryUnit);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = root.getProject(baseName);
    int index = 2;
    while (project.exists()) {
      String name = baseName + index++;
      project = root.getProject(name);
    }
    return project.getName();
  }

  /**
   * Return <code>true</code> if the given URI, when interpreted relative to the source URI, is a
   * reference to the file with the given target URI.
   * 
   * @param uri the URI being tested
   * @param sourceUri the URI that the given URI is relative to
   * @param targetUri the URI of the file that might be referenced by the URI
   * @return <code>true</code> if the given URI is a reference to the given file
   */
  private boolean isReference(String uri, URI sourceUri, URI targetUri) {
    try {
      URI relativeUri = new URI(uri);
      URI absoluteUri = URIUtil.makeAbsolute(relativeUri, sourceUri);
      return URIUtil.sameURI(absoluteUri, targetUri);
    } catch (URISyntaxException exception) {
      // Fall through to return false
    }
    return false;
  }

  /**
   * If the given file contains a library directive, return the AST structure for the content of the
   * file, otherwise return <code>null</code>.
   * 
   * @param file the file being tested
   * @return the AST structure for the library
   */
  private DartUnit parseLibraryFile(File file) {
    if (!Extensions.isDartFile(file)) {
      return null;
    }
    try {

      DartUnit unit = DartCompilerUtilities.parseSource(file.getName(),
          FileUtilities.getContents(file), null);
      List<DartDirective> directives = unit.getDirectives();
      if (directives != null && directives.size() > 0) {
        return unit;
      }
      // check if there is a main method
      List<DartNode> topLevelNodes = unit.getTopLevelNodes();
      for (DartNode node : topLevelNodes) {
        if (node instanceof DartMethodDefinition) {
          DartExpression functionName = ((DartMethodDefinition) node).getName();
          if (((DartIdentifier) functionName).getName().equals(DartFunction.MAIN)) {
            return unit;
          }
        }
      }

    } catch (Exception exception) {
      DartCore.logError("Could not parse a compilation unit to determine whether it is a library",
          exception);
      // Fall through to return null
    }
    return null;
  }

  private void shutdownImpl() {
    DartCore.notYetImplemented();
    ResourceUtil.shutdown();

    IEclipsePreferences preferences = getInstanceScope().getNode(DartCore.PLUGIN_ID);
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      DartCore.logError("Could not save DartCore preferences", e); //$NON-NLS-1$
    }
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.removeResourceChangeListener(deltaState);

    workspace.removeSaveParticipant(DartCore.PLUGIN_ID);

    // // Stop listening to content-type changes
    // Platform.getContentTypeManager().removeContentTypeChangeListener(this);

    // Stop indexing
    if (DartCoreDebug.NEW_INDEXER) {
      InMemoryIndex.getInstance().getOperationProcessor().stop(true);
      InMemoryIndex.getInstance().shutdown();
    } else {
      StandardDriver.shutdown();
    }

    // Stop listening to preferences changes
    preferences.removePreferenceChangeListener(propertyListener);
    ((IEclipsePreferences) preferencesLookup[PREF_DEFAULT].parent()).removeNodeChangeListener(defaultNodeListener);
    preferencesLookup[PREF_DEFAULT] = null;
    ((IEclipsePreferences) preferencesLookup[PREF_INSTANCE].parent()).removeNodeChangeListener(instanceNodeListener);
    preferencesLookup[PREF_INSTANCE].removePreferenceChangeListener(instancePreferencesListener);
    preferencesLookup[PREF_INSTANCE] = null;
    String resourcesPluginId = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
    getInstanceScope().getNode(resourcesPluginId).removePreferenceChangeListener(
        resourcesPropertyListener);

    // wait for the initialization job to finish
    try {
      Job.getJobManager().join(DartCore.PLUGIN_ID, null);
    } catch (InterruptedException e) {
      // ignore
    }

    // Note: no need to close the Dart model as this just removes Dart element
    // infos from the Dart model cache
  }

  /**
   * Initiate the background indexing process. This should be deferred after the plug-in activation.
   */
  private void startIndexing() {
    if (DartCoreDebug.NEW_INDEXER) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          InMemoryIndex.getInstance().getOperationProcessor().run();
        }
      }, "Index Operation Processor").start(); //$NON-NLS-0$
    } else {
      LocationPersitence lp = LocationPersitence.getInstance();
      lp.registerLocationType(CompilationUnitLocation.TYPE); // C
      lp.registerLocationType(FieldLocation.TYPE); // F
      lp.registerLocationType(FunctionLocation.TYPE); // N
      lp.registerLocationType(FunctionTypeAliasLocation.TYPE); // A
      lp.registerLocationType(MethodLocation.TYPE); // M
      lp.registerLocationType(SyntheticLocationType.getInstance()); // Z
      lp.registerLocationType(TypeLocation.TYPE); // T
      lp.registerLocationType(VariableLocation.TYPE); // V

      StandardDriver.getInstance();
      //
      // TODO(brianwilkerson) The following line is a short-term work around. The issue is that when
      // the indexer is out of sync we have no way to recognize that fact. The most common cause for
      // the indexer getting out of sync is changes to the mementos for elements.
      //
      // StandardDriver.getInstance().rebuildIndex();

      DartCore.notYetImplemented();
      // if (indexManager != null) {
      // indexManager.reset();
      // }
    }
  }

  private void startupImpl() {
    DartCore.notYetImplemented();
    try {
      // configurePluginDebugOptions();

      // initialize Dart model cache
      infoCache = new DartModelCache();

      // request state folder creation (workaround 19885)
      DartCore.getPlugin().getStateLocation();

      // Initialize eclipse preferences
      initializePreferences();

      // Listen to preference changes
      propertyListener = new IEclipsePreferences.IPreferenceChangeListener() {
        @Override
        public void preferenceChange(PreferenceChangeEvent event) {
          DartModelManager.this.optionsCache = null;
        }
      };
      getInstanceScope().getNode(DartCore.PLUGIN_ID).addPreferenceChangeListener(propertyListener);

      // listen for encoding changes (see
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=255501 )
      resourcesPropertyListener = new IEclipsePreferences.IPreferenceChangeListener() {
        @Override
        public void preferenceChange(PreferenceChangeEvent event) {
          if (ResourcesPlugin.PREF_ENCODING.equals(event.getKey())) {
            DartModelManager.this.optionsCache = null;
          }
        }
      };
      String resourcesPluginId = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
      getInstanceScope().getNode(resourcesPluginId).addPreferenceChangeListener(
          resourcesPropertyListener);

      // // Listen to content-type changes
      // Platform.getContentTypeManager().addContentTypeChangeListener(this);

      // // retrieve variable values
      // long start = -1;
      // if (VERBOSE)
      // start = System.currentTimeMillis();
      // loadVariablesAndContainers();
      // if (VERBOSE)
      //        traceVariableAndContainers("Loaded", start); //$NON-NLS-1$

      // listen for resource changes
      // deltaState.initializeRootsWithPreviousSession();
      final IWorkspace workspace = ResourcesPlugin.getWorkspace();
      workspace.addResourceChangeListener(deltaState,
      /*
       * update spec in DartCore#addPreProcessingResourceChangedListener(...) if adding more event
       * types
       */
//       IResourceChangeEvent.PRE_BUILD
//       | IResourceChangeEvent.POST_BUILD
//       |
          IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE
              | IResourceChangeEvent.PRE_CLOSE
//       | IResourceChangeEvent.PRE_REFRESH
      );

      startIndexing();

      ResourceUtil.startup();
      DartCore.notYetImplemented();
      // // process deltas since last activated in indexer thread so that
      // indexes are up-to-date.
      // // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38658
      // Job processSavedState = new Job(Messages.savedState_jobName) {
      // protected IStatus run(IProgressMonitor monitor) {
      // try {
      // // add save participant and process delta atomically
      // // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=59937
      // workspace.run(
      // new IWorkspaceRunnable() {
      // public void run(IProgressMonitor progress) throws CoreException {
      // ISavedState savedState =
      // workspace.addSaveParticipant(DartCore.PLUGIN_ID,
      // DartModelManager.this);
      // if (savedState != null) {
      // // the event type coming from the saved state is always POST_AUTO_BUILD
      // // force it to be POST_CHANGE so that the delta processor can handle it
      // DartModelManager.this.deltaState.getDeltaProcessor().overridenEventType
      // = IResourceChangeEvent.POST_CHANGE;
      // savedState.processResourceChangeEvents(DartModelManager.this.deltaState);
      // }
      // }
      // },
      // monitor);
      // } catch (CoreException e) {
      // return e.getStatus();
      // }
      // return Status.OK_STATUS;
      // }
      // };
      // processSavedState.setSystem(true);
      // processSavedState.setPriority(Job.SHORT); // process asap
      // processSavedState.schedule();
    } catch (RuntimeException e) {
      shutdown();
      throw e;
    }
  }
}
