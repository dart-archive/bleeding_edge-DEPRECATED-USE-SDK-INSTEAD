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
package com.google.dart.tools.update.core.internal.jobs;

import com.google.dart.tools.update.core.UpdateCore;
import com.google.dart.tools.update.core.internal.UpdateUtils;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A job to cleanup installation (notably to harvest stale plugins).
 */
public class CleanupInstallationJob extends Job {

  /**
   * Sorts files in reverse lexical order, ensuring that the most recent bundle id is first.
   * http://wiki.eclipse.org/index.php/Version_Numbering
   */
  private static final Comparator<File> BUNDLE_SORTER = new Comparator<File>() {
    @Override
    public int compare(File f1, File f2) {

      try {

        return lexicalCompareBundleFileNames(f1.getName(), f2.getName());

      } catch (IllegalArgumentException e) {
        UpdateCore.logError("error parsing bundle versions", e);
      }

      //fall back to lexicographic ordering
      return f2.getName().compareTo(f1.getName());
    }
  };

  /**
   * Threshold for how many versions of a bundle to keep around.
   */
  private static final int MATCH_THRESHOLD = 2;

  //public for testing
  public static final int lexicalCompareBundleFileNames(String b1, String b2)
      throws IllegalArgumentException {

    if (b1.equalsIgnoreCase(b2)) {
      return 0;
    }

    Version v1 = Version.parseVersion(getBundleVersionDetails(b1));
    Version v2 = Version.parseVersion(getBundleVersionDetails(b2));

    return v1.compareTo(v2);
  }

  /**
   * Builds a map of (unqualified) bundle names to lists of bundle files. An example entry might
   * look like this: "org.junit" => [org.junit_4.8.2.v4_8_2_v20110321-1705.jar,
   * org.junit_3.8.2.v3_8_2_v20100427-1100.jar]
   */
  private static Map<String, List<File>> buildBundleMap(File bundleDir) {

    File[] files = bundleDir.listFiles();

    Map<String, List<File>> map = new HashMap<String, List<File>>();

    for (File file : files) {
      String name = getBundleName(file);
      List<File> matches = map.get(name);
      if (matches == null) {
        matches = new ArrayList<File>();
        map.put(name, matches);
      }
      matches.add(file);
    }

    return map;
  }

  private static List<File> findStaleBundles(File bundleDir) {

    Map<String, List<File>> bundleMap = buildBundleMap(bundleDir);
    List<File> duplicates = indentifyStaleBundles(bundleMap);

    return duplicates;
  }

  private static String getBundleName(File file) {
    //strips off version suffix info
    //for example: org.eclipse.osgi_3.7.2.v20120110-1415.jar => org.eclipse.osgi
    String name = file.getName();
    return name.split("_[0-9].*")[0];
  }

  private static String getBundleVersionDetails(String bundleName) {
    //strips off name prefix and extension suffix info
    //for example: org.eclipse.osgi_3.7.2.v20120110-1415.jar => 3.7.2.v20120110-1415
    String version = bundleName.indexOf('_') != -1 ? bundleName.split("_")[1] : bundleName;
    return version.indexOf(".jar") != -1 ? version.substring(0, version.lastIndexOf(".jar"))
        : version;
  }

  private static File getInstallDir() throws IOException {
    return FileLocator.getBundleFile(UpdateCore.getInstance().getBundle()).getParentFile();
  }

  private static Bundle[] getInstalledBundles() {
    return UpdateCore.getInstance().getBundle().getBundleContext().getBundles();
  }

  private static List<File> indentifyStaleBundles(Map<String, List<File>> map) {

    List<File> staleBundles = new ArrayList<File>();

    for (List<File> matches : map.values()) {
      if (matches.size() > MATCH_THRESHOLD) {
        Collections.sort(matches, BUNDLE_SORTER);
        for (File match : matches.subList(MATCH_THRESHOLD, matches.size())) {
          //sanity check to ensure we don't try and remove an active bundle file
          if (!installedBundle(match)) {
            staleBundles.add(match);
          }
        }
      }
    }

    return staleBundles;
  }

  private static boolean installedBundle(File bundleFile) {
    try {
      for (Bundle bundle : getInstalledBundles()) {
        if (FileLocator.getBundleFile(bundle).equals(bundleFile)) {
          return true;
        }
      }
    } catch (IOException e) {
      //since we can't be sure, err on the safe side
      return true;
    }
    return false;
  }

  public CleanupInstallationJob() {
    super(UpdateJobMessages.CleanupInstallationJob_label);
    setSystem(true);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {

    try {

      List<File> staleBundles = findStaleBundles(getInstallDir());

      monitor.beginTask("Removing unused bundles", staleBundles.size());
      for (File file : staleBundles) {
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        UpdateUtils.delete(file, monitor);
      }
      monitor.done();

    } catch (IOException e) {
      //don't surface the error to the user since there's nothing they can do
      //(moreover, we'll get another chance to cleanup on restart)
      UpdateCore.logError(e);
    }

    return Status.OK_STATUS;
  }

}
