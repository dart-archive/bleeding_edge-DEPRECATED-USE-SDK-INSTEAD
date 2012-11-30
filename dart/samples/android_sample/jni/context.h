#ifndef CONTEXT_H
#define CONTEXT_H

class Graphics;
class InputHandler;
class VMGlue;
class Timer;

struct Context {
  Graphics* graphics;
  VMGlue* vm_glue;
  InputHandler* input_handler;
  Timer* timer;
};

#endif

