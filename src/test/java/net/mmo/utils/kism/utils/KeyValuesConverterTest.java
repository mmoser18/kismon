/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 01.01.2021
 */

package net.mmo.utils.kism.utils;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
public class KeyValuesConverterTest
{
	final static String SIMPLE_CASE  = "foo:bar,bar:foo,baz:else";
	final static String COMPLEX_CASE = "foo-bar:xyz,bar_foo:zyx+abc\tbaz:some;thing\nxyz:else${abc}";
	final static String COMPLEX_CASE_SORTED = "bar_foo:zyx+abc\tbaz:some;thing\nfoo-bar:xyz,xyz:else${abc}";

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
		                        .replace('\t',KeyValuesConverter.PROPERTY_SEPARATOR)
		                        .replace(',',KeyValuesConverter.PROPERTY_SEPARATOR), reconverted);
	}
}
