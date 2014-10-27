/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.core.generator;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around the Stagehand CLI tool.
 */
public class Stagehand {
  public static class StagehandTuple {
    public String id;
    public String description;
    public String entrypoint;

    public StagehandTuple(String id, String description) {
      this.id = id;
      this.description = description;
    }

    public StagehandTuple(String id, String description, String entrypoint) {
      this.id = id;
      this.description = description;
      this.entrypoint = entrypoint;
    }

    @Override
    public String toString() {
      return "[" + id + "," + description + "]";
    }
  }

  private static final List<StagehandTuple> EMPTY = new ArrayList<Stagehand.StagehandTuple>();

  public Stagehand() {

  }

  /**
   * Generate the given sample id into the given directory. Return the main (launchable) file, if
   * any.
   */
  public void generateInto(File projectDirectory, String sampleId) throws StagehandException {
    // pub global run stagehand sample
    ProcessRunner runner = createPubRunner(
        new String[] {"global", "run", "stagehand", sampleId},
        projectDirectory);

    try {
      int retValue = runner.runSync(null);

      if (retValue != 0) {
        throw new StagehandException(runner.getStdErr());
      }
    } catch (IOException e) {
      throw new StagehandException(e);
    }
  }

  public List<StagehandTuple> getAvailableSamples() {
    // pub global run stagehand --machine
    ProcessRunner runner = createPubRunner(new String[] {"global", "run", "stagehand", "--machine"});

    try {
      int exitCode = runner.runSync(null);

      if (exitCode != 0) {
        return EMPTY;
      }

      // [{"name":"consoleapp","description":"A minimal command-line application."},{"name",...
      JSONArray arr = new JSONArray(runner.getStdOut());
      List<StagehandTuple> result = new ArrayList<Stagehand.StagehandTuple>();

      for (int i = 0; i < arr.length(); i++) {
        JSONObject obj = arr.getJSONObject(i);

        result.add(new StagehandTuple(
            obj.getString("name"),
            obj.getString("description"),
            obj.optString("entrypoint")));
      }

      return result;
    } catch (IOException e) {
      DartCore.logError(e);
      return EMPTY;
    } catch (JSONException e) {
      DartCore.logError(e);
      return EMPTY;
    }
  }

  public void install() {
    // pub global activate stagehand
    DartSdkManager.getManager().getSdk().getVmExecutable().getPath();
    ProcessRunner runner = createPubRunner(new String[] {"global", "activate", "stagehand"});

    try {
      runner.runSync(null);
    } catch (IOException e) {

    }
  }

  public boolean isInstalled() {
    // pub global list
    ProcessRunner runner = createPubRunner(new String[] {"global", "list"});

    try {
      int exitCode = runner.runSync(null);

      if (exitCode != 0) {
        return false;
      }

      String[] lines = runner.getStdOut().split("\n");

      for (String line : lines) {
        if (line.startsWith("stagehand ")) {
          return true;
        }
      }

      return false;
    } catch (IOException e) {
      return false;
    }
  }

  public void upgrade() {
    // pub global activate stagehand
    install();
  }

  private ProcessRunner createPubRunner(String[] args) {
    return createPubRunner(args, null);
  }

  private ProcessRunner createPubRunner(String[] args, File cwd) {
    String vmPath = DartSdkManager.getManager().getSdk().getPubExecutable().getPath();
    List<String> list = new ArrayList<String>();
    list.add(vmPath);
    for (String str : args) {
      list.add(str);
    }
    ProcessBuilder builder = new ProcessBuilder(list);
    if (cwd != null) {
      builder.directory(cwd);
    }
    return new ProcessRunner(builder);
  }
}
