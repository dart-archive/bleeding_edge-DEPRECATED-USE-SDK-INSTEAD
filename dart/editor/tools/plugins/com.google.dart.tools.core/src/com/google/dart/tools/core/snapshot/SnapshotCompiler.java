/*
 * Copyright 2013 Dart project authors.
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

package com.google.dart.tools.core.snapshot;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Use the dart VM to generate a script snapshot file. A Dart snapshot is a binary serialization of
 * the token stream, generated from parsing the code. It can decrease application load time by a
 * factor of 10x.
 * <p>
 * To generate a snapshot: dart --generate-script-snapshot=foo.snapshot foo.dart
 * <p>
 * To use a snapshot: dart --use-script-snapshot=foo.snapshot foo.dart
 * 
 * @see http://www.dartlang.org/articles/snapshots/
 */
public class SnapshotCompiler {

  /**
   * Given a file named something like 'foo.dart', return a file named 'foo.snapshot'.
   * 
   * @param sourceFile
   * @return
   */
  public static File createDestFileName(File sourceFile) {
    String name = sourceFile.getName();

    int index = name.lastIndexOf('.');

    if (index != -1) {
      name = name.substring(0, index);
    }

    name += ".snapshot";

    return new File(sourceFile.getParentFile(), name);
  }

  public SnapshotCompiler() {

  }

  /**
   * Compile the given Dart source file into a Dart snapshot.
   * 
   * @param sourceFile
   * @param destFile
   * @return
   */
  public IStatus compile(File sourceFile, File destFile) {
    ProcessBuilder builder = new ProcessBuilder();

    List<String> args = new ArrayList<String>();

    args.add(DartSdkManager.getManager().getSdk().getVmExecutable().getPath());
    args.add("--generate-script-snapshot=" + destFile.getPath());
    args.add(sourceFile.getPath());

    builder.command(args);
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder);

    try {
      runner.runSync(null);
    } catch (IOException e) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.getMessage());
    }

    refreshParentFolder(destFile);

    int exitCode = runner.getExitCode();
    int severity = exitCode == 0 ? IStatus.OK : IStatus.ERROR;
    return new Status(severity, DartCore.PLUGIN_ID, exitCode, runner.getStdOut(), null);
  }

  public boolean isAvailable() {
    return DartSdkManager.getManager().hasSdk();
  }

  private void refreshParentFolder(File outFile) {
    URI uri = outFile.getParentFile().toURI();
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
