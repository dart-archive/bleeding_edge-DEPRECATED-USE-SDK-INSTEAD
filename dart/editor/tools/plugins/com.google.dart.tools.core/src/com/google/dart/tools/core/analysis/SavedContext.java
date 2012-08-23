/*
 * Copyright 2012 Dart project authors.
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

import com.google.dart.compiler.PackageLibraryManager;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

/**
 * Analysis of Dart source saved on disk
 */
public class SavedContext extends Context {

  /**
   * A mapping of directories containing a "packages" to current package contexts. This should only
   * be accessed on the background thread.
   */
  private HashMap<File, PackageContext> packageContexts = new HashMap<File, PackageContext>();

  SavedContext(AnalysisServer server, PackageLibraryManager libraryManager) {
    super(server, libraryManager);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  void discardPackageContext(PackageContext context) {
    packageContexts.remove(context.getApplicationDirectory());
  }

  /**
   * Answer the package context for the specified application directory, creating and caching a new
   * one if one does not already exist.
   * 
   * @param applicationDirectory a directory containing a "packages" directory
   * @return the cached or created package context
   */
  PackageContext getOrCreatePackageContext(File applicationDirectory) {
    PackageContext context = packageContexts.get(applicationDirectory);
    if (context == null) {
      context = new PackageContext(server, applicationDirectory);
      packageContexts.put(applicationDirectory, context);
    }
    return context;
  }

  Collection<PackageContext> getPackageContexts() {
    return packageContexts.values();
  }
}
