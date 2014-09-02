package com.google.dart.tools.ui.actions;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;

/*
 * Utilities to assist with instrumenting actions
 */
public class ActionInstrumentationUtilities {

  public static void record(Exception exception, InstrumentationBuilder instrumentation) {
    instrumentation.metric("Problem-Class", exception.getClass().toString());
    instrumentation.data("Problem-Exception", exception.toString());
  }

  public static void recordElement(Element element, InstrumentationBuilder instrumentation) {

    if (element == null) {
      instrumentation.metric("Element", "null");
      return;
    }

    instrumentation.metric("Element-Class", element.getClass().toString());

    instrumentation.data("Element-Name", element.getDisplayName());

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
