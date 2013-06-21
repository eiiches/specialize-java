package net.thisptr.specialize.processor.internal.javac.util;

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;

public class Modifications {

	public static void removeNonStaticStarImport(final JCCompilationUnit unit, final Package packge) {
		final List<JCTree> defs = new ArrayList<JCTree>();
		for (final JCTree tree : unit.defs) {
			if (tree instanceof JCImport) {
				final JCImport imp = (JCImport) tree;
				if (!imp.staticImport && (packge.getName() + ".*").equals(imp.qualid.toString()))
					continue;
			}
			defs.add(tree);
		}
		unit.defs = Utils.toImmutableList(defs);
	}

	public static void removeNonStaticImport(final JCCompilationUnit unit, final Class<?> clazz) {
		final List<JCTree> defs = new ArrayList<JCTree>();
		for (final JCTree tree : unit.defs) {
			if (tree instanceof JCImport) {
				final JCImport imp = (JCImport) tree;
				if (!imp.staticImport && clazz.getCanonicalName().equals(imp.qualid.toString()))
					continue;
			}
			defs.add(tree);
		}
		unit.defs = Utils.toImmutableList(defs);
	}
	
	public static void removeAnnotation(final JCModifiers mods, final Class<?> annotationClass) {
		final List<JCAnnotation> annotations = new ArrayList<JCAnnotation>();
		for (JCAnnotation annotation : mods.annotations) {
			if (annotation.getAnnotationType().toString().equals(annotationClass.getSimpleName()))
				continue;
	
			annotations.add(annotation);
		}
		mods.annotations = Utils.toImmutableList(annotations);
	}

	public static void removeAnnotation(final JCClassDecl decl, final Class<?> annotationClass) {
		removeAnnotation(decl.mods, annotationClass);
	}

	public static void removeAnnotation(final JCMethodDecl decl, final Class<?> annotationClass) {
		removeAnnotation(decl.mods, annotationClass);
	}

}
