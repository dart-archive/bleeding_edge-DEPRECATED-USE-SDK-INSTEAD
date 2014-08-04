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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.builder.AbstractBuildVisitor;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.builder.CleanVisitor;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.pub.IPackageRootProvider;
import com.google.dart.tools.core.snapshot.SnapshotCompilationServer;

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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class invokes the build.dart scripts in a project's root directory.
 * <p>
 * For full builds, --full is passed into the dart script. For clean builds, we pass in a --clean
 * flag. For all other builds, we passed in the list of changed files using --changed and --removed
 * parameters. E.g. --changed=file1.txt --changed=file2.foo --removed=file3.bar.
 * 
 * @see DartBuilder
 * @coverage dart.tools.core.builder
 */
public class BuildDartParticipant implements BuildParticipant {
  // The name of the build.dart snapshot file.
  private static final String BUILD_DART_SNAPSHOT_NAME = "build.snapshot";

  // The generic unix/max/bsd CLI limit is 262144.
  private static final int GENERAL_CLI_LIMIT = 262000;

  // The win32 CreateProcess() function can take a max of 32767 chars.
  private static final int WIN_CLI_LIMIT = 32500;

  static final String ISSUE_MARKER = DartCore.PLUGIN_ID + ".buildDartIssue";

  private static final String CLEAN = "--clean";
  private static final String FULL_BUILD = "--full";
  private static final String MACHINE = "--machine";

  private static final String BUILD_LOG_NAME = ".buildlog";

  private static boolean USE_SNAPSHOT = false;

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

  private IPackageRootProvider packageRootProvider;

  private String genDirPath;

  public BuildDartParticipant() {
    this(IPackageRootProvider.DEFAULT);
  }

  public BuildDartParticipant(IPackageRootProvider packageRootProvider) {
    this.packageRootProvider = packageRootProvider;
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

    if (event.isFullBuild()) {
      event.traverse(new AbstractBuildVisitor() {
        @Override
        public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
          if (proxy.getType() == IResource.FOLDER || proxy.getType() == IResource.PROJECT) {
            IContainer container = (IContainer) proxy.requestResource();

            IFile builderFile = container.getFile(new Path(BUILD_DART_FILE_NAME));

            if (DartCore.isBuildDart(builderFile)) {
              // Perform a full build.
              runBuildDart(builderFile, Arrays.asList(FULL_BUILD), monitor);

              return false;
            }
          }

          return true;
        }
      }, false);
    } else {
      // Perform an incremental build.
      event.traverse(new AbstractBuildVisitor() {
        @Override
        public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
          IResource resource = delta.getResource();

          if (resource.getType() == IResource.FOLDER || resource.getType() == IResource.PROJECT) {
            IFile builderFile = ((IContainer) resource).getFile(new Path(BUILD_DART_FILE_NAME));

            if (DartCore.isBuildDart(builderFile)) {
              processDelta(builderFile, 0, delta, monitor);
              return false;
            }
          }

          return true;
        }
      }, false);
    }
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
        if (proxy.getType() == IResource.FILE) {
          if (proxy.getName().equals(BUILD_DART_FILE_NAME)) {
            IFile builderFile = (IFile) proxy.requestResource();
            if (DartCore.isBuildDart(builderFile)) {
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

    monitor.beginTask(msg.toString(), IProgressMonitor.UNKNOWN);

    IContainer container = builderFile.getParent();
    buildArgs = new ArrayList<String>(buildArgs);
    buildArgs.add(0, MACHINE);
    String commandSummary = createCommandSummary(buildArgs);

    // If we're over the CLI length limit, instead of sending in a comprehensive list of all the
    // changes and deletions, just request a full build.
    if ((DartCore.isWindows() && commandSummary.length() > WIN_CLI_LIMIT)
        || (!DartCore.isWindows() && commandSummary.length() > GENERAL_CLI_LIMIT)) {
      buildArgs.clear();

      buildArgs.add(MACHINE);
      buildArgs.add(FULL_BUILD);

      commandSummary = createCommandSummary(buildArgs);
    }

    // Trim long command summaries - used for verbose printing.
    if (commandSummary.length() > 100) {
      commandSummary = commandSummary.substring(0, 100) + "...";
    }

    ProcessBuilder builder = new ProcessBuilder();

    List<String> args = new ArrayList<String>();

    SnapshotCompilationServer snapshotCompiler = null;
    IStatus snapshotStatus = Status.OK_STATUS;

    long startTime = System.currentTimeMillis();

    if (USE_SNAPSHOT) {
      snapshotCompiler = new SnapshotCompilationServer(builderFile.getLocation().toFile());

      snapshotStatus = snapshotCompiler.compile();

      if (!snapshotStatus.isOK()) {
        snapshotCompiler = null;

        DartCore.logError(snapshotStatus.toString());
      }
    }

    args.add(DartSdkManager.getManager().getSdk().getVmExecutable().getPath());

    // --package-root
    File packageRoot = packageRootProvider.getPackageRoot(builderFile.getProject());
    if (packageRoot != null) {
      String path = packageRoot.getPath();
      if (!path.endsWith(File.separator)) {
        path += File.separator;
      }
      args.add("--package-root=" + path);
    }

    // If we have a snapshot, use that.
    if (snapshotCompiler != null && snapshotCompiler.getDestFile().exists()) {
      args.add(snapshotCompiler.getDestFile().getPath());
    } else {
      args.add(builderFile.getName());
    }

    args.addAll(buildArgs);

    //TODO (danrubel): Older build.dart may rely on DART_SDK env var... so leave for now
    Map<String, String> env = builder.environment();
    DirectoryBasedDartSdk sdk = DartSdkManager.getManager().getSdk();
    env.put("DART_SDK", sdk.getDirectory().getAbsolutePath());

    builder.command(args);
    builder.directory(container.getLocation().toFile());
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder);

    logStart(builderFile);
    log(builderFile, "---\nbuild.dart " + commandSummary);

    int result;

    try {
      // The monitor argument is just used to listen for user cancellations.
      result = runner.runSync(monitor);
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.getMessage(), e));
    }

    String output = runner.getStdOut();
    String processedOutput = processBuilderOutput(container, output);

    log(builderFile, output.trim());

    if (result != 0) {
      DartCore.getConsole().printSeparator("build.dart " + commandSummary);
      if (builderFile.getLocationURI() != null) {
        DartCore.getConsole().println(builderFile.getLocationURI().toString());
      }
      DartCore.getConsole().println("build.dart returned error code " + result);

      String stdout = processedOutput.trim();

      if (stdout.length() > 0) {
        DartCore.getConsole().println();
        DartCore.getConsole().println(stdout);
      }

      DartCore.getConsole().println();
    }

    long elapsedTime = System.currentTimeMillis() - startTime;
    log(builderFile, "build.dart finished [" + elapsedTime + " ms]\n");

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
          // Don't report changes to "." files.
          if (resource.getName().startsWith(".")) {
            return false;
          }

          // Don't report changes to "build.dart" files
          if (resource.getName().equals(BUILD_DART_FILE_NAME)) {
            return false;
          }

          // Don't report changes to "build.snapshot" files
          if (resource.getName().equals(BUILD_DART_SNAPSHOT_NAME)) {
            return false;
          }

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

          // This is to address dartbug.com/10863.
          // A more complete fix will happen for dartbug.com/8478.
          if (resource.getName().equals("out")) {
            return false;
          }

          // Don't trigger builds from packages directories.
          if (resource.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
            return false;
          }
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
    } else if (method.equals("info")) {
      String file = params.optString("file", null);
      if (file != null && file.length() > 0) {
        createMarker(
            container,
            IMarker.SEVERITY_INFO,
            file,
            params.getString("message"),
            params.optInt("line", -1),
            params.optInt("charStart", -1),
            params.optInt("charEnd", -1));
      } else {
        //[{"method":"info","params":{"message":"Took 0.6s (0.3s awaiting secondary inputs)."}}]
      }
    } else if (method.equals("mapping")) {
      String fromPath = params.getString("from");
      String toPath = params.getString("to");

      IFile fromFile = container.getFile(new Path(fromPath));
      IFile toFile = container.getFile(new Path(toPath));

      if (fromFile.exists()) {
        DartCore.setResourceRemapping(fromFile, toFile);
      }
    } else if (method.equals("out")) {
      genDirPath = params.getString("file");

    } else if (method.equals("generated")) {
      // TODO(keertip): add processing for generated messages

    } else {
      DartCore.logError("builder command '" + method + "\' not understood.");
    }
  }

  private void log(IFile builderFile, String string) {
    try {
      IFile logFile = builderFile.getParent().getFile(new Path(BUILD_LOG_NAME));
      File file = new File(logFile.getLocationURI().toURL().toURI());
      Files.append(string + "\n", file, Charsets.UTF_8);
    } catch (IOException ioe) {

    } catch (URISyntaxException e) {

    }
  }

  private void logStart(IFile builderFile) {
    final long TRUNC_SIZE = 1024 * 1024;

    try {
      IFile logFile = builderFile.getParent().getFile(new Path(BUILD_LOG_NAME));
      File file = new File(logFile.getLocationURI().toURL().toURI());

      if (logFile.exists()) {
        if (file.length() > TRUNC_SIZE) {
          RandomAccessFile rFile = new RandomAccessFile(file, "rw");
          rFile.setLength(0);
          rFile.close();
        }
      } else {
        Files.touch(file);
      }
    } catch (IOException ioe) {

    } catch (URISyntaxException e) {

    }
  }

  private String processBuilderOutput(IContainer container, String output) {
    String[] lines = output.split("\n");
    StringBuilder stringBuilder = new StringBuilder(output.length());

    for (String line : lines) {
      String trimmedLine = line.trim();

      if (trimmedLine.startsWith("[{") && trimmedLine.endsWith("}]")) {
        // try and parse this as a builder event
        //[{"method":"error","params":{"file":"foo.html","line":23,"message":"no ID found"}}]
        //[{"method":"warning","params":{"file":"foo.html","line":23,"message":"no ID found"}}]
        //[{"method":"info","params":{"file":"foo.html","line":23,"message":"no ID found"}}]
        //[{"method":"info","params":{"message":"Took 0.6s (0.3s awaiting secondary inputs)."}}]
        //[{"method":"mapping","params":{"from":"foo.html","to":"out/foo.html"}}]

        String jsonStr = trimmedLine.substring(1, trimmedLine.length() - 1);

        try {
          JSONObject json = new JSONObject(jsonStr);

          handleBuilderMessage(container, json);
        } catch (JSONException e) {
          DartCore.logError("Failed to process build.dart message:\n" + trimmedLine, e);
        }
      } else {
        stringBuilder.append(line);
        stringBuilder.append('\n');
      }
    }

    return stringBuilder.toString();
  }

  /**
   * @return whether we should invoke any build.dart files in the given project
   */
  private boolean shouldRunAnyBuildDart(IProject project) {
    boolean disableBuilder = DartCore.getPlugin().getDisableDartBasedBuilder(project);

    return !disableBuilder;
  }

}
