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

import com.google.dart.engine.internal.index.IndexImpl;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.pub.PubBuildParticipant;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.List;

abstract public class AbstractBotView {

  private static IndexImpl indexer = (IndexImpl) DartCore.getProjectManager().getIndex();

  protected final SWTWorkbenchBot bot;

  public AbstractBotView(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  /**
   * Heuristic to determine when analysis is finished: indexer queue is empty, no background
   * analysis is in progress, pub containers is empty, and eclipse build queue is empty. Even with
   * all that, it sometimes fails.
   * 
   * @see waitForAsyncDrain(), waitForToolsOutput(), waitForProjectToLoad()
   */
  public void waitForAnalysis() {
    AnalysisManager am = AnalysisManager.getInstance();
    loop : while (true) {
      if (!indexer.isOperationQueueEmpty() || !am.waitForBackgroundAnalysis(10)) {
        waitForEmptyQueue();
        continue loop;
      }
      if (!PubBuildParticipant.isPubContainersEmpty()) {
        waitForEmptyQueue();
        continue loop;
      }
      waitForEmptyQueue();
      break;
    }
  }

  /**
   * Wait until all async events have been processed. This is sometimes necessary to allow widgets
   * to finish updating.
   */
  public void waitForAsyncDrain() {
    final boolean[] done = new boolean[1];
    done[0] = false;
    UIThreadRunnable.asyncExec(new VoidResult() {
      @Override
      public void run() {
        done[0] = true;
      }
    });
    while (true) {
      waitMillis(5);
      if (done[0]) {
        return;
      }
    }
  }

  /**
   * Wait for everything to finish after a project has been loaded.
   */
  public void waitForProjectToLoad() {
    waitForAnalysis();
    waitForToolsOutput();
    waitForAsyncDrain();
  }

  /**
   * Hackish way to allow Problems to get updated when a new project is loaded.
   */
  public void waitForToolsOutput() {
    if (bot.activeView().getViewReference().getPartName().equals("Files")) {
      waitMillis(500); // allow some time for the console to be activated
    }
    if (bot.activeView().getViewReference().getPartName().equals("Tools Output")) {
      waitMillis(500); // allow some time to append text
    }
  }

  /**
   * Wait for the given number of milliseconds.
   * 
   * @param millis the number of milliseconds to wait
   */
  public void waitMillis(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      // ignore it
    }
  }

  /**
   * Create a SWTBot for the parent of the given <code>widget</code>.
   * 
   * @param widget a Composite or ToolItem
   * @return the bot for parent of the given <code>widget</code>.
   */
  protected SWTBot getParentBot(final Widget widget) {
    Composite parent = UIThreadRunnable.syncExec(new WidgetResult<Composite>() {
      @Override
      public Composite run() {
        if (widget instanceof ToolItem) {
          return ((ToolItem) widget).getParent();
        } else {
          return ((Composite) widget).getParent();
        }
      }
    });
    return new SWTBot(parent);
  }

  abstract protected String viewName();

  // TODO Delete after debugging
  @SuppressWarnings({"rawtypes", "unchecked"})
  List getAllChildren(Widget comp) {
    return bot.getFinder().findControls(comp, new BaseMatcher() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Get All Children");
      }

      @Override
      public boolean matches(Object item) {
        return true;
      }
    }, true);
  }

  // TODO Delete after debugging
  @SuppressWarnings("rawtypes")
  void inspectWidgets(final List widgets) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        widgets.size(); // for breakpoint
      }
    });
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
}
