package net.thisptr.specialize.processor.internal.javac.util;

import com.sun.tools.javac.tree.JCTree;

public class UnhandledSyntaxException extends RuntimeException {
	private static final long serialVersionUID = -6262140313291940410L;

	private JCTree tree;

	public UnhandledSyntaxException(final JCTree tree) {
		this.tree = tree;
	}

	@Override
	public String getMessage() {
		return String.format("Unhandled Syntax of type %s: %s", tree.getClass().getSimpleName(), tree.toString());
	}
}