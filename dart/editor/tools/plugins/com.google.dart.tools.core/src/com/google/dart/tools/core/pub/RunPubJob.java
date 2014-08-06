package com.google.dart.tools.core.pub;

import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs the pub operation as an external process. The operation can be scheduled via the {@link Job}
 * infrastructure or executed directly by calling {@link #run(IProgressMonitor)} or
 * {@link #runSilent(IProgressMonitor)} if this operation should not display output in the console.
 * 
 * @coverage dart.tools.core.pub
 */
public class RunPubJob extends Job {

  public static final String UPDATE_COMMAND = "upgrade"; //$NON-NLS-1$
  public static final String INSTALL_COMMAND = "get"; //$NON-NLS-1$
  public static final String INSTALL_OFFLINE_COMMAND = "get --offline"; //$NON-NLS-1$
  public static final String PUBLISH_COMMAND = "publish"; //$NON-NLS-1$
  public static final String BUILD_COMMAND = "build"; //$NON-NLS-1$

  /**
   * The pub command to be run (e.g. "install", "update")
   */
  private final String command;

  /**
   * The directory in which the pub command will be run
   */
  private final IContainer container;

  /**
   * Indicates whether this pub job was triggered by the builder, or packages view etc. instead of
   * by a menu option.
   */
  private final boolean autorun;

  /**
   * The directory which contains the sources to build, used only for pub build command
   */
  @SuppressWarnings("unused")
  private final IContainer sourceFolder;

  /**
   * Construct a new job for running a pub command
   * 
   * @param container the directory in which the cmd will be run (not {@code null})
   * @param command the command to be run... either {@link #INSTALL_COMMAND} or
   *          {@link #UPDATE_COMMAND}
   * @param autorun indicate whether pub was run by editor without user interaction
   */
  public RunPubJob(IContainer container, String command, boolean autorun) {
    this(container, command, autorun, null);
  }

  /**
   * Construct a new job for running a pub command
   * 
   * @param container the directory in which the cmd will be run (not {@code null})
   * @param command the command to be run... either {@link #INSTALL_COMMAND} or
   *          {@link #UPDATE_COMMAND}
   * @param autorun indicate whether pub was run by editor without user interaction
   * @param the folder containing the sources to build, used only for pub build commands
   */
  public RunPubJob(IContainer container, String command, boolean autorun, IContainer sourceFolder) {
    super(NLS.bind(PubMessages.RunPubJob_name, command));
    this.command = command;
    this.container = container;
    this.autorun = autorun;
    this.sourceFolder = sourceFolder;
    // TODO(keertip): comment out for now, on windows pub install takes long time and blocks builder
    //   setRule(container);
  }

  /**
   * Runs the pub command and displays the output in the console.
   * 
   * @return the result of running the pub command
   */
  @Override
  public IStatus run(IProgressMonitor monitor) {
    MessageConsole console = DartCore.getConsole();
    String path = container.getLocation().toOSString();
    if (autorun) {
      console.printSeparator(NLS.bind(PubMessages.RunPubJob_auto_running, command, path));
    } else {
      console.printSeparator(NLS.bind(PubMessages.RunPubJob_running, command, path));
    }
    IStatus status = runSilent(monitor);
    console.println(status.getMessage());

    return status;
  }

  /**
   * Runs the pub command.
   * 
   * @return the result of running the pub command
   */
  public IStatus runSilent(IProgressMonitor monitor) {
    ProcessRunner runner = null;
    try {
      // Build the process description to run pub
      DirectoryBasedDartSdk sdk = DartSdkManager.getManager().getSdk();
      File pubFile = sdk.getPubExecutable();

      ProcessBuilder builder = new ProcessBuilder();
      builder.directory(container.getLocation().toFile());
      builder.redirectErrorStream(true);

      List<String> args = new ArrayList<String>();
      args.add(pubFile.getAbsolutePath());
      if (DartCoreDebug.NO_PUB_PACKAGES) {
        args.add("--no-package-symlinks");
      }
      if (command.contains(" ")) {
        String[] strings = command.split(" ");
        args.addAll(Arrays.asList(strings));
      } else {
        args.add(command);
      }
      // add flags for pub build
      if (command.equals(BUILD_COMMAND)) {
        args.add("--mode");
        args.add("debug");
        // TODO(keertip): figure out why build fails when passing in folder name.
        args.add("--all");
//        if (sourceFolder != null) {
//          String folderName = getPubDirectoryParent(sourceFolder);
//          if (folderName != null) {
//            args.add(folderName);
//          }
//        } else {
//          args.add("--all");
//        }
      }

      builder.command(args);

      // Run the pub command as an external process.
      runner = newProcessRunner(builder);

      try {
        runner.runSync(monitor);
      } catch (IOException e) {
        String message = NLS.bind(PubMessages.RunPubJob_failed, command, e.toString());
        return new Status(IStatus.CANCEL, DartCore.PLUGIN_ID, message, e);
      }

      StringBuilder stringBuilder = new StringBuilder();

      if (!runner.getStdOut().isEmpty()) {
        stringBuilder.append(runner.getStdOut().trim() + "\n"); //$NON-NLS-1$
      }

      int exitCode = runner.getExitCode();

      if (exitCode != 0) {
        String output = "[" + exitCode + "] " + stringBuilder.toString();
        String message = NLS.bind(PubMessages.RunPubJob_failed, command, output);
        if (command.equals(INSTALL_COMMAND)) {
          message += "\n** Warning: Application may fail to run since packages did not get installed."
              + "Try running pub get again. **";
        }
        return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, message);
      }

      try {
        // Refresh the Eclipse resources
        container.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        // run build.dart after pub updates - do a full build
        if (command.equals(UPDATE_COMMAND)) {
          container.getProject().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        }
        // Disable analysis of the build directory generated by pub build
        if (RunPubJob.BUILD_COMMAND.equals(command)) {
          setDerived(monitor);
        }

      } catch (CoreException e) {
        // Log the exception and move on
        DartCore.logError("Exception refreshing " + container, e);
      }

      return new Status(IStatus.OK, DartCore.PLUGIN_ID, stringBuilder.toString());
    } catch (OperationCanceledException exception) {
      String message = NLS.bind(PubMessages.RunPubJob_canceled, command);
      return new Status(IStatus.CANCEL, DartCore.PLUGIN_ID, message, exception);
    } finally {
      if (runner != null) {
        runner.dispose();
      }
      monitor.done();
    }
  }

  /**
   * Answer the {@link ProcessRunner} used to execute the pub operation. This is overridden when
   * testing this class to prevent from actually running pub.
   * 
   * @param builder the process description (not {@code null})
   * @return the process runner (not {@code null})
   */
  protected ProcessRunner newProcessRunner(ProcessBuilder builder) {
    return new ProcessRunner(builder);
  }

  @SuppressWarnings("unused")
  private String getPubDirectoryParent(IContainer folder) {
    while (folder != null) {
      if (DartCore.pubDirectories.contains(folder.getName())) {
        return folder.getName();
      }
      folder = folder.getParent();
    }
    return null;
  }

  private void setDerived(IProgressMonitor monitor) {
    IFolder buildDir = container.getFolder(new Path("build"));
    try {
      ((IResource) buildDir).setDerived(true, monitor);
    } catch (CoreException e) {
      DartCore.logError("Failed to set derived flag: " + buildDir, e);
    }
  }

}
