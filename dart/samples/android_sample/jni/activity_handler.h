#ifndef ACTIVITY_HANDLER_H
#define ACTIVITY_HANDLER_H

class ActivityHandler {
  public:
    virtual int32_t OnActivate() = 0;
    virtual void OnDeactivate() = 0;
    virtual int32_t OnStep() = 0;
    virtual void OnStart() {}
    virtual void OnResume() {}
    virtual void OnPause() {}
    virtual void OnStop() {}
    virtual void OnDestroy() {}
    virtual void OnSaveState(void** data, size_t* size) {}
    virtual void OnConfigurationChanged() {}
    virtual void OnLowMemory() {}
    virtual void OnCreateWindow() {}
    virtual void OnDestroyWindow() {}
    virtual void OnGainedFocus() {}
    virtual void OnLostFocus() {}
    virtual ~ActivityHandler() {}
};

#endif

