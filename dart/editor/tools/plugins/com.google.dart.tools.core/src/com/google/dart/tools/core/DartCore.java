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
package com.google.dart.tools.core;

import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.logging.Logger;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.AnalysisServerImpl;
import com.google.dart.tools.core.analysis.AnalysisServerMock;
import com.google.dart.tools.core.analysis.index.AnalysisIndexManager;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.MessageConsoleImpl;
import com.google.dart.tools.core.internal.OptionManager;
import com.google.dart.tools.core.internal.analysis.model.ProjectManagerImpl;
import com.google.dart.tools.core.internal.builder.RootArtifactProvider;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.internal.model.DartModelImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.DartProjectImpl;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.internal.operation.BatchOperation;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.jobs.CleanLibrariesJob;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartIgnoreListener;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.core.model.DartSdkListener;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.ElementChangedListener;
import com.google.dart.tools.core.utilities.general.StringUtilities;
import com.google.dart.tools.core.utilities.performance.PerformanceManager;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * The class <code>DartCore</code> is used to access the elements modeling projects that have a Dart
 * nature.
 */
public class DartCore extends Plugin implements DartSdkListener {
  /**
   * The unique instance of this class.
   */
  private static DartCore PLUG_IN;

  /**
   * The log used by {@link #logError(String)} and other local methods for logging errors, warnings,
   * and information or <code>null</code> to use the default system log.
   */
  private static ILog PLUGIN_LOG;

  /**
   * Flag indicating whether instrumentation logging of errors is enabled
   */
  private static boolean instrumentationLogErrorEnabled = true;

  /**
   * The id of the plug-in that defines the Dart model.
   */
  public static final String PLUGIN_ID = DartCore.class.getPackage().getName();

  /**
   * The id of the project nature used for Dart projects.
   */
  public static final String DART_PROJECT_NATURE = PLUGIN_ID + ".dartNature";

  /**
   * The id of the builder used for Dart projects.
   */
  public static final String DART_BUILDER_ID = PLUGIN_ID + ".dartBuilder";

  /**
   * The id of the content type used to represent Dart compilation units.
   */
  public static final String DART_SOURCE_CONTENT_TYPE = PLUGIN_ID + ".dartSourceFile";

  /**
   * Eclipse problem marker type used to display Dart compilation errors
   */
  public static final String DART_PROBLEM_MARKER_TYPE = PLUGIN_ID + ".problem";

  /**
   * Eclipse problem marker type used to display Dart parsing errors
   */
  public static final String DART_PARSING_PROBLEM_MARKER_TYPE = PLUGIN_ID + ".parsingProblem";

  /**
   * Eclipse problem marker type used to display Dart resolution errors
   */
  public static final String DART_RESOLUTION_PROBLEM_MARKER_TYPE = PLUGIN_ID + ".resolutionProblem";

  /**
   * Extension for single unit compiled into JavaScript.
   */
  public static final String EXTENSION_JS = "js";

  /**
   * Preference for the automatically running pub
   */
  public static final String PUB_AUTO_RUN_PREFERENCE = "pubAutoRun";

  public static final String PROJECT_PREF_PACKAGE_ROOT = "projectPackageRoot";

  /**
   * Preference to control if "not a member" warnings should be reported for inferred types.
   */
  public static final String TYPE_CHECKS_FOR_INFERRED_TYPES = "typeChecksForInferredTypes";

  /**
   * Preference to control if "not a member" warnings should be reported for classes implementing
   * "noSuchMethod".
   */
  public static final String REPORT_NO_MEMBER_WHEN_HAS_INTERCEPTOR = "reportNoMemberWhenHasInterceptor";

  public static final String PROJECT_PREF_DISABLE_DART_BASED_BUILDER = "disableDartBasedBuilder";

  public static final String PROJECT_PREF_DART2JS_FLAGS = "dart2jsFlags";

  /**
   * Cached extensions for CSS files.
   */
  private static final String[] CSS_FILE_EXTENSIONS = {"css"};

  /**
   * Cached extensions for HTML files.
   */
  private static final String[] HTML_FILE_EXTENSIONS = {"html", "htm"};

  /**
   * Cached extensions for JS files.
   */
  private static final String[] JS_FILE_EXTENSIONS = {"js"};

  /**
   * Cached extensions for TXT files.
   */
  private static final String[] TXT_FILE_EXTENSIONS = {"txt"};

  /**
   * Cached extensions for files that are generated by the dartc compiler.
   */
  private static final String[] DART_GENERATED_FILE_EXTENSIONS = {"api", "deps", "js", "map"};

  /**
   * Cached extensions for image files.
   */
  public static final String[] IMAGE_FILE_EXTENSIONS = {
      "bmp", "gif", "jpeg", "jpg", "png", "raw", "thm", "tif", "tiff"};

  /**
   * Name of directory for packages installed by pub
   */
  public static final String PACKAGES_DIRECTORY_NAME = "packages";

  /**
   * Path string for packages directory
   */
  public static final String PACKAGES_DIRECTORY_PATH = File.separator + PACKAGES_DIRECTORY_NAME
      + File.separator;

  public static final String PACKAGE_SCHEME_SPEC = "package:";

  /**
   * Path string for lib directory
   */
  public static final String LIB_DIRECTORY_PATH = File.separator + "lib" + File.separator;

  /**
   * Name of pubspec file
   */
  public static final String PUBSPEC_FILE_NAME = "pubspec.yaml";

  /**
   * Name of special pubspec lock file
   */
  public static final String PUBSPEC_LOCK_FILE_NAME = "pubspec.lock";

  /**
   * The name of the build.dart special file.
   */
  public static final String BUILD_DART_FILE_NAME = "build.dart";

  /**
   * The shared message console instance.
   */
  private static final MessageConsole CONSOLE = new MessageConsoleImpl();

  /**
   * The QualifiedName for a resource remapping (a.foo ==> a.bar).
   */
  private static final QualifiedName RESOURCE_REMAP_NAME = new QualifiedName(
      PLUGIN_ID,
      "resourceRemap");

  /**
   * The QualifiedName for the package version
   */
  public static final QualifiedName PUB_PACKAGE_VERSION = new QualifiedName(
      DartCore.PLUGIN_ID,
      "pub.package.version");

  /**
   * The unique project manager used for analysis of anything in the workspace
   */
  private static ProjectManager projectManager;

  /**
   * Configures the given marker attribute map for the given Dart element. Used for markers, which
   * denote a Dart element rather than a resource.
   * 
   * @param attributes the mutable marker attribute map
   * @param element the Dart element for which the marker needs to be configured
   */
  public static void addDartElementMarkerAttributes(Map<String, String> attributes,
      DartElement element) {
    // if (element instanceof IMember)
    // element = ((IMember) element).getClassFile();
    // if (attributes != null && element != null) {
    // attributes.put(ATT_HANDLE_ID, element.getHandleIdentifier());
    // }
    notYetImplemented();
  }

  /**
   * Add the given listener to the list of objects that are listening for changes to Dart elements.
   * Has no effect if an identical listener is already registered.
   * <p>
   * This listener will only be notified during the POST_CHANGE resource change notification and any
   * reconcile operation (POST_RECONCILE). For finer control of the notification, use
   * {@link #addElementChangedListener(IElementChangedListener,int)}, which allows to specify a
   * different eventMask.
   * 
   * @param listener the listener being added
   */
  public static void addElementChangedListener(ElementChangedListener listener) {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      // TODO(devoncarew): this needs to be converted to fast-fail

    } else {
      addElementChangedListener(listener, ElementChangedEvent.POST_CHANGE
          | ElementChangedEvent.POST_RECONCILE);
    }
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
  public static void addElementChangedListener(ElementChangedListener listener, int eventMask) {
    DartModelManager.getInstance().addElementChangedListener(listener, eventMask);
  }

  /**
   * Add the given listener for dart ignore changes to the Dart Model. Has no effect if an identical
   * listener is already registered.
   * 
   * @param listener the listener to add
   */
  public static void addIgnoreListener(DartIgnoreListener listener) {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      getProjectManager().getIgnoreManager().addListener(listener);
    } else {
      DartIgnoreManager.getInstance().addListener(listener);
    }
  }

  /**
   * Add the given resource to the set of ignored resources.
   * 
   * @param resource the resource to ignore
   * @throws IOException if there was an error accessing the ignore file
   * @throws CoreException if there was an error deleting markers
   */
  public static void addToIgnores(IResource resource) throws IOException, CoreException {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      getProjectManager().getIgnoreManager().addToIgnores(resource);
    } else {
      DartModelManager.getInstance().addToIgnores(resource);
    }
  }

  /**
   * Remove any resource mapping for the given container.
   * 
   * @param resource
   */
  public static void clearResourceRemapping(IContainer container) {
    try {
      container.accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (resource instanceof IFile) {
            clearResourceRemapping((IFile) resource);

            return true;
          } else if (resource instanceof IContainer) {
            IContainer childContainer = (IContainer) resource;

            if (childContainer.getName().startsWith(".")) {
              return false;
            } else {
              return true;
            }
          } else {
            return false;
          }
        }
      });
    } catch (CoreException e) {
      DartCore.logError(e);
    }
  }

  /**
   * Remove any resource mapping for the given file.
   * 
   * @param resource
   */
  public static void clearResourceRemapping(IFile resource) {
    if (getResourceRemapping(resource) != null) {
      setResourceRemapping(resource, null);
    }
  }

  /**
   * Return true if directory contains a "packages" directory that is installed by pub
   */
  public static boolean containsPackagesDirectory(File file) {
    return new File(file, PACKAGES_DIRECTORY_NAME).isDirectory();
  }

  /**
   * Return <code>true</code> if the directory contains a "pubspec.yaml" file
   */
  public static boolean containsPubspecFile(File directory) {
    return new File(directory, PUBSPEC_FILE_NAME).isFile();
  }

  /**
   * Return the Dart element corresponding to the given file, or <code>null</code> if the given file
   * is not associated with any Dart element.
   * 
   * @param file the file corresponding to the Dart element
   * @return the Dart element corresponding to the given file
   */
  public static DartElement create(IFile file) {
    return DartModelManager.getInstance().create(file);
  }

  /**
   * Return the Dart element corresponding to the given folder, or <code>null</code> if the given
   * file is not associated with any Dart element.
   * 
   * @param folder the folder corresponding to the Dart element
   * @return the Dart element corresponding to the given folder
   */
  public static DartElement create(IFolder folder) {
    return DartModelManager.getInstance().create(folder);
  }

  /**
   * Return the Dart project corresponding to the given project. Note that no check is made to
   * ensure that the project has the Dart nature.
   * 
   * @param project the resource corresponding to the Dart project
   * @return the Dart project corresponding to the given project
   */
  public static DartProject create(IProject project) {
    return DartModelManager.getInstance().create(project);
  }

  /**
   * Return the Dart element corresponding to the given resource, or <code>null</code> if the given
   * resource is not associated with any Dart element.
   * 
   * @param resource the resource corresponding to the Dart element
   * @return the Dart element corresponding to the given resource
   */
  public static DartElement create(IResource resource) {
    return DartModelManager.getInstance().create(resource);
  }

  /**
   * Return the Dart model corresponding to the given workspace root.
   * 
   * @param project the workspace root corresponding to the model
   * @return the Dart model corresponding to the given workspace root
   */
  public static DartModel create(IWorkspaceRoot workspaceRoot) {
    if (workspaceRoot == null) {
      return null;
    }
    return DartModelManager.getInstance().getDartModel();
  }

  /**
   * Return the element represented by the given identifier, or <code>null</code> if the identifier
   * does not represent any element.
   * 
   * @param identifier the identifier used to identify a specific element in the Dart element
   *          structure
   * @return the element represented by the given identifier
   */
  public static DartElement create(String identifier) {
    return create(identifier, DefaultWorkingCopyOwner.getInstance());
  }

  /**
   * Returns the Dart model element corresponding to the given handle identifier generated by
   * <code>DartElement.getHandleIdentifier()</code>, or <code>null</code> if unable to create the
   * associated element. If the returned Dart element is a <code>CompilationUnit</code> or an
   * element inside a compilation unit, the compilation unit's owner is the given owner if such a
   * working copy exists, otherwise the compilation unit is a primary compilation unit.
   * 
   * @param identifier the given handle identifier
   * @param owner the owner of the returned compilation unit, ignored if the returned element is not
   *          a compilation unit, or an element inside a compilation unit
   * @return the Dart element corresponding to the handle identifier
   */
  public static DartElement create(String identifier, WorkingCopyOwner owner) {
    if (identifier == null) {
      return null;
    }
    if (owner == null) {
      owner = DefaultWorkingCopyOwner.getInstance();
    }
    MementoTokenizer memento = new MementoTokenizer(identifier);
    DartModelImpl model = DartModelManager.getInstance().getDartModel();
    return model.getHandleFromMemento(memento, owner);
  }

  /**
   * A factory for creating analysis server instances.
   * 
   * @return an analysis server
   */
  public static AnalysisServer createAnalysisServer() {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      return new AnalysisServerMock();
    }
    return new AnalysisServerImpl(PackageLibraryManagerProvider.getAnyLibraryManager());
  }

  /**
   * Given some directory {@link File}, return the {@link DartLibrary} in that directory, if there
   * is such a Dart library that is also already loaded in the Dart Editor workspace. Otherwise,
   * return <code>null</code>.
   * 
   * @see DartModelManager#findLibraryInDirectory
   * @param directory the file defining the library to be opened
   * @return the library found in the passed directory
   * @throws DartModelException if the library exists but could not be opened for some reason
   */
  public static DartLibrary findLibraryInDirectory(File directory) throws DartModelException {
    return DartModelManager.getInstance().findLibraryInDirectory(directory);
  }

  /**
   * Answer the application directory (a directory contains a "packages" directory and a
   * "pubspec.yaml" file) directly or indirectly containing the specified file or the file itself if
   * it is an application directory.
   * 
   * @param libFileOrDir the library file or directory
   * @return the context in which the specified library should be analyzed (not <code>null</code>)
   */
  public static File getApplicationDirectory(File file) {
    while (file != null) {
      if (isApplicationDirectory(file)) {
        return file;
      }
      file = file.getParentFile();
    }
    return null;
  }

  /**
   * Returns the day (yyyy-MM-dd) the product was built.
   */
  public static String getBuildDate() {
    return "@BUILDDATE@";
  }

  /**
   * Returns the SVN revision number as a String.
   */
  public static String getBuildId() {
    return "@REVISION@";
  }

  /**
   * Return a unique token that can be used to determine whether cached data that changes only when
   * the version of the editor changes is still valid.
   * 
   * @return a token used to determine the validity of cached data
   */
  public static String getBuildIdOrDate() {
    String buildIdOrDate = getBuildId();
    if (buildIdOrDate.startsWith("@")) {
      buildIdOrDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
    return buildIdOrDate;
  }

  /**
   * Returns the shared message console. Unlike the log ({@link DartCore#getLog()}), the console is
   * intended for communication with the end-user.
   * 
   * @return the message console
   */
  public static MessageConsole getConsole() {
    return CONSOLE;
  }

  /**
   * Return the list of known Dart-like file extensions. Dart-like extensions are defined in the
   * {@link Platform.getContentManager() content type manager} for the
   * {@link #DART_SOURCE_CONTENT_TYPE}. Note that a Dart-like extension does not include the leading
   * dot, and that the "dart" extension is always defined as a Dart-like extension.
   * 
   * @return the list of known Dart-like file extensions
   */
  public static String[] getDartLikeExtensions() {
    IContentType dartContentType = Platform.getContentTypeManager().getContentType(
        DART_SOURCE_CONTENT_TYPE);
    HashSet<String> extensionSet = new HashSet<String>();
    for (IContentType contentType : Platform.getContentTypeManager().getAllContentTypes()) {
      if (contentType.isKindOf(dartContentType)) {
        for (String extension : contentType.getFileSpecs(IContentType.FILE_EXTENSION_SPEC)) {
          extensionSet.add(extension);
        }
      }
    }
    extensionSet.remove(Extensions.DART);
    ArrayList<String> extensionList = new ArrayList<String>(extensionSet);
    extensionList.add(0, Extensions.DART);
    return extensionList.toArray(new String[extensionList.size()]);
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
  public static Hashtable<String, String> getDefaultOptions() {
    return DartModelManager.getInstance().getDefaultOptions();
  }

  /**
   * Return the workspace root default charset encoding.
   * 
   * @return the name of the default charset encoding for the workspace root
   */
  public static String getEncoding() {
    try {
      return ResourcesPlugin.getWorkspace().getRoot().getDefaultCharset();
    } catch (IllegalStateException exception) {
      // happen when there's no workspace (see bug
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=216817)
      // or when it is shutting down (see bug
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=60687)
      return System.getProperty("file.encoding"); //$NON-NLS-1$
    } catch (CoreException exception) {
      // fails silently and return plugin global encoding if core exception
      // occurs
    }
    return ResourcesPlugin.getEncoding();
  }

  /**
   * Utility method for returning one option value only. Equivalent to
   * <code>DartCore.getOptions().get(optionName)</code> Note that it may answer <code>null</code> if
   * this option does not exist.
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
  public static String getOption(String optionName) {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      return OptionManager.getInstance().getOption(optionName);
    } else {
      return DartModelManager.getInstance().getOption(optionName);
    }
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
  public static HashMap<String, String> getOptions() {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      return OptionManager.getInstance().getOptions();
    } else {
      return DartModelManager.getInstance().getOptions();
    }
  }

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static DartCore getPlugin() {
    return PLUG_IN;
  }

  /**
   * Answer the log used by {@link #logError(String)} and other local methods for logging errors,
   * warnings, and information.
   * 
   * @return the log (not <code>null</code>)
   */
  public static ILog getPluginLog() {
    return PLUGIN_LOG != null ? PLUGIN_LOG : getPlugin().getLog();
  }

  /**
   * Answer the unique project manager used for analysis of anything in the workspace.
   * 
   * @return the manager (not {@code null})
   */
  public static ProjectManager getProjectManager() {
    if (projectManager == null) {
      projectManager = new ProjectManagerImpl(
          ResourcesPlugin.getWorkspace().getRoot(),
          DartSdkManager.getManager().getNewSdk(),
          DartIgnoreManager.getInstance());
    }
    return projectManager;
  }

  /**
   * @return the mapping for the given resource, if any
   */
  public static String getResourceRemapping(IFile originalResource) {
    try {
      return originalResource.getPersistentProperty(RESOURCE_REMAP_NAME);
    } catch (CoreException e) {
      return null;
    }
  }

  /**
   * Returns the name of the directory in packages that is linked to the "lib" folder in the project
   * 
   * @param project
   * @return the name of the directory, <code>null</code> if there is no self linked packages folder
   */
  public static String getSelfLinkedPackageName(IResource resource) {
    String packageName = null;
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      try {
        PubFolder folder = DartCore.getProjectManager().getPubFolder(resource);
        packageName = folder.getPubspec().getName();
      } catch (Exception e) {
        DartCore.logError(e);
      }
    } else {
      DartProjectImpl dartProject = (DartProjectImpl) DartCore.create(resource.getProject());
      packageName = dartProject.getSelfLinkedPackageDirName();
    }
    return packageName;
  }

  /**
   * Returns the path string for the default dart directory - user.home/dart
   * 
   * @return the name of the user.home/dart directory
   */
  public static String getUserDefaultDartFolder() {
    String defaultLocation = System.getProperty("user.home"); //$NON-NLS-1$
    return defaultLocation + File.separator + "dart" + File.separator; //$NON-NLS-1$
  }

  /**
   * Returns the current value of the string-valued user-defined property with the given name.
   * Returns <code>null</code> if there is no user-defined property with the given name.
   * <p>
   * User-defined properties are defined in the <code>editor.properties</code> file located in the
   * eclipse installation directory.
   * 
   * @see DartCore#getEclipseInstallationDirectory()
   * @param name the name of the property
   * @return the string-valued property
   */
  public static String getUserDefinedProperty(String key) {

    Properties properties = new Properties();

    File installDirectory = getEclipseInstallationDirectory();
    File file = new File(installDirectory, "editor.properties");

    if (file.exists()) {
      try {
        properties.load(new FileReader(file));
      } catch (FileNotFoundException e) {
        logError(e);
      } catch (IOException e) {
        logError(e);
      }
    }

    return properties.getProperty(key);
  }

  /**
   * @return the version text for this plugin (i.e. 1.1.0)
   */
  public static String getVersion() {
    Version version = getPlugin().getBundle().getVersion();

    return version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
  }

  public static boolean is32Bit() {
    return Platform.getOSArch().indexOf("64") == -1;
  }

  /**
   * Return <code>true</code> if the given resource should be analyzed. All resources are to be
   * analyzed unless they have been excluded.
   * 
   * @param resource the resource being tested
   * @return <code>true</code> if the given resource should be analyzed
   */
  public static boolean isAnalyzed(IResource resource) {

    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      return getProjectManager().getIgnoreManager().isAnalyzed(resource);
    }

    return DartModelManager.getInstance().isAnalyzed(resource);
  }

  /**
   * Return true if directory contains a "packages" directory and a "pubspec.yaml" file
   */
  public static boolean isApplicationDirectory(File directory) {
    return containsPubspecFile(directory) && containsPackagesDirectory(directory);
  }

  /**
   * Answer {@code true} if the specified resource is a build.dart file and exists either in a
   * project or in a folder containing a pubspec file.
   * 
   * @param file the file
   * @return {@code true} if the file is a build.dart file that will be run by the builder
   */
  public static boolean isBuildDart(IFile file) {
    if (file == null || !file.getName().equals(BUILD_DART_FILE_NAME) || !file.exists()) {
      return false;
    }

    IContainer container = file.getParent();

    // Always run build.dart in a project's root.
    if (container.getType() == IResource.PROJECT) {
      return true;
    }

    return container.getFile(new Path(PUBSPEC_FILE_NAME)).exists();
  }

  /**
   * Return <code>true</code> if the given file is contained in the packages directory created by
   * pub
   * 
   * @param file the file that is to be checked
   * @return <code>true</code> if the given file is in packages
   */
  public static boolean isContainedInPackages(File file) {
    if (file.getAbsolutePath().contains(PACKAGES_DIRECTORY_NAME)) {
      return true;
    }
    return false;
  }

  /**
   * Return <code>true</code> if the given file name's extension is an CSS-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name's extension is an CSS-like extension
   */
  public static boolean isCSSLikeFileName(String fileName) {
    return isLikeFileName(fileName, CSS_FILE_EXTENSIONS);
  }

  /**
   * Return <code>true</code> if the given file name's extension is a generated Dart like extension.
   * 
   * @return <code>true</code> if the given file name's extension is a generated Dart like extension
   */
  public static boolean isDartGeneratedFile(String fileName) {
    return isLikeFileName(fileName, DART_GENERATED_FILE_EXTENSIONS);
  }

  /**
   * Return <code>true</code> if the given file name's extension is a Dart-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name's extension is a Dart-like extension
   * @see #getDartLikeExtensions()
   */
  public static boolean isDartLikeFileName(String fileName) {
    return isLikeFileName(fileName, getDartLikeExtensions());
  }

  /**
   * Return <code>true</code> if file is in the dart sdk lib directory
   * 
   * @param file
   * @return <code>true</code> if file is in dart-sdk/lib
   */
  public static boolean isDartSdkLibraryFile(File file) {
    File sdkLibrary = DartSdkManager.getManager().getSdk().getLibraryDirectory();
    File parentFile = file.getParentFile();
    while (parentFile != null) {
      if (parentFile.equals(sdkLibrary)) {
        return true;
      }
      parentFile = parentFile.getParentFile();
    }
    return false;
  }

  /**
   * Return <code>true</code> if the given file name's extension is an HTML-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name's extension is an HTML-like extension
   */
  public static boolean isHTMLLikeFileName(String fileName) {
    return isLikeFileName(fileName, HTML_FILE_EXTENSIONS);
  }

  /**
   * Return <code>true</code> if the given file name's extension is an image-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name's extension is an image-like extension
   */
  public static boolean isImageLikeFileName(String fileName) {
    return isLikeFileName(fileName, IMAGE_FILE_EXTENSIONS);
  }

  /**
   * Return <code>true</code> if the given file name's extension is an HTML-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name's extension is an HTML-like extension
   */
  public static boolean isJSLikeFileName(String fileName) {
    return isLikeFileName(fileName, JS_FILE_EXTENSIONS);
  }

  public static boolean isLinux() {
    return !isMac() && !isWindows();
  }

  public static boolean isMac() {
    // Look for the "Mac" OS name.
    return System.getProperty("os.name").toLowerCase().startsWith("mac");
  }

  /**
   * Return true if directory is one that is installed by pub
   * 
   * @param file the file to be checked
   * @return <code>true</code> if file name matches and is sibling of pubspec.yaml
   */
  public static boolean isPackagesDirectory(File file) {
    if (file.getName().equals(PACKAGES_DIRECTORY_NAME)) {
      return true;
    }
    return false;
  }

  /**
   * Return true if directory is one that is installed by pub
   * 
   * @param folder the folder to be checked
   * @return <code>true</code> if folder name matches and is sibling of pubspec.yaml
   */
  public static boolean isPackagesDirectory(IFolder folder) {
    IPath location = folder.getLocation();
    return location != null && isPackagesDirectory(location.toFile());
  }

  /**
   * Answer <code>true</code> if the string is a package spec
   */
  public static boolean isPackageSpec(String spec) {
    return spec != null && spec.startsWith(PACKAGE_SCHEME_SPEC);
  }

  /**
   * @return <code>true</code> if given {@link IResource} was installed by pub.
   */
  public static boolean isPackagesResource(IResource resource) {
    if (resource instanceof IFolder) {
      if (isPackagesDirectory((IFolder) resource)) {
        return true;
      }
    }
    if (resource != null && resource.getParent() instanceof IFolder) {
      IFolder parentFolder = (IFolder) resource.getParent();
      return isPackagesResource(parentFolder);
    }
    return false;
  }

  /**
   * @return <code>true</code> if given {@link CompilationUnit} is declared in "packages".
   */
  public static boolean isPackagesUnit(CompilationUnit unit) {
    IResource resource = unit.getResource();
    return DartCore.isPackagesResource(resource);
  }

  /**
   * Check if this URI denotes a patch file.
   */
  public static boolean isPatchfile(File file) {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      return file != null && file.getName().endsWith("_patch.dart");
    } else {
      return PackageLibraryManager.isPatchFile(file);
    }
  }

  /**
   * @return whether we're running in the context of the eclipse plugins build
   */
  public static boolean isPluginsBuild() {
    return Platform.getBundle("com.google.dart.eclipse.core") != null;
  }

  /**
   * Checks if the given linkedResource points to a file in the container. This is used to check for
   * the self link in the packages directory
   * 
   * @param resource
   * @param linkedResource
   * @return <code>true</code> if the linked resource points to a file/folder in the project
   */
  public static boolean isSelfLinkedResource(IContainer resource, IResource linkedResource) {

    try {
      String resourcePath = resource.getLocation().toFile().getCanonicalPath();
      String linkPath = linkedResource.getLocation().toFile().getCanonicalPath();
      if (linkPath.startsWith(resourcePath)) {
        return true;
      }
    } catch (IOException e) {
      return false;
    }
    return false;
  }

  /**
   * Return <code>true</code> if the given file name's extension is an HTML-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name's extension is an HTML-like extension
   */
  public static boolean isTXTLikeFileName(String fileName) {
    return isLikeFileName(fileName, TXT_FILE_EXTENSIONS);
  }

  public static boolean isWindows() {
    // Look for the "Windows" OS name.
    return System.getProperty("os.name").toLowerCase().startsWith("win");
  }

  public static boolean isWindowsXp() {
    // Look for the "Windows XP" OS name.
    return System.getProperty("os.name").toLowerCase().equals("windows xp");
  }

  /**
   * Log the given message as an error.
   * 
   * @param message an explanation of why the error occurred or what it means
   */
  public static void logError(String message) {
    logErrorImpl(message, null);
    instrumentationLogErrorImpl(message, null);
  }

  /**
   * Log the given exception as one representing an error.
   * 
   * @param message an explanation of why the error occurred or what it means
   * @param exception the exception being logged
   */
  public static void logError(String message, Throwable exception) {
    logErrorImpl(message, exception);
    instrumentationLogErrorImpl(message, exception);
  }

  /**
   * Log the given exception as one representing an error.
   * 
   * @param exception the exception being logged
   */
  public static void logError(Throwable exception) {
    logErrorImpl(exception.getMessage(), exception);
    instrumentationLogErrorImpl(null, exception);
  }

  /**
   * Log the given informational message.
   * 
   * @param message an explanation of why the error occurred or what it means
   * @param exception the exception being logged
   */
  public static void logInformation(String message) {
    logInformation(message, null);
  }

  /**
   * Log the given exception as one representing an informational message.
   * 
   * @param message an explanation of why the error occurred or what it means
   * @param exception the exception being logged
   */
  public static void logInformation(String message, Throwable exception) {

    if (DartCoreDebug.VERBOSE) {
      getPluginLog().log(new Status(Status.INFO, PLUGIN_ID, "INFO: " + message, exception));
    }

    instrumentationLogErrorImpl(message, exception);
  }

  /**
   * This method exists as a convenient marker for methods that have not yet been fully implemented.
   * It should be deleted before this product ships.
   */
  public static void notYetImplemented() {
  }

  /**
   * Check for accesses to the old model when the new analysis engine is enabled. This will fail by
   * throwing a runtime exception.
   */
  public static void oldModelCheck() {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      IllegalStateException exception = new IllegalStateException(
          "Wildly inappropriate access to the old model");

      DartCore.logError(exception);

      throw exception;
    }
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
  public static DartLibrary openLibrary(File libraryFile, IProgressMonitor monitor)
      throws DartModelException {
    return DartModelManager.getInstance().openLibrary(libraryFile, monitor);
  }

  /**
   * Removes the file extension from the given file name, if it has a Dart-like file extension.
   * Otherwise the file name itself is returned. Note this removes the dot ('.') before the
   * extension as well.
   * 
   * @param fileName the name of a file
   * @return the fileName without the Dart-like extension
   */
  public static String removeDartLikeExtension(String fileName) {
    return Util.getNameWithoutDartLikeExtension(fileName);
  }

  /**
   * Remove the given listener from the list of objects that are listening for changes to Dart
   * elements. Has no affect if an identical listener is not registered.
   * 
   * @param listener the listener to be removed
   */
  public static void removeElementChangedListener(ElementChangedListener listener) {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      // TODO(devoncarew): this needs to be converted to fast-fail

    } else {
      DartModelManager.getInstance().removeElementChangedListener(listener);
    }
  }

  /**
   * Remove the given resource (as a path) from the set of ignored resources.
   * 
   * @param resource the resource path to (un)ignore
   * @throws IOException if there was an error accessing the ignore file
   */
  public static void removeFromIgnores(IPath resource) throws IOException {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      getProjectManager().getIgnoreManager().removeFromIgnores(resource);
    } else {
      // Unsupported in the old model
    }
  }

  /**
   * Remove the given resource from the set of ignored resources.
   * 
   * @param resource the resource to (un)ignore
   * @throws IOException if there was an error accessing the ignore file
   */
  public static void removeFromIgnores(IResource resource) throws IOException {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      getProjectManager().getIgnoreManager().removeFromIgnores(resource);
    } else {
      DartModelManager.getInstance().removeFromIgnores(resource);
    }
  }

  /**
   * Remove the given listener for dart ignore changes from the Dart Model. Has no effect if an
   * identical listener is not registered.
   * 
   * @param listener the non-<code>null</code> listener to remove
   */
  public static void removeIgnoreListener(DartIgnoreListener listener) {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      getProjectManager().getIgnoreManager().addListener(listener);
    } else {
      DartIgnoreManager.getInstance().removeListener(listener);
    }
  }

  /**
   * Runs the given action as an atomic Dart model operation.
   * <p>
   * After running a method that modifies Dart elements, registered listeners receive after-the-fact
   * notification of what just transpired, in the form of a element changed event. This method
   * allows clients to call a number of methods that modify Dart elements and only have element
   * changed event notifications reported at the end of the entire batch.
   * </p>
   * <p>
   * If this method is called outside the dynamic scope of another such call, this method runs the
   * action and then reports a single element changed event describing the net effect of all changes
   * done to Dart elements by the action.
   * </p>
   * <p>
   * If this method is called in the dynamic scope of another such call, this method simply runs the
   * action.
   * </p>
   * 
   * @param action the action to perform
   * @param monitor a progress monitor, or <code>null</code> if progress reporting and cancellation
   *          are not desired
   * @exception CoreException if the operation failed.
   */
  public static void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
    run(action, ResourcesPlugin.getWorkspace().getRoot(), monitor);
  }

  /**
   * Runs the given action as an atomic Dart model operation.
   * <p>
   * After running a method that modifies Dart elements, registered listeners receive after-the-fact
   * notification of what just transpired, in the form of a element changed event. This method
   * allows clients to call a number of methods that modify Dart elements and only have element
   * changed event notifications reported at the end of the entire batch.
   * </p>
   * <p>
   * If this method is called outside the dynamic scope of another such call, this method runs the
   * action and then reports a single element changed event describing the net effect of all changes
   * done to Dart elements by the action.
   * </p>
   * <p>
   * If this method is called in the dynamic scope of another such call, this method simply runs the
   * action.
   * </p>
   * <p>
   * The supplied scheduling rule is used to determine whether this operation can be run
   * simultaneously with workspace changes in other threads. See <code>IWorkspace.run(...)</code>
   * for more details.
   * </p>
   * 
   * @param action the action to perform
   * @param rule the scheduling rule to use when running this operation, or <code>null</code> if
   *          there are no scheduling restrictions for this operation.
   * @param monitor a progress monitor, or <code>null</code> if progress reporting and cancellation
   *          are not desired
   * @exception CoreException if the operation failed.
   */
  public static void run(IWorkspaceRunnable action, ISchedulingRule rule, IProgressMonitor monitor)
      throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    if (workspace.isTreeLocked()) {
      new BatchOperation(action).run(monitor);
    } else {
      // use IWorkspace.run(...) to ensure that a build will be done in
// autobuild mode
      workspace.run(new BatchOperation(action), rule, IWorkspace.AVOID_UPDATE, monitor);
    }
  }

  public static void setOptions(HashMap<String, String> newOptions) {
    DartModelManager.getInstance().setOptions(new Hashtable<String, String>(newOptions));
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
  public static void setOptions(Hashtable<String, String> newOptions) {
    DartModelManager.getInstance().setOptions(newOptions);
  }

  /**
   * TESTING ONLY: Set the log used by {@link #logError(String)} and other local methods for logging
   * errors, warnings, and information.
   * 
   * @param log the log or <code>null</code> to use the default system log
   * @return the log prior to calling this method or <code>null</code> for the default system log
   */
  public static ILog setPluginLog(ILog log) {
    ILog oldLog = PLUGIN_LOG;
    PLUGIN_LOG = log;
    return oldLog;
  }

  /**
   * Set a symbolic resource mapping from one resource to another. For some uses of the original
   * resource, like serving web content, the mapped resource should be substituted.
   * 
   * @param originalResource
   * @param newResource
   */
  public static void setResourceRemapping(IFile originalResource, IFile newResource) {
    try {
      String resourcePath = (newResource == null ? null
          : newResource.getFullPath().toPortableString());
      originalResource.setPersistentProperty(RESOURCE_REMAP_NAME, resourcePath);
    } catch (CoreException e) {
      DartCore.logError(e);
    }
  }

  /**
   * Sets the value of the string-valued user-defined property for the given key.
   * <p>
   * User-defined properties are defined in the <code>editor.properties</code> file located in the
   * eclipse installation directory.
   * 
   * @see DartCore#getEclipseInstallationDirectory()
   * @param key the name of the property
   * @param value the string-valued property
   */
  public static void setUserDefinedProperty(String key, String value) {

    Properties properties = new Properties();

    File installDirectory = getEclipseInstallationDirectory();
    File file = new File(installDirectory, "editor.properties");

    try {
      if (!file.exists()) {
        file.createNewFile();
      }
      properties.load(new FileReader(file));
      properties.setProperty(key, value);
      properties.store(new FileWriter(file), null);
    } catch (FileNotFoundException e) {
      logError(e);
    } catch (IOException e) {
      logError(e);
    }

  }

  private static File getEclipseInstallationDirectory() {
    return new File(Platform.getInstallLocation().getURL().getFile());
  }

  private static void instrumentationLogErrorImpl(String message, Throwable exception) {
    if (instrumentationLogErrorEnabled) {

      InstrumentationBuilder instrumentation = Instrumentation.builder("DartCore.LogError");
      try {
        instrumentation.data("Message", message != null ? message : "null");
        instrumentation.data("Exception", exception != null ? exception.toString() : "null");

        if (exception != null) {
          final PrintWriter printWriter = new PrintWriter(new StringWriter());
          exception.printStackTrace(printWriter);
          instrumentation.data("StackTrace", printWriter.toString());
        }

      } catch (Exception e) {
        instrumentationLogErrorEnabled = false;
        logErrorImpl("Instrumentation failed to log error", exception);
      } finally {
        instrumentation.log();
      }
    }
  }

  /**
   * Return <code>true</code> if the given file name's extension matches one of the passed
   * extensions.
   * 
   * @param fileName the file name being tested
   * @param extensions an array of file extensions to test against
   * @return <code>true</code> if the given file name's extension matches one of the passed
   *         extensions
   */
  private static boolean isLikeFileName(String fileName, String[] extensions) {
    if (fileName == null || fileName.length() == 0) {
      return false;
    }
    for (String extension : extensions) {
      if (StringUtilities.endsWithIgnoreCase(fileName, '.' + extension)) {
        return true;
      }
    }
    return false;
  }

  private static void logErrorImpl(String message, Throwable exception) {
    getPluginLog().log(new Status(Status.ERROR, PLUGIN_ID, message, exception));
  }

  private IEclipsePreferences prefs;

  /**
   * Initialize a newly created instance of this class.
   */
  public DartCore() {
    PLUG_IN = this;
  }

  /**
   * Use dart2js if the SDK is present.
   */
  public boolean getCompileWithDart2JS() {
    return DartSdkManager.getManager().hasSdk();
  }

  public String getDart2jsFlags(IProject project) {
    return getProjectPreferences(project).get(PROJECT_PREF_DART2JS_FLAGS, null);
  }

  public boolean getDisableDartBasedBuilder(IProject project) {
    return getProjectPreferences(project).getBoolean(PROJECT_PREF_DISABLE_DART_BASED_BUILDER, false);
  }

  /**
   * Given an File, return the appropriate java.io.File representing a package root. This can be
   * null if a package root should not be used.
   * 
   * @param file the File
   * @return the File for the package root, or null if none
   */
  public File getPackageRoot(File file) {
    IResource resource = ResourceUtil.getResource(file);
    if (resource != null) {
      return getPackageRoot(resource.getProject());
    }
    return null;
  }

  /**
   * Given an IProject, return the appropriate java.io.File representing a package root. This can be
   * null if a package root should not be used.
   * 
   * @param project the IProject
   * @return the File for the package root, or null if none
   */
  public File getPackageRoot(IProject project) {
    if (project != null) {
      String setting = getProjectPreferences(project).get(PROJECT_PREF_PACKAGE_ROOT, "");

      if (setting != null && setting.length() > 0) {
        return new File(setting);
      }
    }

    File[] roots = CmdLineOptions.getOptions().getPackageRoots();

    if (roots.length > 0) {
      return roots[0];
    }

    return null;
  }

  public IEclipsePreferences getPrefs() {
    if (prefs == null) {
      prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
    }

    return prefs;
  }

  public IEclipsePreferences getProjectPreferences(IProject project) {
    ProjectScope projectScope = new ProjectScope(project);

    return projectScope.getNode(PLUGIN_ID);
  }

  public boolean isAutoRunPubEnabled() {
    return DartCore.getPlugin().getPrefs().getBoolean(PUB_AUTO_RUN_PREFERENCE, true);
  }

  @Override
  public void sdkUpdated(DartSdk sdk) {
    Job job = new CleanLibrariesJob(true);

    job.schedule();
  }

  public void setDart2jsFlags(IProject project, String flags) throws CoreException {
    try {
      IEclipsePreferences prefs = getProjectPreferences(project);
      prefs.put(PROJECT_PREF_DART2JS_FLAGS, flags);
      prefs.flush();
    } catch (BackingStoreException ex) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, ex.toString(), ex));
    }
  }

  public void setDisableDartBasedBuilder(IProject project, boolean value) throws CoreException {
    try {
      IEclipsePreferences prefs = getProjectPreferences(project);
      prefs.putBoolean(PROJECT_PREF_DISABLE_DART_BASED_BUILDER, value);
      prefs.flush();
    } catch (BackingStoreException ex) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, ex.toString(), ex));
    }
  }

  public void setPackageRoot(IProject project, String packageRootPath) throws CoreException {
    try {
      IEclipsePreferences prefs = getProjectPreferences(project);
      prefs.put(PROJECT_PREF_PACKAGE_ROOT, packageRootPath);
      prefs.flush();
    } catch (BackingStoreException ex) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, ex.toString(), ex));
    }
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    CmdLineOptions.setOptions(CmdLineOptions.parseCmdLine(Platform.getApplicationArgs()));
    CmdLineOptions.getOptions().printWarnings();
    AnalysisEngine.getInstance().setLogger(new Logger() {
      @Override
      public void logError(String message) {
        DartCore.logError(message);
      }

      @Override
      public void logError(String message, Throwable exception) {
        DartCore.logError(message, exception);
      }

      @Override
      public void logError(Throwable exception) {
        DartCore.logError(exception);
      }

      @Override
      public void logInformation(String message) {
        DartCore.logInformation(message);
      }

      @Override
      public void logInformation(String message, Throwable exception) {
        DartCore.logInformation(message, exception);
      }
    });
    DartSdkManager.getManager().addSdkListener(this);
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      getProjectManager().start();
    }
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    DartSdkManager.getManager().removeSdkListener(this);

    try {
      if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
        getProjectManager().stop();
      }
      AnalysisIndexManager.stopServerAndIndexing();
      DartModelManager.shutdown();
      RootArtifactProvider.shutdown();

      if (DartCoreDebug.METRICS) {
        StringWriter writer = new StringWriter();
        PerformanceManager.getInstance().printMetrics(new PrintWriter(writer));
        String metricsInfo = writer.toString();
        if (metricsInfo.length() > 0) {
          getLog().log(new Status(Status.INFO, PLUGIN_ID, metricsInfo, null));
        }
      }
    } finally {
      super.stop(context);
    }
  }
}
