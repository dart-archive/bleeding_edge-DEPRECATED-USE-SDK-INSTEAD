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
package com.google.dart.tools.core;

import com.google.dart.tools.core.model.DartSdk;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * A manager for the set of top-level directories displayed in the Files view.
 */
public class DirectorySetManager {

  private static String DIR_SET = "filesViewDirectorySet";

  private static String PATH_SEPARATOR = ";";

  static DirectorySetManager instance = new DirectorySetManager();

  private Set<String> pathSet;

  private IEclipsePreferences prefs;

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
  }

  public boolean addPath(String str) {
    boolean result = false;
    if (str != null && str.length() > 0) {
      File f = new File(str);
      if (f.exists() && !f.isHidden()) {
        result = pathSet.add(str);
        prefs.put(DIR_SET, toString());
      }
    }
    return result;
  }

  public File[] getChildren() {
    String[] strArray = pathSet.toArray(new String[pathSet.size()]);
    File[] fileArray = new File[strArray.length];
    for (int i = 0; i < strArray.length; i++) {
      fileArray[i] = new File(strArray[i]);
    }
    return fileArray;
  }

  public boolean removePath(String str) {
    boolean result = false;
    if (str != null) {
      result = pathSet.remove(str);
      // if the path was removed, then update the preference
      if (result) {
        prefs.put(DIR_SET, toString());
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
    String homeDir = System.getProperty("user.home");
    String samplesStrDir = DartSdk.getInstallDirectory().getAbsolutePath() + File.separator
        + "samples";
    String result = "";
    if (new File(homeDir).exists()) {
      result = homeDir;
    }
    File samplesFile = new File(samplesStrDir);
    if (samplesFile.exists() && !samplesFile.isHidden()) {
      if (result.length() > 0) {
        result += PATH_SEPARATOR;
      }
      result += samplesStrDir;
    }
    return result;
  }

}
