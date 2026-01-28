/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"nls", "javadoc"})
public class KeyValuesConverterTest
{
	final static String SIMPLE_CASE  = "foo:bar\tbar:foo\nbaz:else";
	final static String COMPLEX_CASE = "foo-bar:xyz\nbar_foo:zyx+abc\tbaz:some;thing\nxyz:else${abc}";
	final static String COMPLEX_CASE_SORTED = "bar_foo:zyx+abc\tbaz:some;thing\nfoo-bar:xyz\nxyz:else${abc}";

	@Test
	void SimpleStringToMapTest() {
		Map<String, Object> map = KeyValuesConverter.convertStringToMap(SIMPLE_CASE);
		Assertions.assertEquals(3, map.size());
		Assertions.assertEquals("bar", map.get("foo"));
		Assertions.assertEquals("foo", map.get("bar"));
	}
	@Test
	void ComplexStringToMapTest() {
		Map<String, Object> map = KeyValuesConverter.convertStringToMap(COMPLEX_CASE);
		Assertions.assertEquals(4, map.size());
		Assertions.assertEquals("xyz", map.get("foo-bar"));
		Assertions.assertEquals("zyx+abc", map.get("bar_foo"));
		Assertions.assertEquals("some;thing", map.get("baz"));
		Assertions.assertEquals("else${abc}", map.get("xyz"));
	}
	@Test
	void ComplexBidrectionalTest() {
		Map<String, Object> map = KeyValuesConverter.convertStringToMap(COMPLEX_CASE);
		String reconverted = KeyValuesConverter.convertMapToString(map);
		Assertions.assertEquals(COMPLEX_CASE_SORTED
		                        .replace('\t', KeyValuesConverter.PROPERTY_SEPARATOR)
		//                        .replace(',', KeyValuesConverter.PROPERTY_SEPARATOR)
		                        .replaceAll("\\:",": "),
		                        reconverted);
	}
}
