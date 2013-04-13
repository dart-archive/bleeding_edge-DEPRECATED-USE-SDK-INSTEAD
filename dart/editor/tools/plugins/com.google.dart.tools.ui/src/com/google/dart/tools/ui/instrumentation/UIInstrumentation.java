package com.google.dart.tools.ui.instrumentation;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.instrumentation.InstrumentationLevel;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * The class {@code EclipseInstrumentation} wrappers and augments {@link Instrumentation} and
 * {@link InstrumentationBuilder} to provide an Eclipse specific API for logging instrumentation
 * information.
 */
public class UIInstrumentation {

  /**
   * A Eclipse specific instrumentation builder that augments the underlying builder. Instances of
   * this class do not log information, but provide convenience methods for collecting and
   * forwarding information to the underlying instrumentation builder.
   */
  private static class AugmentedInformationBuilder implements UIInstrumentationBuilder {

    /**
     * The real logger which is augmented by this class (not {@code null})
     */
    private final InstrumentationBuilder builder;

    public AugmentedInformationBuilder(InstrumentationBuilder builder) {
      this.builder = builder;
    }

    @Override
    public InstrumentationBuilder data(String name, boolean value) {
      builder.data(name, value);
      return this;
    }

    @Override
    public UIInstrumentationBuilder data(String name, long value) {
      builder.data(name, value);
      return this;
    }

    @Override
    public UIInstrumentationBuilder data(String name, String value) {
      builder.data(name, value);
      return this;
    }

    @Override
    public UIInstrumentationBuilder data(String name, String[] value) {
      builder.data(name, value);
      return this;
    }

    @Override
    public InstrumentationLevel getInstrumentationLevel() {
      return builder.getInstrumentationLevel();
    }

    @Override
    public void log() {
      builder.log();
    }

    @Override
    public InstrumentationBuilder metric(String name, boolean value) {
      builder.metric(name, value);
      return this;
    }

    @Override
    public UIInstrumentationBuilder metric(String name, long value) {
      builder.metric(name, value);
      return this;
    }

    @Override
    public UIInstrumentationBuilder metric(String name, String value) {
      builder.metric(name, value);
      return this;
    }

    @Override
    public UIInstrumentationBuilder metric(String name, String[] value) {
      builder.metric(name, value);
      return this;
    }

    @Override
    public void record(CompilationUnit cu) {

      metric("CompilationUnit-LastModified", cu.getModificationStamp());
      data("CompilationUnit-Name", cu.getElementName());
      //Try and extract the source code
      String source = null;
      try {
        metric("CompilationUnit-hasUnsavedChanges", String.valueOf(cu.hasUnsavedChanges()));

        source = cu.getSource();
      } catch (DartModelException e) {
      } //discard this, it doesn't necessarily indicate a real problem
      //we just won't be able to capture much about this compilation unit

      if (source != null) {
        metric("CompilationUnit-SourceLength", source.length());
        data("CompilationUnit-Source", source);
      }
    }

    @Override
    public void record(CompilationUnit[] cus) {
      record(cus, null);
    }

    @Override
    public void record(CompilationUnit[] cus, String collectionName) {
      if (collectionName != null) {
        metric("CompilationUnits", collectionName);
      }

      if (cus == null) {
        metric("CompilationUnits", "null");
        return;
      }

      metric("CompilationUnits-Length", cus.length);

      for (CompilationUnit cu : cus) {
        record(cu);
      }
    }

    @Override
    public void record(DartTextSelection selection) {
      if (selection == null) {
        metric("Selection", "null");
        return;
      }

      metric("Selection-Class", selection.getClass().toString());

      metric("Selection-length", selection.getLength());
      metric("Selection-startLine", selection.getStartLine());
      metric("Selection-endLine", selection.getEndLine());
      metric("Selection-offset", selection.getOffset());

      data("Selection-text", selection.getText());
    }

    @Override
    public void record(IResource[] resources) {
      if (resources == null) {
        metric("resouces", "null");
        return;
      }

      metric("resourcesLength", resources.length);

      for (IResource ir : resources) {
        data("ResourceName", ir.getName());
      }
    }

    @Override
    public void record(ISelection selection) {
      if (selection == null) {
        metric("Selection", "null");
        return;
      }

      if (selection instanceof DartTextSelection) {
        record((DartTextSelection) selection);
      } else if (selection instanceof IStructuredSelection) {
        record((IStructuredSelection) selection);
      } else if (selection instanceof ITextSelection) {
        record((ITextSelection) selection);
      } else if (selection != null) {
        metric("Selection-Class", selection.getClass().toString());
      }
    }

    @Override
    public void record(IStructuredSelection selection) {
      if (selection == null) {
        metric("Selection", "null");
        return;
      }

      metric("Selection-Class", selection.getClass().toString());

      Object firstElement = selection.getFirstElement();
      if (firstElement != null) {
        metric("Selection-FirstElement", firstElement.getClass().toString());
      }
    }

    @Override
    public void record(ITextSelection selection) {
      if (selection == null) {
        metric("Selection", "null");
        return;
      }

      metric("Selection-Class", selection.getClass().toString());

      metric("Selection-length", selection.getLength());
      metric("Selection-startLine", selection.getStartLine());
      metric("Selection-endLine", selection.getEndLine());
      metric("Selection-offset", selection.getOffset());

      data("Selection-text", selection.getText());
    }

    @Override
    public InstrumentationBuilder record(Throwable exception) {
      builder.record(exception);
      return this;
    }
  }

  /**
   * Create an Eclipse specific builder that can collect the data associated with an operation.
   * 
   * @param clazz the class performing the operation (not {@code null})
   * @return the builder that was created (not {@code null})
   */
  public static UIInstrumentationBuilder builder(Class<?> clazz) {
    return builder(clazz.getSimpleName());
  }

  /**
   * Create an Eclipse specific builder that can collect the data associated with an operation.
   * 
   * @param name the name used to uniquely identify the operation (not {@code null})
   * @return the builder that was created (not {@code null})
   */
  public static UIInstrumentationBuilder builder(String name) {
    // TODO (danrubel): Provide an Eclipse specific NULL logger
    return new AugmentedInformationBuilder(Instrumentation.builder(name));
  }

  /**
   * Return a builder that will silently ignore all data and logging requests.
   * 
   * @return the builder (not {@code null})
   */
  public static UIInstrumentationBuilder getNullBuilder() {
    // TODO (danrubel): Provide an Eclipse specific NULL logger
    return new AugmentedInformationBuilder(Instrumentation.getNullBuilder());
  }
}
