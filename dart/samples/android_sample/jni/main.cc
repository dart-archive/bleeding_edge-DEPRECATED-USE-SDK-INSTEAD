#include "jni/context.h"
#include "jni/dart_host.h"
#include "jni/eventloop.h"
#include "jni/graphics.h"
#include "jni/input_service.h"
#include "jni/vm_glue.h"

SoundService* psound_service;

void android_main(android_app* application) {
  Timer timer;
  Graphics graphics(application, &timer);
  VMGlue vmGlue(&graphics);
  InputService inputService(application, &vmGlue,
      graphics.width(), graphics.height());
  SoundService sound_service(application);
  psound_service = &sound_service;
  Context context;
  context = { &graphics, &inputService, psound_service, &timer, &vmGlue };
  EventLoop eventLoop(application);
  DartHost host(&context);
  eventLoop.Run(&host, &context);
}

void PlayBackground(const char* path) {
  psound_service->PlayBackground(path);
}

void StopBackground() {
  psound_service->StopBackground();
}
