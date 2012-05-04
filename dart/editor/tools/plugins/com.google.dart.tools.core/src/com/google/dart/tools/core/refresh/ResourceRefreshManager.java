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
package com.google.dart.tools.core.refresh;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Instances of the class <code>ResourceRefreshManager</code> generate {@link ResourceChangeEvent
 * resource change events} when linked files have been modified on disk. These events are only
 * generated when explicitly requested; it is some other object's responsibility to request
 * notification at the appropriate times.
 */
public class ResourceRefreshManager {
  /**
   * The interface <code>FileVisitor</code> defines the behavior of objects that can be used to
   * visit files.
   */
  private interface FileVisitor {
    /**
     * Visit the given file.
     * 
     * @param file the file to be visited
     */
    public void visitFile(IFile file);
  }

  private class WorkspaceChangeListener implements IResourceChangeListener {
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      updateWithoutNotification();
    }
  }

  /**
   * Return the file in which the modification times are stored between sessions.
   * 
   * @return the file in which the modification times are stored between sessions
   */
  private static File getTimeStore() {
    return DartCore.getPlugin().getStateLocation().append("resourceTime.data").toFile();
  }

  /**
   * A table mapping linked files to the modification stamp of the files to which they are linked.
   */
  private HashMap<IFile, Long> modificationTimes = new HashMap<IFile, Long>();

  /**
   * A list containing the objects listening for notification when resources have been modified.
   */
  private ArrayList<ResourceChangeListener> listeners = new ArrayList<ResourceChangeListener>();

  private WorkspaceChangeListener listener = new WorkspaceChangeListener();

  /**
   * Initialize a newly created resource refresh manager. The manager will be initialized either
   * from state stored on disk or from the resources currently loaded (if there is no state stored
   * on disk).
   */
  public ResourceRefreshManager() {
    if (!readModificationTimes()) {
      initializeModificationTimes();
    }
    ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
  }

  /**
   * Add the given listener to the list of listeners that will be notified when resources are
   * modified.
   * 
   * @param listener the listener to be added to the list
   */
  public void addResourceChangeListener(ResourceChangeListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  /**
   * Determine whether any of the linked files have been modified since the last known modification
   * time, and trigger a resource change event for any that have been.
   */
  public void refresh() {
    final ArrayList<IFile> addedFiles = new ArrayList<IFile>();
    final ArrayList<IFile> modifiedFiles = new ArrayList<IFile>();
    final HashSet<IFile> deletedFiles = new HashSet<IFile>();
    synchronized (modificationTimes) {
      deletedFiles.addAll(modificationTimes.keySet());
      visitFiles("refreshing", new FileVisitor() {
        @Override
        public void visitFile(IFile file) {
          Long recordedTime = modificationTimes.get(file);
          long actualTime = file.getLocation().toFile().lastModified();
          if (recordedTime == null) {
            addedFiles.add(file);
            modificationTimes.put(file, new Long(actualTime));
          } else if (recordedTime.longValue() < actualTime) {
            modifiedFiles.add(file);
            modificationTimes.put(file, new Long(actualTime));
          }
          deletedFiles.remove(file);
        }
      });
      for (IFile deletedFile : deletedFiles) {
        modificationTimes.remove(deletedFile);
      }
    }
    fireResourceChangeEvent(addedFiles, modifiedFiles, deletedFiles);
  }

  /**
   * Remove the given listener from the list of listeners that will be notified when resources are
   * modified.
   * 
   * @param listener the listener to be removed from the list
   */
  public void removeResourceChangeListener(ResourceChangeListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  /**
   * Save the state associated with this manager prior to shutdown.
   */
  public void shutdown() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
    writeModificationTimes();
  }

  /**
   * Notify any listeners that are registered that the given files were changed.
   * 
   * @param addedFiles the files that have been added
   * @param modifiedFiles the files whose content has changed
   * @param deletedFiles the files that have been deleted
   */
  private void fireResourceChangeEvent(ArrayList<IFile> addedFiles, ArrayList<IFile> modifiedFiles,
      HashSet<IFile> deletedFiles) {
    ResourceChangeEvent event = null;
    synchronized (listeners) {
      for (ResourceChangeListener listener : listeners) {
        if (event == null) {
          event = new ResourceChangeEvent(addedFiles, modifiedFiles, new ArrayList<IFile>(
              deletedFiles));
        }
        listener.resourcesChanged(event);
      }
    }
  }

  /**
   * Initialize the table mapping linked files to modification times.
   */
  private void initializeModificationTimes() {
    synchronized (modificationTimes) {
      modificationTimes.clear();
      visitFiles("initializing", new FileVisitor() {
        @Override
        public void visitFile(IFile file) {
          long modificationTime = file.getLocation().toFile().lastModified();
          modificationTimes.put(file, new Long(modificationTime));
        }
      });
    }
  }

  /**
   * Read the last-known modification times from this plug-in's storage location.
   * 
   * @return <code>true</code> if the times were read correctly
   */
  private boolean readModificationTimes() {
    File timeStore = getTimeStore();
    if (!timeStore.exists()) {
      return false;
    }
    boolean correctlyRead = false;
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    ObjectInputStream input = null;
    try {
      input = new ObjectInputStream(new FileInputStream(timeStore));
      int count = input.readInt();
      synchronized (modificationTimes) {
        for (int i = 0; i < count; i++) {
          String fullPath = input.readUTF();
          long modificationTime = input.readLong();
          IResource resource = root.findMember(new Path(fullPath));
          if (resource.exists() && resource.getType() == IResource.FILE) {
            modificationTimes.put((IFile) resource, Long.valueOf(modificationTime));
          }
        }
      }
      correctlyRead = true;
    } catch (IOException exception) {
      correctlyRead = false;
      DartCore.logError("Could not read " + timeStore.getAbsolutePath(), exception);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException exception) {
          correctlyRead = false;
          DartCore.logError(
              "Could not close " + timeStore.getAbsolutePath() + " after read",
              exception);
        }
      }
    }
    if (!correctlyRead) {
      timeStore.delete();
    }
    return correctlyRead;
  }

  private void updateWithoutNotification() {
    final HashSet<IFile> deletedFiles = new HashSet<IFile>();
    synchronized (modificationTimes) {
      deletedFiles.addAll(modificationTimes.keySet());
      visitFiles("updating", new FileVisitor() {
        @Override
        public void visitFile(IFile file) {
          long actualTime = file.getLocation().toFile().lastModified();
          modificationTimes.put(file, new Long(actualTime));
          deletedFiles.remove(file);
        }
      });
      for (IFile deletedFile : deletedFiles) {
        modificationTimes.remove(deletedFile);
      }
    }
  }

  /**
   * Visit each of the linked files in the workspace by passing the file to the given visitor. The
   * map of modification times will be locked for the duration of the visit operation.
   * 
   * @param activity a description of why the files are being visited, only used for debugging
   *          purposes
   * @param visitor the visitor used to visit each of the linked files
   */
  private void visitFiles(String activity, final FileVisitor visitor) {
    synchronized (modificationTimes) {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      for (IProject project : root.getProjects()) {
        if (project.isOpen()) {
          try {
            project.accept(new IResourceProxyVisitor() {
              @Override
              public boolean visit(IResourceProxy proxy) throws CoreException {
                if (proxy.getType() == IResource.FILE && proxy.isLinked()) {
                  IResource resource = proxy.requestResource();
                  if (resource != null) {
                    visitor.visitFile((IFile) resource);
                  }
                }
                return true;
              }
            }, 0);
          } catch (CoreException exception) {
            DartCore.logError("Could not visit resources in project " + project.getName()
                + " while " + activity + " the resource refresh manager", exception);
          }
        }
      }
    }
  }

  /**
   * Write the last-known modification times to this plug-in's storage location.
   * 
   * @return <code>true</code> if the file was correctly written
   */
  private boolean writeModificationTimes() {
    File timeStore = getTimeStore();
    boolean correctlyWritten = true;
    ObjectOutputStream output = null;
    try {
      output = new ObjectOutputStream(new FileOutputStream(timeStore));
      synchronized (modificationTimes) {
        output.writeInt(modificationTimes.size());
        for (Map.Entry<IFile, Long> entry : modificationTimes.entrySet()) {
          output.writeUTF(entry.getKey().getFullPath().toString());
          output.writeLong(entry.getValue().longValue());
        }
      }
    } catch (IOException exception) {
      correctlyWritten = false;
      DartCore.logError("Could not write " + timeStore.getAbsolutePath(), exception);
    } finally {
      if (output != null) {
        try {
          output.flush();
        } catch (IOException exception) {
          correctlyWritten = false;
          DartCore.logError(
              "Could not flush " + timeStore.getAbsolutePath() + " after write",
              exception);
        }
        try {
          output.close();
        } catch (IOException exception) {
          correctlyWritten = false;
          DartCore.logError(
              "Could not close " + timeStore.getAbsolutePath() + " after write",
              exception);
        }
      }
    }
    if (timeStore.exists() && !correctlyWritten) {
      timeStore.delete();
    }
    return correctlyWritten;
  }
}
