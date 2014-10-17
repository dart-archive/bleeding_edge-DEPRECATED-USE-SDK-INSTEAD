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

import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.debug.core.source.UriToFileResolver;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

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
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      IFile file = workspaceRoot.getFileForLocation(new Path(filePath));
      if (file != null) {
        return file;
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

  private UriToFileResolver uriToFileResolver;

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
        uriToFileResolver = new UriToFileResolver(launch);
      }
    }
  }

  @Override
  public void disconnect() {
    this.console = null;
    uriToFileResolver.dispose();
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

  private Location parseForLocation(String url, String lineStr) {
    String filePath = uriToFileResolver.getFileForUri(url);
    if (filePath == null) {
      return null;
    }

    int line;
    try {
      line = Integer.parseInt(lineStr);
    } catch (NumberFormatException nfe) {
      return null;
    }

    return new Location(filePath, line);
  }
}
