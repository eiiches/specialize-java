package net.thisptr.specialize.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import net.thisptr.specialize.util.AnnotationUtils;

import org.junit.Test;


public class AnnotationUtilsTest {
	public @interface TestInterface {
		public int value();
		public String yukino() default "yukino";
	}

	@Test
	public void testGetMembers() {
		assertEquals(new HashSet<String>(Arrays.asList("value", "yukino")), AnnotationUtils.getMembers(TestInterface.class));
	}

	public void testGetDefaultValue() {
		assertEquals("yukino", AnnotationUtils.getDefaultValue(TestInterface.class, "yukino"));
	}
}
