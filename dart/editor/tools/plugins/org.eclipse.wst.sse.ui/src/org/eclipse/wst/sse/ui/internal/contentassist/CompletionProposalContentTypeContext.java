/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentassist;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * There should be one context for each content type. It keeps track of the describers associated
 * with different partition types for its unique content type.
 * </p>
 */
public class CompletionProposalContentTypeContext {

  /** the content type this context is associated with */
  private final String fContentTypeID;

  /**
   * <code>{@link Map}&lt{@link String}, {@link Set}&lt{@link CompletionProposalComputerDescriptor}&gt&gt</code>
   * <ul>
   * <li><b>key:</b> partition type ID</li>
   * <li><b>value:</b> {@link Set} of associated computer descriptors</li>
   * </ul>
   */
  private Map fPartitionTypesToDescriptors;

  /**
   * <p>
   * Create a new context for the given content type ID.
   * </p>
   * <p>
   * There should only ever be one context for any unique content type
   * </p>
   * 
   * @param contentTypeID the content type this context is for
   */
  public CompletionProposalContentTypeContext(String contentTypeID) {
    this.fContentTypeID = contentTypeID;
    this.fPartitionTypesToDescriptors = new HashMap();
  }

  /**
   * <p>
   * Adds a describer to this context for a given partition type. There can be more then one
   * describer for one partition type.
   * </p>
   * 
   * @param partitionTypeID the partition type to associate the given descriptor with
   * @param descriptor {@link CompletionProposalComputerDescriptor} to associate with the given
   *          partition type in this context
   */
  public void putDescriptor(String partitionTypeID, CompletionProposalComputerDescriptor descriptor) {
    Set descriptors = (Set) this.fPartitionTypesToDescriptors.get(partitionTypeID);
    if (descriptors != null) {
      descriptors.add(descriptor);
    } else {
      descriptors = new HashSet();
      descriptors.add(descriptor);
      this.fPartitionTypesToDescriptors.put(partitionTypeID, descriptors);
    }
  }

  /**
   * @return All of the {@link CompletionProposalComputerDescriptor}s associated with any partition
   *         type in this context
   */
  public Set getDescriptors() {
    Set allDescriptors = new HashSet();
    Collection descriptorSets = this.fPartitionTypesToDescriptors.values();
    Iterator iter = descriptorSets.iterator();
    while (iter.hasNext()) {
      Set descriptorSet = (Set) iter.next();
      allDescriptors.addAll(descriptorSet);
    }

    return Collections.unmodifiableSet(allDescriptors);
  }

  /**
   * @param partitionTypeID get {@link CompletionProposalComputerDescriptor}s for only this
   *          partition type for this context
   * @return {@link CompletionProposalComputerDescriptor}s assoicated with the given partition type
   *         in this context
   */
  public Set getDescriptors(String partitionTypeID) {
    Set descriptors = (Set) this.fPartitionTypesToDescriptors.get(partitionTypeID);
    return descriptors != null ? Collections.unmodifiableSet(descriptors) : Collections.EMPTY_SET;
  }

  /**
   * @return the content type this context is for
   */
  public String getContentTypeID() {
    return fContentTypeID;
  }

  /**
   * @return the hash code of the content type ID
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return fContentTypeID.hashCode();
  }

  /**
   * <p>
   * Two {@link CompletionProposalContentTypeContext} are equal if they have the same content type
   * ID.
   * </p>
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    boolean equal = false;
    if (obj instanceof CompletionProposalContentTypeContext) {
      equal = this.fContentTypeID.equals(((CompletionProposalContentTypeContext) obj).fContentTypeID);
    }

    return equal;
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer buff = new StringBuffer(this.fContentTypeID);
    Set partitions = this.fPartitionTypesToDescriptors.keySet();
    Iterator partitionsIter = partitions.iterator();
    while (partitionsIter.hasNext()) {
      String partitionType = (String) partitionsIter.next();
      buff.append("\n\t" + partitionType); //$NON-NLS-1$
      List descriptors = (List) this.fPartitionTypesToDescriptors.get(partitionType);
      for (int i = 0; i < descriptors.size(); ++i) {
        buff.append("\n\t\t" + descriptors.get(i).toString()); //$NON-NLS-1$
      }
    }

    return buff.toString();
  }
}
