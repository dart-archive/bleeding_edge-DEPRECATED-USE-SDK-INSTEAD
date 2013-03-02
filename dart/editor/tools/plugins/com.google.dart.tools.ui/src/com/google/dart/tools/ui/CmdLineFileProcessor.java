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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.CmdLineOptions;
import com.google.dart.tools.core.internal.perf.Performance;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.actions.CreateAndRevealProjectAction;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import java.io.File;

/**
 * Instances of {@code CmdLineFileProcessor} open editor tabs for each of the *.dart files specified
 * on the command line and open projects for each of the directories specified on the command line.
 * Typical usage is:
 * 
 * <pre>
 * new CmdLineFileProcessor({@link CmdLineOptions#getOptions()}).{@link #run()};
 * </pre>
 */
public class CmdLineFileProcessor {

  private final CmdLineOptions options;

  /**
   * Create a new instance that opens tabs and projects based upon the files specified in the
   * options.
   * 
   * @param options the options (not {@code null})
   */
  public CmdLineFileProcessor(CmdLineOptions options) {
    this.options = options;
  }

  /**
   * Loop through the files input on the command line, retrieved from
   * {@link CmdLineOptions#getFiles()}, and open them in the Editor appropriately.
   */
  public void run() {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        for (File file : options.getFiles()) {
          processFile(file);
        }
        if (options.getMeasurePerformance()) {
          Performance.TIME_TO_OPEN.log(options.getStartTime());
        }
      }
    });
  }

  /**
   * Process the specified file by opening the editor tab or project corresponding to that file.
   * This method MUST be called on the UI thread.
   * 
   * @param file the file (not {@code null})
   */
  private void processFile(File file) {

    // If the file does not exist, then make an attempt locating the file
    // relative to the Dart Editor install directory
    if (!file.exists()) {
      File eclipseInstallFile = new File(Platform.getInstallLocation().getURL().getFile());
      File altFile = new File(eclipseInstallFile, file.getPath());
      if (!altFile.exists()) {
        System.out.println("Input \"" + file.getPath() //$NON-NLS-1$
            + "\" could not be parsed as a valid file. Files paths specified on the command line " //$NON-NLS-1$
            + "must be absolute, or relative to the current working directory, or relative to " //$NON-NLS-1$
            + "the directory in which the Dart Editor is installed."); //$NON-NLS-1$
        return;
      }
      file = altFile;
    }
    file = file.getAbsoluteFile();

    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (file.isFile()) {
      // Open the directory containing the file and then the file
      String directoryToOpen = file.getParentFile().getAbsolutePath();
      new CreateAndRevealProjectAction(workbenchWindow, directoryToOpen).run();
      try {
        EditorUtility.openInEditor(ResourceUtil.getFile(file));
      } catch (PartInitException e) {
        e.printStackTrace();
      } catch (DartModelException e) {
        e.printStackTrace();
      }
    } else {
      // If this File to open is a directory, instead of a file, then just open the directory.
      String directoryToOpen = file.getAbsolutePath();
      new CreateAndRevealProjectAction(workbenchWindow, directoryToOpen).run();
    }
  }

}
