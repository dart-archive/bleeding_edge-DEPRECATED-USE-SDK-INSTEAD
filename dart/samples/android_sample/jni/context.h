#ifndef CONTEXT_H
#define CONTEXT_H

class Graphics;
class InputHandler;
class SoundService;
class Timer;
class VMGlue;

struct Context {
  Graphics* graphics;
  InputHandler* input_handler;
  SoundService* sound_service;
  Timer* timer;
  VMGlue* vm_glue;
};

#endif

