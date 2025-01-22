/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

/**
 * Provides a pretty-printed (i.e. line-wrappend and properly indented) version of an Object
 * (e.g. a Node-hierarchy). Brackets and Braces are considered for indentation.
 */
@SuppressWarnings("javadoc")
public class PrettyPrint
{
	public static int indentation = 4;

	public static String prettyPrinted(Object obj) {
		StringBuilder buf = new StringBuilder();
		prettyPrinted(obj.toString(), 0, 0, buf);
		return buf.toString();
	}

	private static int prettyPrinted(String input, int start, int nesting, StringBuilder buf) {
		int pos = start;
		emitIndentation(nesting, buf);
		while (pos < input.length()) {
			char ch = input.charAt(pos++);
			switch (ch) {
			case ',':
				buf.append(ch);
				buf.append('\n');
				emitIndentation(nesting, buf);
				if (input.charAt(pos) == ' ') pos++; // swallow ' ' following ','
 				break;
			case '[':
			case '{':
				buf.append(ch);
				buf.append('\n');
				pos = prettyPrinted(input, pos, nesting+1, buf);
				break;
			case ']':
			case '}':
				buf.append('\n');
				emitIndentation(nesting-1, buf);
				buf.append(ch);
				return pos;
			default:
				buf.append(ch);
			}
		}
		return pos;
	}

	private static void emitIndentation(int nesting, StringBuilder buf) {
		for (int n = 0; n < nesting; n++) {
			for (int i = 0; i < indentation; i++) buf.append(' ');
		}
	}
}
