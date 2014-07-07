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
import com.google.dart.tools.debug.core.webkit.WebkitCallback;
import com.google.dart.tools.debug.core.webkit.WebkitNode;
import com.google.dart.tools.debug.core.webkit.WebkitResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.IOException;

/**
 * A class to listen for resource changes to html files and sync their contents over a WIP
 * connection.
 */
public class HtmlScriptManager implements ResourceChangeParticipant {

  private class UpdateHtmlFileJob extends Job {

    IFile file;

    public UpdateHtmlFileJob(IFile file) {
      super("Updating file");
      this.file = file;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      String fileUrl = target.getResourceResolver().getUrlForResource(file);

      if (fileUrl != null && fileUrl.equals(rootNode.getDocumentURL())) {
        uploadNewSource(rootNode, file);
      }

      return Status.OK_STATUS;
    }

  }

  private DartiumDebugTarget target;

  private WebkitNode rootNode;

  public HtmlScriptManager(DartiumDebugTarget target) {
    this.target = target;

    ResourceChangeManager.getManager().addChangeParticipant(this);
  }

  public void dispose() {
    ResourceChangeManager.removeChangeParticipant(this);
  }

  public void handleDocumentUpdated() {
    resync();
  }

  @Override
  public void handleFileAdded(IFile file) {

  }

  @Override
  public final void handleFileChanged(IFile file) {
    if ("html".equals(file.getFileExtension())) {
      UpdateHtmlFileJob job = new UpdateHtmlFileJob(file);
      job.schedule();
    }
  }

  @Override
  public void handleFileRemoved(IFile file) {

  }

  public void handleLoadEventFired() {
    resync();
  }

  private void resync() {
    // Flush everything.
    rootNode = null;

    // Get the root node.
    try {
      // TODO(devoncarew): check if the connection is no longer open?

      // {"id":13,"result":{"root":{"childNodeCount":3,"localName":"","nodeId":1,"documentURL":"http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/samples/solar/solar.html","baseURL":"http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/samples/solar/solar.html","nodeValue":"","nodeName":"#document","xmlVersion":"","children":[{"localName":"","nodeId":2,"internalSubset":"","publicId":"","nodeValue":"","nodeName":"html","systemId":"","nodeType":10},{"localName":"","nodeId":3,"nodeValue":" Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file\n     for details. All rights reserved. Use of this source code is governed by a\n     BSD-style license that can be found in the LICENSE file. ","nodeName":"","nodeType":8},{"childNodeCount":2,"localName":"html","nodeId":4,"nodeValue":"","nodeName":"HTML","children":[{"childNodeCount":3,"localName":"head","nodeId":5,"nodeValue":"","nodeName":"HEAD","attributes":[],"nodeType":1},{"childNodeCount":6,"localName":"body","nodeId":6,"nodeValue":"","nodeName":"BODY","attributes":[],"nodeType":1}],"attributes":[],"nodeType":1}],"nodeType":9}}}
      target.getConnection().getDom().getDocument(new WebkitCallback<WebkitNode>() {
        @Override
        public void handleResult(WebkitResult<WebkitNode> result) {
          rootNode = result.getResult();
        }
      });
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    }
  }

  private void uploadNewSource(WebkitNode node, IFile file) {
    try {
      target.getConnection().getDom().setOuterHTML(
          node.getNodeId(),
          IFileUtilities.getContents(file));
    } catch (IOException e) {
      // We upload changed html on a best-effort basis.
      // DartDebugCorePlugin.logError(e);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);
    }
  }

}
