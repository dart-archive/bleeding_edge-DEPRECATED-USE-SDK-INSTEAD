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
package com.google.dart.tools.ui.test.model.internal.workbench;

import com.google.dart.tools.ui.test.runnable.ExceptionResult;
import com.google.dart.tools.ui.test.runnable.UIThreadRunnable;

import org.eclipse.ui.IWorkbenchWindow;

/**
 * Base class for workbench commands.
 */
public abstract class WorkbenchCommand {

  /**
   * NOTE: it is callers responsibility to validate post exec.
   */
  public final void run() throws CommandException {
    run(WorkbenchFinder.getActiveWindow());
  }

  /**
   * Execute the given runnable.
   * 
   * @param runnable the runnable to execute
   * @throws CommandException if an exception was caught in execution
   */
  public void syncExec(final CommandRunnable runnable) throws CommandException {

    Exception exception = UIThreadRunnable.syncExec(new ExceptionResult() {

      @Override
      public Exception run() {
        try {
          runnable.run();
          return null;
        } catch (Exception e) {
          return e;
        }
      }
    });

    if (exception != null) {
      throw new CommandException(exception.getMessage());
    }
  }

  protected abstract void run(IWorkbenchWindow activeWindow) throws CommandException;

}
