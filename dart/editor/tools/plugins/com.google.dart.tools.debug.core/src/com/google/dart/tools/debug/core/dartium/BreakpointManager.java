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

package com.google.dart.tools.debug.core.dartium;

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.util.IResourceResolver;
import com.google.dart.tools.debug.core.webkit.WebkitBreakpoint;
import com.google.dart.tools.debug.core.webkit.WebkitCallback;
import com.google.dart.tools.debug.core.webkit.WebkitLocation;
import com.google.dart.tools.debug.core.webkit.WebkitResult;
import com.google.dart.tools.debug.core.webkit.WebkitScript;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handle adding a removing breakpoints to the WebKit connection for the DartiumDebugTarget class.
 */
public class BreakpointManager implements IBreakpointListener, DartBreakpointManager {
  public static class NullBreakpointManager implements DartBreakpointManager {

    public NullBreakpointManager() {

    }

    @Override
    public void connect() throws IOException {

    }

    @Override
    public void dispose(boolean deleteAll) {

    }

    @Override
    public DartBreakpoint getBreakpointFor(WebkitLocation location) {
      return null;
    }

    @Override
    public void handleBreakpointResolved(WebkitBreakpoint breakpoint) {

    }

    @Override
    public void handleGlobalObjectCleared() {

    }
  }

  private static String PACKAGES_DIRECTORY_PATH = "/packages/";

  private static String LIB_DIRECTORY_PATH = "/lib/";

  private DartiumDebugTarget debugTarget;
  private Map<IBreakpoint, List<String>> breakpointToIdMap = new HashMap<IBreakpoint, List<String>>();

  private Map<String, DartBreakpoint> breakpointsToUpdateMap = new HashMap<String, DartBreakpoint>();

  private List<IBreakpoint> ignoredBreakpoints = new ArrayList<IBreakpoint>();

  public BreakpointManager(DartiumDebugTarget debugTarget) {
    this.debugTarget = debugTarget;
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    if (debugTarget.supportsBreakpoint(breakpoint)) {
      try {
        addBreakpoint((DartBreakpoint) breakpoint);
      } catch (IOException exception) {
        if (!debugTarget.isTerminated()) {
          DartDebugCorePlugin.logError(exception);
        }
      }
    }
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    // We generate this change event in the handleBreakpointResolved() method - ignore one
    // instance of the event.
    if (ignoredBreakpoints.contains(breakpoint)) {
      ignoredBreakpoints.remove(breakpoint);
      return;
    }

    if (debugTarget.supportsBreakpoint(breakpoint)) {
      breakpointRemoved(breakpoint, delta);
      breakpointAdded(breakpoint);
    }
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    if (debugTarget.supportsBreakpoint(breakpoint)) {
      List<String> breakpointIds = breakpointToIdMap.get(breakpoint);

      if (breakpointIds != null) {
        for (String breakpointId : breakpointIds) {
          breakpointsToUpdateMap.remove(breakpointId);

          try {
            debugTarget.getWebkitConnection().getDebugger().removeBreakpoint(breakpointId);
          } catch (IOException exception) {
            if (!debugTarget.isTerminated()) {
              DartDebugCorePlugin.logError(exception);
            }
          }
        }
      }
    }
  }

  @Override
  public void connect() throws IOException {
    IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
        DartDebugCorePlugin.DEBUG_MODEL_ID);

    for (IBreakpoint breakpoint : breakpoints) {
      if (debugTarget.supportsBreakpoint(breakpoint)) {
        addBreakpoint((DartBreakpoint) breakpoint);
      }
    }

    DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
  }

  @Override
  public void dispose(boolean deleteAll) {
    // Null check for when the editor is shutting down.
    if (DebugPlugin.getDefault() != null) {
      if (deleteAll) {
        try {
          for (List<String> ids : breakpointToIdMap.values()) {
            if (ids != null) {
              for (String id : ids) {
                debugTarget.getWebkitConnection().getDebugger().removeBreakpoint(id);
              }
            }
          }
        } catch (IOException exception) {
          if (!debugTarget.isTerminated()) {
            DartDebugCorePlugin.logError(exception);
          }
        }
      }

      DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
    }
  }

  @Override
  public DartBreakpoint getBreakpointFor(WebkitLocation location) {
    IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
        DartDebugCorePlugin.DEBUG_MODEL_ID);

    WebkitScript script = debugTarget.getWebkitConnection().getDebugger().getScript(
        location.getScriptId());

    if (script == null) {
      return null;
    }

    String url = script.getUrl();
    int line = WebkitLocation.webkitToElipseLine(location.getLineNumber());

    for (IBreakpoint bp : breakpoints) {
      if (debugTarget.supportsBreakpoint(bp)) {
        DartBreakpoint breakpoint = (DartBreakpoint) bp;

        if (breakpoint.getLine() == line) {
          IFile file = breakpoint.getFile();
          String bpUrl = null;
          if (file != null) {
            bpUrl = getResourceResolver().getUrlForResource(file);
          } else {
            bpUrl = breakpoint.getFilePath();
          }

          if (bpUrl != null && bpUrl.equals(url)) {
            return breakpoint;
          }
        }
      }
    }

    return null;
  }

  @VisibleForTesting
  public String getPackagePath(String regex, IResource resource) {
    Path path = new Path(regex);
    int i = 0;
    if (regex.indexOf(LIB_DIRECTORY_PATH) != -1) {
      // remove all segments after "lib", they show path in the package
      while (i < path.segmentCount() && !path.segment(i).equals("lib")) {
        i++;
      }
    } else {
      i = 1;
    }
    String filePath = regex;
    if (path.segmentCount() > i + 1) {
      filePath = new Path(regex).removeLastSegments(path.segmentCount() - (i + 1)).toString();
    }

    String packagePath = resolvePathToPackage(resource, filePath);
    if (packagePath != null) {
      packagePath += "/" + path.removeFirstSegments(i + 1);
    }
    return packagePath;
  }

  @Override
  public void handleBreakpointResolved(WebkitBreakpoint webkitBreakpoint) {
    DartBreakpoint breakpoint = breakpointsToUpdateMap.get(webkitBreakpoint.getBreakpointId());

    if (breakpoint != null) {
      int eclipseLine = WebkitLocation.webkitToElipseLine(webkitBreakpoint.getLocation().getLineNumber());

      if (breakpoint.getLine() != eclipseLine) {
        ignoredBreakpoints.add(breakpoint);

        String message = "[breakpoint in " + breakpoint.getName() + " moved from line "
            + breakpoint.getLine() + " to " + eclipseLine + "]";
        debugTarget.writeToStdout(message);

        breakpoint.updateLineNumber(eclipseLine);
      }
    }
  }

  @Override
  public void handleGlobalObjectCleared() {

  }

  @VisibleForTesting
  protected String resolvePathToPackage(IResource resource, String filePath) {
    return debugTarget.getUriToFileResolver().getUriForPath(filePath);
  }

  void addToBreakpointMap(IBreakpoint breakpoint, String id, boolean trackChanges) {
    synchronized (breakpointToIdMap) {
      if (breakpointToIdMap.get(breakpoint) == null) {
        breakpointToIdMap.put(breakpoint, new ArrayList<String>());
      }

      breakpointToIdMap.get(breakpoint).add(id);

      if (trackChanges) {
        breakpointsToUpdateMap.put(id, (DartBreakpoint) breakpoint);
      }
    }
  }

  private void addBreakpoint(final DartBreakpoint breakpoint) throws IOException {
    if (breakpoint.isBreakpointEnabled()) {

      String regex = null;
      IFile file = breakpoint.getFile();
      if (file != null) {
        regex = getResourceResolver().getUrlRegexForResource(file);
      } else {
        regex = getUrlRegexForNonResource(breakpoint);
      }

      if (regex == null) {
        return;
      }

      int line = WebkitLocation.eclipseToWebkitLine(breakpoint.getLine());

      int packagesIndex = regex.indexOf(PACKAGES_DIRECTORY_PATH);

      if (packagesIndex != -1) {
        // convert xxx/packages/foo/foo.dart to *foo/foo.dart
        regex = regex.substring(packagesIndex + PACKAGES_DIRECTORY_PATH.length());
      } else if (file != null && isInSelfLinkedLib(file)) {
        // Check if source is located in the "lib" directory; if there is a link to it from the 
        // packages directory breakpoint should be /packages/...
        String packageName = DartCore.getSelfLinkedPackageName(file);

        if (packageName != null) {
          // Create a breakpoint for self-links.
          int libIndex = regex.lastIndexOf(LIB_DIRECTORY_PATH);
          String packageRegex = packageName + "/"
              + regex.substring(libIndex + LIB_DIRECTORY_PATH.length());

          debugTarget.getWebkitConnection().getDebugger().setBreakpointByUrl(
              null,
              packageRegex,
              line,
              -1,
              new WebkitCallback<String>() {
                @Override
                public void handleResult(WebkitResult<String> result) {
                  if (!result.isError()) {
                    addToBreakpointMap(breakpoint, result.getResult(), false);
                  }
                }
              });
        }
      }

      // check if it is a file in a package, if so replace regex with package uri path
      // TODO(keertip): revisit when moved to calling pub for package info
      IResource resource = null;
      if (file != null) {
        resource = file;
      } else {
        DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(
            debugTarget.getLaunch().getLaunchConfiguration());
        resource = wrapper.getProject();
      }

      String packagePath = getPackagePath(regex, resource);
      if (packagePath != null) {
        regex = packagePath;
      }

      debugTarget.getWebkitConnection().getDebugger().setBreakpointByUrl(
          null,
          regex,
          line,
          -1,
          new WebkitCallback<String>() {
            @Override
            public void handleResult(WebkitResult<String> result) {
              if (!result.isError()) {
                addToBreakpointMap(breakpoint, result.getResult(), true);
              }
            }
          });

      // Handle source mapped breakpoints - set an additional breakpoint if the file is under
      // source mapping.
      SourceMapManager sourceMapManager = debugTarget.getSourceMapManager();

      if (file != null) {
        if (sourceMapManager != null && sourceMapManager.isMapTarget(file)) {
          List<SourceMapManager.SourceLocation> locations = sourceMapManager.getReverseMappingsFor(
              file,
              line);

          for (SourceMapManager.SourceLocation location : locations) {
            String mappedRegex = getResourceResolver().getUrlRegexForResource(location.getFile());

            if (mappedRegex != null) {
              if (DartDebugCorePlugin.LOGGING) {
                System.out.println("breakpoint [" + regex + "," + breakpoint.getLine()
                    + ",-1] ==> mapped to [" + mappedRegex + "," + location.getLine() + ","
                    + location.getColumn() + "]");
              }

              debugTarget.getWebkitConnection().getDebugger().setBreakpointByUrl(
                  null,
                  mappedRegex,
                  location.getLine(),
                  location.getColumn(),
                  new WebkitCallback<String>() {
                    @Override
                    public void handleResult(WebkitResult<String> result) {
                      if (!result.isError()) {
                        addToBreakpointMap(breakpoint, result.getResult(), false);
                      }
                    }
                  });
            }
          }
        }
      }
    }
  }

  private IResourceResolver getResourceResolver() {
    return debugTarget.getResourceResolver();
  }

  private String getUrlRegexForNonResource(DartBreakpoint breakpoint) {
    String url = breakpoint.getFilePath();
    Path path = new Path(url);
    int i = 0;
    if (url.indexOf("lib") != -1) {
      // find lib
      while (i < path.segmentCount() && !path.segment(i).equals("lib")) {
        i++;
      }
    }
    // get the library name
    if (i > 2) {
      url = path.removeFirstSegments(i - 2).toPortableString();
    }
    return url;
  }

  private boolean isInSelfLinkedLib(IFile file) {
    if (file == null) {
      return false;
    }

    return isPubLib(file.getParent());
  }

  private boolean isPubLib(IContainer container) {
    if (container.getParent() == null) {
      return false;
    }

    if (container.getName().equals("lib")) {
      if (DartCore.isApplicationDirectory(container.getParent())) {
        return true;
      }
    }

    if (container.getParent() instanceof IWorkspaceRoot) {
      return false;
    }

    return isPubLib(container.getParent());
  }
}
