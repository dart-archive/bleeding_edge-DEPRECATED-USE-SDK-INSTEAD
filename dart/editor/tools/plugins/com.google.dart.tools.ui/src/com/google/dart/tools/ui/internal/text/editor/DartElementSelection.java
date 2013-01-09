package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DartElementSelection extends DartTextSelection implements IStructuredSelection {

  public DartElementSelection(DartEditor editor, DartElement element, IDocument document,
      int offset, int length) {
    super(editor, element, document, offset, length);
  }

  @Override
  public DartElement getFirstElement() {
    DartElement[] elements = toArray();
    if (elements.length > 0) {
      return elements[0];
    }
    return null;
  }

  @Override
  public Iterator<?> iterator() {
    return toList().iterator();
  }

  @Override
  public int size() {
    return toArray().length;
  }

  @Override
  public DartElement[] toArray() {
    try {
      DartElement[] nodes = resolveElementAtOffset();
      if (nodes == null) {
        // TODO(scheglov): Delete this test if not needed.
        nodes = new DartElement[0];
      }
      return nodes;
    } catch (DartModelException ex) {
      return new DartElement[0];
    }
  }

  @Override
  public List<?> toList() {
    return Arrays.asList(toArray());
  }

}
