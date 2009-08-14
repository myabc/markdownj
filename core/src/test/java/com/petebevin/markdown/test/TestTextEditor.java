package com.petebevin.markdown.test;

import com.petebevin.markdown.TextEditor;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestTextEditor {

    @Test
    public void testDetabify() {
        assertEquals("    ", new TextEditor("\t").detabify().toString());
        assertEquals("    ", new TextEditor(" \t").detabify().toString());
        assertEquals("    ", new TextEditor("  \t").detabify().toString());
        assertEquals("    ", new TextEditor("   \t").detabify().toString());
        assertEquals("        ", new TextEditor("    \t").detabify().toString());

        assertEquals("     ", new TextEditor("\t ").detabify().toString());
        assertEquals("        ", new TextEditor("\t \t").detabify().toString());
    }

}
