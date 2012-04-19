/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.core.frog;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.internal.builder.DartBuilder;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartSdk;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.IOException;

/**
 * Launch the frog process and collect stdout, stderr, and exit code information.
 */
public class FrogCompiler {

  public static class CompilationResult {
    private ProcessRunner runner;
    private IPath outputPath;

    CompilationResult(ProcessRunner runner, IPath outputPath) {
      this.runner = runner;
      this.outputPath = outputPath;
    }

    public String getAllOutput() {
      StringBuilder builder = new StringBuilder();

      if (!getStdOut().isEmpty()) {
        builder.append(getStdOut().trim() + "\n");
      }

      if (!getStdErr().isEmpty()) {
        builder.append(getStdErr());
      }

      return builder.toString().trim();
    }

    public int getExitCode() {
      return runner.getExitCode();
    }

    public IPath getOutputPath() {
      return outputPath;
    }

    public String getStdErr() {
      return runner.getStdErr();
    }

    public String getStdOut() {
      return runner.getStdOut();
    }

    @Override
    public String toString() {
      return "frog result=" + getExitCode();
    }
  }

  private static final String FROG_COMPILER_PATH = "frog/frogc.dart";

  /**
   * A static utlility method to handle the common use case for the FrogCompiler class. Compile the
   * given dart library, optionally poll the given monitor to check for user cancellation, and write
   * any output to the given console.
   * 
   * @param library
   * @param monitor
   * @param console
   * @throws OperationCanceledException
   */
  public static CompilationResult compileLibrary(DartLibrary library, IProgressMonitor monitor,
      MessageConsole console) throws CoreException {
    long startTime = System.currentTimeMillis();

    IPath path = library.getCorrespondingResource().getLocation();
    IPath outputPath = DartBuilder.getJsAppArtifactPath(path);

    FrogCompiler compiler = new FrogCompiler();

    console.clear();
    console.println("Generating JavaScript...");

    try {
      CompilationResult result = compiler.compile(library.getCorrespondingResource().getLocation(),
          outputPath, monitor);

      refreshResources(library.getCorrespondingResource());

      if (!result.getStdOut().isEmpty()) {
        console.println(result.getStdOut().trim());
      }

      if (!result.getStdErr().isEmpty()) {
        console.println(result.getStdErr().trim());
      }

      if (result.getExitCode() == 0) {
        long elapsed = System.currentTimeMillis() - startTime;

        // Trim to 1/10th of a second.
        elapsed = (elapsed / 100) * 100;

        File outputFile = outputPath.toFile();
        // Trim to 1/10th of a kb.
        double fileLength = ((int) (((outputFile.length() + 1023) / 1024) * 10)) / 10;

        String message = fileLength + "kb";
        message += " written in " + (elapsed / 1000.0) + " seconds";

        console.println(NLS.bind("Wrote {0} [{1}]", outputFile.getPath(), message));
      }

      return result;
    } catch (IOException ioe) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, ioe.toString(), ioe));
    }
  }

  /**
   * Frog creates java.io.Files; we need to tell the workspace about the new / changed resources.
   * 
   * @param correspondingResource
   * @throws CoreException
   */
  private static void refreshResources(IResource resource) throws CoreException {
    IContainer container;
    if (resource == null) {
      return;
    }

    if (resource instanceof IContainer) {
      container = (IContainer) resource;
    } else {
      container = resource.getParent();
    }

    container.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
  }

  /**
   * Create a new FrogCompiler.
   */
  public FrogCompiler() {

  }

  /**
   * Run frog as a process to compile the given input file to the given output file. If an
   * IProgressMonitor is passed in, it is polled to see if the user cancelled the compile operation.
   * The progress monitor is not used for any other purpose.
   * 
   * @param inputPath
   * @param outputPath
   * @param monitor
   * @return
   * @throws IOException
   * @throws OperationCanceledException if the user cancelled the operation
   */
  public CompilationResult compile(IPath inputPath, IPath outputPath, IProgressMonitor monitor)
      throws IOException {
    ProcessBuilder builder = new ProcessBuilder();

    builder.command(DartSdk.getInstance().getVmExecutable().getPath(), "--new_gen_heap_size=256",
        FROG_COMPILER_PATH, "--compile-only", "--suppress_warnings", "--no_colors", "--libdir="
            + DartSdk.getInstance().getLibraryDirectory().getPath(),
        "--out=" + outputPath.toOSString(), inputPath.toOSString());
    builder.directory(DartSdk.getInstance().getLibraryDirectory());
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder);

    runner.runSync(monitor);

    return new CompilationResult(runner, outputPath);
  }

  public boolean isAvailable() {
    return DartSdk.isInstalled();
  }

}
