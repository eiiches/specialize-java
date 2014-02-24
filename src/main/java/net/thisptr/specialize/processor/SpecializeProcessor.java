package net.thisptr.specialize.processor;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import net.thisptr.specialize.processor.internal.javac.JavacProcessor;


@SupportedAnnotationTypes("*")
public class SpecializeProcessor extends AbstractProcessor {
	private AbstractProcessor processor = null;
	private Messager messager = null;

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		if (processor != null)
			return processor.process(annotations, roundEnv);
		return false;
	}

	@Override
	public synchronized void init(final ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();

		if (processingEnv.getClass().getName().startsWith("org.eclipse.jdt.")) {
			// processor = new EclipseProcessor();
			messager.printMessage(Kind.WARNING, String.format("Your compiler (%s) is not supported by specialize-java. Specializations will not be performed.", processingEnv.getClass().getCanonicalName()));
		} else if (processingEnv.getClass().getName().startsWith("com.sun.tools.javac.processing.JavacProcessingEnvironment")) {
			processor = new JavacProcessor();
		} else {
			messager.printMessage(Kind.WARNING, String.format("Your compiler (%s) is not supported by specialize-java. Specializations will not be performed.", processingEnv.getClass().getCanonicalName()));
		}

		if (processor != null)
			processor.init(processingEnv);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}
}
