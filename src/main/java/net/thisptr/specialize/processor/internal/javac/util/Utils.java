package net.thisptr.specialize.processor.internal.javac.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.thisptr.specialize.processor.internal.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;

public class Utils {
	private static Logger log = LoggerFactory.getLogger(Utils.class);

	public static <T extends JCTree> T copyTree(final Context context, final T tree) {
		return (T) new TreeCopier<Void>(TreeMaker.instance(context)).copy(tree);
	}

	public static void dumpSource(final File file, final JCTree tree) throws IOException {
		final FileWriter writer = new FileWriter(file);
		try {
			tree.accept(new Pretty(writer, true));
		} finally {
			IOUtils.closeQuietly(writer);
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

	public static <T> com.sun.tools.javac.util.List<T> toImmutableList(final Collection<T> mutableList) {
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
		return com.sun.tools.javac.util.List.<T>nil();
	}
}
