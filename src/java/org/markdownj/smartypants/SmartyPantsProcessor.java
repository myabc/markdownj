/*
 * Copyright (c) 2003 John Gruber
 * <http://daringfireball.net/>
 * All rights reserved.
 *
 * Copyright (c) 2004-2005 Michel Fortin <http://www.michelf.com>
 * Copyright (c) 2008 Alex Coles <http://ikonoklastik.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  *	Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 *  *	Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *  *	Neither the name "SmartyPants" nor the names of its contributors may
 * be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * This software is provided by the copyright holders and contributors "as is"
 * and any express or implied warranties, including, but not limited to, the
 * implied warranties of merchantability and fitness for a particular purpose
 * are disclaimed. In no event shall the copyright owner or contributors be
 * liable for any direct, indirect, incidental, special, exemplary, or
 * consequential damages (including, but not limited to, procurement of
 * substitute goods or services; loss of use, data, or profits; or business
 * interruption) however caused and on any theory of liability, whether in
 * contract, strict liability, or tort (including negligence or otherwise)
 * arising in any way out of the use of this software, even if advised of the
 * possibility of such damage.
 *
 */
package org.markdownj.smartypants;

import com.petebevin.markdown.HTMLToken;
import com.petebevin.markdown.TextEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SmartyPants  -  Smart punctuation for web sites
 *
 * by John Gruber
 * <http://daringfireball.net>
 *
 * PHP port by Michel Fortin
 * <http://www.michelf.com/>
 *
 * Portions of this plug-in are based on Brad Choate's nifty MTRegex plug-in.
 * Brad Choate also contributed a few bits of source code to this plug-in.
 * Brad Choate is a fine hacker indeed. (<http://bradchoate.com/>.
 *
 * Jeremy Hedley (<http://antipixel.com/>) and Charles Wiltgen
 * (<http://playbacktime.com/>) deserve mention for exemplary beta testing.
 *
 * @author John Gruber
 * @author Michel Fortin
 * @author Alex Coles
 * @version 1.0
 */
public class SmartyPantsProcessor {

    public static final String DEFAULT_OPTION = "1";
						  //  1 =>  "--" for em-dashes; no en-dash support
						  //  2 =>  "---" for em-dashes; "--" for en-dashes
						  //  3 =>  "--" for em-dashes; "---" for en-dashes
						  //  See docs for more configuration options.

    public static final String TAGS_TO_SKIP = ""; // "<(/?)(?:pre|code|kbd|script|math)[\s>]";


    /**
     *
     * @param txt text to be parsed
     * @return
     */
    public static String smartyPants(String txt) {
        return smartyPants(txt, null);
    }

    /**
     * @param txt text to be parsed
     * @param attr the option for how SmartyPants processing should be handled
     * @return
     */
    public static String smartyPants(String txt, String attr) {
        if (txt == null) {
            txt = "";
        }

        if (attr == null) {
            attr = DEFAULT_OPTION;
        }

        // Options to specify which transformations to make:
        boolean do_quotes = false;
        int do_backticks = 0;
        int do_dashes = 0;
        boolean do_ellipses = false;
        boolean do_stupefy = false;
        boolean convert_quot = false; // should we translate &quot; entities into normal quotes?

        // Parse attributes:
        // 0 : do nothing
        // 1 : set all
        // 2 : set all, using old school en- and em- dash shortcuts
        //
        // 3 : set all, using inverted old school en and em- dash shortcuts
        //
        // q : quotes
        // b : backtick quotes (``double'' only)
        // B : backtick quotes (``double'' and `single')
        // d : dashes
        // D : old school dashes
        // i : inverted old school dashes
        // e : ellipses
        // w : convert &quot; entities to " for Dreamweaver users

        if (attr == "0") {
            // Do nothing.
            return txt;
        }
        else if (attr == "1") {
            // Do everything, turn all options on.
            do_quotes    = true;
            do_backticks = 1;
            do_dashes    = 1;
            do_ellipses  = true;
        }
        else if (attr == "2") {
            // Do everything, turn all options on, use old school dash shorthand.
            do_quotes    = true;
            do_backticks = 1;
            do_dashes    = 2;
            do_ellipses  = true;
        }
        else if (attr == "3") {
            // Do everything, turn all options on, use inverted old school dash shorthand.
            do_quotes    = true;
            do_backticks = 1;
            do_dashes    = 3;
            do_ellipses  = true;
        }
        else if (attr == "-1") {
            // Special "stupefy" mode.
            do_stupefy   = true;
        }
        else {
            String[] chars = attr.split("//");
            for (String c : chars) {
                if      (c == "q") { do_quotes    = true; }
                else if (c == "b") { do_backticks = 1; }
                else if (c == "B") { do_backticks = 2; }
                else if (c == "d") { do_dashes    = 1; }
                else if (c == "D") { do_dashes    = 2; }
                else if (c == "i") { do_dashes    = 3; }
                else if (c == "e") { do_ellipses  = true; }
                else if (c == "w") { convert_quot = true; }
                else {
                    // Unknown attribute option, ignore.
                }
            }
        }

        TextEditor text = new TextEditor(txt);
        List<HTMLToken> tokens = (List<HTMLToken>) text.tokenizeHTML();
        String result = "";

        boolean in_pre = false;  // Keep track of when we're inside <pre> or <code> tags.

        String prev_token_last_char = "";     // This is a cheat, used to get some context
                                        // for one-character tokens that consist of
                                        // just a quote char. What we do is remember
                                        // the last character of the previous text
                                        // token, to use as context to curl single-
                                        // character quote tokens correctly.

        for (HTMLToken curr_token : tokens) {
            if (curr_token.isTag()) {
                // Don't mess with quotes inside tags.
                result += curr_token.getText();

                Pattern p = Pattern.compile("@"+TAGS_TO_SKIP+"@");
                Matcher m = p.matcher(curr_token.getText());
                if (m.matches()) {
                    //$in_pre = isset($matches[1]) && $matches[1] == '/' ? 0 : 1;
                }

            } else {
                String t = curr_token.getText();
                String last_char = t.substring(t.length()); // Remember last char of this token before processing.
                if (! in_pre) {
                    t = processEscapes(t);

                    if (convert_quot) {
                        t = t.replaceAll("/&quot;/", "\"");
                    }

                    if (do_dashes > 0) {
                        if (do_dashes == 1) t = educateDashes(t);
                        if (do_dashes == 2) t = educateDashesOldSchool(t);
                        if (do_dashes == 3) t = educateDashesOldSchoolInverted(t);
                    }

                    if (do_ellipses) t = educateEllipses(t);

                    // Note: backticks need to be processed before quotes.
                    if (do_backticks > 0) {
                        t = educateBackticks(t);
                        if (do_backticks == 2) t = educateSingleBackticks(t);
                    }

                    if (do_quotes) {
                        if (t == "'") {
                            // Special case: single-character ' token
                            if (prev_token_last_char.matches("/S/")) { // \S
                                t = "&#8217;";
                            }
                            else {
                                t = "&#8216;";
                            }
                        }
                        else if (t == "\"") {
                            // Special case: single-character " token
                            if (prev_token_last_char.matches("/S/")) { // \S
                                t = "&#8221;";
                            }
                            else {
                                t = "&#8220;";
                            }
                        }
                        else {
                            // Normal case:
                            t = educateQuotes(t);
                        }
                    }

                    if (do_stupefy) t = stupefyEntities(t);
                }
                prev_token_last_char = last_char;
                result += t;
            }
        }

        return result;
    }


    /**
     * @param txt text to be parsed
     * @param attr the option for how smart quotes processing should be handled
     * @return
     */
    public static String smartQuotes(String txt, String attr) {
        if (txt == null) {
            txt = "";
        }

        if (attr == null) {
            attr = DEFAULT_OPTION;
        }

        boolean do_backticks; //  # should we educate ``backticks'' -style quotes?

        if (attr == "0") {
            // do nothing;
            return txt;
        } else if (attr == "2") {
            // smarten ``backticks'' -style quotes
            do_backticks = true;
        } else {
            do_backticks = false;
        }
        // Special case to handle quotes at the very end of text when preceded by
        // an HTML tag. Add a space to give the quote education algorithm a bit of
        // context, so that it can guess correctly that it's a closing quote:
        boolean add_extra_space = false;
        if (txt.matches("/>['\"]\\z/")) {
            add_extra_space = true; // Remember, so we can trim the extra space later.
            txt += " ";
        }

        TextEditor text = new TextEditor(txt);
        List<HTMLToken> tokens = (List<HTMLToken>) text.tokenizeHTML();
        String result = "";

        boolean in_pre = false;  // Keep track of when we're inside <pre> or <code> tags

        // for one-character tokens that consist of
        // just a quote char. What we do is remember
        // the last character of the previous text
        // token, to use as context to curl single-
        // character quote tokens correctly.

        // Keep track of when we're inside <pre> or <code> tags
        String prev_token_last_char = "";
        for (HTMLToken curr_token : tokens) {
            if (curr_token.isTag()) {
                // Don't mess with quotes inside tags
                result += curr_token.getText();

                Pattern p = Pattern.compile("@" + TAGS_TO_SKIP + "@");
                Matcher m = p.matcher(curr_token.getText());
                if (m.matches()) {
                    //$in_pre = isset($matches[1]) && $matches[1] == '/' ? 0 : 1;
                }
            } else {
                String t = curr_token.getText();
                String last_char = t.substring(t.length()); // Remember last char of this token before processing.
                if (!in_pre) {
                    t = processEscapes(t);
                    if (do_backticks) {
                        t = educateBackticks(t);
                    }

                    if (t == "'") {
                        // Special case: single-character ' token
                        if (prev_token_last_char.matches("/S/")) {
                            t = "&#8217;";
                        } else {
                            t = "&#8216;";
                        }
                    } else if (t == "\"") {
                        // Special case: single-character " token
                        if (prev_token_last_char.matches("/S/")) {
                            t = "&#8221;";
                        } else {
                            t = "&#8220;";
                        }
                    } else {
                        // Normal case:
                        t = educateQuotes(t);
                    }

                }
                prev_token_last_char = last_char;
                result += t;
            }
        }

        if (add_extra_space) {
            result.replace("/ z/", ""); // / \z/
        }
        return result;
    }

    /**
     * @param txt text to be parsed
     * @param attr the option for how smart dashes processing should be handled
     * @return
     */
    public static String smartDashes(String txt, String attr) {
        if (txt == null) {
            txt = "";
        }

        if (attr == null) {
            attr = DEFAULT_OPTION;
        }

        // reference to the subroutine to use for dash education, default to educateDashes:
        String dash_sub_ref = "EducateDashes";

        if (attr == "0") {
            // do nothing;
            return txt; // TODO: or txt??
        } else if (attr == "2") {
            // use old smart dash shortcuts, "--" for en, "---" for em
            dash_sub_ref = "EducateDashesOldSchool";
        } else if (attr == "3") {
            // inverse of 2, "--" for em, "---" for en
            dash_sub_ref = "EducateDashesOldSchoolInverted";
        }

        TextEditor text = new TextEditor(txt);
        List<HTMLToken> tokens = (List<HTMLToken>) text.tokenizeHTML();

        String result = "";
        boolean in_pre = false;  // Keep track of when we're inside <pre> or <code> tags

        for (HTMLToken curr_token : tokens) {
            if (curr_token.isTag()) {
                // Don't mess with quotes inside tags
                result += curr_token.getText();

                Pattern p = Pattern.compile("@" + TAGS_TO_SKIP + "@");
                Matcher m = p.matcher(curr_token.getText());
                if (m.matches()) {
                    //$in_pre = isset($matches[1]) && $matches[1] == '/' ? 0 : 1;
                }

            } else {
                String t = curr_token.getText();
                if (!in_pre) {
                    t = processEscapes(t);

                    // XXX: Reflection is an ugly-hack just to be PHP-like.
                    java.lang.reflect.Method sub_method = null;
                    try {
                        sub_method = SmartyPantsProcessor.class.getMethod(dash_sub_ref);
                    } catch (NoSuchMethodException ex) {
                        Logger.getLogger(SmartyPantsProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (SecurityException ex) {
                        Logger.getLogger(SmartyPantsProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {
                        t = (String) sub_method.invoke(t);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(SmartyPantsProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(SmartyPantsProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(SmartyPantsProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                result += t;
            }
        }
        return result;
    }

    /**
     * @param txt text to be parsed
     * @param attr the option for how smart ellipses processing should be handled
     * @return
     */
    public static String smartEllipses(String txt, String attr) {

        if (txt == null) {
            txt = "";
        }

        if (attr == null) {
            attr = DEFAULT_OPTION;
        }

        if (attr == "0") {
            // do nothing;
            return txt;
        }

        TextEditor text = new TextEditor(txt);
        List<HTMLToken> tokens = (List<HTMLToken>) text.tokenizeHTML();

        String result = "";
        boolean in_pre = false;  // Keep track of when we're inside <pre> or <code> tags

        for (HTMLToken curr_token : tokens) {
            if (curr_token.isTag()) {
                // Don't mess with quotes inside tags
                result += curr_token.getText();

                Pattern p = Pattern.compile("@" + TAGS_TO_SKIP + "@");
                Matcher m = p.matcher(curr_token.getText());
                if (m.matches()) {
                    //$in_pre = isset($matches[1]) && $matches[1] == '/' ? 0 : 1;
                }

            } else {
                String t = curr_token.getText();
                if (!in_pre) {
                    t = processEscapes(t);
                    t = educateEllipses(t);
                }
                result += t;
            }
        }
        return result;
    }

    /**
     *
     * @param String str
     * @return The string, with "educated" curly quote HTML entities.
     *
     *   Example input:  "Isn't this fun?"
     *   Example output: &#8220;Isn&#8217;t this fun?&#8221;
     *
     */
    private static String educateQuotes(String str) {

        // Make our own "punctuation" character class, because the POSIX-style
        // [:PUNCT:] is only available in Perl 5.6 or later:
        // ("[!\"#\\$\\%'()*+,-.\\/:;<=>?\\@\\[\\\\\]\\^_`{|}~]");
        Pattern punct_class = Pattern.compile("\\p{Punct}");

        // Special case if the very first character is a quote
        // followed by punctuation at a non-word-break. Close the quotes by brute force:
        str = str.replaceAll("/^'(?=$punct_class\\B)/", "&#8217;");     // TODO: include punct_clas
        str = str.replaceAll("/^\"(?=$punct_class\\B)/", "&#8221;");    // TODO: ditto

        // Special case for double sets of quotes, e.g.:
        //   <p>He said, "'Quoted' words in a larger quote."</p>
        str = str.replaceAll("/\"'(?=\\w)/", "&#8220;&#8216;");
        str = str.replaceAll("/'\"(?=\\w)/", "&#8216;&#8220;");

        // Special case for decade abbreviations (the '80s):
        str = str.replaceAll("/'(?=\\d{2}s)/", "&#8217;");

        String close_class = "[^\\ \t\r\n\\[\\{\\(\\-]";
        String dec_dashes = "&\\#8211;|&\\#8212;";

        // Get most opening single quotes:
        str = str.replaceAll("{" +
            "(" +
                "\\s          |" +  // a whitespace char, or
                "&nbsp;      | " + // a non-breaking space entity, or
                "--          | " + // dashes, or
                "&[mn]dash;  | " + // named dash entities
                dec_dashes + " |  " + // or decimal entities
                "&\\#x201[34]; |"+   // or hex
            ")" +
            "'" +                  // the quote
            "(?=\\w)" +              // followed by a word character
            "}x", "\\1&#8216;");
        // Single closing quotes:
        str = str.replaceAll("{" +
            "("+close_class+")?" +
            "'" +
            "(?(1)|" +          // If $1 captured, then do nothing;
              "(?=\\s | s\\b)" +  // otherwise, positive lookahead for a whitespace
            ")"+               // char or an 's' at a word ending position. This
                            // is a special case to handle something like:
                            // \"<i>Custer</i>'s Last Stand.\"
            "}xi", "\\1&#8217;");

        // Any remaining single quotes should be opening ones:
        str = str.replaceAll("'", "&#8216;");

        // Get most opening double quotes:
        str = str.replace("{" +
                "(" +
                "\\s         |"+   // a whitespace char, or
                "&nbsp;      |"+   // a non-breaking space entity, or
                "--          |"+   // dashes, or
                "&[mn]dash;  |"+   // named dash entities
                dec_dashes + " |"+   // or decimal entities
               "&\\#x201[34];" +  // or hex
            ")" +
               " \"" +                  // the quote
            "(?=\\w) "+              // followed by a word character
          "}x",
          "\\1&#8220;");

        // Double closing quotes:
        str = str.replace("{"+
              "("+close_class+")?"+
                "\"" +
            "(?(1)|(?=\\s))"+   // If $1 captured, then do nothing;
                             // if not, then make sure the next char is whitespace.
            "}x", "\\1&#8221;");

        // Any remaining quotes should be opening ones.
        str = str.replace("\"", "&#8220;");

        return str;
    }

    /**
     * @param String str
     * @return The string, with ``backticks'' -style double quotes
     *         translated into HTML curly quote entities.
     *
     *   Example input:  ``Isn't this fun?''
     *   Example output: &#8220;Isn't this fun?&#8221;
     *
     */
    private static String educateBackticks(String str) {
        str = str.replace("``", "&#8220;");
        str = str.replace("''", "&#8221;");
        return str;
    }

    /**
     * @param String str
     * @return The string, with `backticks' -style single quotes
     *         translated into HTML curly quote entities.
     *
     *   Example input:  `Isn't this fun?'
     *   Example output: &#8216;Isn&#8217;t this fun?&#8217;
     *
     */
    private static String educateSingleBackticks(String str) {
        str = str.replace("`", "&#8216;");
        str = str.replace("'", "&#8217;");
        return str;
    }

    /**
     * @param String str
     * @return The string, with each instance of "--" translated to
     *         an em-dash HTML entity.
     */
    private static String educateDashes(String str) {
        str = str.replace("--", "&#8212;");
        return str;
    }

    /**
     * @param String str
     * @return The string, with each instance of "--" translated to
     *         an en-dash HTML entity, and each "---" translated to
     *         an em-dash HTML entity.
     */
    private static String educateDashesOldSchool(String str) {
        str = str.replace("---", "&#8212;"); // em-dash
        str = str.replace("--", "&#8211;");  // en-dash
        return str;
    }

    /**
     * @param String str
     * @return The string, with each instance of "--" translated to
     *              an em-dash HTML entity, and each "---" translated to
     *              an en-dash HTML entity. Two reasons why: First, unlike the
     *              en- and em-dash syntax supported by
     *              educateDashesOldSchool(), it's compatible with existing
     *              entries written before SmartyPants 1.1, back when "--" was
     *              only used for em-dashes.  Second, em-dashes are more
     *              common than en-dashes, and so it sort of makes sense that
     *              the shortcut should be shorter to type. (Thanks to Aaron
     *              Swartz for the idea.)
     */
    private static String educateDashesOldSchoolInverted(String str) {
        str = str.replace("---", "&#8211;"); // en-dash
        str = str.replace("--", "&#8212;");  // em-dash
        return str;
    }

    /**
     * @param String str
     * @return The string, with each instance of "..." translated to
     *               an ellipsis HTML entity. Also converts the case where
     *               there are spaces between the dots.
     *
     *   Example input:  Huh...?
     *   Example output: Huh&#8230;?
     */
    private static String educateEllipses(String str) {
        str.replace("...", "&#8230;");
        str.replace(". . .", "&#8230;");
        return str;
    }

    /**
     * @param String str
     * @return The string, with each SmartyPants HTML entity translated to
     *         its ASCII counterpart.
     *
     *   Example input:  &#8220;Hello &#8212; world.&#8221;
     *   Example output: "Hello -- world."
     */
    public static String stupefyEntities(String str) {

        // dashes
        str = str.replace("&#8211;", "-"); // en-dash
        str = str.replace("&#8212;", "--"); // em-dash

        // single quote
        str = str.replace("&#8216;", "'"); // open
        str = str.replace("&#8217;", "'"); // close

        // double quote
        str = str.replace("&#8220;", "\""); // open
        str = str.replace("&#8221;", "\""); // close

        // ellipsis
        str = str.replace("&#8230;", "...");

        return str;
    }

    /**
     * @param String str
     * @return the string, with after processing the following backslash
     *         escape sequences. This is useful if you want to force a "dumb"
     *         quote or other character to appear.
     *
     *               Escape  Value
     *               ------  -----
     *               \\      &#92;
     *               \"      &#34;
     *               \'      &#39;
     *               \.      &#46;
     *               \-      &#45;
     *               \`      &#96;
     */
    private static String processEscapes(String str) {

        str = str.replace("\\\\", "&#92;");
        str = str.replace("\\\"", "&#34;");
        str = str.replace("\\'", "&#49;");
        str = str.replace("\\.", "&#46;");
        str = str.replace("\\-", "&#45;");
        str = str.replace("\\`", "&#96;");

        return str;
    }

    @Override
    public String toString() {
        return "SmartyPants Processor for Java 0.1.0 (compatible with SmartyPants 1.5.1)";
    }

}
