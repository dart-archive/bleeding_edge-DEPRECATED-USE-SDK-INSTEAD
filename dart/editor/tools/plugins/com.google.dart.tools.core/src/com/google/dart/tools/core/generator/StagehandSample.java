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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;

/**
 * Create a Dart project using the Stagehand CLI tool.
 */
public class StagehandSample extends AbstractSample {
  private Stagehand stagehand;

  public StagehandSample(Stagehand stagehand, String id, String description, String entrypoint) {
    super(id, description);

    this.stagehand = stagehand;

    setMainFile(entrypoint);
  }

  @Override
  public IFile generateInto(IContainer container, String sampleName) throws CoreException {
    File projectDirectory = container.getLocation().toFile().getAbsoluteFile();

    try {
      stagehand.generateInto(projectDirectory, getStagehandId());
    } catch (StagehandException e) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.getMessage(), e));
    }

    container.refreshLocal(IResource.DEPTH_INFINITE, null);

    if (mainFile != null) {
      IResource resource = container.findMember(mainFile);

      if (resource instanceof IFile) {
        return (IFile) resource;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  public String getStagehandId() {
    return getTitle();
  }

  @Override
  public boolean shouldBeDefault() {
    return getTitle().equals("consoleapp");
  }
}
