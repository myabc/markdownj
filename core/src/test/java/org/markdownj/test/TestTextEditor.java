package org.markdownj.test;

import static org.junit.Assert.*;
import org.junit.Test;
import org.markdownj.TextEditor;

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
