#ifndef INPUTSERVICE_H
#define INPUTSERVICE_H

#include <android_native_app_glue.h>
#include "jni/input_handler.h"
#include "jni/types.h"
#include "jni/vm_glue.h"

class InputService : public InputHandler {
  public:
    InputService(android_app* application,
                 VMGlue* vm_glue,
                 const int32_t& width,
                 const int32_t& height);

  public:
    int32_t Start();
    bool OnTouchEvent(AInputEvent* event);
    bool OnKeyEvent(AInputEvent* event);

  private:
    android_app* application_;
    VMGlue* vm_glue_;
    const int32_t& width_, &height_;
};

#endif

