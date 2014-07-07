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
import com.google.dart.tools.debug.core.webkit.WebkitStyleSheetRef;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.IOException;
import java.util.List;

/**
 * Manage known css scripts loaded in the target browser. Listen for resource change events (using
 * the ResourceChangeManager class). When a css scripts changes on disk that the browser knows
 * about, send the new contents to the browser using the Webkit inspector protocol.
 */
class CssScriptManager implements ResourceChangeParticipant {

  private class UpdateCssFileJob extends Job {

    IFile file;

    public UpdateCssFileJob(IFile file) {
      super("Updating file");
      this.file = file;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      String fileUrl = target.getResourceResolver().getUrlForResource(file);

      if (fileUrl != null) {
        List<WebkitStyleSheetRef> scripts = target.getConnection().getCSS().getStyleSheets();

        for (WebkitStyleSheetRef ref : scripts) {
          if (fileUrl.equals(ref.getSourceURL())) {
            uploadNewSource(ref, file);
          }
        }
      }

      return Status.OK_STATUS;
    }

  }

  private DartiumDebugTarget target;

  public CssScriptManager(DartiumDebugTarget target) {
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
    if ("css".equals(file.getFileExtension())) {
      UpdateCssFileJob job = new UpdateCssFileJob(file);
      job.schedule();
    }
  }

  @Override
  public void handleFileRemoved(IFile file) {

  }

  private void uploadNewSource(WebkitStyleSheetRef ref, IFile file) {
    try {
      target.getConnection().getCSS().setStyleSheetText(
          ref.getStyleSheetId(),
          IFileUtilities.getContents(file));
    } catch (IOException e) {
      // We upload changed css scripts on a best-effort basis.
      //DartDebugCorePlugin.logError(e);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);
    }
  }

}
