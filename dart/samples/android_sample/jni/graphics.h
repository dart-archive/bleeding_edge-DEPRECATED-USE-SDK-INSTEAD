#ifndef GRAPHICS_H
#define GRAPHICS_H

#include <android_native_app_glue.h>
#include <EGL/egl.h>
#include "bin/log.h"
#include "jni/timer.h"

class Graphics {
  public:
    Graphics(android_app* application, Timer* timer);

    const int32_t& height();
    const int32_t& width();
    int32_t Start();
    void Stop();
    int32_t Update();

  private:
    android_app* application_;
    Timer* timer_;
    int32_t width_, height_;
    EGLDisplay display_;
    EGLSurface surface_;
    EGLContext context_;
};

#endif

