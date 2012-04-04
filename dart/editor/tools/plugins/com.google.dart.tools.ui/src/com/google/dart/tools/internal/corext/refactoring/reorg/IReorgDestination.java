package com.google.dart.tools.internal.corext.refactoring.reorg;

import org.eclipse.jface.viewers.ViewerDropAdapter;

/**
 * {@link ReorgDestinationFactory} can create concrete instances
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public interface IReorgDestination {

  public static final int LOCATION_BEFORE = ViewerDropAdapter.LOCATION_BEFORE;
  public static final int LOCATION_AFTER = ViewerDropAdapter.LOCATION_AFTER;
  public static final int LOCATION_ON = ViewerDropAdapter.LOCATION_ON;

  public Object getDestination();

  public int getLocation();
}
