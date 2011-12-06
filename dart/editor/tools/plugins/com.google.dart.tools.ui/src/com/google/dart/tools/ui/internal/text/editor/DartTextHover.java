package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.dartdoc.DartDocUtilities;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;

import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * An implementation of ITextHover for Dart documentation.
 */
public class DartTextHover extends DefaultTextHover {
  private CompilationUnitEditor editor;
  private DartSourceViewerConfiguration sourceViewerConfiguration;

  public DartTextHover(ITextEditor editor, ISourceViewer sourceViewer,
      DartSourceViewerConfiguration sourceViewerConfiguration) {
    super(sourceViewer);

    if (editor instanceof CompilationUnitEditor) {
      this.editor = (CompilationUnitEditor) editor;
    }

    this.sourceViewerConfiguration = sourceViewerConfiguration;
  }

  @Override
  public String getHoverInfo(ITextViewer textViewer, IRegion region) {
    String annotationHover = super.getHoverInfo(textViewer, region);

    if (annotationHover != null) {
      return annotationHover;
    }

    if (editor == null) {
      return null;
    }

    DartUnit unit = editor.getAST();

    try {
      return DartDocUtilities.getDartDoc((CompilationUnit) editor.getInputDartElement(), unit,
          region.getOffset(), region.getOffset() + region.getLength());
    } catch (DartModelException e) {
      return null;
    }
  }

  @Override
  protected boolean isIncluded(Annotation annotation) {
    return sourceViewerConfiguration.isShownInText(annotation);
  }

}
