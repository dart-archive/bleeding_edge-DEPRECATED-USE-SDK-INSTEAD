package com.google.dart.tools.internal.corext.refactoring.base;

import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;

import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

/**
 * {@link DartStatusContext} that can be used to annotate a {@link RefactoringStatusEntry} with
 * detailed information about an error detected in an {@link DartElement}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public abstract class DartStatusContext extends RefactoringStatusContext {

//  private static class CompilationUnitSourceContext extends JavaStatusContext {
//    private final CompilationUnit fCUnit;
//    private SourceRange fSourceRange;
//
//    private CompilationUnitSourceContext(CompilationUnit cunit, SourceRange range) {
//      fCUnit = cunit;
//      fSourceRange = range;
//      if (fSourceRange == null) {
//        fSourceRange = new SourceRangeImpl(0, 0);
//      }
//    }
//
//    @Override
//    public CompilationUnit getCompilationUnit() {
//      return fCUnit;
//    }
//
//    @Override
//    public SourceRange getSourceRange() {
//      return fSourceRange;
//    }
//
//    @Override
//    public String toString() {
//      return getSourceRange() + " in " + super.toString(); //$NON-NLS-1$
//    }
//  }

//  private static class ImportDeclarationSourceContext extends JavaStatusContext {
//    private final IImportDeclaration fImportDeclartion;
//
//    private ImportDeclarationSourceContext(IImportDeclaration declaration) {
//      fImportDeclartion = declaration;
//    }
//
//    @Override
//    public IClassFile getClassFile() {
//      return null;
//    }
//
//    @Override
//    public CompilationUnit getCompilationUnit() {
//      return (CompilationUnit) fImportDeclartion.getParent().getParent();
//    }
//
//    @Override
//    public SourceRange getSourceRange() {
//      try {
//        return fImportDeclartion.getSourceRange();
//      } catch (JavaModelException e) {
//        return new SourceRange(0, 0);
//      }
//    }
//
//    @Override
//    public boolean isBinary() {
//      return false;
//    }
//  }

  private static class DartElementSourceContext extends DartStatusContext {
    private final CompilationUnitElement element;

    private DartElementSourceContext(CompilationUnitElement element) {
      this.element = element;
    }

    @Override
    public CompilationUnit getCompilationUnit() {
      return element.getCompilationUnit();
    }

    @Override
    public SourceRange getSourceRange() {
      if (element instanceof SourceReference) {
        try {
          return ((SourceReference) element).getSourceRange();
        } catch (DartModelException e) {
        }
      }
      return new SourceRangeImpl(0, 0);
    }
  }

//  /**
//   * Creates an status entry context for the given import declaration
//   * 
//   * @param declaration the import declaration for which the context is supposed to be created
//   * @return the status entry context or <code>null</code> if the context cannot be created
//   */
//  public static RefactoringStatusContext create(IImportDeclaration declaration) {
//    if (declaration == null || !declaration.exists()) {
//      return null;
//    }
//    return new ImportDeclarationSourceContext(declaration);
//  }

  /**
   * @return the {@link RefactoringStatusContext} for given {@link CompilationUnitElement}, may be
   *         <code>null</code> if the context cannot be created.
   */
  public static RefactoringStatusContext create(CompilationUnitElement element) {
    if (element == null || !element.exists()) {
      return null;
    }
    return new DartElementSourceContext(element);
  }

//  /**
//   * Creates an status entry context for the given method binding
//   * 
//   * @param method the method binding for which the context is supposed to be created
//   * @return the status entry context or <code>Context.NULL_CONTEXT</code> if the context cannot be
//   *         created
//   */
//  public static RefactoringStatusContext create(IMethodBinding method) {
//    return create((IMethod) method.getJavaElement());
//  }
//
//  /**
//   * Creates an status entry context for the given type root.
//   * 
//   * @param typeRoot the type root containing the error
//   * @return the status entry context or <code>Context.NULL_CONTEXT</code> if the context cannot be
//   *         created
//   */
//  public static RefactoringStatusContext create(ITypeRoot typeRoot) {
//    return create(typeRoot, (SourceRange) null);
//  }
//
//  /**
//   * Creates an status entry context for the given type root and AST node.
//   * 
//   * @param typeRoot the type root containing the error
//   * @param node an astNode denoting the source range that has caused the error
//   * @return the status entry context or <code>Context.NULL_CONTEXT</code> if the context cannot be
//   *         created
//   */
//  public static RefactoringStatusContext create(ITypeRoot typeRoot, ASTNode node) {
//    SourceRange range = null;
//    if (node != null) {
//      range = new SourceRange(node.getStartPosition(), node.getLength());
//    }
//    return create(typeRoot, range);
//  }
//
//  /**
//   * Creates an status entry context for the given type root and source range.
//   * 
//   * @param typeRoot the type root containing the error
//   * @param range the source range that has caused the error or <code>null</code> if the source
//   *          range is unknown
//   * @return the status entry context or <code>null</code> if the context cannot be created
//   */
//  public static RefactoringStatusContext create(ITypeRoot typeRoot, SourceRange range) {
//    if (typeRoot instanceof CompilationUnit) {
//      return new CompilationUnitSourceContext((CompilationUnit) typeRoot, range);
//    } else if (typeRoot instanceof IClassFile) {
//      return new ClassFileSourceContext((IClassFile) typeRoot, range);
//    } else {
//      return null;
//    }
//  }
//
//  /**
//   * Creates an status entry context for the given type root and selection.
//   * 
//   * @param typeRoot the type root containing the error
//   * @param selection a selection denoting the source range that has caused the error
//   * @return the status entry context or <code>Context.NULL_CONTEXT</code> if the context cannot be
//   *         created
//   */
//  public static RefactoringStatusContext create(ITypeRoot typeRoot, Selection selection) {
//    SourceRange range = null;
//    if (selection != null) {
//      range = new SourceRange(selection.getOffset(), selection.getLength());
//    }
//    return create(typeRoot, range);
//  }

  /**
   * Returns the compilation unit this context is working on. Returns <code>null</code> if the
   * context is a binary context.
   * 
   * @return the compilation unit
   */
  public abstract CompilationUnit getCompilationUnit();

  @Override
  public Object getCorrespondingElement() {
    return getCompilationUnit();
  }

  /**
   * Returns the source range associated with this element.
   * 
   * @return the source range
   */
  public abstract SourceRange getSourceRange();
}
