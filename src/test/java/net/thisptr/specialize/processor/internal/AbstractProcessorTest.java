package net.thisptr.specialize.processor.internal;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import net.thisptr.specialize.processor.SpecializeProcessor;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(Theories.class)
public abstract class AbstractProcessorTest {
	private static Logger log = LoggerFactory.getLogger(AbstractProcessorTest.class);
	private static Logger rlog = LoggerFactory.getLogger("RAW");

	@DataPoints
	public static String[] classNames = new String[] {
		"net.thisptr.specialize.example.Example",
	};

	@Theory
	public void testCompile(final JavaCompiler compiler, final String className) throws IOException {
		final Locale locale = Locale.getDefault();
		final Charset charset = Charset.defaultCharset();

		final DiagnosticListener<JavaFileObject> diagnosticListener = new DiagnosticListener<JavaFileObject>() {
			private int plines = 0;
			@Override
			public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
				JavaFileObject source = null;
				try {
					source = diagnostic.getSource();
				} catch (Exception e) {}
				final Diagnostic.Kind kind = diagnostic.getKind();
				final String[] lines = diagnostic.getMessage(Locale.getDefault()).split("\n");
				if (lines.length + (source != null ? 1 : 0) > 1 || plines > 1)
					rlog.info("[{}] ----------------------------------------------------", kind);
				if (source != null)
					rlog.info("[{}] {} ({}:{})", kind, source.getName(), diagnostic.getLineNumber(), diagnostic.getColumnNumber());
				for (final String line : lines)
					rlog.info("[{}] {}", kind, line);
				plines = lines.length + (source != null ? 1 : 0);
			}
		};

		final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticListener, locale, charset);
		fileManager.setLocation(StandardLocation.SOURCE_PATH, Arrays.asList(new File("src/test/java")));
		fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File("target/test-classes")));
		fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, Arrays.asList(new File("target/generated-test-sources")));
		final JavaFileObject sourceFile = fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, className, Kind.SOURCE);

		final List<String> options = Arrays.asList(new String[] {
				"-target", "1.7",
				"-source", "1.7",
		});

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final Writer writer = new OutputStreamWriter(bos);
		try {
			final CompilationTask task = compiler.getTask(writer, fileManager, diagnosticListener, options, null, Arrays.asList(sourceFile));
			task.setProcessors(Arrays.asList(new SpecializeProcessor()));

			final boolean success = task.call();
			assertTrue(success);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {}
//			System.err.println(bos.toString());
		}
	}
}
