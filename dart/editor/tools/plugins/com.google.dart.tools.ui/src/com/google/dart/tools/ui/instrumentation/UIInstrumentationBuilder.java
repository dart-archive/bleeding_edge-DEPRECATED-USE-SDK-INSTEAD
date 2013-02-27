package com.google.dart.tools.ui.instrumentation;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Instances of {@code UIInstrumentationBuilder} are a drop in replacement for
 * {@link InstrumentationBuilder} and provide convenience methods for logging Eclipse specific
 * information. Instances do not themselves log information, but simply forward that information to
 * the {@link InstrumentationBuilder} returned by {@link Instrumentation#builder(String)}
 */
public interface UIInstrumentationBuilder extends InstrumentationBuilder {

  /**
   * Append information about the exception.
   * 
   * @param exception the exception (may be {@code null})
   */
  void record(Throwable exception);

  /**
   * Append information about the selection.
   * 
   * @param selection the selection (may be {@code null})
   */
  void record(DartTextSelection selection);

  /**
   * Append information about the selection.
   * 
   * @param selection the selection (may be {@code null})
   */
  void record(ISelection selection);

  /**
   * Append information about the selection.
   * 
   * @param selection the selection (may be {@code null})
   */
  void record(IStructuredSelection selection);

  /**
   * Append information about the selection.
   * 
   * @param selection the selection (may be {@code null})
   */
  void record(ITextSelection selection);
}
