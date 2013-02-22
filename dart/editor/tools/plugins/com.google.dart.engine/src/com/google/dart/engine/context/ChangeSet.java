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
package com.google.dart.engine.context;

import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

import java.util.ArrayList;

/**
 * Instances of {@code ChangeSet} indicate what sources have been added, changed, or removed.
 */
public class ChangeSet {
  private ArrayList<Source> added = new ArrayList<Source>();
  private ArrayList<Source> changed = new ArrayList<Source>();
  private ArrayList<Source> removed = new ArrayList<Source>();
  private ArrayList<SourceContainer> removedContainers = new ArrayList<SourceContainer>();

  /**
   * Mark the specified source as added.
   * 
   * @param source the source that was added
   */
  public void added(Source source) {
    if (source != null) {
      added.add(source);
    }
  }

  /**
   * Mark the specified source as changed.
   * 
   * @param source the source that has changed
   */
  public void changed(Source source) {
    if (source != null) {
      changed.add(source);
    }
  }

  /**
   * Return the sources that were added.
   * 
   * @return a collection of added sources (not {@code null}, contains no {@code null}s)
   */
  public ArrayList<Source> getAdded() {
    return added;
  }

  /**
   * Return the sources that were changed.
   * 
   * @return a collection of changed sources (not {@code null}, contains no {@code null}s)
   */
  public ArrayList<Source> getChanged() {
    return changed;
  }

  /**
   * Return the sources that were removed.
   * 
   * @return a collection of removed sources (not {@code null}, contains no {@code null}s)
   */
  public ArrayList<Source> getRemoved() {
    return removed;
  }

  /**
   * Return the source containers that were removed.
   * 
   * @return a collection of removed source containers (not {@code null}, contains no {@code null}s)
   */
  public ArrayList<SourceContainer> getRemovedContainers() {
    return removedContainers;
  }

  /**
   * Determine if the receiver contains any changes.
   * 
   * @return {@code true} if the receiver does not contain any changes
   */
  public boolean isEmpty() {
    return added.isEmpty() && changed.isEmpty() && removed.isEmpty() && removedContainers.isEmpty();
  }

  /**
   * Mark the specified source as having been removed.
   * 
   * @param source the source that was removed
   */
  public void removed(Source source) {
    if (source != null) {
      removed.add(source);
    }
  }

  /**
   * Mark the specified source container as having been removed.
   * 
   * @param sourceContainer the container that was removed
   */
  public void removedContainer(SourceContainer sourceContainer) {
    if (sourceContainer != null) {
      removedContainers.add(sourceContainer);
    }
  }
}
