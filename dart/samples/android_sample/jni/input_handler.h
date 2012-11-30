#ifndef INPUTHANDLER_H
#define INPUTHANDLER_H

#include <android/input.h>

class InputHandler {
  public:
    virtual int32_t Start() {}
    virtual bool OnTouchEvent(AInputEvent* event) = 0;
    virtual bool OnKeyEvent(AInputEvent* event) = 0;
    virtual ~InputHandler() {}
};

#endif

