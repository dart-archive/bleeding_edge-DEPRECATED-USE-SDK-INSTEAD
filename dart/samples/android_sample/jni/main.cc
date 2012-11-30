#include "jni/context.h"
#include "jni/dart_host.h"
#include "jni/eventloop.h"
#include "jni/graphics.h"
#include "jni/input_service.h"
#include "jni/vm_glue.h"

void android_main(android_app* application) {
  Timer timer;
  Graphics graphics(application, &timer);
  VMGlue vmGlue(&graphics);
  InputService inputService(application, &vmGlue,
      graphics.width(), graphics.height());
  Context context = { &graphics, &vmGlue, &inputService, &timer };
  EventLoop eventLoop(application);
  DartHost host(&context);
  eventLoop.Run(&host, &context);
}


