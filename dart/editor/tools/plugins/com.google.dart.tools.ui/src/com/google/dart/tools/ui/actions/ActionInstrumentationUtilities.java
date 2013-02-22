package com.google.dart.tools.ui.actions;

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/*
 * Utilities to assist with instrumenting actions
 */
public class ActionInstrumentationUtilities {

  public static void RecordSelection(DartTextSelection selection,
      InstrumentationBuilder instrumentation) {

    instrumentation.metric("Selection-Class", selection.getClass().toString());

    instrumentation.metric("Selection-length", selection.getLength());
    instrumentation.metric("Selection-startLine", selection.getStartLine());
    instrumentation.metric("Selection-endLine", selection.getEndLine());
    instrumentation.metric("Selection-offset", selection.getOffset());

    instrumentation.data("Selection-text", selection.getText());

  }

  public static void RecordSelection(ISelection selection, InstrumentationBuilder instrumentation) {

    instrumentation.metric("Selection-Class", selection.getClass().toString());

    if (selection instanceof DartTextSelection) {
      RecordSelection((DartTextSelection) selection, instrumentation);
    } else if (selection instanceof IStructuredSelection) {
      RecordSelection((IStructuredSelection) selection, instrumentation);
    } else if (selection instanceof ITextSelection) {
      RecordSelection((ITextSelection) selection, instrumentation);
    }
  }

  public static void RecordSelection(IStructuredSelection selection,
      InstrumentationBuilder instrumentation) {

    instrumentation.metric("Selection-Class", selection.getClass().toString());

    Object firstElement = selection.getFirstElement();
    if (firstElement != null) {
      instrumentation.metric("Selection-FirstElement", firstElement.getClass().toString());
    }

  }

  public static void RecordSelection(ITextSelection selection,
      InstrumentationBuilder instrumentation) {

    instrumentation.metric("Selection-Class", selection.getClass().toString());

    instrumentation.metric("Selection-length", selection.getLength());
    instrumentation.metric("Selection-startLine", selection.getStartLine());
    instrumentation.metric("Selection-endLine", selection.getEndLine());
    instrumentation.metric("Selection-offset", selection.getOffset());

    instrumentation.data("Selection-text", selection.getText());

  }

}
