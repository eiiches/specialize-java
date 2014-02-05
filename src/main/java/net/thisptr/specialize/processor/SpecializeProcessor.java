package net.thisptr.specialize.processor;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import net.thisptr.specialize.processor.internal.eclipse.EclipseProcessor;
import net.thisptr.specialize.processor.internal.javac.JavacProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SupportedAnnotationTypes("*")
public class SpecializeProcessor extends AbstractProcessor {
	private static Logger log = LoggerFactory.getLogger(SpecializeProcessor.class);

	private AbstractProcessor processor = null;

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		if (processor != null)
			return processor.process(annotations, roundEnv);
		return false;
	}

	@Override
	public synchronized void init(final ProcessingEnvironment processingEnv) {
		super.init(processingEnv);

		if (processingEnv.getClass().getName().startsWith("org.eclipse.jdt.")) {
			processor = new EclipseProcessor();
		} else if (processingEnv.getClass().getName().startsWith("com.sun.tools.javac.processing.JavacProcessingEnvironment")) {
			processor = new JavacProcessor();
		} else {
			log.error("Unsupported compiler: {}", processingEnv.getClass().getName());
		}

		if (processor != null)
			processor.init(processingEnv);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}
}
