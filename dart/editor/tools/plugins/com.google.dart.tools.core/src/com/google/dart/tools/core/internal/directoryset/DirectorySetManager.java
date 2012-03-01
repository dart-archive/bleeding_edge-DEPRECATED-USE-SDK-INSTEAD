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
package com.google.dart.tools.core.internal.directoryset;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A manager for the set of top-level directories displayed in the Files view.
 * 
 * @see DirectorySetEvent
 * @see DirectorySetListener
 */
public class DirectorySetManager {

  private static String DIR_SET = "filesViewDirectorySet";

  private static String PATH_SEPARATOR = ";";

  private static DirectorySetManager instance = new DirectorySetManager();

  public static DirectorySetManager getInstance() {
    return instance;
  }

  private Set<String> pathSet;

  private IEclipsePreferences prefs;

  private ArrayList<DirectorySetListener> listeners;

  private DirectorySetEvent directorySetEvent;

  private DirectorySetManager() {
    String defaultDirectories = getDefaultDirectorySet();
    prefs = DartCore.getPlugin().getPrefs();
    String dirSetStr = prefs.get(DIR_SET, defaultDirectories);

    pathSet = new HashSet<String>();
    String[] pathArray = dirSetStr.split(PATH_SEPARATOR);
    for (String strPath : pathArray) {
      File f = new File(strPath);
      if (f.exists() && !f.isHidden()) {
        pathSet.add(strPath);
      }
    }
    prefs.put(DIR_SET, toString());
    listeners = new ArrayList<DirectorySetListener>();
  }

  public void addListener(DirectorySetListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  public boolean addPath(String str) {
    boolean result = false;
    if (str != null && str.length() > 0) {
      File f = new File(str);
      if (f.exists() && !f.isHidden()) {
        ArrayList<String> pathsToRemove = new ArrayList<String>();
        for (String path : pathSet) {
          if (str.equals(path) || str.startsWith(path + '/')) {
            // The new path is the same as or a descendant of a directory that is already in the set
            // so there is no need to add it.
            return false;
          } else if (path.startsWith(str + '/')) {
            // The new path is an ancestor of a directory that is already in the set so the old path
            // should be removed and the new path added.
            pathsToRemove.add(path);
          }
        }
        result = pathSet.removeAll(pathsToRemove) | pathSet.add(str);
        if (result) {
          saveAndFire();
        }
      }
    }
    return result;
  }

  public void fire() {
    synchronized (listeners) {
      for (int i = 0; i < listeners.size(); i++) {
        DirectorySetListener listener = listeners.get(i);
        // if directorySetEvent is null, initialize it
        if (directorySetEvent == null) {
          directorySetEvent = new DirectorySetEvent();
        }
        // fire the event for this listener
        listener.directorySetChange(directorySetEvent);
      }
    }
  }

  public File[] getChildren() {
    String[] strArray = pathSet.toArray(new String[pathSet.size()]);
    File[] fileArray = new File[strArray.length];
    for (int i = 0; i < strArray.length; i++) {
      fileArray[i] = new File(strArray[i]);
    }
    return fileArray;
  }

  public boolean hasPath(String str) {
    return pathSet.contains(str);
  }

  public void removeListener(DirectorySetListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  public boolean removePath(String str) {
    boolean result = false;
    if (str != null) {
      result = pathSet.remove(str);
      // if the path was removed, then update the preference
      if (result) {
        saveAndFire();
      }
    }
    return result;
  }

  @Override
  public String toString() {
    String[] pathArray = pathSet.toArray(new String[pathSet.size()]);
    String result = "";
    for (int i = 0; i < pathArray.length; i++) {
      if (i == pathArray.length) {
        result += pathArray[i];
      } else {
        result += pathArray[i] + PATH_SEPARATOR;
      }
    }
    return result;
  }

  private String getDefaultDirectorySet() {
    // This commented out source code can be used for manually testing the Files view.
//    String homeDir = System.getProperty("user.home");
//    String samplesStrDir = DartSdk.getInstallDirectory().getAbsolutePath() + File.separator
//        + "samples";
//    String librariesStrDir = DartSdk.getInstallDirectory().getAbsolutePath() + File.separator
//        + "libraries";
//    String result = "";
//    if (new File(homeDir).exists()) {
//      result = homeDir;
//    }
//    // append samples directory
//    File samplesFile = new File(samplesStrDir);
//    if (samplesFile.exists() && !samplesFile.isHidden()) {
//      if (result.length() > 0) {
//        result += PATH_SEPARATOR;
//      }
//      result += samplesStrDir;
//    }
//    // append libraries directory
//    File librariesFile = new File(librariesStrDir);
//    if (librariesFile.exists() && !librariesFile.isHidden()) {
//      if (result.length() > 0) {
//        result += PATH_SEPARATOR;
//      }
//      result += librariesStrDir;
//    }
//    return result;
    return "";
  }

  private void saveAndFire() {
    prefs.put(DIR_SET, toString());
    fire();
  }

}
