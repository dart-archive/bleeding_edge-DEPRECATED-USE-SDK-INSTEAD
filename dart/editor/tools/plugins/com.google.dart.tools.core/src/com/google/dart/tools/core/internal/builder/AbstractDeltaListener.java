package com.google.dart.tools.core.internal.builder;

/**
 * Abstract {@link DeltaListener} to be subclassed for convenience.
 */
public class AbstractDeltaListener implements DeltaListener {

  @Override
  public void packageSourceAdded(SourceDeltaEvent event) {
    // ignored
  }

  @Override
  public void packageSourceChanged(SourceDeltaEvent event) {
    // ignored
  }

  @Override
  public void packageSourceContainerRemoved(SourceContainerDeltaEvent event) {
    // ignored
  }

  @Override
  public void packageSourceRemoved(SourceDeltaEvent event) {
    // ignored
  }

  @Override
  public void pubspecAdded(ResourceDeltaEvent event) {
    // ignored
  }

  @Override
  public void pubspecChanged(ResourceDeltaEvent event) {
    // ignored
  }

  @Override
  public void pubspecRemoved(ResourceDeltaEvent event) {
    // ignored
  }

  @Override
  public void sourceAdded(SourceDeltaEvent event) {
    // ignored
  }

  @Override
  public void sourceChanged(SourceDeltaEvent event) {
    // ignored
  }

  @Override
  public void sourceContainerRemoved(SourceContainerDeltaEvent event) {
    // ignored
  }

  @Override
  public void sourceRemoved(SourceDeltaEvent event) {
    // ignored
  }
}
