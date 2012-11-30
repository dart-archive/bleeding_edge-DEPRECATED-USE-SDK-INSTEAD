#ifndef DART_HOST_H
#define DART_HOST_H

#include <android_native_app_glue.h>
#include "bin/dartutils.h"
#include "include/dart_api.h"
#include "jni/activity_handler.h"
#include "jni/context.h"
#include "jni/graphics.h"
#include "jni/input_service.h"
#include "jni/timer.h"
#include "jni/vm_glue.h"

// Currently the life cycle management is very crude. We conservatively
// shutdown the main isolate when we lose focus and create a new one when
// we resume. This needs to be improved later when we understand this better,
// and we need some hooks to tell the Dart script to save/restore state
// (and an API that will support that).

class DartHost : public ActivityHandler {
 public:
  explicit DartHost(Context* context);
  virtual ~DartHost();

 protected:
  int32_t OnActivate();
  void OnDeactivate();
  int32_t OnStep();

  void OnStart();
  void OnResume();
  void OnPause();
  void OnStop();
  void OnDestroy();

  void OnSaveState(void** data, size_t size);
  void OnConfigurationChanged();
  void OnLowMemory();
  void OnCreateWindow();
  void OnDestroyWindow();
  void OnGainedFocus();
  void OnLostFocus();

 private:
  void Clear();
  int32_t Activate();
  void Deactivate();

  ANativeWindow_Buffer window_buffer_;
  InputHandler* input_handler_;
  Timer* timer_;
  Graphics* graphics_;
  VMGlue* vm_glue_;
  bool active_;
};

#endif

