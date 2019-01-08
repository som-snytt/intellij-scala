/*
 * Copyright 2000-2008 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.scala.highlighter;

import com.intellij.lexer.HtmlHighlightingLexer;
import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.StringLiteralLexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.lang.lexer.ScalaLexer;
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes;
import org.jetbrains.plugins.scala.lang.lexer.ScalaXmlTokenTypes;
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocLexer;
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType;
import org.jetbrains.plugins.scala.lang.scaladoc.parser.ScalaDocElementTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes.*;

/**
 * @author ilyas
 *         Date: 24.09.2006
 */
public class ScalaSyntaxHighlighter extends SyntaxHighlighterBase {

  // Comments
  static final TokenSet tLINE_COMMENTS = TokenSet.create(
          ScalaTokenTypes.tLINE_COMMENT
  );

  static final TokenSet tBLOCK_COMMENTS = TokenSet.create(
          ScalaTokenTypes.tBLOCK_COMMENT, ScalaTokenTypes.tSH_COMMENT
  );

  static final TokenSet tDOC_COMMENTS = TokenSet.create(
          ScalaDocElementTypes.SCALA_DOC_COMMENT
  );

  // ScalaXmlTokenTypes.XML tags
  static final TokenSet tXML_TAG = TokenSet.create(
          tOPENXMLTAG, tCLOSEXMLTAG, tXMLTAGPART, tBADCLOSEXMLTAG,
          ScalaXmlTokenTypes.XML_PI_START(),
          ScalaXmlTokenTypes.XML_PI_END(),
          ScalaXmlTokenTypes.XML_TAG_CHARACTERS(),
          ScalaXmlTokenTypes.XML_WHITE_SPACE()
  );

  static final TokenSet tXML_TAG_NAME = TokenSet.create(
          ScalaXmlTokenTypes.XML_TAG_NAME()
  );

  static final TokenSet tXML_TAG_DATA = TokenSet.create(
          ScalaXmlTokenTypes.XML_DATA_CHARACTERS(),
          ScalaXmlTokenTypes.XML_CDATA_START(),
          ScalaXmlTokenTypes.XML_CDATA_END()
  );

  static final TokenSet tXML_ATTRIBUTE_NAME = TokenSet.create(
          ScalaXmlTokenTypes.XML_ATTRIBUTE_NAME(),
          ScalaXmlTokenTypes.XML_EQ()
  );

  static final TokenSet tXML_ATTRIBUTE_VALUE = TokenSet.create(
          ScalaXmlTokenTypes.XML_ATTRIBUTE_VALUE_START_DELIMITER(),
          ScalaXmlTokenTypes.XML_ATTRIBUTE_VALUE_TOKEN(),
          ScalaXmlTokenTypes.XML_ATTRIBUTE_VALUE_END_DELIMITER()
  );

  static final TokenSet tXML_COMMENT = TokenSet.create(
          tXML_COMMENT_START, tXML_COMMENT_END,
          ScalaXmlTokenTypes.XML_COMMENT_START(),
          ScalaXmlTokenTypes.XML_COMMENT_END(),
          ScalaXmlTokenTypes.XML_COMMENT_CHARACTERS()
  );

  //Html escape sequences
  static final TokenSet tSCALADOC_HTML_ESCAPE = TokenSet.create(
      ScalaDocTokenType.DOC_HTML_ESCAPE_HIGHLIGHTED_ELEMENT
  );

  // ScalaXmlTokenTypes.XML tags in ScalaDoc
  static final TokenSet tSCALADOC_HTML_TAGS = TokenSet.create(
      ScalaXmlTokenTypes.XML_TAG_NAME(), ScalaXmlTokenTypes.XML_START_TAG_START(), ScalaXmlTokenTypes.XML_EMPTY_ELEMENT_END(),
      ScalaXmlTokenTypes.XML_END_TAG_START(), ScalaXmlTokenTypes.XML_TAG_END()
  );

  //ScalaDoc Wiki syntax elements
  static final TokenSet tSCALADOC_WIKI_SYNTAX = ScalaDocTokenType.ALL_SCALADOC_SYNTAX_ELEMENTS;

  //for value in @param value
  static final TokenSet tDOC_TAG_PARAM = TokenSet.create(ScalaDocTokenType.DOC_TAG_VALUE_TOKEN);

  // Variables
  static final TokenSet tVARIABLES = TokenSet.create(
          ScalaTokenTypes.tIDENTIFIER
  );

  // Numbers
  static final TokenSet tNUMBERS = TokenSet.create(
          ScalaTokenTypes.tINTEGER,
          ScalaTokenTypes.tFLOAT
  );

  // Braces
  static final TokenSet tBRACES = TokenSet.create(
          ScalaTokenTypes.tLBRACE,
          ScalaTokenTypes.tRBRACE
  );

  // Brackets
  static final TokenSet tBRACKETS = TokenSet.create(
          ScalaTokenTypes.tLSQBRACKET,
          ScalaTokenTypes.tRSQBRACKET
  );

  // Parentheses
  static final TokenSet tPARENTESES = TokenSet.create(
          ScalaTokenTypes.tLPARENTHESIS,
          ScalaTokenTypes.tRPARENTHESIS
  );


  // Strings
  static final TokenSet tSTRINGS = TokenSet.create(
          ScalaTokenTypes.tSTRING,
          ScalaTokenTypes.tMULTILINE_STRING,
          ScalaTokenTypes.tWRONG_STRING,
          ScalaTokenTypes.tCHAR,
          ScalaTokenTypes.tSYMBOL,
          ScalaTokenTypes.tINTERPOLATED_MULTILINE_STRING,
          ScalaTokenTypes.tINTERPOLATED_STRING,
          ScalaTokenTypes.tINTERPOLATED_STRING_ID,
          ScalaTokenTypes.tINTERPOLATED_STRING_END
  );

  static final TokenSet tINTERPOLATED_STRINGS = TokenSet.create(
      ScalaTokenTypes.tINTERPOLATED_STRING_INJECTION
  );

  // Valid escape in string
  static final TokenSet tVALID_STRING_ESCAPE = TokenSet.create(
          StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN, ScalaTokenTypes.tINTERPOLATED_STRING_ESCAPE
  );

  // Invalid character escape in string
  static final TokenSet tINVALID_CHARACTER_ESCAPE = TokenSet.create(
          StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN
  );

  // Invalid unicode escape in string
  static final TokenSet tINVALID_UNICODE_ESCAPE = TokenSet.create(
          StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN
  );

  // Keywords
  public static final TokenSet kRESWORDS = TokenSet.create(
          ScalaTokenTypes.kABSTRACT,
          ScalaTokenTypes.kCASE,
          ScalaTokenTypes.kCATCH,
          ScalaTokenTypes.kCLASS,
          ScalaTokenTypes.kDEF,
          ScalaTokenTypes.kDO,
          ScalaTokenTypes.kELSE,
          ScalaTokenTypes.kEXTENDS,
          ScalaTokenTypes.kFALSE,
          ScalaTokenTypes.kFINAL,
          ScalaTokenTypes.kFINALLY,
          ScalaTokenTypes.kFOR,
          ScalaTokenTypes.kFOR_SOME,
          ScalaTokenTypes.kIF,
          ScalaTokenTypes.kIMPLICIT,
          ScalaTokenTypes.kIMPORT,
          ScalaTokenTypes.kLAZY,
          ScalaTokenTypes.kMATCH,
          ScalaTokenTypes.kNEW,
          ScalaTokenTypes.kNULL,
          ScalaTokenTypes.kOBJECT,
          ScalaTokenTypes.kOVERRIDE,
          ScalaTokenTypes.kPACKAGE,
          ScalaTokenTypes.kPRIVATE,
          ScalaTokenTypes.kPROTECTED,
          ScalaTokenTypes.kRETURN,
          ScalaTokenTypes.kSEALED,
          ScalaTokenTypes.kSUPER,
          ScalaTokenTypes.kTHIS,
          ScalaTokenTypes.kTHROW,
          ScalaTokenTypes.kTRAIT,
          ScalaTokenTypes.kTRY,
          ScalaTokenTypes.kTRUE,
          ScalaTokenTypes.kTYPE,
          ScalaTokenTypes.kVAL,
          ScalaTokenTypes.kVAR,
          ScalaTokenTypes.kWHILE,
          ScalaTokenTypes.kWITH,
          ScalaTokenTypes.kYIELD,
          ScalaTokenTypes.kMACRO
  );

  static final TokenSet tOPS = TokenSet.create(
          ScalaTokenTypes.tASSIGN
  );

  static final TokenSet tARROW = TokenSet.create(
          ScalaTokenTypes.tFUNTYPE
  );

  static final TokenSet tSEMICOLON = TokenSet.create(
          ScalaTokenTypes.tSEMICOLON
  );

  static final TokenSet tDOT = TokenSet.create(
          ScalaTokenTypes.tDOT
  );

  static final TokenSet tCOMMA = TokenSet.create(
          ScalaTokenTypes.tCOMMA
  );

  //ScalaDoc comment tags like @see
  static final TokenSet tCOMMENT_TAGS = TokenSet.create(
      ScalaDocTokenType.DOC_TAG_NAME
  );

  private static final Map<IElementType, TextAttributesKey> ATTRIBUTES0 = new HashMap<IElementType, TextAttributesKey>();
  private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<IElementType, TextAttributesKey>();
  private boolean treatDocCommentAsBlockComment;

  public ScalaSyntaxHighlighter(boolean treatDocCommentAsBlockComment) {
    this.treatDocCommentAsBlockComment = treatDocCommentAsBlockComment;
  }

  public ScalaSyntaxHighlighter() {
    this(false);
  }

  static {
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tLINE_COMMENTS, DefaultHighlighter.LINE_COMMENT);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tBLOCK_COMMENTS, DefaultHighlighter.BLOCK_COMMENT);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tDOC_COMMENTS, DefaultHighlighter.DOC_COMMENT);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, kRESWORDS, DefaultHighlighter.KEYWORD);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tNUMBERS, DefaultHighlighter.NUMBER);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tVALID_STRING_ESCAPE, DefaultHighlighter.VALID_STRING_ESCAPE);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tINVALID_CHARACTER_ESCAPE, DefaultHighlighter.INVALID_STRING_ESCAPE);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tINVALID_UNICODE_ESCAPE, DefaultHighlighter.INVALID_STRING_ESCAPE);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tSTRINGS, DefaultHighlighter.STRING);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tBRACES, DefaultHighlighter.BRACES);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tBRACKETS, DefaultHighlighter.BRACKETS);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tPARENTESES, DefaultHighlighter.PARENTHESES);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tSEMICOLON, DefaultHighlighter.SEMICOLON);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tDOT, DefaultHighlighter.DOT);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tCOMMA, DefaultHighlighter.COMMA);

    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tOPS, DefaultHighlighter.ASSIGN);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tARROW, DefaultHighlighter.ARROW);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tCOMMENT_TAGS, DefaultHighlighter.SCALA_DOC_TAG);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, TokenSet.orSet(TokenSet.andNot(ScalaDocTokenType.ALL_SCALADOC_TOKENS, tCOMMENT_TAGS),
        TokenSet.create(ScalaDocTokenType.DOC_COMMENT_BAD_CHARACTER,
        ScalaDocTokenType.DOC_HTML_ESCAPE_HIGHLIGHTED_ELEMENT)), DefaultHighlighter.DOC_COMMENT);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tSCALADOC_HTML_TAGS, DefaultHighlighter.SCALA_DOC_HTML_TAG);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tSCALADOC_WIKI_SYNTAX, DefaultHighlighter.SCALA_DOC_WIKI_SYNTAX);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tSCALADOC_HTML_ESCAPE, DefaultHighlighter.SCALA_DOC_HTML_ESCAPE);

    SyntaxHighlighterBase.fillMap(ATTRIBUTES0, tXML_TAG, DefaultHighlighter.XML_TAG);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES0, tXML_TAG_NAME, DefaultHighlighter.XML_TAG);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES0, tXML_ATTRIBUTE_NAME, DefaultHighlighter.XML_TAG);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES0, tXML_ATTRIBUTE_VALUE, DefaultHighlighter.XML_TAG);

    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tXML_TAG_NAME, DefaultHighlighter.XML_TAG_NAME);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tXML_TAG_DATA, DefaultHighlighter.XML_TAG_DATA);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tXML_ATTRIBUTE_NAME, DefaultHighlighter.XML_ATTRIBUTE_NAME);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tXML_ATTRIBUTE_VALUE, DefaultHighlighter.XML_ATTRIBUTE_VALUE);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tXML_COMMENT, DefaultHighlighter.XML_COMMENT);

    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tDOC_TAG_PARAM, DefaultHighlighter.SCALA_DOC_TAG_PARAM_VALUE);
    SyntaxHighlighterBase.fillMap(ATTRIBUTES, tINTERPOLATED_STRINGS, DefaultHighlighter.INTERPOLATED_STRING_INJECTION);
  }


  @NotNull
  public Lexer getHighlightingLexer() {
    return new CompoundLexer(treatDocCommentAsBlockComment);
  }

  private static class CompoundLexer extends LayeredLexer {
    CompoundLexer(boolean treatDocCommentAsBlockComment) {
      super(new CustomScalaLexer(treatDocCommentAsBlockComment));

      registerSelfStoppingLayer(new StringLiteralLexer('\"', ScalaTokenTypes.tSTRING),
                                new IElementType[]{ScalaTokenTypes.tSTRING}, IElementType.EMPTY_ARRAY);

      registerSelfStoppingLayer(new StringLiteralLexer('\'', ScalaTokenTypes.tSTRING),
          new IElementType[]{ScalaTokenTypes.tCHAR}, IElementType.EMPTY_ARRAY);

      //interpolated string highlighting
      registerLayer(new LayeredLexer(new StringLiteralLexer(StringLiteralLexer.NO_QUOTE_CHAR, ScalaTokenTypes.tINTERPOLATED_STRING)),
          ScalaTokenTypes.tINTERPOLATED_STRING);

      //scaladoc highlighting
      LayeredLexer scalaDocLexer = new LayeredLexer(new ScalaDocLexerHighlightingWrapper());
      scalaDocLexer.registerLayer(new ScalaHtmlHighlightingLexerWrapper(), ScalaDocTokenType.DOC_COMMENT_DATA);

      registerSelfStoppingLayer(scalaDocLexer, new IElementType[]{ScalaDocElementTypes.SCALA_DOC_COMMENT},
          IElementType.EMPTY_ARRAY);
    }
  }

  private static class CustomScalaLexer extends ScalaLexer {
    private Stack<String> openingTags = new Stack<String>();
    private boolean tagMatch = false;
    private boolean isInClosingTag = false;
    private boolean afterStartTagStart = false;
    private int nameIndex = 0;

    public CustomScalaLexer(boolean treatDocCommentAsBlockComment) {
      super(treatDocCommentAsBlockComment);
    }

    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
      myCurrentLexer = myScalaPlainLexer;
      myCurrentLexer.start(buffer, startOffset, endOffset, initialState);
      myBraceStack.clear();
      myLayeredTagStack.clear();
      myXmlState = 0;
      myBuffer = buffer;
      myBufferEnd = endOffset;
      myTokenType = null;
      openingTags = new Stack<String>();
      tagMatch = false;
      isInClosingTag = false;
      afterStartTagStart = false;
      nameIndex = 0;
    }

    public IElementType getTokenType() {
      IElementType type = super.getTokenType();

      if (type == TokenType.WHITE_SPACE) {
        return ScalaXmlTokenTypes.XML_WHITE_SPACE();
      } else if (type == ScalaXmlTokenTypes.XML_START_TAG_START()) {
        return tOPENXMLTAG;
      } else if (type == ScalaXmlTokenTypes.XML_TAG_END() && isInClosingTag) {
        return tagMatch ? tCLOSEXMLTAG : tBADCLOSEXMLTAG;
      } else if (type == ScalaXmlTokenTypes.XML_NAME()) {
        return nameIndex == 0 ? ScalaXmlTokenTypes.XML_TAG_NAME() : ScalaXmlTokenTypes.XML_ATTRIBUTE_NAME();
      } else if (type == ScalaXmlTokenTypes.XML_EMPTY_ELEMENT_END()) {
        return tCLOSEXMLTAG;
      } else if (tSCALADOC_HTML_TAGS.contains(type)) {
        return tXMLTAGPART;
      } else if (type == ScalaXmlTokenTypes.XML_COMMENT_START()) {
        return tXML_COMMENT_START;
      } else if (type == ScalaXmlTokenTypes.XML_COMMENT_END()) {
        return tXML_COMMENT_END;
      }

      return type;
    }

    @Override
    public void advance() {
      IElementType type = super.getTokenType();
      String tokenText = getTokenText();
      super.advance();

      if (type == ScalaXmlTokenTypes.XML_NAME()) {
        nameIndex++;
      } else if (type == ScalaXmlTokenTypes.XML_TAG_END() || type == ScalaXmlTokenTypes.XML_EMPTY_ELEMENT_END()) {
        nameIndex = 0;
      }

      if (type == ScalaXmlTokenTypes.XML_END_TAG_START()) {
        isInClosingTag = true;
      } else if (type == ScalaXmlTokenTypes.XML_EMPTY_ELEMENT_END()) {
        if (!openingTags.empty()) {
          openingTags.pop();
        }
      } else if (type == ScalaXmlTokenTypes.XML_TAG_END() && isInClosingTag) {
        isInClosingTag = false;
        if (tagMatch) {
          openingTags.pop();
        }
      } else if (type == ScalaXmlTokenTypes.XML_NAME() && (afterStartTagStart || isInClosingTag)) {
        if (!isInClosingTag) openingTags.push(tokenText); else tagMatch = !openingTags.empty() && openingTags.peek().equals(tokenText);
        afterStartTagStart = false;
      } else if (type == ScalaXmlTokenTypes.XML_START_TAG_START()) {
        afterStartTagStart = true;
      }
    }
  }

  public static class ScalaHtmlHighlightingLexerWrapper extends HtmlHighlightingLexer {
    public ScalaHtmlHighlightingLexerWrapper() {
      super(FileTypeManager.getInstance().getStdFileType("CSS"));
    }

    @Override
    public IElementType getTokenType() {
      final IElementType htmlType = ScalaXmlTokenTypes.substitute(super.getTokenType());

      if (htmlType == ScalaXmlTokenTypes.XML_CHAR_ENTITY_REF()) {
        return ScalaDocTokenType.DOC_HTML_ESCAPE_HIGHLIGHTED_ELEMENT;
      } else if (htmlType == ScalaXmlTokenTypes.XML_DATA_CHARACTERS() || htmlType == ScalaXmlTokenTypes.XML_BAD_CHARACTER() ||
          ScalaXmlTokenTypes.XML_COMMENTS().contains(htmlType)) {
        return ScalaDocTokenType.DOC_COMMENT_DATA;
      }

      return htmlType;
    }
  }

  private static class ScalaDocLexerHighlightingWrapper extends ScalaDocLexer {
    private static TokenSet SYNTAX_TO_SWAP = TokenSet.andNot(tSCALADOC_WIKI_SYNTAX, TokenSet.create(ScalaDocTokenType.DOC_LINK_TAG,
        ScalaDocTokenType.DOC_LINK_CLOSE_TAG, ScalaDocTokenType.DOC_HTTP_LINK_TAG, ScalaDocTokenType.DOC_INNER_CODE_TAG,
        ScalaDocTokenType.DOC_INNER_CLOSE_CODE_TAG));

    private Stack<IElementType> elements = new Stack<IElementType>();

    @Override
    public IElementType getTokenType() {
      IElementType tokenType = super.getTokenType();

      if (tokenType == ScalaTokenTypes.tDOT || tokenType == tIDENTIFIER) {
        return ScalaDocTokenType.DOC_COMMENT_DATA;
      } else if (!elements.isEmpty() && elements.peek() == ScalaDocTokenType.DOC_COMMON_CLOSE_WIKI_TAG) {
        return ScalaDocTokenType.DOC_COMMON_CLOSE_WIKI_TAG;
      }

      return tokenType;
    }

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
      elements.clear();
      super.start(buffer, startOffset, endOffset, initialState);
    }

    @Override
    public void advance() {
      super.advance();
      if (!elements.isEmpty() && elements.peek() == ScalaDocTokenType.DOC_COMMON_CLOSE_WIKI_TAG) {
        elements.pop();
        elements.pop(); //will never be empty there
      }

      IElementType token = super.getTokenType();

      if (SYNTAX_TO_SWAP.contains(token)) {
        if (elements.isEmpty() || elements.peek() != token) {
          elements.push(token);
        } else {
          elements.push(ScalaDocTokenType.DOC_COMMON_CLOSE_WIKI_TAG);
        }
      }
    }
  }
  
  @NotNull
  public TextAttributesKey[] getTokenHighlights(IElementType iElementType) {
    return pack(ATTRIBUTES0.get(iElementType), ATTRIBUTES.get(iElementType));
  }
}
