<!-- Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
     for details. All rights reserved. Use of this source code is governed by a
     BSD-style license that can be found in the LICENSE file. -->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="text"/>

  <xsl:template match="GUIDE">
    <xsl:apply-templates select="//DART_CODE_SNIPPET"/>
  </xsl:template>

  <xsl:template match="//OBSOLETE//DART_CODE_SNIPPET"/>
  <xsl:template match="//NO_GRAMMAR_CHECK//DART_CODE_SNIPPET"/>
  <xsl:template match="OBSOLETE" mode="dart_untyped"/>
  <xsl:template match="NO_GRAMMAR_CHECK" mode="dart_typed"/>
  <xsl:template match="OBSOLETE" mode="dart_untyped"/>
  <xsl:template match="NO_GRAMMAR_CHECK" mode="dart_typed"/>

  <xsl:template match="INT" mode="dart_typed">int </xsl:template>
  <xsl:template match="INT" mode="dart_untyped"><xsl:value-of select="@alt"/></xsl:template>
  <xsl:template match="VINT" mode="dart_typed">int</xsl:template>
  <xsl:template match="VINT" mode="dart_untyped">var</xsl:template>
  <xsl:template match="DOUBLE" mode="dart_typed">double </xsl:template>
  <xsl:template match="DOUBLE" mode="dart_untyped"><xsl:value-of select="@alt"/></xsl:template>
  <xsl:template match="VDOUBLE" mode="dart_typed">double</xsl:template>
  <xsl:template match="VDOUBLE" mode="dart_untyped">var</xsl:template>
  <xsl:template match="BOOL" mode="dart_typed">bool </xsl:template>
  <xsl:template match="BOOL" mode="dart_untyped"><xsl:value-of select="@alt"/></xsl:template>
  <xsl:template match="VBOOL" mode="dart_typed">bool</xsl:template>
  <xsl:template match="VBOOL" mode="dart_untyped">var</xsl:template>
  <xsl:template match="STRING" mode="dart_typed">String </xsl:template>
  <xsl:template match="STRING" mode="dart_untyped"><xsl:value-of select="@alt"/></xsl:template>
  <xsl:template match="VSTRING" mode="dart_typed">String</xsl:template>
  <xsl:template match="VSTRING" mode="dart_untyped">var</xsl:template>
  <xsl:template match="NUMBER" mode="dart_typed">Number </xsl:template>
  <xsl:template match="NUMBER" mode="dart_untyped"><xsl:value-of select="@alt"/></xsl:template>
  <xsl:template match="VNUMBER" mode="dart_typed">Number</xsl:template>
  <xsl:template match="VNUMBER" mode="dart_untyped">var</xsl:template>
  <xsl:template match="OBJECT" mode="dart_typed">Object </xsl:template>
  <xsl:template match="OBJECT" mode="dart_untyped"><xsl:value-of select="@alt"/></xsl:template>
  <xsl:template match="VOBJECT" mode="dart_typed">Object</xsl:template>
  <xsl:template match="VOBJECT" mode="dart_untyped">var</xsl:template>
  <xsl:template match="VOID" mode="dart_typed">void </xsl:template>
  <xsl:template match="VOID" mode="dart_untyped"></xsl:template>
  <xsl:template match="T" mode="dart_typed"><xsl:value-of select="@t"/><xsl:apply-templates/></xsl:template>
  <xsl:template match="T" mode="dart_untyped"><xsl:value-of select="@alt"/></xsl:template>
  <xsl:template match="TYPE" mode="dart_typed"><xsl:value-of select="@t"/><xsl:apply-templates/></xsl:template>
  <xsl:template match="TYPE" mode="dart_untyped"><xsl:value-of select="@alt"/></xsl:template>
  <xsl:template match="V" mode="dart_typed"><xsl:value-of select="@t"/><xsl:apply-templates/></xsl:template>
  <xsl:template match="V" mode="dart_untyped">var</xsl:template>

  <xsl:template match="STATEMENTS" mode="dart_untyped">
    class $ {
      $() {
      <xsl:apply-templates mode="dart_untyped"/>
      }
    }
  </xsl:template>
  <xsl:template match="STATEMENTS" mode="dart_typed">
    class $ {
      $() {
      <xsl:apply-templates mode="dart_typed"/>
      }
    }
  </xsl:template>

  <xsl:template match="EXPRESSION" mode="dart_untyped">
    class $ {
      $() {
        return <xsl:apply-templates mode="dart_untyped"/>
            ;
      }
    }
  </xsl:template>
  <xsl:template match="EXPRESSION" mode="dart_typed">
    class $ {
      $() {
        return <xsl:apply-templates mode="dart_typed"/>
            ;
      }
    }
  </xsl:template>

  <xsl:template match="DART_CODE_SNIPPET">
    <xsl:value-of select="'// Typed&#xA;'"/>
    <xsl:call-template name="print_without_leading_chars">
      <xsl:with-param name="text">
        <xsl:apply-templates mode="dart_typed"/>
      </xsl:with-param>
      <xsl:with-param name="strip" select="1"/>
      <xsl:with-param name="is_firstline" select="1"/>
      <xsl:with-param name="trim_count">
        <xsl:call-template name="num_leading_spaces">
          <xsl:with-param name="max_so_far" select="1000"/>
          <xsl:with-param name="text">
            <xsl:apply-templates mode="dart_typed"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:value-of select="'&#xA;&#xA;'"/>
    <xsl:value-of select="'// Untyped&#xA;'"/>
    <xsl:call-template name="print_without_leading_chars">
      <xsl:with-param name="text">
        <xsl:apply-templates mode="dart_untyped"/>
      </xsl:with-param>
      <xsl:with-param name="strip" select="1"/>
      <xsl:with-param name="is_firstline" select="1"/>
      <xsl:with-param name="trim_count">
        <xsl:call-template name="num_leading_spaces">
          <xsl:with-param name="max_so_far" select="1000"/>
          <xsl:with-param name="text">
            <xsl:apply-templates mode="dart_untyped"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:value-of select="'&#xA;&#xA;'"/>
  </xsl:template>

  <!-- Given text, evaluates to the number of leading spaces. -->
  <xsl:template name="num_leading_spaces_one_line">
    <xsl:param name="text"/>
    <xsl:param name="current_count"/>
    <xsl:choose>
      <xsl:when test="starts-with($text, ' ')">
        <xsl:call-template name="num_leading_spaces_one_line">
          <xsl:with-param name="text" select="substring($text, 2)"/>
          <xsl:with-param name="current_count" select="$current_count + 1"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$current_count"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Given a block of text, each line terminated by \n, evaluates to
       the indentation-level of that text; that is, the largest number
       n such that every non-blank line starts with at least n spaces. -->
  <xsl:template name="num_leading_spaces">
    <xsl:param name="text"/>
    <xsl:param name="max_so_far"/>
    <!-- TODO(csilvers): deal with case text doesn't end in a newline -->
    <xsl:variable name="line" select="substring-before($text, '&#xA;')"/>
    <xsl:variable name="rest" select="substring-after($text, '&#xA;')"/>
    <xsl:variable name="num_spaces_this_line">
      <xsl:choose>
        <xsl:when test="$line=''">
           <xsl:value-of select="$max_so_far"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="num_leading_spaces_one_line">
            <xsl:with-param name="text" select="$line"/>
            <xsl:with-param name="current_count" select="0"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="new_max_so_far">
       <xsl:choose>
         <xsl:when test="$num_spaces_this_line &lt; $max_so_far">
           <xsl:value-of select="$num_spaces_this_line"/>
         </xsl:when>
         <xsl:otherwise>
           <xsl:value-of select="$max_so_far"/>
         </xsl:otherwise>
       </xsl:choose>
    </xsl:variable>
    <!-- now check if we're on the last line, and if not, recurse -->
    <xsl:if test="$rest=''">
      <xsl:value-of select="$new_max_so_far"/>
    </xsl:if>
    <xsl:if test="not($rest='')">
      <xsl:call-template name="num_leading_spaces">
        <xsl:with-param name="text" select="$rest"/>
        <xsl:with-param name="max_so_far" select="$new_max_so_far"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <!-- Given a block of text, each line terminated by \n, and a number n,
       emits the text with the first n characters of each line
       deleted.  If strip==1, then we omit blank lines at the beginning
       and end of the text (but not the middle!) -->
  <xsl:template name="print_without_leading_chars">
    <xsl:param name="text"/>
    <xsl:param name="trim_count"/>
    <xsl:param name="strip"/>
    <xsl:param name="is_firstline"/>
    <!-- TODO(csilvers): deal with case text doesn't end in a newline -->
    <xsl:variable name="line" select="substring-before($text, '&#xA;')"/>
    <xsl:variable name="rest" select="substring-after($text, '&#xA;')"/>
    <xsl:variable name="stripped_line" select="substring($line, $trim_count+1)"/>
    <xsl:choose>
      <!-- $line (or $rest) is considered empty if we'd trim the entire line -->
      <xsl:when test="($strip = '1') and ($is_firstline = '1') and
                      (string-length($line) &lt;= $trim_count)">
      </xsl:when>
      <xsl:when test="($strip = '1') and
                      (string-length($rest) &lt;= $trim_count)">
        <xsl:value-of select="$stripped_line"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$stripped_line"/>
        <xsl:text>&#xA;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="not($rest='')">
      <xsl:call-template name="print_without_leading_chars">
        <xsl:with-param name="text" select="$rest"/>
        <xsl:with-param name="trim_count" select="$trim_count"/>
        <xsl:with-param name="strip" select="$strip"/>
        <xsl:with-param name="is_firstline" select="0"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
