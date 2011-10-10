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
package com.google.dart.indexer.workspace.index;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexingQueue {
  private class ProjectState {
    private final IProject project;

    int queuedFiles;

    int prioritizationRequestCount;

    final LinkedList<IndexingTarget> queue = new LinkedList<IndexingTarget>();

    public ProjectState(IProject project) {
      if (project == null) {
        throw new NullPointerException("project is null");
      }
      this.project = project;
    }

    public IndexingTarget dequeue() {
      IndexingTarget result = queue.removeFirst();
      --queuedFiles;
      --queuedFilesInAllProjects;
      removeIfEmpty();
      return result;
    }

    public void enqueue(IndexingTarget target) {
      queue.addLast(target);
      incrementCounters();
    }

    public IProject getProject() {
      return project;
    }

    public boolean isEmpty() {
      return queuedFiles == 0 && prioritizationRequestCount == 0;
    }

    public void reenqueue(IndexingTarget target) {
      queue.addFirst(target);
      ++queuedFiles;
      ++queuedFilesInAllProjects;
      incrementCounters();
    }

    @Override
    public String toString() {
      return queuedFiles + " in " + project;
    }

    void incrementCounters() {
      ++queuedFiles;
      ++queuedFilesInAllProjects;
      if (prioritizationRequestCount > 0 && queuedFiles == 1) {
        // The project is a prioritized project (i.e. a request is waiting for
        // the project to be indexed).
        // However, this project has already been indexed and has been removed
        // from the queue.
        // We don't want to add the project to the tail of the list,
        // because this will mess up the natural priority grouping.
        // Instead, we'll reset the queue to the full list of prioritized
        // projects,
        // and the indexed ones will be removed the next time dequeue() is
        // called.
        priorityOrderedProjectsWithRemainingWork.clear();
        priorityOrderedProjectsWithRemainingWork.addAll(priorityOrderedProjects);
      }
    }

    private void removeIfEmpty() {
      if (isEmpty()) {
        projectsToStates.remove(project);
      }
    }
  }

  private final Map<IProject, ProjectState> projectsToStates = new HashMap<IProject, ProjectState>();

  private final Set<IProject> priorityProjects = new HashSet<IProject>();

  private final List<IProject> priorityOrderedProjects = new ArrayList<IProject>();

  // always a subset of priorityOrderedProjects
  // projects are removed from the head when no remaining work is left
  private final List<IProject> priorityOrderedProjectsWithRemainingWork = new ArrayList<IProject>();

  private ProjectState lastProjectStateWithRemainingWork = null;

  private int queuedFilesInAllProjects = 0;

  private QueueState state = QueueState.NORMAL;

  public synchronized void abnormalState(QueueState state) {
    if (!state.isAbnormal()) {
      throw new IllegalArgumentException("Guess what? abnormalState needs an *abnormal* state.");
    }
    this.state = state;
    doClearQueue();
  }

  public void addedFile(IFile file) {
    enqueue(new ResourceIndexingTarget(file));
  }

  public void changedFile(IFile file) {
    enqueue(new ResourceIndexingTarget(file));
  }

  public void deletedFile(IFile file) {
    enqueue(new ResourceIndexingTarget(file));
  }

  public synchronized void enqueue(IFile[] changedFiles) {
    state = QueueState.NORMAL;
    for (int i = 0; i < changedFiles.length; i++) {
      doEnqueueTarget(new ResourceIndexingTarget(changedFiles[i]));
    }
  }

  public synchronized void enqueue(IndexingTarget[] targets) {
    state = QueueState.NORMAL;
    for (int i = 0; i < targets.length; i++) {
      doEnqueueTarget(targets[i]);
    }
  }

  /**
   * Utility method used to update monitor state by an IndexingJob
   * 
   * @return current size of a queue
   */
  public synchronized int getQueueSize() {
    return size();
  }

  public synchronized boolean hasQueuedFilesIn(IProject project) {
    ProjectState projectState = projectsToStates.get(project);
    if (projectState == null || projectState.queuedFiles == 0) {
      return false;
    }
    return true;
  }

  public synchronized boolean hasQueuedFilesIn(Set<IProject> projects) {
    for (Iterator<IProject> iterator = projects.iterator(); iterator.hasNext();) {
      IProject project = iterator.next();
      if (hasQueuedFilesIn(project)) {
        return true;
      }
    }
    return false;
  }

  public synchronized boolean isPendingResyncOrRebuild() {
    return state.isAbnormal();
  }

  public synchronized QueueState pollState() {
    QueueState state = this.state;
    this.state = QueueState.NORMAL;
    return state;
  }

  public synchronized void prioritizeProject(IProject project) {
    if (priorityProjects.add(project)) {
      priorityOrderedProjects.add(project);
      priorityOrderedProjectsWithRemainingWork.add(project);
      ProjectState state = findOrCreateState(project);
      ++state.prioritizationRequestCount;
    }
  }

  public synchronized void replaceWith(IFile[] filesToIndex) {
    state = QueueState.NORMAL;
    doClearQueue();
    for (int i = 0; i < filesToIndex.length; i++) {
      doEnqueueTarget(new ResourceIndexingTarget(filesToIndex[i]));
    }
  }

  public synchronized int size() {
    return queuedFilesInAllProjects;
  }

  @Override
  public synchronized String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(getClass().getSimpleName());
    return buf.toString();
  }

  public synchronized void unprioritizeProject(IProject project) {
    ProjectState state = projectsToStates.get(project);
    if (state == null || 0 == state.prioritizationRequestCount) {
      return;
    }
    if (--state.prioritizationRequestCount == 0) {
      priorityProjects.remove(project);
      priorityOrderedProjects.remove(project);
      priorityOrderedProjectsWithRemainingWork.remove(project);
    }
  }

  synchronized IndexingTarget dequeue() {
    while (!priorityOrderedProjectsWithRemainingWork.isEmpty()) {
      IProject project = priorityOrderedProjectsWithRemainingWork.get(0);
      ProjectState state = findOrCreateState(project);
      if (state.queuedFiles == 0) {
        priorityOrderedProjectsWithRemainingWork.remove(0);
        continue;
      }
      return state.dequeue();
    }
    if (lastProjectStateWithRemainingWork != null
        && lastProjectStateWithRemainingWork.queuedFiles > 0) {
      return lastProjectStateWithRemainingWork.dequeue();
    }
    for (Iterator<ProjectState> iterator = projectsToStates.values().iterator(); iterator.hasNext();) {
      ProjectState state = iterator.next();
      if (state.queuedFiles > 0) {
        lastProjectStateWithRemainingWork = state;
        return state.dequeue();
      }
    }
    return null;
  }

  synchronized void reenqueue(IndexingTarget target) {
    state = QueueState.NORMAL; // why?..
    IProject project = target.getProject();
    ProjectState projectState = findOrCreateState(project);
    projectState.reenqueue(target);
  }

  private void doClearQueue() {
    projectsToStates.clear();
  }

  private void doEnqueueTarget(IndexingTarget target) {
    findOrCreateState(target.getProject()).enqueue(target);
  }

  private synchronized void enqueue(IndexingTarget target) {
    state = QueueState.NORMAL;
    doEnqueueTarget(target);
  }

  private ProjectState findOrCreateState(IProject project) {
    ProjectState projectState = projectsToStates.get(project);
    if (projectState == null) {
      projectState = new ProjectState(project);
      projectsToStates.put(project, projectState);
    }
    return projectState;
  }
}
