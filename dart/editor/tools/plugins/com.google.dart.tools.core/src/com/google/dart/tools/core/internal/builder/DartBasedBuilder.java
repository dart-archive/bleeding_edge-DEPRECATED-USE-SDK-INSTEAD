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
import com.google.dart.tools.core.frog.ProcessRunner;
import com.google.dart.tools.core.model.DartSdk;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class invokes the build.dart scripts in a project's root directory.
 * <p>
 * For full builds, no arguments are passed into the dart script. For clean builds, we pass in a
 * --clean flag. For all other builds, we passed in the list of changed files using --changed and
 * --removed parameters. E.g. --changed=file1.txt --changed=file2.foo --removed=file3.bar.
 * 
 * @see DartBuilder
 */
public class DartBasedBuilder {
  private static final boolean VERBOSE = true;

  // The generic unix/max/bsd CLI limit is 262144.
  private static final int GENERAL_CLI_LIMIT = 262000;

  // The win32 CreateProcess() function can take a max of 32767 chars.
  private static final int WIN_CLI_LIMIT = 32500;

  private static DartBasedBuilder builder = new DartBasedBuilder();

  public static DartBasedBuilder getBuilder() {
    return builder;
  }

  private DartBasedBuilder() {

  }

  public void build(IProject project, int kind, IResourceDelta delta, IProgressMonitor monitor)
      throws CoreException {
    IFile builderFile = getBuilderFile(project);

    if (builderFile != null) {
      monitor.beginTask("Running build.dart...", IProgressMonitor.UNKNOWN);

      try {
        List<String> args;

        if (IncrementalProjectBuilder.FULL_BUILD == kind) {
          // no args == a full build
          args = Collections.emptyList();
        } else if (delta != null) {
          // Find the changed and removed files.
          List<String> changedFiles = new ArrayList<String>();
          List<String> deletedFiles = new ArrayList<String>();

          getFileDeltas(project, delta, changedFiles, deletedFiles);

          // Construct the args array.
          args = new ArrayList<String>(changedFiles.size() + deletedFiles.size());

          for (String file : changedFiles) {
            args.add("--changed=" + file);
          }

          for (String file : deletedFiles) {
            args.add("--removed=" + file);
          }
        } else {
          // Will we encounter this else clause?
          args = Collections.emptyList();
        }

        invokeBuilder(monitor, project, builderFile, args);

        BuilderUtil.delayedRefresh(project);
      } catch (IOException ioe) {
        throw new CoreException(
            new Status(IStatus.ERROR, DartCore.PLUGIN_ID, ioe.getMessage(), ioe));
      } finally {
        monitor.done();
      }
    }
  }

  public void handleClean(IProject project, IProgressMonitor monitor) throws CoreException {
    IFile builderFile = getBuilderFile(project);

    if (builderFile != null) {
      monitor.beginTask("Running build.dart --clean", IProgressMonitor.UNKNOWN);

      try {
        invokeBuilder(monitor, project, builderFile, Arrays.asList(new String[] {"--clean"}));
        BuilderUtil.delayedRefresh(project);
      } catch (IOException ioe) {
        throw new CoreException(
            new Status(IStatus.ERROR, DartCore.PLUGIN_ID, ioe.getMessage(), ioe));
      } finally {
        monitor.done();
      }
    }
  }

  private String createCommandSummary(List<String> buildArgs) {
    StringBuilder builder = new StringBuilder();

    for (String arg : buildArgs) {
      builder.append(arg);
      builder.append(' ');
    }

    return builder.toString().trim();
  }

  /**
   * Locate and return build.dart. Returns null if no such file exists.
   * 
   * @return
   */
  private IFile getBuilderFile(IProject project) {
    IResource resource = project.findMember("build.dart");

    if (resource instanceof IFile) {
      return (IFile) resource;
    } else {
      return null;
    }
  }

  private void getFileDeltas(IProject project, IResourceDelta delta,
      final List<String> changedFiles, final List<String> deletedFiles) throws CoreException {
    // TODO(devoncarew): perhaps auto-filter the annotations the script is interested in
    // based on annotations on build.dart? So @changed("*.css,*.foo")?

    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();

        if (resource.getType() == IResource.FILE) {
          switch (delta.getKind()) {
            case IResourceDelta.ADDED:
              changedFiles.add(getFilePath((IFile) resource));
              break;
            case IResourceDelta.CHANGED:
              changedFiles.add(getFilePath((IFile) resource));
              break;
            case IResourceDelta.REMOVED:
              deletedFiles.add(getFilePath((IFile) resource));
              break;
          }
        }

        return true;
      }
    });
  }

  private String getFilePath(IFile file) {
    return file.getProjectRelativePath().toOSString();
  }

  private String indent(String str) {
    final String lineSep = System.getProperty("line.separator");

    return "  " + str.replaceAll(lineSep, lineSep + "  ");
  }

  private void invokeBuilder(IProgressMonitor monitor, IProject project, IFile builderFile,
      List<String> buildArgs) throws IOException {
    String commandSummary = createCommandSummary(buildArgs);

    // If we're over the CLI length limit, instead of sending in a comprehensive list of all the
    // changes and deletions, just request a full build.
    if (DartCore.isWindows()) {
      if (commandSummary.length() > WIN_CLI_LIMIT) {
        buildArgs.clear();
        commandSummary = "";
      }
    } else {
      if (commandSummary.length() > GENERAL_CLI_LIMIT) {
        buildArgs.clear();
        commandSummary = "";
      }
    }

    // Trim long command summaries - used for verbose printing.
    if (commandSummary.length() > 60) {
      commandSummary = commandSummary.substring(0, 60) + "...";
    }

    ProcessBuilder builder = new ProcessBuilder();

    List<String> args = new ArrayList<String>();

    args.add(DartSdk.getInstance().getVmExecutable().getPath());
    args.add("--new_gen_heap_size=256");
    args.add(builderFile.getProjectRelativePath().toOSString());
    args.addAll(buildArgs);

    builder.command(args);
    builder.directory(project.getLocation().toFile());
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder);

    if (VERBOSE) {
      System.out.println("build.dart " + commandSummary);
    }

    long startTime = System.currentTimeMillis();

    // The monitor argument is just used to listen for user cancellations.
    int result = runner.runSync(monitor);

    // TODO(devoncarew): process the stdout of the builder, looking for generated:<filepath>
    // messages? This convention would let us mark such files as derived resources.

    if (VERBOSE) {
      long elapsedTime = System.currentTimeMillis() - startTime;

      System.out.println("build.dart finished [" + elapsedTime + " ms]");
    }

    if (result != 0) {
      DartCore.getConsole().println(
          builderFile.getFullPath().toString() + " " + commandSummary + " failed with error code "
              + result);
      DartCore.getConsole().println(indent(runner.getStdOut().trim()));
    }
  }

}
