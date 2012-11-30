#ifndef TIMER_H
#define TIMER_H

#include <time.h>

class Timer {
 public:
  Timer();
  void reset();
  void update();
  double now();
  float elapsed();

 private:
  float elapsed_;
  double last_time_;
};

#endif

