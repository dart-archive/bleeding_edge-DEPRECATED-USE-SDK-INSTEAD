package com.google.dart.tools.core.mock;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;

public class MockResourceChangeEvent implements IResourceChangeEvent {

  private final MockDelta delta;
  private final int type;

  public MockResourceChangeEvent(MockDelta delta) {
    this(delta, IResourceChangeEvent.POST_CHANGE);
  }

  public MockResourceChangeEvent(MockDelta delta, int type) {
    this.delta = delta;
    this.type = type;
  }

  @Override
  public IMarkerDelta[] findMarkerDeltas(String type, boolean includeSubtypes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getBuildKind() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IResourceDelta getDelta() {
    return delta;
  }

  @Override
  public IResource getResource() {
    return delta.getResource();
  }

  @Override
  public Object getSource() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getType() {
    return type;
  }
}
