/**
 * Instances of the class [Point] represent a point in a two-dimensional Euclidean space. Points are
 * immutable.
 */

part of Geometry;

class Point {
  /**
   * The x-coordinate of the point.
   */
  final int x;

  /**
   * The y-coordinate of the point.
   */
  final int y;

  /**
   * Initialize a newly created point to have the given coordinates.
   */
  Point(this.x, this.y) {}
}
