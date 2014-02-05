package net.thisptr.specialize.processor.internal.eclipse;

import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import net.thisptr.specialize.processor.internal.IOUtils;
import net.thisptr.specialize.processor.internal.SpecializeInfo;

import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EclipseProcessor extends AbstractProcessor {

	private static Logger log = LoggerFactory.getLogger(EclipseProcessor.class);

	private Messager messager;
	
	private boolean sourceCreated = false;

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		if (roundEnv.processingOver())
			return true;
		
		if (sourceCreated)
			return true;
		
		try {
			log.info("Overwriting the source");
			final JavaFileObject specializedSource = processingEnv.getFiler().createSourceFile("net.thisptr.specialize.example.EclipseDebug");
			 // BatchFilerImpl
			log.info(processingEnv.getFiler().toString());
			final Writer writer = specializedSource.openWriter();
			try {
				writer.write("public class EclipseDebug { public static void main() { System.out.println(\"hoge\"); } }");
			} finally {
				IOUtils.closeQuietly(writer);
			}
			sourceCreated = true;
		} catch (Exception e) {
			log.error("Exception", e);
		}
		
//		log.info("Running specialize-java.");
//
//		final BatchProcessingEnvImpl eclipseProcessingEnv = (BatchProcessingEnvImpl) processingEnv;
//		final RoundEnvImpl eclipseRoundEnv = (RoundEnvImpl) roundEnv;
//
//		final CompilationUnitDeclaration[] units = Utils.getCompilationUnitDeclarations(eclipseRoundEnv);
//		log.info("# of Units: {}", units.length);
//
//		// patch parser and load all statements
//		for (final CompilationUnitDeclaration unit : units) {
//			final Parser parser = eclipseProcessingEnv.getCompiler().parser;
//			final CompilerOptions options = unit.scope.compilerOptions();
//			parser.scanner = new PrimitiveAsIdentifierScanner(false, false, false, options.sourceLevel, options.complianceLevel, options.taskTags, options.taskPriorities, options.isTaskCaseSensitive);
//			parser.getMethodBodies(unit);
//		}
//
//		// specialize
//		for (final Element element : roundEnv.getElementsAnnotatedWith(Specialize.class)) {
//			log.info("Annotation Found: " + element.getSimpleName());
//			
//			final TypeMirror mirror = element.asType();
//			final Specialize specialize = element.getAnnotation(Specialize.class);
//			final SpecializeInfo specializeInfo = SpecializeInfo.parse(specialize);
//			
//			final ASTNode node = Utils.getASTNote(element);
//			if (node instanceof TypeDeclaration) {
//				specializeTypeDeclaration((TypeDeclaration) node, specializeInfo);
//			}
//		}
//
//		for (final CompilationUnitDeclaration unit : units) {
//			unit.traverse(new SpecializeTypeReferenceVisitor(), unit.scope);
//		}
//		
//		for (final CompilationUnitDeclaration unit : units) {
//			
//			unit.traverse(new ASTVisitor() {
//				@Override
//				public boolean visit(final TypeDeclaration memberTypeDeclaration, final ClassScope scope) {
//					// memberTypeDeclaration.binding = null;
//					new ClassScope(scope, memberTypeDeclaration);
//					return true;
//				}
//
//				@Override
//				public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
//					// methodDeclaration.binding = null;
//					new MethodScope(scope, methodDeclaration, methodDeclaration.isStatic());
//					return true;
//				}
//
//				@Override
//				public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
//					// TODO Auto-generated method stub
//					new MethodScope(scope, constructorDeclaration, constructorDeclaration.isStatic());
//					return super.visit(constructorDeclaration, scope);
//				}
//			}, unit.scope);
//		}
//		
//		try {
//			final JavaFileObject specializedSource = processingEnv.getFiler().createSourceFile("net.thisptr.specialize.example.Example.java");
//			final Writer writer = specializedSource.openWriter();
//			try {
//				writer.write("public class Example { public static void main() { System.out.println(\"hoge\"); } }");
//			} finally {
//				IOUtils.closeQuietly(writer);
//			}
//		} catch (Exception e) {
//			log.error("Exception", e);
//		}
//		
//
//		// dump specialized source for debugging
//		for (final CompilationUnitDeclaration unit : units) {
//			final File out = new File(new String(unit.getFileName()) + ".specialized");
//			try {
//				Utils.dumpSource(out, unit);
//			} catch (IOException e) {
//				log.warn("Cannot write to {}.", out);
//			};
//		}
//
//		//		for (final Element element : roundEnv.getRootElements()) {
//		//			messager.printMessage(Kind.OTHER, "ElementType: " + element.getClass(), element);
//		//
//		//			final TypeElementImpl impl = (TypeElementImpl) element;
//		//			final TypeMirror unit = element.asType();
//		//
//		//			final DeclaredTypeImpl typeimpl = (DeclaredTypeImpl) unit;
//		//
//		//			log.info("unit: {}[{}]", unit, unit.getClass());
//		//		}
		return true;
	}

	private static TypeDeclaration specializeTypeDeclaration(final TypeDeclaration typeDeclaration, final SpecializeInfo specializeInfo) {
		return typeDeclaration;
	}
	
	private static MethodDeclaration specializeMethodDeclaration(final MethodDeclaration methodDeclaration, final SpecializeInfo specializeInfo) {
		return methodDeclaration;
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		log.info("Initializing specialize-java");

		super.init(processingEnv);
		messager = processingEnv.getMessager();
	}
}