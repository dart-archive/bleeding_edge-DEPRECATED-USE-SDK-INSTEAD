/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.internal.formatter;

import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches the <code>dartfmt</code> process collecting stdout, stderr, and exit code information.
 */
public class DartFormatter {

//  /**
//   * Run the formatter on the given input path.
//   * 
//   * @param path the path to pass to the formatter
//   * @param monitor the monitor for displaying progress
//   * @param console the console to which output should be directed
//   * @throws IOException if an exception was thrown during execution
//   * @throws CoreException if an exception occurs in file refresh
//   */
//  public static void format(IPath path, IProgressMonitor monitor, MessageConsole console)
//      throws IOException, CoreException {
//
//    File dartfmt = DartSdkManager.getManager().getSdk().getDartFmtExecutable();
//    if (!dartfmt.canExecute()) {
//      return;
//    }
//
//    ProcessBuilder builder = new ProcessBuilder();
//
//    List<String> args = new ArrayList<String>();
//    args.add(dartfmt.getPath());
//    args.addAll(buildArguments(path));
//
//    builder.command(args);
//    builder.redirectErrorStream(true);
//
//    ProcessRunner runner = new ProcessRunner(builder);
//    runner.runSync(monitor);
//
//    if (runner.getExitCode() == 0) {
//      ResourcesPlugin.getWorkspace().getRoot().getFile(path).refreshLocal(
//          IResource.DEPTH_INFINITE,
//          monitor);
//    }
//
//    StringBuilder sb = new StringBuilder();
//
//    if (!runner.getStdOut().isEmpty()) {
//      sb.append(runner.getStdOut() + "\n");
//    }
//
//    //TODO (pquitslund): better error handling
//    if (!runner.getStdErr().isEmpty()) {
//      sb.append(runner.getStdErr() + "\n");
//    }
//
//    console.print(sb.toString());
//
//  }

  /**
   * Run the formatter on the given input source.
   * 
   * @param source the source to pass to the formatter
   * @param monitor the monitor for displaying progress
   * @param console the console to which output should be directed
   * @throws IOException if an exception was thrown during execution
   * @throws CoreException if an exception occurs in file refresh
   * @return the formatted source string (or null in case formatting could not be executed)
   */
  public static String format(final String source, IProgressMonitor monitor, MessageConsole console)
      throws IOException, CoreException {

    File dartfmt = DartSdkManager.getManager().getSdk().getDartFmtExecutable();
    if (!dartfmt.canExecute()) {
      return null;
    }

    ProcessBuilder builder = new ProcessBuilder();

    List<String> args = new ArrayList<String>();
    args.add(dartfmt.getPath());

    builder.command(args);
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder) {
      @Override
      protected void processStarted(Process process) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
            process.getOutputStream(),
            "UTF-8"), source.length());
        writer.append(source);
        writer.close();
      }
    };

    runner.runSync(monitor);

    StringBuilder sb = new StringBuilder();

    if (!runner.getStdOut().isEmpty()) {
      sb.append(runner.getStdOut());
    }

    //TODO (pquitslund): better error handling
    if (runner.getExitCode() != 0) {
      throw new IOException(runner.getStdErr());
    }

    //TODO (pquitslund): figure out why we need to remove an extra trailing NEWLINE
    String formattedSource = sb.toString();
    if (formattedSource.endsWith("\n\n")) {
      return formattedSource.substring(0, formattedSource.length() - 1);
    }
    return formattedSource;
  }

  public static boolean isAvailable() {
    return DartSdkManager.getManager().getSdk().getDartFmtExecutable().canExecute();
  }

//  private static List<String> buildArguments(IPath path) {
//    ArrayList<String> args = new ArrayList<String>();
//    args.add("-w");
//    args.add(path.toOSString());
//    return args;
//  }

}
