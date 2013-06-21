package net.thisptr.specialize.processor.internal.eclipse;

import net.thisptr.specialize.processor.internal.AbstractProcessorTest;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.junit.experimental.theories.DataPoint;


public class EclipseProcessorTest extends AbstractProcessorTest {
	@DataPoint
	public static EclipseCompiler eclipse = new EclipseCompiler();
}
