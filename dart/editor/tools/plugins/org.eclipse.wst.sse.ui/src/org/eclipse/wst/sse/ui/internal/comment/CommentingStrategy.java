/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.comment;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Defines a commenting strategy defined by the
 * <code>org.eclipse.wst.sse.ui.commentinStrategy</code> extension point and tracked by the
 * {@link CommentingStrategyRegistry}. Though it is important to note it is not a one to one
 * relationship of {@link CommentingStrategy}s to extensions, there is actually one
 * {@link CommentingStrategy} for each content type defined for each
 * <code>org.eclipse.wst.sse.ui.commentinStrategy</code> extension.
 * <p>
 * <p>
 * The expected use case is that a {@link CommentingStrategy} is created off the basic configuration
 * of the extension point and then cloned for each associated content type and then each clone has
 * its partition information set using {@link #setPartitionInformation}. Then the
 * {@link CommentingStrategyRegistry} can be used to retrieve applicable {@link CommentingStrategy}s
 * and apply them to documents.
 * </p>
 * <p>
 * It is important to note that any instance of a {@link CommentingStrategy} is only valid for a
 * specific content type ID but this relationship is tracked by the
 * {@link CommentingStrategyRegistry} and not by the strategy itself. Thus any reference to the
 * strategy being valid for specific or general partition types is implying it is already only valid
 * for a specific content type
 * </p>
 */
public abstract class CommentingStrategy {
  /**
   * <code>true</code> if this strategy has any required partition types, <code>false</code>
   * otherwise
   */
  private boolean fHasRequiredPartitionTypes;

  /**
   * <p>
   * required partition type IDs that must be seen for this strategy to be valid, the number of them
   * that must be seen for the strategy to be valid is determined by
   * {@link #fRequireAllRequiredPartitionTypes} this requirement is waved if the optional
   * {@link #fAssociatedCommentPartitionTypeID} is specified and present because this strategy must
   * be valid if its {@link #fAssociatedCommentPartitionTypeID} is present
   * </p>
   * 
   * @see #fRequireAllRequiredPartitionTypes
   * @see #fAssociatedCommentPartitionTypeID
   */
  private List fRequriedPartitionTypeIDs;

  /**
   * <p>
   * if <code>true</code> then {@link #fAllowablePartitionTypeIDs} is ignored because this strategy
   * is valid for any partition type, if <code>false</code> then this strategy is only valid for
   * those partition types listed in {@link #fAllowablePartitionTypeIDs}
   * </p>
   * 
   * @see #fAllowablePartitionTypeIDs
   */
  private boolean fAllPartitionTypesAllowable;

  /**
   * <p>
   * the partition types that this strategy is valid for, it is also automatically valid for any
   * partition types listed in {@link #fRequriedPartitionTypeIDs} and the optionally specified
   * {@link #fAssociatedCommentPartitionTypeID}
   * </p>
   * 
   * @see #fAllPartitionTypesAllowable
   * @see #fRequriedPartitionTypeIDs
   * @see #fAssociatedCommentPartitionTypeID
   */
  private List fAllowablePartitionTypeIDs;

  /**
   * an optional associated comment partition type ID, if this partition type is seen then the the
   * {@link #fRequriedPartitionTypeIDs} requirement is waved as to weather this strategy is valid or
   * not
   * 
   * @see #fRequriedPartitionTypeIDs
   */
  private String fAssociatedCommentPartitionTypeID;

  /**
   * <p>
   * Default constructor, the specific initialization is done by {@link #setPartitionInformation}
   * </p>
   */
  public CommentingStrategy() {
    this.fAssociatedCommentPartitionTypeID = null;
    this.fRequriedPartitionTypeIDs = Collections.EMPTY_LIST;
    this.fAllowablePartitionTypeIDs = Collections.EMPTY_LIST;
    this.fHasRequiredPartitionTypes = false;
    this.fAllPartitionTypesAllowable = false;
  }

  /**
   * <p>
   * Used to set up the partition information for this strategy
   * </p>
   * <p>
   * This information is used to determine if a strategy is valid for a set of {@link ITypedRegion}
   * s.
   * </p>
   * 
   * @param allowablePartitionTypeIDs the partition types this strategy is valid for, the strategy
   *          will also be considered valid for any of the required partition types
   * @param allPartitionTypesAllowable if <code>true</code> then this strategy is valid for any
   *          partition types thus ignoring the <code>allowablePartitionTypeIDs</code>,
   *          <code>false</code> otherwise
   * @param requiredPartitionTypeIDs partition type IDs that must be seen for this strategy to be
   *          valid, there are exceptions to this rule, see {@link #isApplicableFor(ITypedRegion[])}
   * @param requireAllRequiredPartitionTypes <code>true</code> if all of the
   *          <code>requiredPartitionTypeIDs must
   * be seen for this strategy to be valid, <code>false</code> otherwise, there are exceptions to
   *          these rules, see {@link #isApplicableFor(ITypedRegion[])}
   * @param associatedCommentPartitionTypeID optional comment partition type associated with this
   *          strategy, maybe <code>null</code>
   * @see #isApplicableFor(ITypedRegion[])
   */
  protected final void setPartitionInformation(List allowablePartitionTypeIDs,
      boolean allPartitionTypesAllowable, List requiredPartitionTypeIDs,
      String associatedCommentPartitionTypeID) {

    this.fAllPartitionTypesAllowable = allPartitionTypesAllowable;

    this.fRequriedPartitionTypeIDs = requiredPartitionTypeIDs;
    if (this.fRequriedPartitionTypeIDs == null) {
      this.fRequriedPartitionTypeIDs = Collections.EMPTY_LIST;
    }

    this.fHasRequiredPartitionTypes = this.fRequriedPartitionTypeIDs.size() != 0;

    this.fAllowablePartitionTypeIDs = allowablePartitionTypeIDs;
    if (this.fAllowablePartitionTypeIDs == null) {
      this.fAllowablePartitionTypeIDs = Collections.EMPTY_LIST;
    }

    this.fAssociatedCommentPartitionTypeID = associatedCommentPartitionTypeID;
  }

  /**
   * <p>
   * Applies this strategy to the given model starting at the given offset for the given length
   * </p>
   * 
   * @param document {@link IStructuredDocument} to apply this strategy too
   * @param offset the offset to start this comment at
   * @param length the length of the region to apply this comment too
   * @throws BadLocationException it is not the fault of the strategy if callers passes a bad
   *           <code>offset</code> and/or <code>length</code> for the given <code>model</code>
   */
  public abstract void apply(IStructuredDocument document, int offset, int length)
      throws BadLocationException;

  /**
   * <p>
   * Remove any comments associated with this strategy from the given model for the given offset to
   * the given length. Weather a comment surrounding the given range should be removed can also be
   * specified
   * </p>
   * 
   * @param document {@link IStructuredDocument} to remove comments associated with this strategy
   *          from
   * @param offset the location to start removing comments associated with this strategy from
   * @param length the length of the region to remove associated comments from
   * @param removeEnclosing weather a comment should be removed if it incloses the region specified
   *          by the given <code>offset</code> and <code>length</code>
   * @throws BadLocationException it is not the fault of the strategy if callers passes a bad
   *           <code>offset</code> and/or <code>length</code> for the given <code>model</code>
   */
  public abstract void remove(IStructuredDocument document, int offset, int length,
      boolean removeEnclosing) throws BadLocationException;

  /**
   * <p>
   * Determines if the given region is a comment region commented by this strategy.
   * </p>
   * 
   * @param document {@link IStructuredDocument} containing the given <code>region</code>
   * @param region determine if this region is a comment region commented by this strategy
   * @return <code>true</code> if the given <code>region</code> has already been commented by this
   *         strategy, <code>false</code> otherwise
   * @throws BadLocationException it is not the fault of the strategy if callers passes a bad
   *           <code>offset</code> and/or <code>length</code> for the given <code>model</code>
   */
  public abstract boolean alreadyCommenting(IStructuredDocument document, IRegion region)
      throws BadLocationException;

  /**
   * <p>
   * Implementers should return a copy of themselves
   * </p>
   * <p>
   * Allows the {@link CommentingStrategyRegistry} to create a {@link CommentingStrategy} for each
   * of its associated content types.
   * </p>
   * 
   * @return implementers should return an object of type {@link CommentingStrategy}
   * @see java.lang.Object#clone()
   */
  public abstract Object clone();

  /**
   * <p>
   * Determines if this strategy is applicable for the given regions for either commenting or
   * un-commenting. For this strategy to be applicable the given regions must contain at least one
   * or all of the {@link #fRequriedPartitionTypeIDs} (depending on the value of
   * {@link #fRequireAllRequiredPartitionTypes}) or contain at least one region of type
   * {@link #fAssociatedCommentPartitionTypeID}. Also if the value of
   * {@link #fAllPartitionTypesAllowable} is <code>false</code> the given regions must all be of
   * type {@link #fAllowablePartitionTypeIDs} and/or {@link #fRequriedPartitionTypeIDs} and/or
   * {@link #fAssociatedCommentPartitionTypeID} otherwise the regions can be of any type except for
   * they still must beet the required partition type requirements
   * </p>
   * 
   * @param regions determine if this strategy is applicable for these regions
   * @return <code>true</code> if this strategy is applicable for the given <code>regions</code>
   *         <code>false</code> otherwise.
   */
  public final boolean isApplicableFor(ITypedRegion[] regions) {
    List partitionTypeIDs = getPartitionTypeIDs(regions);

    boolean foundAssociatedCommentPartitions = false;
    if (this.fAssociatedCommentPartitionTypeID != null) {
      foundAssociatedCommentPartitions = partitionTypeIDs.contains(this.fAssociatedCommentPartitionTypeID);
      if (foundAssociatedCommentPartitions) {
        //remove all instances of the comment partition type
        boolean removed;
        do {
          removed = partitionTypeIDs.remove(this.fAssociatedCommentPartitionTypeID);
        } while (removed);
      }
    }

    //determine if required partitions requirements are met
    boolean requiredPartitionsRequirementsMet = !this.fHasRequiredPartitionTypes
        || partitionTypeIDs.removeAll(this.fRequriedPartitionTypeIDs);

    //determine if allowable partitions requirements are met
    boolean allowablePartitionsRequirementsMet = false;
    if (this.fAllPartitionTypesAllowable) {
      allowablePartitionsRequirementsMet = true;
    } else {
      partitionTypeIDs.removeAll(this.fAllowablePartitionTypeIDs);

      //at this point all required partitions and allowable partitions have been removed
      allowablePartitionsRequirementsMet = partitionTypeIDs.size() == 0;
    }

    return (requiredPartitionsRequirementsMet || foundAssociatedCommentPartitions)
        && allowablePartitionsRequirementsMet;
  }

  /**
   * <p>
   * Convenience method to take a list of regions and create one encompassing region to pass to
   * {@link #alreadyCommenting(IDocument, IRegion)}
   * </p>
   * 
   * @param document {@link IDocument} that contains the given <code>regions</code>
   * @param regions {@link IRegion}s to combine into one region and pass onto
   *          {@link #alreadyCommenting(IDocument, IRegion)}
   * @return the result of a call to {@link #alreadyCommenting(IDocument, IRegion)} combining all of
   *         the given <code>regions</code> into one region
   * @throws BadLocationException it is not the fault of the strategy if callers passes a bad
   *           <code>offset</code> and/or <code>length</code> for the given <code>model</code>
   */
  public final boolean alreadyCommenting(IStructuredDocument document, IRegion[] regions)
      throws BadLocationException {
    boolean alreadyCommenting = false;
    if (regions != null && regions.length > 0) {
      //create one region spanning all the given regions
      int offset = regions[0].getOffset();
      int length = regions[regions.length - 1].getOffset()
          + regions[regions.length - 1].getLength() - offset;

      IRegion region = new Region(offset, length);
      alreadyCommenting = this.alreadyCommenting(document, region);
    }

    return alreadyCommenting;
  }

  /**
   * <p>
   * Given a list of {@link ITypedRegion}s returns the sub set of that list that are of the comment
   * region type associated with this strategy
   * </p>
   * 
   * @param typedRegions {@link ITypedRegion}s to filter down to only the comment regions associated
   *          with this strategy
   * @return {@link List} of {@link ITypedRegion}s from the given <code>typedRegions</code> that are
   *         of the comment partition type associated with this strategy
   */
  protected List getAssociatedCommentedRegions(ITypedRegion[] typedRegions) {
    List commentedRegions = new ArrayList();

    for (int i = 0; i < typedRegions.length; ++i) {
      if (typedRegions[i].getType().equals(this.fAssociatedCommentPartitionTypeID)) {
        commentedRegions.add(typedRegions[i]);
      }
    }

    return commentedRegions;
  }

  /**
   * <p>
   * Given a list of {@link ITypedRegion}s returns a list of the partition type IDs taken from the
   * given regions.
   * </p>
   * 
   * @param regions {@link ITypedRegion}s to get the partition type IDs from
   * @return {@link List} of the partition type IDs taken from the given <code>regions</code>
   */
  private static List getPartitionTypeIDs(ITypedRegion[] regions) {
    List partitionTypes = new ArrayList(regions.length);
    for (int i = 0; i < regions.length; ++i) {
      partitionTypes.add(regions[i].getType());
    }

    return partitionTypes;
  }

  /**
   * <p>
   * This method modifies the given document to remove the given comment prefix at the given comment
   * prefix offset and the given comment suffix at the given comment suffix offset. In the case of
   * removing a line comment that does not have a suffix, pass <code>null</code> for the comment
   * suffix and it and its associated offset will be ignored.
   * </p>
   * <p>
   * <b>NOTE:</b> it is a good idea if a model is at hand when calling this to warn the model of an
   * impending update
   * </p>
   * 
   * @param document the document to remove the comment from
   * @param commentPrefixOffset the offset of the comment prefix
   * @param commentSuffixOffset the offset of the comment suffix (ignored if
   *          <code>commentSuffix</code> is <code>null</code>)
   * @param commentPrefix the prefix of the comment to remove from its associated given offset
   * @param commentSuffix the suffix of the comment to remove from its associated given offset, or
   *          null if there is not suffix to remove for this comment
   */
  protected static void uncomment(IDocument document, int commentPrefixOffset,
      String commentPrefix, int commentSuffixOffset, String commentSuffix) {

    try {
      //determine if there is a space after the comment prefix that should also be removed
      int commentPrefixLength = commentPrefix.length();
      String postCommentPrefixChar = document.get(commentPrefixOffset + commentPrefix.length(), 1);
      if (postCommentPrefixChar.equals(" ")) {
        commentPrefixLength++;
      }

      //remove the comment prefix
      document.replace(commentPrefixOffset, commentPrefixLength, ""); //$NON-NLS-1$

      if (commentSuffix != null) {
        commentSuffixOffset -= commentPrefixLength;

        //determine if there is a space before the comment suffix that should also be removed
        int commentSuffixLength = commentSuffix.length();
        String preCommentSuffixChar = document.get(commentSuffixOffset - 1, 1);
        if (preCommentSuffixChar.equals(" ")) {
          commentSuffixLength++;
          commentSuffixOffset--;
        }

        //remove the comment suffix
        document.replace(commentSuffixOffset, commentSuffixLength, ""); //$NON-NLS-1$
      }
    } catch (BadLocationException e) {
      Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
    }
  }
}
