/*
Copyright (c) 2005, Martian Software
Authors: Pete Bevin, John Mutchek
http://www.martiansoftware.com/markdownj

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

/**
 * Convert Markdown text into HTML, as per http://daringfireball.net/projects/markdown/ .
 * Usage:
 * <pre><code>
 *     MarkdownProcessor markdown = new MarkdownProcessor();
 *     String html = markdown.markdown("*italic*   **bold**\n_italic_   __bold__");
 * </code></pre>
 *
 * @deprecated as of MarkdownJ 0.4. Use @{@link org.markdownj.MarkdownProcessor} instead.
 * This package has been renamed, and will be removed in MarkdownJ 0.5.
 */
@Deprecated
public class MarkdownProcessor extends org.markdownj.MarkdownProcessor {

    /**
     *
     * @param txt - input in markdown format
     * @return HTML block corresponding to txt passed in.
     * @deprecated as of MarkdownJ 0.4. Use @{@link org.markdownj.MarkdownProcessor#markdown(java.lang.String)} instead.
     */
    @Deprecated
    @Override
    public String markdown(String txt) {
        return super.markdown(txt);
    }

    /**
     *
     * @param args
     * @deprecated as of MarkdownJ 0.4. Use @{@link org.markdownj.MarkdownProcessor#main(java.lang.String[])} instead.
     */
    @Deprecated
    public static void main(java.lang.String[] args) {
        MarkdownProcessor.main(args);
    }

}
