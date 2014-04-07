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
package com.google.dart.tools.core.pub;

import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the pub cache list command to retrieve information about the packages currently installed in
 * the pub cache. Returns a string in JSON format contain the pub cache info if successful.
 * 
 * @coverage dart.tools.core.pub
 */
public class RunPubCacheListJob extends Job {

  public static final String CACHE_COMMAND = "cache"; //$NON-NLS-1$
  public static final String CACHELIST_COMMAND = "list"; //$NON-NLS-1$

  public RunPubCacheListJob() {
    super("Pub cache list");
  }

  @Override
  public IStatus run(IProgressMonitor monitor) {
    List<String> args = buildCacheListCommand();

    ProcessBuilder builder = new ProcessBuilder();
    builder.command(args);
    ProcessRunner runner = new ProcessRunner(builder);
    int result;

    try {
      // The monitor argument is just used to listen for user cancellations.
      result = runner.runSync(monitor);
    } catch (IOException e) {
      DartCore.logError(CACHELIST_COMMAND, e);
      runner.dispose();
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.getMessage(), e);
    }
    if (result != 0) {
      String message = NLS.bind(PubMessages.RunPubJob_failed, CACHELIST_COMMAND, runner.getStdErr());
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, message);
    }
    return new Status(IStatus.OK, DartCore.PLUGIN_ID, runner.getStdOut());
  }

  protected List<String> buildCacheListCommand() {
    DirectoryBasedDartSdk sdk = DartSdkManager.getManager().getSdk();
    File pubFile = sdk.getPubExecutable();
    List<String> args = new ArrayList<String>();
    args.add(pubFile.getAbsolutePath());
    args.add(CACHE_COMMAND);
    args.add(CACHELIST_COMMAND);
    return args;
  }

}
