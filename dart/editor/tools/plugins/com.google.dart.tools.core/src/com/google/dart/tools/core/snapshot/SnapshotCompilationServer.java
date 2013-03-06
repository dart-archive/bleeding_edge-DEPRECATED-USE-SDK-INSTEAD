/*
 * Copyright 2013 Dart project authors.
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

package com.google.dart.tools.core.snapshot;

import com.google.dart.engine.element.LibraryElement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class uses the {@link SnapshotCompiler} class to automatically recompile Dart snapshots if
 * any of the referenced source has changed.
 * 
 * @see SnapshotCompiler
 */
public class SnapshotCompilationServer {
  private File sourceFile;

  /**
   * Create a compilation server which can recompile the given Dart source file.
   * 
   * @param sourceFile
   */
  public SnapshotCompilationServer(File sourceFile) {
    if (!DartCore.isDartLikeFileName(sourceFile.getName())) {
      throw new IllegalArgumentException("expected dart file");
    }

    this.sourceFile = sourceFile;
  }

  /**
   * Check if a re-compilation is necessary and perform the recompile.
   * 
   * @return
   */
  public IStatus compile() {
    if (needsRecompilation()) {
      return performCompile();
    }

    return Status.OK_STATUS;
  }

  /**
   * @return the destination snapshot file
   */
  public File getDestFile() {
    return SnapshotCompiler.createDestFileName(getSourceFile());
  }

  /**
   * @return the source Dart file
   */
  public File getSourceFile() {
    return sourceFile;
  }

  /**
   * Return whether the snapshot file needs to be recompiled (i.e., if any of its source files are
   * newer then it).
   * <p>
   * The compile() method will call this automatically.
   * 
   * @return whether the snapshot file needs to be recompiled
   */
  public boolean needsRecompilation() {
    if (!getDestFile().exists()) {
      return true;
    }

    if (!getSourceFile().exists()) {
      return false;
    }

    IFile[] resources = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(
        sourceFile.toURI());

    if (resources.length > 0) {
      IFile file = resources[0];

      if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
        LibraryElement library = DartCore.getProjectManager().getLibraryElement(file);

        if (library == null) {
          return true;
        } else {
          return !library.isUpToDate(getDestFile().lastModified());
        }
      } else {
        // TODO: this call can take a long time
        DartElement element = DartCore.create(file);

        if (element instanceof CompilationUnit) {
          CompilationUnit compilationUnit = (CompilationUnit) element;

          // Is the .snapshot file older then any of the .dart files?
          if (needsRecompilation(compilationUnit, getDestFile())) {
            return true;
          }
        }
      }
    }

    return false;
  }

  protected IStatus performCompile() {
    SnapshotCompiler compiler = new SnapshotCompiler();

    return compiler.compile(getSourceFile(), getDestFile());
  }

  @Deprecated
  private List<File> getFilesFor(List<CompilationUnit> compilationUnits) {
    Set<File> files = new HashSet<File>();

    for (CompilationUnit unit : compilationUnits) {
      IResource resource = unit.getResource();

      if (resource != null) {
        if (resource.getLocation() != null) {
          File file = resource.getLocation().toFile();

          if (file != null && file.exists()) {
            files.add(file);
          }
        }
      } else {
        // Check for non-workspace (external) CompilationUnits.
        IPath path = unit.getPath();

        File file = path.toFile();

        if (file != null && file.exists()) {
          files.add(file);
        }
      }
    }

    return new ArrayList<File>(files);
  }

  @Deprecated
  private boolean needsRecompilation(CompilationUnit compilationUnit, File outputFile) {
    if (outputFile == null || !outputFile.exists()) {
      return true;
    }

    try {
      // TODO: this call can take a long time
      List<CompilationUnit> compilationUnits = compilationUnit.getLibrary().getCompilationUnitsTransitively();

      for (File sourceFile : getFilesFor(compilationUnits)) {
        if (sourceFile.lastModified() > outputFile.lastModified()) {
          return true;
        }
      }

      return false;
    } catch (DartModelException e) {
      DartCore.logError(e);

      return true;
    }
  }

}
