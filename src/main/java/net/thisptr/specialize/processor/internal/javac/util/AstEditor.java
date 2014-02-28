package net.thisptr.specialize.processor.internal.javac.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCImport;

public class AstEditor {

	public static class JavaImport {
		public String importClass;
		public JCImport importStatement;

		public JavaImport(final String importClass, final JCImport importStatement) {
			this.importClass = importClass;
			this.importStatement = importStatement;
		}
	}

	private JCCompilationUnit unit;
	private LinkedList<JavaImport> imports;
	private boolean modified = false;

	public AstEditor(final JCCompilationUnit unit) {
		this.unit = unit;
		this.imports = parseImports(unit);
	}

	private static JavaImport parseImport(final JCImport imp) {
		if (imp.qualid instanceof JCFieldAccess) {
			final JCFieldAccess fieldAccess = (JCFieldAccess) imp.qualid;
			final String className = fieldAccess.name.toString();
			return new JavaImport(className, imp);
		} else {
			throw new UnhandledSyntaxException(imp.qualid);
		}
	}

	private static LinkedList<JavaImport> parseImports(final JCCompilationUnit unit) {
		final LinkedList<JavaImport> result = new LinkedList<JavaImport>();
		for (final JCTree tree : unit.defs)
			if (tree instanceof JCImport)
				result.add(parseImport((JCImport) tree));
		return result;
	}

	public List<JCImport> findImport(final String className) {
		final List<JCImport> result = new ArrayList<JCImport>();
		for (final JavaImport imp : imports)
			if (className.equals(imp.importClass))
				result.add(imp.importStatement);
		return result;
	}

	public void prependImport(final JCImport specializedImp) {
		final JavaImport jimp = parseImport(specializedImp);
		unit.defs.prepend(specializedImp);
		imports.addFirst(jimp);
		modified = true;
	}

	public void removeWildcardImport(final Package packge) {
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
		modified = true;
		// TODO: also remove from imports
	}

	public void removeImport(final Class<?> clazz) {
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
		modified = true;
		// TODO: also remove from imports
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}
}
