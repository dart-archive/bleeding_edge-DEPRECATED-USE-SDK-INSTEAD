package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.dartdoc.DartDocUtilities;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;

import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of ITextHover for Dart documentation. Other ITextHover implementations can
 * register themselves using {@link #addContributer(ITextHover)} and they will be invoked before the
 * default dart doc hover tool tip.
 */
public class DartTextHover extends DefaultTextHover {
  private CompilationUnitEditor editor;
  private DartSourceViewerConfiguration sourceViewerConfiguration;

  private static List<ITextHover> hoverContributors = new ArrayList<ITextHover>();

  /**
   * Register a ITextHover tooltip contributor
   * 
   * @param hoverContributor
   */
  public static void addContributer(ITextHover hoverContributor) {
    hoverContributors.add(hoverContributor);
  }

  /**
   * Remove a hover contributor
   * 
   * @param hoverContributor
   */
  public static void removeContributer(ITextHover hoverContributor) {
    hoverContributors.remove(hoverContributor);
  }

  public DartTextHover(ITextEditor editor, ISourceViewer sourceViewer,
      DartSourceViewerConfiguration sourceViewerConfiguration) {
    super(sourceViewer);

    if (editor instanceof CompilationUnitEditor) {
      this.editor = (CompilationUnitEditor) editor;
    }

    this.sourceViewerConfiguration = sourceViewerConfiguration;
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getHoverInfo(ITextViewer textViewer, IRegion region) {
    String annotationHover = super.getHoverInfo(textViewer, region);

    if (annotationHover != null) {
      return annotationHover;
    }

    if (editor == null) {
      return null;
    }

    if (hoverContributors.size() > 0) {
      // return the first non null string from the contributors
      for (ITextHover hoverContributer : hoverContributors) {
        String hoverText = hoverContributer.getHoverInfo(textViewer, region);
        if (hoverText != null) {
          return hoverText;
        }
      }
    }

    DartUnit unit = editor.getAST();
    if (unit == null) {
      return null;
    }

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
