/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.taginfo;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Provides the best hover help documentation (by using other hover help processors) Priority of
 * hover help processors is: ProblemHoverProcessor, TagInfoProcessor, AnnotationHoverProcessor
 */
public class BestMatchHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {
  /** Current best match text hover */
  private ITextHover fBestMatchHover;
  /**
   * Documentation / Information hovers
   */
  private ITextHover[] fTagInfoHovers;
  /** List of text hovers to consider in best match */
  private List fTextHovers;
  /**
   * Partition type for which to create text hovers (when deferred for performance)
   */
  private String fPartitionType;

  public BestMatchHover(ITextHover infoTagHover) {
    this(new ITextHover[] {infoTagHover});
  }

  public BestMatchHover(ITextHover[] infoTagHovers) {
    fTagInfoHovers = infoTagHovers;
  }

  public BestMatchHover(String partitionType) {
    fPartitionType = partitionType;
  }

  /**
   * Create a list of text hovers applicable to this best match hover processor
   * 
   * @return List of ITextHover - in abstract class this is empty list
   */
  private List createTextHoversList() {
    List hoverList = new ArrayList();
    // if currently debugging, then add the debug hover to the list of
    // best match
    if (Logger.isTracing(DebugInfoHoverProcessor.TRACEFILTER)) {
      hoverList.add(new DebugInfoHoverProcessor());
    }

    hoverList.add(new ProblemAnnotationHoverProcessor());

    if (fPartitionType != null && fTagInfoHovers == null) {
      List extendedTextHover = ExtendedConfigurationBuilder.getInstance().getConfigurations(
          ExtendedConfigurationBuilder.DOCUMENTATIONTEXTHOVER, fPartitionType);
      fTagInfoHovers = (ITextHover[]) extendedTextHover.toArray(new ITextHover[extendedTextHover.size()]);
    }
    if (fTagInfoHovers != null) {
      for (int i = 0; i < fTagInfoHovers.length; i++) {
        hoverList.add(fTagInfoHovers[i]);
      }
    }
    hoverList.add(new AnnotationHoverProcessor());
    return hoverList;
  }

  public IInformationControlCreator getHoverControlCreator() {
    IInformationControlCreator creator = null;

    if (fBestMatchHover instanceof ITextHoverExtension) {
      creator = ((ITextHoverExtension) fBestMatchHover).getHoverControlCreator();
    }
    return creator;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer,
   * org.eclipse.jface.text.IRegion)
   */
  public String getHoverInfo(ITextViewer viewer, IRegion hoverRegion) {
    String displayText = null;

    // already have a best match hover picked out from getHoverRegion call
    if (fBestMatchHover != null) {
      displayText = fBestMatchHover.getHoverInfo(viewer, hoverRegion);
    }
    // either had no best match hover or best match hover returned null
    if (displayText == null) {
      // go through list of text hovers and return first display string
      Iterator i = getTextHovers().iterator();
      while ((i.hasNext()) && (displayText == null)) {
        ITextHover hover = (ITextHover) i.next();
        displayText = hover.getHoverInfo(viewer, hoverRegion);
      }
    }
    return displayText;
  }

  public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
    Object information = null;

    // already have a best match hover picked out from getHoverRegion call
    if (fBestMatchHover instanceof ITextHoverExtension2) {
      information = ((ITextHoverExtension2) fBestMatchHover).getHoverInfo2(textViewer, hoverRegion);
    } else if (fBestMatchHover != null) {
      information = fBestMatchHover.getHoverInfo(textViewer, hoverRegion);
    }
    // either had no best match hover or best match hover returned null
    if (information == null) {
      // go through list of text hovers and return first display string
      Iterator i = getTextHovers().iterator();
      while ((i.hasNext()) && (information == null)) {
        ITextHover hover = (ITextHover) i.next();
        if (hover == fBestMatchHover)
          continue;
        if (hover instanceof ITextHoverExtension2) {
          information = ((ITextHoverExtension2) hover).getHoverInfo2(textViewer, hoverRegion);
        } else {
          information = hover.getHoverInfo(textViewer, hoverRegion);
        }
      }
    }
    return information;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
   */
  public IRegion getHoverRegion(ITextViewer viewer, int offset) {
    IRegion hoverRegion = null;

    // go through list of text hovers and return first hover region
    ITextHover hover = null;
    Iterator i = getTextHovers().iterator();
    while ((i.hasNext()) && (hoverRegion == null)) {
      hover = (ITextHover) i.next();
      hoverRegion = hover.getHoverRegion(viewer, offset);
    }

    // store the text hover processor that found region
    if (hoverRegion != null)
      fBestMatchHover = hover;
    else
      fBestMatchHover = null;

    return hoverRegion;
  }

  private List getTextHovers() {
    if (fTextHovers == null) {
      fTextHovers = createTextHoversList();
    }
    return fTextHovers;
  }
}
