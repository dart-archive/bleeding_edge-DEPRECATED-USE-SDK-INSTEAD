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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexingQueue {
  private class ProjectState {
    private final IProject project;

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
      removeIfEmpty();
      return result;
    }

    public void enqueue(IndexingTarget target) {
      URI targetUri = target.getUri();
      for (IndexingTarget queuedTarget : queue) {
        URI queuedUri = queuedTarget.getUri();
        if (queuedUri != null && queuedUri.equals(targetUri)) {
          return;
        }
      }
      queue.addLast(target);
      incrementCounters();
    }

    public boolean isEmpty() {
      return queue.size() == 0 && prioritizationRequestCount == 0;
    }

    public void reenqueue(IndexingTarget target) {
      queue.addFirst(target);
      incrementCounters();
    }

    @Override
    public String toString() {
      return queue.size() + " in " + project;
    }

    private void incrementCounters() {
      if (prioritizationRequestCount > 0 && queue.size() == 1) {
        // The project is a prioritized project (i.e. a request is waiting for the project to be
        // indexed). However, this project has already been indexed and has been removed from the
        // queue. We don't want to add the project to the tail of the list, because this will mess
        // up the natural priority grouping. Instead, we'll reset the queue to the full list of
        // prioritized projects, and the indexed ones will be removed the next time dequeue() is
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

  private QueueState state = QueueState.NORMAL;

  public synchronized void abnormalState(QueueState state) {
    if (!state.isAbnormal()) {
      throw new IllegalArgumentException("Guess what? abnormalState needs an *abnormal* state.");
    }
    this.state = state;
    doClearQueue();
  }

  public synchronized void addedFile(IFile file) {
    enqueue(new ResourceIndexingTarget(file));
  }

  public synchronized void changedFile(IFile file) {
    enqueue(new ResourceIndexingTarget(file));
  }

  public synchronized void deletedFile(IFile file) {
    enqueue(new ResourceIndexingTarget(file));
  }

  public synchronized IndexingTarget dequeue() {
    while (!priorityOrderedProjectsWithRemainingWork.isEmpty()) {
      IProject project = priorityOrderedProjectsWithRemainingWork.get(0);
      ProjectState state = findOrCreateState(project);
      if (state.queue.size() == 0) {
        priorityOrderedProjectsWithRemainingWork.remove(0);
        continue;
      }
      return state.dequeue();
    }
    if (lastProjectStateWithRemainingWork != null
        && lastProjectStateWithRemainingWork.queue.size() > 0) {
      return lastProjectStateWithRemainingWork.dequeue();
    }
    for (ProjectState state : projectsToStates.values()) {
      if (state.queue.size() > 0) {
        lastProjectStateWithRemainingWork = state;
        return state.dequeue();
      }
    }
    return null;
  }

  public synchronized void enqueue(IFile[] changedFiles) {
    state = QueueState.NORMAL;
    for (IFile file : changedFiles) {
      doEnqueueTarget(new ResourceIndexingTarget(file));
    }
  }

  public synchronized void enqueue(IndexingTarget[] targets) {
    state = QueueState.NORMAL;
    for (IndexingTarget target : targets) {
      doEnqueueTarget(target);
    }
  }

  /**
   * Return the number of targets waiting to be indexed.
   * 
   * @return current size of the queue
   */
  public synchronized int getQueueSize() {
    return size();
  }

  /**
   * Return <code>true</code> if there are any targets waiting to be indexed that are contained in
   * the given project.
   * 
   * @param project the project being tested
   * @return <code>true</code> if there are any targets waiting to be indexed that are contained in
   *         the given project
   */
  public synchronized boolean hasQueuedFilesIn(IProject project) {
    ProjectState projectState = projectsToStates.get(project);
    if (projectState == null || projectState.queue.size() == 0) {
      return false;
    }
    return true;
  }

  /**
   * Return <code>true</code> if there are any targets waiting to be indexed that are contained in
   * any of the given projects.
   * 
   * @param projects the projects being tested
   * @return <code>true</code> if there are any targets waiting to be indexed that are contained in
   *         any of the given projects
   */
  public synchronized boolean hasQueuedFilesIn(Set<IProject> projects) {
    for (IProject project : projects) {
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

  public synchronized void reenqueue(IndexingTarget target) {
    state = QueueState.NORMAL; // why?..
    IProject project = target.getProject();
    ProjectState projectState = findOrCreateState(project);
    projectState.reenqueue(target);
  }

  public synchronized void replaceWith(IFile[] filesToIndex) {
    state = QueueState.NORMAL;
    doClearQueue();
    for (IFile file : filesToIndex) {
      doEnqueueTarget(new ResourceIndexingTarget(file));
    }
  }

  public synchronized int size() {
    int size = 0;
    for (ProjectState projectState : projectsToStates.values()) {
      size += projectState.queue.size();
    }
    return size;
  }

  @Override
  public synchronized String toString() {
    return getClass().getSimpleName();
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

  private void doClearQueue() {
    projectsToStates.clear();
  }

  private void doEnqueueTarget(IndexingTarget target) {
    findOrCreateState(target.getProject()).enqueue(target);
  }

  private void enqueue(IndexingTarget target) {
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
