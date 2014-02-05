package net.thisptr.specialize.processor.internal.javac;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import net.thisptr.specialize.processor.internal.AbstractProcessorTest;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;


public class JavacProcessorTest extends AbstractProcessorTest {
	@DataPoint
	public static JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

	@DataPoints
	public static String[] classNames = new String[] {
		"net.thisptr.specialize.example.Example",
	};
}
