/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SuppressWarnings("javadoc")
@Slf4j
public class XmlPrettyPrinter
{
	public static int xmlIndentation = 4;
	/** @maxLineLength the maximum length of each line in the returned string (not including indent if specified). */
	public static int maxLineLength = 150;
	public static boolean maintainNewlines = false;

	public static String prettyPrintXML(String xmlInput, boolean validate) {
		log.debug("Input XML:\n{}", xmlInput); //$NON-NLS-1$
		String formattedXML = (validate ? prettyPrintXMLviaDOM(xmlInput) : prettyPrintSimple(xmlInput, 0));
		log.debug("Formatted XML:\n{}", formattedXML); //$NON-NLS-1$
		return formattedXML;
	}

	public static String prettyPrintXMLviaDOM(String xmlString) {
		Document xmlDoc = null;
		String formattedXML = ""; //$NON-NLS-1$
		try {
			xmlDoc       = toXmlDocument(xmlString);
			formattedXML = prettyPrint(xmlDoc);
		} catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
			log.error("exception pretty-printing XML", e); //$NON-NLS-1$
		}
		return formattedXML;
	}

	private static String prettyPrint(Document document) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();

		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(xmlIndentation)); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$

		DOMSource source = new DOMSource(document);
		StringWriter strWriter = new StringWriter();
		StreamResult result = new StreamResult(strWriter);

		transformer.transform(source, result);

		return strWriter.getBuffer().toString();
	}

	private static Document toXmlDocument(String str)
		throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(new InputSource(new StringReader(str)));
		return document;
	}

	public static String prettyPrintSimple(String xmlInput, int initialIndent) {
		boolean singleLine = false;
		int indent = initialIndent;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < xmlInput.length(); i++) {
			char currentChar = xmlInput.charAt(i);
			if (currentChar == '<') {
				char nextChar = xmlInput.charAt(i + 1);
				if (nextChar == '/') {
					indent -= XmlPrettyPrinter.xmlIndentation;
				}
				if (!singleLine) { // Don't indent before closing element if we're creating
				                   // opening and closing elements on a single line.
					sb.append(buildWhitespace(indent));
				}
				if (nextChar != '?' && nextChar != '!' && nextChar != '/') {
					indent += XmlPrettyPrinter.xmlIndentation;
				}
				singleLine = false; // Reset flag.
			}
			sb.append(currentChar);
			if (currentChar == '>') {
				if (xmlInput.charAt(i - 1) == '/') {
					indent -= XmlPrettyPrinter.xmlIndentation;
					sb.append('\n');
				} else {
					int nextStartElementPos = xmlInput.indexOf('<', i);
					if (nextStartElementPos > i+1) {
						String textBetweenElements = xmlInput.substring(i+1, nextStartElementPos);
						// If the space between elements is solely newlines, let them through
						// to preserve additional newlines in source document.
						if (maintainNewlines && textBetweenElements.replaceAll("\n", "").length() == 0) { //$NON-NLS-1$//$NON-NLS-2$
							sb.append(textBetweenElements).append('\n');
						} else if (textBetweenElements.length() <= XmlPrettyPrinter.maxLineLength * 0.5) {
							// Put tags and text on a single line if the text is short.
							sb.append(textBetweenElements);
							singleLine = true;
						} else { // For larger amounts of text, wrap lines to a maximum line length.
							sb.append('\n')
							  .append(lineWrap(textBetweenElements, XmlPrettyPrinter.maxLineLength, indent, null))
							  .append('\n');
						}
						i = nextStartElementPos-1;
					} else {
						sb.append('\n');
					}
				}
			}
		}
		return sb.toString();
	}

	private static String buildWhitespace(int numChars) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < numChars; i++) sb.append(' ');
		return sb.toString();
	}

	/**
	 * Wraps the supplied text to the specified line length.
	 *
	 * @indent optional number of whitespace characters to prepend to each
	 *         line before the text.
	 * @linePrefix optional string to append to the indent (before the
	 *             text).
	 * @returns the supplied text wrapped so that no line exceeds the
	 *          specified line length + indent, optionally with indent and
	 *          prefix applied to each line.
	 */
	private static String lineWrap(String s, int lineLength, int indent,
	                               String linePrefix) {
		if (s == null) return null;

		StringBuilder sb = new StringBuilder();
		int lineStartPos = 0;
		int lineEndPos;
		boolean firstLine = true;
		while (lineStartPos < s.length()) {
			if (!firstLine) sb.append("\n"); //$NON-NLS-1$
			else firstLine = false;

			if (lineStartPos + lineLength > s.length()) {
				lineEndPos = s.length() - 1;
			} else {
				lineEndPos = lineStartPos + lineLength - 1;
				while (lineEndPos > lineStartPos && !Character.isWhitespace(s.charAt(lineEndPos))) {
					lineEndPos--;
				}
			}
			sb.append(buildWhitespace(indent));
			if (linePrefix != null) sb.append(linePrefix);

			sb.append(s.substring(lineStartPos, lineEndPos + 1));
			lineStartPos = lineEndPos + 1;
		}
		return sb.toString();
	}
}