/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.core.pub;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maintains a list of the latest versions for all packages installed in the local pub cache.
 */
public class PubCacheManager {

  class FillPubCacheList extends Job {

    public FillPubCacheList(String name) {
      super(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public IStatus run(IProgressMonitor monitor) {

      RunPubCacheListJob job = new RunPubCacheListJob();
      String message = job.run(new NullProgressMonitor()).getMessage();
      // no errors
      if (message.startsWith("{\"packages")) {
        Map<String, Object> object = PubYamlUtils.parsePubspecYamlToMap(message);
        Map<String, Object> map = (Map<String, Object>) object.get(PubspecConstants.PACKAGES);
        Map<String, Object> added, removed;
        synchronized (pubCachePackages) {
          added = getPackagesAdded(map);
          removed = getPackagesRemoved(map);
        }
        PubManager.getInstance().notifyListeners(added, removed);
      } else {
        DartCore.logError(message);
      }
      return Status.OK_STATUS;
    }

  }

  /**
   * A map to store the list of the installed packages currently in use and their locations.
   */
  protected final HashMap<String, Object> pubCachePackages = new HashMap<String, Object>();

  private static final PubCacheManager INSTANCE = new PubCacheManager();

  public static final PubCacheManager getInstance() {
    return INSTANCE;
  }

  public HashMap<String, Object> getLocalPackages() {
    synchronized (pubCachePackages) {
      HashMap<String, Object> copy = new HashMap<String, Object>(pubCachePackages);
      return copy;
    }
  }

  public void updatePubCacheList(int delay) {
    new FillPubCacheList("update pub cache list").schedule(delay);
  }

  // callers of this method should synchronize on pubCachePackages
  protected Map<String, Object> getPackagesAdded(Map<String, Object> packages) {
    Map<String, Object> added = new HashMap<String, Object>();
    if (pubCachePackages.isEmpty()) {
      pubCachePackages.putAll(packages);
      added.putAll(packages);
    } else {
      Set<String> keys = packages.keySet();
      Set<String> cacheKeys = pubCachePackages.keySet();
      for (String key : keys) {
        if (!cacheKeys.contains(key)) {
          Object o = packages.get(key);
          added.put(key, o);
          pubCachePackages.put(key, o);
        } else {
          Object o = packages.get(key);
          if (!pubCachePackages.get(key).equals(o)) {
            pubCachePackages.put(key, o);
            added.put(key, o);
          }
        }
      }
    }
    return added;
  }

  //callers of this method should synchronize on pubCachePackages
  protected Map<String, Object> getPackagesRemoved(Map<String, Object> packages) {
    Map<String, Object> removed = new HashMap<String, Object>();
    Set<String> keys = packages.keySet();
    Set<String> cacheKeys = pubCachePackages.keySet();
    for (String key : cacheKeys) {
      if (!keys.contains(key)) {
        removed.put(key, pubCachePackages.get(key));
      }
    }
    for (String key : removed.keySet()) {
      pubCachePackages.remove(key);
    }
    return removed;
  }
}
