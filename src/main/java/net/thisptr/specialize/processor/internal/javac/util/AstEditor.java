package net.thisptr.specialize.processor.internal.javac.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

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
	private Context context;
	private TreeMaker make;
	private Names names;

	public AstEditor(final Context context, final JCCompilationUnit unit) {
		this.context = context;
		this.unit = unit;
		this.imports = parseImports(unit);
		this.make = TreeMaker.instance(context);
		this.names = Names.instance(context);
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

	public void prependWildcardImport(final Package pkg) {
		prependImport(pkg.getName() + ".*");
	}

	public void prependImport(final String pkg) {
		final LinkedList<String> hierarchy = new LinkedList<String>(Arrays.asList(pkg.split("\\.")));

		JCExpression head = make.Ident(names.fromString(hierarchy.removeFirst()));
		while (!hierarchy.isEmpty())
			head = make.Select(head, names.fromString(hierarchy.removeFirst()));

		prependImport(make.Import(head, false));
	}

	public void prependImport(final Class<?> cls) {
		prependImport(cls.getName());
	}

	public void prependImport(final JCImport specializedImp) {
		unit.defs = unit.defs.prepend(specializedImp);
		imports.addFirst(parseImport(specializedImp));
		modified = true;
	}

	public void removeWildcardImports(final Package pkg) {
		final List<JCTree> defs = new ArrayList<JCTree>();
		for (final JCTree tree : unit.defs) {
			if (tree instanceof JCImport) {
				final JCImport imp = (JCImport) tree;
				if (!imp.staticImport && (pkg.getName() + ".*").equals(imp.qualid.toString()))
					continue;
			}
			defs.add(tree);
		}
		unit.defs = Utils.toImmutableList(defs);
		modified = true;
		// TODO: also remove from imports
	}

	public void removeImports(final Class<?> clazz) {
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

	public static JCImport rename(final Context context, final JCImport src, final Name newname) {
		final JCImport copy = Utils.copyTree(context, src);
		((JCFieldAccess) copy.qualid).name = newname;
		return copy;
	}
}
