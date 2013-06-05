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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maintains a list of the latest versions for all packages installed in the local pub cache.
 */
public class PubCacheManager {

  protected class FillPubCacheList extends Job {

    Collection<String> packages = null;

    public FillPubCacheList(String name, Collection<String> packages) {
      super(name);
      this.packages = packages;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IStatus run(IProgressMonitor monitor) {

      String message = getPubCacheList();
      // no errors
      if (message.startsWith("{\"packages")) {
        Map<String, Object> object = PubYamlUtils.parsePubspecYamlToMap(message);
        synchronized (pubUsedPackages) {
          pubCachePackages = (HashMap<String, Object>) object.get(PubspecConstants.PACKAGES);
        }
        if (packages == null) {
          initializeList();
          if (!pubUsedPackages.isEmpty()) {
            PubManager.getInstance().notifyListeners(getLocalPackages());
          }
        } else {
          Map<String, Object> added = processPackages(packages);
          if (!added.isEmpty()) {
            PubManager.getInstance().notifyListeners(added);
          }
        }
      } else {
        DartCore.logError(message);
      }
      return Status.OK_STATUS;
    }

  }

  /**
   * A map to store the list of the installed packages and their locations, synchronize on
   * pubUsedPackages
   */
  protected HashMap<String, Object> pubCachePackages = new HashMap<String, Object>();

  /**
   * A map of packages & locations used in the open folders in the editor, access should be
   * synchronized against itself
   */
  protected final HashMap<String, Object> pubUsedPackages = new HashMap<String, Object>();

  private static final PubCacheManager INSTANCE = new PubCacheManager();

  public static final PubCacheManager getInstance() {
    return INSTANCE;
  }

  public HashMap<String, Object> getLocalPackages() {
    synchronized (pubUsedPackages) {
      HashMap<String, Object> copy = new HashMap<String, Object>(pubUsedPackages);
      return copy;
    }
  }

  public void updatePackagesList(int delay) {
    updatePackagesList(delay, null);
  }

  public void updatePackagesList(int delay, Collection<String> packages) {
    new FillPubCacheList("update installed packages", packages).schedule(delay);
  }

  protected IProject[] getProjects() {
    return ResourcesPlugin.getWorkspace().getRoot().getProjects();
  }

  protected String getPubCacheList() {
    RunPubCacheListJob job = new RunPubCacheListJob();
    return job.run(new NullProgressMonitor()).getMessage();
  }

  protected void processLockFileContents(IResource resource) {
    Map<String, String> versionMap = PubYamlUtils.getPackageVersionMap(resource);
    if (versionMap != null && !versionMap.isEmpty()) {
      synchronized (pubUsedPackages) {
        for (String key : versionMap.keySet()) {
          Object object = pubCachePackages.get(key);
          if (object != null) {
            pubUsedPackages.put(key, pubCachePackages.get(key));
          }
        }
      }

    }
  }

  protected Map<String, Object> processPackages(Collection<String> packages) {
    Map<String, Object> added = new HashMap<String, Object>();
    synchronized (pubUsedPackages) {
      Set<String> keySet = pubUsedPackages.keySet();
      for (String packageName : packages) {
        if (!keySet.contains(packageName)) {
          Object object = pubCachePackages.get(packageName);
          if (object != null) {
            pubUsedPackages.put(packageName, pubCachePackages.get(packageName));
            added.put(packageName, pubCachePackages.get(packageName));
          }
        }
      }
    }
    return added;
  }

  private void initializeList() {
    IProject[] projects = getProjects();
    for (IProject project : projects) {
      try {
        project.accept(new IResourceVisitor() {

          @Override
          public boolean visit(IResource resource) throws CoreException {

            if (resource.getType() != IResource.FILE) {
              return true;
            }
            if (resource.getName().equals(DartCore.PUBSPEC_LOCK_FILE_NAME)) {
              processLockFileContents(resource);
            }
            return false;
          }
        });
      } catch (CoreException e) {
        // do nothing
      }
    }

  }
}
