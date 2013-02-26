package com.google.dart.tools.ui.actions;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import java.util.List;

/*
 * Utilities to assist with instrumenting actions
 */
public class ActionInstrumentationUtilities {

  public static void record(DartElement[] members, String collectionName,
      InstrumentationBuilder instrumentation) {
    if (members == null) {
      instrumentation.metric(collectionName, "null");
      return;
    }

    instrumentation.metric(collectionName + "-length", members.length);
    for (DartElement m : members) {
      recordElement(m, instrumentation);
    }
  }

  public static void record(DartNode node, InstrumentationBuilder instrumentation) {

    if (node == null) {
      instrumentation.metric("DartNode", "null");
    }

    instrumentation.metric("DartNode-Class", node.getClass().toString());

    com.google.dart.compiler.resolver.Element element = node.getElement();

    if (element == null) {
      instrumentation.metric("Element", "null");
    } else {
      instrumentation.metric("Element-Name", element.getName());
    }

  }

  public static void record(List<DartElement> members, String collectionName,
      InstrumentationBuilder instrumentation) {

    if (members == null) {
      instrumentation.metric(collectionName, "null");
      return;
    }

    instrumentation.metric(collectionName + "-length", members.size());
    for (DartElement m : members) {
      recordElement(m, instrumentation);
    }
  }

  public static void record(TypeMember member, InstrumentationBuilder instrumentation) {
    instrumentation.data("TypeMember-Name", member.getElementName());
  }

  public static void record(TypeMember[] members, String collectionName,
      InstrumentationBuilder instrumentation) {
    if (members == null) {
      instrumentation.metric(collectionName, "null");
      return;
    }

    instrumentation.metric(collectionName + "-length", members.length);
    for (TypeMember m : members) {
      record(m, instrumentation);
    }
  }

  public static void recordCompilationUnit(CompilationUnit cu,
      InstrumentationBuilder instrumentation) {

    instrumentation.metric("CompilationUnit-LastModified", cu.getModificationStamp());
    instrumentation.data("CompilationUnit-Name", cu.getElementName());

    //TODO(lukechurch): Wrap this inside an async generator block
    /*
    //Try and extract the source code
    String source = null;
    try {
      instrumentation.metric(
          "CompilationUnit-hasUnsavedChanges",
          String.valueOf(cu.hasUnsavedChanges()));

      source = cu.getSource();
    } catch (DartModelException e) {
    } //discard this, it doesn't necessarily indicate a real problem
    //we just won't be able to capture much about this compilation unit

    if (source != null) {
      instrumentation.metric("CompilationUnit-SourceLength", source.length());
      instrumentation.data("CompilationUnit-Source", source);
    }*/
  }

  public static void recordElement(DartElement element, InstrumentationBuilder instrumentation) {

    if (element == null) {
      instrumentation.metric("Element", "null");
      return;
    }

    instrumentation.metric("Element-Class", element.getClass().toString());

    instrumentation.data("Element-Name", element.getElementName());

  }

  public static void recordElement(Element element, InstrumentationBuilder instrumentation) {

    if (element == null) {
      instrumentation.metric("Element", "null");
      return;
    }

    instrumentation.metric("Element-Class", element.getClass().toString());

    instrumentation.data("Element-Name", element.getName());

  }

  /**
   * Appropriately record the information in an exception Use this method for exceptions that aren't
   * passed to the Eclipse Exception Handler
   * 
   * @param e
   * @param instrumentation
   */
  public static void recordException(Throwable e, InstrumentationBuilder instrumentation) {
    instrumentation.metric("Problem-Exception Thrown", e.getClass().toString());

    instrumentation.data("Problem-Exception Message", e.getMessage());
    instrumentation.data("Problem-Exception StackTrace", e.getStackTrace().toString());

  }

  public static void recordLibrary(DartLibrary library, InstrumentationBuilder instrumentation) {

    if (library == null) {
      instrumentation.metric("Library", "null");
      return;
    }

    instrumentation.data("Library-Name", library.getElementName());

  }

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
