/*
Copyright (c) 2005, Pete Bevin.
<http://markdownj.petebevin.com>

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

* Neither the name "Markdown" nor the names of its contributors may
  be used to endorse or promote products derived from this software
  without specific prior written permission.

This software is provided by the copyright holders and contributors "as
is" and any express or implied warranties, including, but not limited
to, the implied warranties of merchantability and fitness for a
particular purpose are disclaimed. In no event shall the copyright owner
or contributors be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to,
procurement of substitute goods or services; loss of use, data, or
profits; or business interruption) however caused and on any theory of
liability, whether in contract, strict liability, or tort (including
negligence or otherwise) arising in any way out of the use of this
software, even if advised of the possibility of such damage.

*/

package com.petebevin.markdown;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert Markdown text into HTML, as per http://daringfireball.net/projects/markdown/ .
 * Usage:
 * <pre><code>
 *     MarkdownProcessor markdown = new MarkdownProcessor();
 *     String html = markdown.markdown("*italic*   **bold**\n_italic_   __bold__");
 * </code></pre>
 */
public class MarkdownProcessor {
    private Random rnd = new Random();
    private Map linkDefinitions = new TreeMap();
    private static final CharacterProtector HTML_PROTECTOR = new CharacterProtector();
    private static final CharacterProtector CHAR_PROTECTOR = new CharacterProtector();
    private int listLevel;

    /**
     * Creates a new Markdown processor.
     */
    public MarkdownProcessor() {
        listLevel = 0;
    }

    /**
     * Perform the conversion from Markdown to HTML.
     *
     * @param txt - input in markdown format
     * @return HTML block corresponding to txt passed in.
     */
    public String markdown(String txt) {
        if (txt == null) {
            txt = "";
        }
        TextEditor text = new TextEditor(txt);
        text.replaceAll("\\r\\n", "\n");
        text.replaceAll("\\r", "\n");
        text.replaceAll("^[ \\t]+$", "");
        text.append("\n\n");
        text.detabify();
        text.deleteAll("^[ ]+$");
        hashHTMLBlocks(text);
        stripLinkDefinitions(text);
        text = runBlockGamut(text);
        unEscapeSpecialChars(text);

        text.append("\n");
        return text.toString();
    }

    private TextEditor encodeBackslashEscapes(TextEditor text) {
        char[] normalChars = "`_>!".toCharArray();
        char[] escapedChars = "*{}[]()#+-.".toCharArray();

        // Two backslashes in a row
        text.replaceAllLiteral("\\\\\\\\", CHAR_PROTECTOR.encode("\\"));

        // Normal characters don't require a backslash in the regular expression
        encodeEscapes(text, normalChars, "\\\\");
        encodeEscapes(text, escapedChars, "\\\\\\");

        return text;
    }

    private TextEditor encodeEscapes(TextEditor text, char[] chars, String slashes) {
        for (int i = 0; i < chars.length; i++) {
            String ch = String.valueOf(chars[i]);
            String regex = slashes + ch;
            text.replaceAllLiteral(regex, CHAR_PROTECTOR.encode(ch));
        }
        return text;
    }

    private void stripLinkDefinitions(TextEditor text) {
        Pattern p = Pattern.compile("^[ ]{0,3}\\[(.+)\\]:" + // ID = $1
                "[ \\t]*\\n?[ \\t]*" + // Space
                "<?(\\S+?)>?" + // URL = $2
                "[ \\t]*\\n?[ \\t]*" + // Space
                "(?:[\"(](.+?)[\")][ \\t]*)?" + // Optional title = $3
                "(?:\\n+|\\Z)",
                Pattern.MULTILINE);

        text.replaceAll(p, new Replacement() {
            public String replacement(Matcher m) {
                String id = m.group(1).toLowerCase();
                String url = encodeAmpsAndAngles(new TextEditor(m.group(2))).toString();
                String title = m.group(3);
                if (title == null) {
                    title = "";
                }
                title = replaceAll(title, "\"", "&quot;");
                linkDefinitions.put(id, new LinkDefinition(url, title));
                return "";
            }
        });
    }

    public TextEditor runBlockGamut(TextEditor text) {
        doHeaders(text);
        doHorizontalRules(text);
        doLists(text);
        doCodeBlocks(text);
        doBlockQuotes(text);

        hashHTMLBlocks(text);

        return formParagraphs(text);
    }

    private void doHorizontalRules(TextEditor text) {
        String[] hrDelimiters = {"\\*", "-", "_"};
        for (int i = 0; i < hrDelimiters.length; i++) {
            String hrDelimiter = hrDelimiters[i];
            text.replaceAll("^[ ]{0,2}([ ]?" + hrDelimiter + "[ ]?){3,}[ ]*$", "<hr />");
        }
    }

    private void hashHTMLBlocks(TextEditor text) {
        // Hashify HTML blocks:
        // We only want to do this for block-level HTML tags, such as headers,
        // lists, and tables. That's because we still want to wrap <p>s around
        // "paragraphs" that are wrapped in non-block-level tags, such as anchors,
        // phrase emphasis, and spans. The list of tags we're looking for is
        // hard-coded:

        String[] tagsA = {
            "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "table",
            "dl", "ol", "ul", "script", "noscript", "form", "fieldset", "iframe", "math"
        };
        String[] tagsB = {"ins", "del"};

        String alternationA = join("|", tagsA);
        String alternationB = alternationA + "|" + join("|", tagsB);

        // First, look for nested blocks, e.g.:
        //   <div>
        //       <div>
        //       tags for inner block must be indented.
        //       </div>
        //   </div>
        //
        // The outermost tags must start at the left margin for this to match, and
        // the inner nested divs must be indented.
        // We need to do this before the next, more liberal match, because the next
        // match will start at the first `<div>` and stop at the first `</div>`.
        Pattern p1 = Pattern.compile("(" +
                "^<(" + alternationA + ")" +
                "\\b" +
                "(.*\\n)*?" +
                "</\\2>" +
                "[ ]*" +
                "(?=\\n+|\\Z))", Pattern.MULTILINE);

        Replacement protectHTML = new Replacement() {
            public String replacement(Matcher m) {
                String literal = m.group();
                return "\n\n" + HTML_PROTECTOR.encode(literal) + "\n\n";
            }
        };
        text.replaceAll(p1, protectHTML);

        // Now match more liberally, simply from `\n<tag>` to `</tag>\n`
        Pattern p2 = Pattern.compile("(" +
                "^" +
                "<(" + alternationB + ")" +
                "\\b" +
                "(.*\\n)*?" +
                ".*</\\2>" +
                "[ ]*" +
                "(?=\\n+|\\Z))", Pattern.MULTILINE);
        text.replaceAll(p2, protectHTML);

        // Special case for <hr>
        Pattern p3 = Pattern.compile("(?:" +
                "(?<=\\n\\n)" +
                "|" +
                "\\A\\n?" +
                ")" +
                "(" +
                "[ ]{0,3}" + // TODO
                "<(hr)" +
                "\\b" +
                "([^<>])*?" +
                "/?>" +
                "[ ]*" +
                "(?=\\n{2,}|\\Z))");
        text.replaceAll(p3, protectHTML);

        // Special case for standalone HTML comments:
        Pattern p4 = Pattern.compile("(?:" +
                "(?<=\\n\\n)" +
                "|" +
                "\\A\\n?" +
                ")" +
                "(" +
                "[ ]{0,3}" + // TODO
                "(?s:" +
                "<!" +
                "(--.*?--\\s*)+" +
                ">" +
                ")" +
                "[ ]*" +
                "(?=\\n{2,}|\\Z)" +
                ")");
        text.replaceAll(p4, protectHTML);
    }

    private TextEditor formParagraphs(TextEditor markup) {
        markup.deleteAll("\\A\\n+");
        markup.deleteAll("\\n+\\z");

        String[] paragraphs;
        if (markup.isEmpty()) {
            paragraphs = new String[0];
        } else {
            paragraphs = Pattern.compile("\\n{2,}").split(markup.toString());
        }
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            String decoded = HTML_PROTECTOR.decode(paragraph);
            if (decoded != null) {
                paragraphs[i] = decoded;
            } else {
                paragraph = runSpanGamut(new TextEditor(paragraph)).toString();
                paragraphs[i] = "<p>" + paragraph + "</p>";
            }
        }
        return new TextEditor(join("\n\n", paragraphs));
    }


    private TextEditor doAutoLinks(TextEditor markup) {
        markup.replaceAll("<((https?|ftp):[^'\">\\s]+)>", "<a href=\"$1\">$1</a>");
        Pattern email = Pattern.compile("<([-.\\w]+\\@[-a-z0-9]+(\\.[-a-z0-9]+)*\\.[a-z]+)>");
        markup.replaceAll(email, new Replacement() {
            public String replacement(Matcher m) {
                String address = m.group(1);
                TextEditor ed = new TextEditor(address);
                unEscapeSpecialChars(ed);
                String addr = encodeEmail(ed.toString());
                String url = encodeEmail("mailto:" + ed.toString());
                return "<a href=\"" + url + "\">" + addr + "</a>";
            }
        });
        return markup;
    }

    private void unEscapeSpecialChars(TextEditor ed) {
        for (Iterator iterator = CHAR_PROTECTOR.getAllEncodedTokens().iterator(); iterator.hasNext();) {
            String hash = (String) iterator.next();
            String plaintext = CHAR_PROTECTOR.decode(hash);
            ed.replaceAllLiteral(hash, plaintext);
        }
    }

    private String encodeEmail(String s) {
        StringBuffer sb = new StringBuffer();
        char[] email = s.toCharArray();
        for (int i = 0; i < email.length; i++) {
            char ch = email[i];
            double r = rnd.nextDouble();
            if (r < 0.45) {      // Decimal
                sb.append("&#");
                sb.append((int) ch);
                sb.append(';');
            } else if (r < 0.9) {  // Hex
                sb.append("&#x");
                sb.append(Integer.toString((int) ch, 16));
                sb.append(';');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private TextEditor doBlockQuotes(TextEditor markup) {
        Pattern p = Pattern.compile("(" +
                "(" +
                "^[ ]*>[ ]?" + // > at the start of a line
                ".+\\n" + // rest of the first line
                "(.+\\n)*" + // subsequent consecutive lines
                "\\n*" + // blanks
                ")+" +
                ")", Pattern.MULTILINE);
        return markup.replaceAll(p, new Replacement() {
            public String replacement(Matcher m) {
                TextEditor blockQuote = new TextEditor(m.group(1));
                blockQuote.deleteAll("^[ ]*>[ ]?");
                blockQuote.deleteAll("^[ ]+$");
                blockQuote = runBlockGamut(blockQuote);
                blockQuote.replaceAll("^", "  ");


                Pattern p1 = Pattern.compile("(\\s*<pre>.*?</pre>)", Pattern.DOTALL);
                blockQuote = blockQuote.replaceAll(p1, new Replacement() {
                    public String replacement(Matcher m1) {
                        String pre = m1.group(1);
                        return deleteAll(pre, "^  ");
                    }
                });
                return "<blockquote>\n" + blockQuote + "\n</blockquote>\n\n";
            }
        });
    }

    private TextEditor doCodeBlocks(TextEditor markup) {
        Pattern p = Pattern.compile("" +
                "(?:\\n\\n|\\A)" +
                "((?:" +
                "(?:[ ]{4})" +
                ".*\\n+" +
                ")+" +
                ")" +
                "((?=^[ ]{0,4}\\S)|\\Z)", Pattern.MULTILINE);
        return markup.replaceAll(p, new Replacement() {
                    public String replacement(Matcher m) {
                        String codeBlock = m.group(1);
                        TextEditor ed = new TextEditor(codeBlock);
                        ed.outdent();
                        encodeCode(ed);
                        ed.detabify().deleteAll("\\A\\n+").deleteAll("\\s+\\z");
                        return "\n\n<pre><code>" + ed.toString() + "\n</code></pre>\n\n";
                    }
                });
    }

    private void encodeCode(TextEditor ed) {
        ed.replaceAll("&", "&amp;");
        ed.replaceAll("<", "&lt;");
        ed.replaceAll(">", "&gt;");
        ed.replaceAll("\\*", CHAR_PROTECTOR.encode("*"));
        ed.replaceAll("_", CHAR_PROTECTOR.encode("_"));
        ed.replaceAll("\\{", CHAR_PROTECTOR.encode("{"));
        ed.replaceAll("\\}", CHAR_PROTECTOR.encode("}"));
        ed.replaceAll("\\[", CHAR_PROTECTOR.encode("["));
        ed.replaceAll("\\]", CHAR_PROTECTOR.encode("]"));
        ed.replaceAll("\\\\", CHAR_PROTECTOR.encode("\\"));
    }

    private TextEditor doLists(TextEditor text) {
        int lessThanTab = 3; // TODO
        String wholeList =
                "(" +
                "(" +
                "[ ]{0," + lessThanTab + "}" +
                "((?:[-+*]|\\d+[.]))" + // $3 is first list item marker
                "[ ]+" +
                ")" +
                "(?s:.+?)" +
                "(" +
                "\\z" + // End of input is OK
                "|" +
                "\\n{2,}" +
                "(?=\\S)" + // If not end of input, then a new para
                "(?![ ]*" +
                "(?:[-+*]|\\d+[.])" +
                "[ ]+" +
                ")" + // negative lookahead for another list marker
                ")" +
                ")";

        Replacement replacer = new Replacement() {
            public String replacement(Matcher m) {
                String list = m.group(1);

                // Turn double returns into triple returns, so that we can make a
                // paragraph for the last item in a list, if necessary:
                list = replaceAll(list, "\n{2,}", "\n\n\n");

                String result = processListItems(list);
                String listStart = m.group(3);
                String html;
                if (Character.isDigit(listStart.charAt(0))) {
                    html = "<ol>\n" + result + "</ol>\n\n";
                } else {
                    html = "<ul>\n" + result + "</ul>\n\n";
                }
                return html;
            }
        };
        String anchor;
        if (listLevel == 0) {
            anchor = "(?:(?<=\\n\\n)|\\A\\n?)";
        } else {
            anchor = "^";
        }

        Pattern matchStartOfLine = Pattern.compile(anchor + wholeList, Pattern.MULTILINE);
        text.replaceAll(matchStartOfLine, replacer);

        return text;
    }

    private String processListItems(String list) {
        // The listLevel variable keeps track of when we're inside a list.
        // Each time we enter a list, we increment it; when we leave a list,
        // we decrement. If it's zero, we're not in a list anymore.
        //
        // We do this because when we're not inside a list, we want to treat
        // something like this:
        //
        //       I recommend upgrading to version
        //       8. Oops, now this line is treated
        //       as a sub-list.
        //
        // As a single paragraph, despite the fact that the second line starts
        // with a digit-period-space sequence.
        //
        // Whereas when we're inside a list (or sub-list), that line will be
        // treated as the start of a sub-list. What a kludge, huh? This is
        // an aspect of Markdown's syntax that's hard to parse perfectly
        // without resorting to mind-reading. Perhaps the solution is to
        // change the syntax rules such that sub-lists must start with a
        // starting cardinal number; e.g. "1." or "a.".
        listLevel++;

        // Trim trailing blank lines:
        list = replaceAll(list, "\\n{2,}\\z", "\n");

        Pattern p = Pattern.compile("(\\n)?" +
                "^([ \\t]*)([-+*]|\\d+[.])[ ]+" +
                "((?s:.+?)(\\n{1,2}))" +
                "(?=\\n*(\\z|\\2([-+\\*]|\\d+[.])[ \\t]+))",
                Pattern.MULTILINE);
        list = replaceAll(list, p, new Replacement() {
            public String replacement(Matcher m) {
                String text = m.group(4);
                TextEditor item = new TextEditor(text);
                String leadingLine = m.group(1);
                if (!isEmptyString(leadingLine) || hasParagraphBreak(item)) {
                    item = runBlockGamut(item.outdent());
                } else {
                    // Recurse sub-lists
                    item = doLists(item.outdent());
                    item = runSpanGamut(item);
                }
                return "<li>" + item.trim().toString() + "</li>\n";
            }
        });
        listLevel--;
        return list;
    }

    private boolean hasParagraphBreak(TextEditor item) {
        return item.toString().indexOf("\n\n") != -1;
    }

    private boolean isEmptyString(String leadingLine) {
        return leadingLine == null || leadingLine.equals("");
    }

    private TextEditor doHeaders(TextEditor markup) {
        // setext-style headers
        markup.replaceAll("^(.*)\n====+$", "<h1>$1</h1>");
        markup.replaceAll("^(.*)\n----+$", "<h2>$1</h2>");

        // atx-style headers - e.g., "#### heading 4 ####"
        Pattern p = Pattern.compile("^(#{1,6})\\s*(.*?)\\s*\\1?$", Pattern.MULTILINE);
        markup.replaceAll(p, new Replacement() {
            public String replacement(Matcher m) {
                String marker = m.group(1);
                String heading = m.group(2);
                int level = marker.length();
                String tag = "h" + level;
                return "<" + tag + ">" + heading + "</" + tag + ">\n";
            }
        });
        return markup;
    }

    private String join(String separator, String[] strings) {
        int length = strings.length;
        StringBuffer buf = new StringBuffer();
        if (length > 0) {
            buf.append(strings[0]);
            for (int i = 1; i < length; i++) {
                buf.append(separator).append(strings[i]);
            }
        }
        return buf.toString();
    }

    public TextEditor runSpanGamut(TextEditor text) {
        text = doCodeSpans(text);
        text = escapeSpecialChars(text);

        doImages(text);
        doAnchors(text);
        doAutoLinks(text);
        encodeAmpsAndAngles(text);
        doItalicsAndBold(text);

        // Manual line breaks
        text.replaceAll(" {2,}\n", " <br />\n");
        return text;
    }

    private TextEditor escapeSpecialChars(TextEditor text) {
        Collection tokens = text.tokenizeHTML();
        TextEditor newText = new TextEditor("");

        for (Iterator iterator = tokens.iterator(); iterator.hasNext();) {
            HTMLToken token = (HTMLToken) iterator.next();
            if (token.isTag()) {
                String value = token.getText();
                // TODO: escape * and _
                newText.append(value);
            } else {
                TextEditor ed = new TextEditor(token.getText());
                encodeBackslashEscapes(ed);
                String s = ed.toString();
                newText.append(s);
            }
        }

        return newText;
    }

    private void doImages(TextEditor text) {
        text.replaceAll("!\\[(.*)\\]\\((.*) \"(.*)\"\\)", "<img src=\"$2\" alt=\"$1\" title=\"$3\" />");
        text.replaceAll("!\\[(.*)\\]\\((.*)\\)", "<img src=\"$2\" alt=\"$1\" />");
    }

    private TextEditor doAnchors(TextEditor markup) {
        // Internal references: [link text] [id]
        Pattern internalLink = Pattern.compile("(" +
                "\\[(.*?)\\]" + // Link text = $2
                "[ ]?(?:\\n[ ]*)?" +
                "\\[(.*?)\\]" + // ID = $3
                ")");
        markup.replaceAll(internalLink, new Replacement() {
            public String replacement(Matcher m) {
                String replacementText;
                String wholeMatch = m.group(1);
                String linkText = m.group(2);
                String id = m.group(3);
                if (id == null || "".equals(id)) { // for shortcut links like [this][]
                    id = linkText.toLowerCase();
                }

                LinkDefinition defn = (LinkDefinition) linkDefinitions.get(id.toLowerCase());
                if (defn != null) {
                    String url = defn.getUrl();
                    // TODO: subst * and _
                    String title = defn.getTitle();
                    String titleTag = "";
                    if (title != null && !title.equals("")) {
                        titleTag = " title=\"" + title + "\"";
                    }
                    replacementText = "<a href=\"" + url + "\"" + titleTag + ">" + linkText + "</a>";
                } else {
                    replacementText = wholeMatch;
                }
                return replacementText;
            }
        });

        // Inline-style links: [link text](url "optional title")
        Pattern inlineLink = Pattern.compile("(" + // Whole match = $1
                "\\[(.*?)\\]" + // Link text = $2
                "\\(" +
                "[ \\t]*" +
                "<?(.*?)>?" + // href = $3
                "[ \\t]*" +
                "(" +
                "(['\"])" + // Quote character = $5
                "(.*?)" + // Title = $6
                "\\5" +
                ")?" +
                "\\)" +
                ")", Pattern.DOTALL);
        markup.replaceAll(inlineLink, new Replacement() {
            public String replacement(Matcher m) {
                String linkText = m.group(2);
                String url = m.group(3);
                String title = m.group(6);
                StringBuffer result = new StringBuffer();
                result.append("<a href=\"").append(url).append("\"");
                if (title != null) {
                    title = replaceAll(title, "\"", "&quot;");
                    result.append(" title=\"");
                    result.append(title);
                    result.append("\"");
                }
                result.append(">").append(linkText);
                result.append("</a>");
                return result.toString();
            }
        });

        return markup;
    }

    private TextEditor doItalicsAndBold(TextEditor markup) {
        markup.replaceAll("(\\*\\*|__)(?=\\S)(.+?[*_]*)(?<=\\S)\\1", "<strong>$2</strong>");
        markup.replaceAll("(\\*|_)(?=\\S)(.+?)(?<=\\S)\\1", "<em>$2</em>");
        return markup;
    }

    private TextEditor encodeAmpsAndAngles(TextEditor markup) {
        markup.replaceAll("&(?!#?[xX]?(?:[0-9a-fA-F]+|\\w+);)", "&amp;");
        markup.replaceAll("<(?![a-z/?\\$!])", "&lt;");
        return markup;
    }

    private TextEditor doCodeSpans(TextEditor markup) {
        return markup.replaceAll(Pattern.compile("(`+)(.+?)(?<!`)\\1(?!`)"), new Replacement() {
                    public String replacement(Matcher m) {
                        String code = m.group(2);
                        TextEditor subEditor = new TextEditor(code);
                        subEditor.deleteAll("^[ \\t]+").deleteAll("[ \\t]+$");
                        encodeCode(subEditor);
                        return "<code>" + subEditor.toString() + "</code>";
                    }
                });
    }


    private String deleteAll(String text, String regex) {
        return replaceAll(text, regex, "");
    }

    private String replaceAll(String text, String regex, String replacement) {
        TextEditor ed = new TextEditor(text);
        ed.replaceAll(regex, replacement);
        return ed.toString();
    }

    private String replaceAll(String markup, Pattern pattern, Replacement replacement) {
        TextEditor ed = new TextEditor(markup);
        ed.replaceAll(pattern, replacement);
        return ed.toString();
    }

    public String toString() {
        return "Markdown Processor version 0.1.0";
    }
}

