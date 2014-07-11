package com.google.dart.server.internal;

import com.google.dart.server.Element;
import com.google.dart.server.OverrideMember;

/**
 * A concrete implementation of {@link OverrideMember}.
 * 
 * @coverage dart.server
 */
public class OverrideMemberImpl implements OverrideMember {

  private final int offset;
  private final int length;
  private final Element superclassElement;

  public OverrideMemberImpl(int offset, int length, Element superclassElement) {
    this.offset = offset;
    this.length = length;
    this.superclassElement = superclassElement;
  }

  @java.lang.Override
  public int getLength() {
    return length;
  }

  @java.lang.Override
  public int getOffset() {
    return offset;
  }

  @java.lang.Override
  public Element getSuperclassElement() {
    return superclassElement;
  }

}
