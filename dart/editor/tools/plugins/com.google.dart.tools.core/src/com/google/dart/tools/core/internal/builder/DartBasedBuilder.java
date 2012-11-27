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
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.json.JSONException;
import org.json.JSONObject;

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
  private static final boolean VERBOSE = false;

  // The generic unix/max/bsd CLI limit is 262144.
  private static final int GENERAL_CLI_LIMIT = 262000;

  // The win32 CreateProcess() function can take a max of 32767 chars.
  private static final int WIN_CLI_LIMIT = 32500;

  private static DartBasedBuilder builder = new DartBasedBuilder();

  static final String ISSUE_MARKER = DartCore.PLUGIN_ID + ".buildDartIssue";

  public static DartBasedBuilder getBuilder() {
    return builder;
  }

  private static void createErrorMarker(IFile file, int severity, String message, int line,
      int charStart, int charEnd) throws CoreException {
    IMarker marker = file.createMarker(ISSUE_MARKER);
    marker.setAttribute(IMarker.SEVERITY, severity);
    marker.setAttribute(IMarker.MESSAGE, message);
    marker.setAttribute(IMarker.LINE_NUMBER, line);
    if (charStart != -1) {
      marker.setAttribute(IMarker.CHAR_START, charStart);
    }
    if (charEnd != -1) {
      marker.setAttribute(IMarker.CHAR_END, charEnd);
    }
  }

  private static void deleteMarkers(IContainer container) throws CoreException {
    container.deleteMarkers(ISSUE_MARKER, true, IResource.DEPTH_INFINITE);
  }

  private static void deleteMarkers(IFile file) throws CoreException {
    file.deleteMarkers(ISSUE_MARKER, true, IResource.DEPTH_ZERO);
  }

  private DartBasedBuilder() {

  }

  public void handleBuild(IProject project, int kind, IResourceDelta delta, IProgressMonitor monitor)
      throws CoreException {
    IFile builderFile = getBuilderFile(project);

    if (builderFile != null && shouldInvokeDartBasedBuilder(project)) {
      monitor.beginTask("Running build.dart...", IProgressMonitor.UNKNOWN);

      try {
        List<String> args;
        boolean invokeBuilder = true;

        if (IncrementalProjectBuilder.FULL_BUILD == kind) {
          // no args == a full build
          args = Collections.emptyList();
        } else if (delta != null) {
          // Find the changed and removed files.
          List<IFile> changedFiles = new ArrayList<IFile>();
          List<IFile> deletedFiles = new ArrayList<IFile>();

          getFileDeltas(project, delta, changedFiles, deletedFiles);

          // Don't report changes to "build.dart".
          changedFiles.remove(DartCore.BUILD_DART_FILE_NAME);
          deletedFiles.remove(DartCore.BUILD_DART_FILE_NAME);

          // Construct the args array.
          args = new ArrayList<String>(changedFiles.size() + deletedFiles.size());

          for (IFile file : changedFiles) {
            deleteMarkers(file);
            DartCore.clearResourceRemapping(file);

            args.add("--changed=" + getFilePath(file));
          }

          for (IFile file : deletedFiles) {
            args.add("--removed=" + getFilePath(file));
          }

          // No changes were of interest to the build.dart script.
          if (args.size() == 0) {
            invokeBuilder = false;
          }
        } else {
          // Will we encounter this else clause?
          args = Collections.emptyList();
        }

        if (invokeBuilder) {
          invokeBuilder(monitor, project, builderFile, args);

          BuilderUtil.delayedRefresh(project);
        }
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

    if (builderFile != null && shouldInvokeDartBasedBuilder(project)) {
      monitor.beginTask("Running build.dart --clean", IProgressMonitor.UNKNOWN);

      try {
        deleteMarkers(project);

        DartCore.clearResourceRemapping(project);

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

  protected void invokeBuilder(IProgressMonitor monitor, IProject project, IFile builderFile,
      List<String> buildArgs) throws IOException {
    buildArgs = new ArrayList<String>(buildArgs);
    String commandSummary = createCommandSummary(buildArgs);

    // If we're over the CLI length limit, instead of sending in a comprehensive list of all the
    // changes and deletions, just request a full build.
    if ((DartCore.isWindows() && commandSummary.length() > WIN_CLI_LIMIT)
        || (!DartCore.isWindows() && commandSummary.length() > GENERAL_CLI_LIMIT)) {
      buildArgs.clear();
      commandSummary = "";
    }

    buildArgs.add(0, "--machine");

    // Trim long command summaries - used for verbose printing.
    if (commandSummary.length() > 100) {
      commandSummary = commandSummary.substring(0, 100) + "...";
    }

    ProcessBuilder builder = new ProcessBuilder();

    List<String> args = new ArrayList<String>();

    args.add(DartSdkManager.getManager().getSdk().getVmExecutable().getPath());
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

    processBuilderOutput(project, runner.getStdOut());

    if (result != 0) {
      DartCore.getConsole().println("build.dart " + commandSummary);
      if (builderFile.getLocationURI() != null) {
        DartCore.getConsole().println(builderFile.getLocationURI().toString());
      }
      DartCore.getConsole().println("build.dart returned error code " + result);

      String stdout = runner.getStdOut().trim();

      if (stdout.length() > 0) {
        DartCore.getConsole().println();
        DartCore.getConsole().println(stdout);
      }

      DartCore.getConsole().println();
    }

    if (VERBOSE) {
      long elapsedTime = System.currentTimeMillis() - startTime;

      System.out.println("build.dart finished [" + elapsedTime + " ms]");
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

  private void createMarker(IProject project, int severity, String path, String message, int line,
      int charStart, int charEnd) {
    IFile file = project.getFile(new Path(path));

    if (file.exists()) {
      try {
        createErrorMarker(file, severity, message, line, charStart, charEnd);
      } catch (CoreException e) {
        DartCore.logError(e);
      }
    }
  }

  /**
   * Locate and return build.dart. Returns null if no such file exists.
   * 
   * @return
   */
  private IFile getBuilderFile(IProject project) {
    IResource resource = project.findMember(DartCore.BUILD_DART_FILE_NAME);

    if (resource instanceof IFile) {
      return (IFile) resource;
    } else {
      return null;
    }
  }

  private void getFileDeltas(IProject project, IResourceDelta delta,
      final List<IFile> changedFiles, final List<IFile> deletedFiles) throws CoreException {
    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();

        if (resource.getType() == IResource.FILE) {
          switch (delta.getKind()) {
            case IResourceDelta.ADDED:
              changedFiles.add((IFile) resource);
              break;
            case IResourceDelta.CHANGED:
              changedFiles.add((IFile) resource);
              break;
            case IResourceDelta.REMOVED:
              deletedFiles.add((IFile) resource);
              break;
          }
        } else if (resource.getType() == IResource.FOLDER) {
          // Don't report changes in hidden directories, specifically SCM (.svn, .git) directories.
          // https://code.google.com/p/dart/issues/detail?id=4885
          if (resource.getName().startsWith(".")) {
            return false;
          }

          // Don't trigger builds from packages directories.
          if (DartCore.isPackagesDirectory((IFolder) resource)) {
            return false;
          }

//          // Don't report changes to resources in an 'out' directory.
//          // TODO(devoncarew): For now, we're not doing this; there is no specially handled output
//          // directory from the pov of the editor.
//          if (resource.getName().equals("out")) {
//            return false;
//          }
        }

        return true;
      }
    });
  }

  private String getFilePath(IFile file) {
    return file.getProjectRelativePath().toOSString();
  }

  private void handleBuilderMessage(IProject project, JSONObject json) throws JSONException {
    String method = json.getString("method");
    JSONObject params = json.optJSONObject("params");

    if (method.equals("error")) {
      createMarker(
          project,
          IMarker.SEVERITY_ERROR,
          params.getString("file"),
          params.getString("message"),
          params.optInt("line", -1),
          params.optInt("charStart", -1),
          params.optInt("charEnd", -1));
    } else if (method.equals("warning")) {
      createMarker(
          project,
          IMarker.SEVERITY_WARNING,
          params.getString("file"),
          params.getString("message"),
          params.optInt("line", -1),
          params.optInt("charStart", -1),
          params.optInt("charEnd", -1));
    } else if (method.equals("mapping")) {
      String fromPath = params.getString("from");
      String toPath = params.getString("to");

      IFile fromFile = project.getFile(new Path(fromPath));
      IFile toFile = project.getFile(new Path(toPath));

      if (fromFile.exists()) {
        DartCore.setResourceRemapping(fromFile, toFile);
      }
    } else {
      DartCore.logError("builder command '" + method + "\' not understood.");
    }
  }

  private void processBuilderOutput(IProject project, String output) {
    String[] lines = output.split("\n");

    for (String line : lines) {
      line = line.trim();

      if (line.startsWith("[{") && line.endsWith("}]")) {
        // try and parse this as a builder event
        //[{"method":"error","params":{"file":"foo.html","line":23,"message":"no ID found"}}]
        //[{"method":"warning","params":{"file":"foo.html","line":23,"message":"no ID found"}}]
        //[{"method":"mapping","params":{"from":"foo.html","to":"out/foo.html"}}]

        String jsonStr = line.substring(1, line.length() - 1);

        try {
          JSONObject json = new JSONObject(jsonStr);

          handleBuilderMessage(project, json);
        } catch (JSONException e) {
          DartCore.logError(e);
        }
      }
    }
  }

  /**
   * @return whether we should invoke build.dart for the given project
   */
  private boolean shouldInvokeDartBasedBuilder(IProject project) {
    boolean disableBuilder = DartCore.getPlugin().getDisableDartBasedBuilder(project);

    return !disableBuilder;
  }

}
