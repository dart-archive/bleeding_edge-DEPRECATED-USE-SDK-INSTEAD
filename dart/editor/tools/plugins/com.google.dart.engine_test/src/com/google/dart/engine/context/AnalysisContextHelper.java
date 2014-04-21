package com.google.dart.engine.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

/**
 * Helper for creating and managing single {@link AnalysisContext}.
 */
public class AnalysisContextHelper {
  public final AnalysisContext context;

  /**
   * Creates new {@link AnalysisContext} using {@link AnalysisContextFactory#contextWithCore()}.
   */
  public AnalysisContextHelper() {
    context = AnalysisContextFactory.contextWithCore();
    AnalysisOptionsImpl options = new AnalysisOptionsImpl(context.getAnalysisOptions());
    options.setCacheSize(256);
    context.setAnalysisOptions(options);
  }

  public Source addSource(String path, String code) {
    Source source = new FileBasedSource(createFile(path));
    if (path.endsWith(".dart") || path.endsWith(".html")) {
      ChangeSet changeSet = new ChangeSet();
      changeSet.addedSource(source);
      context.applyChanges(changeSet);
    }
    context.setContents(source, code);
    return source;
  }

  public CompilationUnitElement getDefiningUnitElement(Source source) throws Exception {
    return context.getCompilationUnitElement(source, source);
  }

  public CompilationUnit resolveDefiningUnit(Source source) throws Exception {
    LibraryElement libraryElement = context.computeLibraryElement(source);
    return context.resolveCompilationUnit(source, libraryElement);
  }

  public void runTasks() {
    AnalysisResult result = context.performAnalysisTask();
    while (result.getChangeNotices() != null) {
      result = context.performAnalysisTask();
    }
  }
}
