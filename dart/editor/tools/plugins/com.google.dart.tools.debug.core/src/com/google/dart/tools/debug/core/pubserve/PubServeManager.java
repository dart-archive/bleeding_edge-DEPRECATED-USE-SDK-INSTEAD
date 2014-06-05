/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.debug.core.pubserve;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.pub.IPubServeListener;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;

import java.io.IOException;

/**
 * Manages the pub serve process for launches. Clients should call serve(DartLaunchconfigWrapper,
 * PubCallback<String>) to serve an application and get a url for launch.
 */
public class PubServeManager implements IResourceChangeListener {

  private static PubServeManager manager = new PubServeManager();

  public static PubServeManager getManager() {
    return manager;
  }

  private MessageConsole console;

  private PubServe pubserve;

  private final ListenerList listeners = new ListenerList();

  public PubServeManager() {

  }

  public void addListener(IPubServeListener listener) {
    listeners.add(listener);
  }

  public void dispose() {
    if (pubserve != null) {
      pubserve.dispose();
    }
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  public String getStdErrorString() {
    return pubserve == null ? "" : pubserve.getStdErrorString();
  }

  /**
   * Indicates whether pub serve is running
   */
  public boolean isServing() {
    return pubserve != null && pubserve.isAlive();
  }

  public void notifyListeners(boolean isServing) {
    for (Object listener : listeners.getListeners()) {
      ((IPubServeListener) listener).pubServeStatus(isServing);
    }
  }

  public void removeListener(IPubServeListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
      IResource resource = event.getResource();
      if (resource != null && resource instanceof IProject) {
        IContainer pubServeWorkingDir = getCurrentServeWorkingDir();
        if (pubServeWorkingDir != null) {
          // if project that is being served is going to be deleted, stop pub serve
          if (pubServeWorkingDir.getProject() == resource) {
            terminatePubServe();
          }
        }
      }
    } else if (event.getDelta() != null && event.getDelta().getKind() == IResourceDelta.CHANGED) {
      // TODO(keertip): remove when pub can detect changes to pubspec and restart pub serve
      try {
        event.getDelta().accept(new IResourceDeltaVisitor() {
          @Override
          public boolean visit(IResourceDelta delta) throws CoreException {
            IResource res = delta.getResource();
            if (res == null) {
              return false;
            }
            if (res instanceof IFile) {
              if (res.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
                IContainer pubServeWorkingDir = getCurrentServeWorkingDir();
                if (pubServeWorkingDir == res.getParent()) {
                  DartCore.getConsole().printSeparator(
                      "Stopping pub serve, " + res.getFullPath().toString() + " has changed");
                  terminatePubServe();
                }
              }
              return false;
            }
            return true;
          }
        });
      } catch (CoreException e) {

      }
    }
  }

  /**
   * Sends a get asset for given url command to current pub serve
   * 
   * @param url
   * @param callback
   * @throws IOException
   */
  public void sendGetAssetIdCommand(String url, PubCallback<PubAsset> callback) throws IOException {
    pubserve.sendGetAssetIdCommand(url, callback);
  }

  /**
   * Sends a getUrl command to the current pub serve
   * 
   * @param resource
   * @param callback
   * @throws IOException
   */
  public void sendGetUrlCommand(IResource resource, PubCallback<String> callback)
      throws IOException {
    String path = getPathFromWorkingDir(resource);
    if (path != null) {
      if (pubserve.isAlive()) {
        pubserve.sendGetUrlCommand(path, callback);
      }
    } else {
      DartCore.logInformation("Path from working directory not found for resource "
          + resource.getName());
    }
  }

  /**
   * Starts pub serve for a given launch configuration. Checks if the current pub serve is for the
   * same pubspec.yaml, if not then starts up pub serve.
   * 
   * @param wrapper - the launch config wrapper
   * @param pubConnectionCallback
   */
  public void serve(DartLaunchConfigWrapper wrapper, PubCallback<String> pubConnectionCallback)
      throws Exception {

    console = DartCore.getConsole();
    IResource resource = wrapper.getApplicationResource();
    console.printSeparator("Starting pub serve : " + resource.getProject().getName());

    IContainer appDir = DartCore.getApplicationDirectory(resource);

    if (pubserve != null && pubserve.isAlive() && pubserve.getWorkingDir().equals(appDir)) {

      // make sure pub is serving the directory, send serve directory command
      serveDirectory(wrapper.getApplicationResource());

    } else {
      if (pubserve != null) {
        // terminate existing pub serve if any
        pubserve.dispose();
      }
      pubserve = new PubServe(appDir, getPubServeRootDir(appDir, resource));
      ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    sendGetUrlCommand(resource, pubConnectionCallback);
    notifyListeners(true);
  }

  /**
   * Stop pub serve and notify listeners of change of state.
   */
  public void terminatePubServe() {
    dispose();
    notifyListeners(false);
  }

  /**
   * Returns the working directory / application directory for the current pub serve
   */
  IContainer getCurrentServeWorkingDir() {
    if (pubserve != null) {
      return pubserve.getWorkingDir();
    }
    return null;
  }

  /**
   * Returns the name of the directory containing the given resource that can be used as root by pub
   * serve. Pub serve uses the directories that are siblings to the pubspec as root.
   * 
   * @param container - directory which contains the pubspec.yaml
   * @param resource - the resource to launch
   * @return
   */
  String getPubServeRootDir(IContainer container, IResource resource) {

    try {
      IResource[] folders = container.members();
      for (IResource folder : folders) {
        if (folder instanceof IFolder
            && !(folder.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME) || folder.getName().equals(
                DartCore.BUILD_DIRECTORY_NAME))) {
          if (resource.getFullPath().toString().startsWith(folder.getFullPath().toString())) {
            return folder.getName();
          }
        }
      }
    } catch (CoreException e) {
      DartCore.logInformation("", e);
    }

    return null;
  }

  /**
   * Returns the path to the resource from the directory where the pubspec resides. - myproj -
   * pubspec.yaml - web - index.html => web/index.html
   */
  private String getPathFromWorkingDir(IResource resource) {
    IContainer workingDir = DartCore.getApplicationDirectory(resource);
    if (workingDir != null) {
      return resource.getFullPath().removeFirstSegments(workingDir.getFullPath().segmentCount()).toString();
    }
    return null;
  }

  /**
   * Send a serve directory command to the current pub serve
   * 
   * @param resource
   * @throws Exception
   */
  private void serveDirectory(IResource resource) throws Exception {

    IContainer workingDir = DartCore.getApplicationDirectory(resource);
    try {
      pubserve.serveDirectory(getPubServeRootDir(workingDir, resource));
    } catch (IOException ioe) {
      DartCore.logError(ioe);
    }

  }
}
