package net.thisptr.specialize;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import net.thisptr.specialize.annotation.InjectPrimitive;
import net.thisptr.specialize.annotation.InjectPrimitives;
import net.thisptr.specialize.annotation.Specialize;
import net.thisptr.specialize.annotation.Specializes;
import net.thisptr.specialize.util.AnnotationUtils;
import net.thisptr.specialize.util.EvaluateUtils;
import net.thisptr.specialize.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

// TODO: 型パラメータに含まれるものを特殊化するときはspecialize,
// それ以外のところを埋めるだけなのはinjectのバリデーション

// @SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("*")
public class Processor extends AbstractProcessor {
	private static Logger log = LoggerFactory.getLogger(Processor.class);
	
	private static Type toType(final Context context, final Class<?> clazz) {
		final Symtab symtab = Symtab.instance(context);
		
		if (clazz == int.class) return  symtab.intType;
		if (clazz == double.class) return  symtab.doubleType;
		if (clazz == float.class) return  symtab.floatType;
		if (clazz == char.class) return  symtab.charType;
		if (clazz == short.class) return  symtab.shortType;
		if (clazz == byte.class) return  symtab.byteType;
		if (clazz == boolean.class) return  symtab.booleanType;
		if (clazz == long.class) return  symtab.longType;
		
		log.error("Unhandled class type: {}", clazz);
		return null;
	}
	
	private static <T extends JCTree> T injectPrimitive(final Context context, final T tree, final Map<String, InjectPrimitiveInfo> injections) {
		final TreeMaker treeMaker = TreeMaker.instance(context);
		
		tree.accept(new TreeTranslator() {
			@Override
			public void visitIdent(final JCIdent tree) {
				if (!injections.containsKey(tree.toString())) {
					super.visitIdent(tree);
					return;
				}
				
				final InjectPrimitiveInfo info = injections.get(tree.toString());
				result = treeMaker.Type(toType(context, info.type));
			}
		});
		
		return tree;
	}
	
	private static void eachSpecialization(final java.util.List<Map<String, InjectPrimitiveInfo>> results, final java.util.List<SpecializeInfo> specialization, final Stack<InjectPrimitiveInfo> injections) {
		if (specialization.isEmpty()) {
			if (injections.isEmpty()) // all generic version
				return;
			
			final Map<String, InjectPrimitiveInfo> result = new HashMap<String, InjectPrimitiveInfo>();
			for (final InjectPrimitiveInfo injection : injections) {
				result.put(injection.key, injection);
			}
			results.add(result);
			
			log.info("  + {}", injections);
		} else {
			final SpecializeInfo head = specialization.get(0);

			for (final Class<?> type : head.types) {
				injections.push(new InjectPrimitiveInfo(head.key, type));
				eachSpecialization(results, specialization.subList(1, specialization.size()), injections);
				injections.pop();
			}
			
			if (head.generic)
				eachSpecialization(results, specialization.subList(1, specialization.size()), injections);
		}
	}
	
	private static java.util.List<Map<String, InjectPrimitiveInfo>> eachSpecialization(final Collection<SpecializeInfo> specializations) {
		final java.util.List<Map<String, InjectPrimitiveInfo>> results = new ArrayList<Map<String, InjectPrimitiveInfo>>();
		eachSpecialization(results, new ArrayList<SpecializeInfo>(specializations), new Stack<InjectPrimitiveInfo>());
		return results;
	}
	
	private static List<JCTree> specializeMethodDef(final Context context, final JCMethodDecl methodDecl, final Map<String, SpecializeInfo> specializations) {
		List<JCTree> specializedMethods = List.nil();
		
		boolean hasGeneric = false;
		for (final SpecializeInfo info : specializations.values())
			hasGeneric |= info.generic;
		
		for (final Map<String, InjectPrimitiveInfo> injections : eachSpecialization(specializations.values())) {
			final JCMethodDecl specialized = injectPrimitive(context, Utils.copyTree(context, methodDecl), injections);
			
			// modify type parameters
			List<JCTypeParameter> genericArguments = List.nil();
			for (final JCTypeParameter typaram : methodDecl.typarams) {
				if (injections.containsKey(typaram.toString()))
					continue;
				genericArguments = genericArguments.append(typaram);
			}
			specialized.typarams = genericArguments;
			
			// some clean up
			Utils.removeAnnotation(specialized, Specialize.class);
			Utils.removeAnnotation(specialized, Specializes.class);
			
			specializedMethods = specializedMethods.append(specialized);
		}
		
		if (hasGeneric)
			specializedMethods = specializedMethods.append(methodDecl);
		
		return specializedMethods;
	}
	
	private static JCClassDecl specializeClassDef(final Context context, final JCClassDecl classDecl, final Map<String, SpecializeInfo> specializations) {
		final Names names = Names.instance(context);
		
		boolean hasGeneric = false;
		for (final SpecializeInfo info : specializations.values())
			hasGeneric |= info.generic;
		
		List<JCTree> specializedClasses = List.nil();
		
		for (final Map<String, InjectPrimitiveInfo> injections : eachSpecialization(specializations.values())) {
			final JCClassDecl specialized = injectPrimitive(context, Utils.copyTree(context, classDecl), injections);
			
			// modify class name and parameter
			final StringBuilder specializedName = new StringBuilder("$specialized");
			List<JCTypeParameter> genericArguments = List.nil();

			for (final JCTypeParameter typaram: classDecl.typarams) {
				if (injections.containsKey(typaram.toString())) {
					final InjectPrimitiveInfo injection = injections.get(typaram.toString());
					specializedName.append("$" + injection.type.getSimpleName());
				} else {
					genericArguments = genericArguments.append(typaram);
					specializedName.append("$_");
				}
			}
			
			specialized.name = names.fromString(specializedName.toString());
			specialized.typarams = genericArguments;
			
			// FIXME: this should be done only if original class is a top level class.
			specialized.mods.flags |= Modifier.STATIC;
			
			// some clean up
			Utils.removeAnnotation(specialized, Specialize.class);
			Utils.removeAnnotation(specialized, Specializes.class);
			
			specializedClasses = specializedClasses.append(specialized);
		}
		
		if (!hasGeneric)
			classDecl.defs = List.nil();
		
		classDecl.defs = classDecl.defs.appendList(specializedClasses);
		
		return classDecl;
	}
	
	private static JCExpression specializeTypeApply(final Context context, final JCTypeApply typeApply) {
		final TreeMaker treeMaker = TreeMaker.instance(context);
		final Names names = Names.instance(context);
		
		final StringBuilder specializedName = new StringBuilder("$specialized");
		List<JCExpression> genericArguments = List.<JCExpression>nil();
		List<JCExpression> primitiveArguments = List.<JCExpression>nil();
		
		for (final JCExpression argument : typeApply.arguments) {
			if (argument instanceof JCPrimitiveTypeTree) {
				primitiveArguments = primitiveArguments.append(argument);
				specializedName.append("$" + argument.toString());
			} else {
				genericArguments = genericArguments.append(argument);
				specializedName.append("$_");
			}
		}

		if (primitiveArguments.isEmpty())
			return typeApply;
		
		final JCExpression specializedClass = treeMaker.Select(typeApply.clazz, names.fromString(specializedName.toString()));
		
		if (genericArguments.isEmpty()) {
			// fully specialized
			return specializedClass;
		} else {
			// apply remaining type argument
			final JCTypeApply result = treeMaker.TypeApply(specializedClass, genericArguments);
			return result;
		}
	}
	
	private static InjectPrimitiveInfo readInjectPrimitiveInfo(final EvaluateUtils.AnnotationInfo injectPrimitive) {
		if (injectPrimitive.annotationClassName == null || !injectPrimitive.annotationClassName.endsWith(InjectPrimitive.class.getSimpleName())) {
			log.error("Unsupported annotation: {}, expecting {}", injectPrimitive, InjectPrimitive.class.getSimpleName());
			return null; // TODO: report error
		}
					
		final EvaluateUtils.AnnotationInfo.Value keyObject = injectPrimitive.values.get("key");
		if (keyObject == null || keyObject.value == null || !(keyObject.value instanceof String)) {
			log.error("No valid 'key' attribute found: {}", keyObject);
			return null; // TODO: report error
		}

		final EvaluateUtils.AnnotationInfo.Value typeObject = injectPrimitive.values.get("type");
		if (typeObject == null || typeObject.value == null || !(typeObject.value instanceof Class)) {
			log.error("No valid 'type' attribute found: {}", typeObject);
			return null; // TODO: report error
		}
		
		final String key = (String) keyObject.value;
		final Class<?> type = (Class<?>) typeObject.value;
		
		return new InjectPrimitiveInfo(key, type);
	}
	
	private static SpecializeInfo readSpecializeInfo(final EvaluateUtils.AnnotationInfo specialize) {
		if (specialize.annotationClassName == null || !specialize.annotationClassName.endsWith(Specialize.class.getSimpleName())) {
			log.error("Unsupported annotation: {}, expecting {}", specialize, Specialize.class.getSimpleName());
			return null; // TODO: report error
		}
		
		final EvaluateUtils.AnnotationInfo.Value keyObject = specialize.values.get("key");
		if (keyObject == null || keyObject.value == null || !(keyObject.value instanceof String)) {
			log.error("No valid 'key' attribute found: {}", keyObject);
			return null; // TODO: report error
		}

		final EvaluateUtils.AnnotationInfo.Value typeObject = specialize.values.get("type");
		if (typeObject == null || typeObject.value == null || !(typeObject.value instanceof ArrayList || typeObject.value instanceof Class)) {
			log.error("No valid 'type' attribute found: {}", typeObject);
			return null; // TODO: report error
		}
		
		final EvaluateUtils.AnnotationInfo.Value genericObject = specialize.values.get("generic");
		if (genericObject != null && (genericObject.value == null || !(genericObject.value instanceof Integer))) {
			log.error("Invalid 'generic' attribute: {}", genericObject);
			return null; // TODO: report error
		}
		
		final String key = (String) keyObject.value;
		final Boolean generic = genericObject != null
				? ((int) genericObject.value != 0)
				: (Boolean) AnnotationUtils.getDefaultValue(Specialize.class, "generic");
		
		final ArrayList<Class<?>> types = new ArrayList<Class<?>>();
		if (typeObject.value instanceof ArrayList) {
			for (final Object type : (ArrayList<?>) typeObject.value) {
				if (!(type instanceof Class)) {
					log.error("Invalid type in 'type' attribute: {}", type);
					return null;
				}
				types.add((Class<?>) type);
			}
		}
		if (typeObject.value instanceof Class) {
			types.add((Class<?>) typeObject.value);
		}
		
		return new SpecializeInfo(key, types, generic);
	}
	
	private static class SpecializeInfo {
		public final String key;
		public final ArrayList<Class<?>> types;
		public final boolean generic;
		
		public SpecializeInfo(final String key, final ArrayList<Class<?>> types, final boolean generic) {
			this.key = key;
			this.types = new ArrayList<Class<?>>(types);
			this.generic = generic;
		}

		@Override
		public String toString() {
			return String.format("{key: \"%s\", types: %s, generic: %s}", key, types, String.valueOf(generic));
		}
	}

	private static class InjectPrimitiveInfo {
		public final String key;
		public final Class<?> type;

		public InjectPrimitiveInfo(final String key, final Class<?> type) {
			this.key = key;
			this.type = type;
		}

		@Override
		public String toString() {
			return String.format("{key: \"%s\", type: %s}", key, type);
		}
	}
	
	private Map<String, SpecializeInfo> readAllSpecializedInfo(final JCTree tree) {
		final JCAnnotation specializeExpr = Utils.getAnnotation(tree, Specialize.class);
		final JCAnnotation specializesExpr = Utils.getAnnotation(tree, Specializes.class);

		if (specializeExpr == null && specializesExpr == null)
			return null;

		final Map<String, SpecializeInfo> specializations = new HashMap<String, SpecializeInfo>();

		if (specializeExpr != null) {
			final EvaluateUtils.AnnotationInfo specialize = EvaluateUtils.evaluateAnnotation((JCAnnotation) specializeExpr);
			log.debug("Found annotation: {}", specialize);

			final SpecializeInfo specialization = readSpecializeInfo(specialize);
			if (specialization == null)
				return null; // TODO: report error
			
			specializations.put(specialization.key, specialization);
		}

		if (specializesExpr != null) {
			EvaluateUtils.AnnotationInfo specializes = EvaluateUtils.evaluateAnnotation((JCAnnotation) specializesExpr);
			log.debug("Found annotation: {}", specializes);

			final EvaluateUtils.AnnotationInfo.Value valueObject = specializes.values.get("value");
			if (valueObject == null || valueObject.value == null || !(valueObject.value instanceof ArrayList)) {
				log.error("No valid 'value' attribute found: {}", valueObject);
				return null; // TODO: report error
			}

			for (final Object specializeObject : (ArrayList<?>) valueObject.value) {
				if (!(specializeObject instanceof EvaluateUtils.AnnotationInfo)) {
					log.error("Invalid type element for @InjectPrimitives.value(): {}", valueObject);
					return null; // TODO: report error
				}

				final EvaluateUtils.AnnotationInfo specialize = (EvaluateUtils.AnnotationInfo) specializeObject;
				final SpecializeInfo specialization = readSpecializeInfo(specialize);
				if (specialization == null)
					return null; // TODO: report error
				
				specializations.put(specialization.key, specialization);
			}
		}
		
		return specializations;
	}
	
	public Map<String, InjectPrimitiveInfo> readAllInjectPrimitiveInfo(final JCTree tree) {
		final JCAnnotation injectPrimitiveExpr = Utils.getAnnotation(tree, InjectPrimitive.class);
		final JCAnnotation injectPrimitivesExpr = Utils.getAnnotation(tree, InjectPrimitives.class);

		if (injectPrimitiveExpr == null && injectPrimitivesExpr == null)
			return null;

		final Map<String, InjectPrimitiveInfo> injections = new HashMap<String, InjectPrimitiveInfo>();

		if (injectPrimitiveExpr != null) {
			final EvaluateUtils.AnnotationInfo injectPrimitive = EvaluateUtils.evaluateAnnotation((JCAnnotation) injectPrimitiveExpr);
			log.debug("Found annotation: {}", injectPrimitive);

			final InjectPrimitiveInfo injection = readInjectPrimitiveInfo(injectPrimitive);
			if (injection == null) {
				return null; // TODO: report error
			}
			injections.put(injection.key, injection);
		}

		if (injectPrimitivesExpr != null) {
			EvaluateUtils.AnnotationInfo injectPrimitives = EvaluateUtils.evaluateAnnotation((JCAnnotation) injectPrimitivesExpr);
			log.debug("Found annotation: {}", injectPrimitives);

			final EvaluateUtils.AnnotationInfo.Value valueObject = injectPrimitives.values.get("value");
			if (valueObject == null || valueObject.value == null || !(valueObject.value instanceof ArrayList)) {
				log.error("No valid 'value' attribute found: {}", valueObject);
				return null; // TODO: report error
			}

			for (final Object injectPrimitiveObject : (ArrayList<?>) valueObject.value) {
				if (!(injectPrimitiveObject instanceof EvaluateUtils.AnnotationInfo)) {
					log.error("Invalid type element for @InjectPrimitives.value(): {}", valueObject);
					return null; // TODO: report error
				}

				final EvaluateUtils.AnnotationInfo injectPrimitive = (EvaluateUtils.AnnotationInfo) injectPrimitiveObject;
				final InjectPrimitiveInfo injection = readInjectPrimitiveInfo(injectPrimitive);
				if (injection == null) {
					return null; // TODO: report error
				}
				injections.put(injection.key, injection);
			}
		}

		return injections;
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		@SuppressWarnings("unused")
		final Messager messager = processingEnv.getMessager();
		final JavacProcessingEnvironment javacProcessingEnv = (JavacProcessingEnvironment) processingEnv;
		final Trees trees = Trees.instance(processingEnv);
		final Context context = javacProcessingEnv.getContext();
		
//		for (final Element element : roundEnv.getElementsAnnotatedWith(Specialize.class)) {
//			Specialize annotation = element.getAnnotation(Specialize.class);
//			log.info("annotation: {}", annotation);
//		}
		
		for (final Element rootElement : roundEnv.getRootElements()) {
			final JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(rootElement).getCompilationUnit();
			
			// ignore other than sources
			if (unit.getSourceFile().getKind() != JavaFileObject.Kind.SOURCE)
				continue;
			
			// Specialize functions
			unit.accept(new TreeTranslator() {
				@Override
				public void visitClassDef(final JCClassDecl tree) {
					List<JCTree> defs = List.nil();
					
					for (final JCTree member : tree.defs) {
						if (!(member instanceof JCMethodDecl)) {
							defs = defs.append(member);
							continue;
						}
						
						final JCMethodDecl methodDecl = (JCMethodDecl) member;
						
						final Map<String, SpecializeInfo> specializations = readAllSpecializedInfo(methodDecl);
						if (specializations == null) {
							defs = defs.append(member);
							continue;
						}
						
						log.info("Generate specializations of {}.{}({}).", new Object[] { tree.name, methodDecl.name, methodDecl.params });
						defs = defs.appendList(specializeMethodDef(context, methodDecl, specializations));
					}
					
					tree.defs = defs;
					result = tree;
				}
			});
			
			// Specialize classes
			unit.accept(new TreeTranslator() {
				@Override
				public void visitClassDef(final JCClassDecl tree) {
					final Map<String, SpecializeInfo> specializations = readAllSpecializedInfo(tree);
					if (specializations == null) {
						super.visitClassDef(tree);
						return;
					}

					log.info("Generate specializations of {}.", tree.name);
					result = specializeClassDef(context, tree, specializations);
				}
			});
			
			// InjectPrimitive on methods
			unit.accept(new TreeTranslator() {
				@Override
				public void visitMethodDef(JCMethodDecl tree) {
					final Map<String, InjectPrimitiveInfo> injections = readAllInjectPrimitiveInfo(tree);
					if (injections == null) {
						super.visitMethodDef(tree);
						return;
					}
					
					result = injectPrimitive(context, tree, injections);
					log.info("Inject {} on {}.", injections.values(), tree.name);
				}
			});
			

			// InjectPrimitive on classes
			unit.accept(new TreeTranslator() {
				@Override
				public void visitClassDef(JCClassDecl tree) {
					final Map<String, InjectPrimitiveInfo> injections = readAllInjectPrimitiveInfo(tree);
					if (injections == null) {
						super.visitClassDef(tree);
						return;
					}
					
					result = injectPrimitive(context, tree, injections);
					log.info("Inject {} on {}.", injections.values(), tree.name);
				}
			});
			
			// Specialize type names
			unit.accept(new TreeTranslator() {
				@Override
				public void visitTypeApply(JCTypeApply tree) {
					result = specializeTypeApply(context, tree);
					if (result != tree)
						log.info("Rewrite {} to {}.", tree, result);
				}
			});

			final File out = new File(unit.sourcefile.getName() + ".specialized");
			try {
				Utils.dumpSource(out, unit);
			} catch (IOException e) {
				log.warn("Cannot write to {}.", out);
			};
		}
		
		return true;
	}
	
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.values()[SourceVersion.values().length - 1];
	}
}
