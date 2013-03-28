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

package com.google.dart.tools.core.dart2js;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.pub.IPackageRootProvider;
import com.google.dart.tools.core.utilities.general.StringUtilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Launch the dart2js process and collect stdout, stderr, and exit code information.
 */
public class Dart2JSCompiler {

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
      return "dart2js result=" + getExitCode();
    }
  }

  /**
   * A static utility method to handle the common use case for the Dart2JSCompiler class. Compile
   * the given dart library, optionally poll the given monitor to check for user cancellation, and
   * write any output to the given console.
   * 
   * @param file
   * @param monitor
   * @param console
   * @throws OperationCanceledException
   */
  public static CompilationResult compileLibrary(IFile file, IProgressMonitor monitor,
      final MessageConsole console) throws CoreException {
    long startTime = System.currentTimeMillis();

    IPath path = file.getLocation();

    final IPath inputPath = file.getLocation();
    final IPath outputPath = getJsAppArtifactPath(path);

    Dart2JSCompiler compiler = new Dart2JSCompiler();

    console.printSeparator("Running dart2js...");

    try {
      CompilationResult result = compiler.compile(inputPath, outputPath, monitor, console);

      refreshResources(file);

      displayCompilationResult(compiler, result, outputPath, startTime, console);
      return result;
    } catch (IOException ioe) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, ioe.toString(), ioe));
    }
  }

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

  private static void displayCompilationResult(Dart2JSCompiler compiler, CompilationResult result,
      IPath outputPath, long startTime, MessageConsole console) {
    StringBuilder builder = new StringBuilder();

    if (!result.getStdOut().isEmpty()) {
      builder.append(result.getStdOut().trim() + "\n");
    }

    if (!result.getStdErr().isEmpty()) {
      builder.append(result.getStdErr().trim() + "\n");
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

      builder.append(NLS.bind("Wrote {0} [{1}]\n", outputFile.getPath(), message));
    }

    console.print(builder.toString());
  }

  /**
   * Dart2js creates java.io.Files; we need to tell the workspace about the new / changed resources.
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

  private IPackageRootProvider packageRootProvider;

  /**
   * Create a new Dart2JSCompiler.
   */
  public Dart2JSCompiler() {
    this(IPackageRootProvider.DEFAULT);
  }

  public Dart2JSCompiler(IPackageRootProvider packageRootProvider) {
    this.packageRootProvider = packageRootProvider;
  }

  /**
   * Run dart2js as a process to compile the given input file to the given output file. If an
   * IProgressMonitor is passed in, it is polled to see if the user cancelled the compile operation.
   * The progress monitor is not used for any other purpose.
   * 
   * @param inputPath
   * @param outputPath
   * @param monitor
   * @param console
   * @return
   * @throws IOException
   * @throws OperationCanceledException if the user cancelled the operation
   */
  public CompilationResult compile(IPath inputPath, IPath outputPath, IProgressMonitor monitor,
      MessageConsole console) throws IOException {
    ProcessBuilder builder = new ProcessBuilder();

    List<String> args = new ArrayList<String>();

    args.add(DartSdkManager.getManager().getSdk().getDart2JsExecutable().getPath());
    args.addAll(getCompilerArguments(inputPath, outputPath));

    builder.command(args);
    builder.directory(DartSdkManager.getManager().getSdk().getDirectory());
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder);

    runner.runSync(monitor);

    // Echo command line used to launch only if there is an error executing
    if (runner.getExitCode() != 0 && console != null) {
      StringBuilder msg = new StringBuilder();
      for (String arg : args) {
        msg.append(arg);
        msg.append(" ");
      }
      console.println(msg.toString().trim());
    }

    refreshParentFolder(outputPath);

    return new CompilationResult(runner, outputPath);
  }

  public String getName() {
    return "dart2js";
  }

  public boolean isAvailable() {
    return DartSdkManager.getManager().hasSdk();
  }

  protected List<String> getCompilerArguments(IPath inputPath, IPath outputPath) {
    List<String> args = new ArrayList<String>();

    args.add("--suppress-warnings");

    File packageRoot = packageRootProvider.getPackageRoot(getProjectFor(inputPath));
    if (packageRoot != null) {
      args.add("--package-root=" + packageRoot.getPath());
    }
    args.add("--out=" + outputPath.toOSString());
    args.add(inputPath.toOSString());

    // Add any custom dart2js settings for this project.
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(inputPath);

    if (files.length > 0) {
      IProject project = files[0].getProject();

      String flags = DartCore.getPlugin().getDart2jsFlags(project);

      if (flags != null && flags.length() > 0) {
        for (String arg : StringUtilities.parseArgumentString(flags)) {
          args.add(arg);
        }
      }
    }

    return args;
  }

  private IProject getProjectFor(IPath path) {
    return ResourceUtil.getFile(path.toFile()).getProject();
  }

  private void refreshParentFolder(IPath outputPath) {
    URI uri = outputPath.removeLastSegments(1).toFile().toURI();
    IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(
        uri);

    if (containers.length > 0) {
      try {
        containers[0].refreshLocal(1, new NullProgressMonitor());
      } catch (CoreException e) {

      }
    }
  }

}
