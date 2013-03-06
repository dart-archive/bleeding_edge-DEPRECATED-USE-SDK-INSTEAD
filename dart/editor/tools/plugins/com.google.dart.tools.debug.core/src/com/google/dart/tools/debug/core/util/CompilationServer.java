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

package com.google.dart.tools.debug.core.util;

import com.google.dart.engine.element.LibraryElement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.dart2js.Dart2JSCompiler;
import com.google.dart.tools.core.dart2js.Dart2JSCompiler.CompilationResult;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A compilation server for Dart code.
 */
public class CompilationServer {
  private static CompilationServer server = new CompilationServer();

  public static CompilationServer getServer() {
    return server;
  }

  private CompilationServer() {

  }

  public void recompileJavaScriptArtifact(File jsFile) {
    String dartName = jsFile.getName();
    dartName = dartName.substring(0, dartName.length() - ".js".length());

    // Is there a .dart file?
    IPath jsPath = Path.fromOSString(jsFile.getPath());
    IPath dartPath = jsPath.removeLastSegments(1).append(dartName);
    File dartFile = dartPath.toFile();

    if (dartFile.exists()) {
      // Is it a library?
      IFile[] resources = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(
          dartFile.toURI());

      if (resources.length > 0) {
        IFile dartResource = resources[0];

        if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
          LibraryElement library = DartCore.getProjectManager().getLibraryElement(dartResource);

          if (library != null && !library.isUpToDate(jsFile.lastModified())) {
            // Recompile it.
            compile(dartResource, jsFile);
          }
        } else {
          DartElement element = DartCore.create(dartResource);

          if (element instanceof CompilationUnit) {
            CompilationUnit compilationUnit = (CompilationUnit) element;

            // Is the .dart.js file older then any of the .dart files?
            if (needsRecompilation(compilationUnit, jsFile)) {
              // If so, recompile it.
              compile(dartResource, jsFile);
            }
          }
        }
      }
    }
  }

  private void compile(IFile dartFile, File outFile) {
    MessageConsole console = DartCore.getConsole();
    try {
      IPath inputPath = dartFile.getLocation();
      IPath outputPath = Path.fromOSString(outFile.getPath());

      Dart2JSCompiler compiler = new Dart2JSCompiler();

      console.printSeparator("Compiling " + dartFile.getFullPath() + "...");

      CompilationResult result = compiler.compile(
          inputPath,
          outputPath,
          new NullProgressMonitor(),
          console);

      String output = result.getAllOutput();

      if (output.length() > 0) {
        console.println(output);
      }
    } catch (IOException ex) {
      console.println("Error while compiling: " + ex.toString());

      DartDebugCorePlugin.logError(ex);
    }
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
      List<CompilationUnit> compilationUnits = compilationUnit.getLibrary().getCompilationUnitsTransitively();

      for (File sourceFile : getFilesFor(compilationUnits)) {
        if (sourceFile.lastModified() > outputFile.lastModified()) {
          return true;
        }
      }

      return false;
    } catch (DartModelException e) {
      return false;
    }
  }

}
