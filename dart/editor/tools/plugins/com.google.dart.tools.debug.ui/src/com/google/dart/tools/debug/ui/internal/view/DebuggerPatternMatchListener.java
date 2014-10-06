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

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.CreateContextConsumer;
import com.google.dart.server.MapUriConsumer;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  public static String DARTIUM_PATTERN_1 = "\\((\\S+):(\\d+):(\\d+)\\)";
  public static String DARTIUM_PATTERN_2 = "\\((\\S+):(\\d+)\\)";
  public static String UNITTEST_PATTERN = "^  (\\S*\\.dart) (\\d*)";

  private static Pattern dartiumPattern1 = Pattern.compile(DebuggerPatternMatchListener.DARTIUM_PATTERN_1);
  private static Pattern dartiumPattern2 = Pattern.compile(DebuggerPatternMatchListener.DARTIUM_PATTERN_2);
  private static Pattern unitTestPattern = Pattern.compile(DebuggerPatternMatchListener.UNITTEST_PATTERN);

  private TextConsole console;

  private IResource resource;
  private String resourcePath;

  private String executionContextId;

  public DebuggerPatternMatchListener() {
  }

  @Override
  public void connect(TextConsole console) {
    this.console = console;
    // get the launched resource
    if (console instanceof ProcessConsole) {
      ProcessConsole processConsole = (ProcessConsole) console;
      IProcess process = processConsole.getProcess();
      ILaunch launch = process.getLaunch();
      if (launch != null) {
        ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
        DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(launchConfiguration);
        resource = wrapper.getApplicationResource();
        resourcePath = resource.getLocation().toOSString();
      }
    }
    // create an execution context
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER && resourcePath != null) {
      DartCore.getAnalysisServer().execution_createContext(
          resourcePath,
          new CreateContextConsumer() {
            @Override
            public void computedExecutionContext(String contextId) {
              executionContextId = contextId;
            }
          });
    }
  }

  @Override
  public void disconnect() {
    this.console = null;
    // delete the execution context
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER && executionContextId != null) {
      DartCore.getAnalysisServer().execution_deleteContext(executionContextId);
    }
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

    //   cmd.dart 67:13                                        main.<fn>.<fn>
    //   package:unittest/src/test_case.dart 109:30            _run.<fn>
    //   dart:async/zone.dart 717                              _rootRunUnary

    return "(\\(.*\\))|(  \\S*.dart .*)";
  }

  @Override
  public void matchFound(PatternMatchEvent event) {
    if (console == null) {
      return;
    }

    try {
      String text = console.getDocument().get(event.getOffset(), event.getLength());

      Matcher match = dartiumPattern1.matcher(text);

      if (!match.find()) {
        match = dartiumPattern2.matcher(text);

        if (!match.find()) {
          match = unitTestPattern.matcher(text);

          if (!match.find()) {
            match = null;
          }
        }
      }

      if (match != null) {
        Location location = parseForLocation(match.group(1), match.group(2));

        if (location != null && location.doesExist()) {
          console.addHyperlink(
              new FileLink(location.getFile(), null, -1, -1, location.getLine()),
              event.getOffset() + match.start(1),
              match.end(2) - match.start(1));
          return;
        }
      }
    } catch (BadLocationException e) {
      // don't create a hyperlink

    }
  }

  private IFile getIFileForAbsolutePath(String pathStr) {
    return ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(pathStr));
  }

  private Location parseForLocation(String url, String lineStr) {
    final String chromeExt = "chrome-extension://";

    int line;

    try {
      line = Integer.parseInt(lineStr);
    } catch (NumberFormatException nfe) {
      return null;
    }

    try {
      String filePath;

      // /Users/foo/dart/serverapp/serverapp.dart
      // file:///Users/foo/dart/webapp2/webapp2.dart
      // http://0.0.0.0:3030/webapp/webapp.dart
      // package:abc/abc.dart
      // chrome-extension://kcjgcakhgelcejampmijgkjkadfcncjl/spark.dart

      // resolve package: urls to file: urls
      if (DartCore.isPackageSpec(url)) {
        url = resolvePackageUri(url);
      }

      if (url == null) {
        return null;
      }

      // Special case Chrome extension paths.
      if (url.startsWith(chromeExt)) {
        url = url.substring(chromeExt.length());
        if (url.indexOf('/') != -1) {
          url = url.substring(url.indexOf('/') + 1);
        }
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
      } else if (url.indexOf(':') == -1) {
        // handle relative file path
        filePath = resolveRelativePath(url);
      } else {
        filePath = new URI(url).getPath();
      }

      // dart:
      if (filePath == null) {
        return null;
      } else {
        return new Location(filePath, line);
      }
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private String resolvePackageUri(String url) {
    if (resource != null) {
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        if (executionContextId != null) {
          final String[] filePathPtr = {null};
          final CountDownLatch latch = new CountDownLatch(1);
          DartCore.getAnalysisServer().execution_mapUri(
              executionContextId,
              null,
              url,
              new MapUriConsumer() {
                @Override
                public void computedFileOrUri(String file, String uri) {
                  filePathPtr[0] = file;
                  latch.countDown();
                }
              });
          Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.SECONDS);
          return filePathPtr[0];
        }
      } else {
        IFile file = DartCore.getProjectManager().resolvePackageUri(resource, url);
        if (file != null) {
          return file.getLocation().toFile().toURI().toString();
        }
      }
    }
    return null;
  }

  private String resolveRelativePath(String url) {
    if (resource != null) {
      IResource file = resource.getParent().findMember(url);
      if (file != null) {
        return file.getLocation().toPortableString();
      }
    }
    return null;
  }
}
