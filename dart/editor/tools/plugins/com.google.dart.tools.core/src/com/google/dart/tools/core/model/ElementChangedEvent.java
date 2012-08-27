/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.model;

import java.util.EventObject;

/**
 * Instances of the class <code>ElementChangedEvent</code> describe a change to the structure or
 * contents of a tree of Dart elements. The changes to the elements are described by the associated
 * delta object carried by this event.
 */
public class ElementChangedEvent extends EventObject {
  private static final long serialVersionUID = 1L;

  /**
   * Event type constant (bit mask) indicating an after-the-fact report of creations, deletions, and
   * modifications to one or more Dart element(s) expressed as a hierarchical Dart element delta as
   * returned by <code>getDelta()</code>.
   * <p>
   * Note: this notification occurs during the corresponding POST_CHANGE resource change
   * notification, and contains a full delta accounting for any DartModel operation and/or resource
   * change.
   */
  public static final int POST_CHANGE = 1;

  /**
   * Event type constant (bit mask) indicating an after-the-fact report of creations, deletions, and
   * modifications to one or more Dart element(s) expressed as a hierarchical Dart element delta as
   * returned by <code>getDelta</code>.
   * <p>
   * Note: this notification occurs as a result of a working copy reconcile operation.
   */
  public static final int POST_RECONCILE = 4;

  /**
   * Event type indicating the nature of this event. It can be a combination either:
   * {@link #POST_CHANGE} or {@link #POST_RECONCILE}.
   */
  private int type;

  /**
   * Initialize a newly created element changed event based on the given delta.
   * 
   * @param delta the Dart element delta.
   * @param type the type of delta (ADDED, REMOVED, CHANGED) this event contains
   */
  public ElementChangedEvent(DartElementDelta delta, int type) {
    super(delta);
    this.type = type;
  }

  /**
   * Return the delta describing the change.
   * 
   * @return the delta describing the change
   */
  public DartElementDelta getDelta() {
    return (DartElementDelta) this.source;
  }

  /**
   * Return the type of event being reported.
   * 
   * @return one of the event type constants
   * @see #POST_CHANGE
   * @see #PRE_AUTO_BUILD
   * @see #POST_RECONCILE
   */
  public int getType() {
    return this.type;
  }
}
