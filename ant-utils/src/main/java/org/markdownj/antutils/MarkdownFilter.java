/*
Copyright (c) 2005, Martian Software
Authors: Marty Lamb
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

* Neither the name "Markdown", "MarkdownJ" nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

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
package org.markdownj.antutils;

import java.io.Reader;
import org.apache.tools.ant.filters.BaseParamFilterReader;
import org.apache.tools.ant.filters.ChainableReader;
import org.markdown.MarkdownProcessor;

/**
 * Provides a Markdown-based FilterReader suitable for use by Ant.
 *
 * <code><pre>
 * &lt;copy file="${src.file}" tofile="${dest.file}"&gt;
 *   &lt;filterchain&gt;
 *     &lt;filterreader classname="org.markdownj.MarkdownFilter"/&gt;
 *   &lt;/filterchain&gt;
 * &lt;/copy&gt;
 * </pre></code>
 * @author <a href="http://www.martiansoftware.com/contact.html">Marty Lamb</a>
 */
public class MarkdownFilter extends BaseParamFilterReader implements ChainableReader {

    /**
     * Stores the characters post markdown
     */
    char[] chars = null;

    /**
     * The index of the next character to return
     */
    int pos = 0;

    /**
     * The number of characters in the array (avoid repeated chars.length calls)
     */
    int len = 0;

    /**
     * Constructor for "dummy" instances.
     *
     * @see org.apache.tools.ant.filters.BaseFilterReader#BaseFilterReader()
     */
    public MarkdownFilter() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public MarkdownFilter(Reader reader) {
        super(reader);
    }

    /**
     * Creates a new MarkdownFilter using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(Reader reader) {
        MarkdownFilter result = new MarkdownFilter(reader);
        result.setParameters(this.getParameters());
        return (result);
    }

    /**
     * Returns the next character in the filtered stream, after performing
     * the Markdown processing
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading
     */
    @Override
    public final int read() throws java.io.IOException {

        if (chars == null) {
            char[] cbuf = new char[1024];
            StringBuffer buf = new StringBuffer();
            int charsRead = in.read(cbuf);
            while (charsRead >= 0) {
                buf.append(cbuf, 0, charsRead);
                charsRead = in.read(cbuf);
            }
            MarkdownProcessor markdown = new MarkdownProcessor();
            chars = markdown.markdown(buf.toString()).toCharArray();
            len = chars.length;
        }

        return (pos >= len ? -1 : chars[pos++]);
    }
}
