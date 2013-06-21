package net.thisptr.specialize.processor.internal.javac;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import net.thisptr.specialize.processor.internal.AbstractProcessorTest;

import org.junit.experimental.theories.DataPoint;


public class JavacProcessorTest extends AbstractProcessorTest {
	@DataPoint
	public static JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
}
