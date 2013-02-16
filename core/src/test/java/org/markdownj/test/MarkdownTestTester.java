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

package org.markdownj.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.markdownj.MarkdownProcessor;

@RunWith(value = Parameterized.class)
public class MarkdownTestTester {

    private final static String MARKDOWN_TEST_DIR = "/MarkdownTest";

    String test;
    String dir;

    @Parameters
    public static Collection<Object[]> markdownTests() {
        List list = new ArrayList<Object[]>();
        URL fileUrl = MarkdownTestTester.class.getResource(MARKDOWN_TEST_DIR);
        File dir = new File(fileUrl.getFile());
        File[] dirEntries = dir.listFiles();

        for (int i = 0; i < dirEntries.length; i++) {
            File dirEntry = dirEntries[i];
            String fileName = dirEntry.getName();
            if (fileName.endsWith(".text")) {
                String testName = fileName.substring(0, fileName.lastIndexOf('.'));
                list.add(new Object[]{MARKDOWN_TEST_DIR, testName});
            }
        }

        return list;
    }

    public MarkdownTestTester(String dir, String test) {
        this.test = test;
        this.dir = dir;
    }

    @Test
    public void runTest() throws IOException {
        String testText = slurp(dir + File.separator + test + ".text");
        String htmlText = slurp(dir + File.separator + test + ".html");
        MarkdownProcessor markup = new MarkdownProcessor();
        String markdownText = markup.markdown(testText);
        assertEquals(test, htmlText.trim(), markdownText.trim());
    }

    private String slurp(String fileName) throws IOException {
        URL fileUrl = this.getClass().getResource(fileName);
        File file = new File(URLDecoder.decode(fileUrl.getFile(), "UTF-8"));
        FileReader in = new FileReader(file);
        StringBuffer sb = new StringBuffer();
        int ch;
        while ((ch = in.read()) != -1) {
            sb.append((char) ch);
        }
        return sb.toString();
    }
}
