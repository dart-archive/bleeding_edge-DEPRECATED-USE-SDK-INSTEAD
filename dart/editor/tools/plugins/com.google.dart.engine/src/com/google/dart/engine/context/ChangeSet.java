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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class {@code ChangeSet} indicate what sources have been added, changed, or
 * removed.
 */
public class ChangeSet {
  /**
   * A table mapping the sources that have been added to their contents.
   */
  private HashMap<Source, String> added = new HashMap<Source, String>();

  /**
   * A table mapping the sources that have been changed to their contents.
   */
  private HashMap<Source, String> changed = new HashMap<Source, String>();

  /**
   * A list containing the sources that have been removed..
   */
  private ArrayList<Source> removed = new ArrayList<Source>();

  /**
   * A list containing the source containers specifying additional sources that have been removed.
   */
  private ArrayList<SourceContainer> removedContainers = new ArrayList<SourceContainer>();

  /**
   * Initialize a newly created change set to be empty.
   */
  public ChangeSet() {
    super();
  }

  /**
   * Record that the specified source has been added and that it's content is the default contents
   * of the source.
   * 
   * @param source the source that was added
   */
  public void added(Source source) {
    added(source, null);
  }

  /**
   * Record that the specified source has been added and that it has the given content. If the
   * content is non-{@code null}, this has the effect of overriding the default contents of the
   * source. If the contents are {@code null}, any previous the override is removed so that the
   * default contents will be used.
   * 
   * @param source the source that was added
   * @param content the content of the new source
   */
  public void added(Source source, String content) {
    if (source != null) {
      added.put(source, content);
    }
  }

  /**
   * Record that the specified source has been changed and that it's content is the default contents
   * of the source.
   * 
   * @param source the source that was changed
   */
  public void changed(Source source) {
    changed(source, null);
  }

  /**
   * Record that the specified source has been changed and that it now has the given content. If the
   * content is non-{@code null}, this has the effect of overriding the default contents of the
   * source. If the contents are {@code null}, any previous the override is removed so that the
   * default contents will be used.
   * 
   * @param source the source that was changed
   * @param content the new content of the source
   */
  public void changed(Source source, String content) {
    if (source != null) {
      changed.put(source, content);
    }
  }

  /**
   * Return a table mapping the sources that have been added to their contents.
   * 
   * @return a table mapping the sources that have been added to their contents
   */
  public Map<Source, String> getAddedWithContent() {
    return added;
  }

  /**
   * Return a table mapping the sources that have been changed to their contents.
   * 
   * @return a table mapping the sources that have been changed to their contents
   */
  public Map<Source, String> getChangedWithContent() {
    return changed;
  }

  /**
   * Return a list containing the sources that were removed.
   * 
   * @return a list containing the sources that were removed
   */
  public List<Source> getRemoved() {
    return removed;
  }

  /**
   * Return a list containing the source containers that were removed.
   * 
   * @return a list containing the source containers that were removed
   */
  public List<SourceContainer> getRemovedContainers() {
    return removedContainers;
  }

  /**
   * Return {@code true} if this change set does not contain any changes.
   * 
   * @return {@code true} if this change set does not contain any changes
   */
  public boolean isEmpty() {
    return added.isEmpty() && changed.isEmpty() && removed.isEmpty() && removedContainers.isEmpty();
  }

  /**
   * Record that the specified source has been removed.
   * 
   * @param source the source that was removed
   */
  public void removed(Source source) {
    if (source != null) {
      removed.add(source);
    }
  }

  /**
   * Record that the specified source container has been removed.
   * 
   * @param container the source container that was removed
   */
  public void removedContainer(SourceContainer container) {
    if (container != null) {
      removedContainers.add(container);
    }
  }
}
