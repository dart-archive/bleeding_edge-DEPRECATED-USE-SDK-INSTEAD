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
package com.google.dart.tools.core.analysis;

import com.google.common.collect.Lists;
import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.tools.core.DartCore;

import java.io.File;

/**
 * A context for analyzing applications with a "packages" directory and a "pubspec.yaml" file.
 * Analysis of libraries directly or indirectly referenced by this context's applications are cached
 * in this context.
 */
public class PackageContext extends Context {

  /**
   * The directory containing a "packages" directory and a pubspec.yaml
   * 
   * @see DartCore#isApplicationDirectory(File)
   */
  private final File applicationDirectory;

  PackageContext(AnalysisServer server, File applicationDirectory) {
    super(server, new PackageLibraryManager());
    this.applicationDirectory = applicationDirectory;
    File packagesDir = new File(applicationDirectory, DartCore.PACKAGES_DIRECTORY_NAME);
    getLibraryManager().setPackageRoots(Lists.newArrayList(packagesDir));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + applicationDirectory + "]";
  }

  @Override
  File getApplicationDirectory() {
    return applicationDirectory;
  }
}
