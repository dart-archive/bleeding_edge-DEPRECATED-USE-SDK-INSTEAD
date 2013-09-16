/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.taginfo;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.ITemporaryAnnotation;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Hover help that displays annotations shown in text of editor. Currently, this text hover is used
 * in conjunction with AbstractTextHoverProcessor.
 * 
 * @author amywu
 */
public class AnnotationHoverProcessor extends AbstractHoverProcessor {
  private final static String LIST_BEGIN = "<ul>"; //$NON-NLS-1$
  private final static String LIST_ELEMENT = "<li>"; //$NON-NLS-1$
  private final static String PARAGRAPH_END = "</p>"; //$NON-NLS-1$
  private final static String PARAGRAPH_START = "<p>"; //$NON-NLS-1$

  private IPreferenceStore fPreferenceStore = null;

  /**
	 * 
	 */
  public AnnotationHoverProcessor() {
    super();
  }

  /**
   * Formats a msg to a proper html message
   * 
   * @param msg - assumes msg is neither null nor empty string
   * @return
   */
  private String formatMessage(String msg) {
    StringBuffer buf = new StringBuffer();
    buf.append(PARAGRAPH_START);
    buf.append(StringUtils.convertToHTMLContent(msg));
    buf.append(PARAGRAPH_END);
    return buf.toString();
  }

  /**
   * Formats multiple messages into proper html message
   * 
   * @param messages
   * @return
   */
  private String formatMessages(List messages) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(PARAGRAPH_START);
    buffer.append(SSEUIMessages.Multiple_errors); //$NON-NLS-1$
    buffer.append(LIST_BEGIN);

    Iterator e = messages.iterator();
    while (e.hasNext()) {
      buffer.append(LIST_ELEMENT);
      buffer.append(StringUtils.convertToHTMLContent((String) e.next()));
    }
    buffer.append(PARAGRAPH_END);
    return buffer.toString();
  }

  /**
   * Returns the annotation preference for the given annotation. (copied from
   * org.eclipse.jdt.internal.ui.text.java.hover.AnnotationHover)
   * 
   * @param annotation the annotation
   * @return the annotation preference or <code>null</code> if none
   */
  private AnnotationPreference getAnnotationPreference(Annotation annotation) {

    if (annotation.isMarkedDeleted())
      return null;
    return EditorsUI.getAnnotationPreferenceLookup().getAnnotationPreference(annotation);
  }

  public String getHoverInfo(ITextViewer viewer, IRegion hoverRegion) {
    IAnnotationModel model = ((SourceViewer) viewer).getAnnotationModel();
    if (model != null) {
      List messages = new ArrayList();
      Iterator e = model.getAnnotationIterator();
      while (e.hasNext()) {
        Annotation a = (Annotation) e.next();
        if (!isAnnotationValid(a))
          continue;

        Position p = model.getPosition(a);
        // check if this is an annotation in the region we are
        // concerned with
        if (p.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
          String msg = a.getText();
          if ((msg != null) && msg.trim().length() > 0) {
            // it is possible for temporary annotations to
            // duplicate other annotations so make sure not to add
            // dups
            if (a instanceof ITemporaryAnnotation) {
              boolean duplicated = false;
              int j = 0;
              while (j < messages.size() && !duplicated) {
                duplicated = messages.get(j).equals(msg);
                ++j;
              }
              if (!duplicated) {
                messages.add(msg);
              }
            } else {
              messages.add(msg);
            }
          }
        }
      }
      if (messages.size() > 1) {
        return formatMessages(messages);
      } else if (messages.size() > 0) {
        return formatMessage(messages.get(0).toString());
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
   */
  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    IAnnotationModel model = ((SourceViewer) textViewer).getAnnotationModel();
    Region hoverRegion = null;

    if (model != null) {
      Iterator e = model.getAnnotationIterator();
      while (e.hasNext()) {
        Annotation a = (Annotation) e.next();
        if (!isAnnotationValid(a))
          continue;
        Position p = model.getPosition(a);
        if (p != null && p.includes(offset)) {
          // find the smallest region containing offset
          if ((hoverRegion == null) || (hoverRegion.getLength() > p.getLength())) {
            hoverRegion = new Region(p.getOffset(), p.getLength());
          }
        }
      }
    }
    return hoverRegion;
  }

  /**
   * Retreives the preference store If no preference store is currently stored, retreive the
   * appropriate preference store
   */
  private IPreferenceStore getPreferenceStore() {
    if (fPreferenceStore == null) {
      IPreferenceStore sseEditorPrefs = SSEUIPlugin.getDefault().getPreferenceStore();
      IPreferenceStore baseEditorPrefs = EditorsUI.getPreferenceStore();
      fPreferenceStore = new ChainedPreferenceStore(new IPreferenceStore[] {
          sseEditorPrefs, baseEditorPrefs});
    }
    return fPreferenceStore;
  }

  boolean isAnnotationValid(Annotation a) {
    AnnotationPreference preference = getAnnotationPreference(a);
    if (preference == null)
      return false;
    String textPreferenceKey = preference.getTextPreferenceKey();
    String highlightPreferenceKey = preference.getHighlightPreferenceKey();
    if (textPreferenceKey == null || !(getPreferenceStore().getBoolean(textPreferenceKey))
        || highlightPreferenceKey == null
        || getPreferenceStore().getBoolean(highlightPreferenceKey))
      return false;
    return true;
  }
}
