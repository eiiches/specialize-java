package net.thisptr.specialize.processor.internal.javac.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;

public class Utils {
	private static Logger log = LoggerFactory.getLogger(Utils.class);

	public static JCAnnotation getAnnotation(final JCModifiers modifiers, final Class<?> annotationClass) {
		for (JCAnnotation annotation : modifiers.getAnnotations())
			if (annotation.getAnnotationType().toString().equals(annotationClass.getSimpleName()))
				return annotation;
		return null;
	}

	public static JCAnnotation getAnnotation(final JCVariableDecl decl, final Class<?> annotationClass) {
		return getAnnotation(decl.getModifiers(), annotationClass);
	}

	public static JCAnnotation getAnnotation(final JCMethodDecl decl, final Class<?> annotationClass) {
		return getAnnotation(decl.getModifiers(), annotationClass);
	}

	public static JCAnnotation getAnnotation(final JCClassDecl decl, final Class<?> annotationClass) {
		return getAnnotation(decl.getModifiers(), annotationClass);
	}

	public static JCAnnotation getAnnotation(final JCTree tree, final Class<?> annotationClass) {
		if (tree instanceof JCClassDecl)
			return getAnnotation((JCClassDecl) tree, annotationClass);
		if (tree instanceof JCMethodDecl)
			return getAnnotation((JCMethodDecl) tree, annotationClass);
		if (tree instanceof JCVariableDecl)
			return getAnnotation((JCVariableDecl) tree, annotationClass);
		if (tree instanceof JCModifiers)
			return getAnnotation((JCModifiers) tree, annotationClass);
		return null;
	}

	public static JCClassDecl removeAnnotation(final JCClassDecl decl, final Class<?> annotationClass) {
		final List<JCAnnotation> annotations = new ArrayList<JCAnnotation>();
		for (JCAnnotation annotation : decl.mods.annotations) {
			if (annotation.getAnnotationType().toString().equals(annotationClass.getSimpleName()))
				continue;

			annotations.add(annotation);
		}
		decl.mods.annotations = toImmutableList(annotations);
		return decl;
	}

	public static JCMethodDecl removeAnnotation(final JCMethodDecl decl, final Class<?> annotationClass) {
		final List<JCAnnotation> annotations = new ArrayList<JCAnnotation>();
		for (JCAnnotation annotation : decl.mods.annotations) {
			if (annotation.getAnnotationType().toString().equals(annotationClass.getSimpleName()))
				continue;

			annotations.add(annotation);
		}
		decl.mods.annotations = toImmutableList(annotations);
		return decl;
	}

	public static boolean hasAnnotation(final JCModifiers modifiers, final Class<?> annotationClass) {
		return getAnnotation(modifiers, annotationClass) != null;
	}

	public static boolean hasAnnotation(final JCVariableDecl decl, final Class<?> annotationClass) {
		return getAnnotation(decl, annotationClass) != null;
	}

	public static boolean hasAnnotation(final JCMethodDecl decl, final Class<?> annotationClass) {
		return getAnnotation(decl, annotationClass) != null;
	}

	public static boolean hasAnnotation(final JCClassDecl decl, final Class<?> annotationClass) {
		return getAnnotation(decl, annotationClass) != null;
	}

	public static String debug(final JCTree tree) {
		if (tree instanceof JCNewClass) {
			return null;
		} else {
			return tree.toString();
		}
	}

	public static <T extends JCTree> T copyTree(final Context context, final T tree) {
		return (T) new TreeCopier<Void>(TreeMaker.instance(context)).copy(tree);
	}

	public static void dumpSource(final File file, final JCTree tree) throws IOException {
		try (final FileWriter writer = new FileWriter(file)) {
			tree.accept(new Pretty(writer, true));
		}

	}

	public static Type toType(final Context context, final String type) {
		final Symtab symtab = Symtab.instance(context);

		if ("int".equals(type)) return  symtab.intType;
		if ("short".equals(type)) return  symtab.shortType;
		if ("long".equals(type)) return  symtab.longType;
		if ("byte".equals(type)) return  symtab.byteType;
		if ("char".equals(type)) return  symtab.charType;
		if ("boolean".equals(type)) return  symtab.booleanType;
		if ("float".equals(type)) return  symtab.floatType;
		if ("double".equals(type)) return  symtab.doubleType;

		log.error("Unhandled class type: {}", type);
		return null;
	}

	public static <T> List<T> toMutableList(final List<T> immutableList) {
		return new LinkedList<T>(immutableList);
	}

	public static <T> com.sun.tools.javac.util.List<T> toImmutableList(final List<T> mutableList) {
		com.sun.tools.javac.util.List<T> result = com.sun.tools.javac.util.List.nil();
		for (final T item : mutableList)
			result = result.append(item);
		return result;
	}
	
	@SafeVarargs
	public static <T> com.sun.tools.javac.util.List<T> toImmutableList(final T... items) {
		return toImmutableList(Arrays.asList(items));
	}
	
	public static <T> com.sun.tools.javac.util.List<T> emptyImmutableList() {
		return toImmutableList();
	}

	public static void removeImport(final JCCompilationUnit unit, final Class<?> clazz) {
		final List<JCTree> defs = new ArrayList<JCTree>();
		for (final JCTree tree : unit.defs) {
			if (tree instanceof JCImport) {
				final JCImport imp = (JCImport) tree;
				if (clazz.getCanonicalName().equals(imp.qualid.toString()))
					continue;
			}
			defs.add(tree);
		}
		unit.defs = toImmutableList(defs);
	}
}
