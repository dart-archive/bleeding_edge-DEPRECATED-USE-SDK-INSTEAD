package com.google.dart.engine.utilities.instrumentation;

public interface AsyncValue {

  /**
   * Returns a String to be logged This would typically be used with an anonymous implementation
   * closing over some variables with an expensive operation to be performed in the background
   * 
   * @return The data to be logged
   */
  public String compute();

}
