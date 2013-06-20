package net.thisptr.specialize.processor.internal.javac;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import net.thisptr.specialize.Specialize;
import net.thisptr.specialize.processor.internal.InjectInfo;
import net.thisptr.specialize.processor.internal.SpecializeInfo;
import net.thisptr.specialize.processor.internal.javac.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
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
public class JavacProcessor extends AbstractProcessor {
	private static Logger log = LoggerFactory.getLogger(JavacProcessor.class);

	private static String[] primitiveTypes = new String[] {
			"int", "short", "char", "byte", "long", "float", "double", "boolean"
	};

	private static Map<String, String> dollaredTypes = new HashMap<String, String>();
	static {
		for (final String primitiveType : primitiveTypes)
			dollaredTypes.put("$" + primitiveType, primitiveType);
	}

	private static <T extends JCTree> T replaceIdentifierWithPrimitive(final Context context, final T tree, final Map<String, String> mapping) {
		final TreeMaker treeMaker = TreeMaker.instance(context);

		tree.accept(new TreeTranslator() {
			@Override
			public void visitIdent(final JCIdent tree) {
				if (!mapping.containsKey(tree.toString())) {
					super.visitIdent(tree);
					return;
				}

				result = treeMaker.Type(Utils.toType(context, mapping.get(tree.toString())));
			}
		});

		return tree;
	}

	private static <T extends JCTree> T injectPrimitive(final Context context, final T tree, final InjectInfo injectInfo) {
		final TreeMaker treeMaker = TreeMaker.instance(context);

		tree.accept(new TreeTranslator() {
			@Override
			public void visitIdent(final JCIdent tree) {
				if (!injectInfo.contains(tree.toString())) {
					super.visitIdent(tree);
					return;
				}

				final InjectInfo.TypeParam typeParam = injectInfo.get(tree.toString());
				result = treeMaker.Type(Utils.toType(context, typeParam.type));
			}
		});

		return tree;
	}

	private static java.util.List<JCMethodDecl> specializeMethodDef(final Context context, final JCMethodDecl methodDecl, final SpecializeInfo specializeInfo) {
		final java.util.List<JCMethodDecl> result = new ArrayList<JCMethodDecl>();

		for (final InjectInfo injectInfo : specializeInfo.getTypeCombinations()) {
			final JCMethodDecl specialized = injectPrimitive(context, Utils.copyTree(context, methodDecl), injectInfo);

			// modify type parameters
			List<JCTypeParameter> genericArguments = List.nil();
			for (final JCTypeParameter typaram : methodDecl.typarams) {
				if (injectInfo.contains(typaram.toString()))
					continue;
				genericArguments = genericArguments.append(typaram);
			}
			specialized.typarams = genericArguments;

			// some clean up
			Utils.removeAnnotation(specialized, Specialize.class);

			result.add(specialized);
		}

		return result;
	}

	private static java.util.List<JCClassDecl> specializeClassDef(final Context context, final JCClassDecl classDecl, final SpecializeInfo specializeInfo) {
		final Names names = Names.instance(context);

		final java.util.List<JCClassDecl> result = new ArrayList<JCClassDecl>();

		for (final InjectInfo injectInfo : specializeInfo.getTypeCombinations()) {
			final JCClassDecl specialized = injectPrimitive(context, Utils.copyTree(context, classDecl), injectInfo);

			// modify class name and parameter
			final StringBuilder specializedName = new StringBuilder("$specialized");
			List<JCTypeParameter> genericArguments = List.nil();

			for (final JCTypeParameter typaram: classDecl.typarams) {
				if (injectInfo.contains(typaram.toString())) {
					final InjectInfo.TypeParam typeParam = injectInfo.get(typaram.toString());
					specializedName.append("$" + typeParam.type);
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

			result.add(specialized);
		}

		return result;
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

	private Messager messager;

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		if (roundEnv.processingOver())
			return true;

		messager.printMessage(Kind.NOTE, "Running specialize-java.");

		final JavacProcessingEnvironment javacProcessingEnv = (JavacProcessingEnvironment) processingEnv;
		final Trees trees = Trees.instance(processingEnv);
		final Context context = javacProcessingEnv.getContext();

		// Replace $primitive types.
		for (final Element rootElement : roundEnv.getRootElements()) {
			final JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(rootElement).getCompilationUnit();

			// ignore other than sources
			if (unit.getSourceFile().getKind() != JavaFileObject.Kind.SOURCE)
				continue;

			replaceIdentifierWithPrimitive(context, unit, dollaredTypes);
		}

//		// InjectPrimitive
//		for (final Element element : roundEnv.getElementsAnnotatedWith(InjectPrimitive.class)) {
//			final InjectPrimitive annotation = element.getAnnotation(InjectPrimitive.class);
//			log.info("annotation: {}", annotation);
//
//			final InjectInfo injectInfo = InjectInfo.parse(annotation);
//
//			final Tree annotated = trees.getTree(element);
//
//			injectPrimitive(context, (JCTree) annotated, injectInfo);
//		}

		// Specialize
		for (final Element element : roundEnv.getElementsAnnotatedWith(Specialize.class)) {
			final Specialize annotation = element.getAnnotation(Specialize.class);
			log.info("annotation: {}", annotation);

			final SpecializeInfo specializeInfo = SpecializeInfo.parse(annotation);

			final Tree annotated = trees.getTree(element);
			final Tree enclosure = trees.getTree(element.getEnclosingElement());

			// TODO: anonymous inner class?
			if (annotated instanceof JCMethodDecl && enclosure instanceof JCClassDecl) {
				final JCMethodDecl methodDecl = (JCMethodDecl) annotated;
				final JCClassDecl classDecl = (JCClassDecl) enclosure;

				final java.util.List<JCTree> defs = Utils.toMutableList(classDecl.defs);

				if (!specializeInfo.isGenericAllowed())
					defs.remove(methodDecl);

				defs.addAll(specializeMethodDef(context, methodDecl, specializeInfo));

				classDecl.defs = Utils.toImmutableList(defs);
			} else if (annotated instanceof JCClassDecl) {
				log.info("Enclosure: {}", enclosure);
				final JCClassDecl classDecl = (JCClassDecl) annotated;

				final java.util.List<JCTree> defs = Utils.toMutableList(classDecl.defs);

				if (!specializeInfo.isGenericAllowed())
					defs.clear(); // TODO: add private ctor to prevent instantiation

				defs.addAll(specializeClassDef(context, classDecl, specializeInfo));

				classDecl.defs = Utils.toImmutableList(defs);
			} else {
				log.error("Unhandled, enclosure: {}, element: {}", enclosure.getClass(), annotated.getClass());
			}
		}

		// Specialize type apply
		for (final Element rootElement : roundEnv.getRootElements()) {
			final JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(rootElement).getCompilationUnit();

			// ignore other than sources
			if (unit.getSourceFile().getKind() != JavaFileObject.Kind.SOURCE)
				continue;

			// Specialize type names
			unit.accept(new TreeTranslator() {
				@Override
				public void visitTypeApply(JCTypeApply tree) {
					result = specializeTypeApply(context, tree);
					if (result != tree)
						log.info("Rewrite {} to {}.", tree, result);
				}
			});
		}

		// TODO: トップレベルクラスの特殊化は別ファイルにしてしまうのも要検討
		//   他のクラスについても同階層にクラスを作ることになるので、統一感
		//   inner classにしないので、non-static内部クラスも扱えるようになる

		// TODO: remove import statements.

		// Dump
		for (final Element rootElement : roundEnv.getRootElements()) {
			final JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(rootElement).getCompilationUnit();

			// ignore other than sources
			if (unit.getSourceFile().getKind() != JavaFileObject.Kind.SOURCE)
				continue;

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
		messager = processingEnv.getMessager();
	}
}
