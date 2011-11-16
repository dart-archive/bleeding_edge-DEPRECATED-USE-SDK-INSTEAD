/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.compiler.UrlDartSource;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import java.io.File;
import java.io.IOException;

/**
 * A singleton which caches artifacts for the session
 */
public class RootArtifactProvider extends CachingArtifactProvider {

  /**
   * Internal class for pruning the cached artifacts based upon workspace lifecycle events. This is
   * necessary because the resource path is incorrect for linked resources that have been removed
   * thus we must use a workspace {@link LifecycleListener} to detect linked resources just prior to
   * removal.
   */
  @SuppressWarnings("restriction")
  private static class LifecycleListener implements
      org.eclipse.core.internal.events.ILifecycleListener {

    static void hookListener() {
      org.eclipse.core.internal.resources.Workspace workspace;
      workspace = (org.eclipse.core.internal.resources.Workspace) ResourcesPlugin.getWorkspace();
      workspace.addLifecycleListener(new LifecycleListener());
    }

    @Override
    public void handleEvent(org.eclipse.core.internal.events.LifecycleEvent event)
        throws CoreException {
      if ((event.kind & org.eclipse.core.internal.events.LifecycleEvent.PRE_LINK_DELETE) > 0
          || (event.kind & org.eclipse.core.internal.events.LifecycleEvent.PRE_PROJECT_DELETE) > 0) {
        IResource res = event.resource;
        if (res != null) {
          remove(res);
        }
      }
    }

    protected void remove(IResource res) throws CoreException {
      RootArtifactProvider provider = INSTANCE;
      while (provider != null) {
        provider.removeResource(res);
        provider = provider.nextProvider;
      }
    }

  }

  private static final Object lock = new Object();
  private static RootArtifactProvider INSTANCE;

  /**
   * Answer the root artifact provider shared by all throughout the session.
   */
  public static RootArtifactProvider getInstance() {
    synchronized (lock) {
      if (INSTANCE == null) {
        INSTANCE = new RootArtifactProvider();
        loadArtifacts();
        LifecycleListener.hookListener();
      }
    }
    return INSTANCE;
  }

  /**
   * Answer a new instance for testing purposes only. Tests must call {@link #dispose()} when the
   * test is complete.
   */
  public static RootArtifactProvider newInstanceForTesting() {
    RootArtifactProvider newProvider = new RootArtifactProvider();
    RootArtifactProvider root = getInstance();
    synchronized (lock) {
      newProvider.nextProvider = root.nextProvider;
      root.nextProvider = newProvider;
    }
    return newProvider;
  }

  /**
   * Save the artifacts cached by the root artifact provider
   */
  public static void shutdown() {
    synchronized (lock) {
      if (INSTANCE != null) {
        saveArtifacts();
      }
    }
  }

  /**
   * Answer the artifact.cache file
   * 
   * @return the file (may not exist) or <code>null</code> if it could not be determined
   */
  private static File getArtifactCacheFile() {
    return DartCore.getPlugin().getStateLocation().append("artifact.cache").toFile();
  }

  /**
   * Load artifacts from disk if they were cached from the prior session
   */
  private static void loadArtifacts() {
    File cacheFile = getArtifactCacheFile();
    if (cacheFile == null) {
      return;
    }
    if (!cacheFile.exists()) {
      if (DartCoreDebug.WARMUP) {
        DartCore.logInformation("No cached artifacts file " + cacheFile);
      }
      return;
    }
    int artifactCount;
    long delta;
    try {
      long start = System.currentTimeMillis();
      artifactCount = INSTANCE.loadCachedArtifacts(cacheFile);
      delta = System.currentTimeMillis() - start;
    } catch (IOException e) {
      DartCore.logError("Load cached artifacts failed", e);
      return;
    }
    if (DartCoreDebug.WARMUP) {
      DartCore.logInformation("Loaded " + artifactCount + " cached artifacts in " + delta
          + " ms from " + cacheFile);
    }
  }

  /**
   * Save artifacts to disk to be loaded during the next session's warmup
   */
  private static void saveArtifacts() {
    File cacheFile = getArtifactCacheFile();
    if (cacheFile == null) {
      return;
    }
    int artifactCount;
    long delta;
    try {
      long start = System.currentTimeMillis();
      artifactCount = INSTANCE.saveCachedArtifacts(cacheFile);
      delta = System.currentTimeMillis() - start;
    } catch (IOException e) {
      DartCore.logError("Save artifacts failed: " + cacheFile, e);
      return;
    }
    if (DartCoreDebug.WARMUP) {
      DartCore.logInformation("Saved " + artifactCount + " artifacts in " + delta + " ms to "
          + cacheFile);
    }
  }

  /**
   * A linked list of providers starting with {@link #INSTANCE} or <code>null</code> if this is the
   * last in the list.
   * 
   * @see #hookChangeListener()
   * @see #dispose()
   */
  private RootArtifactProvider nextProvider;

  private RootArtifactProvider() {
  }

  /**
   * Remove the receiver from the listener's list so that it can be garbage collected. This should
   * not be called on the instance returned by {@link #getInstance()} because it exists for the
   * entire session and should not be disposed.
   * 
   * @see #newInstanceForTesting()
   */
  public void dispose() {
    synchronized (lock) {
      RootArtifactProvider provider = INSTANCE;
      while (provider != null) {
        if (provider.nextProvider == this) {
          provider.nextProvider = nextProvider;
          break;
        }
        provider = provider.nextProvider;
      }
    }
  }

  private void removeContainer(IContainer container) throws CoreException {
    if (!container.exists()) {
      return;
    }
    IResource[] members = container.members();
    if (members != null) {
      for (IResource res : members) {
        removeResource(res);
      }
    }
  }

  private void removeFile(IFile file) throws DartModelException {
    if (!file.exists()) {
      return;
    }
    DartElementImpl elem = DartModelManager.getInstance().create(file);
    if (elem instanceof CompilationUnitImpl) {
      CompilationUnitImpl unit = (CompilationUnitImpl) elem;
      DartLibraryImpl lib = (DartLibraryImpl) unit.getLibrary();
      if (unit.equals(lib.getDefiningCompilationUnit())) {
        CompilationUnit[] children = lib.getCompilationUnits();
        for (CompilationUnit child : children) {
          removeUnit((CompilationUnitImpl) child);
        }
      } else {
        removeUnit(unit);
      }
    }
  }

  private void removeResource(IResource res) throws CoreException {
    switch (res.getType()) {
      case IResource.PROJECT:
      case IResource.FOLDER:
        removeContainer((IContainer) res);
        break;
      case IResource.FILE:
        removeFile((IFile) res);
        break;
      default:
        break;
    }
  }

  private void removeUnit(CompilationUnitImpl unit) {
    IResource resource = unit.getResource();
    if (resource == null) {
      return;
    }
    IPath location = resource.getLocation();
    if (location == null) {
      return;
    }
    File srcFile = location.toFile();
    DartLibraryImpl lib = (DartLibraryImpl) unit.getLibrary();
    UrlDartSource dartSrc = new UrlDartSource(srcFile, lib.getLibrarySourceFile());
    removeArtifactsFor(dartSrc);
  }
}
