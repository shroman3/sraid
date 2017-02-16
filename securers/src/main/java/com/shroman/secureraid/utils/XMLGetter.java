/*******************************************************************************
 * SSDPlayer Visualization Platform (Version 1.0)
 * Authors: Or Mauda, Roman Shor, Gala Yadgar, Eitan Yaakobi, Assaf Schuster
 * Copyright (c) 2015, Technion � Israel Institute of Technology
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that
 * the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *******************************************************************************/
package com.shroman.secureraid.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.shroman.secureraid.utils.XMLParsingException.IllegalXMLAttribute;
import com.shroman.secureraid.utils.XMLParsingException.IllegalXMLElement;
import com.shroman.secureraid.utils.XMLParsingException.NoSuchXMLAttribute;
import com.shroman.secureraid.utils.XMLParsingException.NoSuchXMLElement;

public class XMLGetter {
	public static class Getter {
		private Element element;

		private Getter(Element element) {
			this.element = element;
		}

		public String getAttribute(String attrName) throws XMLParsingException {
			return XMLGetter.getAttribute(element, attrName);
		}

		public int getIntAttribute(String attrName) throws XMLParsingException {
			return XMLGetter.getIntAttribute(element, attrName);
		}
	}

	private Document dom;

	public XMLGetter(String patternsFilePath) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		db = dbf.newDocumentBuilder();
		FileInputStream fis = new FileInputStream(patternsFilePath);
		dom = db.parse(fis);
	}

	public Iterator<Getter> getIterator(String section, String field) throws XMLParsingException {
		NodeList nodeList = getElement(section, dom.getDocumentElement()).getElementsByTagName(field);
		if (nodeList.getLength() <= 0) {
			throw new NoSuchXMLElement(field);
		}

		return new Iterator<Getter>() {
			private int index = 0;

			@Override
			public Getter next() {
				Element element = (Element) nodeList.item(index);
				index++;
				return new Getter(element);
			}

			@Override
			public boolean hasNext() {
				return index < nodeList.getLength();
			}
		};
	}

	public int getIntField(String section, String field) throws XMLParsingException {
		Element element = getElement(section, field);
		return Integer.parseInt(element.getTextContent());
	}
	// public String getStringField(String section, String field) throws
	// XMLParsingException {
	// Element element = getElement(section, field);
	// return element.getTextContent();
	// }
	//
	//
	// public Boolean getBooleanField(String section, String field) throws
	// XMLParsingException {
	// Element element = getElement(section, field);
	// return element.getTextContent().toLowerCase().equals("yes");
	// }
	//
	// public Color getColorField(String section, String field) throws
	// XMLParsingException {
	// Element element = getElement(section, field);
	// int r = getIntAttribute(element, "r");
	// int g = getIntAttribute(element, "g");
	// int b = getIntAttribute(element, "b");
	// return new Color(r,g,b);
	// }
	//
	// public List<Integer> getListField(String section, String field) throws
	// XMLParsingException {
	// Element element = getElement(section, dom.getDocumentElement());
	// NodeList nodeList = element.getElementsByTagName(field);
	// if (nodeList.getLength() <= 0) {
	// throw new NoSuchXMLElement(field);
	// }
	// List<Integer> result = new ArrayList<Integer>(nodeList.getLength());
	// for (int i = 0; i < nodeList.getLength(); ++i) {
	// Node node = nodeList.item(i);
	// try {
	// result.add(Integer.parseInt(node.getTextContent()));
	// } catch (NumberFormatException e) {
	// throw new WrongTypeXMLElement(field, node.getTextContent(), "int");
	// }
	// }
	// return result;
	// }

	// public List<Color> getColorsListField(String section, String field)
	// throws XMLParsingException {
	// Element element = getElement(section, dom.getDocumentElement());
	// NodeList nodeList = element.getElementsByTagName(field);
	// if (nodeList.getLength() <= 0) {
	// throw new NoSuchXMLElement(field);
	// }
	// List<Color> result = new ArrayList<Color>(nodeList.getLength());
	// for (int i = 0; i < nodeList.getLength(); ++i) {
	// Node node = nodeList.item(i);
	// try {
	// int r = getIntAttribute((Element) node, "r");
	// int g = getIntAttribute((Element) node, "g");
	// int b = getIntAttribute((Element) node, "b");
	// result.add(new Color(r, g, b));
	// } catch (NumberFormatException e) {
	// throw new WrongTypeXMLElement(field, node.getTextContent(), "Color");
	// }
	// }
	// return result;
	// }

	private Element getElement(String section, String field) throws IllegalXMLElement, NoSuchXMLElement {
		Element element = getElement(section, dom.getDocumentElement());
		return getElement(field, element);
	}

	private static int getIntAttribute(Element element, String attrName) throws XMLParsingException {
		String attrValue = element.getAttribute(attrName);
		if (attrValue == null) {
			throw new NoSuchXMLAttribute(element.getTagName(), attrName);
		}
		try {
			int parseInt = Integer.parseInt(attrValue);
			return parseInt;
		} catch (NumberFormatException e) {
			throw new IllegalXMLAttribute(element.getTagName(), attrName, "int");
		}
	}

	private static String getAttribute(Element element, String attrName) throws XMLParsingException {
		String attrValue = element.getAttribute(attrName);
		if (attrValue == null) {
			throw new NoSuchXMLAttribute(element.getTagName(), attrName);
		}
		return attrValue;
	}

	private static Element getElement(String field, Element element) throws NoSuchXMLElement, IllegalXMLElement {
		NodeList nodeList = element.getElementsByTagName(field);
		if (nodeList.getLength() <= 0) {
			throw new NoSuchXMLElement(field);
		}
		Node node = nodeList.item(0);
		if (!(node instanceof Element)) {
			throw new IllegalXMLElement(field);
		}
		return (Element) node;
	}
}
