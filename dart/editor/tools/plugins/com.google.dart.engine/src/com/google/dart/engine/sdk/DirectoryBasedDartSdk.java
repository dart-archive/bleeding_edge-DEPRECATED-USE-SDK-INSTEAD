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
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.sdk.LibraryMap;
import com.google.dart.engine.internal.sdk.SdkLibrariesReader;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.UriKind;
import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.engine.utilities.os.OSUtilities;
import com.google.dart.engine.utilities.translation.DartBlockBody;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.engine.utilities.translation.DartOptional;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Instances of the class {@code DirectoryBasedDartSdk} represent a Dart SDK installed in a
 * specified directory.
 * 
 * @coverage dart.engine.sdk
 */
public class DirectoryBasedDartSdk implements DartSdk {
  /**
   * The {@link AnalysisContext} which is used for all of the sources in this {@link DartSdk}.
   */
  private InternalAnalysisContext analysisContext;

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
   * The name of the file containing the Dartium executable on Linux.
   */
  private static final String DARTIUM_EXECUTABLE_NAME_LINUX = "chrome"; //$NON-NLS-1$

  /**
   * The name of the file containing the Dartium executable on Macintosh.
   */
  private static final String DARTIUM_EXECUTABLE_NAME_MAC = "Chromium.app/Contents/MacOS/Chromium"; //$NON-NLS-1$

  /**
   * The name of the file containing the Dartium executable on Windows.
   */
  private static final String DARTIUM_EXECUTABLE_NAME_WIN = "Chrome.exe"; //$NON-NLS-1$

  /**
   * The name of the {@link System} property whose value is the path to the default Dart SDK
   * directory.
   */
  private static final String DEFAULT_DIRECTORY_PROPERTY_NAME = "com.google.dart.sdk"; //$NON-NLS-1$

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
   * The name of the pub executable on windows.
   */
  private static final String PUB_EXECUTABLE_NAME_WIN = "pub.bat"; //$NON-NLS-1$

  /**
   * The name of the pub executable on non-windows operating systems.
   */
  private static final String PUB_EXECUTABLE_NAME = "pub"; //$NON-NLS-1$

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
  public static DirectoryBasedDartSdk getDefaultSdk() {
    File sdkDirectory = getDefaultSdkDirectory();
    if (sdkDirectory == null) {
      return null;
    }
    return new DirectoryBasedDartSdk(sdkDirectory);
  }

  /**
   * Return the default directory for the Dart SDK, or {@code null} if the directory cannot be
   * determined (or does not exist). The default directory is provided by a {@link System} property
   * named {@code com.google.dart.sdk}.
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
  @DartOmit
  public DirectoryBasedDartSdk(File sdkDirectory) {
    this(sdkDirectory, false);
  }

  /**
   * Initialize a newly created SDK to represent the Dart SDK installed in the given directory.
   * 
   * @param sdkDirectory the directory containing the SDK
   * @param useDart2jsPaths {@code true} if the dart2js path should be used when it is available
   */
  public DirectoryBasedDartSdk(File sdkDirectory,
      @DartOptional(defaultValue = "false") boolean useDart2jsPaths) {
    this.sdkDirectory = sdkDirectory.getAbsoluteFile();
    initializeSdk();
    initializeLibraryMap(useDart2jsPaths);
    analysisContext = new AnalysisContextImpl();
    analysisContext.setSourceFactory(new SourceFactory(new DartUriResolver(this)));
    String[] uris = getUris();
    ChangeSet changeSet = new ChangeSet();
    for (String uri : uris) {
      changeSet.addedSource(analysisContext.getSourceFactory().forUri(uri));
    }
    analysisContext.applyChanges(changeSet);
  }

  @Override
  public Source fromEncoding(UriKind kind, URI uri) {
    return new FileBasedSource(new File(uri), kind);
  }

  @Override
  public AnalysisContext getContext() {
    return analysisContext;
  }

  /**
   * Return the file containing the Dartium executable, or {@code null} if it does not exist.
   * 
   * @return the file containing the Dartium executable
   */
  public File getDartiumExecutable() {
    synchronized (this) {
      if (dartiumExecutable == null) {
        File file = new File(getDartiumWorkingDirectory(), getDartiumBinaryName());
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
    return new File(sdkDirectory.getParentFile(), CHROMIUM_DIRECTORY_NAME);
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
   * Return the file containing the Pub executable, or {@code null} if it does not exist.
   * 
   * @return the file containing the Pub executable
   */
  public File getPubExecutable() {
    String pubBinaryName = OSUtilities.isWindows() ? PUB_EXECUTABLE_NAME_WIN : PUB_EXECUTABLE_NAME;

    File file = new File(new File(sdkDirectory, BIN_DIRECTORY_NAME), pubBinaryName);

    return file.exists() ? file : null;
  }

  @Override
  public SdkLibrary[] getSdkLibraries() {
    return libraryMap.getSdkLibraries();
  }

  @Override
  public SdkLibrary getSdkLibrary(String dartUri) {
    return libraryMap.getLibrary(dartUri);
  }

  /**
   * Return the revision number of this SDK, or {@code "0"} if the revision number cannot be
   * discovered.
   * 
   * @return the revision number of this SDK
   */
  @Override
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
   * Return an array containing the library URI's for the libraries defined in this SDK.
   * 
   * @return the library URI's for the libraries defined in this SDK
   */
  @Override
  public String[] getUris() {
    return libraryMap.getUris();
  }

  /**
   * Return the file containing the VM executable, or {@code null} if it does not exist.
   * 
   * @return the file containing the VM executable
   */
  public File getVmExecutable() {
    synchronized (this) {
      if (vmExecutable == null) {
        File file = new File(new File(sdkDirectory, BIN_DIRECTORY_NAME), getVmBinaryName());
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

  @Override
  public Source mapDartUri(String dartUri) {
    SdkLibrary library = getSdkLibrary(dartUri);
    if (library == null) {
      return null;
    }
    return new FileBasedSource(new File(getLibraryDirectory(), library.getPath()), UriKind.DART_URI);
  }

  /**
   * Ensure that the dart VM is executable. If it is not, make it executable and log that it was
   * necessary for us to do so.
   */
  @DartBlockBody({})
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
   * Return the name of the file containing the VM executable.
   * 
   * @return the name of the file containing the VM executable
   */
  private String getVmBinaryName() {
    if (OSUtilities.isWindows()) {
      return VM_EXECUTABLE_NAME_WIN;
    } else {
      return VM_EXECUTABLE_NAME;
    }
  }

  /**
   * Read all of the configuration files to initialize the library maps.
   * 
   * @param useDart2jsPaths {@code true} if the dart2js path should be used when it is available
   */
  private void initializeLibraryMap(boolean useDart2jsPaths) {
    File librariesFile = new File(new File(getLibraryDirectory(), INTERNAL_DIR), LIBRARIES_FILE);
    try {
      String contents = FileUtilities.getContents(librariesFile);
      libraryMap = new SdkLibrariesReader(useDart2jsPaths).readFromFile(librariesFile, contents);
    } catch (Exception exception) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not initialize the library map from " + librariesFile.getAbsolutePath(),
          exception);
      libraryMap = new LibraryMap();
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
