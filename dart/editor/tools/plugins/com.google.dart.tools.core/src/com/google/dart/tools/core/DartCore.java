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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.logging.Logger;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.internal.remote.RemoteAnalysisServerImpl;
import com.google.dart.server.internal.remote.StdioServerSocket;
import com.google.dart.server.utilities.logging.Logging;
import com.google.dart.tools.core.analysis.model.AnalysisServerData;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.MessageConsoleImpl;
import com.google.dart.tools.core.internal.OptionManager;
import com.google.dart.tools.core.internal.analysis.model.AnalysisServerDataImpl;
import com.google.dart.tools.core.internal.analysis.model.DartProjectManager;
import com.google.dart.tools.core.internal.analysis.model.ProjectManagerImpl;
import com.google.dart.tools.core.internal.analysis.model.WorkspaceAnalysisServerListener;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.jobs.CleanLibrariesJob;
import com.google.dart.tools.core.model.DartIgnoreListener;
import com.google.dart.tools.core.model.DartSdkListener;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.utilities.general.StringUtilities;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.core.utilities.performance.PerformanceManager;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 * The class <code>DartCore</code> is used to access the elements modeling projects that have a Dart
 * nature.
 * 
 * @coverage dart.tools.core
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
   * Name of file containing user-defined properties.
   */
  public static final String EDITOR_PROPERTIES = "editor.properties";

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
   * Eclipse problem marker type used to display Dart compilation errors.
   */
  public static final String DART_PROBLEM_MARKER_TYPE = PLUGIN_ID + ".problem";

  /**
   * Eclipse problem marker type used to display Dart hints.
   */
  public static final String DART_HINT_MARKER_TYPE = PLUGIN_ID + ".hint";

  /**
   * Eclipse problem marker type used to display todo markers.
   */
  public static final String DART_TASK_MARKER_TYPE = PLUGIN_ID + ".task";

  /**
   * Eclipse problem marker type used to Angular warning markers
   */
  public static final String ANGULAR_WARNING_MARKER_TYPE = PLUGIN_ID + ".angular_warning";

  /**
   * Extension for single unit compiled into JavaScript.
   */
  public static final String EXTENSION_JS = "js";

  /**
   * Preference for enabling Angular analysis.
   */
  public static final String ENABLE_ANGULAR_ANALYSIS_PREFERENCE = "enableAngularAnalysis";

  /**
   * Preference for the automatically running pub.
   */
  public static final String PUB_AUTO_RUN_PREFERENCE = "pubAutoRun";

  /**
   * Preference for enabling hints.
   */
  public static final String ENABLE_HINTS_PREFERENCE = "enableHints";

  /**
   * Preference for enabling dart2js related hints.
   */
  public static final String ENABLE_HINTS_DART2JS_PREFERENCE = "enableHints_dart2js";

  public static final String PROJECT_PREF_PACKAGE_ROOT = "projectPackageRoot";

  public static final String PREFS_DART2JS_FLAGS = "dart2jsFlags";

  /**
   * Preference to control if "not a member" warnings should be reported for inferred types.
   */
  public static final String TYPE_CHECKS_FOR_INFERRED_TYPES = "typeChecksForInferredTypes";

  public static final String PROJECT_PREF_DISABLE_DART_BASED_BUILDER = "disableDartBasedBuilder";

  // white listed dirs that can be used for pub build
  public static final List<String> pubDirectories = Arrays.asList(
      "assest",
      "benchmark",
      "bin",
      "example",
      "test",
      "web");

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
   * Name of directory for build artifacts generated by pub
   */
  public static final String BUILD_DIRECTORY_NAME = "build";

  /**
   * Name of "lib" directory in package
   */
  public static final String LIB_DIRECTORY_NAME = "lib";

  /**
   * Path string for packages directory, uses "/" as path separator, equals "/packages/".
   */
  public static final String PACKAGES_DIRECTORY_PATH = "/" + PACKAGES_DIRECTORY_NAME + "/";

  /**
   * Path string for packages directory in a url.
   */
  public static final String PACKAGES_DIRECTORY_URL = "/" + PACKAGES_DIRECTORY_NAME + "/";

  public static final String PACKAGE_SCHEME_SPEC = "package:";

  /**
   * Path string for lib directory in a url.
   */
  public static final String LIB_URL_PATH = "/lib/";

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
   * Used by the {@link DartDebugUserAgentManager} to indicate whether the allow remote connection
   * dialog is open.
   */
  public static boolean allowConnectionDialogOpen = false;

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
   * The QualifiedName for the package version.
   */
  public static final QualifiedName PUB_PACKAGE_VERSION = new QualifiedName(
      DartCore.PLUGIN_ID,
      "pub.package.version");

  /**
   * The QualifiedName for library names.
   */
  public static final QualifiedName LIBRARY_NAME = new QualifiedName(
      DartCore.PLUGIN_ID,
      "libraryName");

  /**
   * The unique project manager used for analysis of anything in the workspace
   */
  private static ProjectManager projectManager;

  /**
   * Used to synchronize access to {@link #projectManager}.
   */
  private static final Object projectManagerLock = new Object();

  /**
   * The unique {@link AnalysisServer} used for analysis of anything in the workspace.
   */
  private static AnalysisServer analysisServer;

  /**
   * The unique {@link AnalysisServerDataImpl} instance.
   */
  private static AnalysisServerDataImpl analysisServerDataImpl = new AnalysisServerDataImpl();

  /**
   * The unique {@link WorkspaceAnalysisServerListener} instance.
   */
  private static WorkspaceAnalysisServerListener analysisServerListener;

  /**
   * Used to synchronize access to {@link #analysisServer}.
   */
  private static final Object analysisServerLock = new Object();

  /**
   * Add the given listener for dart ignore changes to the Dart Model. Has no effect if an identical
   * listener is already registered.
   * 
   * @param listener the listener to add
   */
  public static void addIgnoreListener(DartIgnoreListener listener) {
    getProjectManager().getIgnoreManager().addListener(listener);
  }

  /**
   * Add the given resource to the set of ignored resources.
   * 
   * @param resource the resource to ignore
   * @throws IOException if there was an error accessing the ignore file
   * @throws CoreException if there was an error deleting markers
   */
  public static void addToIgnores(IResource resource) throws IOException, CoreException {
    getProjectManager().getIgnoreManager().addToIgnores(resource);
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
   * Answer the unique {@link AnalysisServer} used for analysis of anything in the workspace.
   * 
   * @return the {@link AnalysisServer} (not {@code null})
   */
  public static AnalysisServer getAnalysisServer() {
    synchronized (analysisServerLock) {
      if (analysisServer == null) {
        // TODO(scheglov) remove local analysis server
//        analysisServer = new com.google.dart.server.internal.local.LocalAnalysisServerImpl();
        DartSdkManager sdkManager = DartSdkManager.getManager();
        if (!sdkManager.hasSdk()) {
          DartCore.logError("Add the dart sdk (com.google.dart.sdk) as a JVM argument");
          System.exit(1);
        }
        String svnRoot = System.getProperty("com.google.dart.svnRoot");
        if (svnRoot == null) {
          DartCore.logError("Add the dart svnRoot (com.google.dart.svnRoot) as a JVM argument");
          System.exit(1);
        }
        String runtimePath = sdkManager.getSdk().getVmExecutable().getAbsolutePath();
        String analysisServerPath = svnRoot + "/pkg/analysis_server/bin/server.dart";
        try {
          // prepare debug stream
          PrintStream debugStream;
          {
            String logPath = DartCoreDebug.ANALYSIS_SERVER_LOG_FILE;
            if (StringUtils.isBlank(logPath)) {
              debugStream = null;
            } else if ("console".equals(logPath)) {
              debugStream = System.out;
            } else {
              debugStream = new PrintStream(logPath);
            }
          }
          // start server
          StdioServerSocket socket = new StdioServerSocket(
              runtimePath,
              analysisServerPath,
              debugStream,
              DartCoreDebug.ANALYSIS_SERVER_DEBUG);
          analysisServer = new RemoteAnalysisServerImpl(socket);
          analysisServer.start(DartCoreDebug.ANALYSIS_SERVER_DEBUG ? 0 : 15000);
          analysisServerDataImpl.setServer(analysisServer);
          analysisServerListener = new WorkspaceAnalysisServerListener(
              analysisServerDataImpl,
              new DartProjectManager(
                  ResourcesPlugin.getWorkspace().getRoot(),
                  analysisServer,
                  DartIgnoreManager.getInstance()));
          analysisServer.addAnalysisServerListener(analysisServerListener);
        } catch (Throwable e) {
          DartCore.logError("Enable to start stdio server", e);
          System.exit(1);
        }
      }
    }
    return analysisServer;
  }

  /**
   * Answer the unique {@link AnalysisServerData} used to provide analysis information in the
   * workspace.
   * 
   * @return the {@link AnalysisServerData} (not {@code null})
   */
  public static AnalysisServerData getAnalysisServerData() {
    getAnalysisServer();
    return analysisServerDataImpl;
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
   * Answer the application directory (a directory that contains a "packages" directory and a
   * "pubspec.yaml" file) directly or indirectly containing the specified resource or the resource
   * itself if it is an application directory.
   * 
   * @param resource the file or directory
   * @return the directory with the pubspec for the given file
   */
  public static IContainer getApplicationDirectory(IResource resource) {
    IContainer container;
    if (resource instanceof IFile) {
      container = resource.getParent();
    } else {
      container = (IContainer) resource;
    }
    while (container != null) {
      if (isApplicationDirectory(container)) {
        return container;
      }
      container = container.getParent();
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
    return null;
  }

  public static File getEclipseInstallationDirectory() {
    return new File(Platform.getInstallLocation().getURL().getFile());
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
   * Extract {@link ErrorCode} form the given {@link IMarker}.
   * 
   * @return the {@link ErrorCode}, may be {@code null}.
   */
  public static ErrorCode getErrorCode(IMarker marker) {
    return AnalysisMarkerManager.getErrorCode(marker);
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
    return OptionManager.getInstance().getOption(optionName);
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
    return OptionManager.getInstance().getOptions();
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
   * Answer the unique project manager used for analysis of anything in the workspace.
   * 
   * @return the manager (not {@code null})
   */
  public static ProjectManager getProjectManager() {
    synchronized (projectManagerLock) {
      if (projectManager == null) {
        // start index
        final Index index;
        {
          File stateDir = getPlugin().getStateLocation().toFile();
          File indexDir = new File(stateDir, "index");
          indexDir.mkdirs();
          IndexStore indexStore = IndexFactory.newFileIndexStore(indexDir);
          index = IndexFactory.newIndex(indexStore);
          Thread thread = new Thread() {
            @Override
            public void run() {
              index.run();
            }
          };
          thread.setName("Index Thread");
          thread.setDaemon(true);
          thread.start();
        }
        // create ProjectManagerImpl
        projectManager = new ProjectManagerImpl(
            ResourcesPlugin.getWorkspace().getRoot(),
            DartSdkManager.getManager().getSdk(),
            DartSdkManager.getManager().getSdkContextId(),
            index,
            DartIgnoreManager.getInstance());
      }
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

    try {
      PubFolder folder = DartCore.getProjectManager().getPubFolder(resource);
      IContainer appFolder = getApplicationDirectory(resource);
      if (appFolder != null && folder != null && !appFolder.equals(folder.getResource())) {
        // this is a case of nested pubspecs
        IResource pubspec = appFolder.findMember(DartCore.PUBSPEC_FILE_NAME);
        if (pubspec != null) {
          String contents = FileUtilities.getContents(pubspec.getLocation().toFile(), "UTF-8");
          if (contents != null) {
            String name = PubYamlUtils.getPubspecName(contents);
            if (name != null) {
              return name;
            }
          }
        }
      }
      if (folder != null) {
        packageName = folder.getPubspec().getName();
      }
    } catch (Exception e) {
      DartCore.logError(e);
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
    File file = new File(installDirectory, EDITOR_PROPERTIES);

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
    return getProjectManager().getIgnoreManager().isAnalyzed(resource);
  }

  /**
   * Return true if directory contains a "packages" directory and a "pubspec.yaml" file
   */
  public static boolean isApplicationDirectory(File directory) {
    return containsPubspecFile(directory)
        && (DartCoreDebug.NO_PUB_PACKAGES || containsPackagesDirectory(directory));
  }

  /**
   * Return true if directory contains a "packages" directory and a "pubspec.yaml" file
   */
  public static boolean isApplicationDirectory(IContainer container) {
    if (container.getLocation() == null) {
      return false;
    }

    File directory = container.getLocation().toFile();

    return isApplicationDirectory(directory);
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
   * Return true if directory is one that is generated by pub build
   * 
   * @param folder the folder to be checked
   * @return <code>true</code> if folder name matches and is sibling of pubspec.yaml
   */
  public static boolean isBuildDirectory(IFolder folder) {
    IPath location = folder.getLocation();
    if (location != null && folder.getName().equals(BUILD_DIRECTORY_NAME)) {
      if (folder.getParent().findMember(PUBSPEC_FILE_NAME) != null) {
        return true;
      }
    }
    return false;
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
   * Return <code>true</code> if the given file is contained in the packages directory created by
   * pub
   * 
   * @param file the file that is to be checked
   * @return <code>true</code> if the given file is in packages
   */
  public static boolean isContainedInPackages(IFile file) {
    return file.getFullPath().toString().contains(DartCore.PACKAGES_DIRECTORY_PATH);
  }

  /**
   * Return <code>true</code> if the given file name's extension is an CSS-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name's extension is an CSS-like extension
   */
  public static boolean isCssLikeFileName(String fileName) {
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
   * Return <code>true</code> if the name of the given {@link IFile} has a Dart-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the name of the given {@link IFile} has a Dart-like extension
   */
  public static boolean isDartLikeFile(IFile file) {
    String fileName = file.getName();
    return isDartLikeFileName(fileName);
  }

  /**
   * Return <code>true</code> if the given file name's extension is a Dart-like extension.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name's extension is a Dart-like extension
   */
  public static boolean isDartLikeFileName(String fileName) {
    return StringUtilities.endsWithIgnoreCase(fileName, ".dart");
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
  public static boolean isHtmlLikeFileName(String fileName) {
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
   * Return true if directory is build directory created by pub
   * 
   * @param folder the folder to be checked
   * @return <code>true</code> if folder/parent name matches and is sibling of pubspec.yaml
   */
  public static boolean isInBuildDirectory(IContainer folder) {
    while (folder.getParent() != null) {
      if (folder.getName().equals(BUILD_DIRECTORY_NAME)
          && folder.getParent().getFile(new Path(PUBSPEC_FILE_NAME)).exists()) {
        return true;
      }
      folder = folder.getParent();
    }
    return false;
  }

  /**
   * @return {@code true} if the given resource is located in the top-level "packages" folder of its
   *         enclosing package.
   */
  public static boolean isInDuplicatePackageFolder(IResource resource) {
    if (resource == null) {
      return false;
    }
    // prepare "pub" folder
    PubFolder pubFolder = DartCore.getProjectManager().getPubFolder(resource);
    if (pubFolder == null) {
      return false;
    }
    IPath pubPath = pubFolder.getResource().getFullPath();
    // check if resource is in "packages"
    IPath resourcePath = resource.getFullPath();
    String[] segments = resourcePath.segments();
    for (int i = 1; i < segments.length - 1; i++) {
      String segment = segments[i];
      if (segment.equals(PACKAGES_DIRECTORY_NAME)) {
        return !resourcePath.uptoSegment(i).equals(pubPath);
      }
    }
    // not in "packages"
    return false;
  }

  /**
   * @return {@code true} if the given resource is located in the "packages" sub-folder that
   *         corresponds to the enclosing package.
   */
  public static boolean isInSelfLinkedPackageFolder(IResource resource) {
    if (resource == null) {
      return false;
    }
    try {
      // prepare "pub" folder
      PubFolder pubFolder = DartCore.getProjectManager().getPubFolder(resource);
      if (pubFolder == null) {
        return false;
      }
      // the name of the enclosing package
      String packageName = pubFolder.getPubspec().getName();
      // check if resource is in "packages" and references the enclosing package
      String[] segments = resource.getFullPath().segments();
      for (int i = 0; i < segments.length - 1; i++) {
        String segment = segments[i];
        if (segment.equals(PACKAGES_DIRECTORY_NAME) && segments[i + 1].equals(packageName)) {
          return true;
        }
      }
    } catch (Throwable e) {
      // pubFolder.getPubspec() may fail
      return false;
    }
    // not a self-reference
    return false;
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
   * Check if this URI denotes a patch file.
   */
  public static boolean isPatchfile(File file) {
    return file != null && file.getName().endsWith("_patch.dart");
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
      //     if (linkedResource.getParent().getParent() == resource) {
      String resourcePath = resource.getLocation().toFile().getCanonicalPath();
      String linkPath = linkedResource.getLocation().toFile().getCanonicalPath();
      if (linkPath.startsWith(resourcePath)) {
        return true;
      }
      //     }
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
  public static boolean isTxtLikeFileName(String fileName) {
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
      ILog pluginLog = getPluginLog();
      if (pluginLog != null) {
        pluginLog.log(new Status(Status.INFO, PLUGIN_ID, "INFO: " + message, exception));
      }
    }

    instrumentationLogInfoImpl(message, exception);
  }

  /**
   * This method exists as a convenient marker for methods that have not yet been fully implemented.
   * It should be deleted before this product ships.
   */
  public static void notYetImplemented() {
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
   * Remove the given resource (as a path) from the set of ignored resources.
   * 
   * @param resource the resource path to (un)ignore
   * @throws IOException if there was an error accessing the ignore file
   */
  public static void removeFromIgnores(IPath resource) throws IOException {
    getProjectManager().getIgnoreManager().removeFromIgnores(resource);
  }

  /**
   * Remove the given resource from the set of ignored resources.
   * 
   * @param resource the resource to (un)ignore
   * @throws IOException if there was an error accessing the ignore file
   */
  public static void removeFromIgnores(IResource resource) throws IOException {
    getProjectManager().getIgnoreManager().removeFromIgnores(resource);
  }

  /**
   * Remove the given listener for dart ignore changes from the Dart Model. Has no effect if an
   * identical listener is not registered.
   * 
   * @param listener the non-<code>null</code> listener to remove
   */
  public static void removeIgnoreListener(DartIgnoreListener listener) {
    getProjectManager().getIgnoreManager().addListener(listener);
  }

  public static void setOptions(HashMap<String, String> newOptions) {

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
    File file = new File(installDirectory, EDITOR_PROPERTIES);

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

  /**
   * Answer the log used by {@link #logError(String)} and other local methods for logging errors,
   * warnings, and information.
   * 
   * @return the {@link ILog} or {@code null} if not accessible (for example during platform
   *         shutdown)
   */
  private static ILog getPluginLog() {
    try {
      return PLUGIN_LOG != null ? PLUGIN_LOG : getPlugin().getLog();
    } catch (Throwable e) {
      return null;
    }
  }

  /**
   * Record an error
   */
  private static void instrumentationLogErrorImpl(String message, Throwable exception) {
    instrumentationLogImpl(message, exception, "LogError");
  }

  /**
   * Internal instrumentation message delivery implementation
   * 
   * @param message optional message to be delivered
   * @param exception optional exception to be delivered
   * @param messageType message type, common values LogError/LogMessage
   */
  private static void instrumentationLogImpl(String message, Throwable exception, String messageType) {
    if (instrumentationLogErrorEnabled) {
      InstrumentationBuilder instrumentation = Instrumentation.builder("DartCore." + messageType);
      try {
        instrumentation.data("Log_Message", message != null ? message : "null");
        instrumentation.data("Log_Exception", exception != null ? exception.toString() : "null");
        if (exception != null) {
          instrumentation.record(exception);
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
   * Record an 'informational exception'
   */
  private static void instrumentationLogInfoImpl(String message, Throwable exception) {

    instrumentationLogImpl(message, exception, "LogInfo");

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
    ILog pluginLog = getPluginLog();
    if (pluginLog != null) {
      pluginLog.log(new Status(Status.ERROR, PLUGIN_ID, message, exception));
    }
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

  public String getDart2jsFlags() {
    return getPrefs().get(PREFS_DART2JS_FLAGS, "");
  }

  public String[] getDart2jsFlagsAsArray() {
    return StringUtilities.parseArgumentString(getDart2jsFlags());
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
        String[] paths = setting.split(File.pathSeparator);
        return new File(paths[0]);
      }
    }

    File[] roots = CmdLineOptions.getOptions().getPackageRoots();

    if (roots.length > 0) {
      return roots[0];
    }

    return null;
  }

  /**
   * Get the core plugin preferences. Use savePrefs() to save the preferences.
   */
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

  public boolean isAngularAnalysisEnabled() {
    return DartCore.getPlugin().getPrefs().getBoolean(ENABLE_ANGULAR_ANALYSIS_PREFERENCE, true);
  }

  public boolean isAutoRunPubEnabled() {
    return DartCore.getPlugin().getPrefs().getBoolean(PUB_AUTO_RUN_PREFERENCE, true);
  }

  public boolean isHintsDart2JSEnabled() {
    return DartCore.getPlugin().getPrefs().getBoolean(ENABLE_HINTS_DART2JS_PREFERENCE, true);
  }

  public boolean isHintsEnabled() {
    return DartCore.getPlugin().getPrefs().getBoolean(ENABLE_HINTS_PREFERENCE, true);
  }

  /**
   * Save the core plugin preferences
   * 
   * @throws CoreException
   */
  public void savePrefs() throws CoreException {
    try {
      getPrefs().flush();
    } catch (BackingStoreException e) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, e.toString(), e));
    }
  }

  @Override
  public void sdkUpdated(DirectoryBasedDartSdk sdk) {
    Job job = new CleanLibrariesJob();

    job.schedule();
  }

  public void setDart2JsPreferences(String args) {
    IEclipsePreferences prefs = getPrefs();
    prefs.put(PREFS_DART2JS_FLAGS, args);

    try {
      getPrefs().flush();
    } catch (BackingStoreException exception) {
      logError(exception);
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

    if (DartCoreDebug.PERF_THREAD_CONTENTION_MONIOR) {
      try {
        java.lang.management.ThreadMXBean th = ManagementFactory.getThreadMXBean();
        th.setThreadContentionMonitoringEnabled(true);
      } catch (UnsupportedOperationException e) {
      }
    }

    AnalysisEngine analysisEngine = AnalysisEngine.getInstance();
    analysisEngine.setLogger(new Logger() {
      @Override
      public void logError(String message) {
        DartCore.logError(message);
      }

      @Override
      public void logError(String message, Throwable exception) {
        DartCore.logError(message, exception);
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
    Logging.setLogger(new com.google.dart.server.utilities.logging.Logger() {
      @Override
      public void logError(String message) {
        DartCore.logError(message);
      }

      @Override
      public void logError(String message, Throwable exception) {
        DartCore.logError(message, exception);
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
    if (!DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      getProjectManager().hookListeners();
    }
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    DartSdkManager.getManager().removeSdkListener(this);

    try {
      getProjectManager().stop();

      synchronized (analysisServerLock) {
        if (analysisServer != null) {
          analysisServer.server_shutdown();
        }
      }

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
