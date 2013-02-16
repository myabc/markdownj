/*
 Copyright (c) 2008 - 2012, Alex Coles

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
package com.petebevin.markdown.test;

import com.petebevin.markdown.MarkdownProcessor;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class Images {

    MarkdownProcessor m;

    @Before
    public void createProcessor() {
        m = new MarkdownProcessor();
    }

    @Test
    public void inlineStyleImages() {
        String html = m.markdown("![Lilium](/images/flower.png)");
        assertEquals("<p><img src=\"/images/flower.png\" alt=\"Lilium\" /></p>", html.trim());
    }

    @Test
    @Ignore("alt text is not being rendered")
    public void referenceStyleImages() {
        String html = m.markdown(
                "![Lilium][id]"
                + "\n[id]: /images/flower.png");
        assertEquals("<p><img src=\"/images/flower.png\"/></p>", html.trim());
    }

    @Test
    public void referenceStyleImagesWithTitle() {
        String html = m.markdown(
                "![Castles of Scotland][id]"
                + "\n[id]: /images/castles.jpg \"Beauty is in the eye of the beholder\"");
        assertEquals("<p><img src=\"/images/castles.jpg\" alt=\"Castles of Scotland\" title=\"Beauty is in the eye of the beholder\"/></p>", html.trim());
    }

    @Test
    @Ignore("alt text is not being rendered")
    public void referenceStyleImagesAfterText() {
        String html = m.markdown(
                "A wonderful image: ![Lilium][id]"
                + "\n[id]: /images/flower.png");
        assertEquals("<p>A wonderful image: <img src=\"/images/flower.png\"/></p>", html.trim());
    }
}
