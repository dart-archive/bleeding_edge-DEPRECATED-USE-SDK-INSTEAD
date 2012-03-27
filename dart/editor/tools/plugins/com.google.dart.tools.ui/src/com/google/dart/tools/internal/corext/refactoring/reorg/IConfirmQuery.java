package com.google.dart.tools.internal.corext.refactoring.reorg;

import org.eclipse.core.runtime.OperationCanceledException;

public interface IConfirmQuery {
  public boolean confirm(String question) throws OperationCanceledException;

  public boolean confirm(String question, Object[] elements) throws OperationCanceledException;
}
