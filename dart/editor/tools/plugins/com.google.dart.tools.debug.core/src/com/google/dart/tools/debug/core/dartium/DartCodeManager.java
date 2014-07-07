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

import com.google.dart.tools.core.utilities.resource.IFileUtilities;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.util.ResourceChangeManager;
import com.google.dart.tools.debug.core.util.ResourceChangeParticipant;
import com.google.dart.tools.debug.core.webkit.WebkitScript;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.IOException;
import java.util.Collection;

/**
 * Manage known Dart files loaded in the target browser. Listen for resource change events (using
 * the ResourceChangeManager class). When a Dart file changes on disk that the browser knows about,
 * send the new contents to the browser using the Webkit inspector protocol.
 */
public class DartCodeManager implements ResourceChangeParticipant {

  private class UpdateDartFileJob extends Job {

    IFile file;

    public UpdateDartFileJob(IFile file) {
      super("Updating file");
      this.file = file;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      String fileUrl = target.getResourceResolver().getUrlForResource(file);

      if (fileUrl != null) {
        Collection<WebkitScript> scripts = target.getConnection().getDebugger().getAllScripts();

        for (WebkitScript script : scripts) {
          if (fileUrl.equals(script.getUrl())) {
            uploadNewSource(script.getScriptId(), file);
          }
        }
      }
      return Status.OK_STATUS;
    }

  }

  private DartiumDebugTarget target;

  public DartCodeManager(DartiumDebugTarget target) {
    this.target = target;

    ResourceChangeManager.getManager().addChangeParticipant(this);
  }

  public void dispose() {
    ResourceChangeManager.removeChangeParticipant(this);
  }

  @Override
  public void handleFileAdded(IFile file) {
    handleFileChanged(file);
  }

  @Override
  public void handleFileChanged(IFile file) {
    if (!target.supportsSetScriptSource()) {
      return;
    }

    if ("dart".equals(file.getFileExtension())) {
      UpdateDartFileJob job = new UpdateDartFileJob(file);
      job.schedule();
    }
  }

  @Override
  public void handleFileRemoved(IFile file) {

  }

  private void uploadNewSource(String scriptId, IFile file) {
    try {
      target.getConnection().getDebugger().setScriptSource(
          scriptId,
          IFileUtilities.getContents(file));
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);
    }
  }

}
