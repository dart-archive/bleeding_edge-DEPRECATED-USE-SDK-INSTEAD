package com.google.dart.tools.wst.ui;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.ui.internal.text.dart.DartPriorityFileEditor;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;

/**
 * This {@link IAdapterFactory} adapts {@link StructuredTextEditor} to
 * {@link DartPriorityFileEditor}.
 */
public class StructuredTextEditorToPriorityFileAdapterFactory implements IAdapterFactory {

  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adaptableObject instanceof StructuredTextEditor
        && DartPriorityFileEditor.class.equals(adapterType)) {
      StructuredTextEditor textEditor = (StructuredTextEditor) adaptableObject;
      final StructuredTextViewer textViewer = textEditor.getTextViewer();
      final StyledText textWidget = textViewer.getTextWidget();
      final IDocument document = textViewer.getDocument();
      final StructuredDocumentDartInfo documentInfo = StructuredDocumentDartInfo.create(document);
      if (documentInfo == null) {
        return null;
      }
      return new DartPriorityFileEditor() {
        @Override
        public AnalysisContext getInputAnalysisContext() {
          return documentInfo.getContext();
        }

        @Override
        public String getInputFilePath() {
          return documentInfo.getFile().getAbsolutePath();
        }

        @Override
        public Project getInputProject() {
          return documentInfo.getProject();
        }

        @Override
        public Source getInputSource() {
          return documentInfo.getSource();
        }

        @Override
        public boolean isVisible() {
          return textWidget.isVisible();
        }
      };
    }
    return null;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Class[] getAdapterList() {
    return new Class[] {DartPriorityFileEditor.class};
  }
}
