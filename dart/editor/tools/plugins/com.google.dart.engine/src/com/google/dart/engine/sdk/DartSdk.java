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
package com.google.dart.engine.sdk;

import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.engine.utilities.os.OSUtilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Instances of the class <code>DartSdk</code> represent a Dart SDK installed in a specified
 * location.
 */
public class DartSdk {
  /**
   * Instances of the class <code>LibraryMap</code> map Dart library URI's to the file within the
   * SDK representing that library.
   */
  public static class LibraryMap {
    /**
     * A table mapping Dart library URI's to the file within the SDK representing that library.
     */
    private HashMap<String, File> libraryMap = new HashMap<String, File>();

    /**
     * Prevent the creation of instances of this class from outside the SDK.
     */
    private LibraryMap() {
      super();
    }

    /**
     * Return an array containing the library URI's for which a mapping is available.
     * 
     * @return the library URI's for which a mapping is available
     */
    public String[] getUris() {
      return libraryMap.keySet().toArray(new String[libraryMap.size()]);
    }

    /**
     * Return the file within the SDK that represents the library with the given URI, or
     * <code>null</code> if the URI does not map to a file.
     * 
     * @param dartUri the URI to be mapped to a file
     * @return the file within the SDK that represents the library with the given URI
     */
    public File mapDartUri(String dartUri) {
      return libraryMap.get(dartUri);
    }

    /**
     * Return the number of library URI's for which a mapping is available.
     * 
     * @return the number of library URI's for which a mapping is available
     */
    public int size() {
      return libraryMap.size();
    }
  }

  /**
   * The directory containing the SDK.
   */
  private final File sdkDirectory;

  /**
   * The revision number of this SDK, or <code>"0"</code> if the revision number cannot be
   * discovered.
   */
  private String sdkVersion;

  /**
   * The revision number of Dartium that is included in this SDK, or <code>"0"</code> if the
   * revision number cannot be discovered.
   */
  private String dartiumVersion;

  /**
   * The file containing the Dartium executable.
   */
  private File dartiumExecutable;

  /**
   * The file containing the VM executable.
   */
  private File vmExecutable;

  /**
   * A table mapping platforms to a list of the library files associated with that platform.
   */
  private Map<Platform, LibraryMap> platformMap;

  /**
   * The name of the directory within the SDK directory that contains executables.
   */
  private static final String BIN_DIRECTORY_NAME = "bin"; //$NON-NLS-1$

  /**
   * The name of the directory within the SDK directory that contains Chromium.
   */
  private static final String CHROMIUM_DIRECTORY_NAME = "chromium"; //$NON-NLS-1$

  /**
   * The name of the file containing property values for Chromium.
   */
  private static final String CHROMIUM_PROPERTIES_FILE_NAME = "chromium.properties"; //$NON-NLS-1$

  /**
   * The name of the property whose value is the version of Chromium.
   */
  private static final String CHROMIUM_VERSION_PROPERTY_NAME = "chromium.version"; //$NON-NLS-1$

  /**
   * The name of the directory within the library directory that contains the platform configuration
   * files.
   */
  private static final String CONFIG_DIRECTORY_NAME = "config"; //$NON-NLS-1$

  /**
   * The prefix at the beginning of the name of every platform configuration file.
   */
  private static final String CONFIG_FILE_PREFIX = "import_"; //$NON-NLS-1$

  /**
   * The prefix at the beginning of the name of every platform configuration file.
   */
  private static final int CONFIG_FILE_PREFIX_LENGTH = CONFIG_FILE_PREFIX.length();

  /**
   * The suffix at the end of the name of every platform configuration file.
   */
  private static final String CONFIG_FILE_SUFFIX = ".config"; //$NON-NLS-1$

  /**
   * The suffix at the end of the name of every platform configuration file.
   */
  private static final int CONFIG_FILE_SUFFIX_LENGTH = CONFIG_FILE_SUFFIX.length();

  /**
   * The name of the file containing the Dartium executable on Linux.
   */
  private static final String DARTIUM_EXECUTABLE_NAME_LINUX = "chromium/chrome"; //$NON-NLS-1$

  /**
   * The name of the file containing the Dartium executable on Macintosh.
   */
  private static final String DARTIUM_EXECUTABLE_NAME_MAC = "Chromium.app/Contents/MacOS/Chromium"; //$NON-NLS-1$

  /**
   * The name of the file containing the Dartium executable on Windows.
   */
  private static final String DARTIUM_EXECUTABLE_NAME_WIN = "chromium/Chrome.exe"; //$NON-NLS-1$

  /**
   * The name of the {@link System} property whose value is the path to the default Dart SDK
   * directory.
   */
  private static final String DEFAULT_DIRECTORY_PROPERTY_NAME = "com.google.dart.sdk"; //$NON-NLS-1$

  /**
   * The version number that is returned when the real version number could not be determined.
   */
  private static final String DEFAULT_VERSION = "0"; //$NON-NLS-1$

  /**
   * The name of the directory within the SDK directory that contains documentation for the
   * libraries.
   */
  private static final String DOCS_DIRECTORY_NAME = "docs"; //$NON-NLS-1$

  /**
   * The suffix added to the name of a library to derive the name of the file containing the
   * documentation for that library.
   */
  private static final String DOC_FILE_SUFFIX = "_api.json"; //$NON-NLS-1$

  /**
   * The name of the directory within the SDK directory that contains the libraries.
   */
  private static final String LIB_DIRECTORY_NAME = "lib"; //$NON-NLS-1$

  /**
   * The name of the file within the SDK directory that contains the revision number of the SDK.
   */
  private static final String REVISION_FILE_NAME = "revision"; //$NON-NLS-1$

  /**
   * The name of the file containing the VM executable on the Windows operating system.
   */
  private static final String VM_EXECUTABLE_NAME_WIN = "dart.exe"; //$NON-NLS-1$

  /**
   * The name of the file containing the VM executable on non-Windows operating systems.
   */
  private static final String VM_EXECUTABLE_NAME = "dart"; //$NON-NLS-1$

  /**
   * Return the default Dart SDK, or <code>null</code> if the directory containing the default SDK
   * cannot be determined (or does not exist).
   * 
   * @return the default Dart SDK
   */
  public static DartSdk getDefaultSdk() {
    File sdkDirectory = getDefaultSdkDirectory();
    if (sdkDirectory == null) {
      return null;
    }
    return new DartSdk(sdkDirectory);
  }

  /**
   * Return the default directory for the Dart SDK, or <code>null</code> if the directory cannot be
   * determined (or does not exist). The default directory is provided by a {@link System} property
   * named <code>com.google.dart.sdk</code>.
   * 
   * @return the default directory for the Dart SDK
   */
  public static File getDefaultSdkDirectory() {
    String sdkProperty = System.getProperty(DEFAULT_DIRECTORY_PROPERTY_NAME);
    if (sdkProperty == null) {
      return null;
    }
    File sdkDirectory = new File(sdkProperty);
    if (!sdkDirectory.exists()) {
      return null;
    }
    return sdkDirectory;
  }

  /**
   * Initialize a newly created SDK to represent the Dart SDK installed in the given directory.
   * 
   * @param sdkDirectory the directory containing the SDK
   */
  public DartSdk(File sdkDirectory) {
    this.sdkDirectory = sdkDirectory.getAbsoluteFile();
    initializeSdk();
    initializeLibraryMap();
  }

  /**
   * Return the file containing the Dartium executable, or <code>null</code> if it does not exist.
   * 
   * @return the file containing the Dartium executable
   */
  public File getDartiumExecutable() {
    synchronized (this) {
      if (dartiumExecutable == null) {
        File file = new File(sdkDirectory, getDartiumBinaryName());
        if (file.exists()) {
          dartiumExecutable = file;
        }
      }
    }
    return dartiumExecutable;
  }

  /**
   * Return the revision number of Dartium that is included in this SDK.
   * 
   * @return the revision number of Dartium included in this SDK
   */
  public String getDartiumVersion() {
    synchronized (this) {
      if (dartiumVersion == null) {
        dartiumVersion = DEFAULT_VERSION;
        File versionFile = new File(sdkDirectory, CHROMIUM_PROPERTIES_FILE_NAME);
        if (versionFile.exists()) {
          Properties properties = readProperties(versionFile);
          dartiumVersion = properties.getProperty(CHROMIUM_VERSION_PROPERTY_NAME, DEFAULT_VERSION);
        }
      }
    }
    return dartiumVersion;
  }

  /**
   * Return the directory where dartium can be found in the Dart SDK (the directory that will be the
   * working directory is Dartium is invoked without changing the default).
   * 
   * @return the directory where dartium can be found
   */
  public File getDartiumWorkingDirectory() {
    if (OSUtilities.isWindows() || OSUtilities.isMac()) {
      return sdkDirectory;
    } else {
      return new File(sdkDirectory, CHROMIUM_DIRECTORY_NAME);
    }
  }

  /**
   * Return the directory containing the SDK.
   * 
   * @return the directory containing the SDK
   */
  public File getDirectory() {
    return sdkDirectory;
  }

  /**
   * Return the directory containing documentation for the SDK.
   * 
   * @return the SDK's documentation directory
   */
  public File getDocDirectory() {
    return new File(sdkDirectory, DOCS_DIRECTORY_NAME);
  }

  /**
   * Return the auxiliary documentation file for the given library, or <code>null</code> if no such
   * file exists.
   * 
   * @param libraryName the name of the library associated with the documentation file to be
   *          returned
   * @return the auxiliary documentation file for the library
   */
  public File getDocFileFor(String libraryName) {
    File dir = getDocDirectory();
    if (!dir.exists()) {
      return null;
    }
    File libDir = new File(dir, libraryName);
    File docFile = new File(libDir, libraryName + DOC_FILE_SUFFIX);
    if (docFile.exists()) {
      return docFile;
    }
    return null;
  }

  /**
   * Return a map from Dart library URI's to the file within the SDK representing that library. The
   * map will be specific to the given platform.
   * 
   * @param platform the platform defining the libraries that can be mapped to files
   * @return a map from Dart library URI's to the file within the SDK representing that library
   */
  public LibraryMap getLibrariesForPlatform(Platform platform) {
    return platformMap.get(platform);
  }

  /**
   * @return the SDK's library directory path
   */
  public File getLibraryDirectory() {
    return new File(sdkDirectory, LIB_DIRECTORY_NAME);
  }

  /**
   * Return the revision number of this SDK, or <code>"0"</code> if the revision number cannot be
   * discovered.
   * 
   * @return the revision number of this SDK
   */
  public String getSdkVersion() {
    synchronized (this) {
      if (sdkVersion == null) {
        sdkVersion = DEFAULT_VERSION;
        File revisionFile = new File(sdkDirectory, REVISION_FILE_NAME);
        try {
          String revision = FileUtilities.getContents(revisionFile);
          if (revision != null) {
            sdkVersion = revision;
          }
        } catch (IOException exception) {
          // Fall through to return the default.
        }
      }
    }
    return sdkVersion;
  }

  /**
   * Return an array containing all of the platforms that are supported by this SDK.
   * 
   * @return the platforms that are supported by this SDK
   */
  public Platform[] getSupportedPlatforms() {
    return platformMap.keySet().toArray(new Platform[platformMap.size()]);
  }

  /**
   * Return the file containing the VM executable, or <code>null</code> if it does not exist.
   * 
   * @return the file containing the VM executable
   */
  public File getVmExecutable() {
    synchronized (this) {
      if (vmExecutable == null) {
        File file = new File(new File(sdkDirectory, BIN_DIRECTORY_NAME), getBinaryName());
        if (file.exists()) {
          vmExecutable = file;
        }
      }
    }
    return vmExecutable;
  }

  /**
   * Return <code>true</code> if this SDK includes documentation.
   * 
   * @return <code>true</code> if this installation of the SDK has documentation
   */
  public boolean hasDocumentation() {
    return getDocDirectory().exists();
  }

  /**
   * Return <code>true</code if the Dartium binary is available.
   * 
   * @return <code>true</code if the Dartium binary is available
   */
  public boolean isDartiumInstalled() {
    return getDartiumExecutable() != null;
  }

  /**
   * Ensure that the dart VM is executable. If it is not, make it executable and log that it was
   * necessary for us to do so.
   */
  private void ensureVmIsExecutable() {
    File dartVm = getVmExecutable();
    if (dartVm != null) {
      if (!dartVm.canExecute()) {
        FileUtilities.makeExecutable(dartVm);
        //DartCore.logError(dartVm.getPath() + " was not executable");
      }
    }
  }

  /**
   * Return the name of the platform for which the given file name is a configuration file.
   * 
   * @param configFileName the file name containing the name of the platform being configured
   * @return the name of the platform embedded in the given file name
   */
  private String extractPlatformName(String configFileName) {
    return configFileName.substring(CONFIG_FILE_PREFIX_LENGTH, configFileName.length()
        - CONFIG_FILE_SUFFIX_LENGTH);
  }

  /**
   * Return the name of the file containing the VM executable.
   * 
   * @return the name of the file containing the VM executable
   */
  private String getBinaryName() {
    if (OSUtilities.isWindows()) {
      return VM_EXECUTABLE_NAME_WIN;
    } else {
      return VM_EXECUTABLE_NAME;
    }
  }

  /**
   * Return the name of the file containing the Dartium executable.
   * 
   * @return the name of the file containing the Dartium executable
   */
  private String getDartiumBinaryName() {
    if (OSUtilities.isWindows()) {
      return DARTIUM_EXECUTABLE_NAME_WIN;
    } else if (OSUtilities.isMac()) {
      return DARTIUM_EXECUTABLE_NAME_MAC;
    } else {
      return DARTIUM_EXECUTABLE_NAME_LINUX;
    }
  }

  /**
   * Read the given configuration file to extract the information about the libraries that are
   * defined for the platform it configures.
   * 
   * @param configFile the configuration file to be read
   * @return the information that was extracted from the file
   */
  private LibraryMap getLibrariesForPlatform(File configFile) {
    if (!configFile.exists()) {
      // TODO(brianwilkerson) Report the error?
      return new LibraryMap();
    }
    Properties properties = readProperties(configFile);
    return getLibrariesFromProperties(properties);
  }

  /**
   * Process the given properties to extract the information about the libraries that are defined
   * for the platform it configures.
   * 
   * @param properties the properties to be processed
   * @return the information that was extracted from the properties
   */
  private LibraryMap getLibrariesFromProperties(Properties properties) {
    LibraryMap libraries = new LibraryMap();
    File base = getLibraryDirectory();
    HashSet<String> explicitShortNames = new HashSet<String>();
    for (Entry<Object, Object> entry : properties.entrySet()) {
      String shortName = ((String) entry.getKey()).trim();
      String path = ((String) entry.getValue()).trim();
      File file = new File(base, path);
      if (!file.exists()) {
        // TODO(brianwilkerson) Report the error?
        continue;
      }
      if (shortName.endsWith(":")) { //$NON-NLS-1$
        // If the shortName ends with ":" then search the associated directory for libraries
        if (file.isDirectory()) {
          for (File child : file.listFiles()) {
            String host = child.getName();
            // Do not overwrite explicit shortName to dart file mappings
            if (!explicitShortNames.contains(shortName + host) && child.isDirectory()) {
              File dartFile = new File(child, child.getName() + ".dart"); //$NON-NLS-1$
              if (dartFile.isFile()) {
                libraries.libraryMap.put(shortName, dartFile);
              }
            }
          }
        }
      } else {
        // Otherwise treat the entry as an explicit shortName to dart file mapping
        int index = shortName.indexOf(':');
        if (index > 0) {
          libraries.libraryMap.put(shortName, file);
        }
      }
    }
    return libraries;
  }

  /**
   * Read all of the configuration files to initialize the library maps.
   */
  private void initializeLibraryMap() {
    platformMap = new HashMap<Platform, LibraryMap>();
    File libraryDirectory = getLibraryDirectory();
    File configDirectory = new File(libraryDirectory, CONFIG_DIRECTORY_NAME);
    for (File configFile : configDirectory.listFiles()) {
      String configFileName = configFile.getName();
      if (isConfigFileName(configFileName)) {
        Platform platform = Platform.getPlatform(extractPlatformName(configFileName));
        platformMap.put(platform, getLibrariesForPlatform(configFile));
      }
    }
  }

  /**
   * Initialize the state of the SDK.
   */
  private void initializeSdk() {
    if (!OSUtilities.isWindows()) {
      ensureVmIsExecutable();
    }
  }

  /**
   * Return <code>true</code> if the given file name is the name of a configuration file.
   * 
   * @param fileName the file name being tested
   * @return <code>true</code> if the given file name is the name of a configuration file
   */
  private boolean isConfigFileName(String fileName) {
    return fileName.startsWith(CONFIG_FILE_PREFIX) && fileName.endsWith(CONFIG_FILE_SUFFIX);
  }

  /**
   * Read the given file as a properties file.
   * 
   * @param file the file containing the properties to be read
   * @return the properties that were read from the file
   */
  private Properties readProperties(File file) {
    Properties importConfig = new Properties();
    InputStream stream = null;
    try {
      stream = new BufferedInputStream(new FileInputStream(file));
      importConfig.load(stream);
    } catch (IOException exception) {
      // Fall through to return an empty set of properties.
    } finally {
      try {
        stream.close();
      } catch (IOException exception) {
        // Ignored
      }
    }
    return importConfig;
  }
}
