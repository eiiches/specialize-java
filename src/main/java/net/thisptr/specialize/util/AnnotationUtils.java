package net.thisptr.specialize.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationUtils {
	private AnnotationUtils() { }

	private static Logger log = LoggerFactory.getLogger(AnnotationUtils.class);

	public static <A extends Annotation> Set<String> getMembers(final Class<A> annotationClass) {
		final Set<String> result = new HashSet<String>();
		for (final Method annotationMethod : annotationClass.getDeclaredMethods())
			result.add(annotationMethod.getName());
		return result;
	}

	public static Object getDefaultValue(final Class<? extends Annotation> annotationClass, final String name) {
		try {
			return annotationClass.getMethod(name).getDefaultValue();
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("No such attribute in {}: {}", annotationClass, name);
			return null;
		}
	}
}
