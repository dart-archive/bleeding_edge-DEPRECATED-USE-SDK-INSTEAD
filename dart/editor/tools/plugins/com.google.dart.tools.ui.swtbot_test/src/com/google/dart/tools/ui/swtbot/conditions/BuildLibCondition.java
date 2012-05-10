/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot.conditions;

import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities.PerformanceListener;
import com.google.dart.tools.ui.swtbot.DartLib;
import com.google.dart.tools.ui.swtbot.performance.SwtBotPerformance;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Condition that waits for the specified library to be built
 */
public final class BuildLibCondition implements ICondition {
  private static Collection<Job> buildJobs;

  /**
   * Start listening to the background job manager for build jobs. This must be called before any
   * background builds have started and before using instances of this class.
   */
  public static void startListening() {
    buildJobs = new ArrayList<Job>();
    Job.getJobManager().addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        Job job = event.getJob();
        if (buildJobs.remove(job)) {
        }
      }

      @Override
      public void scheduled(IJobChangeEvent event) {
        Job job = event.getJob();
        if (job.getName().startsWith("Building workspace")) {
          buildJobs.add(job);
        }
      }
    });
    DartCompilerUtilities.setPerformanceListener(new PerformanceListener() {

      @Override
      public void analysisComplete(long start, String libName) {
        SwtBotPerformance.ANALYZE.log(start, fileNameWithoutExtension(libName));
      }

      @Override
      public void compileComplete(long start, String libName) {
        SwtBotPerformance.COMPILE.log(start, fileNameWithoutExtension(libName));
      }

      private String fileNameWithoutExtension(String libName) {
        String simpleName = libName.substring(libName.lastIndexOf('/') + 1);
        if (simpleName.endsWith(".dart")) {
          simpleName = simpleName.substring(0, simpleName.length() - 5);
        }
        return simpleName;
      }
    });
  }

  private DartLib lib;
  private boolean hasBuildStarted;

  public BuildLibCondition(DartLib lib) {
    if (buildJobs == null) {
      throw new IllegalStateException("Must call " + getClass().getSimpleName()
          + "#startListening before using instances of this class");
    }
    if (lib == null) {
      new IllegalArgumentException();
    }
    this.lib = lib;
    this.hasBuildStarted = false;
  }

  @Override
  public String getFailureMessage() {
    return "Background job still compiling " + lib.name;
  }

  @Override
  public void init(SWTBot bot) {
  }

  @Override
  public boolean test() throws Exception {
    // Ensure that the build has had a chance to start
    if (!hasBuildStarted) {
      if (!buildJobs.isEmpty()) {
        hasBuildStarted = true;
      }
      return false;
    }
    return buildJobs.isEmpty();
  }
}
