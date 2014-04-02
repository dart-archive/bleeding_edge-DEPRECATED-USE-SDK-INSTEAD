// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Declares a Simulator for ARM64 instructions if we are not generating a native
// ARM64 binary. This Simulator allows us to run and debug ARM64 code generation
// on regular desktop machines.
// Dart calls into generated code by "calling" the InvokeDartCode stub,
// which will start execution in the Simulator or forwards to the real entry
// on a ARM64 HW platform.

#ifndef VM_SIMULATOR_ARM64_H_
#define VM_SIMULATOR_ARM64_H_

#ifndef VM_SIMULATOR_H_
#error Do not include simulator_arm64.h directly; use simulator.h.
#endif

#include "vm/constants_arm64.h"
#include "vm/object.h"

namespace dart {

class Isolate;

class Simulator {
 public:
  static const uword kSimulatorStackUnderflowSize = 64;

  Simulator();
  ~Simulator();

  // The currently executing Simulator instance, which is associated to the
  // current isolate
  static Simulator* Current();

  // Accessors for register state.
  void set_register(Register reg, int64_t value, R31Type r31t = R31IsUndef);
  int64_t get_register(Register reg, R31Type r31t = R31IsUndef) const;
  void set_wregister(Register reg, int32_t value, R31Type r31t = R31IsUndef);
  int32_t get_wregister(Register reg, R31Type r31t = R31IsUndef) const;

  int64_t get_pc() const;
  void set_pc(int64_t pc);

  // Accessor to the internal simulator stack top.
  uword StackTop() const;

  // The isolate's top_exit_frame_info refers to a Dart frame in the simulator
  // stack. The simulator's top_exit_frame_info refers to a C++ frame in the
  // native stack.
  uword top_exit_frame_info() const { return top_exit_frame_info_; }
  void set_top_exit_frame_info(uword value) { top_exit_frame_info_ = value; }

  // Call on program start.
  static void InitOnce() {}

  // Dart generally calls into generated code with 5 parameters. This is a
  // convenience function, which sets up the simulator state and grabs the
  // result on return. The return value is R0. The parameters are placed in
  // R0-3.
  int64_t Call(int64_t entry,
               int64_t parameter0,
               int64_t parameter1,
               int64_t parameter2,
               int64_t parameter3);

  void Longjmp(uword pc,
               uword sp,
               uword fp,
               RawObject* raw_exception,
               RawObject* raw_stacktrace) {
    UNIMPLEMENTED();
  }

 private:
  // Known bad pc value to ensure that the simulator does not execute
  // without being properly setup.
  static const uword kBadLR = -1;
  // A pc value used to signal the simulator to stop execution.  Generally
  // the lr is set to this value on transition from native C code to
  // simulated execution, so that the simulator can "return" to the native
  // C code.
  static const uword kEndSimulatingPC = -2;

  // CPU state.
  int64_t registers_[kNumberOfCpuRegisters];
  bool n_flag_;
  bool z_flag_;
  bool c_flag_;
  bool v_flag_;

  // Simulator support.
  int64_t pc_;
  char* stack_;
  bool pc_modified_;
  intptr_t icount_;
  static int64_t flag_stop_sim_at_;
  uword top_exit_frame_info_;

  // Registered breakpoints.
  Instr* break_pc_;
  int64_t break_instr_;

  // Illegal memory access support.
  static bool IsIllegalAddress(uword addr) {
    return addr < 64*1024;
  }
  void HandleIllegalAccess(uword addr, Instr* instr);

  // Handles a legal instruction that the simulator does not implement.
  void UnimplementedInstruction(Instr* instr);

  // Unsupported instructions use Format to print an error and stop execution.
  void Format(Instr* instr, const char* format);

  // Helper functions to set the conditional flags in the architecture state.
  void SetNZFlagsW(int32_t val);
  bool CarryFromW(int32_t left, int32_t right);
  bool BorrowFromW(int32_t left, int32_t right);
  bool OverflowFromW(
      int32_t alu_out, int32_t left, int32_t right, bool addition);

  void SetNZFlagsX(int64_t val);
  bool CarryFromX(int64_t left, int64_t right);
  bool BorrowFromX(int64_t left, int64_t right);
  bool OverflowFromX(
      int64_t alu_out, int64_t left, int64_t right, bool addition);

  void SetCFlag(bool val);
  void SetVFlag(bool val);

  int64_t ShiftOperand(uint8_t reg_size,
                       int64_t value,
                       Shift shift_type,
                       uint8_t amount);

  int64_t ExtendOperand(uint8_t reg_size,
                        int64_t value,
                        Extend extend_type,
                        uint8_t amount);

  int64_t DecodeShiftExtendOperand(Instr* instr);

  // Decode instructions.
  void InstructionDecode(Instr* instr);
  #define DECODE_OP(op)                                                        \
    void Decode##op(Instr* instr);
  APPLY_OP_LIST(DECODE_OP)
  #undef DECODE_OP

  // Executes ARM64 instructions until the PC reaches kEndSimulatingPC.
  void Execute();

  DISALLOW_COPY_AND_ASSIGN(Simulator);
};

}  // namespace dart

#endif  // VM_SIMULATOR_ARM64_H_
