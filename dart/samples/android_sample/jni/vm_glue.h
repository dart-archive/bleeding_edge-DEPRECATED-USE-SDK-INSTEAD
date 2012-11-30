#ifndef VM_GLUE_H
#define VM_GLUE_H

#include <android_native_app_glue.h>
#include "bin/dartutils.h"
#include "include/dart_api.h"
#include "jni/graphics.h"

class VMGlue {
 public:
  explicit VMGlue(Graphics* graphics);

  int InitializeVM();
  int StartMainIsolate();
  int CallSetup();
  int CallUpdate();
  int OnMotionEvent(const char* function, int64_t when,
                    float move_x, float move_y);
  int OnKeyEvent(const char* function, int64_t when, int32_t flags,
             int32_t key_code, int32_t meta_state, int32_t repeat);
  void FinishMainIsolate();

 private:
  int Invoke(const char *function, int argc, Dart_Handle* args);

  static int ErrorExit(const char* format, ...);
  static Dart_Handle SetupRuntimeOptions(CommandLineOptions* options,
                                         const char* executable_name,
                                         const char* script_name);
  static bool CreateIsolateAndSetupHelper(const char* script_uri,
                                          const char* main,
                                          void* data,
                                          char** error);
  static bool CreateIsolateAndSetup(const char* script_uri,
                                    const char* main,
                                    void* data, char** error);
  static void ShutdownIsolate(void* callback_data);

  Graphics* graphics_;
  Dart_Isolate isolate_;
  bool initialized_vm_;
  bool initialized_script_;
};

#endif

