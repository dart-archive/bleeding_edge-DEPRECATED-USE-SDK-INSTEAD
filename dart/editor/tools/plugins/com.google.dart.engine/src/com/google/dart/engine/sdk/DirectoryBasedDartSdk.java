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
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.SdkAnalysisContext;
import com.google.dart.engine.internal.sdk.LibraryMap;
import com.google.dart.engine.internal.sdk.SdkLibrariesReader;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.engine.utilities.os.OSUtilities;
import com.google.dart.engine.utilities.translation.DartBlockBody;
import com.google.dart.engine.utilities.translation.DartExpressionBody;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.engine.utilities.translation.DartOptional;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Instances of the class {@code DirectoryBasedDartSdk} represent a Dart SDK installed in a
 * specified directory. Typical Dart SDK layout is something like...
 * 
 * <pre>
 *    dart-sdk/
 *       bin/
 *          dart[.exe]  <-- VM
 *       lib/
 *          core/
 *             core.dart
 *             ... other core library files ...
 *          ... other libraries ...
 *       util/
 *          ... Dart utilities ...
 *    Chromium/   <-- Dartium typically exists in a sibling directory
 * </pre>
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
   * The file containing the dart2js executable.
   */
  private File dart2jsExecutable;

  /**
   * The file containing the dart formatter executable.
   */
  private File dartFmtExecutable;

  /**
   * The file containing the Dartium executable.
   */
  private File dartiumExecutable;

  /**
   * The file containing the pub executable.
   */
  private File pubExecutable;

  /**
   * The file containing the VM executable.
   */
  private File vmExecutable;

  /**
   * A mapping from Dart library URI's to the library represented by that URI.
   */
  private LibraryMap libraryMap;

  /**
   * The default SDK, or {@code null} if the default SDK either has not yet been created or cannot
   * be created for some reason.
   */
  private static DirectoryBasedDartSdk DEFAULT_SDK;

  /**
   * The name of the directory within the SDK directory that contains executables.
   */
  private static final String BIN_DIRECTORY_NAME = "bin"; //$NON-NLS-1$

  /**
   * The name of the directory on non-Mac that contains dartium.
   */
  private static final String DARTIUM_DIRECTORY_NAME = "chromium"; //$NON-NLS-1$

  /**
   * The name of the dart2js executable on non-windows operating systems.
   */
  private static final String DART2JS_EXECUTABLE_NAME = "dart2js"; //$NON-NLS-1$

  /**
   * The name of the file containing the dart2js executable on Windows.
   */
  private static final String DART2JS_EXECUTABLE_NAME_WIN = "dart2js.bat"; //$NON-NLS-1$

  /**
   * The name of the dart formatter executable on non-windows operating systems.
   */
  private static final String DARTFMT_EXECUTABLE_NAME = "dartfmt"; //$NON-NLS-1$

  /**
   * The name of the dart formatter executable on windows operating systems.
   */
  private static final String DARTFMT_EXECUTABLE_NAME_WIN = "dartfmt.bat"; //$NON-NLS-1$

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
   * The name of the file within the SDK directory that contains the version number of the SDK.
   */
  private static final String VERSION_FILE_NAME = "version"; //$NON-NLS-1$

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
    if (DEFAULT_SDK == null) {
      File sdkDirectory = getDefaultSdkDirectory();
      if (sdkDirectory == null) {
        return null;
      }
      DEFAULT_SDK = new DirectoryBasedDartSdk(sdkDirectory);
    }
    return DEFAULT_SDK;
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
    libraryMap = initialLibraryMap(useDart2jsPaths);
  }

  @Override
  public Source fromFileUri(URI uri) {
    File file = new File(uri);
    String filePath = file.getAbsolutePath();
    String libPath = getLibraryDirectory().getAbsolutePath();
    if (!filePath.startsWith(libPath + File.separator)) {
      return null;
    }
    filePath = filePath.substring(libPath.length() + 1);
    for (SdkLibrary library : libraryMap.getSdkLibraries()) {
      String libraryPath = library.getPath();
      if (filePath.replace('\\', '/').equals(libraryPath)) {
        String path = library.getShortName();
        try {
          return new FileBasedSource(new URI(path), file);
        } catch (URISyntaxException exception) {
          AnalysisEngine.getInstance().getLogger().logInformation(
              "Failed to create URI: " + path,
              exception);
          return null;
        }
      }
      libraryPath = new File(libraryPath).getParent();
      if (filePath.startsWith(libraryPath + File.separator)) {
        String path = library.getShortName() + "/" + filePath.substring(libraryPath.length() + 1);
        try {
          return new FileBasedSource(new URI(path), file);
        } catch (URISyntaxException exception) {
          AnalysisEngine.getInstance().getLogger().logInformation(
              "Failed to create URI: " + path,
              exception);
          return null;
        }
      }
    }
    return null;
  }

  @Override
  public AnalysisContext getContext() {
    if (analysisContext == null) {
      analysisContext = new SdkAnalysisContext();
      SourceFactory factory = new SourceFactory(new DartUriResolver(this));
      analysisContext.setSourceFactory(factory);
      String[] uris = getUris();
      ChangeSet changeSet = new ChangeSet();
      for (String uri : uris) {
        changeSet.addedSource(factory.forUri(uri));
      }
      analysisContext.applyChanges(changeSet);
    }
    return analysisContext;
  }

  /**
   * Return the file containing the dart2js executable, or {@code null} if it does not exist.
   * 
   * @return the file containing the dart2js executable
   */
  public File getDart2JsExecutable() {
    synchronized (this) {
      if (dart2jsExecutable == null) {
        dart2jsExecutable = verifyExecutable(new File(
            new File(sdkDirectory, BIN_DIRECTORY_NAME),
            OSUtilities.isWindows() ? DART2JS_EXECUTABLE_NAME_WIN : DART2JS_EXECUTABLE_NAME));
      }
    }
    return dart2jsExecutable;
  }

  /**
   * Return the file containing the dart formatter executable, or {@code null} if it does not exist.
   * 
   * @return the file containing the dart formatter executable
   */
  public File getDartFmtExecutable() {
    synchronized (this) {
      if (dartFmtExecutable == null) {
        dartFmtExecutable = verifyExecutable(new File(
            new File(sdkDirectory, BIN_DIRECTORY_NAME),
            OSUtilities.isWindows() ? DARTFMT_EXECUTABLE_NAME_WIN : DARTFMT_EXECUTABLE_NAME));
      }
    }
    return dartFmtExecutable;
  }

  /**
   * Return the file containing the Dartium executable, or {@code null} if it does not exist.
   * 
   * @return the file containing the Dartium executable
   */
  public File getDartiumExecutable() {
    synchronized (this) {
      if (dartiumExecutable == null) {
        dartiumExecutable = verifyExecutable(new File(
            getDartiumWorkingDirectory(),
            getDartiumBinaryName()));
      }
    }
    return dartiumExecutable;
  }

  /**
   * Return the directory where dartium can be found (the directory that will be the working
   * directory is Dartium is invoked without changing the default).
   * 
   * @return the directory where dartium can be found
   */
  public File getDartiumWorkingDirectory() {
    return getDartiumWorkingDirectory(sdkDirectory.getParentFile());
  }

  /**
   * Return the directory where dartium can be found (the directory that will be the working
   * directory is Dartium is invoked without changing the default).
   * 
   * @param installDir the installation directory
   * @return the directory where dartium can be found
   */
  public File getDartiumWorkingDirectory(File installDir) {
    return new File(installDir, DARTIUM_DIRECTORY_NAME);
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
    synchronized (this) {
      if (pubExecutable == null) {
        pubExecutable = verifyExecutable(new File(
            new File(sdkDirectory, BIN_DIRECTORY_NAME),
            OSUtilities.isWindows() ? PUB_EXECUTABLE_NAME_WIN : PUB_EXECUTABLE_NAME));
      }
    }
    return pubExecutable;
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
        File revisionFile = new File(sdkDirectory, VERSION_FILE_NAME);
        try {
          String revision = FileUtilities.getContents(revisionFile);
          if (revision != null) {
            sdkVersion = revision.trim();
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
        vmExecutable = verifyExecutable(new File(
            new File(sdkDirectory, BIN_DIRECTORY_NAME),
            getVmBinaryName()));
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
    String libraryName;
    String relativePath;
    int index = dartUri.indexOf('/');
    if (index >= 0) {
      libraryName = dartUri.substring(0, index);
      relativePath = dartUri.substring(index + 1);
    } else {
      libraryName = dartUri;
      relativePath = "";
    }
    SdkLibrary library = getSdkLibrary(libraryName);
    if (library == null) {
      return null;
    }
    try {
      File file = new File(getLibraryDirectory(), library.getPath());
      if (!relativePath.isEmpty()) {
        file = file.getParentFile();
        file = new File(file, relativePath);
      }
      return new FileBasedSource(new URI(dartUri), file);
    } catch (URISyntaxException exception) {
      return null;
    }
  }

  /**
   * Read all of the configuration files to initialize the library maps.
   * 
   * @param useDart2jsPaths {@code true} if the dart2js path should be used when it is available
   * @return the initialized library map
   */
  protected LibraryMap initialLibraryMap(boolean useDart2jsPaths) {
    File librariesFile = new File(new File(getLibraryDirectory(), INTERNAL_DIR), LIBRARIES_FILE);
    try {
      String contents = FileUtilities.getContents(librariesFile);
      return new SdkLibrariesReader(useDart2jsPaths).readFromFile(librariesFile, contents);
    } catch (Exception exception) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not initialize the library map from " + librariesFile.getAbsolutePath(),
          exception);
      return new LibraryMap();
    }
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
   * Verify that the given executable file exists and is executable.
   * 
   * @param file the binary file
   * @return the file if it exists and is executable, else {@code null}
   */
  @DartExpressionBody("file.isExecutable() ? file : null")
  private File verifyExecutable(File file) {
    return FileUtilities.ensureExecutable(file) ? file : null;
  }
}
