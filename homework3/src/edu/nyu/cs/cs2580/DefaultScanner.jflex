package edu.nyu.cs.cs2580;

%%

%unicode 6.3
%integer
%public
%final
%class DefaultScanner
%function getNextToken
%buffer 1024

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

%{
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
  { return 1; }

// subset of the below for typing purposes only!
{HangulEx}+
  { return 2; }
  
{KatakanaEx}+
  { return 3; }

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
  { return 4; }


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
{ComplexContextEx}+ { return 5; }

// UAX#29 WB14.  Any ÷ Any
//
{HanEx} { return 6; }
{HiraganaEx} { return 7; }


// UAX#29 WB3.   CR × LF
//        WB3a.  (Newline | CR | LF) ÷
//        WB3b.  ÷ (Newline | CR | LF)
//        WB13c. Regional_Indicator × Regional_Indicator
//        WB14.  Any ÷ Any
//
{RegionalIndicatorEx} {RegionalIndicatorEx}+ | [^]
  { /* Break so we don't hit fall-through warning: */ break; /* Not numeric, word, ideographic, hiragana, or SE Asian -- ignore it. */ }
