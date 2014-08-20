package com.google.dart.tools.ui.instrumentation;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Instances of {@code UIInstrumentationBuilder} are a drop in replacement for
 * {@link InstrumentationBuilder} and provide convenience methods for logging Eclipse specific
 * information. Instances do not themselves log information, but simply forward that information to
 * the {@link InstrumentationBuilder} returned by {@link Instrumentation#builder(String)}
 */
public interface UIInstrumentationBuilder extends InstrumentationBuilder {

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

  /**
   * Append information about the workbench page
   * 
   * @param page the page (may be {@code null})
   */
  void record(IWorkbenchPage page);

  /**
   * Append information about the workbench part
   * 
   * @param part the part (may be {@code null})
   */
  void record(IWorkbenchPartReference part);

  /**
   * Append information about the workbench window
   * 
   * @param window the window (may be {@code null})
   */
  void record(IWorkbenchWindow window);

}
