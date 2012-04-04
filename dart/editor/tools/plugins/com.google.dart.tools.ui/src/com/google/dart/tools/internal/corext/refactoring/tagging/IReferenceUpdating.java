package com.google.dart.tools.internal.corext.refactoring.tagging;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public interface IReferenceUpdating {

  /**
   * Asks the refactoring object whether references should be updated.
   * 
   * @return <code>true</code> iff reference updating is enabled
   */
  boolean getUpdateReferences();

  /**
   * Informs the refactoring object whether references should be updated.
   * 
   * @param update <code>true</code> to enable reference updating
   */
  void setUpdateReferences(boolean update);

}
