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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.util.Extensions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

import java.io.File;
import java.util.Map;

/**
 * Instances of the class <code>DartBuilder</code> implement the incremental builder for Dart
 * projects.
 */
public class DartBuilder extends IncrementalProjectBuilder {

  /**
   * Answer the JavaScript application file for the specified source.
   * 
   * @param source the application source file (not <code>null</code>)
   * @return the application file (may not exist)
   */
  public static File getJsAppArtifactFile(IPath sourceLocation) {
    return sourceLocation.addFileExtension(DartCore.EXTENSION_JS).toFile();
  }

  /**
   * Answer the JavaScript application file for the specified source.
   * 
   * @param source the application source file (not <code>null</code>)
   * @return the application file (may not exist)
   */
  public static File getJsAppArtifactFile(IResource source) {
    return getJsAppArtifactFile(source.getLocation());
  }

  /**
   * Answer the JavaScript application file path for the specified source.
   * 
   * @param source the application source file (not <code>null</code>)
   * @return the application file path (may not exist)
   */
  public static IPath getJsAppArtifactPath(IPath libraryPath) {
    return Path.fromOSString(getJsAppArtifactFile(libraryPath).getAbsolutePath());
  }

  private final DartcBuildHandler dartcBuildHandler = new DartcBuildHandler();

  private boolean firstBuildThisSession = true;

  @SuppressWarnings("rawtypes")
  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {

    // Use AnalysisServer instead
    if (DartCoreDebug.ANALYSIS_SERVER) {
      return new IProject[] {};
    }

    boolean compileWithFrog = DartCore.getPlugin().getCompileWithFrog();

    // TODO(keertip) : remove call to dartc if frog is being used, once indexer is independent
    // If building using frog, then dartc does not produce any js files
    if (firstBuildThisSession || hasDartSourceChanged()) {
      dartcBuildHandler.buildAllApplications(getProject(), !compileWithFrog, monitor);

      if (firstBuildThisSession) {
        firstBuildThisSession = false;
        dartcBuildHandler.triggerDependentBuilds(getProject(), SubMonitor.convert(monitor, 100));
      }

      monitor.done();
    }

    // Return the projects upon which this project depends
    return dartcBuildHandler.getPrerequisiteProjects();
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    dartcBuildHandler.clean(getProject(), monitor);
  }

  /**
   * Obtain the current resource changed delta(s) to determine if any of the resources that have
   * changed were Dart related source files.
   * 
   * @return <code>true</code> if at least one Dart related source file has changed.
   */
  protected boolean hasDartSourceChanged() throws CoreException {
    if (hasDartSourceChanged(getDelta(getProject()))) {
      return true;
    }
    // TODO(keertip): fix this once dartc is no longer being called
    for (IProject project : dartcBuildHandler.getPrerequisiteProjects()) {
      if (hasDartSourceChanged(getDelta(project))) {
        return true;
      }
    }
    return false;
  }

  private boolean hasDartSourceChanged(IResourceDelta delta) throws CoreException {
    if (delta == null) {
      return true;
    }
    final boolean shouldBuild[] = new boolean[1];
    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) {
        IResource resource = delta.getResource();
        if (resource.getType() != IResource.FILE) {
          // Visit children only if we have not already found a changed source file
          return !shouldBuild[0];
        }
        String name = resource.getName();
        if (name.endsWith(Extensions.DOT_DART)) {
          shouldBuild[0] = true;
        }
        return false;
      }
    });
    return shouldBuild[0];
  }

//  private void queueFilesForIndexer(Collection<LibraryUnit> libraries) {
//    ArrayList<IndexingTarget> targets = new ArrayList<IndexingTarget>();
//    for (LibraryUnit libraryUnit : libraries) {
//      DartLibrary library = null; //DartModelManager.getInstance().getLibraryWithUri(libraryUnit.getSource().getUri());
//      if (library != null) {
//        // TODO(brianwilkerson) Remove the enclosing test once the indexer is no longer tied to
//        // resources.
//        for (DartUnit unit : libraryUnit.getUnits()) {
//          if (!unit.isDiet()) {
//            CompilationUnit compilationUnit = library.getCompilationUnit(unit.getSourceName());
//            targets.add(new CompilationUnitIndexingTarget(compilationUnit, unit));
//          }
//        }
//      }
//    }
//    StandardDriver.getInstance().enqueueTargets(targets.toArray(new IndexingTarget[targets.size()]));
//  }
}
