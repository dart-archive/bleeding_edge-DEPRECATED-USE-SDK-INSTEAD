/*
 * Copyright (c) 2011, the Dart project authors.
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

package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.frog.FrogManager;
import com.google.dart.tools.core.frog.ResponseDone;
import com.google.dart.tools.core.frog.ResponseHandler;
import com.google.dart.tools.core.frog.ResponseMessage;
import com.google.dart.tools.core.frog.ResponseObject;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Called from DartBuilder - this is a Frog specific builder handler to perform a build.
 */
public class FrogBuilderHandler {

  class CompileResponseHandler extends ResponseHandler {
    private IProject project;
    private CountDownLatch latch;
    private IStatus exitStatus = Status.OK_STATUS;
    private List<ResponseMessage> messages = new ArrayList<ResponseMessage>();

    public CompileResponseHandler(IProject project, CountDownLatch latch) {
      this.project = project;
      this.latch = latch;
    }

    @Override
    public void response(ResponseObject response) throws IOException, JSONException {
      try {
        // process response
        if (response.isMessageResponse()) {
          ResponseMessage message = response.createMessageResponse();

          messages.add(message);
        } else if (response.isDoneResponse()) {
          ResponseDone done = response.createDoneResponse();

          if (!done.isSuccess()) {
            exitStatus = new Status(IStatus.ERROR, DartCore.PLUGIN_ID, 0,
                "Unable to generate Javascript.", null);
          }

          latch.countDown();
        }
      } catch (JSONException exception) {
        BuilderUtil.createErrorMarker(project, 0, 0, 1,
            "Internal compiler error: " + exception.toString());

        DartCore.logError("Exception caught from frog protocol:", exception);

        latch.countDown();

        throw exception;
      }
    }

    protected IStatus getExitStatus() {
      return exitStatus;
    }

    protected List<ResponseMessage> getMessages() {
      return messages;
    }
  }

  public FrogBuilderHandler() {

  }

  public IProject[] build(IProgressMonitor monitor, IProject project) throws CoreException {
    DartProject proj = DartCore.create(project);

    // TODO(devoncarew): This is temporary - we should associate errors with the
    // library we're compiling, and clear them out before the next compile.
    BuilderUtil.clearErrorMarkers(project);

    DartLibrary[] allLibraries = proj.getDartLibraries();

    monitor.beginTask("Building " + proj.getElementName() + "...", allLibraries.length);

    try {
      for (DartLibrary library : allLibraries) {
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        try {
          buildLibrary(project, library);

          monitor.worked(1);
        } catch (Throwable exception) {
          BuilderUtil.createErrorMarker(library.getCorrespondingResource(), 0, 0, 1,
              "Internal compiler error: " + exception.toString());

          DartCore.logError("Exception caught while building " + library.getElementName(),
              exception);
        }
      }
    } finally {
      monitor.done();
    }

    return new IProject[0];
  }

  public void clean(IProject project, IProgressMonitor monitor) {
    // TODO (danrubel): implement
  }

  private void buildLibrary(IProject project, DartLibrary library) throws CoreException {
    CountDownLatch latch = new CountDownLatch(1);
    CompileResponseHandler responseHandler = new CompileResponseHandler(project, latch);

    IPath libraryPath = library.getCorrespondingResource().getLocation();
    IPath outputPath = Path.fromPortableString(libraryPath.toPortableString() + ".app.js");

    // Don't try and generate Javascript from non-application libraries.
    if (!((DartLibraryImpl) library).hasMain()) {
      // TODO(devoncarew): Need to ask the model which libraries depend upon this library and trigger
      // those builders.

      outputPath = null;
    }

    long startTime = System.currentTimeMillis();

    try {
      FrogManager.getServer().compile(libraryPath, outputPath, responseHandler);
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.toString(), e));
    }

    try {
      latch.await();
    } catch (InterruptedException e) {

    }

    for (ResponseMessage message : responseHandler.getMessages()) {
      ResponseMessage.Location location = message.getLocation();

      createMarker((IFile) library.getCorrespondingResource(), location == null ? null
          : location.path, message.getSeverity(), message.getMessage(), location);
    }

    if (DartCoreDebug.FROG && outputPath != null) {
      long elapsed = System.currentTimeMillis() - startTime;

      // Trim to 1/10th of a second.
      elapsed = (elapsed / 100) * 100;

      File outputFile = outputPath.toFile();
      // Trim to 1/10th of a kb.
      double fileLength = ((int) ((outputFile.length() / 1024) * 10)) / 10;

      String message = fileLength + "kb";
      message += " written in " + (elapsed / 1000.0) + "sec";

      DartCore.logInformation("Wrote " + outputFile.getPath() + " [" + message + "]");
    }
  }

  private void createMarker(IFile libraryFile, String path, int severity, String message,
      ResponseMessage.Location location) {
    IFile file = null;

    if (location == null) {
      file = libraryFile;
    } else {
      IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(path));

      if (files.length > 0) {
        file = files[0];
      }
    }

    if (file != null) {
      BuilderUtil.createMarker(file, severity, location.start, location.end - location.start,
          location.line, message);
    }
  }

}
