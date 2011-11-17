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
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class IndexingQueue {
  private class GroupState {
    private final IndexingTargetGroup group;

    int prioritizationRequestCount;

    final LinkedList<IndexingTarget> queue = new LinkedList<IndexingTarget>();

    public GroupState(IndexingTargetGroup group) {
      if (group == null) {
        throw new NullPointerException("group is null");
      }
      this.group = group;
    }

    public IndexingTarget dequeue() {
      IndexingTarget result = queue.removeFirst();
      removeIfEmpty();
      return result;
    }

    /**
     * Add the given target to this group's queue. If a matching target was already on the queue,
     * replace the queued target with the give target (the new target is assumed to be more
     * up-to-date).
     * 
     * @param target the target to be added to the queue
     */
    public void enqueue(IndexingTarget target) {
      URI targetUri = target.getUri();
      ListIterator<IndexingTarget> iterator = queue.listIterator();
      while (iterator.hasNext()) {
        IndexingTarget queuedTarget = iterator.next();
        URI queuedUri = queuedTarget.getUri();
        if (queuedUri != null && queuedUri.equals(targetUri)) {
          iterator.set(target);
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
      return queue.size() + " in " + group;
    }

    private void incrementCounters() {
      if (prioritizationRequestCount > 0 && queue.size() == 1) {
        // The project is a prioritized project (i.e. a request is waiting for the project to be
        // indexed). However, this project has already been indexed and has been removed from the
        // queue. We don't want to add the project to the tail of the list, because this will mess
        // up the natural priority grouping. Instead, we'll reset the queue to the full list of
        // prioritized projects, and the indexed ones will be removed the next time dequeue() is
        // called.
        priorityOrderedGroupsWithRemainingWork.clear();
        priorityOrderedGroupsWithRemainingWork.addAll(priorityOrderedGroups);
      }
    }

    private void removeIfEmpty() {
      if (isEmpty()) {
        groupsToStates.remove(group);
      }
    }
  }

  private final Map<IndexingTargetGroup, GroupState> groupsToStates = new HashMap<IndexingTargetGroup, GroupState>();

  private final Set<IndexingTargetGroup> priorityGroups = new HashSet<IndexingTargetGroup>();

  private final List<IndexingTargetGroup> priorityOrderedGroups = new ArrayList<IndexingTargetGroup>();

  // always a subset of priorityOrderedGroups
  // groups are removed from the head when no remaining work is left
  private final List<IndexingTargetGroup> priorityOrderedGroupsWithRemainingWork = new ArrayList<IndexingTargetGroup>();

  private GroupState lastProjectStateWithRemainingWork = null;

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
    while (!priorityOrderedGroupsWithRemainingWork.isEmpty()) {
      IndexingTargetGroup group = priorityOrderedGroupsWithRemainingWork.get(0);
      GroupState state = findOrCreateState(group);
      if (state.queue.size() == 0) {
        priorityOrderedGroupsWithRemainingWork.remove(0);
        continue;
      }
      return state.dequeue();
    }
    if (lastProjectStateWithRemainingWork != null
        && lastProjectStateWithRemainingWork.queue.size() > 0) {
      return lastProjectStateWithRemainingWork.dequeue();
    }
    for (GroupState state : groupsToStates.values()) {
      if (state.queue.size() > 0) {
        lastProjectStateWithRemainingWork = state;
        return state.dequeue();
      }
    }
    return null;
  }

  @Deprecated
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
  @Deprecated
  public synchronized boolean hasQueuedFilesIn(IProject project) {
    GroupState state = groupsToStates.get(project);
    if (state == null || state.queue.size() == 0) {
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
  @Deprecated
  public synchronized boolean hasQueuedFilesIn(Set<IProject> projects) {
    for (IProject project : projects) {
      if (hasQueuedFilesIn(project)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return <code>true</code> if there are any targets waiting to be indexed that are contained in
   * the given group.
   * 
   * @param group the group being tested
   * @return <code>true</code> if there are any targets waiting to be indexed that are contained in
   *         the given group
   */
  public synchronized boolean hasQueuedTargetsIn(IndexingTargetGroup group) {
    GroupState state = groupsToStates.get(group);
    if (state == null || state.queue.size() == 0) {
      return false;
    }
    return true;
  }

  /**
   * Return <code>true</code> if there are any targets waiting to be indexed that are contained in
   * any of the given projects.
   * 
   * @param groups the projects being tested
   * @return <code>true</code> if there are any targets waiting to be indexed that are contained in
   *         any of the given projects
   */
  public synchronized boolean hasQueuedTargetsIn(Set<IndexingTargetGroup> groups) {
    for (IndexingTargetGroup group : groups) {
      if (hasQueuedTargetsIn(group)) {
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

  public synchronized void prioritizeGroup(IndexingTargetGroup group) {
    if (priorityGroups.add(group)) {
      priorityOrderedGroups.add(group);
      priorityOrderedGroupsWithRemainingWork.add(group);
      GroupState state = findOrCreateState(group);
      ++state.prioritizationRequestCount;
    }
  }

  @Deprecated
  public synchronized void prioritizeProject(IProject project) {
    prioritizeGroup(ResourceIndexingTargetGroup.getGroupFor(project));
  }

  public synchronized void reenqueue(IndexingTarget target) {
    state = QueueState.NORMAL; // why?..
    IndexingTargetGroup group = target.getGroup();
    GroupState projectState = findOrCreateState(group);
    projectState.reenqueue(target);
  }

  @Deprecated
  public synchronized void replaceWith(IFile[] filesToIndex) {
    state = QueueState.NORMAL;
    doClearQueue();
    for (IFile file : filesToIndex) {
      doEnqueueTarget(new ResourceIndexingTarget(file));
    }
  }

  public synchronized int size() {
    int size = 0;
    for (GroupState state : groupsToStates.values()) {
      size += state.queue.size();
    }
    return size;
  }

  @Override
  public synchronized String toString() {
    return getClass().getSimpleName();
  }

  public synchronized void unprioritizeGroup(IndexingTargetGroup group) {
    GroupState state = groupsToStates.get(group);
    if (state == null || 0 == state.prioritizationRequestCount) {
      return;
    }
    if (--state.prioritizationRequestCount == 0) {
      priorityGroups.remove(group);
      priorityOrderedGroups.remove(group);
      priorityOrderedGroupsWithRemainingWork.remove(group);
    }
  }

  @Deprecated
  public synchronized void unprioritizeProject(IProject project) {
    unprioritizeGroup(ResourceIndexingTargetGroup.getGroupFor(project));
  }

  private void doClearQueue() {
    groupsToStates.clear();
  }

  private void doEnqueueTarget(IndexingTarget target) {
    findOrCreateState(target.getGroup()).enqueue(target);
  }

  private void enqueue(IndexingTarget target) {
    state = QueueState.NORMAL;
    doEnqueueTarget(target);
  }

  private GroupState findOrCreateState(IndexingTargetGroup group) {
    GroupState projectState = groupsToStates.get(group);
    if (projectState == null) {
      projectState = new GroupState(group);
      groupsToStates.put(group, projectState);
    }
    return projectState;
  }
}
