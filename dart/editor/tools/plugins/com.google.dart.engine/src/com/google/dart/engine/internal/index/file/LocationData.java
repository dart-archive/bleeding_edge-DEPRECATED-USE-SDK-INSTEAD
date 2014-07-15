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
  final int kindId0;
  final int kindId1;
  final int offset;
  final int length;

  public LocationData(ElementCodec elementCodec, Location location) {
    Element element = location.getElement();
    this.elementId = elementCodec.encode(element);
    int[] kindIds = elementCodec.getSourceKindIds(element);
    this.kindId0 = kindIds[0];
    this.kindId1 = kindIds[1];
    this.offset = location.getOffset();
    this.length = location.getLength();
  }

  public LocationData(int elementId, int kindId0, int kindId1, int offset, int length) {
    this.elementId = elementId;
    this.kindId0 = kindId0;
    this.kindId1 = kindId1;
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
    Element element = elementCodec.decode(context, elementId, kindId0, kindId1);
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
