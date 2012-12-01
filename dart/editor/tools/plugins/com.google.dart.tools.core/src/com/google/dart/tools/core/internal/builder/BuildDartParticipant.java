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
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.builder.CleanVisitor;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;

import static com.google.dart.tools.core.DartCore.BUILD_DART_FILE_NAME;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.json.JSONException;
import org.json.JSONObject;

import static org.eclipse.core.resources.IResource.FILE;
import static org.eclipse.core.resources.IResource.PROJECT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
public class BuildDartParticipant implements BuildParticipant {

  private static final boolean VERBOSE = false;

  // The generic unix/max/bsd CLI limit is 262144.
  private static final int GENERAL_CLI_LIMIT = 262000;

  // The win32 CreateProcess() function can take a max of 32767 chars.
  private static final int WIN_CLI_LIMIT = 32500;

  static final String ISSUE_MARKER = DartCore.PLUGIN_ID + ".buildDartIssue";

  private static final Path REL_PUBSPEC_PATH = new Path(DartCore.PUBSPEC_FILE_NAME);
  private static final Path REL_BUILD_DART_PATH = new Path(BUILD_DART_FILE_NAME);
  private static final String CLEAN = "--clean";

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

  /**
   * Traverse resources looking for build.dart files that has been added or which has resources
   * contained in its parent which have been added or have changed.
   */
  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {
    if (!shouldRunAnyBuildDart(event.getProject())) {
      return;
    }
    event.traverse(new BuildVisitor() {

      @Override
      public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
        IResource resource = delta.getResource();
        if (resource.getType() != FILE) {
          IFile builderFile = ((IContainer) resource).getFile(REL_BUILD_DART_PATH);
          if (shouldRunBuildDart(builderFile)) {
            processDelta(builderFile, 0, delta, monitor);
          }
        }
        return true;
      }

      @Override
      public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
        if (proxy.getType() == FILE) {
          if (proxy.getName().equals(BUILD_DART_FILE_NAME)) {
            IFile builderFile = (IFile) proxy.requestResource();
            if (shouldRunBuildDart(builderFile)) {
              runBuildDart(builderFile, Arrays.asList(new String[] {}), monitor);
            }
          }
        }
        return true;
      }
    }, false);
  }

  /**
   * Clear markers and invoke each builder with the --clean flag
   */
  @Override
  public void clean(CleanEvent event, final IProgressMonitor monitor) throws CoreException {
    deleteMarkers(event.getProject());
    DartCore.clearResourceRemapping(event.getProject());
    event.traverse(new CleanVisitor() {

      @Override
      public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
        if (proxy.getType() == FILE) {
          if (proxy.getName().equals(BUILD_DART_FILE_NAME)) {
            IFile builderFile = (IFile) proxy.requestResource();
            if (shouldRunBuildDart(builderFile)) {
              runBuildDart(builderFile, Arrays.asList(new String[] {CLEAN}), monitor);
            }
          }
        }
        return true;
      }
    }, false);
  }

  /**
   * Process the specified delta and invoke the builder as appropriate.
   * 
   * @param builderFile the build.dart file (not <code>null</code>)
   * @param delta the resource delta or <code>null</code> if none
   * @param monitor the progress monitor (not <code>null</code>) to use for reporting progress to
   *          the user. It is the caller's responsibility to call done() on the given monitor.
   */
  protected void processDelta(IFile builderFile, int kind, IResourceDelta delta,
      IProgressMonitor monitor) throws CoreException {

    // Find the changed and removed files.
    List<IFile> changedFiles = new ArrayList<IFile>();
    List<IFile> deletedFiles = new ArrayList<IFile>();

    getFileDeltas(delta, changedFiles, deletedFiles);

    // Construct the args array.
    int containerDepth = builderFile.getParent().getFullPath().segmentCount();
    List<String> args = new ArrayList<String>(changedFiles.size() + deletedFiles.size());
    for (IFile file : changedFiles) {
      deleteMarkers(file);
      DartCore.clearResourceRemapping(file);

      args.add("--changed=" + getFilePath(containerDepth, file));
    }

    for (IFile file : deletedFiles) {
      DartCore.clearResourceRemapping(file);

      args.add("--removed=" + getFilePath(containerDepth, file));
    }

    // Only invoke builder if there were changes of interest
    if (args.size() > 0) {
      runBuildDart(builderFile, args, monitor);
    }
  }

  /**
   * Execute the build.dart application. This method is overridden during testing to record which
   * build.dart files would be run rather than actually running them.
   * 
   * @param builderFile the build.dart file (not <code>null</code>)
   * @param buildArgs the arguments passed to the build file
   * @param monitor the progress monitor (not <code>null</code>) to use for reporting progress to
   *          the user. It is the caller's responsibility to call done() on the given monitor.
   */
  protected void runBuildDart(IFile builderFile, List<String> buildArgs, IProgressMonitor monitor)
      throws CoreException {

    StringBuilder msg = new StringBuilder();
    msg.append("Running ");
    msg.append(builderFile.getLocation());
    if (buildArgs.size() > 0 && buildArgs.get(0).equals(CLEAN)) {
      msg.append(" ");
      msg.append(CLEAN);
    }
    monitor.beginTask(msg.toString(), IProgressMonitor.UNKNOWN);

    IContainer container = builderFile.getParent();
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
    builder.directory(container.getLocation().toFile());
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder);

    if (VERBOSE) {
      System.out.println("build.dart " + commandSummary);
    }

    long startTime = System.currentTimeMillis();

    // The monitor argument is just used to listen for user cancellations.
    int result;
    try {
      result = runner.runSync(monitor);
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.getMessage(), e));
    }

    processBuilderOutput(container, runner.getStdOut());

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

    BuilderUtil.delayedRefresh(builderFile.getParent());
  }

  private String createCommandSummary(List<String> buildArgs) {
    StringBuilder builder = new StringBuilder();

    for (String arg : buildArgs) {
      builder.append(arg);
      builder.append(' ');
    }

    return builder.toString().trim();
  }

  private void createMarker(IContainer container, int severity, String path, String message,
      int line, int charStart, int charEnd) {
    IFile file = container.getFile(new Path(path));

    if (file.exists()) {
      try {
        createErrorMarker(file, severity, message, line, charStart, charEnd);
      } catch (CoreException e) {
        DartCore.logError(e);
      }
    }
  }

  private void getFileDeltas(IResourceDelta delta, final List<IFile> changedFiles,
      final List<IFile> deletedFiles) throws CoreException {
    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();

        if (resource.getType() == IResource.FILE) {
          // Don't report changes to "build.dart" files
          if (!resource.getName().equals(BUILD_DART_FILE_NAME)) {
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
          }
        } else if (resource.getType() == IResource.FOLDER) {
          // Don't report changes in hidden directories, specifically SCM (.svn, .git) directories.
          // https://code.google.com/p/dart/issues/detail?id=4885
          if (resource.getName().startsWith(".")) {
            return false;
          }

          // Don't trigger builds from packages directories.
          if (resource.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
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

  /**
   * Answer the path of the specified file relative to a container
   * 
   * @param containerDepth the number of segments in the containers full path
   * @param file the file (not <code>null</code>)
   */
  private String getFilePath(int containerDepth, IFile file) {
    return file.getFullPath().removeFirstSegments(containerDepth).toOSString();
  }

  private void handleBuilderMessage(IContainer container, JSONObject json) throws JSONException {
    String method = json.getString("method");
    JSONObject params = json.optJSONObject("params");

    if (method.equals("error")) {
      createMarker(
          container,
          IMarker.SEVERITY_ERROR,
          params.getString("file"),
          params.getString("message"),
          params.optInt("line", -1),
          params.optInt("charStart", -1),
          params.optInt("charEnd", -1));
    } else if (method.equals("warning")) {
      createMarker(
          container,
          IMarker.SEVERITY_WARNING,
          params.getString("file"),
          params.getString("message"),
          params.optInt("line", -1),
          params.optInt("charStart", -1),
          params.optInt("charEnd", -1));
    } else if (method.equals("mapping")) {
      String fromPath = params.getString("from");
      String toPath = params.getString("to");

      IFile fromFile = container.getFile(new Path(fromPath));
      IFile toFile = container.getFile(new Path(toPath));

      if (fromFile.exists()) {
        DartCore.setResourceRemapping(fromFile, toFile);
      }
    } else {
      DartCore.logError("builder command '" + method + "\' not understood.");
    }
  }

  private void processBuilderOutput(IContainer container, String output) {
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

          handleBuilderMessage(container, json);
        } catch (JSONException e) {
          DartCore.logError(e);
        }
      }
    }
  }

  /**
   * @return whether we should invoke any build.dart files in the given project
   */
  private boolean shouldRunAnyBuildDart(IProject project) {
    boolean disableBuilder = DartCore.getPlugin().getDisableDartBasedBuilder(project);

    return !disableBuilder;
  }

  /**
   * @return whether we should invoke the specified build.dart file
   */
  private boolean shouldRunBuildDart(IFile builderFile) {
    if (!builderFile.exists()) {
      return false;
    }
    IContainer container = builderFile.getParent();
    // Legacy... always run build.dart in project
    if (container.getType() == PROJECT) {
      return true;
    }
    return container.getFile(REL_PUBSPEC_PATH).exists();
  }

}
