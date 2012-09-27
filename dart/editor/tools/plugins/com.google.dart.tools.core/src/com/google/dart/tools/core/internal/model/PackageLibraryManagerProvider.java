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

import com.google.common.collect.Lists;
import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.compiler.SystemLibrary;
import com.google.dart.compiler.SystemLibraryProvider;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.Context;
import com.google.dart.tools.core.analysis.SavedContext;
import com.google.dart.tools.core.analysis.index.AnalysisIndexManager;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.core.model.DartSdkManager;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * The class <code>PackageLibraryManagerProvider</code> manages the {@link PackageLibraryManager
 * system library managers} used by the tools.
 */
public class PackageLibraryManagerProvider {

  private static final class MissingSdkLibaryManager extends PackageLibraryManager {

    private static final SystemLibrary[] NO_LIBRARIES = new SystemLibrary[0];
    private static final Collection<String> NO_SPECS = Lists.newArrayList();

    public MissingSdkLibaryManager(File sdkPath, String platformName) {
      super(sdkPath, platformName);
    }

    @Override
    public Collection<String> getAllLibrarySpecs() {
      return NO_SPECS;
    }

    @Override
    public URI getRelativeUri(URI fileUri) {
      return fileUri;
    }

    @Override
    public URI getShortUri(URI uri) {
      return uri;
    }

    @Override
    public Collection<SystemLibrary> getSystemLibraries() {
      return Arrays.asList(getDefaultLibraries());
    }

    @Override
    protected SystemLibrary[] getDefaultLibraries() {
      return NO_LIBRARIES;
    }

    @Override
    protected void initLibraryManager(SystemLibraryProvider libraryProvider) {
      //no SDK means no system libraries
    }

  }

  private static final Object lock = new Object();

  private static PackageLibraryManager ANY_LIBRARY_MANAGER;

  /**
   * Return the manager for VM libraries
   */
  public static PackageLibraryManager getAnyLibraryManager() {
    synchronized (lock) {
      if (ANY_LIBRARY_MANAGER == null) {

        DartSdk sdk = DartSdkManager.getManager().getSdk();
        if (!DartSdkManager.getManager().hasSdk()) {
          DartCore.logError("Missing SDK");
          ANY_LIBRARY_MANAGER = new MissingSdkLibaryManager(null, "any");
          return ANY_LIBRARY_MANAGER;
        }

        File sdkDir = sdk.getDirectory();
        if (!sdkDir.exists()) {
          DartCore.logError("Missing libraries directory: " + sdkDir);
          return null;
        }

        DartCore.logInformation("Reading bundled libraries from " + sdkDir);

        ANY_LIBRARY_MANAGER = new PackageLibraryManager(sdkDir, "any");
        String packageRoot = DartCore.getPlugin().getPrefs().get(
            DartCore.PACKAGE_ROOT_DIR_PREFERENCE,
            "");
        if (packageRoot != null && !packageRoot.isEmpty()) {
          String[] roots = packageRoot.split(";");
          List<File> packageRoots = new ArrayList<File>();
          for (String path : roots) {
            packageRoots.add(new File(path));
          }
          ANY_LIBRARY_MANAGER.setPackageRoots(packageRoots);
        }

      }
    }
    return ANY_LIBRARY_MANAGER;
  }

  /**
   * Answer the server used to analyze source against the "dart-sdk/lib" directory
   */
  public static AnalysisServer getDefaultAnalysisServer() {
    return AnalysisIndexManager.getServer();
  }

  /**
   * Return the default library manager.
   */
  public static PackageLibraryManager getPackageLibraryManager() {
    return getAnyLibraryManager();
  }

  /**
   * Return the default library manager.
   */
  public static PackageLibraryManager getPackageLibraryManager(File file) {
    SavedContext savedContext = getDefaultAnalysisServer().getSavedContext();
    Context context = savedContext.getSuggestedContext(file);
    if (context != null) {
      return context.getLibraryManager();
    } else {
      return getAnyLibraryManager();
    }
  }

  /**
   * Reset the cached library manager. (Required to ensure a re-initialization post SDK
   * upgrade/install).
   */
  public static void resetLibraryManager() {
    ANY_LIBRARY_MANAGER = null;
  }

}
