#include "jni/dart_host.h"

#include <math.h>
#include <unistd.h>

#include "bin/eventhandler.h"
#include "bin/isolate_data.h"
#include "bin/log.h"
#include "bin/platform.h"
#include "bin/process.h"
#include "vm/flags.h"

DartHost::DartHost(Context *context)
    : input_handler_(context->input_handler),
      timer_(context->timer),
      graphics_(context->graphics),
      vm_glue_(context->vm_glue),
      active_(false) {
  Log::Print("Creating DartHost");
}

DartHost::~DartHost() {
  Log::Print("Freeing DartHost");
}

int32_t DartHost::OnActivate() {
  return Activate();
}

int32_t DartHost::Activate() {
  if (!active_) {
    Log::Print("Activating DartHost");
    if (graphics_->Start() != 0) {
      return -1;
    }
    if (input_handler_->Start() != 0) {
      return -1;
    }
    timer_->reset();
    Log::Print("Starting main isolate");
    int result = vm_glue_->StartMainIsolate();
    if (result != 0) {
      Log::PrintErr("startMainIsolate returned %d", result);
      return -1;
    }
    active_ = true;
    vm_glue_->CallSetup();
  }
  return 0;
}

void DartHost::OnDeactivate() {
  Deactivate();
}

void DartHost::Deactivate() {
  if (active_) {
    active_ = false;
    vm_glue_->FinishMainIsolate();
    Log::Print("Deactivating DartHost");
    graphics_->Stop();
  }
}

int32_t DartHost::OnStep() {
  timer_->update();
  vm_glue_->CallUpdate();
  if (graphics_->Update() != 0) {
    return -1;
  }
  return 0;
}

void DartHost::OnStart() {
  Log::Print("Starting DartHost");
}

void DartHost::OnResume() {
  Log::Print("Resuming DartHost");
}

void DartHost::OnPause() {
  Log::Print("Pausing DartHost");
}

void DartHost::OnStop() {
  Log::Print("Stopping DartHost");
}

void DartHost::OnDestroy() {
  Log::Print("Destroying DartHost");
}

void DartHost::OnSaveState(void** data, size_t size) {
  Log::Print("Saving DartHost state");
}

void DartHost::OnConfigurationChanged() {
  Log::Print("DartHost config changed");
}

void DartHost::OnLowMemory() {
  Log::Print("DartHost low on memory");
}

void DartHost::OnCreateWindow() {
  Log::Print("DartHost creating window");
}

void DartHost::OnDestroyWindow() {
  Log::Print("DartHost destroying window");
}

void DartHost::OnGainedFocus() {
  Log::Print("DartHost gained focus");
}

void DartHost::OnLostFocus() {
  Log::Print("DartHost lost focus");
}

void DartHost::Clear() {
  memset(window_buffer_.bits, 0,
         window_buffer_.stride * window_buffer_.height * sizeof(int32_t));
}

