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
 * Instances of the class {@code ChangeSet} indicate which sources have been added, changed,
 * removed, or deleted. In the case of a changed source, there are multiple ways of indicating the
 * nature of the change.
 * <p>
 * No source should be added to the change set more than once, either with the same or a different
 * kind of change. It does not make sense, for example, for a source to be both added and removed,
 * and it is redundant for a source to be marked as changed in its entirety and changed in some
 * specific range.
 * 
 * @coverage dart.engine
 */
public class ChangeSet {
  /**
   * Instances of the class {@code ContentChange} represent a change to the content of a source.
   */
  public static class ContentChange {
    /**
     * The new contents of the source.
     */
    private String contents;

    /**
     * The offset into the current contents.
     */
    private int offset;

    /**
     * The number of characters in the original contents that were replaced
     */
    private int oldLength;

    /**
     * The number of characters in the replacement text.
     */
    private int newLength;

    /**
     * Initialize a newly created change object to represent a change to the content of a source.
     * 
     * @param contents the new contents of the source
     * @param offset the offset into the current contents
     * @param oldLength the number of characters in the original contents that were replaced
     * @param newLength the number of characters in the replacement text
     */
    public ContentChange(String contents, int offset, int oldLength, int newLength) {
      this.contents = contents;
      this.offset = offset;
      this.oldLength = oldLength;
      this.newLength = newLength;
    }

    /**
     * Return the new contents of the source.
     * 
     * @return the new contents of the source
     */
    public String getContents() {
      return contents;
    }

    /**
     * Return the number of characters in the replacement text.
     * 
     * @return the number of characters in the replacement text
     */
    public int getNewLength() {
      return newLength;
    }

    /**
     * Return the offset into the current contents.
     * 
     * @return the offset into the current contents
     */
    public int getOffset() {
      return offset;
    }

    /**
     * Return the number of characters in the original contents that were replaced.
     * 
     * @return the number of characters in the original contents that were replaced
     */
    public int getOldLength() {
      return oldLength;
    }
  }

  /**
   * A list containing the sources that have been added.
   */
  private ArrayList<Source> addedSources = new ArrayList<Source>();

  /**
   * A list containing the sources that have been changed.
   */
  private ArrayList<Source> changedSources = new ArrayList<Source>();

  /**
   * A table mapping the sources whose content has been changed to the current content of those
   * sources.
   */
  private HashMap<Source, String> changedContent = new HashMap<Source, String>();

  /**
   * A table mapping the sources whose content has been changed within a single range to the current
   * content of those sources and information about the affected range.
   */
  private HashMap<Source, ContentChange> changedRanges = new HashMap<Source, ContentChange>();

  /**
   * A list containing the sources that have been removed.
   */
  private ArrayList<Source> removedSources = new ArrayList<Source>();

  /**
   * A list containing the source containers specifying additional sources that have been removed.
   */
  private ArrayList<SourceContainer> removedContainers = new ArrayList<SourceContainer>();

  /**
   * A list containing the sources that have been deleted.
   */
  private ArrayList<Source> deletedSources = new ArrayList<Source>();

  /**
   * Initialize a newly created change set to be empty.
   */
  public ChangeSet() {
    super();
  }

  /**
   * Record that the specified source has been added and that its content is the default contents of
   * the source.
   * 
   * @param source the source that was added
   */
  public void addedSource(Source source) {
    addedSources.add(source);
  }

  /**
   * Record that the specified source has been changed and that its content is the given contents.
   * 
   * @param source the source that was changed
   * @param contents the new contents of the source, or {@code null} if the default contents of the
   *          source are to be used
   */
  public void changedContent(Source source, String contents) {
    changedContent.put(source, contents);
  }

  /**
   * Record that the specified source has been changed and that its content is the given contents.
   * 
   * @param source the source that was changed
   * @param contents the new contents of the source
   * @param offset the offset into the current contents
   * @param oldLength the number of characters in the original contents that were replaced
   * @param newLength the number of characters in the replacement text
   */
  public void changedRange(Source source, String contents, int offset, int oldLength, int newLength) {
    changedRanges.put(source, new ContentChange(contents, offset, oldLength, newLength));
  }

  /**
   * Record that the specified source has been changed. If the content of the source was previously
   * overridden, this has no effect (the content remains overridden). To cancel (or change) the
   * override, use {@link #changedContent(Source, String)} instead.
   * 
   * @param source the source that was changed
   */
  public void changedSource(Source source) {
    changedSources.add(source);
  }

  /**
   * Record that the specified source has been deleted.
   * 
   * @param source the source that was deleted
   */
  public void deletedSource(Source source) {
    deletedSources.add(source);
  }

  /**
   * Return a collection of the sources that have been added.
   * 
   * @return a collection of the sources that have been added
   */
  public List<Source> getAddedSources() {
    return addedSources;
  }

  /**
   * Return a table mapping the sources whose content has been changed to the current content of
   * those sources.
   * 
   * @return a table mapping the sources whose content has been changed to the current content of
   *         those sources
   */
  public Map<Source, String> getChangedContents() {
    return changedContent;
  }

  /**
   * Return a table mapping the sources whose content has been changed within a single range to the
   * current content of those sources and information about the affected range.
   * 
   * @return a table mapping sources to information about the changes to them
   */
  public HashMap<Source, ContentChange> getChangedRanges() {
    return changedRanges;
  }

  /**
   * Return a collection of sources that have been changed.
   * 
   * @return a collection of sources that have been changed
   */
  public List<Source> getChangedSources() {
    return changedSources;
  }

  /**
   * Return a collection of sources that have been deleted.
   * 
   * @return a collection of sources that have been deleted
   */
  public List<Source> getDeletedSources() {
    return deletedSources;
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
   * Return a list containing the sources that were removed.
   * 
   * @return a list containing the sources that were removed
   */
  public List<Source> getRemovedSources() {
    return removedSources;
  }

  /**
   * Return {@code true} if this change set does not contain any changes.
   * 
   * @return {@code true} if this change set does not contain any changes
   */
  public boolean isEmpty() {
    return addedSources.isEmpty() && changedSources.isEmpty() && changedContent.isEmpty()
        && changedRanges.isEmpty() && removedSources.isEmpty() && removedContainers.isEmpty()
        && deletedSources.isEmpty();
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

  /**
   * Record that the specified source has been removed.
   * 
   * @param source the source that was removed
   */
  public void removedSource(Source source) {
    if (source != null) {
      removedSources.add(source);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean needsSeparator = appendSources(builder, addedSources, false, "addedSources");
    needsSeparator = appendSources(builder, changedSources, needsSeparator, "changedSources");
    needsSeparator = appendSources(builder, changedContent, needsSeparator, "changedContent");
    needsSeparator = appendSources(builder, changedRanges, needsSeparator, "changedRanges");
    needsSeparator = appendSources(builder, deletedSources, needsSeparator, "deletedSources");
    needsSeparator = appendSources(builder, removedSources, needsSeparator, "removedSources");
    int count = removedContainers.size();
    if (count > 0) {
      if (removedSources.isEmpty()) {
        if (needsSeparator) {
          builder.append("; ");
        }
        builder.append("removed: from ");
        builder.append(count);
        builder.append(" containers");
      } else {
        builder.append(", and more from ");
        builder.append(count);
        builder.append(" containers");
      }
    }
    return builder.toString();
  }

  /**
   * Append the given sources to the given builder, prefixed with the given label and possibly a
   * separator.
   * 
   * @param builder the builder to which the sources are to be appended
   * @param sources the sources to be appended
   * @param needsSeparator {@code true} if a separator is needed before the label
   * @param label the label used to prefix the sources
   * @return {@code true} if future lists of sources will need a separator
   */
  private boolean appendSources(StringBuilder builder, ArrayList<Source> sources,
      boolean needsSeparator, String label) {
    if (sources.isEmpty()) {
      return needsSeparator;
    }
    if (needsSeparator) {
      builder.append("; ");
    }
    builder.append(label);
    String prefix = " ";
    for (Source source : sources) {
      builder.append(prefix);
      builder.append(source.getFullName());
      prefix = ", ";
    }
    return true;
  }

  /**
   * Append the given sources to the given builder, prefixed with the given label and possibly a
   * separator.
   * 
   * @param builder the builder to which the sources are to be appended
   * @param sources the sources to be appended
   * @param needsSeparator {@code true} if a separator is needed before the label
   * @param label the label used to prefix the sources
   * @return {@code true} if future lists of sources will need a separator
   */
  private boolean appendSources(StringBuilder builder, HashMap<Source, ?> sources,
      boolean needsSeparator, String label) {
    if (sources.isEmpty()) {
      return needsSeparator;
    }
    if (needsSeparator) {
      builder.append("; ");
    }
    builder.append(label);
    String prefix = " ";
    for (Source source : sources.keySet()) {
      builder.append(prefix);
      builder.append(source.getFullName());
      prefix = ", ";
    }
    return true;
  }
}
