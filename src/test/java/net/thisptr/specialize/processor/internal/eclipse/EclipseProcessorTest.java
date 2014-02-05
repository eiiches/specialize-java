package net.thisptr.specialize.processor.internal.eclipse;

import net.thisptr.specialize.processor.internal.AbstractProcessorTest;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.junit.Ignore;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;


@Ignore
public class EclipseProcessorTest extends AbstractProcessorTest {
	@DataPoint
	public static EclipseCompiler eclipse = new EclipseCompiler();

	@DataPoints
	public static String[] classNames = new String[] {
		"net.thisptr.specialize.example.EclipseDebug",
	};
}
