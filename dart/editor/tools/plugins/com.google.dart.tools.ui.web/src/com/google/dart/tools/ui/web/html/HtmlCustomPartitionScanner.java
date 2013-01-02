package com.google.dart.tools.ui.web.html;

import com.google.dart.tools.core.html.HtmlParser;
import com.google.dart.tools.core.html.XmlDocument;
import com.google.dart.tools.core.html.XmlNode;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HtmlCustomPartitionScanner implements IPartitionTokenScanner {
  private static class HtmlToken {
    private IToken token;
    private HtmlToken next;
    private int offset;
    private int length;

    public HtmlToken() {

    }

    public HtmlToken(IToken token, int offset, int length) {
      this.token = token;
      this.offset = offset;
      this.length = length;
    }

    @Override
    public String toString() {
      return "[" + token.getData() + "," + offset + "," + length + "]";
    }
  }

  private static IToken DEFAULT_TOKEN = new Token(null);
  private static IToken COMMENT_TOKEN = new Token(HtmlEditor.HTML_COMMENT_PARTITION);
  private static IToken BRACKET_TOKEN = new Token(HtmlEditor.HTML_BRACKET_PARTITION);
  private static IToken STYLE_TOKEN = new Token(HtmlEditor.HTML_STYLE_PARTITION);
  private static IToken CODE_TOKEN = new Token(HtmlEditor.HTML_CODE_PARTITION);

  private HtmlToken root;
  private HtmlToken current;

  public HtmlCustomPartitionScanner() {

  }

  @Override
  public int getTokenLength() {
    return current.length;
  }

  @Override
  public int getTokenOffset() {
    return current.offset;
  }

  @Override
  public IToken nextToken() {
    if (current == null) {
      current = root;
    } else {
      current = current.next;
    }

    return current.token;
  }

  @Override
  public void setPartialRange(IDocument document, int offset, int length, String contentType,
      int partitionOffset) {
    setRange(document, partitionOffset, length + offset - partitionOffset);
  }

  @Override
  public void setRange(IDocument document, int offset, int length) {
    current = null;

    if (offset != 0 || length != document.getLength()) {
      root = trimTokenData(parse(document.get(), offset), offset, length);
    } else {
      root = parse(document.get(), 0);
    }

    //print(root);
  }

  private HtmlToken convertToLinkedList(List<HtmlToken> tokens, int docLength) {
    Collections.sort(tokens, new Comparator<HtmlToken>() {
      @Override
      public int compare(HtmlToken one, HtmlToken two) {
        return one.offset - two.offset;
      }
    });

    // set up the next pointers and add any necessary default tokens
    HtmlToken fakeHead = new HtmlToken();
    HtmlToken current = fakeHead;
    int lastEmitted = 0;

    for (HtmlToken token : tokens) {
      if (lastEmitted < token.offset) {
        current.next = new HtmlToken(DEFAULT_TOKEN, lastEmitted, token.offset - lastEmitted);
        lastEmitted = token.offset;
        current = current.next;
      }

      current.next = token;

      current = token;

      lastEmitted = token.offset + token.length;
    }

    if (lastEmitted < docLength) {
      current.next = new HtmlToken(DEFAULT_TOKEN, lastEmitted, docLength - lastEmitted);
      current = current.next;
      lastEmitted = current.offset;
    }

    current.next = new HtmlToken(Token.EOF, docLength, 0);

    return fakeHead.next;
  }

  private HtmlToken convertToTokens(XmlDocument document, int length) {
    List<HtmlToken> tokens = createTokensFrom(document);

    return convertToLinkedList(tokens, length);
  }

  private List<HtmlToken> createTokensFrom(XmlDocument document) {
    List<HtmlToken> tokens = new ArrayList<HtmlToken>();

    for (XmlNode node : document.getChildren()) {
      createTokensFrom(node, tokens);
    }

    return tokens;
  }

  private void createTokensFrom(XmlNode node, List<HtmlToken> tokens) {
    boolean recurse = true;

    if (node.isComment()) {
      tokens.add(new HtmlToken(
          COMMENT_TOKEN,
          node.getStartToken().getLocation(),
          node.getEndOffset() - node.getStartOffset()));
    } else {
      tokens.add(new HtmlToken(BRACKET_TOKEN, node.getStartOffset(), node.getEndOffset()
          - node.getStartOffset()));

      if (node.getEndNode() != null) {
        XmlNode endNode = node.getEndNode();

        tokens.add(new HtmlToken(BRACKET_TOKEN, endNode.getStartOffset(), endNode.getEndOffset()
            - endNode.getStartOffset()));

        if ("style".equals(node.getLabel())) {
          recurse = false;

          tokens.add(new HtmlToken(STYLE_TOKEN, node.getEndOffset(), endNode.getStartOffset()
              - node.getEndOffset()));
        } else if ("script".equals(node.getLabel())) {
          recurse = false;

          tokens.add(new HtmlToken(CODE_TOKEN, node.getEndOffset(), endNode.getStartOffset()
              - node.getEndOffset()));
        }
      }
    }

    if (recurse) {
      for (XmlNode n : node.getChildren()) {
        createTokensFrom(n, tokens);
      }
    }
  }

  private HtmlToken parse(String str, int offset) {
    HtmlParser parser = new HtmlParser(str);

    return convertToTokens(parser.parse(), str.length());
  }

  @SuppressWarnings("unused")
  private void print(HtmlToken token) {
    while (token != null) {
      System.out.println(token);
      token = token.next;
    }
  }

  /**
   * Adjust the linked list of tokens so that only those that encompass the given range of
   * characters will be returned.
   * 
   * @param offset the offset of the first character to be included in a token
   * @param length the number of characters to be included in tokens
   */
  private HtmlToken trimTokenData(HtmlToken start, int offset, int length) {
    // Skip over any tokens that should not be returned. currentToken is assumed to be the fake
    // token created before the first real token.
    HtmlToken nextToken = start;
    while (nextToken != nextToken.next && nextToken.next.offset <= offset) {
      nextToken = nextToken.next;
    }
    start = nextToken;

    // Trim the tail of the list to cover only the requested length.
    int totalLength = nextToken.length - (offset - nextToken.offset);
    while (nextToken.next != null && totalLength < length) {
      nextToken = nextToken.next;
      totalLength += nextToken.length;
    }

    if (totalLength > length) {
      HtmlToken lastToken = nextToken.next;
      while (lastToken.next != null) {
        lastToken = lastToken.next;
      }
      nextToken.next = lastToken;
    }

    return start;
  }

}
