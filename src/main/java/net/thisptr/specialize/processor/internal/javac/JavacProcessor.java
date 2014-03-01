package net.thisptr.specialize.processor.internal.javac;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import net.thisptr.specialize.processor.internal.IOUtils;
import net.thisptr.specialize.processor.internal.InjectInfo;
import net.thisptr.specialize.processor.internal.SpecializeInfo;
import net.thisptr.specialize.processor.internal.javac.util.AstEditor;
import net.thisptr.specialize.processor.internal.javac.util.Modifications;
import net.thisptr.specialize.processor.internal.javac.util.UnhandledSyntaxException;
import net.thisptr.specialize.processor.internal.javac.util.Utils;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

@SupportedAnnotationTypes("*")
public class JavacProcessor extends AbstractProcessor {
	private static String[] primitiveTypes = new String[] {
			"int", "short", "char", "byte", "long", "float", "double", "boolean"
	};

	private static Map<String, String> dollaredTypes = new HashMap<String, String>();
	static {
		for (final String primitiveType : primitiveTypes)
			dollaredTypes.put("$" + primitiveType, primitiveType);
	}

	private <T extends JCTree> T injectPrimitive(final Context context, final T tree, final InjectInfo injectInfo) {
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
				final List<JCExpression> arguments = new ArrayList<JCExpression>();
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
						messager.printMessage(Kind.WARNING, String.format("Unhandled TypeApply argument type: %s[%s]", argument.toString(), argument.getClass()));
						arguments.add(argument);
					}
				}
				tree.arguments = Utils.toImmutableList(arguments);
				result = tree;
			}
		});

		return tree;
	}

	// TODO: verify type parameters in method signature.
	private List<JCMethodDecl> specializeMethodDef(final Context context, final JCMethodDecl methodDecl, final SpecializeInfo specializeInfo) {
		final List<JCMethodDecl> result = new ArrayList<JCMethodDecl>();

		for (final InjectInfo injectInfo : specializeInfo.getTypeCombinations()) {
			final JCMethodDecl specialized = injectPrimitive(context, Utils.copyTree(context, methodDecl), injectInfo);

			// modify type parameters
			final List<JCTypeParameter> genericArguments = new ArrayList<JCTypeParameter>();
			for (final JCTypeParameter typaram : methodDecl.typarams) {
				if (injectInfo.contains(typaram.toString()))
					continue;
				genericArguments.add(typaram);
			}
			specialized.typarams = Utils.toImmutableList(genericArguments);

			// some clean up
			Modifications.removeAnnotation(specialized, Specialize.class);

			result.add(specialized);
		}

		return result;
	}

	// TODO: verify type parameters in class signature.
	private List<JCClassDecl> specializeClassDef(final Context context, final JCClassDecl classDecl, final SpecializeInfo specializeInfo) {
		final Names names = Names.instance(context);

		final List<JCClassDecl> result = new ArrayList<JCClassDecl>();

		for (final InjectInfo injectInfo : specializeInfo.getTypeCombinations()) {
			final JCClassDecl specialized = injectPrimitive(context, Utils.copyTree(context, classDecl), injectInfo);

			// modify class name and parameter
			final StringBuilder specializedName = new StringBuilder(classDecl.name.toString() + "$specialized");
			final List<JCTypeParameter> genericArguments = new ArrayList<JCTypeParameter>();

			for (final JCTypeParameter typaram : classDecl.typarams) {
				if (injectInfo.contains(typaram.toString())) {
					final InjectInfo.TypeParam typeParam = injectInfo.get(typaram.toString());
					specializedName.append("$" + typeParam.type);
				} else {
					genericArguments.add(typaram);
					specializedName.append("$_");
				}
			}

			specialized.name = names.fromString(specializedName.toString());
			specialized.typarams = Utils.toImmutableList(genericArguments);

			// some clean up
			Modifications.removeAnnotation(specialized, Specialize.class);

			result.add(specialized);
		}

		return result;
	}

	private JCExpression specializeTypeApply(final Context context, final JCTypeApply typeApply) {
		final TreeMaker treeMaker = TreeMaker.instance(context);
		final Names names = Names.instance(context);

		final StringBuilder specializedName = new StringBuilder();
		if (typeApply.clazz instanceof JCFieldAccess) {
			specializedName.append(((JCFieldAccess) typeApply.clazz).name.toString());
			specializedName.append("$specialized");
		} else if (typeApply.clazz instanceof JCIdent) {
			specializedName.append(((JCIdent) typeApply.clazz).toString());
			specializedName.append("$specialized");
		} else {
			throw new UnhandledSyntaxException(typeApply);
		}

		final List<JCExpression> genericArguments = new ArrayList<JCExpression>();
		final List<JCExpression> primitiveArguments = new ArrayList<JCExpression>();

		for (final JCExpression argument : typeApply.arguments) {
			if (argument instanceof JCIdent && dollaredTypes.containsKey(argument.toString())) {
				primitiveArguments.add(argument);
				specializedName.append("$" + dollaredTypes.get(argument.toString()));
			} else if (argument instanceof JCTypeApply) {
				genericArguments.add(specializeTypeApply(context, (JCTypeApply) argument));
				specializedName.append("$_");
			} else {
				genericArguments.add(argument);
				specializedName.append("$_");
			}
		}

		if (primitiveArguments.isEmpty())
			return typeApply;

		final JCExpression specializedClass = Utils.copyTree(context, typeApply.clazz);
		if (typeApply.clazz instanceof JCFieldAccess) {
			((JCFieldAccess) specializedClass).name = names.fromString(specializedName.toString());
		} else if (typeApply.clazz instanceof JCIdent) {
			((JCIdent) specializedClass).name = names.fromString(specializedName.toString());
		} else {
			throw new UnhandledSyntaxException(typeApply);
		}

		if (genericArguments.isEmpty()) {
			// fully specialized
			return specializedClass;
		} else {
			// apply remaining type argument
			final JCTypeApply result = treeMaker.TypeApply(specializedClass, Utils.toImmutableList(genericArguments));
			return result;
		}
	}

	private Messager messager;

	public static void info(final JCCompilationUnit unit, final String format, final Object... args) {
		final File base = new File(System.getProperty("user.dir"));

		File src = new File(unit.sourcefile.getName());
		if (src.getPath().startsWith(base.getPath() + "/"))
			src = new File(src.getPath().substring(base.getPath().length() + 1));

		System.out.println("[specialize-java] " + src.getPath() + ": " + String.format(format, args));
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		if (roundEnv.processingOver())
			return true;

		messager.printMessage(Kind.WARNING, "Running specialize-java.");

		final JavacProcessingEnvironment javacProcessingEnv = (JavacProcessingEnvironment) processingEnv;
		final Trees trees = Trees.instance(processingEnv);
		final Context context = javacProcessingEnv.getContext();
		final Set<JCCompilationUnit> changedUnits = new HashSet<JCCompilationUnit>();

		// Specialize
		for (final Element element : roundEnv.getElementsAnnotatedWith(Specialize.class)) {
			final Specialize annotation = element.getAnnotation(Specialize.class);
			messager.printMessage(Kind.NOTE, String.format("Processing %s", annotation), element);

			final SpecializeInfo specializeInfo = SpecializeInfo.parse(annotation);

			final Tree annotated = trees.getTree(element);
			final Tree enclosure = trees.getTree(element.getEnclosingElement());
			final JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(element).getCompilationUnit();

			if (annotated instanceof JCMethodDecl && enclosure instanceof JCClassDecl) {
				final JCMethodDecl methodDecl = (JCMethodDecl) annotated;
				final JCClassDecl enclosureDecl = (JCClassDecl) enclosure;

				final List<JCTree> defs = Utils.toMutableList(enclosureDecl.defs);

				defs.addAll(specializeMethodDef(context, methodDecl, specializeInfo));
				Modifications.removeAnnotation(methodDecl, Specialize.class);

				if (!specializeInfo.shouldGenerateGeneric())
					defs.remove(methodDecl);

				enclosureDecl.defs = Utils.toImmutableList(defs);
			} else if (annotated instanceof JCClassDecl && enclosure instanceof JCClassDecl) {
				final JCClassDecl classDecl = (JCClassDecl) annotated;
				final JCClassDecl enclosureDecl = (JCClassDecl) enclosure;

				final List<JCTree> defs = Utils.toMutableList(enclosureDecl.defs);

				defs.addAll(specializeClassDef(context, classDecl, specializeInfo));
				Modifications.removeAnnotation(classDecl, Specialize.class);

				if (!specializeInfo.shouldGenerateGeneric())
					classDecl.defs = Utils.emptyImmutableList();

				enclosureDecl.defs = Utils.toImmutableList(defs);
			} else if (annotated instanceof JCClassDecl && enclosure == null) {
				final JCClassDecl classDecl = (JCClassDecl) annotated;

				if ((classDecl.mods.flags & Flags.PUBLIC) != 0) { // then the class is principal top level.
					try {
						for (final JCClassDecl specialized : specializeClassDef(context, classDecl, specializeInfo)) {
							final String name = "unnamed package".equals(unit.packge.toString())
									? specialized.name.toString()
									: unit.packge.toString() + "." + specialized.name.toString();

							final JavaFileObject specializedSource = processingEnv.getFiler().createSourceFile(name);
							final Writer writer = specializedSource.openWriter();
							try {
								final JCCompilationUnit copyUnit = Utils.copyTree(context, unit);

								// exclude top level classes, keeping import statements, etc.
								final List<JCTree> defs = new ArrayList<JCTree>();
								for (final JCTree def : copyUnit.defs) {
									if (def instanceof JCClassDecl)
										continue;
									defs.add(def);
								}
								defs.add(specialized);
								copyUnit.defs = Utils.toImmutableList(defs);

								copyUnit.accept(new Pretty(writer, true));
							} finally {
								IOUtils.closeQuietly(writer);
							}
						}
						Modifications.removeAnnotation(classDecl, Specialize.class);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					if (!specializeInfo.shouldGenerateGeneric())
						classDecl.defs = Utils.emptyImmutableList();

				} else { // non-public top level class, which can be specialized in-file.
					final List<JCTree> defs = Utils.toMutableList(unit.defs);

					defs.addAll(specializeClassDef(context, classDecl, specializeInfo));
					Modifications.removeAnnotation(classDecl, Specialize.class);

					if (!specializeInfo.shouldGenerateGeneric())
						classDecl.defs = Utils.emptyImmutableList();

					unit.defs = Utils.toImmutableList(defs);
				}
			} else {
				messager.printMessage(Kind.WARNING, String.format("Unhandled, enclosure: %s, element: %s", enclosure.getClass(), annotated.getClass()), element.getEnclosingElement());
			}

			changedUnits.add(unit);
		}

		// Specialize type apply
		for (final Element rootElement : roundEnv.getRootElements()) {
			final TreePath rootElementPath = trees.getPath(rootElement);
			if (rootElementPath == null) // e.g. package-info.java
				continue;

			final JCCompilationUnit unit = (JCCompilationUnit) rootElementPath.getCompilationUnit();

			// ignore other than sources
			if (unit.getSourceFile().getKind() != JavaFileObject.Kind.SOURCE)
				continue;

			final AstEditor editor = new AstEditor(context, unit);
			final List<String> pendingImports = new ArrayList<String>();

			// specialize-java-rt support
			for (final JCImport imp : editor.findImport("*")) {
				final String packageName = ((JCFieldAccess) imp.qualid).selected.toString(); // FIXME do not use toString
				if (packageName.startsWith("java.")) {
					try {
						final Package pkg = getClass().getClassLoader().loadClass("net.thisptr.specialize." + packageName + ".package-info").getPackage();
						if (pkg != null) {
							info(unit, "%s --> %s (copy import)", packageName + ".*", pkg.toString() + ".*");
							pendingImports.add(pkg.toString() + ".*");
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}

			// specialize type names
			unit.accept(new TreeTranslator() {
				@Override
				public void visitTypeApply(final JCTypeApply tree) {
					// specialize type name if needed
					result = specializeTypeApply(context, tree);
					// the instance is returned untouched if the type does not need specialization
					if (result == tree)
						return;

					messager.printMessage(Kind.NOTE, String.format("Rewriting %s to %s", tree, result));

					// add import if needed
					if (tree.clazz instanceof JCFieldAccess) {
						// nothing to do, because tree.clazz is a fully qualified type (with package name)
					} else if (tree.clazz instanceof JCIdent) {
						final List<JCImport> imports = editor.findImport(tree.clazz.toString());
						if (!imports.isEmpty()) {
							final JCImport unspecializedImport = imports.get(0);

							String className = null;
							if (result instanceof JCTypeApply) {
								className = ((JCIdent) ((JCTypeApply) result).clazz).name.toString();
							} else if (result instanceof JCIdent) {
								className = ((JCIdent) result).name.toString();
							} else {
								throw new UnhandledSyntaxException(result);
							}

							String packageName = null;
							if (unspecializedImport.qualid.toString().startsWith("java.")) {
								try {
									final String tmp = "net.thisptr.specialize." + ((JCFieldAccess) unspecializedImport.qualid).selected.toString();
									packageName = getClass().getClassLoader().loadClass(tmp + "." + className).getPackage().getName();
								} catch (ClassNotFoundException e) {
									e.printStackTrace();
									throw new RuntimeException("The specializations of the class are not defined. Maybe specializa-java-rt is missing in your classpath?", e);
								}
							} else {
								packageName = ((JCFieldAccess) unspecializedImport.qualid).selected.toString();
							}

							info(unit, "%s --> %s (copy import)", unspecializedImport.qualid, packageName + "." + className);
							pendingImports.add(packageName + "." + className);
						} else {
							// in this case, the class seems to be imported using wildcard, or the class is in the same package, which means,
							// there is no need to explicitly import the specialized class.
						}
					} else {
						throw new UnhandledSyntaxException(tree);
					}

					editor.setModified(true);
				}
			});

			for (final String pendingImport : pendingImports)
				editor.prependImport(pendingImport);
			editor.removeImports(Specialize.class);
			editor.removeWildcardImports(Specialize.class.getPackage());

			if (editor.isModified()) {
				final File out = new File(unit.sourcefile.getName() + ".specialized");
				try {
					Utils.dumpSource(out, unit);
				} catch (IOException e) {
					messager.printMessage(Kind.WARNING, String.format("Cannot write to %s", out));
				}
			}
		}

		return true;
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
	}
}
