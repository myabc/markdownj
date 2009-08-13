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

package com.petebevin.markdown.test;

import com.petebevin.markdown.MarkdownProcessor;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkupFileTester extends TestCase {
    TestResultPair pair;

    public MarkupFileTester(TestResultPair pair) {
        super(pair.toString());
        this.pair = pair;
    }

    public static Test suite() throws IOException {
        TestSuite suite = new TestSuite("Test files");
        suite.addTest(newSuite("/dingus.txt"));
        suite.addTest(newSuite("/paragraphs.txt"));
        suite.addTest(newSuite("/snippets.txt"));
        suite.addTest(newSuite("/lists.txt"));
        return suite;
    }

    public static Test newSuite(String filename) throws IOException {
        TestSuite suite = new TestSuite("Markdown file " + filename);
        URL fileUrl = MarkupFileTester.class.getResource(filename);
        FileReader file = new FileReader(fileUrl.getFile());
        BufferedReader in = new BufferedReader(file);
        StringBuffer test = null;
        StringBuffer result = null;

        Pattern pTest = Pattern.compile("# Test (\\w+) \\((.*)\\)");
        Pattern pResult = Pattern.compile("# Result (\\w+)");
        String line;
        int lineNumber = 0;

        String testNumber = null;
        String testName = null;
        StringBuffer curbuf = null;
        while ((line = in.readLine()) != null) {
            lineNumber++;
            Matcher mTest = pTest.matcher(line);
            Matcher mResult = pResult.matcher(line);

            if (mTest.matches()) { // # Test
                addTest(suite, test, result, testNumber, testName);
                testNumber = mTest.group(1);
                testName = mTest.group(2);
                test = new StringBuffer();
                result = new StringBuffer();
                curbuf = test;
            } else if (mResult.matches()) { // # Result
                if (testNumber == null) {
                    throw new RuntimeException("Test file has result without a test (line " + lineNumber + ")");
                }
                String resultNumber = mResult.group(1);
                if (!testNumber.equals(resultNumber)) {
                    throw new RuntimeException("Result " + resultNumber + " test " + testNumber + " (line " + lineNumber + ")");
                }

                curbuf = result;
            } else {
                curbuf.append(line);
                curbuf.append("\n");
            }
        }

        addTest(suite, test, result, testNumber, testName);

        return suite;
    }

    private static void addTest(TestSuite suite, StringBuffer testbuf, StringBuffer resultbuf, String testNumber, String testName) {
        if (testbuf == null || resultbuf == null) {
            return;
        }

        String test = chomp(testbuf.toString());
        String result = chomp(resultbuf.toString());

        String id = testNumber + "(" + testName + ")";

        suite.addTest(new MarkupFileTester(new TestResultPair(id, test, result)));
    }

    private static String chomp(String s) {
        int lastPos = s.length() - 1;
        while (s.charAt(lastPos) == '\n' || s.charAt(lastPos) == '\r') {
            lastPos--;
        }
        return s.substring(0, lastPos + 1);
    }

    @Override
    public void runTest() {
        MarkdownProcessor markup = new MarkdownProcessor();
        assertEquals(pair.toString(), pair.getResult().trim(), markup.markdown(pair.getTest()).trim());
    }
}
