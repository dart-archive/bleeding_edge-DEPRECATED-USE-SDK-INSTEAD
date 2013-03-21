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

package com.google.dart.tools.tests.buildbot;

import com.google.dart.tools.core.CmdLineOptions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;

/**
 * The plugin activator for the com.google.dart.tools.tests.buildbot plugin.
 */
public class BuildbotPlugin extends Plugin {
  public static final String PLUGIN_ID = "com.google.dart.tools.tests.buildbot";

  private static BuildbotPlugin plugin;

  /**
   * @return the plugin singleton instance
   */
  public static BuildbotPlugin getPlugin() {
    return plugin;
  }

  /**
   * Log the given exception.
   * 
   * @param e
   */
  public void log(Throwable e) {
    getPlugin().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
  }

  @Override
  public void start(BundleContext context) throws Exception {
    plugin = this;

    super.start(context);

    // When the plugin is initialized, check for a --test command-line parameter to the
    // application. If it exists, run the buildbot test suite.
    if (shouldRunTests()) {
      Job job = new BuildbotTestsJob(shouldExit(), TestAll.suite());

      job.schedule(2000);
    }
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    plugin = null;
  }

  private boolean shouldExit() {
    return CmdLineOptions.getOptions().getAutoExit();
  }

  private boolean shouldRunTests() {
    return CmdLineOptions.getOptions().getRunTests();
  }

}
