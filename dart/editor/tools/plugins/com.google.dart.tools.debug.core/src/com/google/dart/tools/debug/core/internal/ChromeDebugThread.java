/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.core.internal;


/**
 * The IThread implementation for the Chrome debug elements.
 */
public class ChromeDebugThread {
//  extends ChromeDebugElement implements IThread {
//
//  /**
//   * The thread state implementation for when the VM is running.
//   */
//  private class RunningThreadState implements ThreadState {
//    //private final ResumeReason resumeReason;
//
//    RunningThreadState() {//ResumeReason resumeReason) {
//      //this.resumeReason = resumeReason;
//    }
//
//    @Override
//    public boolean canStep() {
//      return false;
//    }
//
//    @Override
//    public boolean canSuspend() {
//      return true;
//    }
//
//    @Override
//    public void dismiss() {
//    }
//
//    @Override
//    public IBreakpoint[] getBreakpoints() {
//      return EMPTY_BREAKPOINTS;
//    }
//
//    @Override
//    public IStackFrame[] getStackFrames() {
//      return EMPTY_FRAMES;
//    }
//
//    @Override
//    public boolean isStepping() {
//      return false;
//    }
//
//    @Override
//    public boolean isSuspended() {
//      return false;
//    }
//
//    @Override
//    public void resume() {
//      // Ignore.
//    }
//
//    @Override
//    public void step(StepAction stepAction, int resumeReason) {
//      // Ignore.
//    }
//
//    @Override
//    public void suspend() {
//      expectedSuspendReason = DebugEvent.CLIENT_REQUEST;
//      getChromeDebugTarget().getJavascriptVm().suspend(null);
//    }
//  }
//
//  /**
//   * The thread state implementation for when the VM is suspended.
//   */
//  private class SuspendedThreadState implements ThreadState {
//    private DebugContext context;
//    private ChromeDebugStackFrame[] stackFrames;
//
//    public SuspendedThreadState(DebugContext context) {
//      this.context = context;
//    }
//
//    @Override
//    public boolean canStep() {
//      return true;
//    }
//
//    @Override
//    public boolean canSuspend() {
//      return false;
//    }
//
//    @Override
//    public void dismiss() {
//
//    }
//
//    @Override
//    public IBreakpoint[] getBreakpoints() {
//      return suspendBreakpoints;
//    }
//
//    @Override
//    public IStackFrame[] getStackFrames() {
//      if (stackFrames == null) {
//        stackFrames = createStackFrames();
//      }
//
//      return stackFrames;
//    }
//
//    @Override
//    public boolean isStepping() {
//      return false;
//    }
//
//    @Override
//    public boolean isSuspended() {
//      return true;
//    }
//
//    @Override
//    public void resume() {
//      expectedSuspendReason = DebugEvent.CLIENT_REQUEST;
//
//      context.continueVm(StepAction.CONTINUE, 0, new DebugContext.ContinueCallback() {
//        @Override
//        public void failure(String errorMessage) {
//          getChromeDebugTarget().reportError("Error Resuming VM", errorMessage);
//        }
//
//        @Override
//        public void success() {
//
//        }
//      });
//    }
//
//    @Override
//    public void step(StepAction stepAction, int resumeReason) {
//      expectedSuspendReason = resumeReason;
//
//      context.continueVm(stepAction, 0, new DebugContext.ContinueCallback() {
//        @Override
//        public void failure(String errorMessage) {
//          getChromeDebugTarget().reportError("Error executing step command", errorMessage);
//        }
//
//        @Override
//        public void success() {
//
//        }
//      });
//    }
//
//    @Override
//    public void suspend() {
//      // do nothing
//
//    }
//
//    private ChromeDebugStackFrame[] createStackFrames() {
//      List<ChromeDebugStackFrame> frames = new ArrayList<ChromeDebugStackFrame>();
//
//      ChromeDebugStackFrame parentFrame = null;
//
//      for (CallFrame jsFrame : context.getCallFrames()) {
//        ChromeDebugStackFrame frame = new ChromeDebugStackFrame(getDebugTarget(),
//            ChromeDebugThread.this, parentFrame, jsFrame);
//
//        frames.add(frame);
//
//        Map<String, String> frameVarStates = variablesStates.get(frame.getFrameId());
//        frame.setPreviousVariableStates(frameVarStates);
//
//        parentFrame = frame;
//      }
//
//      return frames.toArray(new ChromeDebugStackFrame[frames.size()]);
//    }
//  }
//
//  private static interface ThreadState {
//    boolean canStep();
//
//    boolean canSuspend();
//
//    void dismiss();
//
//    IBreakpoint[] getBreakpoints();
//
//    IStackFrame[] getStackFrames();
//
//    boolean isStepping();
//
//    boolean isSuspended();
//
//    void resume();
//
//    void step(StepAction stepAction, int resumeReason);
//
//    void suspend();
//  }
//
//  private static final IBreakpoint[] EMPTY_BREAKPOINTS = new IBreakpoint[0];
//
//  private static final IStackFrame[] EMPTY_FRAMES = new IStackFrame[0];
//
//  private int expectedSuspendReason = DebugEvent.UNSPECIFIED;
//
//  private ThreadState threadState = new RunningThreadState();
//
//  private IBreakpoint[] suspendBreakpoints = EMPTY_BREAKPOINTS;
//
//  private Map<String, Map<String, String>> variablesStates = new HashMap<String, Map<String, String>>();
//
//  /**
//   * Create a new ChromeDebugThread.
//   * 
//   * @param target
//   */
//  public ChromeDebugThread(ChromeDebugTarget target) {
//    super(target);
//  }
//
//  @Override
//  public boolean canResume() {
//    return !isDisconnected() && isSuspended();
//  }
//
//  @Override
//  public boolean canStepInto() {
//    return threadState.canStep();
//  }
//
//  @Override
//  public boolean canStepOver() {
//    return threadState.canStep();
//  }
//
//  @Override
//  public boolean canStepReturn() {
//    return threadState.canStep();
//  }
//
//  @Override
//  public boolean canSuspend() {
//    return !isDisconnected() && threadState.canSuspend();
//  }
//
//  @Override
//  public boolean canTerminate() {
//    return getDebugTarget().canTerminate();
//  }
//
//  @Override
//  public IBreakpoint[] getBreakpoints() {
//    return threadState.getBreakpoints();
//  }
//
//  @Override
//  public String getName() throws DebugException {
//    return "Call Stack";
//  }
//
//  @Override
//  public int getPriority() throws DebugException {
//    return 0;
//  }
//
//  @Override
//  public IStackFrame[] getStackFrames() throws DebugException {
//    return threadState.getStackFrames();
//  }
//
//  @Override
//  public IStackFrame getTopStackFrame() throws DebugException {
//    IStackFrame[] frames = getStackFrames();
//    if (frames.length == 0) {
//      return null;
//    }
//    // This is a check for exception frames.
////    if (frames[0].isRegularFrame()) {
////      return frames[0];
////    }
//    return frames[0];
//  }
//
//  @Override
//  public boolean hasStackFrames() throws DebugException {
//    return isSuspended();
//  }
//
//  @Override
//  public boolean isStepping() {
//    return threadState.isStepping();
//  }
//
//  @Override
//  public boolean isSuspended() {
//    return !isDisconnected() && threadState.isSuspended();
//  }
//
//  @Override
//  public boolean isTerminated() {
//    return getDebugTarget().isTerminated();
//  }
//
//  @Override
//  public void resume() throws DebugException {
//    threadState.resume();
//  }
//
//  @Override
//  public void stepInto() throws DebugException {
//    threadState.step(StepAction.IN, DebugEvent.STEP_INTO);
//  }
//
//  @Override
//  public void stepOver() throws DebugException {
//    threadState.step(StepAction.OVER, DebugEvent.STEP_OVER);
//  }
//
//  @Override
//  public void stepReturn() throws DebugException {
//    threadState.step(StepAction.OUT, DebugEvent.STEP_RETURN);
//  }
//
//  @Override
//  public void suspend() throws DebugException {
//    threadState.suspend();
//  }
//
//  @Override
//  public void terminate() throws DebugException {
//    getDebugTarget().terminate();
//  }
//
//  protected ChromeDebugTarget getChromeDebugTarget() {
//    return (ChromeDebugTarget) getDebugTarget();
//  }
//
//  /**
//   * Called by ChromeDebugTarget.
//   */
//  protected void resumed() {
//    try {
//      rememberVariableStates();
//    } catch (DebugException ex) {
//      DartDebugCorePlugin.logError(ex);
//    }
//
//    threadState.dismiss();
//    threadState = new RunningThreadState();
//
//    //resumeReason = D.UNSPECIFIED;
//
//    fireResumeEvent(DebugEvent.CLIENT_REQUEST);
//  }
//
//  /**
//   * Called by ChromeDebugTarget.
//   */
//  protected void suspended(DebugContext context) {
//    if (threadState.isSuspended()) {
//      throw new IllegalStateException("Already in suspended state");
//    }
//
//    threadState = new SuspendedThreadState(context);
//
//    int suspendedDetail = DebugEvent.UNSPECIFIED;
//
//    clearSuspendBreakpoints();
//
//    if (context.getState() == org.chromium.sdk.DebugContext.State.EXCEPTION) {
//      suspendedDetail = DebugEvent.BREAKPOINT;
//    } else {
//      Collection<? extends Breakpoint> jsBreakpoints = context.getBreakpointsHit();
//
//      if (jsBreakpoints.isEmpty()) {
//        suspendedDetail = expectedSuspendReason;
//      } else {
//        suspendedDetail = DebugEvent.BREAKPOINT;
//
//        List<DartBreakpoint> breakpoints = new ArrayList<DartBreakpoint>();
//
//        for (Breakpoint jsBreakpoint : jsBreakpoints) {
//          DartBreakpoint dartBreakpoint = getTarget().getDartBreakpoint(jsBreakpoint);
//
//          if (dartBreakpoint != null) {
//            breakpoints.add(dartBreakpoint);
//          }
//        }
//
//        setSuspendBreakpoints(breakpoints);
//      }
//    }
//
//    fireSuspendEvent(suspendedDetail);
//  }
//
//  private void clearSuspendBreakpoints() {
//    suspendBreakpoints = EMPTY_BREAKPOINTS;
//  }
//
//  private boolean isDisconnected() {
//    return getDebugTarget().isDisconnected();
//  }
//
//  private void rememberVariableStates() throws DebugException {
//    variablesStates.clear();
//
//    for (IStackFrame stackFrame : threadState.getStackFrames()) {
//      ChromeDebugStackFrame frame = (ChromeDebugStackFrame) stackFrame;
//
//      String frameId = frame.getFrameId();
//
//      Map<String, String> frameVarStates = new HashMap<String, String>();
//
//      frame.storeVariableStates(frameVarStates);
//
//      variablesStates.put(frameId, frameVarStates);
//    }
//  }
//
//  private void setSuspendBreakpoints(List<DartBreakpoint> breakpoints) {
//    suspendBreakpoints = breakpoints.toArray(new IBreakpoint[breakpoints.size()]);
//  }

}
