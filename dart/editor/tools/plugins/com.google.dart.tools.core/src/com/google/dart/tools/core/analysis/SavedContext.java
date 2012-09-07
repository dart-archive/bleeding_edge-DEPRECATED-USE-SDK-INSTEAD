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

import static com.google.dart.tools.core.analysis.AnalysisUtility.equalsOrContains;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Analysis of Dart source saved on disk
 */
public class SavedContext extends Context {

  /**
   * A mapping of application directories (directories containing a "packages" directory) to current
   * package contexts. This should only be accessed on the background thread.
   */
  private HashMap<File, PackageContext> packageContexts = new HashMap<File, PackageContext>();

  SavedContext(AnalysisServer server, PackageLibraryManager libraryManager) {
    super(server, libraryManager);
  }

  /**
   * TESTING: Answer an array of directories for which a {@link PackageContext} has been defined
   */
  public File[] getApplicationDirectories() {
    return packageContexts.keySet().toArray(new File[packageContexts.keySet().size()]);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  /**
   * Discard all package contexts in addition to all libraries without notifying any listeners about
   * discarded libraries
   */
  @Override
  void discardAllLibraries() {
    packageContexts.clear();
    super.discardAllLibraries();
  }

  /**
   * If the specified file is a directory, then discard all libraries in that directory tree
   * otherwise discard the specified library. In both cases, discard all libraries that directly or
   * indirectly reference the discarded libraries.
   * 
   * @param rootFile the original file or directory to discard
   * @return the collection of discarded libraries (not <code>null</code>, contains no
   *         <code>null</code>s)
   */
  Collection<Library> discardLibraries(File rootFile) {
    ArrayList<Library> discarded = new ArrayList<Library>(40);
    discardLibraries(rootFile, discarded);
    return discarded;
  };

  /**
   * If the specified file is a directory, then discard all libraries and package contexts in that
   * directory tree otherwise discard the specified library. In both cases, discard all libraries
   * that directly or indirectly reference the discarded libraries.
   * 
   * @param rootFile the original file or directory to discard
   * @param discarded the collection of discarded libraries and to which newly discarded libraries
   *          should be added (not <code>null</code>, contains no <code>null</code>s)
   */
  @Override
  void discardLibraries(File rootFile, ArrayList<Library> discarded) {
    super.discardLibraries(rootFile, discarded);
    Iterator<PackageContext> iter = packageContexts.values().iterator();
    while (iter.hasNext()) {
      PackageContext context = iter.next();
      context.discardLibraries(rootFile, discarded);
      if (equalsOrContains(rootFile, context.getApplicationDirectory())) {
        iter.remove();
      }
    }
  };

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
}
