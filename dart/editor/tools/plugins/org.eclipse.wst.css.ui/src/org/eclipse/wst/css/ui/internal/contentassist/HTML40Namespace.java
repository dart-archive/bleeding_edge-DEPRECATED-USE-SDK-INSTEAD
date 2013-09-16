/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
/* nlsXXX */
package org.eclipse.wst.css.ui.internal.contentassist;

/**
 * Provide all names defined in the HTML 4.0? specification.
 */
public interface HTML40Namespace {

  // Element names
  public static interface ElementName {
    public static final String A = "A"; //$NON-NLS-1$
    public static final String ABBR = "ABBR"; //$NON-NLS-1$
    public static final String ACRONYM = "ACRONYM"; //$NON-NLS-1$
    public static final String ADDRESS = "ADDRESS"; //$NON-NLS-1$
    public static final String APPLET = "APPLET"; //$NON-NLS-1$
    public static final String AREA = "AREA"; //$NON-NLS-1$
    public static final String B = "B"; //$NON-NLS-1$
    public static final String BASE = "BASE"; //$NON-NLS-1$
    public static final String BASEFONT = "BASEFONT"; //$NON-NLS-1$
    public static final String BDO = "BDO"; //$NON-NLS-1$
    public static final String BGSOUND = "BGSOUND"; // D205513
    // //$NON-NLS-1$
    public static final String BIG = "BIG"; //$NON-NLS-1$
    public static final String BLINK = "BLINK"; //$NON-NLS-1$
    public static final String BLOCKQUOTE = "BLOCKQUOTE"; //$NON-NLS-1$
    public static final String BODY = "BODY"; //$NON-NLS-1$
    public static final String BR = "BR"; //$NON-NLS-1$
    public static final String BUTTON = "BUTTON"; //$NON-NLS-1$
    public static final String CAPTION = "CAPTION"; //$NON-NLS-1$
    public static final String CENTER = "CENTER"; //$NON-NLS-1$
    public static final String CITE = "CITE"; //$NON-NLS-1$
    public static final String CODE = "CODE"; //$NON-NLS-1$
    public static final String COL = "COL"; //$NON-NLS-1$
    public static final String COLGROUP = "COLGROUP"; //$NON-NLS-1$
    public static final String DD = "DD"; //$NON-NLS-1$
    public static final String DEL = "DEL"; //$NON-NLS-1$
    public static final String DFN = "DFN"; //$NON-NLS-1$
    public static final String DIR = "DIR"; //$NON-NLS-1$
    public static final String DIV = "DIV"; //$NON-NLS-1$
    public static final String DL = "DL"; //$NON-NLS-1$
    public static final String DT = "DT"; //$NON-NLS-1$
    public static final String EM = "EM"; //$NON-NLS-1$
    public static final String EMBED = "EMBED"; //$NON-NLS-1$
    public static final String FIELDSET = "FIELDSET"; //$NON-NLS-1$
    public static final String FONT = "FONT"; //$NON-NLS-1$
    public static final String FORM = "FORM"; //$NON-NLS-1$
    public static final String FRAME = "FRAME"; //$NON-NLS-1$
    public static final String FRAMESET = "FRAMESET"; //$NON-NLS-1$
    public static final String H1 = "H1"; //$NON-NLS-1$
    public static final String H2 = "H2"; //$NON-NLS-1$
    public static final String H3 = "H3"; //$NON-NLS-1$
    public static final String H4 = "H4"; //$NON-NLS-1$
    public static final String H5 = "H5"; //$NON-NLS-1$
    public static final String H6 = "H6"; //$NON-NLS-1$
    public static final String HEAD = "HEAD"; //$NON-NLS-1$
    public static final String HR = "HR"; //$NON-NLS-1$
    public static final String HTML = "HTML"; //$NON-NLS-1$
    public static final String I = "I"; //$NON-NLS-1$
    public static final String IFRAME = "IFRAME"; //$NON-NLS-1$
    public static final String IMG = "IMG"; //$NON-NLS-1$
    public static final String INPUT = "INPUT"; //$NON-NLS-1$
    public static final String INS = "INS"; //$NON-NLS-1$
    public static final String ISINDEX = "ISINDEX"; //$NON-NLS-1$
    public static final String KBD = "KBD"; //$NON-NLS-1$
    public static final String LABEL = "LABEL"; //$NON-NLS-1$
    public static final String LEGEND = "LEGEND"; //$NON-NLS-1$
    public static final String LI = "LI"; //$NON-NLS-1$
    public static final String LINK = "LINK"; //$NON-NLS-1$
    public static final String MAP = "MAP"; //$NON-NLS-1$
    public static final String MENU = "MENU"; //$NON-NLS-1$
    public static final String META = "META"; //$NON-NLS-1$
    public static final String NOBR = "NOBR"; // D205513 //$NON-NLS-1$
    public static final String NOEMBED = "NOEMBED"; //$NON-NLS-1$
    public static final String NOFRAMES = "NOFRAMES"; //$NON-NLS-1$
    public static final String NOSCRIPT = "NOSCRIPT"; //$NON-NLS-1$
    public static final String OBJECT = "OBJECT"; //$NON-NLS-1$
    public static final String OL = "OL"; //$NON-NLS-1$
    public static final String OPTGROUP = "OPTGROUP"; //$NON-NLS-1$
    public static final String OPTION = "OPTION"; //$NON-NLS-1$
    public static final String P = "P"; //$NON-NLS-1$
    public static final String PARAM = "PARAM"; //$NON-NLS-1$
    public static final String PRE = "PRE"; //$NON-NLS-1$
    public static final String Q = "Q"; //$NON-NLS-1$
    public static final String S = "S"; //$NON-NLS-1$
    public static final String SAMP = "SAMP"; //$NON-NLS-1$
    public static final String SCRIPT = "SCRIPT"; //$NON-NLS-1$
    public static final String SELECT = "SELECT"; //$NON-NLS-1$
    public static final String SMALL = "SMALL"; //$NON-NLS-1$
    public static final String SPAN = "SPAN"; //$NON-NLS-1$
    public static final String STRIKE = "STRIKE"; //$NON-NLS-1$
    public static final String STRONG = "STRONG"; //$NON-NLS-1$
    public static final String STYLE = "STYLE"; //$NON-NLS-1$
    public static final String SUB = "SUB"; //$NON-NLS-1$
    public static final String SUP = "SUP"; //$NON-NLS-1$
    public static final String TABLE = "TABLE"; //$NON-NLS-1$
    public static final String TBODY = "TBODY"; //$NON-NLS-1$
    public static final String TD = "TD"; //$NON-NLS-1$
    public static final String TEXTAREA = "TEXTAREA"; //$NON-NLS-1$
    public static final String TFOOT = "TFOOT"; //$NON-NLS-1$
    public static final String TH = "TH"; //$NON-NLS-1$
    public static final String THEAD = "THEAD"; //$NON-NLS-1$
    public static final String TITLE = "TITLE"; //$NON-NLS-1$
    public static final String TR = "TR"; //$NON-NLS-1$
    public static final String TT = "TT"; //$NON-NLS-1$
    public static final String U = "U"; //$NON-NLS-1$
    public static final String UL = "UL"; //$NON-NLS-1$
    public static final String VAR = "VAR"; //$NON-NLS-1$
    public static final String WBR = "WBR"; // D205513 //$NON-NLS-1$
    public static final String MARQUEE = "MARQUEE"; //$NON-NLS-1$
    public static final String SSI_CONFIG = "SSI:CONFIG"; // D210393
    // //$NON-NLS-1$
    public static final String SSI_ECHO = "SSI:ECHO"; //$NON-NLS-1$
    public static final String SSI_EXEC = "SSI:EXEC"; //$NON-NLS-1$
    public static final String SSI_FSIZE = "SSI:FSIZE"; //$NON-NLS-1$
    public static final String SSI_FLASTMOD = "SSI:FLASTMOD"; //$NON-NLS-1$
    public static final String SSI_INCLUDE = "SSI:INCLUDE"; //$NON-NLS-1$
    public static final String SSI_PRINTENV = "SSI:PRINTENV"; //$NON-NLS-1$
    public static final String SSI_SET = "SSI:SET"; //$NON-NLS-1$
    public static final String WML_WML = "wml"; //$NON-NLS-1$
    public static final String WML_CARD = "card"; //$NON-NLS-1$
    public static final String WML_TEMPLATE = "template"; //$NON-NLS-1$
    public static final String WML_ACCESS = "access"; //$NON-NLS-1$
    public static final String WML_DO = "do"; //$NON-NLS-1$
    public static final String WML_ONEVENT = "onevent"; //$NON-NLS-1$
    public static final String WML_TIMER = "timer"; //$NON-NLS-1$
    public static final String WML_ANCHOR = "anchor"; //$NON-NLS-1$
    public static final String WML_PREV = "prev"; //$NON-NLS-1$
    public static final String WML_REFRESH = "refresh"; //$NON-NLS-1$
    public static final String WML_GO = "go"; //$NON-NLS-1$
    public static final String WML_NOOP = "noop"; //$NON-NLS-1$
    public static final String WML_SETVAR = "setvar"; //$NON-NLS-1$
    public static final String WML_POSTFIELD = "postfield"; //$NON-NLS-1$
  }

  // Character Entities
  public static interface EntityName {
    public static final String AACUTE_U = "Aacute"; //$NON-NLS-1$
    public static final String AACUTE_L = "aacute"; //$NON-NLS-1$
    public static final String ACIRC_U = "Acirc"; //$NON-NLS-1$
    public static final String ACIRC_L = "acirc"; //$NON-NLS-1$
    public static final String ACUTE = "acute"; //$NON-NLS-1$
    public static final String AELIG_U = "AElig"; //$NON-NLS-1$
    public static final String AELIG_L = "aelig"; //$NON-NLS-1$
    public static final String AGRAVE_U = "Agrave"; //$NON-NLS-1$
    public static final String AGRAVE_L = "agrave"; //$NON-NLS-1$
    public static final String ALEFSYM = "alefsym"; //$NON-NLS-1$
    public static final String ALPHA_U = "Alpha"; //$NON-NLS-1$
    public static final String ALPHA_L = "alpha"; //$NON-NLS-1$
    public static final String AMP = "amp"; //$NON-NLS-1$
    public static final String AND = "and"; //$NON-NLS-1$
    public static final String ANG = "ang"; //$NON-NLS-1$
    public static final String ARING_U = "Aring"; //$NON-NLS-1$
    public static final String ARING_L = "aring"; //$NON-NLS-1$
    public static final String ASYMP = "asymp"; //$NON-NLS-1$
    public static final String ATILDE_U = "Atilde"; //$NON-NLS-1$
    public static final String ATILDE_L = "atilde"; //$NON-NLS-1$
    public static final String AUML_U = "Auml"; //$NON-NLS-1$
    public static final String AUML_L = "auml"; //$NON-NLS-1$
    public static final String BDQUO = "bdquo"; //$NON-NLS-1$
    public static final String BETA_U = "Beta"; //$NON-NLS-1$
    public static final String BETA_L = "beta"; //$NON-NLS-1$
    public static final String BRVBAR = "brvbar"; //$NON-NLS-1$
    public static final String BULL = "bull"; //$NON-NLS-1$
    public static final String CAP = "cap"; //$NON-NLS-1$
    public static final String CCEDIL_U = "Ccedil"; //$NON-NLS-1$
    public static final String CCEDIL_L = "ccedil"; //$NON-NLS-1$
    public static final String CEDIL = "cedil"; //$NON-NLS-1$
    public static final String CENT = "cent"; //$NON-NLS-1$
    public static final String CHI_U = "Chi"; //$NON-NLS-1$
    public static final String CHI_L = "chi"; //$NON-NLS-1$
    public static final String CIRC = "circ"; //$NON-NLS-1$
    public static final String CLUBS = "clubs"; //$NON-NLS-1$
    public static final String CONG = "cong"; //$NON-NLS-1$
    public static final String COPY = "copy"; //$NON-NLS-1$
    public static final String CRARR = "crarr"; //$NON-NLS-1$
    public static final String CUP = "cup"; //$NON-NLS-1$
    public static final String CURREN = "curren"; //$NON-NLS-1$
    public static final String DAGGER_U = "Dagger"; //$NON-NLS-1$
    public static final String DAGGER_L = "dagger"; //$NON-NLS-1$
    public static final String DARR_U = "dArr"; //$NON-NLS-1$
    public static final String DARR_L = "darr"; //$NON-NLS-1$
    public static final String DEG = "deg"; //$NON-NLS-1$
    public static final String DELTA_U = "Delta"; //$NON-NLS-1$
    public static final String DELTA_L = "delta"; //$NON-NLS-1$
    public static final String DIAMS = "diams"; //$NON-NLS-1$
    public static final String DIVIDE = "divide"; //$NON-NLS-1$
    public static final String EACUTE_U = "Eacute"; //$NON-NLS-1$
    public static final String EACUTE_L = "eacute"; //$NON-NLS-1$
    public static final String ECIRC_U = "Ecirc"; //$NON-NLS-1$
    public static final String ECIRC_L = "ecirc"; //$NON-NLS-1$
    public static final String EGRAVE_U = "Egrave"; //$NON-NLS-1$
    public static final String EGRAVE_L = "egrave"; //$NON-NLS-1$
    public static final String EMPTY = "empty"; //$NON-NLS-1$
    public static final String EMSP = "emsp"; //$NON-NLS-1$
    public static final String ENSP = "ensp"; //$NON-NLS-1$
    public static final String EPSILON_U = "Epsilon"; //$NON-NLS-1$
    public static final String EPSILON_L = "epsilon"; //$NON-NLS-1$
    public static final String EQUIV = "equiv"; //$NON-NLS-1$
    public static final String ETA_U = "Eta"; //$NON-NLS-1$
    public static final String ETA_L = "eta"; //$NON-NLS-1$
    public static final String ETH_U = "ETH"; //$NON-NLS-1$
    public static final String ETH_L = "eth"; //$NON-NLS-1$
    public static final String EUML_U = "Euml"; //$NON-NLS-1$
    public static final String EUML_L = "euml"; //$NON-NLS-1$
    public static final String EURO = "euro"; //$NON-NLS-1$
    public static final String EXIST = "exist"; //$NON-NLS-1$
    public static final String FNOF = "fnof"; //$NON-NLS-1$
    public static final String FORALL = "forall"; //$NON-NLS-1$
    public static final String FRAC12 = "frac12"; //$NON-NLS-1$
    public static final String FRAC14 = "frac14"; //$NON-NLS-1$
    public static final String FRAC34 = "frac34"; //$NON-NLS-1$
    public static final String FRASL = "frasl"; //$NON-NLS-1$
    public static final String GAMMA_U = "Gamma"; //$NON-NLS-1$
    public static final String GAMMA_L = "gamma"; //$NON-NLS-1$
    public static final String GE = "ge"; //$NON-NLS-1$
    public static final String GT = "gt"; //$NON-NLS-1$
    public static final String HARR_U = "hArr"; //$NON-NLS-1$
    public static final String HARR_L = "harr"; //$NON-NLS-1$
    public static final String HEARTS = "hearts"; //$NON-NLS-1$
    public static final String HELLIP = "hellip"; //$NON-NLS-1$
    public static final String IACUTE_U = "Iacute"; //$NON-NLS-1$
    public static final String IACUTE_L = "iacute"; //$NON-NLS-1$
    public static final String ICIRC_U = "Icirc"; //$NON-NLS-1$
    public static final String ICIRC_L = "icirc"; //$NON-NLS-1$
    public static final String IEXCL = "iexcl"; //$NON-NLS-1$
    public static final String IGRAVE_U = "Igrave"; //$NON-NLS-1$
    public static final String IGRAVE_L = "igrave"; //$NON-NLS-1$
    public static final String IMAGE = "image"; //$NON-NLS-1$
    public static final String INFIN = "infin"; //$NON-NLS-1$
    public static final String INT = "int"; //$NON-NLS-1$
    public static final String IOTA_U = "Iota"; //$NON-NLS-1$
    public static final String IOTA_L = "iota"; //$NON-NLS-1$
    public static final String IQUEST = "iquest"; //$NON-NLS-1$
    public static final String ISIN = "isin"; //$NON-NLS-1$
    public static final String IUML_U = "Iuml"; //$NON-NLS-1$
    public static final String IUML_L = "iuml"; //$NON-NLS-1$
    public static final String KAPPA_U = "Kappa"; //$NON-NLS-1$
    public static final String KAPPA_L = "kappa"; //$NON-NLS-1$
    public static final String LAMBDA_U = "Lambda"; //$NON-NLS-1$
    public static final String LAMBDA_L = "lambda"; //$NON-NLS-1$
    public static final String LANG = "lang"; //$NON-NLS-1$
    public static final String LAQUO = "laquo"; //$NON-NLS-1$
    public static final String LARR_U = "lArr"; //$NON-NLS-1$
    public static final String LARR_L = "larr"; //$NON-NLS-1$
    public static final String LCEIL = "lceil"; //$NON-NLS-1$
    public static final String LDQUO = "ldquo"; //$NON-NLS-1$
    public static final String LE = "le"; //$NON-NLS-1$
    public static final String LFLOOR = "lfloor"; //$NON-NLS-1$
    public static final String LOWAST = "lowast"; //$NON-NLS-1$
    public static final String LOZ = "loz"; //$NON-NLS-1$
    public static final String LRM = "lrm"; //$NON-NLS-1$
    public static final String LSAQUO = "lsaquo"; //$NON-NLS-1$
    public static final String LSQUO = "lsquo"; //$NON-NLS-1$
    public static final String LT = "lt"; //$NON-NLS-1$
    public static final String MACR = "macr"; //$NON-NLS-1$
    public static final String MDASH = "mdash"; //$NON-NLS-1$
    public static final String MICRO = "micro"; //$NON-NLS-1$
    public static final String MIDDOT = "middot"; //$NON-NLS-1$
    public static final String MINUS = "minus"; //$NON-NLS-1$
    public static final String MU_U = "Mu"; //$NON-NLS-1$
    public static final String MU_L = "mu"; //$NON-NLS-1$
    public static final String NABLA = "nabla"; //$NON-NLS-1$
    public static final String NBSP = "nbsp"; //$NON-NLS-1$
    public static final String NDASH = "ndash"; //$NON-NLS-1$
    public static final String NE = "ne"; //$NON-NLS-1$
    public static final String NI = "ni"; //$NON-NLS-1$
    public static final String NOT = "not"; //$NON-NLS-1$
    public static final String NOTIN = "notin"; //$NON-NLS-1$
    public static final String NSUB = "nsub"; //$NON-NLS-1$
    public static final String NTILDE_U = "Ntilde"; //$NON-NLS-1$
    public static final String NTILDE_L = "ntilde"; //$NON-NLS-1$
    public static final String NU_U = "Nu"; //$NON-NLS-1$
    public static final String NU_L = "nu"; //$NON-NLS-1$
    public static final String OACUTE_U = "Oacute"; //$NON-NLS-1$
    public static final String OACUTE_L = "oacute"; //$NON-NLS-1$
    public static final String OCIRC_U = "Ocirc"; //$NON-NLS-1$
    public static final String OCIRC_L = "ocirc"; //$NON-NLS-1$
    public static final String OELIG_U = "OElig"; //$NON-NLS-1$
    public static final String OELIG_L = "oelig"; //$NON-NLS-1$
    public static final String OGRAVE_U = "Ograve"; //$NON-NLS-1$
    public static final String OGRAVE_L = "ograve"; //$NON-NLS-1$
    public static final String OLINE = "oline"; //$NON-NLS-1$
    public static final String OMEGA_U = "Omega"; //$NON-NLS-1$
    public static final String OMEGA_L = "omega"; //$NON-NLS-1$
    public static final String OMICRON_U = "Omicron"; //$NON-NLS-1$
    public static final String OMICRON_L = "omicron"; //$NON-NLS-1$
    public static final String OPLUS = "oplus"; //$NON-NLS-1$
    public static final String OR = "or"; //$NON-NLS-1$
    public static final String ORDF = "ordf"; //$NON-NLS-1$
    public static final String ORDM = "ordm"; //$NON-NLS-1$
    public static final String OSLASH_U = "Oslash"; //$NON-NLS-1$
    public static final String OSLASH_L = "oslash"; //$NON-NLS-1$
    public static final String OTILDE_U = "Otilde"; //$NON-NLS-1$
    public static final String OTILDE_L = "otilde"; //$NON-NLS-1$
    public static final String OTIMES = "otimes"; //$NON-NLS-1$
    public static final String OUML_U = "Ouml"; //$NON-NLS-1$
    public static final String OUML_L = "ouml"; //$NON-NLS-1$
    public static final String PARA = "para"; //$NON-NLS-1$
    public static final String PART = "part"; //$NON-NLS-1$
    public static final String PERMIL = "permil"; //$NON-NLS-1$
    public static final String PERP = "perp"; //$NON-NLS-1$
    public static final String PHI_U = "Phi"; //$NON-NLS-1$
    public static final String PHI_L = "phi"; //$NON-NLS-1$
    public static final String PI_U = "Pi"; //$NON-NLS-1$
    public static final String PI_L = "pi"; //$NON-NLS-1$
    public static final String PIV = "piv"; //$NON-NLS-1$
    public static final String PLUSMN = "plusmn"; //$NON-NLS-1$
    public static final String POUND = "pound"; //$NON-NLS-1$
    public static final String PRIME_U = "Prime"; //$NON-NLS-1$
    public static final String PRIME_L = "prime"; //$NON-NLS-1$
    public static final String PROD = "prod"; //$NON-NLS-1$
    public static final String PROP = "prop"; //$NON-NLS-1$
    public static final String PSI_U = "Psi"; //$NON-NLS-1$
    public static final String PSI_L = "psi"; //$NON-NLS-1$
    public static final String QUOT = "quot"; //$NON-NLS-1$
    public static final String RADIC = "radic"; //$NON-NLS-1$
    public static final String RANG = "rang"; //$NON-NLS-1$
    public static final String RAQUO = "raquo"; //$NON-NLS-1$
    public static final String RARR_U = "rArr"; //$NON-NLS-1$
    public static final String RARR_L = "rarr"; //$NON-NLS-1$
    public static final String RCEIL = "rceil"; //$NON-NLS-1$
    public static final String RDQUO = "rdquo"; //$NON-NLS-1$
    public static final String REAL = "real"; //$NON-NLS-1$
    public static final String REG = "reg"; //$NON-NLS-1$
    public static final String RFLOOR = "rfloor"; //$NON-NLS-1$
    public static final String RHO_U = "Rho"; //$NON-NLS-1$
    public static final String RHO_L = "rho"; //$NON-NLS-1$
    public static final String RLM = "rlm"; //$NON-NLS-1$
    public static final String RSAQUO = "rsaquo"; //$NON-NLS-1$
    public static final String RSQUO = "rsquo"; //$NON-NLS-1$
    public static final String SBQUO = "sbquo"; //$NON-NLS-1$
    public static final String SCARON_U = "Scaron"; //$NON-NLS-1$
    public static final String SCARON_L = "scaron"; //$NON-NLS-1$
    public static final String SDOT = "sdot"; //$NON-NLS-1$
    public static final String SECT = "sect"; //$NON-NLS-1$
    public static final String SHY = "shy"; //$NON-NLS-1$
    public static final String SIGMA_U = "Sigma"; //$NON-NLS-1$
    public static final String SIGMA_L = "sigma"; //$NON-NLS-1$
    public static final String SIGMAF = "sigmaf"; //$NON-NLS-1$
    public static final String SIM = "sim"; //$NON-NLS-1$
    public static final String SPADES = "spades"; //$NON-NLS-1$
    public static final String SUB = "sub"; //$NON-NLS-1$
    public static final String SUBE = "sube"; //$NON-NLS-1$
    public static final String SUM = "sum"; //$NON-NLS-1$
    public static final String SUP = "sup"; //$NON-NLS-1$
    public static final String SUP1 = "sup1"; //$NON-NLS-1$
    public static final String SUP2 = "sup2"; //$NON-NLS-1$
    public static final String SUP3 = "sup3"; //$NON-NLS-1$
    public static final String SUPE = "supe"; //$NON-NLS-1$
    public static final String SZLIG = "szlig"; //$NON-NLS-1$
    public static final String TAU_U = "Tau"; //$NON-NLS-1$
    public static final String TAU_L = "tau"; //$NON-NLS-1$
    public static final String THERE4 = "there4"; //$NON-NLS-1$
    public static final String THETA_U = "Theta"; //$NON-NLS-1$
    public static final String THETA_L = "theta"; //$NON-NLS-1$
    public static final String THETASYM = "thetasym"; //$NON-NLS-1$
    public static final String THINSP = "thinsp"; //$NON-NLS-1$
    public static final String THORN_U = "THORN"; //$NON-NLS-1$
    public static final String THORN_L = "thorn"; //$NON-NLS-1$
    public static final String TILDE = "tilde"; //$NON-NLS-1$
    public static final String TIMES = "times"; //$NON-NLS-1$
    public static final String TRADE = "trade"; //$NON-NLS-1$
    public static final String UACUTE_U = "Uacute"; //$NON-NLS-1$
    public static final String UACUTE_L = "uacute"; //$NON-NLS-1$
    public static final String UARR_U = "uArr"; //$NON-NLS-1$
    public static final String UARR_L = "uarr"; //$NON-NLS-1$
    public static final String UCIRC_U = "Ucirc"; //$NON-NLS-1$
    public static final String UCIRC_L = "ucirc"; //$NON-NLS-1$
    public static final String UGRAVE_U = "Ugrave"; //$NON-NLS-1$
    public static final String UGRAVE_L = "ugrave"; //$NON-NLS-1$
    public static final String UML = "uml"; //$NON-NLS-1$
    public static final String UPSIH = "upsih"; //$NON-NLS-1$
    public static final String UPSILON_U = "Upsilon"; //$NON-NLS-1$
    public static final String UPSILON_L = "upsilon"; //$NON-NLS-1$
    public static final String UUML_U = "Uuml"; //$NON-NLS-1$
    public static final String UUML_L = "uuml"; //$NON-NLS-1$
    public static final String WEIERP = "weierp"; //$NON-NLS-1$
    public static final String XI_U = "Xi"; //$NON-NLS-1$
    public static final String XI_L = "xi"; //$NON-NLS-1$
    public static final String YACUTE_U = "Yacute"; //$NON-NLS-1$
    public static final String YACUTE_L = "yacute"; //$NON-NLS-1$
    public static final String YEN = "yen"; //$NON-NLS-1$
    public static final String YUML_U = "Yuml"; //$NON-NLS-1$
    public static final String YUML_L = "yuml"; //$NON-NLS-1$
    public static final String ZETA_U = "Zeta"; //$NON-NLS-1$
    public static final String ZETA_L = "zeta"; //$NON-NLS-1$
    public static final String ZWJ = "zwj"; //$NON-NLS-1$
    public static final String ZWNJ = "zwnj"; //$NON-NLS-1$
  }

  public static final String HTML40_URI = "http://www.w3.org/TR/REC-html40/frameset.dtd"; //$NON-NLS-1$
  public static final String HTML40_TAG_PREFIX = ""; //$NON-NLS-1$
  // global attribute names
  public static final String ATTR_NAME_ID = "id"; // %coreattrs;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CLASS = "class"; // %coreattrs;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_STYLE = "style"; // %coreattrs;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_TITLE = "title"; // %coreattrs;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_LANG = "lang"; // %i18n;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_DIR = "dir"; // %i18n; //$NON-NLS-1$
  public static final String ATTR_NAME_ONCLICK = "onclick"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONDBLCLICK = "ondblclick"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONMOUSEDOWN = "onmousedown"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONMOUSEUP = "onmouseup"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONMOUSEOVER = "onmouseover"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONMOUSEMOVE = "onmousemove"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONMOUSEOUT = "onmouseout"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONKEYPRESS = "onkeypress"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONKEYDOWN = "onkeydown"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONKEYUP = "onkeyup"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONHELP = "onhelp"; // %events;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_BGCOLOR = "bgcolor"; // %bodycolor;,
  // TABLE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_TEXT = "text"; // %bodycolor;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_LINK = "link"; // %bodycolor;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_VLINK = "vlink"; // %bodycolor;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ALINK = "alink"; // %bodycolor;
  // //$NON-NLS-1$
  public static final String ATTR_NAME_VERSION = "version"; // HTML
  // //$NON-NLS-1$
  public static final String ATTR_NAME_PROFILE = "profile"; // HEAD
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONLOAD = "onload"; // BODY
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONUNLOAD = "onunload"; // BODY
  // //$NON-NLS-1$
  public static final String ATTR_NAME_BACKGROUND = "background"; // BODY,
  // TABLE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SRC = "src"; // IMG //$NON-NLS-1$
  public static final String ATTR_NAME_ALT = "alt"; // IMG,AREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_LONGDESC = "longdesc"; // IMG
  // //$NON-NLS-1$
  public static final String ATTR_NAME_NAME = "name"; // IMG //$NON-NLS-1$
  public static final String ATTR_NAME_HEIGHT = "height"; // IMG, TABLE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_WIDTH = "width"; // IMG, TABLE,HR
  // //$NON-NLS-1$
  public static final String ATTR_NAME_USEMAP = "usemap"; // IMG
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ISMAP = "ismap"; // IMG //$NON-NLS-1$
  public static final String ATTR_NAME_ALIGN = "align"; // IMG, TABLE,HR
  // //$NON-NLS-1$
  public static final String ATTR_NAME_BORDER = "border"; // IMG, TABLE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_HSPACE = "hspace"; // IMG
  // //$NON-NLS-1$
  public static final String ATTR_NAME_VSPACE = "vspace"; // IMG
  // //$NON-NLS-1$
  public static final String ATTR_NAME_MAPFILE = "mapfile"; // IMG
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SUMMARY = "summary"; // TABLE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_FRAME = "frame"; // TABLE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_RULES = "rules"; // TABLE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CELLSPACING = "cellspacing"; // TABLE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CELLPADDING = "cellpadding"; // TABLE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_DATAPAGESIZE = "datapagesize"; // TABLE,HR
  // //$NON-NLS-1$
  public static final String ATTR_NAME_COLOR = "color"; // BASEFONT,FONT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_FACE = "face"; // BASEFONT,FONT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SIZE = "size"; // BASEFONT,FONT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CLEAR = "clear"; // BR //$NON-NLS-1$
  public static final String ATTR_NAME_SHAPE = "shape"; // AREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_COORDS = "coords"; // AREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_HREF = "href"; // AREA //$NON-NLS-1$
  public static final String ATTR_NAME_TARGET = "target"; // AREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_NOHREF = "nohref"; // AREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_TABINDEX = "tabindex"; // AREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ACCESSKEY = "accesskey"; // AREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONFOCUS = "onfocus"; // AREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONBLUR = "onblur"; // AREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CHARSET = "charset"; // LINK
  // //$NON-NLS-1$
  public static final String ATTR_NAME_HREFLANG = "hreflang"; // LINK
  // //$NON-NLS-1$
  public static final String ATTR_NAME_TYPE = "type"; // LINK //$NON-NLS-1$
  public static final String ATTR_NAME_REL = "rel"; // LINK //$NON-NLS-1$
  public static final String ATTR_NAME_REV = "rev"; // LINK //$NON-NLS-1$
  public static final String ATTR_NAME_MEDIA = "media"; // LINK
  // //$NON-NLS-1$
  public static final String ATTR_NAME_VALUE = "value"; // PARAM
  // //$NON-NLS-1$
  public static final String ATTR_NAME_VALUETYPE = "valuetype"; // PARAM
  // //$NON-NLS-1$
  public static final String ATTR_NAME_NOSHADE = "noshade"; // HR
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CHECKED = "checked"; // INPUT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_DISABLED = "disabled"; // INPUT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_READONLY = "readonly"; // INPUT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_MAXLENGTH = "maxlength"; // INPUT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONSELECT = "onselect"; // INPUT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONCHANGE = "onchange"; // INPUT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ACCEPT = "accept"; // INPUT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ISTYLE = "istyle"; // INPUT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CHAR = "char"; // COL //$NON-NLS-1$
  public static final String ATTR_NAME_CHAROFF = "charoff"; // COL
  // //$NON-NLS-1$
  public static final String ATTR_NAME_VALIGN = "valign"; // COL
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SPAN = "span"; // COL //$NON-NLS-1$
  public static final String ATTR_NAME_FRAMEBORDER = "frameborder"; // FRAME
  // //$NON-NLS-1$
  public static final String ATTR_NAME_MARGINWIDTH = "marginwidth"; // FRAME
  // //$NON-NLS-1$
  public static final String ATTR_NAME_MARGINHEIGHT = "marginheight"; // FRAME
  // //$NON-NLS-1$
  public static final String ATTR_NAME_NORESIZE = "noresize"; // FRAME
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SCROLLING = "scrolling"; // FRAME
  // //$NON-NLS-1$
  public static final String ATTR_NAME_PROMPT = "prompt"; // ISINDEX
  // //$NON-NLS-1$
  public static final String ATTR_NAME_HTTP_EQUIV = "http-equiv"; // META
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CONTENT = "content"; // META
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SCHEME = "scheme"; // META
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ERRMSG = "errmsg"; // ssi:config
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SIZEFMT = "sizefmt"; // ssi:config
  // //$NON-NLS-1$
  public static final String ATTR_NAME_TIMEFMT = "timefmt"; // ssi:config
  // //$NON-NLS-1$
  public static final String ATTR_NAME_VAR = "var"; // ssi:echo
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CGI = "cgi"; // ssi:exec
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CMD = "cmd"; // ssi:exec
  // //$NON-NLS-1$
  public static final String ATTR_NAME_FILE = "file"; // ssi:fsize
  // //$NON-NLS-1$
  public static final String ATTR_NAME_VIRTUAL = "virtual"; // ssi:fsize
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SELECTED = "selected"; // OPTION
  // //$NON-NLS-1$
  public static final String ATTR_NAME_LABEL = "label"; // OPTION
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ROWS = "rows"; // TEXTAREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_COLS = "cols"; // TEXTAREA
  // //$NON-NLS-1$
  public static final String ATTR_NAME_LANGUAGE = "language"; // SCRIPT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_DEFER = "defer"; // SCRIPT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_EVENT = "event"; // SCRIPT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_FOR = "for"; // SCRIPT //$NON-NLS-1$
  public static final String ATTR_NAME_COMPACT = "compact"; // OL/UL
  // //$NON-NLS-1$
  public static final String ATTR_NAME_START = "start"; // OL/UL
  // //$NON-NLS-1$
  public static final String ATTR_NAME_DIRECTKEY = "directkey"; // A
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CODEBASE = "codebase"; // APPLET
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ARCHIVE = "archive"; // APPLET
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CODE = "code"; // APPLET
  // //$NON-NLS-1$
  public static final String ATTR_NAME_OBJECT = "object"; // APPLET
  // //$NON-NLS-1$
  public static final String ATTR_NAME_MAYSCRIPT = "mayscript"; // APPLET
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CITE = "cite"; // BLOCKQUOTE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_MACRO = "macro"; // D2W //$NON-NLS-1$
  public static final String ATTR_NAME_DATETIME = "datetime"; // INS/DEL
  // //$NON-NLS-1$
  public static final String ATTR_NAME_LOOP = "loop"; // EMBED //$NON-NLS-1$
  public static final String ATTR_NAME_HIDDEN = "hidden"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_VOLUME = "volume"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_AUTOSTART = "autostart"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_AUTOPLAY = "autoplay"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_AUTOSIZE = "autosize"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CONTROLLER = "controller";// EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SCALE = "scale"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SHOWCONTROLS = "showcontrols";// EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_PLAYCOUNT = "playcount"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_REPEAT = "repeat"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_PANEL = "panel"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_PALETTE = "palette"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_TEXTFOCUS = "textfocus"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ACTION = "action"; // FORM
  // //$NON-NLS-1$
  public static final String ATTR_NAME_METHOD = "method"; // FORM
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ENCTYPE = "enctype"; // FORM
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONSUBMIT = "onsubmit"; // FORM
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ONRESET = "onreset"; // FORM
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ACCEPT_CHARSET = "accept-charset";// FORM
  // //$NON-NLS-1$
  public static final String ATTR_NAME_BEHAVIOR = "behavior"; // MARQUEE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_DIRECTION = "direction"; // MARQUEE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SCROLLAMOUNT = "scrollamount";// MARQUEE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SCROLLDELAY = "scrolldelay";// MARQUEE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_TRUESPEED = "truespeed"; // MARQUEE
  // //$NON-NLS-1$
  public static final String ATTR_NAME_DECLARE = "declare"; // OBJECT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CLASSID = "classid"; // OBJECT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_DATA = "data"; // OBJECT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_CODETYPE = "codetype"; // OBJECT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_STANDBY = "standby"; // OBJECT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_MULTIPLE = "multiple"; // SELECT
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ABBR = "abbr"; // TH/TD //$NON-NLS-1$
  public static final String ATTR_NAME_AXIS = "axis"; // TH/TD //$NON-NLS-1$
  public static final String ATTR_NAME_HEADERS = "headers"; // TH/TD
  // //$NON-NLS-1$
  public static final String ATTR_NAME_SCOPE = "scope"; // TH/TD
  // //$NON-NLS-1$
  public static final String ATTR_NAME_ROWSPAN = "rowspan"; // TH/TD
  // //$NON-NLS-1$
  public static final String ATTR_NAME_COLSPAN = "colspan"; // TH/TD
  // //$NON-NLS-1$
  public static final String ATTR_NAME_NOWRAP = "nowrap"; // TH/TD
  // //$NON-NLS-1$
  // <<D205514
  public static final String ATTR_NAME_TOPMARGIN = "topmargin"; // BODY
  // //$NON-NLS-1$
  public static final String ATTR_NAME_BOTTOMMARGIN = "bottommargin"; // BODY
  // //$NON-NLS-1$
  public static final String ATTR_NAME_LEFTMARGIN = "leftmargin"; // BODY
  // //$NON-NLS-1$
  public static final String ATTR_NAME_RIGHTMARGIN = "rightmargin"; // BODY
  // //$NON-NLS-1$
  public static final String ATTR_NAME_BORDERCOLOR = "bordercolor"; // TABLE/TR/TH/TD/FRAMESET/FRAME
  // //$NON-NLS-1$
  // for WML
  public static final String WML_ATTR_NAME_TITLE = "title"; // card
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_DOMAIN = "domain"; // access
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_PATH = "path"; // access
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_ONENTERFORWARD = "onenterforward"; // template,card
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_ONENTERBACKWARD = "onenterbackward"; // template,card
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_ONPICK = "onpick"; // option
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_ONTIMER = "ontimer"; // template,card
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_NEWCONTEXT = "newcontext"; // card
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_ORDERED = "ordered"; // card
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_OPTIONAL = "optional"; // do
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_CACHE_CONTROL = "cache-control"; // go
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_SENDREFERER = "sendreferer"; // go
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_METHOD = "method"; // go
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_ENCTYPE = "enctype"; // go
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_ACCEPT_CHARSET = "accept-charset"; // go
  // //$NON-NLS-1$
  public static final String WML_ATTR_NAME_COLUMNS = "columns"; // table
  // //$NON-NLS-1$
  // D205514
  //<<D215684
  public static final String ATTR_NAME_FRAMESPACING = "framespacing"; // FRAMESET
  // //$NON-NLS-1$
  //D215684
  // global attribute values; mainly used in enumeration.
  public static final String ATTR_VALUE_VERSION_TRANSITIONAL = "-//W3C//DTD HTML 4.01 Transitional//EN"; //$NON-NLS-1$
  public static final String ATTR_VALUE_VERSION_FRAMESET = "-//W3C//DTD HTML 4.01 Frameset//EN"; //$NON-NLS-1$
  public static final String ATTR_VALUE_LTR = "ltr"; // dir //$NON-NLS-1$
  public static final String ATTR_VALUE_RTL = "rtl"; // dir //$NON-NLS-1$
  //   for align (top|middle|bottom|left|right)
  public static final String ATTR_VALUE_TOP = "top"; // align //$NON-NLS-1$
  public static final String ATTR_VALUE_MIDDLE = "middle"; // align
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_BOTTOM = "bottom"; // align
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_LEFT = "left"; // align
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_CENTER = "center"; // align
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_RIGHT = "right"; // align
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_JUSTIFY = "justify"; // align
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_CHAR = "char"; // align
  // //$NON-NLS-1$
  //   for valign (baseline)
  public static final String ATTR_VALUE_BASELINE = "baseline"; // valign
  // //$NON-NLS-1$
  //   for clear (left|all|right|none): left and right are already defined
  // above.
  public static final String ATTR_VALUE_ALL = "all"; // clear //$NON-NLS-1$
  public static final String ATTR_VALUE_NONE = "none"; // clear
  // //$NON-NLS-1$
  //   for shape (rect|circle|poly|default)
  public static final String ATTR_VALUE_RECT = "rect"; // shape
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_CIRCLE = "circle"; // shape
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_POLY = "poly"; // shape
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_DEFAULT = "default"; // shape
  // //$NON-NLS-1$
  //   for valuetype (data|ref|object)
  public static final String ATTR_VALUE_DATA = "data"; // valuetype
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_REF = "ref"; // valuetype
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_OBJECT = "object"; // valuetype
  // //$NON-NLS-1$
  //   for type of INPUT
  //       (text | password | checkbox | radio | submit | reset |
  //        file | hidden | image | button)
  public static final String ATTR_VALUE_TEXT = "text"; // INPUT:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_PASSWORD = "password"; // INPUT:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_CHECKBOX = "checkbox"; // INPUT:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_RADIO = "radio"; // INPUT:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_SUBMIT = "submit"; // INPUT:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_RESET = "reset"; // INPUT:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_FILE = "file"; // INPUT:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_HIDDEN = "hidden"; // INPUT:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_IMAGE = "image"; // INPUT:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_BUTTON = "button"; // INPUT:type
  // //$NON-NLS-1$
  //   for span, colspan, rowspan
  public static final String ATTR_VALUE_1 = "1"; // span //$NON-NLS-1$
  //   for frameborder
  public static final String ATTR_VALUE_0 = "0"; // FRAME //$NON-NLS-1$
  //   for scrolling
  public static final String ATTR_VALUE_YES = "yes"; // FRAME //$NON-NLS-1$
  public static final String ATTR_VALUE_NO = "no"; // FRAME //$NON-NLS-1$
  public static final String ATTR_VALUE_AUTO = "auto"; // FRAME
  // //$NON-NLS-1$
  //   for UL
  public static final String ATTR_VALUE_DISC = "disc"; // UL:type
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_SQUARE = "square"; // UL:type
  // //$NON-NLS-1$
  //   for frame of TABLE
  public static final String ATTR_VALUE_VOID = "void"; // TABLE:frame
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_ABOVE = "above"; // TABLE:frame
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_BELOW = "below"; // TABLE:frame
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_HSIDES = "hsides"; // TABLE:frame
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_LHS = "lhs"; // TABLE:frame
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_RHS = "rhs"; // TABLE:frame
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_VSIDES = "vsides"; // TABLE:frame
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_BOX = "box"; // TABLE:frame
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_BORDER = "border"; // TABLE:frame
  // //$NON-NLS-1$
  //   for rules of TABLE
  public static final String ATTR_VALUE_GROUPS = "groups"; // TABLE:rules
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_ROWS = "rows"; // TEXTAREA
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_COLS = "cols"; // TEXTAREA
  // //$NON-NLS-1$
  //   for method of FORM
  public static final String ATTR_VALUE_GET = "get"; // FORM //$NON-NLS-1$
  public static final String ATTR_VALUE_POST = "post"; // FORM //$NON-NLS-1$
  public static final String ATTR_VALUE_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"; //$NON-NLS-1$
  //   for scope of (TH | TD)
  public static final String ATTR_VALUE_ROW = "row"; // (TH|TD):scope
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_COL = "col"; // (TH|TD):scope
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_ROWGROUP = "rowgroup";// (TH|TD):scope
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_COLGROUP = "colgroup";// (TH|TD):scope
  // //$NON-NLS-1$
  //   for auto?? of EMBED
  public static final String ATTR_VALUE_TRUE = "true"; // EMBED
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_FALSE = "false"; // EMBED
  // //$NON-NLS-1$
  //   for behaviro of MARQUEE
  public static final String ATTR_VALUE_SCROLL = "scroll"; // MARQUEE
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_SLIDE = "slide"; // MARQUEE
  // //$NON-NLS-1$
  public static final String ATTR_VALUE_ALTERNATE = "alternate"; // MARQUEE
  // //$NON-NLS-1$
  //   for direction of MARQUEE
  public static final String ATTR_VALUE_UP = "up"; // MARQUEE //$NON-NLS-1$
  public static final String ATTR_VALUE_DOWN = "down"; // MARQUEE
  // //$NON-NLS-1$
  //   for type of LI (D19924)
  public static final String ATTR_VALUE_NUMBER = "1"; // LI //$NON-NLS-1$
  public static final String ATTR_VALUE_LOWER_ALPHA = "a"; // LI
  // //$NON-NLS-1$
}
