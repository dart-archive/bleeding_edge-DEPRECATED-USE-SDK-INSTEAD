/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import java.util.Hashtable;

/**
 * Instances of the class <code>PerProjectInfo</code>
 */
public class PerProjectInfo {
  // private static final int JAVADOC_CACHE_INITIAL_SIZE = 10;

  // private static final DartModelStatus NEED_RESOLUTION = new
  // DartModelStatusImpl();

  private Hashtable<String, String> options;
  // private Object savedState;
  // private boolean triedRead;

  private IEclipsePreferences preferences;
  private IProject project;

  private static final String OUTPUT_LOCATION = "outputLocation";

  // private Hashtable secondaryTypes;

  // private LRUCache javadocCache;

  public PerProjectInfo(IProject project) {
    this.project = project;
    DartCore.notYetImplemented();
    // this.triedRead = false;
    // this.savedState = null;
    // this.javadocCache = new LRUCache(JAVADOC_CACHE_INITIAL_SIZE);
  }

  public void forgetExternalTimestampsAndIndexes() {
    DartCore.notYetImplemented();
    // IClasspathEntry[] classpath = this.resolvedClasspath;
    // if (classpath == null) return;
    // DartModelManager manager = DartModelManager.getInstance();
    // IndexManager indexManager = manager.indexManager;
    // Map externalTimeStamps = manager.deltaState.getExternalLibTimeStamps();
    // HashMap rootInfos = DartModelManager.getDeltaState().otherRoots;
    // for (int i = 0, length = classpath.length; i < length; i++) {
    // IClasspathEntry entry = classpath[i];
    // if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
    // IPath path = entry.getPath();
    // if (rootInfos.get(path) == null) {
    // externalTimeStamps.remove(path);
    // indexManager.removeIndex(path); // force reindexing on next reference
    // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=250083 )
    // }
    // }
    // }
  }

  public Hashtable<String, String> getOptions() {
    return options;
  }

  public IPath getOutputLocation() {
    String pathString = preferences.get(OUTPUT_LOCATION, null);

    if (pathString == null) {
      return null;
    } else {
      return Path.fromPortableString(pathString);
    }
  }

  public IEclipsePreferences getPreferences() {
    return preferences;
  }

  public void setOptions(Hashtable<String, String> newOptions) {
    options = newOptions;
  }

  public void setOutputLocation(IPath path) {
    if (path == null) {
      preferences.remove(OUTPUT_LOCATION);
    } else {
      preferences.put(OUTPUT_LOCATION, path.toPortableString());
    }

    try {
      preferences.flush();
    } catch (BackingStoreException exception) {
      DartCore.logError(exception);
    }
  }

  public void setPreferences(IEclipsePreferences newPreferences) {
    preferences = newPreferences;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Info for "); //$NON-NLS-1$
    builder.append(project.getFullPath());
    builder.append("Output location:\n  "); //$NON-NLS-1$
    IPath outputLocation = getOutputLocation();
    if (outputLocation == null) {
      builder.append("<null>"); //$NON-NLS-1$
    } else {
      builder.append(outputLocation);
    }
    return builder.toString();
  }

}
