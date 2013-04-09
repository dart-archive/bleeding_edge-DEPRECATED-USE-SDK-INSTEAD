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

package com.google.dart.tools.debug.ui.internal.view;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.browser.BrowserLaunchConfigurationDelegate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

// VM exceptions look like:
//
// Unhandled exception:
// foo
// #1      main (file:///Users/devoncarew/projects/dart/dart/editor/util/debuggertest/cmd_test.dart:30:11)

// Dartium exceptions look like:
//
// Exception: foo
// Stack Trace: #0      List.add (bootstrap_impl:787:7)
// #1      testAnimals (http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/editor/util/debuggertest/web_test.dart:55:11)
// #2      rotateText.rotateText (http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/editor/util/debuggertest/web_test.dart:33:14)

/**
 * A console pattern match listener that creates hyperlinks in the console for VM and Dartium
 * exceptions.
 */
@SuppressWarnings("restriction")
public class DebuggerPatternMatchListener implements IPatternMatchListener {

  /**
   * Link in console that opens a browser on the specified URL when clicked
   */
  private static class BrowserLink implements IHyperlink {

    private final URL url;

    public BrowserLink(URL url) {
      this.url = url;
    }

    @Override
    public void linkActivated() {
      try {
        BrowserLaunchConfigurationDelegate.openBrowser(url.toString());
      } catch (CoreException e) {
        DartCore.logInformation("Failed to open " + url, e);
        Display.getDefault().beep();
      }
    }

    @Override
    public void linkEntered() {
    }

    @Override
    public void linkExited() {
    }
  }

  private static class Location {
    private String filePath;
    private int line;

    public Location(String filePath, int line) {
      this.filePath = filePath;
      this.line = line;
    }

    public boolean doesExist() {
      return getFile() != null;
    }

    public IFile getFile() {
      IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(filePath);

      if (resource instanceof IFile) {
        return (IFile) resource;
      }

      IPath path = new Path(filePath);

      return ResourceUtil.getFile(path.toFile());
    }

    public int getLine() {
      return line;
    }

    @Override
    public String toString() {
      return "[" + filePath + ":" + line + "]";
    }
  }

  private TextConsole console;

  public DebuggerPatternMatchListener() {

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
    // (http://127.0.0.1:3030/Users/util/debuggertest/web_test.dart:33:14)
    // http://127.0.0.1:8081

    return "(http://|file://|package:)\\S+";
  }

  @Override
  public void matchFound(PatternMatchEvent event) {
    if (console == null) {
      return;
    }
    try {
      String match = console.getDocument().get(event.getOffset(), event.getLength());

      // Strip extraneous trailing characters
      int index = match.length() - 1;
      while (index > 0) {
        if (Character.isLetterOrDigit(match.charAt(index))) {
          break;
        }
        index--;
      }
      match = match.substring(0, index + 1);

      // Check for reference to line in dart source
      Location location = parseForLocation(match);

      if (location != null && location.doesExist()) {
        console.addHyperlink(
            new FileLink(location.getFile(), null, -1, -1, location.getLine()),
            event.getOffset(),
            match.length());
        return;
      }

      // Check for generic URL
      URL url = parseForUrl(match);
      if (url != null) {
        console.addHyperlink(new BrowserLink(url), event.getOffset(), match.length());
        return;
      }
    } catch (BadLocationException e) {
      // don't create a hyperlink
    }
  }

  protected IResource getResource() {
    if (console instanceof ProcessConsole) {
      ProcessConsole processConsole = (ProcessConsole) console;

      IProcess process = processConsole.getProcess();

      if (process.getLaunch() != null) {
        ILaunch launch = process.getLaunch();
        DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(
            launch.getLaunchConfiguration());

        return wrapper.getApplicationResource();
      }
    }

    return null;
  }

  private IFile getIFileForAbsolutePath(String pathStr) {
    return ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(pathStr));
  }

  private Location parseForLocation(String match) {
    // (http://127.0.0.1:3030/Users/util/debuggertest/web_test.dart:33:14)

    int index = -1;

    if (match.indexOf(".dart:") != -1) {
      index = match.indexOf(".dart:");
      index += ".dart".length();
    } else if (match.indexOf(".js:") != -1) {
      index = match.indexOf(".js:");
      index += ".js".length();
    }

    if (index == -1) {
      return null;
    }

    String url = match.substring(0, index);

    int lineIndex = match.indexOf(':', index + 1);

    if (lineIndex == -1) {
      return null;
    }

    String lineStr = match.substring(index + 1, lineIndex);

    try {
      String filePath;
      int line = Integer.parseInt(lineStr);

      // /Users/foo/dart/serverapp/serverapp.dart
      // file:///Users/foo/dart/webapp2/webapp2.dart
      // http://0.0.0.0:3030/webapp/webapp.dart
      // package:abc/abc.dart

      // resolve package: urls to file: urls
      if (DartCore.isPackageSpec(url)) {
        url = resolvePackageUri(url);
      }

      if (url == null) {
        return null;
      }

      // Handle both fully absolute path names and http: urls.
      if (url.startsWith("/")) {
        IFile file = getIFileForAbsolutePath(url);

        if (file != null) {
          filePath = file.getFullPath().toPortableString();
        } else {
          return null;
        }
      } else if (url.startsWith("file:")) {
        IFile file = getIFileForAbsolutePath(new URI(url).getPath());

        if (file != null) {
          filePath = file.getFullPath().toPortableString();
        } else {
          return null;
        }
      } else {
        filePath = new URI(url).getPath();
      }

      return new Location(filePath, line);
    } catch (NumberFormatException nfe) {
      return null;
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private URL parseForUrl(String match) {
    // http://127.0.0.1:8081

    // references to Dart source should be handled by parseForLocation
    if (match.startsWith("file:") || match.endsWith(".dart")) {
      return null;
    }

    try {
      return new URL(match);
    } catch (MalformedURLException e) {
      return null;
    }
  }

  private String resolvePackageUri(String url) {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      IResource resource = getResource();

      if (resource != null) {
        AnalysisContext context = DartCore.getProjectManager().getContext(resource);

        if (context != null) {
          // TODO: this will resolve most package: urls to files outside of the workspace (in the
          // pub cache). This is not what we want; we want to be able to open the associated
          // resource. Possibly an enhancement for ProjectManager?
          Source source = context.getSourceFactory().forUri(url);

          if (source instanceof FileBasedSource) {
            FileBasedSource fileSource = (FileBasedSource) source;

            File file = new File(fileSource.getFullName());

            return file.toURI().toString();
          }
        }
      }

      return null;
    } else {
      return PackageLibraryManagerProvider.getPackageLibraryManager().resolvePackageUri(url).toString();
    }
  }

}
