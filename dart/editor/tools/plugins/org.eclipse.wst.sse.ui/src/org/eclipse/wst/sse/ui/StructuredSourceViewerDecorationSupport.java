/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui;

import org.eclipse.jface.text.source.AnnotationPainter;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

class StructuredSourceViewerDecorationSupport extends SourceViewerDecorationSupport {
  public StructuredSourceViewerDecorationSupport(ISourceViewer sourceViewer,
      IOverviewRuler overviewRuler, IAnnotationAccess annotationAccess,
      ISharedTextColors sharedTextColors) {
    super(sourceViewer, overviewRuler, annotationAccess, sharedTextColors);
  }

  protected AnnotationPainter createAnnotationPainter() {
    /*
     * The new squiggly drawer depends on the presentation reconciler to draw its squiggles.
     * Unfortunately, StructuredTextEditors cannot use the presentation reconciler because it
     * conflicts with its highlighter. Overriding createAnnotationPainter so that it is forced to
     * use the old squiggly painter instead of the new one. See
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=201928
     */
    AnnotationPainter painter = super.createAnnotationPainter();
    // dont use new squiggly painter
    painter.addTextStyleStrategy(AnnotationPreference.STYLE_SQUIGGLES, null);
    // use old one
    painter.addDrawingStrategy(AnnotationPreference.STYLE_SQUIGGLES,
        new AnnotationPainter.SquigglesStrategy());
    // dont use new problem underline painter
    painter.addTextStyleStrategy(AnnotationPreference.STYLE_PROBLEM_UNDERLINE, null);
    // use old one
    painter.addDrawingStrategy(AnnotationPreference.STYLE_PROBLEM_UNDERLINE,
        new AnnotationPainter.SquigglesStrategy());
    return painter;
  }
}
