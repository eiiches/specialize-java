package net.thisptr.specialize.processor.internal.eclipse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.apt.dispatch.BatchProcessingEnvImpl;
import org.eclipse.jdt.internal.compiler.apt.dispatch.RoundEnvImpl;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;

public class EclipseProcessor extends AbstractProcessor {
	private static Logger log = LoggerFactory.getLogger(EclipseProcessor.class);

	private Messager messager;

	public static CompilationUnitDeclaration[] getCompilationUnitDeclarations(final RoundEnvImpl roundEnv) {
		try {
			final Field _units = RoundEnvImpl.class.getDeclaredField("_units");
			_units.setAccessible(true);
			return (CompilationUnitDeclaration[]) _units.get(roundEnv);
		} catch (IllegalAccessException | IllegalArgumentException | SecurityException | NoSuchFieldException e) {
			log.error("Could not obtain compilationUnits.");
			return null;
		}
	}

	private static String[] primitiveTypes = new String[] {
			"int", "short", "char", "byte", "long", "float", "double", "boolean"
	};

	private static Map<String, String> underscoredTypes = new HashMap<String, String>();
	static {
		for (final String primitiveType : primitiveTypes)
			underscoredTypes.put("_" + primitiveType, primitiveType);
	}

	private static Map<String, String> dollaredTypes = new HashMap<String, String>();
	static {
		for (final String primitiveType : primitiveTypes)
			dollaredTypes.put("$" + primitiveType, primitiveType);
	}

	public static TypeReference specializeTypeReference(final TypeReference typeReference) {
		if (!(typeReference instanceof ParameterizedSingleTypeReference))
			return typeReference;

		final ParameterizedSingleTypeReference typeApply = (ParameterizedSingleTypeReference) typeReference;

		final StringBuilder specializedName = new StringBuilder("$specialized");
		final List<TypeReference> genericArguments = new ArrayList<TypeReference>();
		final List<TypeReference> primitiveArguments = new ArrayList<TypeReference>();

		for (final TypeReference argument : typeApply.typeArguments) {
			if (dollaredTypes.containsKey(argument.toString())) {
				primitiveArguments.add(argument);
				specializedName.append("$" + argument.toString());
			} else {
				genericArguments.add(argument);
				specializedName.append("$_");
			}
		}

		if (primitiveArguments.isEmpty())
			return typeApply;

		if (genericArguments.isEmpty()) { // fully specialized

		} else { // apply remaining type argument

		}
//
//		final JCExpression specializedClass = treeMaker.Select(typeApply.clazz, names.fromString(specializedName.toString()));
//
//		if (genericArguments.isEmpty()) {
//			// fully specialized
//			return specializedClass;
//		} else {
//			// apply remaining type argument
//			final JCTypeApply result = treeMaker.TypeApply(specializedClass, genericArguments);
//			return result;
//		}
//
		return typeApply;
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		if (roundEnv.processingOver())
			return true;

		messager.printMessage(Kind.NOTE, "Running specialize-java.");

		final BatchProcessingEnvImpl eclipseProcessingEnv = (BatchProcessingEnvImpl) processingEnv;
		final RoundEnvImpl eclipseRoundEnv = (RoundEnvImpl) roundEnv;

		final CompilationUnitDeclaration[] units = getCompilationUnitDeclarations(eclipseRoundEnv);
		log.info("# of Units: {}", units.length);

		for (final CompilationUnitDeclaration unit : units) {
			log.info("Unit: {}", new String(unit.getFileName()));

			final Parser parser = eclipseProcessingEnv.getCompiler().parser;
			final CompilerOptions options = unit.scope.compilerOptions();
			parser.scanner = new PrimitiveAsIdentifierScanner(false, false, false, options.sourceLevel, options.complianceLevel, options.taskTags, options.taskPriorities, options.isTaskCaseSensitive);
			parser.getMethodBodies(unit);

			unit.traverse(new ASTVisitor() {
				@Override
				public boolean visit(final LocalDeclaration localDeclaration, final BlockScope scope) {
					localDeclaration.type = specializeTypeReference(localDeclaration.type);
					return true;
				}

//				@Override
//				public boolean visit(final ParameterizedQualifiedTypeReference typeReference, final ClassScope scope) {
//					log.info("ParametrizedQualifiedTypeReference(ClassScope): {}", typeReference);
//					return true;
//				}
//
//				@Override
//				public boolean visit(final ParameterizedQualifiedTypeReference typeReference, final BlockScope scope) {
//					log.info("ParametrizedQualifiedTypeReference(BlockScope): {}", typeReference);
//					return true;
//				}
//
//				@Override
//				public boolean visit(final ParameterizedSingleTypeReference typeReference, final ClassScope scope) {
//					log.info("ParametrizedSingleTypeReference(ClassScope): {}", typeReference);
//					return true;
//				}
//
//				@Override
//				public boolean visit(final ParameterizedSingleTypeReference typeReference, final BlockScope scope) {
//					log.info("ParametrizedSingleTypeReference(BlockScope): {}", typeReference);
//					log.info("  Arguments: {}", typeReference.typeArguments);
//
//					return true;
//				}

				@Override
				public boolean visit(final MethodDeclaration methodDeclaration, final ClassScope scope) {
					log.info("MethodDeclaration: {}()", new String(methodDeclaration.selector));
//					methodDeclaration.parseStatements(eclipseProcessingEnv.getCompiler().parser, unit);
					// methodDeclaration.resolveStatements();
					log.info("  Body: {}-{}", methodDeclaration.bodyStart, methodDeclaration.bodyEnd);
//					log.info("  Source: {}", new String(Arrays.copyOfRange(eclipseProcessingEnv.getCompiler().parser.scanner.source, methodDeclaration.bodyStart, methodDeclaration.bodyEnd)));

					if (methodDeclaration.statements == null) {
						log.warn("  Statements: !!!!!!!!!!!!!!!!!!!!!!!!!!!!! ");
						return true;
					}

					for (final Statement statement : methodDeclaration.statements) {
						log.info("  Statement: {}", statement.toString());
					}
					// log.info("  Statements: {}", methodDeclaration.statements.toString());
					return true;
				}

			}, unit.scope);
		}

//		for (final Element element : roundEnv.getRootElements()) {
//			messager.printMessage(Kind.OTHER, "ElementType: " + element.getClass(), element);
//
//			final TypeElementImpl impl = (TypeElementImpl) element;
//			final TypeMirror unit = element.asType();
//
//			final DeclaredTypeImpl typeimpl = (DeclaredTypeImpl) unit;
//
//			log.info("unit: {}[{}]", unit, unit.getClass());
//		}

		return true;
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
	}
}