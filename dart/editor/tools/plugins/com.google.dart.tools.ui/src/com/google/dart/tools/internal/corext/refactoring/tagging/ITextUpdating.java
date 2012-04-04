package com.google.dart.tools.internal.corext.refactoring.tagging;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public interface ITextUpdating {

  /**
   * Performs a dynamic check whether this refactoring object is capable of updating references to
   * the renamed element.
   */
  boolean canEnableTextUpdating();

  /**
   * Returns the current name of the element to be renamed.
   * 
   * @return the current name of the element to be renamed
   */
  String getCurrentElementName();

  /**
   * Returns the current qualifier of the element to be renamed.
   * 
   * @return the current qualifier of the element to be renamed
   */
  String getCurrentElementQualifier();

  /**
   * Returns the new name of the element
   * 
   * @return the new element name
   */
  String getNewElementName();

  /**
   * If <code>canEnableTextUpdating</code> returns <code>true</code>, then this method is used to
   * ask the refactoring object whether references in regular (non DartDoc) comments and string
   * literals should be updated. This call can be ignored if <code>canEnableTextUpdating</code>
   * returns <code>false</code>.
   */
  boolean getUpdateTextualMatches();

  /**
   * If <code>canEnableTextUpdating</code> returns <code>true</code>, then this method is used to
   * inform the refactoring object whether references in regular (non DartDoc) comments and string
   * literals should be updated. This call can be ignored if <code>canEnableTextUpdating</code>
   * returns <code>false</code>.
   */
  void setUpdateTextualMatches(boolean update);
}
