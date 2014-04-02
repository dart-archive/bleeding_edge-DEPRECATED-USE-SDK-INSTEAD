/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.functions;

/**
 * Symbols for the heuristic scanner.
 */
public interface Symbols {
  int TokenEOF = -1;
  // Symbols
  int TokenLBRACE = 1; // {
  int TokenRBRACE = 2; // }
  int TokenLBRACKET = 3; // [
  int TokenRBRACKET = 4; // ]
  int TokenLPAREN = 5;
  int TokenRPAREN = 6;
  int TokenSEMICOLON = 7;
  int TokenOTHER = 8; // period, operators, number literals etc
  int TokenCOLON = 9;
  int TokenQUESTIONMARK = 10;
  int TokenCOMMA = 11;
  int TokenEQUAL = 12;
  int TokenLESSTHAN = 13;
  int TokenGREATERTHAN = 14;
  int TokenDEFUN = 15; // =>
  int TokenAT = 16;
  int TokenAND = 17; // &&
  // Keywords
  int TokenASSERT = 1000;
  int TokenBREAK = 1001;
  int TokenCASE = 1002;
  int TokenCATCH = 1003;
  int TokenCLASS = 1004;
  int TokenCONST = 1005;
  int TokenCONTINUE = 1006;
  int TokenDEFAULT = 1007;
  int TokenDO = 1008;
  int TokenELSE = 1009;
  int TokenENUM = 1010;
  int TokenEXTENDS = 1011;
  int TokenFALSE = 1012;
  int TokenFINAL = 1013;
  int TokenFINALLY = 1014;
  int TokenFOR = 1015;
  int TokenIF = 1016;
  int TokenIN = 1017;
  int TokenIS = 1018;
  int TokenNEW = 1019;
  int TokenNULL = 1020;
  int TokenRETHROW = 1021;
  int TokenRETURN = 1022;
  int TokenSUPER = 1023;
  int TokenSWITCH = 1024;
  int TokenTHIS = 1025;
  int TokenTHROW = 1026;
  int TokenTRUE = 1027;
  int TokenTRY = 1028;
  int TokenVAR = 1029;
  int TokenVOID = 1030;
  int TokenWHILE = 1031;
  int TokenWITH = 1032;
  // Pseudo keywords
  int TokenABSTRACT = 1200;
  int TokenAS = 1201;
  int TokenDYNAMIC = 1202;
  int TokenEXPORT = 1203;
  int TokenEXTERNAL = 1204;
  int TokenFACTORY = 1205;
  int TokenGET = 1206;
  int TokenIMPLEMENTS = 1207;
  int TokenIMPORT = 1208;
  int TokenLIBRARY = 1209;
  int TokenOPERATOR = 1210;
  int TokenPART = 1211;
  int TokenSET = 1212;
  int TokenSTATIC = 1213;
  int TokenTYPEDEF = 1214;
  // Special
  int TokenIDENT = 2000;
}
