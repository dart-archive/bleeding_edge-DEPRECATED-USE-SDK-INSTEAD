package com.google.dart.tools.internal.corext.refactoring.reorg;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public interface IReorgDestinationValidator {

  /**
   * Is it possible, that destination contains valid destinations as children?
   * 
   * @param destination the destination to verify
   * @return true if destination can have valid destinations
   */
  public boolean canChildrenBeDestinations(IReorgDestination destination);

  /**
   * Is it possible, that the given kind of destination is a target for the reorg?
   * 
   * @param destination the destination to verify
   * @return true if possible
   */
  public boolean canElementBeDestination(IReorgDestination destination);
}
