package net.thisptr.specialize.processor.internal.javac;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.Pretty;
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

	private static <T extends JCTree> T injectPrimitive(final Context context, final T tree, final InjectInfo injectInfo) {
		final TreeMaker treeMaker = TreeMaker.instance(context);
		final Names names = Names.instance(context);

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

			@Override
			public void visitTypeApply(final JCTypeApply tree) {
				final java.util.List<JCExpression> arguments = new ArrayList<JCExpression>();
				for (final JCExpression argument : tree.arguments) {
					if (argument instanceof JCIdent) {
						if (injectInfo.contains(argument.toString())) {
							final InjectInfo.TypeParam typeParam = injectInfo.get(argument.toString());
							arguments.add(treeMaker.Ident(names.fromString("$" + typeParam.type)));
						} else {
							arguments.add(argument);
						}
					} else if (argument instanceof JCWildcard) {
						arguments.add(argument);
					} else if (argument instanceof JCTypeApply) {
						visitTypeApply((JCTypeApply) argument);
						arguments.add((JCTypeApply) result);
					} else {
						log.error("Unhandled TypeApply argument type: {}[{}]", argument.toString(), argument.getClass());
						arguments.add(argument);
					}
				}
				tree.arguments = Utils.toImmutableList(arguments);
				result = tree;
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
			final StringBuilder specializedName = new StringBuilder(classDecl.name.toString() + "$specialized");
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

			// some clean up
			Utils.removeAnnotation(specialized, Specialize.class);

			result.add(specialized);
		}

		return result;
	}

	private static JCExpression specializeTypeApply(final Context context, final JCTypeApply typeApply) {
		final TreeMaker treeMaker = TreeMaker.instance(context);
		final Names names = Names.instance(context);

		// FIXME: handle case when typeApply.clazz is an instance of JCFieldAccess
		final StringBuilder specializedName = new StringBuilder(typeApply.clazz + "$specialized");
		List<JCExpression> genericArguments = List.<JCExpression>nil();
		List<JCExpression> primitiveArguments = List.<JCExpression>nil();

		for (final JCExpression argument : typeApply.arguments) {
			if (argument instanceof JCIdent && dollaredTypes.containsKey(argument.toString())) {
				primitiveArguments = primitiveArguments.append(argument);
				specializedName.append("$" + dollaredTypes.get(argument.toString()));
			} else {
				genericArguments = genericArguments.append(argument);
				specializedName.append("$_");
			}
		}

		if (primitiveArguments.isEmpty())
			return typeApply;

		// final JCExpression specializedClass = treeMaker.Select(typeApply.clazz, names.fromString(specializedName.toString()));
		final JCExpression specializedClass = treeMaker.Ident(names.fromString(specializedName.toString()));

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
		final Set<JCCompilationUnit> changedUnits = new HashSet<JCCompilationUnit>();

		// Specialize
		for (final Element element : roundEnv.getElementsAnnotatedWith(Specialize.class)) {
			final Specialize annotation = element.getAnnotation(Specialize.class);
			log.info("annotation: {}", annotation);

			final SpecializeInfo specializeInfo = SpecializeInfo.parse(annotation);

			final Tree annotated = trees.getTree(element);
			final Tree enclosure = trees.getTree(element.getEnclosingElement());
			final JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(element).getCompilationUnit();

			// TODO: anonymous inner class?
			if (annotated instanceof JCMethodDecl && enclosure instanceof JCClassDecl) {
				final JCMethodDecl methodDecl = (JCMethodDecl) annotated;
				final JCClassDecl enclosureDecl = (JCClassDecl) enclosure;

				final java.util.List<JCTree> defs = Utils.toMutableList(enclosureDecl.defs);

				defs.addAll(specializeMethodDef(context, methodDecl, specializeInfo));
				Utils.removeAnnotation(methodDecl, Specialize.class);

				if (!specializeInfo.isGenericAllowed())
					defs.remove(methodDecl);

				enclosureDecl.defs = Utils.toImmutableList(defs);
			} else if (annotated instanceof JCClassDecl && enclosure instanceof JCClassDecl) {
				final JCClassDecl classDecl = (JCClassDecl) annotated;
				final JCClassDecl enclosureDecl = (JCClassDecl) enclosure;

				final java.util.List<JCTree> defs = Utils.toMutableList(enclosureDecl.defs);

				defs.addAll(specializeClassDef(context, classDecl, specializeInfo));
				Utils.removeAnnotation(classDecl, Specialize.class);

				if (!specializeInfo.isGenericAllowed())
					classDecl.defs = List.nil();

				enclosureDecl.defs = Utils.toImmutableList(defs);
			} else if (annotated instanceof JCClassDecl && enclosure == null) {
				final JCClassDecl classDecl = (JCClassDecl) annotated;

				if ((classDecl.mods.flags & Flags.PUBLIC) != 0) { // then the class is principal top level.
					try {
						for (final JCClassDecl specialized : specializeClassDef(context, classDecl, specializeInfo)) {
							final JavaFileObject specializedSource = processingEnv.getFiler().createSourceFile(unit.packge.toString() + "." + specialized.name);
							try (final Writer writer = specializedSource.openWriter()) {
								final JCCompilationUnit copyUnit = Utils.copyTree(context, unit);

								// exclude top level classes, keeping import statements, etc.
								final java.util.List<JCTree> defs = new ArrayList<JCTree>();
								for (final JCTree def : copyUnit.defs) {
									if (def instanceof JCClassDecl)
										continue;
									defs.add(def);
								}
								defs.add(specialized);
								copyUnit.defs = Utils.toImmutableList(defs);

								copyUnit.accept(new Pretty(writer, true));
							}
						}
						Utils.removeAnnotation(classDecl, Specialize.class);
					} catch (IOException e) {
						log.error("Cannot open source file for writing.");
						throw new RuntimeException(e);
					}

					if (!specializeInfo.isGenericAllowed())
						classDecl.defs = List.nil();

				} else { // non-public top level class, which can be specialized in-file.
					final java.util.List<JCTree> defs = Utils.toMutableList(unit.defs);

					defs.addAll(specializeClassDef(context, classDecl, specializeInfo));
					Utils.removeAnnotation(classDecl, Specialize.class);

					if (!specializeInfo.isGenericAllowed())
						classDecl.defs = List.nil();

					unit.defs = Utils.toImmutableList(defs);
				}
			} else {
				log.error("Unhandled, enclosure: {}, element: {}", enclosure.getClass(), annotated.getClass());
			}

			changedUnits.add(unit);
		}

		// Specialize type apply
		for (final Element rootElement : roundEnv.getRootElements()) {
			final JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(rootElement).getCompilationUnit();

			// ignore other than sources
			if (unit.getSourceFile().getKind() != JavaFileObject.Kind.SOURCE)
				continue;

			final boolean[] isChanged = new boolean[] { false };

			// Specialize type names
			unit.accept(new TreeTranslator() {
				@Override
				public void visitTypeApply(JCTypeApply tree) {
					result = specializeTypeApply(context, tree);
					if (result != tree) {
						isChanged[0] = true;
						log.info("Rewrite {} to {}.", tree, result);
					}
				}
			});

			if (isChanged[0])
				changedUnits.add(unit);
		}

		// TODO: remove import statements.

		// Dump
		for (final Element rootElement : roundEnv.getRootElements()) {
			final JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(rootElement).getCompilationUnit();
			if (!changedUnits.contains(unit))
				continue;

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
