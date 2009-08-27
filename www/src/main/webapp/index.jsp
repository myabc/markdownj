<%@ page language="java" import="com.petebevin.markdown.MarkdownProcessor" pageEncoding="utf-8" %>
<%
    MarkdownProcessor mp = new MarkdownProcessor();
    String markup = request.getParameter("markdown");
    String view = request.getParameter("view");

    // determine the view to show
    boolean showSource  = true;
    boolean showPreview = true;

    if (view != null) {
      if ("source".equals(view)) {
        showSource  = true;
        showPreview = false;
      } else if ("preview".equals(view)) {
        showSource  = false;
        showPreview = true;
      }; // otherwise, default to showing both
    }
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8" />
<title>MarkdownJ - Markdown in Java</title>
<style type="text/css">
        /*
          Basic layout courtesy of InkNoise's very nifty Layout-o-matic:
          http://www.inknoise.com/experimental/layoutomatic.php
        */
        #sidebar h1 {
                font-size: 1.5em;
                font-weight: bold;
        }
        #sidebar h2 {
                font-size: 1.2em;
                font-weight: bold;
                margin-bottom: -.5em;
        }
        #sidebar h3 {
                font-size: 1em;
                font-weight: bold;
                text-transform: none;
                margin-bottom: .25em;
                margin-top: 1.5em;
        }
        #sidebar code {
                font-family: Monaco, ProFont, "Andale Mono", "Lucida Console", Courier, monospace;
                font-size: 10px;
        }
        #sidebar pre {
                line-height: 12px;
                margin-top: 0;
                background-color: #f5f5f5;
                border: 1px solid #ccc;
                padding: 4px;
        }
        #sidebar p {
                margin-top: 0;
                margin-bottom: 0;
        }

        body {
                background-color: #eee;
                font-family: "Lucida Grande", Verdana, sans-serif;
                font-size: 11px;
                line-height: 1.6em;
        }
        .renderbox {
                background: white;
                font-family: Georgia, serif;
                font-size: 13px;
                border: 1px #888 solid;
                padding: 0 5px;
                width: 97%;
        }
        .label {
                margin-bottom: 4px;
        }

        #container {
                border: 0px solid gray;
                margin: 10px;
                margin-left: auto;
                margin-right: auto;
                padding: 0px;
                min-width: 750px;

                /* Serve up some shit for Win/IE: */
                width:expression("800px" );
                margin-left:expression("0");
        }

        #banner {
                padding: 0;
                margin-bottom: 5px;
                background-color: transparent;
        }

        #app {
                padding: 0 10px 0 10px;
                margin-right: 270px;
                background-color: transparent;
                border-right: 0px solid #bbb;
        }

        #sidebar {
                float: right;
                width: 250px;
                margin: 0;
                margin-right: 10px;
                padding: 0;
                background-color: transparent;
        }

        .footer {
                margin-top: 100px;
        }

        #buttonrow {
                margin-top: 10px;
                margin-bottom: 60px;
        }
        .actionbutton {
                width: 7em;
                margin-left:20px;
        }

        textarea[name="markdown"], textarea[name="xhtml"] {
        /* WinIE is fucking retarded. Thanks to Sam Ruby for the workaround:
                http://www.intertwingly.net/blog/1432.html */
                width: 98%;
        }

        #ProjectSubmenu {
                list-style: none outside;
                padding: 0;
                margin: 2em 0 1em 0;
                height: 4em; /* Setting a height makes it act like a block */
                border: 0px dotted gray;
        }

        #ProjectSubmenu li {
                display: inline;
                padding: 0;
                margin: 0;
        }


        #ProjectSubmenu li a,
        #ProjectSubmenu li a:link,
        #ProjectSubmenu li a:visited {
                text-decoration: none;
                text-align: center;
                float: left;
                display: block;
                min-width: 35px;
                padding: 1px 3px 2px 3px;
                margin: 0;
                margin-right: -1px;
                background: transparent;
                color: black;
                border-color: #999;
                border-width: 1px;
                border-style: solid;
        }

        #ProjectSubmenu li a.selected,
        #ProjectSubmenu li a.selected:hover {
                background: #999;
                color: white;
        }

        #ProjectSubmenu li a:hover {
                background-color: #ccc;
        }

        #ProjectSubmenu li a:active {
                color: #eee;
                background: #bbb;
        }

</style>
</head>

<body>
<div id="container">

<!-- <div id="banner"> -->

<!-- </div> -->

<div id="sidebar">
<h1>MarkdownJ: Dingus</h1>

<ul id="ProjectSubmenu">
    <li><a href="http://markdownj.org/" title="MarkdownJ Project Page">Main</a></li>
    <%--
    <li><a href="/projects/markdown/basics" title="Markdown Basics">Basics</a></li>
    <li><a href="/projects/markdown/syntax" title="Markdown Syntax Documentation">Syntax</a></li>
    --%>
    <li><a href="http://markdownj.org/license.html" title="License Information">License</a></li>
    <li><a class="selected" title="Online MarkdownJ Web Form">Dingus</a></li>
</ul>

<p>(shamelessly stolen from <a href="http://daringfireball.net/projects/markdown/dingus">Markdown</a>).
    Download source and binaries from <a href="http://code.google.com/p/markdownj/downloads/list">Google Code</a></p>

<h2>Syntax Cheatsheet:</h2>

<h3>Phrase Emphasis</h3>

<pre><code>*italic*   **bold**
_italic_   __bold__
</code></pre>

<h3>Links</h3>

<p>Inline:</p>

<pre><code>An [example](http://url.com/ "Title")
</code></pre>

<p>Reference-style labels (titles are optional):</p>

<pre><code>An [example][id]. Then, anywhere
else in the doc, define the link:

  [id]: http://example.com/  "Title"
</code></pre>

<h3>Images</h3>

<p>Inline (titles are optional):</p>

<pre><code>![alt text](/path/img.jpg "Title")
</code></pre>

<p>Reference-style:</p>

<pre><code>![alt text][id]

[id]: /url/to/img.jpg "Title"
</code></pre>

<h3>Headers</h3>

<p>Setext-style:</p>

<pre><code>Header 1
========

Header 2
--------
</code></pre>

<p>atx-style (closing #'s are optional):</p>

<pre><code># Header 1 #

## Header 2 ##

###### Header 6
</code></pre>

<h3>Lists</h3>

<p>Ordered, without paragraphs:</p>

<pre><code>1.  Foo
2.  Bar
</code></pre>

<p>Unordered, with paragraphs:</p>

<pre><code>*   A list item.

    With multiple paragraphs.

*   Bar
</code></pre>

<p>You can nest them:</p>

<pre><code>*   Abacus
    * answer
*   Bubbles
    1.  bunk
    2.  bupkis
        * BELITTLER
    3. burper
*   Cunning
</code></pre>

<h3>Blockquotes</h3>

<pre><code>&gt; Email-style angle brackets
&gt; are used for blockquotes.

&gt; &gt; And, they can be nested.

&gt; #### Headers in blockquotes
&gt;
&gt; * You can quote a list.
&gt; * Etc.
</code></pre>

<h3>Code Spans</h3>

<pre><code>`&lt;code&gt;` spans are delimited
by backticks.

You can include literal backticks
like `` `this` ``.
</code></pre>

<h3>Preformatted Code Blocks</h3>

<p>Indent every line of a code block by at least 4 spaces or 1 tab.</p>

<pre><code>This is a normal paragraph.

    This is a preformatted
    code block.
</code></pre>

<h3>Horizontal Rules</h3>

<p>Three or more dashes or asterisks:</p>

<pre><code>---

* * *

- - - -
</code></pre>

<h3>Manual Line Breaks</h3>

<p>End a line with two or more spaces:</p>

<pre><code>Roses are red,
Violets are blue.
</code></pre>
</div> <!-- sidebar -->

<div id="app">
<form method="POST">
<div>
<p class="label">Markdown Source:</p>
<textarea rows="25" cols="80" style="font-family: Monaco, ProFont, monospace; font-size: 10px;" name="markdown"><%= markup == null ? "" : markup %></textarea>

<div id="buttonrow">
<div style="float:left">
<%--
Filter:&nbsp;<select name="filter" style="font-size: 11px; margin-right: 2em;">
<option value='markdown'>Markdown</option>
<option value='smartypants'>SmartyPants</option>
<option value='both'>Both</option>
</select>
--%>
Results:&nbsp;<select name="view" style="font-size: 11px; margin-right: 2em;">
<option value='source'>Source</option>
<option value='preview'>Preview</option>
<option value='both' selected='selected'>Source &amp; Preview</option>
</select>
</div>

<input type="submit" name="convert" value="Convert" class="actionbutton" />
</div> <!--buttonrow-->

</div>
</form>

<% if (markup != null) {
    String html = mp.markdown(markup);
    //java.io.FileWriter log = new java.io.FileWriter("/home/www/markdown/log/" + System.currentTimeMillis() + ".log");
    //log.write(markup);
    //log.close();
%>
  <% if(showSource) { %>
  <p class="label">HTML Source:</p>
  <div>
  <textarea name="xhtml" rows="25" cols="80" readonly="readonly" style="font-family: Monaco, ProFont, monospace; font-size: 10px;"><%= html %></textarea>
  </div>
  <% } %>
  <% if(showPreview) { %>
  <p class="label">HTML Preview:</p>
  <div class="renderbox"><%= html %></div>
  <% } %>
<% } %>

<%
java.util.Calendar now = java.util.Calendar.getInstance();
int year = now.get(java.util.Calendar.YEAR);
%>
<p class='footer'>MarkdownJ 0.3.0 (compatible with Markdown 1.0.2b2)<br />
  Markdown and the Markdown Dingus are copyright &copy; 2004–<%= year %> John Gruber.
  Used with permission.</p>

</div> <!-- app -->
</div> <!-- container -->

<script type="text/javascript">
//<![CDATA[
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
//]]>
</script>
<script type="text/javascript">
//<![CDATA[
try {
var pageTracker = _gat._getTracker("UA-6093957-2");
pageTracker._trackPageview();
} catch(err) {}
//]]></script>
</body>
</html>
