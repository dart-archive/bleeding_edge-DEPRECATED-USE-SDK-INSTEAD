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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
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
 * <p>
 * Match:
 * <ul>
 * <li>(file:///C:/foo/bar/baz.dart:71:3)
 * <li>(package:web_ui/dwc.dart:71:3)
 * </ul>
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
    // (file:///C:/foo/bar/baz.dart:71:3)
    // (package:web_ui/dwc.dart:71:3)

    return "\\((file|package):.*\\.dart:\\d*:\\d*\\)";
  }

  @Override
  public void matchFound(PatternMatchEvent event) {
    if (console == null) {
      return;
    }

    try {
      String match = console.getDocument().get(event.getOffset(), event.getLength());

      Location location = parseMatch(match);

      if (location != null && doesLocationExist(location)) {
        console.addHyperlink(
            new DeployConsoleHyperlink(location),
            event.getOffset(),
            event.getLength());
      }
    } catch (BadLocationException e) {

    }
  }

  void openLocation(Location location) {
    @SuppressWarnings("deprecation")
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(
        new Path(location.file));

    if (files.length == 1) {
      IFile file = files[0];

      IMarker marker = null;

      try {
        IEditorPart editor = IDE.openEditor(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
            file);

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
    @SuppressWarnings("deprecation")
    IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(
        new Path(location.file));

    return files.length == 1;
  }

  private Location parseMatch(String match) {
    // "(file:///Users/devoncarew/temp/pub-test/build.dart:23:3)"
    // "(package:web_ui/dwc.dart:71:3)"

    match = stripParens(match.trim());

    // Remove the last :num
    int index = match.lastIndexOf(':');

    if (index == -1) {
      return null;
    }

    match = match.substring(0, index);

    index = match.lastIndexOf(':');

    if (index == -1) {
      return null;
    }

    String url = match.substring(0, index);
    int lineNumber = 1;

    try {
      lineNumber = Integer.parseInt(match.substring(index + 1));
    } catch (NumberFormatException nfe) {
      return null;
    }

    if (DartCore.isPackageSpec(url)) {
      // Locate the corresponding file for "package:web_ui/dwc.dart".
      String filePath = resolvePackageUrl(url);

      if (filePath != null) {
        return new Location(filePath, lineNumber);
      } else {
        return null;
      }
    } else if (url.startsWith("file:")) {
      if (url.startsWith("file://")) {
        url = url.substring("file://".length());
      } else {
        url = url.substring("file:".length());
      }

      return new Location(url, lineNumber);
    } else {
      return null;
    }
  }

  /**
   * Given "package:web_ui/dwc.dart", return "/Users/foo/dart/sample/packages/web_ui/dwc.dart".
   * 
   * @param packageUrl
   * @return
   */
  private String resolvePackageUrl(String packageUrl) {
    String urlSnippet = packageUrl.substring("package:".length());

    //TODO (danrubel): map package reference to packages returned by pub-list when packages
    // directory does not exist AND when pubspec is not in project folder
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      IFolder packagesDir = project.getFolder(DartCore.PACKAGES_DIRECTORY_NAME);

      if (packagesDir.exists()) {
        IFile file = packagesDir.getFile(urlSnippet);

        if (file.exists() && file.isAccessible()) {
          return file.getLocation().toPortableString();
        }
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

  private String stripParens(String str) {
    if (str.startsWith("(") && str.endsWith(")")) {
      return str.substring(1, str.length() - 1);
    } else {
      return str;
    }
  }
}
