/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.functions;

/**
 * Symbols for the heuristic java scanner.
 */
public interface Symbols {
  int TokenEOF = -1;
  // Symbols
  int TokenLBRACE = 1;
  int TokenRBRACE = 2;
  int TokenLBRACKET = 3;
  int TokenRBRACKET = 4;
  int TokenLPAREN = 5;
  int TokenRPAREN = 6;
  int TokenSEMICOLON = 7;
  int TokenOTHER = 8;
  int TokenCOLON = 9;
  int TokenQUESTIONMARK = 10;
  int TokenCOMMA = 11;
  int TokenEQUAL = 12;
  int TokenLESSTHAN = 13;
  int TokenGREATERTHAN = 14;
  // Keywords
  int TokenIF = 1009;
  int TokenDO = 1010;
  int TokenFOR = 1011;
  int TokenTRY = 1012;
  int TokenCASE = 1013;
  int TokenELSE = 1014;
  int TokenBREAK = 1015;
  int TokenCATCH = 1016;
  int TokenWHILE = 1017;
  int TokenRETURN = 1018;
  int TokenCONTINUE = 1019;
  int TokenSWITCH = 1020;
  int TokenFINALLY = 1021;
  int TokenVAR = 1022;
  int TokenFUNCTION = 1023;
  int TokenDEFAULT = 1024;
  int TokenNEW = 1025;
  int TokenINSTANCEOF = 1026;
//  int TokenINTERFACE = 1027;
  int TokenNULL = 1028;
  int TokenTHIS = 1029;
  int TokenTRUE = 1030;
  int TokenCONST = 1031;
  int TokenFALSE = 1032;
  int TokenSUPER = 1033;
  int TokenTHROW = 1034;
  // Pseudo-keywords (can be identifiers)
  int TokenABSTRACT = 1200;
  int TokenASSERT = 1201;
  int TokenCLASS = 1202;
  int TokenEXTENDS = 1203;
  int TokenFACTORY = 1204;
  int TokenGET = 1205;
  int TokenIMPLEMENTS = 1206;
  int TokenIMPORT = 1207;
  int TokenINTERFACE = 1208;
  int TokenLIBRARY = 1209;
  int TokenNATIVE = 1210;
  int TokenNEGATE = 1211;
  int TokenOPERATOR = 1212;
  int TokenSET = 1213;
  int TokenSOURCE = 1214;
  int TokenSTATIC = 1215;

  int TokenIDENT = 2000;
}
