package com.google.dart.tools.core.internal.analysis.model;

import com.google.common.collect.Lists;
import com.google.dart.server.generated.AnalysisServer;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.model.DartIgnoreEvent;
import com.google.dart.tools.core.model.DartIgnoreListener;
import com.google.dart.tools.core.pub.IPackageRootProvider;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * There is a single instance of {@code DartProjectManager} which updates {@link AnalysisServer}'s
 * analysis roots as projects are created, opened, closed, and deleted, and as resources are marked
 * as analyze or don't analyze.
 */
public class DartProjectManager {

  private final IWorkspaceRoot root;
  private final AnalysisServer server;
  private final DartIgnoreManager ignoreManager;

  /**
   * Calls {@link #setAnalysisRoots()} when a project is added or removed.
   */
  IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      if (shouldSetAnalysisRoots(event)) {
        setAnalysisRoots();
      }
    }

    private boolean shouldSetAnalysisRoots(IResourceChangeEvent event) {
      if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
        IResourceDelta delta = event.getDelta();
        // Toplevel project added or removed.
        if (delta.getAffectedChildren(IResourceDelta.REMOVED).length > 0) {
          return true;
        }
        if (delta.getAffectedChildren(IResourceDelta.ADDED).length > 0) {
          IResource resource = delta.getAffectedChildren(IResourceDelta.ADDED)[0].getResource();
          if (resource instanceof IProject && hasPubspecAndLoc((IProject) resource)) {
            return true;
          } else {
            return false;
          }
        }

        for (IResourceDelta child : delta.getAffectedChildren(IResourceDelta.CHANGED)) {
          if (child.findMember(new Path(".settings/com.google.dart.tools.core.prefs")) != null) {
            // Toplevel project had its package root changed.
            // TODO(paulberry): is there a better way to detect this, perhaps with
            // IPreferenceChangeListener?
            return true;
          }
          if (child.findMember(new Path(DartCore.PUBSPEC_LOCK_FILE_NAME)) != null) {
            return true;
          }

          IResource resource = child.getResource();
          // for project open and close
          if (resource instanceof IProject && ((child.getFlags() & IResourceDelta.OPEN) != 0)) {
            return true;
          }
        }
      }
      return false;
    }
  };

  /**
   * Calls {@link #setAnalysisRoots()} when the set of ignored resources has changed.
   */
  DartIgnoreListener ignoreManagerListener = new DartIgnoreListener() {
    @Override
    public void ignoresChanged(DartIgnoreEvent event) {
      setAnalysisRoots();
    }
  };

  public DartProjectManager(IWorkspaceRoot root, AnalysisServer server,
      DartIgnoreManager ignoreManager) {
    this.root = root;
    this.server = server;
    this.ignoreManager = ignoreManager;
  }

  /**
   * Set the collection of resources that should be analyzed.
   */
  public void setAnalysisRoots() {
    List<String> includedPaths = Lists.newArrayList();
    List<String> excludedPaths = Lists.newArrayList();
    HashMap<String, String> packageRoots = new HashMap<String, String>();
    for (IProject proj : root.getProjects()) {
      try {
        boolean hasNature = proj.isOpen() && proj.hasNature(DartCore.DART_PROJECT_NATURE);
        if (hasNature) {
          String projPath = proj.getLocation().toOSString();
          includedPaths.add(projPath);
          File packageRoot = IPackageRootProvider.DEFAULT.getPackageRoot(proj);
          if (packageRoot != null) {
            packageRoots.put(projPath, packageRoot.getPath());
          }
        }
      } catch (CoreException e) {
        DartCore.logError("Failed to determine if project should be analyzed: " + proj.getName(), e);
      }
    }
    for (String path : ignoreManager.getExclusionPatterns()) {
      excludedPaths.add(path.replace('/', File.separatorChar));
    }
    server.analysis_setAnalysisRoots(includedPaths, excludedPaths, packageRoots);
  }

  /**
   * Set the {@link AnalysisServer}'s analysis roots and hook resources and ignore manager to keep
   * the analysis roots updated as things change.
   */
  public void start() {
    setAnalysisRoots();
    root.getWorkspace().addResourceChangeListener(resourceChangeListener);
    ignoreManager.addListener(ignoreManagerListener);
  }

  /**
   * Check that if pubspec is present, there is also a lock file, indicating pub get has run, and
   * project is ready for analysis.
   */
  private boolean hasPubspecAndLoc(IProject proj) {
    if (proj.findMember(DartCore.PUBSPEC_FILE_NAME) == null
        || !DartCore.getPlugin().isAutoRunPubEnabled()) {
      return true;
    }

    if (proj.findMember(DartCore.PUBSPEC_FILE_NAME) != null
        && proj.findMember(DartCore.PUBSPEC_LOCK_FILE_NAME) != null) {
      return true;
    }
    return false;
  }
}
