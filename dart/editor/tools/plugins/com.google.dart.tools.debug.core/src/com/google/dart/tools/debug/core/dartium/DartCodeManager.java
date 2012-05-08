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
import com.google.dart.tools.debug.core.util.IResourceResolver;
import com.google.dart.tools.debug.core.util.ResourceChangeManager;
import com.google.dart.tools.debug.core.util.ResourceChangeParticipant;
import com.google.dart.tools.debug.core.webkit.WebkitScript;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;

/**
 * Manage known Dart files loaded in the target browser. Listen for resource change events (using
 * the ResourceChangeManager class). When a Dart file changes on disk that the browser knows about,
 * send the new contents to the browser using the Webkit inspector protocol.
 */
public class DartCodeManager implements ResourceChangeParticipant {
  private DartiumDebugTarget target;
  private IResourceResolver resourceResolver;

  public DartCodeManager(DartiumDebugTarget target, IResourceResolver resourceResolver) {
    this.target = target;
    this.resourceResolver = resourceResolver;

    ResourceChangeManager.getManager().addChangeParticipant(this);
  }

  public void dispose() {
    ResourceChangeManager.removeChangeParticipant(this);
  }

  @Override
  public void handleFileChange(IFile file) {
    if (!target.supportsSetScriptSource()) {
      return;
    }

    if ("dart".equals(file.getFileExtension())) {
      String fileUrl = resourceResolver.getUrlForResource(file);

      for (WebkitScript script : target.getConnection().getDebugger().getAllScripts()) {
        if (fileUrl.equals(script.getUrl())) {
          uploadNewSource(script.getScriptId(), file);
        }
      }
    }
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
