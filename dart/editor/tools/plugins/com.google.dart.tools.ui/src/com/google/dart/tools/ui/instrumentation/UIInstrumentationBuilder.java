package com.google.dart.tools.ui.instrumentation;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.core.resources.IResource;
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
   * Append information about the compilation unit.
   * 
   * @param cu the compilation unit (may be {@code null})
   */
  void record(CompilationUnit cu);

  /**
   * Append information about the compilation units.
   * 
   * @param cus the compilation units (may be {@code null})
   */
  void record(CompilationUnit[] cus);

  /**
   * Append information about the compilation units.
   * 
   * @param cus the compilation units (may be {@code null})
   * @param collectionName the name of the collection of compilation units (may be {@code null})
   */
  void record(CompilationUnit[] cus, String collectionName);

  /**
   * Append information about the selection.
   * 
   * @param selection the selection (may be {@code null})
   */
  void record(DartTextSelection selection);

  /**
   * Append information about the resources.
   * 
   * @param resources the resources to instrument (may be {@code null})
   */
  void record(IResource[] resources);

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
