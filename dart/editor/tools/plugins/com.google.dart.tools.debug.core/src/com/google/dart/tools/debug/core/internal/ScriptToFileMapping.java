/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.core.internal;

import com.google.dart.tools.debug.core.internal.util.LogTimer;

import org.chromium.sdk.JavascriptVm.ScriptsCallback;
import org.chromium.sdk.Script;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class maps from JavaScript scripts loaded into the target VM, and paths to the scripts on
 * disk.
 */
class ScriptToFileMapping {
  private Set<Script> scripts = new HashSet<Script>();

  private ChromeDebugTarget debugTarget;

  public ScriptToFileMapping(ChromeDebugTarget debugTarget) {
    this.debugTarget = debugTarget;

    LogTimer timer = new LogTimer("JS script download");

    this.debugTarget.getJavascriptVm().getScripts(new ScriptsCallback() {
      @Override
      public void failure(String errorMessage) {
        ScriptToFileMapping.this.debugTarget.reportError("Error Retrieving VM Sources",
            errorMessage);
      }

      @Override
      public void success(Collection<Script> inScripts) {
        scripts = new HashSet<Script>(inScripts);
      }
    });

    timer.endTimer();
  }

  public String getScriptName() {
    // Look for dart --> js scripts.
    for (Script script : scripts) {
      String name = script.getName();

      if (name != null && name.endsWith(".app.js")) {
        return name;
      }
    }

    // Look for dart scripts.
    for (Script script : scripts) {
      String name = script.getName();

      if (name != null && name.endsWith(".app")) {
        return name;
      }
    }

    return null;
  }

  public void handleScriptCollected(Script script) {
    scripts.remove(script);
  }

  public void handleScriptContentChanged(Script script) {

  }

  public void handleScriptLoaded(Script script) {
    scripts.add(script);
  }

  public String mapFromScriptToFileName(Script script) {
    String path = script.getName();

    int index = path.indexOf('/');

    if (index != -1) {
      path = path.substring(path.lastIndexOf('/') + 1);
    }

    return path;
  }

}
