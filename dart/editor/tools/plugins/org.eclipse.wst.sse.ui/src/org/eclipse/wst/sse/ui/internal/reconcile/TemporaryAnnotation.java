/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.quickassist.IQuickFixableAnnotation;
import org.eclipse.jface.text.reconciler.IReconcileResult;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.jface.text.source.ImageUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.AnnotationPreferenceLookup;
import org.eclipse.wst.sse.ui.internal.ITemporaryAnnotation;

import java.util.Map;

/**
 * An implementation of ITemporaryAnnotation @
 * 
 * @author pavery
 */
public class TemporaryAnnotation extends Annotation implements ITemporaryAnnotation,
    IReconcileResult, IAnnotationPresentation, IQuickFixableAnnotation {
  // remember to change these if it changes in the extension point
  // may need a different home for them in the future, but they're here for
  // now
  public final static String ANNOT_ERROR = "org.eclipse.wst.sse.ui.temp.error"; //$NON-NLS-1$
  public final static String ANNOT_INFO = "org.eclipse.wst.sse.ui.temp.info"; //$NON-NLS-1$

  // pa_TODO what should the ID be for this?
  public final static String ANNOT_UNKNOWN = Annotation.TYPE_UNKNOWN;
  public final static String ANNOT_WARNING = "org.eclipse.wst.sse.ui.temp.warning"; //$NON-NLS-1$

  // copied from CompilationUnitDocumentProvider.ProblemAnnotation
  //XXX: To be fully correct these constants should be non-static

  private static final int INFO_LAYER;

  private static final int WARNING_LAYER;

  private static final int ERROR_LAYER;

  static {
    AnnotationPreferenceLookup lookup = EditorsUI.getAnnotationPreferenceLookup();
    INFO_LAYER = computeLayer("org.eclipse.wst.sse.ui.temp.info", lookup); //$NON-NLS-1$
    WARNING_LAYER = computeLayer("org.eclipse.wst.sse.ui.temp.warning", lookup); //$NON-NLS-1$
    ERROR_LAYER = computeLayer("org.eclipse.wst.sse.ui.temp.error", lookup); //$NON-NLS-1$
  }

  private static int computeLayer(String annotationType, AnnotationPreferenceLookup lookup) {
    Annotation annotation = new Annotation(annotationType, false, null);
    AnnotationPreference preference = lookup.getAnnotationPreference(annotation);
    if (preference != null)
      return preference.getPresentationLayer() + 1;
    else
      return IAnnotationAccessExtension.DEFAULT_LAYER + 1;
  }

  private Object fAdditionalFixInfo = null;

  private Object fKey = null;
  private Position fPosition = null;
  private Map fAttributes = null;
  private boolean fIsQuickFixable = false;
  private boolean fIsQuickFixableStateSet = false;

  private int fProblemID;

  private int fLayer = DEFAULT_LAYER;

  private Image fImage = null;

  public TemporaryAnnotation(Position p, String type, String message, ReconcileAnnotationKey key) {
    super();
    fPosition = p;
    setType(type);
    fKey = key;
    setText(message);
    initLayer();
    initImage();
  }

  public TemporaryAnnotation(Position p, String type, String message, ReconcileAnnotationKey key,
      int problemId) {
    super();
    fPosition = p;
    fKey = key;
    setType(type);
    setText(message);
    fProblemID = problemId;
    initLayer();
    initImage();
  }

  private void initLayer() {

    String type = getType();
    if (type.equals(ANNOT_ERROR)) {
      fLayer = ERROR_LAYER;
    } else if (type.equals(ANNOT_WARNING)) {
      fLayer = WARNING_LAYER;
    } else if (type.equals(ANNOT_INFO)) {
      fLayer = INFO_LAYER;
    }
  }

  private void initImage() {
    // later we can add support for quick fix images.
    String type = getType();
    if (type.equals(ANNOT_ERROR)) {
      fImage = null;
    } else if (type.equals(ANNOT_WARNING)) {
      fImage = null;
    } else if (type.equals(ANNOT_INFO)) {
      fImage = null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    // this check doesn't take into consideration that annotation
    // positions that change from a text edit before it
    // we should be checking if the annotation is still on the same line,
    // and the distance from the start of the line is the same
    if (obj instanceof TemporaryAnnotation) {

      TemporaryAnnotation ta = (TemporaryAnnotation) obj;

      boolean samePosition = ta.getPosition().equals(this.getPosition());
      boolean sameText = false;

      if (ta.getText() != null && this.getText() != null && ta.getText().equals(this.getText()))
        sameText = true;
      else if (ta.getText() == null && this.getText() == null)
        sameText = true;

      return sameText && samePosition;
    }
    return super.equals(obj);
  }

  /**
   * Additional info required to fix this problem.
   * 
   * @return an Object that contains additional info on how to fix this problem, or null if there is
   *         none
   */
  public Object getAdditionalFixInfo() {
    return fAdditionalFixInfo;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.ITemporaryAnnotation#getDescription()
   */
  public String getDescription() {
    return getText();
  }

  public Object getKey() {
    return fKey;
  }

  public Position getPosition() {
    return fPosition;
  }

  /**
   * @return Returns the problemID.
   */
  public int getProblemID() {
    return fProblemID;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.source.Annotation#isPersistent()
   */
  public boolean isPersistent() {
    return false;
  }

  /**
   * Sets additional information useful to fixing this problem.
   * 
   * @param an Object that contains additional info on how to fix this problem
   */
  public void setAdditionalFixInfo(Object info) {
    fAdditionalFixInfo = info;
    setQuickFixable(true);
  }

  public int getLayer() {
    return fLayer;
  }

  /*
   * @see Annotation#paint
   */
  public void paint(GC gc, Canvas canvas, Rectangle r) {
    //initializeImages();
    if (fImage != null)
      ImageUtilities.drawImage(fImage, gc, canvas, r, SWT.CENTER, SWT.TOP);
  }

  public String toString() {
    return "" + fPosition.getOffset() + ':' + fPosition.getLength() + ": " + getText(); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public Map getAttributes() {
    return fAttributes;
  }

  public void setAttributes(Map attributes) {
    fAttributes = attributes;
  }

  public boolean isQuickFixable() {
    return fIsQuickFixable;
  }

  public void setQuickFixable(boolean state) {
    fIsQuickFixable = state;
    fIsQuickFixableStateSet = true;
  }

  public boolean isQuickFixableStateSet() {
    return fIsQuickFixableStateSet;
  }
}
