/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Maintains a list of all the used packages (all the versions) used by all of the workspace
 * projects.
 * 
 * @coverage dart.tools.core.pub
 */
public class PubCacheManager_NEW {
  /**
   * Information about a referenced package.
   */
  public static class PackageInfo {
    public final String name;
    public final String version;
    private IProject project;

    public PackageInfo(String name, String version, IProject project) {
      this.name = name;
      this.version = version;
      this.project = project;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof PackageInfo) {
        PackageInfo other = (PackageInfo) obj;
        return Objects.equal(other.name, name) && Objects.equal(other.version, version);
      }
      return false;
    }

    public IProject getProject() {
      return project;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, version);
    }

    @Override
    public String toString() {
      return "PackageInfo(" + name + ", " + version + ", " + project + ")";
    }
  }

  /**
   * Information about a package and version.
   */
  public static class PackageVersion {
    public final String name;
    public final String version;

    public PackageVersion(String name, String version) {
      this.name = name;
      this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof PackageVersion) {
        PackageVersion other = (PackageVersion) obj;
        return Objects.equal(other.name, name) && Objects.equal(other.version, version);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, version);
    }

    @Override
    public String toString() {
      return "PackageVersion(" + name + ", " + version + ")";
    }
  }

  private class FillPubCacheList extends Job {
    public FillPubCacheList() {
      super("Update references packages");
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
      Map<PackageVersion, InstalledPackage> installedPackages = readInstalledPackages();
      // prepare old map
      Map<PackageInfo, PackageInfo> oldPackageMap = Maps.newHashMap();
      for (PackageInfo pkg : packages) {
        oldPackageMap.put(pkg, pkg);
      }
      //
      Set<PackageInfo> newPackages = prepareUsedPackages();
      for (Iterator<PackageInfo> I = newPackages.iterator(); I.hasNext();) {
        PackageInfo newPackage = I.next();
        PackageInfo oldPackage = oldPackageMap.remove(newPackage);
        // old package, copy project
        if (oldPackage != null) {
          newPackage.project = oldPackage.project;
          continue;
        }
        // new package, try to create project
        {
          String projectName = newPackage.name + "_" + newPackage.version;
          InstalledPackage installedPackage = installedPackages.get(new PackageVersion(
              newPackage.name,
              newPackage.version));
          if (installedPackage != null) {
            String projectLocation = installedPackage.location;
            newPackage.project = createExternalProject(projectName, projectLocation);
          }
        }
        // if still no project, remove it
        if (newPackage.project == null) {
          I.remove();
        }
      }
      // remove package projects that are not used anymore
      for (PackageInfo oldPackage : oldPackageMap.keySet()) {
        IProject project = oldPackage.project;
        try {
          project.delete(true, null);
        } catch (CoreException e) {
        }
      }
      // done
      packages = newPackages;
      PubManager.getInstance().notifyListeners((Map<String, Object>) null);
      return Status.OK_STATUS;
    }
  }

  private static class InstalledPackage {
    public final String name;
    public final String version;
    public final String location;

    public InstalledPackage(String name, String version, String location) {
      this.name = name;
      this.version = version;
      this.location = location;
    }
  }

  static final QualifiedName PACKAGE_CACHE_PROJECT = new QualifiedName(
      DartCore.PLUGIN_ID,
      "packageCacheProject");

  private static PubCacheManager_NEW instance;

  public static final synchronized PubCacheManager_NEW getInstance() {
    if (instance == null) {
      instance = new PubCacheManager_NEW();
    }
    return instance;
  }

  public static boolean isPubCacheProject(IProject project) {
    try {
      return project.getPersistentProperty(PACKAGE_CACHE_PROJECT) != null;
    } catch (CoreException e) {
      return false;
    }
  }

  public static boolean isPubCacheResource(IResource resource) {
    if (resource != null) {
      IProject project = resource.getProject();
      return isPubCacheProject(project);
    }
    return false;
  }

  private static IProject createExternalProject(String name, String location) {
    try {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      // prepare project
      IProject project = workspace.getRoot().getProject(name);
      if (project.exists()) {
        return project;
      }
      // prepare description
      IProjectDescription description = workspace.newProjectDescription(name);
      description.setLocation(new Path(location));
      // create the project
      project.create(description, null);
      project.open(null);
      project.setPersistentProperty(PACKAGE_CACHE_PROJECT, "TRUE");
      // done
      return project;
    } catch (CoreException e) {
      DartCore.logError("Unable to create project " + name + " at " + location, e);
      return null;
    }
  }

  private static IProject[] getProjects() {
    try {
      return ResourcesPlugin.getWorkspace().getRoot().getProjects();
    } catch (IllegalStateException e) {
      // The workspace is shutting down so return an empty list
      return new IProject[] {};
    }
  }

  private static String getPubCacheList() {
    RunPubCacheListJob job = new RunPubCacheListJob();
    return job.run(new NullProgressMonitor()).getMessage();
  }

  private Set<PackageInfo> packages = Sets.newHashSet();

  public PubCacheManager_NEW() {
    updatePackagesList(2000);
  }

  public PackageInfo[] getPackages() {
    return packages.toArray(new PackageInfo[packages.size()]);
  }

  public void updatePackagesList(int delay) {
    new FillPubCacheList().schedule(delay);
  }

  /**
   * Parses the given "lockFile" and adds new {@link PackageVersion}s.
   */
  private void addUsedPackages(Set<PackageInfo> usedPackages, IFile lockFile) {
    Map<String, String> versionMap = PubYamlUtils.getPackageVersionMap(lockFile);
    Set<Entry<String, String>> entrySet = versionMap.entrySet();
    for (Entry<String, String> entry : entrySet) {
      String name = entry.getKey();
      String version = entry.getValue();
      usedPackages.add(new PackageInfo(name, version, null));
    }
  }

  /**
   * Returns the {@link Set} of packages used by the workspace projects.
   */
  private Set<PackageInfo> prepareUsedPackages() {
    final Set<PackageInfo> packages = Sets.newHashSet();
    IProject[] projects = getProjects();
    for (IProject project : projects) {
      // ignore artificial package projects
      if (isPubCacheProject(project)) {
        continue;
      }
      // scan and parse all pubspec.lock files
      try {
        project.accept(new IResourceVisitor() {
          @Override
          public boolean visit(IResource resource) throws CoreException {
            if (resource instanceof IFile) {
              IFile file = (IFile) resource;
              if (file.getName().equals(DartCore.PUBSPEC_LOCK_FILE_NAME)) {
                addUsedPackages(packages, file);
              }
              return false;
            }
            return true;
          }
        });
      } catch (Throwable e) {
      }
    }
    return packages;
  }

  private Map<PackageVersion, InstalledPackage> readInstalledPackages() {
    Map<PackageVersion, InstalledPackage> installedPackages = Maps.newHashMap();
    // ask Pub
    String message = getPubCacheList();
    if (message.startsWith("{\"packages")) {
      try {
        Map<String, Object> object = PubYamlUtils.parsePubspecYamlToMap(message);
        @SuppressWarnings("unchecked")
        Map<String, Object> rawMap = (Map<String, Object>) object.get(PubspecConstants.PACKAGES);
        for (Entry<String, Object> pkgEntry : rawMap.entrySet()) {
          String name = pkgEntry.getKey();
          if (pkgEntry.getValue() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawVersionsMap = (Map<String, Object>) pkgEntry.getValue();
            for (Entry<String, Object> versionEntry : rawVersionsMap.entrySet()) {
              String version = versionEntry.getKey();
              @SuppressWarnings("unchecked")
              Map<String, Object> rawPropertiesMap = (Map<String, Object>) versionEntry.getValue();
              String location = (String) rawPropertiesMap.get(PubspecConstants.LOCATION);
              installedPackages.put(new PackageVersion(name, version), new InstalledPackage(
                  name,
                  version,
                  location));
            }
          }
        }
      } catch (Throwable e) {
        DartCore.logError("Error while parsing pub cache list", e);
      }
    } else {
      DartCore.logError(message);
    }
    // done
    return installedPackages;
  }
}
