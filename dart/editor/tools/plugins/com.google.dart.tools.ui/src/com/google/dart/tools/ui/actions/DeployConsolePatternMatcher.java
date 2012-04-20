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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;

/**
 * Watch for Dart source file locations in the console and create hyperlinks for them.
 */
public class DeployConsolePatternMatcher implements IPatternMatchListener {

  class DeployConsoleHyperlink implements IHyperlink {
    private Location location;

    public DeployConsoleHyperlink(Location location) {
      this.location = location;
    }

    @Override
    public void linkActivated() {
      openLocation(location);
    }

    @Override
    public void linkEntered() {

    }

    @Override
    public void linkExited() {

    }
  }

  static class Location {
    String file;
    int line;

    public Location(String file, int line) {
      this.file = file;
      this.line = line;
    }

    @Override
    public String toString() {
      return "[" + file + ":" + line + "]";
    }
  }

  private TextConsole console;

  public DeployConsolePatternMatcher() {

  }

  @Override
  public void connect(TextConsole console) {
    this.console = console;
  }

  @Override
  public void disconnect() {
    this.console = null;
  }

  @Override
  public int getCompilerFlags() {
    return 0;
  }

  @Override
  public String getLineQualifier() {
    return null;
  }

  @Override
  public String getPattern() {
    return " .*\\.dart:\\d*,";
  }

  @Override
  public void matchFound(PatternMatchEvent event) {
    try {
      String match = console.getDocument().get(event.getOffset(), event.getLength());

      Location location = parseMatch(match);

      if (location != null && doesLocationExist(location)) {
        int matchOffset = event.getOffset() + match.indexOf(location.file);
        int matchLength = location.file.length() + 1 + Integer.toString(location.line).length();

        console.addHyperlink(new DeployConsoleHyperlink(location), matchOffset, matchLength);
      }
    } catch (BadLocationException e) {

    }
  }

  void openLocation(Location location) {
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(
        new Path(location.file));

    if (files.length == 1) {
      IFile file = files[0];

      IMarker marker = null;

      try {
        IEditorPart editor = IDE.openEditor(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);

        marker = file.createMarker(IMarker.TEXT);
        marker.setAttribute(IMarker.LINE_NUMBER, location.line);

        IDE.gotoMarker(editor, marker);
      } catch (PartInitException e) {
        // We were unable to open the file - indicate this to the user.
        Display.getDefault().beep();

        DartToolsPlugin.log(e);
      } catch (CoreException e) {
        DartToolsPlugin.log(e);
      } finally {
        safeDelete(marker);
      }
    }
  }

  private boolean doesLocationExist(Location location) {
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(
        new Path(location.file));

    return files.length == 1;
  }

  private Location parseMatch(String match) {
    // " foo.dart:123,"

    match = match.trim();

    if (match.endsWith(",")) {
      match = match.substring(0, match.length() - 1);
    }

    String[] strs = match.split(":");

    if (strs.length == 2) {
      try {
        return new Location(strs[0], Integer.parseInt(strs[1]));
      } catch (NumberFormatException nfe) {

      }
    }

    return null;
  }

  private void safeDelete(IMarker marker) {
    try {
      if (marker != null) {
        marker.delete();
      }
    } catch (CoreException e) {

    }
  }

}
