/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.search;

import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.NewSearchUI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;

/**
 * <p>
 * Finds occurrences of a specified region type w/ region text in an IStructuredDocument. Clients
 * must implement getPartitionTypes() and getRegionTypes() to indicate which partition types and
 * region types it can operate on.
 * </p>
 * <p>
 * Clients should override <code>getSearchQuery()</code> in order to provide their own type of
 * "search" (eg. searching for XML start tags, searching for Java elements, etc...)
 * </p>
 */
abstract public class FindOccurrencesProcessor {
  public boolean findOccurrences(IDocument document, ITextSelection textSelection, IFile file) {
    boolean findOccurrences = false;

    // determine if action should be enabled or not
    if (document instanceof IStructuredDocument) {
      IStructuredDocument structuredDocument = (IStructuredDocument) document;
      IStructuredDocumentRegion sdRegion = structuredDocument.getRegionAtCharacterOffset(textSelection.getOffset());
      if (sdRegion != null) {
        ITextRegion r = sdRegion.getRegionAtCharacterOffset(textSelection.getOffset());
        if (r != null) {
          String type = r.getType();
          if (enabledForRegionType(type)) {
            String matchText = sdRegion.getText(r);

            // first of all activate the view
            NewSearchUI.activateSearchResultView();

            if (matchText != null && type != null) {
              ISearchQuery searchQuery = getSearchQuery(file, structuredDocument, matchText, type,
                  textSelection);
              if (searchQuery != null) {
                if (searchQuery.canRunInBackground())
                  NewSearchUI.runQueryInBackground(searchQuery);
                else
                  NewSearchUI.runQueryInForeground(null, searchQuery);
              }
              findOccurrences = true;
            }
          }
        }
      }
    }
    return findOccurrences;
  }

  /**
   * @param regionType
   * @return <code>true</code> if this action can operate on this region type (ITextRegion),
   *         otherwise false.
   */
  protected boolean enabledForRegionType(String regionType) {

    String[] accept = getRegionTypes();
    for (int i = 0; i < accept.length; i++) {
      if (regionType.equals(accept[i]))
        return true;
    }
    return false;
  }

  /**
   * Clients should override this to enable find occurrences on certain partition(s).
   */
  abstract protected String[] getPartitionTypes();

  /**
   * Clients should override this to enable find occurrences on different region type(s).
   */
  abstract protected String[] getRegionTypes();

  /**
   * Clients should override to provide their own search for the file.
   */
  protected ISearchQuery getSearchQuery(IFile file, IStructuredDocument document,
      String regionText, String regionType, ITextSelection textSelection) {
    return new OccurrencesSearchQuery(file, document, regionText, regionType);
  }

  /**
   * @param partitionType
   * @return <code>true</code> if this action can operate on this type of partition, otherwise
   *         <code>false</code>.
   */
  public boolean enabledForParitition(String partitionType) {
    String[] accept = getPartitionTypes();
    for (int i = 0; i < accept.length; i++) {
      if (partitionType.equals(accept[i]))
        return true;
    }
    return false;
  }
}
