#ifndef TYPES_H
#define TYPES_H

struct Location {
  Location() : pos_x_(0), pos_y_(0) {
  };
  void setPosition(float pos_x, float pos_y) {
    pos_x_ = pos_x;
    pos_y_ = pos_y;
  }
  void translate(float amount_x, float amount_y) {
    pos_x_ += amount_x;
    pos_y_ += amount_y;
  }

  float pos_x_, pos_y_;
};
#endif

