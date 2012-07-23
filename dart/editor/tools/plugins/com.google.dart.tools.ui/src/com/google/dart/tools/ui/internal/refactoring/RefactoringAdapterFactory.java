package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.core.internal.refactoring.util.MultiStateCompilationUnitChange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.ui.refactoring.TextEditChangeNode;

public class RefactoringAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = new Class[] {TextEditChangeNode.class};

  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(Object object, Class key) {
    if (!TextEditChangeNode.class.equals(key)) {
      return null;
    }
    if (!(object instanceof CompilationUnitChange)
        && !(object instanceof MultiStateCompilationUnitChange)) {
      return null;
    }
    return new CompilationUnitChangeNode((TextEditBasedChange) object);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }
}
