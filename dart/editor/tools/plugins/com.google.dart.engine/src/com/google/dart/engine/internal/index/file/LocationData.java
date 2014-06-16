package com.google.dart.engine.internal.index.file;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.Location;

/**
 * A container with information about a {@link Location}.
 * 
 * @coverage dart.engine.index
 */
public class LocationData {
  final int elementId;
  final int offset;
  final int length;

  public LocationData(ElementCodec elementCodec, Location location) {
    Element element = location.getElement();
    this.elementId = elementCodec.encode(element);
    this.offset = location.getOffset();
    this.length = location.getLength();
  }

  public LocationData(int elementId, int offset, int length) {
    this.elementId = elementId;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LocationData)) {
      return false;
    }
    LocationData other = (LocationData) obj;
    return other.elementId == elementId && other.offset == offset && other.length == length;
  }

  /**
   * Returns a {@link Location} that is represented by this {@link LocationData}.
   */
  public Location getLocation(AnalysisContext context, ElementCodec elementCodec) {
    Element element = elementCodec.decode(context, elementId);
    if (element == null) {
      return null;
    }
    return new Location(element, offset, length);
  }

  @Override
  public int hashCode() {
    return 31 * (31 * elementId + offset) + length;
  }
}
