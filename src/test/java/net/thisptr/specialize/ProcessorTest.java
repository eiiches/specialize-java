package net.thisptr.specialize;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;


@RunWith(Theories.class)
public class ProcessorTest {
	
	@DataPoint
	public static JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

	// @DataPoint
	public static JavaCompiler eclipse = new EclipseCompiler();

	@DataPoints
	public static String[] classNames = new String[] {
		"net.thisptr.specialize.example.Example",
	};

	@Theory
	public void testCompile(final JavaCompiler compiler, final String className) throws IOException {
		final Locale locale = Locale.getDefault();
		final Charset charset = Charset.defaultCharset();
		
		final DiagnosticListener<JavaFileObject> diagnosticListener = new DiagnosticListener<JavaFileObject>() {
			@Override
			public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
			}
		};
		
		final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticListener, locale, charset);
		fileManager.setLocation(StandardLocation.SOURCE_PATH, Arrays.asList(new File("src/test/java")));
		fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File("target/test-classes")));
		final JavaFileObject sourceFile = fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, className, Kind.SOURCE);
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final Writer writer = new OutputStreamWriter(bos);
		try {
			final CompilationTask task = compiler.getTask(writer, fileManager, diagnosticListener, Arrays.<String>asList(), null, Arrays.asList(sourceFile));
			task.setProcessors(Arrays.asList(new Processor()));

			final boolean success = task.call();
			assertTrue(success);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {}
			System.out.println(bos.toString());
		}
	}
}
