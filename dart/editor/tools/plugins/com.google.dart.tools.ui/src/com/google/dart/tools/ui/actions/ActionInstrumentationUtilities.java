package com.google.dart.tools.ui.actions;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.TypeMember;

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

  public static void record(Exception exception, InstrumentationBuilder instrumentation) {
    instrumentation.metric("Problem-Class", exception.getClass().toString());
    instrumentation.data("Problem-Exception", exception.toString());
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

}
