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

package com.google.dart.tools.core.generator;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.generator.Stagehand.StagehandTuple;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract template class to create sample applications.
 * 
 * @coverage dart.tools.core.generator
 */
public class AbstractSample implements Comparable<AbstractSample> {
  private static Stagehand stagehand = new Stagehand();

  private static List<AbstractSample> cachedSamples;

  /**
   * Get all the known samples. This method can be slow.
   * 
   * @return all the known samples
   */
  public static List<AbstractSample> getAllSamples() {
    if (cachedSamples != null) {
      return cachedSamples;
    }

    boolean doUpgradeCheck = true;

    if (!stagehand.isInstalled()) {
      doUpgradeCheck = false;

      stagehand.install();
    }

    List<StagehandTuple> samples = stagehand.getAvailableSamples();

    // Make sure we're on a reasonably latest version of Stagehand.
    if (doUpgradeCheck) {
      new Thread() {
        @Override
        public void run() {
          stagehand.upgrade();
        }
      }.start();
    }

    cachedSamples = new ArrayList<AbstractSample>();
    // TODO(devoncarew): Remove this when/if a chrome app sample is in stagehand.
    cachedSamples.add(new ChromePackagedAppSample());

    for (StagehandTuple sample : samples) {
      cachedSamples.add(new StagehandSample(
          stagehand,
          sample.id,
          sample.description,
          sample.entrypoint));
    }

    Collections.sort(cachedSamples);

    return cachedSamples;
  }

  private String title;
  private String description;
  private List<String[]> templates = new ArrayList<String[]>();
  protected String mainFile;

  public AbstractSample(String title, String description) {
    this.title = title;
    this.description = description;
  }

  @Override
  public int compareTo(AbstractSample other) {
    return getTitle().compareToIgnoreCase(other.getTitle());
  }

  public IFile generateInto(IContainer container, String sampleName) throws CoreException {
    Map<String, String> variables = new HashMap<String, String>();

    variables.put("{name}", sampleName);
    variables.put("{name.lower}", sampleName.toLowerCase());
    variables.put("{name.title}", toTitleCase(sampleName));

    for (String[] template : templates) {
      String path = substitute(template[0], variables);
      String contents = substitute(template[1], variables);

      try {
        if (contents.startsWith("@") && !contents.endsWith(".png")) {
          contents = substitute(getResourceAsString(contents.substring(1)), variables);
        }
      } catch (IOException e) {
        throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.toString(), e));
      }

      if (path.startsWith(".settings")) {
        createFile(container.getProject(), path, contents);
      } else {
        createFile(container, path, contents);
      }
    }

    if (mainFile != null) {
      IResource resource = container.findMember(substitute(mainFile, variables));

      if (resource instanceof IFile) {
        return (IFile) resource;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  public String getDescription() {
    return description;
  }

  public String getTitle() {
    return title;
  }

  public boolean isValidProjectName(String name) {
    return !name.equalsIgnoreCase(getTitle());
  }

  public boolean shouldBeDefault() {
    return false;
  }

  @Override
  public String toString() {
    return getTitle();
  }

  protected void setMainFile(String mainFile) {
    this.mainFile = mainFile;
  }

  protected void setTemplates(List<String[]> templates) {
    this.templates = templates;
  }

  private IFile createFile(IContainer container, String path, String contents) throws CoreException {
    IFile file = container.getFile(new Path(path));

    if (file.getParent() instanceof IFolder) {
      createFolder((IFolder) file.getParent(), false, true, null);
    }

    InputStream in;

    if (contents.startsWith("@")) {
      in = getClass().getResourceAsStream(contents.substring(1));
    } else {
      in = new ByteArrayInputStream(contents.getBytes(Charsets.UTF_8));
    }

    file.create(in, false, null);

    return file;
  }

  private void createFolder(IFolder folder, boolean force, boolean local, IProgressMonitor monitor)
      throws CoreException {
    if (!folder.exists()) {
      IContainer parent = folder.getParent();
      if (parent instanceof IFolder) {
        createFolder((IFolder) parent, force, local, null);
      }
      folder.create(force, local, monitor);
    }
  }

  private String getResourceAsString(String resourceName) throws IOException {
    InputStream in = getClass().getResourceAsStream(resourceName);

    if (in == null) {
      return "";
    } else {
      Reader reader = new InputStreamReader(in, Charsets.UTF_8);

      return CharStreams.toString(reader);
    }
  }

  private String substitute(String str, Map<String, String> variables) {
    for (String var : variables.keySet()) {
      int index = str.indexOf(var);
      String replacement = variables.get(var);

      while (index != -1) {
        str = str.substring(0, index) + replacement + str.substring(index + var.length());
        index = str.indexOf(var);
      }
    }

    return str;
  }

  private String toTitleCase(String str) {
    if (str.length() < 2) {
      return str.toUpperCase();
    } else {
      return str.substring(0, 1).toUpperCase() + str.substring(1).replaceAll("_", " ");
    }
  }
}
