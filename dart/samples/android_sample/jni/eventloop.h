#ifndef EVENTLOOP_H
#define EVENTLOOP_H

#include <android_native_app_glue.h>
#include "jni/activity_handler.h"
#include "jni/context.h"
#include "jni/input_handler.h"

class EventLoop {
  public:
    explicit EventLoop(android_app* application);
    void Run(ActivityHandler* activityHandler, Context* context);

  protected:
    void Activate();
    void Deactivate();
    void ProcessActivityEvent(int32_t command);
    int32_t ProcessInputEvent(AInputEvent* event);

    static void ActivityCallback(android_app* application, int32_t command);
    static int32_t InputCallback(android_app* application, AInputEvent* event);

  private:
    bool enabled_;
    bool quit_;
    ActivityHandler* activity_handler_;
    InputHandler* input_handler_;
    android_app* application_;
};

#endif

