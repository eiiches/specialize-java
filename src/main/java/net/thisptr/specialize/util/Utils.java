package net.thisptr.specialize.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

public class Utils {

	
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
		List<JCAnnotation> annotations = List.nil();
		for (JCAnnotation annotation : decl.mods.annotations) {
			if (annotation.getAnnotationType().toString().equals(annotationClass.getSimpleName()))
				continue;
				
			annotations = annotations.append(annotation);
		}
		decl.mods.annotations = annotations;
		return decl;
	}
	
	public static JCMethodDecl removeAnnotation(final JCMethodDecl decl, final Class<?> annotationClass) {
		List<JCAnnotation> annotations = List.nil();
		for (JCAnnotation annotation : decl.mods.annotations) {
			if (annotation.getAnnotationType().toString().equals(annotationClass.getSimpleName()))
				continue;
				
			annotations = annotations.append(annotation);
		}
		decl.mods.annotations = annotations;
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
}
