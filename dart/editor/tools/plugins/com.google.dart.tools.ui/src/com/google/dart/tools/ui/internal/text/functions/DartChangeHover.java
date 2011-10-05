/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.functions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * A line change hover for Java source code. Adds a custom information control creator returning a
 * source viewer with syntax coloring.
 */
public class DartChangeHover extends LineChangeHover {

  /** The last computed partition type. */
  private String fPartition;
  /** The last created information control. */
  private ChangeHoverInformationControl fInformationControl;
  /** The document partitioning to be used by this hover. */
  private String fPartitioning;
  /** The last created information control. */
  private int fLastScrollIndex = 0;

  /**
   * The orientation to be used by this hover. Allowed values are: SWT#RIGHT_TO_LEFT or
   * SWT#LEFT_TO_RIGHT
   */
  private int fOrientation;

  /**
   * Creates a new change hover for the given document partitioning.
   * 
   * @param partitioning the document partitioning
   * @param orientation the orientation, allowed values are: SWT#RIGHT_TO_LEFT or SWT#LEFT_TO_RIGHT
   */
  public DartChangeHover(String partitioning, int orientation) {
    Assert.isLegal(orientation == SWT.RIGHT_TO_LEFT || orientation == SWT.LEFT_TO_RIGHT);
    fPartitioning = partitioning;
    fOrientation = orientation;
  }

  /*
   * @see org.eclipse.jface.text.source.IAnnotationHoverExtension#getHoverControlCreator ()
   */
  @Override
  public IInformationControlCreator getHoverControlCreator() {
    return new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell parent) {
        int shellStyle = SWT.TOOL | SWT.NO_TRIM | fOrientation;
        fInformationControl = new ChangeHoverInformationControl(parent, shellStyle, SWT.NONE,
            fPartition, EditorsUI.getTooltipAffordanceString());
        fInformationControl.setHorizontalScrollPixel(fLastScrollIndex);
        return fInformationControl;
      }
    };
  }

  /*
   * @see org.eclipse.jface.text.information.IInformationProviderExtension2#
   * getInformationPresenterControlCreator()
   */
  @Override
  public IInformationControlCreator getInformationPresenterControlCreator() {
    return new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell parent) {
        int shellStyle = SWT.RESIZE | SWT.TOOL | fOrientation;
        int style = SWT.V_SCROLL | SWT.H_SCROLL;
        fInformationControl = new ChangeHoverInformationControl(parent, shellStyle, style,
            fPartition, null);
        fInformationControl.setHorizontalScrollPixel(fLastScrollIndex);
        return fInformationControl;
      }
    };
  }

  /*
   * @see org.eclipse.jface.text.source.LineChangeHover#computeLineRange(org.eclipse
   * .jface.text.source.ISourceViewer, int, int, int)
   */
  @Override
  protected Point computeLineRange(ISourceViewer viewer, int line, int first, int number) {
    Point lineRange = super.computeLineRange(viewer, line, first, number);
    if (lineRange != null) {
      fPartition = getPartition(viewer, lineRange.x);
    } else {
      fPartition = IDocument.DEFAULT_CONTENT_TYPE;
    }
    fLastScrollIndex = viewer.getTextWidget().getHorizontalPixel();
    if (fInformationControl != null) {
      fInformationControl.setStartingPartitionType(fPartition);
      fInformationControl.setHorizontalScrollPixel(fLastScrollIndex);
    }
    return lineRange;
  }

  /*
   * @see org.eclipse.ui.internal.editors.text.LineChangeHover#formatSource(java. lang.String)
   */
  @Override
  protected String formatSource(String content) {
    return content;
  }

  /*
   * @see org.eclipse.jface.text.source.LineChangeHover#getTabReplacement()
   */
  @Override
  protected String getTabReplacement() {
    return Character.toString('\t');
  }

  /**
   * Returns the partition type of the document displayed in <code>viewer</code> at
   * <code>startLine</code>.
   * 
   * @param viewer the viewer
   * @param startLine the line in the viewer
   * @return the partition type at the start of <code>startLine</code>, or
   *         <code>IDocument.DEFAULT_CONTENT_TYPE</code> if none can be detected
   */
  private String getPartition(ISourceViewer viewer, int startLine) {
    if (viewer == null) {
      return null;
    }
    IDocument doc = viewer.getDocument();
    if (doc == null) {
      return null;
    }
    if (startLine <= 0) {
      return IDocument.DEFAULT_CONTENT_TYPE;
    }
    try {
      ITypedRegion region = TextUtilities.getPartition(doc, fPartitioning,
          doc.getLineOffset(startLine) - 1, true);
      return region.getType();
    } catch (BadLocationException e) {
    }
    return IDocument.DEFAULT_CONTENT_TYPE;
  }
}
