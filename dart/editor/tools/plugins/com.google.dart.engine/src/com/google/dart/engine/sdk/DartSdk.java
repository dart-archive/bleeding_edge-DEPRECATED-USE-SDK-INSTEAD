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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.internal.sdk.LibraryMap;
import com.google.dart.engine.internal.sdk.SdkLibrariesReader;
import com.google.dart.engine.internal.sdk.SdkLibrary;
import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.engine.utilities.os.OSUtilities;

import java.io.File;
import java.io.IOException;

/**
 * Instances of the class {@code DartSdk} represent a Dart SDK installed in a specified location.
 */
public class DartSdk {
  /**
   * The directory containing the SDK.
   */
  private final File sdkDirectory;

  /**
   * The revision number of this SDK, or {@code "0"} if the revision number cannot be discovered.
   */
  private String sdkVersion;

  /**
   * The file containing the Dartium executable.
   */
  private File dartiumExecutable;

  /**
   * The file containing the VM executable.
   */
  private File vmExecutable;

  /**
   * A mapping from Dart library URI's to the library represented by that URI.
   */
  private LibraryMap libraryMap;

  /**
   * The name of the directory within the SDK directory that contains executables.
   */
  private static final String BIN_DIRECTORY_NAME = "bin"; //$NON-NLS-1$

  /**
   * The name of the directory within the SDK directory that contains Chromium.
   */
  private static final String CHROMIUM_DIRECTORY_NAME = "chromium"; //$NON-NLS-1$

  /**
   * The name of the environment variable whose value is the path to the default Dart SDK directory.
   */
  private static final String DART_SDK_ENVIRONMENT_VARIABLE_NAME = "DART_SDK"; //$NON-NLS-1$

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
   * The name of the directory within the SDK directory that contains the libraries file.
   */
  private static final String INTERNAL_DIR = "_internal"; //$NON-NLS-1$

  /**
   * The name of the directory within the SDK directory that contains the libraries.
   */
  private static final String LIB_DIRECTORY_NAME = "lib"; //$NON-NLS-1$

  /**
   * The name of the libraries file.
   */
  private static final String LIBRARIES_FILE = "libraries.dart"; //$NON-NLS-1$

  /**
   * The name of the directory within the SDK directory that contains the packages.
   */
  private static final String PKG_DIRECTORY_NAME = "pkg"; //$NON-NLS-1$

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
   * Return the default Dart SDK, or {@code null} if the directory containing the default SDK cannot
   * be determined (or does not exist).
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
   * Return the default directory for the Dart SDK, or {@code null} if the directory cannot be
   * determined (or does not exist). The default directory is provided by a {@link System} property
   * named {@code com.google.dart.sdk}, or, if the property is not defined, an environment variable
   * named {@code DART_SDK}.
   * 
   * @return the default directory for the Dart SDK
   */
  public static File getDefaultSdkDirectory() {
    String sdkProperty = System.getProperty(DEFAULT_DIRECTORY_PROPERTY_NAME);
    if (sdkProperty == null) {
      sdkProperty = System.getenv(DART_SDK_ENVIRONMENT_VARIABLE_NAME);
      if (sdkProperty == null) {
        return null;
      }
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
   * Return the file containing the Dartium executable, or {@code null} if it does not exist.
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
   * Return the auxiliary documentation file for the given library, or {@code null} if no such file
   * exists.
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
   * Return the directory within the SDK directory that contains the libraries.
   * 
   * @return the directory that contains the libraries
   */
  public File getLibraryDirectory() {
    return new File(sdkDirectory, LIB_DIRECTORY_NAME);
  }

  /**
   * Return the directory within the SDK directory that contains the packages.
   * 
   * @return the directory that contains the packages
   */
  public File getPackageDirectory() {
    return new File(getDirectory(), PKG_DIRECTORY_NAME);
  }

  /**
   * Return the revision number of this SDK, or {@code "0"} if the revision number cannot be
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
   * Return the file containing the VM executable, or {@code null} if it does not exist.
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
   * Return {@code true} if this SDK includes documentation.
   * 
   * @return {@code true} if this installation of the SDK has documentation
   */
  public boolean hasDocumentation() {
    return getDocDirectory().exists();
  }

  /**
   * Return {@code true} if the Dartium binary is available.
   * 
   * @return {@code true} if the Dartium binary is available
   */
  public boolean isDartiumInstalled() {
    return getDartiumExecutable() != null;
  }

  /**
   * Return the file representing the library with the given {@code dart:} URI, or {@code null} if
   * the given URI does not denote a library in this SDK.
   * 
   * @param dartUri the URI of the library to be returned
   * @return the file representing the specified library
   */
  public File mapDartUri(String dartUri) {
    SdkLibrary library = libraryMap.getLibrary(dartUri);
    if (library == null) {
      return null;
    }
    return new File(getLibraryDirectory(), library.getPath());
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
        AnalysisEngine.getInstance().getLogger().logError(dartVm.getPath() + " was not executable");
      }
    }
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
   * Read all of the configuration files to initialize the library maps.
   */
  private void initializeLibraryMap() {
    try {
      File librariesFile = new File(new File(getLibraryDirectory(), INTERNAL_DIR), LIBRARIES_FILE);
      String contents = FileUtilities.getContents(librariesFile);
      libraryMap = new SdkLibrariesReader().readFrom(contents);
    } catch (Exception exception) {
      // TODO Auto-generated catch block
      exception.printStackTrace();
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
}
