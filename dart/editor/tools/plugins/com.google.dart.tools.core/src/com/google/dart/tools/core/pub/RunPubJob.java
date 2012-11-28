package com.google.dart.tools.core.pub;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the pub operation as an external process. The operation can be scheduled via the {@link Job}
 * infrastructure or executed directly by calling {@link #run(IProgressMonitor)} or
 * {@link #runSilent(IProgressMonitor)} if this operation should not display output in the console.
 */
public class RunPubJob extends Job {

  public static final String UPDATE_COMMAND = "update"; //$NON-NLS-1$
  public static final String INSTALL_COMMAND = "install"; //$NON-NLS-1$

  private static final String PUB_PATH = "util/pub/pub.dart"; //$NON-NLS-1$

  /**
   * The pub command to be run (e.g. "install", "update")
   */
  private final String command;

  /**
   * The directory in which the pub command will be run
   */
  private final IContainer container;

  /**
   * Construct a new job for running a pub command
   * 
   * @param container the directory in which the cmd will be run (not {@code null})
   * @param command the command to be run... either {@link #INSTALL_COMMAND} or
   *          {@link #UPDATE_COMMAND}
   */
  public RunPubJob(IContainer container, String command) {
    super(NLS.bind(PubMessages.RunPubJob_name, command));
    this.command = command;
    this.container = container;
    setRule(container);
  }

  /**
   * Runs the pub command and displays the output in the console.
   * 
   * @return the result of running the pub command
   */
  @Override
  public IStatus run(IProgressMonitor monitor) {
    MessageConsole console = DartCore.getConsole();
    console.clear();
    console.println(NLS.bind(PubMessages.RunPubJob_running, command));
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
    try {

      // Build the process description to run pub

      DartSdk sdk = DartSdkManager.getManager().getSdk();
      File pubFile = new File(sdk.getDirectory().getAbsolutePath(), RunPubJob.PUB_PATH);

      ProcessBuilder builder = new ProcessBuilder();
      builder.directory(container.getLocation().toFile());
      builder.redirectErrorStream(true);

      List<String> args = new ArrayList<String>();
      args.add(sdk.getVmExecutable().getPath());
      args.add(pubFile.getAbsolutePath());
      args.add(command);
      builder.command(args);

      Map<String, String> env = builder.environment();
      env.put("DART_SDK", sdk.getDirectory().getAbsolutePath()); //$NON-NLS-1$

      // Run the pub command as an external process

      ProcessRunner runner = newProcessRunner(builder);
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
      if (!runner.getStdErr().isEmpty()) {
        stringBuilder.append(runner.getStdErr().trim() + "\n"); //$NON-NLS-1$
      }

      int exitCode = runner.getExitCode();
      if (exitCode != 0) {
        String output = "[" + exitCode + "] " + stringBuilder.toString();
        String message = NLS.bind(PubMessages.RunPubJob_failed, command, output);
        return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, message);
      }

      // Refresh the Eclipse resources

      try {
        container.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      } catch (CoreException e) {
        // do nothing  - exception on project refresh
      }
      String message = stringBuilder.toString();
      return new Status(IStatus.OK, DartCore.PLUGIN_ID, message);

    } catch (OperationCanceledException exception) {
      String message = NLS.bind(PubMessages.RunPubJob_canceled, command);
      return new Status(IStatus.CANCEL, DartCore.PLUGIN_ID, message, exception);

    } finally {
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
}
