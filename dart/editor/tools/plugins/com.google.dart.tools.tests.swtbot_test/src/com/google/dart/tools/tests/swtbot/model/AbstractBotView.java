/*
 * Copyright 2014 Dart project authors.
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
package com.google.dart.tools.tests.swtbot.model;

import com.google.dart.engine.index.Index;
import com.google.dart.engine.internal.index.IndexImpl;
import com.google.dart.engine.internal.index.operation.OperationQueue;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.pub.PubBuildParticipant;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

import java.util.HashSet;

@SuppressWarnings("restriction")
public class AbstractBotView {

  private static OperationQueue OpQueue;
  private static HashSet<IContainer> PubContainers;

  static {
    Index index = DartCore.getProjectManager().getIndex();
    IndexImpl impl = (IndexImpl) index;
    OpQueue = ReflectionUtils.getFieldObject(impl, "queue");
    PubContainers = ReflectionUtils.getFieldObject(PubBuildParticipant.class, "currentContainers");
  }

  protected final SWTWorkbenchBot bot;

  public AbstractBotView(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  public void waitForAnalysis() {
    AnalysisManager am = AnalysisManager.getInstance();
    loop : while (true) {
      if (OpQueue.size() > 0 || !am.waitForBackgroundAnalysis(10)) {
        waitForEmptyQueue();
        continue loop;
      }
      if (!PubContainers.isEmpty()) {
        waitForEmptyQueue();
        continue loop;
      }
      waitForEmptyQueue();
      break;
    }
  }

  public void waitForToolsOutput() {
    if (bot.activeView().getViewReference().getPartName().equals("Files")) {
      waitMillis(500); // allow some time for the console to be activated
    }
    if (bot.activeView().getViewReference().getPartName().equals("Tools Output")) {
      waitMillis(500); // allow some time to append text
    }
  }

  private void waitForEmptyQueue() {
    try {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          // nothing to do!
        }
      }, new NullProgressMonitor());
    } catch (CoreException e) {
    }
  }

  private void waitMillis(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      // ignore it
    }
  }
}
