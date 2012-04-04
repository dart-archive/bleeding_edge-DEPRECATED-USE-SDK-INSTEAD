package com.google.dart.tools.internal.corext.refactoring.tagging;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public interface IQualifiedNameUpdating {

  /**
   * Performs a dynamic check whether this refactoring object is capable of updating qualified names
   * in non Dart files. The return value of this method may change according to the state of the
   * refactoring.
   */
  public boolean canEnableQualifiedNameUpdating();

  public String getFilePatterns();

  /**
   * If <code>canEnableQualifiedNameUpdating</code> returns <code>true</code>, then this method is
   * used to ask the refactoring object whether references in non Dart files should be updated. This
   * call can be ignored if <code>canEnableQualifiedNameUpdating</code> returns <code>false</code>.
   */
  public boolean getUpdateQualifiedNames();

  public void setFilePatterns(String patterns);

  /**
   * If <code>canEnableQualifiedNameUpdating</code> returns <code>true</code>, then this method is
   * used to inform the refactoring object whether references in non Dart files should be updated.
   * This call can be ignored if <code>canEnableQualifiedNameUpdating</code> returns
   * <code>false</code>.
   */
  public void setUpdateQualifiedNames(boolean update);
}
