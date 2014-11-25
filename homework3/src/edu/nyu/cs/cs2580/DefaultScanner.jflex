package edu.nyu.cs.cs2580;

%%

%unicode 6.3
%integer
%public
%final
%class DefaultScanner
%function getNextToken
%buffer 4096

HangulEx            = [\p{Script:Hangul}&&[\p{WB:ALetter}\p{WB:Hebrew_Letter}]] [\p{WB:Format}\p{WB:Extend}]*
HebrewOrALetterEx   = [\p{WB:HebrewLetter}\p{WB:ALetter}]                       [\p{WB:Format}\p{WB:Extend}]*
NumericEx           = [\p{WB:Numeric}[\p{Blk:HalfAndFullForms}&&\p{Nd}]]        [\p{WB:Format}\p{WB:Extend}]*
KatakanaEx          = \p{WB:Katakana}                                           [\p{WB:Format}\p{WB:Extend}]* 
MidLetterEx         = [\p{WB:MidLetter}\p{WB:MidNumLet}\p{WB:SingleQuote}]      [\p{WB:Format}\p{WB:Extend}]* 
MidNumericEx        = [\p{WB:MidNum}\p{WB:MidNumLet}\p{WB:SingleQuote}]         [\p{WB:Format}\p{WB:Extend}]*
ExtendNumLetEx      = \p{WB:ExtendNumLet}                                       [\p{WB:Format}\p{WB:Extend}]*
HanEx               = \p{Script:Han}                                            [\p{WB:Format}\p{WB:Extend}]*
HiraganaEx          = \p{Script:Hiragana}                                       [\p{WB:Format}\p{WB:Extend}]*
SingleQuoteEx       = \p{WB:Single_Quote}                                       [\p{WB:Format}\p{WB:Extend}]*
DoubleQuoteEx       = \p{WB:Double_Quote}                                       [\p{WB:Format}\p{WB:Extend}]*
HebrewLetterEx      = \p{WB:Hebrew_Letter}                                      [\p{WB:Format}\p{WB:Extend}]*
RegionalIndicatorEx = \p{WB:RegionalIndicator}                                  [\p{WB:Format}\p{WB:Extend}]*
ComplexContextEx    = \p{LB:Complex_Context}                                    [\p{WB:Format}\p{WB:Extend}]*

ASCIITLD = "." (
	  [aA][cC]
	| [aA][dD]
	| [aA][eE]
	| [aA][eE][rR][oO]
	| [aA][fF]
	| [aA][gG]
	| [aA][iI]
	| [aA][lL]
	| [aA][mM]
	| [aA][nN]
	| [aA][oO]
	| [aA][qQ]
	| [aA][rR]
	| [aA][rR][pP][aA]
	| [aA][sS]
	| [aA][sS][iI][aA]
	| [aA][tT]
	| [aA][uU]
	| [aA][wW]
	| [aA][xX]
	| [aA][zZ]
	| [bB][aA]
	| [bB][bB]
	| [bB][dD]
	| [bB][eE]
	| [bB][fF]
	| [bB][gG]
	| [bB][hH]
	| [bB][iI]
	| [bB][iI][kK][eE]
	| [bB][iI][zZ]
	| [bB][jJ]
	| [bB][mM]
	| [bB][nN]
	| [bB][oO]
	| [bB][rR]
	| [bB][sS]
	| [bB][tT]
	| [bB][vV]
	| [bB][wW]
	| [bB][yY]
	| [bB][zZ]
	| [cC][aA]
	| [cC][aA][mM][eE][rR][aA]
	| [cC][aA][tT]
	| [cC][cC]
	| [cC][dD]
	| [cC][fF]
	| [cC][gG]
	| [cC][hH]
	| [cC][iI]
	| [cC][kK]
	| [cC][lL]
	| [cC][lL][oO][tT][hH][iI][nN][gG]
	| [cC][mM]
	| [cC][nN]
	| [cC][oO]
	| [cC][oO][mM]
	| [cC][oO][nN][sS][tT][rR][uU][cC][tT][iI][oO][nN]
	| [cC][oO][nN][tT][rR][aA][cC][tT][oO][rR][sS]
	| [cC][oO][oO][pP]
	| [cC][rR]
	| [cC][uU]
	| [cC][vV]
	| [cC][wW]
	| [cC][xX]
	| [cC][yY]
	| [cC][zZ]
	| [dD][eE]
	| [dD][iI][aA][mM][oO][nN][dD][sS]
	| [dD][iI][rR][eE][cC][tT][oO][rR][yY]
	| [dD][jJ]
	| [dD][kK]
	| [dD][mM]
	| [dD][oO]
	| [dD][zZ]
	| [eE][cC]
	| [eE][dD][uU]
	| [eE][eE]
	| [eE][gG]
	| [eE][nN][tT][eE][rR][pP][rR][iI][sS][eE][sS]
	| [eE][qQ][uU][iI][pP][mM][eE][nN][tT]
	| [eE][rR]
	| [eE][sS]
	| [eE][sS][tT][aA][tT][eE]
	| [eE][tT]
	| [eE][uU]
	| [fF][iI]
	| [fF][jJ]
	| [fF][kK]
	| [fF][mM]
	| [fF][oO]
	| [fF][rR]
	| [gG][aA]
	| [gG][aA][lL][lL][eE][rR][yY]
	| [gG][bB]
	| [gG][dD]
	| [gG][eE]
	| [gG][fF]
	| [gG][gG]
	| [gG][hH]
	| [gG][iI]
	| [gG][lL]
	| [gG][mM]
	| [gG][nN]
	| [gG][oO][vV]
	| [gG][pP]
	| [gG][qQ]
	| [gG][rR]
	| [gG][rR][aA][pP][hH][iI][cC][sS]
	| [gG][sS]
	| [gG][tT]
	| [gG][uU]
	| [gG][uU][rR][uU]
	| [gG][wW]
	| [gG][yY]
	| [hH][kK]
	| [hH][mM]
	| [hH][nN]
	| [hH][oO][lL][dD][iI][nN][gG][sS]
	| [hH][rR]
	| [hH][tT]
	| [hH][uU]
	| [iI][dD]
	| [iI][eE]
	| [iI][lL]
	| [iI][mM]
	| [iI][nN]
	| [iI][nN][fF][oO]
	| [iI][nN][tT]
	| [iI][oO]
	| [iI][qQ]
	| [iI][rR]
	| [iI][sS]
	| [iI][tT]
	| [jJ][eE]
	| [jJ][mM]
	| [jJ][oO]
	| [jJ][oO][bB][sS]
	| [jJ][pP]
	| [kK][eE]
	| [kK][gG]
	| [kK][hH]
	| [kK][iI]
	| [kK][iI][tT][cC][hH][eE][nN]
	| [kK][mM]
	| [kK][nN]
	| [kK][pP]
	| [kK][rR]
	| [kK][wW]
	| [kK][yY]
	| [kK][zZ]
	| [lL][aA]
	| [lL][aA][nN][dD]
	| [lL][bB]
	| [lL][cC]
	| [lL][iI]
	| [lL][iI][gG][hH][tT][iI][nN][gG]
	| [lL][kK]
	| [lL][rR]
	| [lL][sS]
	| [lL][tT]
	| [lL][uU]
	| [lL][vV]
	| [lL][yY]
	| [mM][aA]
	| [mM][cC]
	| [mM][dD]
	| [mM][eE]
	| [mM][eE][nN][uU]
	| [mM][gG]
	| [mM][hH]
	| [mM][iI][lL]
	| [mM][kK]
	| [mM][lL]
	| [mM][mM]
	| [mM][nN]
	| [mM][oO]
	| [mM][oO][bB][iI]
	| [mM][pP]
	| [mM][qQ]
	| [mM][rR]
	| [mM][sS]
	| [mM][tT]
	| [mM][uU]
	| [mM][uU][sS][eE][uU][mM]
	| [mM][vV]
	| [mM][wW]
	| [mM][xX]
	| [mM][yY]
	| [mM][zZ]
	| [nN][aA]
	| [nN][aA][mM][eE]
	| [nN][cC]
	| [nN][eE]
	| [nN][eE][tT]
	| [nN][fF]
	| [nN][gG]
	| [nN][iI]
	| [nN][lL]
	| [nN][oO]
	| [nN][pP]
	| [nN][rR]
	| [nN][uU]
	| [nN][zZ]
	| [oO][mM]
	| [oO][rR][gG]
	| [pP][aA]
	| [pP][eE]
	| [pP][fF]
	| [pP][gG]
	| [pP][hH]
	| [pP][hH][oO][tT][oO][gG][rR][aA][pP][hH][yY]
	| [pP][kK]
	| [pP][lL]
	| [pP][lL][uU][mM][bB][iI][nN][gG]
	| [pP][mM]
	| [pP][nN]
	| [pP][oO][sS][tT]
	| [pP][rR]
	| [pP][rR][oO]
	| [pP][sS]
	| [pP][tT]
	| [pP][wW]
	| [pP][yY]
	| [qQ][aA]
	| [rR][eE]
	| [rR][oO]
	| [rR][sS]
	| [rR][uU]
	| [rR][wW]
	| [sS][aA]
	| [sS][bB]
	| [sS][cC]
	| [sS][dD]
	| [sS][eE]
	| [sS][eE][xX][yY]
	| [sS][gG]
	| [sS][hH]
	| [sS][iI]
	| [sS][iI][nN][gG][lL][eE][sS]
	| [sS][jJ]
	| [sS][kK]
	| [sS][lL]
	| [sS][mM]
	| [sS][nN]
	| [sS][oO]
	| [sS][rR]
	| [sS][tT]
	| [sS][uU]
	| [sS][vV]
	| [sS][xX]
	| [sS][yY]
	| [sS][zZ]
	| [tT][aA][tT][tT][oO][oO]
	| [tT][cC]
	| [tT][dD]
	| [tT][eE][cC][hH][nN][oO][lL][oO][gG][yY]
	| [tT][eE][lL]
	| [tT][fF]
	| [tT][gG]
	| [tT][hH]
	| [tT][iI][pP][sS]
	| [tT][jJ]
	| [tT][kK]
	| [tT][lL]
	| [tT][mM]
	| [tT][nN]
	| [tT][oO]
	| [tT][oO][dD][aA][yY]
	| [tT][pP]
	| [tT][rR]
	| [tT][rR][aA][vV][eE][lL]
	| [tT][tT]
	| [tT][vV]
	| [tT][wW]
	| [tT][zZ]
	| [uU][aA]
	| [uU][gG]
	| [uU][kK]
	| [uU][nN][oO]
	| [uU][sS]
	| [uU][yY]
	| [uU][zZ]
	| [vV][aA]
	| [vV][cC]
	| [vV][eE]
	| [vV][eE][nN][tT][uU][rR][eE][sS]
	| [vV][gG]
	| [vV][iI]
	| [vV][nN]
	| [vV][oO][yY][aA][gG][eE]
	| [vV][uU]
	| [wW][fF]
	| [wW][sS]
	| [xX][nN]--3[eE]0[bB]707[eE]
	| [xX][nN]--45[bB][rR][jJ]9[cC]
	| [xX][nN]--80[aA][oO]21[aA]
	| [xX][nN]--80[aA][sS][eE][hH][dD][bB]
	| [xX][nN]--80[aA][sS][wW][gG]
	| [xX][nN]--90[aA]3[aA][cC]
	| [xX][nN]--[cC][lL][cC][hH][cC]0[eE][aA]0[bB]2[gG]2[aA]9[gG][cC][dD]
	| [xX][nN]--[fF][iI][qQ][sS]8[sS]
	| [xX][nN]--[fF][iI][qQ][zZ]9[sS]
	| [xX][nN]--[fF][pP][cC][rR][jJ]9[cC]3[dD]
	| [xX][nN]--[fF][zZ][cC]2[cC]9[eE]2[cC]
	| [xX][nN]--[gG][eE][cC][rR][jJ]9[cC]
	| [xX][nN]--[hH]2[bB][rR][jJ]9[cC]
	| [xX][nN]--[jJ]1[aA][mM][hH]
	| [xX][nN]--[jJ]6[wW]193[gG]
	| [xX][nN]--[kK][pP][rR][wW]13[dD]
	| [xX][nN]--[kK][pP][rR][yY]57[dD]
	| [xX][nN]--[lL]1[aA][cC][cC]
	| [xX][nN]--[lL][gG][bB][bB][aA][tT]1[aA][dD]8[jJ]
	| [xX][nN]--[mM][gG][bB]9[aA][wW][bB][fF]
	| [xX][nN]--[mM][gG][bB][aA]3[aA]4[fF]16[aA]
	| [xX][nN]--[mM][gG][bB][aA][aA][mM]7[aA]8[hH]
	| [xX][nN]--[mM][gG][bB][aA][yY][hH]7[gG][pP][aA]
	| [xX][nN]--[mM][gG][bB][bB][hH]1[aA]71[eE]
	| [xX][nN]--[mM][gG][bB][cC]0[aA]9[aA][zZ][cC][gG]
	| [xX][nN]--[mM][gG][bB][eE][rR][pP]4[aA]5[dD]4[aA][rR]
	| [xX][nN]--[mM][gG][bB][xX]4[cC][dD]0[aA][bB]
	| [xX][nN]--[nN][gG][bB][cC]5[aA][zZ][dD]
	| [xX][nN]--[oO]3[cC][wW]4[hH]
	| [xX][nN]--[oO][gG][bB][pP][fF]8[fF][lL]
	| [xX][nN]--[pP]1[aA][iI]
	| [xX][nN]--[pP][gG][bB][sS]0[dD][hH]
	| [xX][nN]--[qQ]9[jJ][yY][bB]4[cC]
	| [xX][nN]--[sS]9[bB][rR][jJ]9[cC]
	| [xX][nN]--[uU][nN][uU][pP]4[yY]
	| [xX][nN]--[wW][gG][bB][hH]1[cC]
	| [xX][nN]--[wW][gG][bB][lL]6[aA]
	| [xX][nN]--[xX][kK][cC]2[aA][lL]3[hH][yY][eE]2[aA]
	| [xX][nN]--[xX][kK][cC]2[dD][lL]3[aA]5[eE][eE]0[hH]
	| [xX][nN]--[yY][fF][rR][oO]4[iI]67[oO]
	| [xX][nN]--[yY][gG][bB][iI]2[aA][mM][mM][xX]
	| [xX][xX][xX]
	| [yY][eE]
	| [yY][tT]
	| [zZ][aA]
	| [zZ][mM]
	| [zZ][wW]
	) "."?   // Accept trailing root (empty) domain

	DomainLabel = [A-Za-z0-9] ([-A-Za-z0-9]* [A-Za-z0-9])?
	DomainNameStrict = {DomainLabel} ("." {DomainLabel})* {ASCIITLD}
    DomainNameLoose = {DomainLabel} ("." {DomainLabel})*

    IPv4DecimalOctet = "0"{0,2} [0-9] | "0"? [1-9][0-9] | "1" [0-9][0-9] | "2" ([0-4][0-9] | "5" [0-5])
    IPv4Address  = {IPv4DecimalOctet} ("." {IPv4DecimalOctet}){3}
    IPv6Hex16Bit = [0-9A-Fa-f]{1,4}
    IPv6LeastSignificant32Bits = {IPv4Address} | ({IPv6Hex16Bit} ":" {IPv6Hex16Bit})
    IPv6Address =                                                  ({IPv6Hex16Bit} ":"){6} {IPv6LeastSignificant32Bits}
                |                                             "::" ({IPv6Hex16Bit} ":"){5} {IPv6LeastSignificant32Bits}
                |                            {IPv6Hex16Bit}?  "::" ({IPv6Hex16Bit} ":"){4} {IPv6LeastSignificant32Bits}
                | (({IPv6Hex16Bit} ":"){0,1} {IPv6Hex16Bit})? "::" ({IPv6Hex16Bit} ":"){3} {IPv6LeastSignificant32Bits}
                | (({IPv6Hex16Bit} ":"){0,2} {IPv6Hex16Bit})? "::" ({IPv6Hex16Bit} ":"){2} {IPv6LeastSignificant32Bits}
                | (({IPv6Hex16Bit} ":"){0,3} {IPv6Hex16Bit})? "::"  {IPv6Hex16Bit} ":"     {IPv6LeastSignificant32Bits}
                | (({IPv6Hex16Bit} ":"){0,4} {IPv6Hex16Bit})? "::"                         {IPv6LeastSignificant32Bits}
                | (({IPv6Hex16Bit} ":"){0,5} {IPv6Hex16Bit})? "::"                         {IPv6Hex16Bit}
                | (({IPv6Hex16Bit} ":"){0,6} {IPv6Hex16Bit})? "::"

    URIunreserved = [-._~A-Za-z0-9]
    URIpercentEncoded = "%" [0-9A-Fa-f]{2}
    URIsubDelims = [!$&'()*+,;=]
    URIloginSegment = ({URIunreserved} | {URIpercentEncoded} | {URIsubDelims})*
    URIlogin = {URIloginSegment} (":" {URIloginSegment})? "@"
    URIquery    = "?" ({URIunreserved} | {URIpercentEncoded} | {URIsubDelims} | [:@/?])*
    URIfragment = "#" ({URIunreserved} | {URIpercentEncoded} | {URIsubDelims} | [:@/?])*
    URIport = ":" [0-9]{1,5}
    URIhostStrict = ("[" {IPv6Address} "]") | {IPv4Address} | {DomainNameStrict}
    URIhostLoose  = ("[" {IPv6Address} "]") | {IPv4Address} | {DomainNameLoose}
    URIauthorityLoose  = {URIlogin}? {URIhostLoose}  {URIport}?

    HTTPsegment = ({URIunreserved} | {URIpercentEncoded} | [;:@&=])*
    HTTPpath = ("/" {HTTPsegment})+
    HTTPscheme = [hH][tT][tT][pP][sS]? "://"
    HTTPurlFull = {HTTPscheme} {URIlogin}? {URIhostLoose} {URIport}? {HTTPpath}? {URIquery}? {URIfragment}?
    URIportRequired = {URIport} {HTTPpath}? {URIquery}? {URIfragment}?
    HTTPpathRequired = {URIport}? {HTTPpath} {URIquery}? {URIfragment}?
    URIqueryRequired = {URIport}? {HTTPpath}? {URIquery} {URIfragment}?
    URIfragmentRequired = {URIport}? {HTTPpath}? {URIquery}? {URIfragment}
    // {HTTPurlNoScheme} excludes {URIlogin}, because it could otherwise accept e-mail addresses
    HTTPurlNoScheme = {URIhostStrict} ({URIportRequired} | {HTTPpathRequired} | {URIqueryRequired} | {URIfragmentRequired})
    HTTPurl = {HTTPurlFull} | {HTTPurlNoScheme}

    FTPorFILEsegment = ({URIunreserved} | {URIpercentEncoded} | [?:@&=])*
    FTPorFILEpath = "/" {FTPorFILEsegment} ("/" {FTPorFILEsegment})*
    FTPtype = ";" [tT][yY][pP][eE] "=" [aAiIdD]
    FTPscheme = [fF][tT][pP] "://"
    FTPurl = {FTPscheme} {URIauthorityLoose} {FTPorFILEpath} {FTPtype}? {URIfragment}?

    FILEscheme = [fF][iI][lL][eE] "://"
    FILEurl = {FILEscheme} {URIhostLoose}? {FTPorFILEpath} {URIfragment}?

    URL = {HTTPurl} | {FTPurl} | {FILEurl}

%{
  public static final int WORD = Tokenizer.ALPHANUM;

  public static final int NUMERIC = Tokenizer.NUM;

  public static final int SOUTH_EAST_ASIAN = Tokenizer.SOUTHEAST_ASIAN;

  public static final int IDEOGRAPHIC = Tokenizer.IDEOGRAPHIC;

  public static final int HIRAGANA = Tokenizer.HIRAGANA;

  public static final int KATAKANA = Tokenizer.KATAKANA;

  public static final int HANGUL = Tokenizer.HANGUL;

public final String getText() {
    return String.copyValueOf(zzBuffer, zzStartRead, zzMarkedPos-zzStartRead);
  }
%}

%%

<<EOF>> { return YYEOF; }

// UAX#29 WB8.   Numeric × Numeric
//        WB11.  Numeric (MidNum | MidNumLet | Single_Quote) × Numeric
//        WB12.  Numeric × (MidNum | MidNumLet | Single_Quote) Numeric
//        WB13a. (ALetter | Hebrew_Letter | Numeric | Katakana | ExtendNumLet) × ExtendNumLet
//        WB13b. ExtendNumLet × (ALetter | Hebrew_Letter | Numeric | Katakana) 
//
{ExtendNumLetEx}* {NumericEx} ( ( {ExtendNumLetEx}* | {MidNumericEx} ) {NumericEx} )* {ExtendNumLetEx}* 
  { return NUMERIC; }

// subset of the below for typing purposes only!
{HangulEx}+
  { return HANGUL; }
  
{KatakanaEx}+
  { return KATAKANA; }

// UAX#29 WB5.   (ALetter | Hebrew_Letter) × (ALetter | Hebrew_Letter)
//        WB6.   (ALetter | Hebrew_Letter) × (MidLetter | MidNumLet | Single_Quote) (ALetter | Hebrew_Letter)
//        WB7.   (ALetter | Hebrew_Letter) (MidLetter | MidNumLet | Single_Quote) × (ALetter | Hebrew_Letter)
//        WB7a.  Hebrew_Letter × Single_Quote
//        WB7b.  Hebrew_Letter × Double_Quote Hebrew_Letter
//        WB7c.  Hebrew_Letter Double_Quote × Hebrew_Letter
//        WB9.   (ALetter | Hebrew_Letter) × Numeric
//        WB10.  Numeric × (ALetter | Hebrew_Letter)
//        WB13.  Katakana × Katakana
//        WB13a. (ALetter | Hebrew_Letter | Numeric | Katakana | ExtendNumLet) × ExtendNumLet
//        WB13b. ExtendNumLet × (ALetter | Hebrew_Letter | Numeric | Katakana) 
//
{ExtendNumLetEx}*  ( {KatakanaEx}          ( {ExtendNumLetEx}*   {KatakanaEx}                           )*
                   | ( {HebrewLetterEx}    ( {SingleQuoteEx}     | {DoubleQuoteEx}  {HebrewLetterEx}    )
                     | {NumericEx}         ( ( {ExtendNumLetEx}* | {MidNumericEx} ) {NumericEx}         )*
                     | {HebrewOrALetterEx} ( ( {ExtendNumLetEx}* | {MidLetterEx}  ) {HebrewOrALetterEx} )*
                     )+
                   )
({ExtendNumLetEx}+ ( {KatakanaEx}          ( {ExtendNumLetEx}*   {KatakanaEx}                           )*
                   | ( {HebrewLetterEx}    ( {SingleQuoteEx}     | {DoubleQuoteEx}  {HebrewLetterEx}    )
                     | {NumericEx}         ( ( {ExtendNumLetEx}* | {MidNumericEx} ) {NumericEx}         )*
                     | {HebrewOrALetterEx} ( ( {ExtendNumLetEx}* | {MidLetterEx}  ) {HebrewOrALetterEx} )*
                     )+
                   )
)*
{ExtendNumLetEx}* 
  { return WORD; }


// From UAX #29:
//
//    [C]haracters with the Line_Break property values of Contingent_Break (CB), 
//    Complex_Context (SA/South East Asian), and XX (Unknown) are assigned word 
//    boundary property values based on criteria outside of the scope of this
//    annex.  That means that satisfactory treatment of languages like Chinese
//    or Thai requires special handling.
// 
// In Unicode 6.3, only one character has the \p{Line_Break = Contingent_Break}
// property: U+FFFC ( ￼ ) OBJECT REPLACEMENT CHARACTER.
//
// In the ICU implementation of UAX#29, \p{Line_Break = Complex_Context}
// character sequences (from South East Asian scripts like Thai, Myanmar, Khmer,
// Lao, etc.) are kept together.  This grammar does the same below.
//
// See also the Unicode Line Breaking Algorithm:
//
//    http://www.unicode.org/reports/tr14/#SA
//
{ComplexContextEx}+ { return SOUTH_EAST_ASIAN; }

// UAX#29 WB14.  Any ÷ Any
//
{HanEx} { return IDEOGRAPHIC; }
{HiraganaEx} { return HIRAGANA; }


// UAX#29 WB3.   CR × LF
//        WB3a.  (Newline | CR | LF) ÷
//        WB3b.  ÷ (Newline | CR | LF)
//        WB13c. Regional_Indicator × Regional_Indicator
//        WB14.  Any ÷ Any
//
{RegionalIndicatorEx} {RegionalIndicatorEx}+ | [^]
  { /* Break so we don't hit fall-through warning: */ break; /* Not numeric, word, ideographic, hiragana, or SE Asian -- ignore it. */ }

{URL} {/* Break so we don't hit fall-through warning: */ break; /* URL -- ignore it. */}
