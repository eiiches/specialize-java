package net.thisptr.specialize.processor.internal.eclipse;

import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;

public class PrimitiveAsIdentifierScanner extends Scanner {
	public PrimitiveAsIdentifierScanner() {
		super();
	}

	public PrimitiveAsIdentifierScanner(boolean tokenizeComments,
			boolean tokenizeWhiteSpace,
			boolean checkNonExternalizedStringLiterals, long sourceLevel,
			char[][] taskTags, char[][] taskPriorities,
			boolean isTaskCaseSensitive) {
		super(tokenizeComments, tokenizeWhiteSpace, checkNonExternalizedStringLiterals,
				sourceLevel, taskTags, taskPriorities, isTaskCaseSensitive);
	}

	public PrimitiveAsIdentifierScanner(boolean tokenizeComments,
			boolean tokenizeWhiteSpace,
			boolean checkNonExternalizedStringLiterals, long sourceLevel,
			long complianceLevel, char[][] taskTags,
			char[][] taskPriorities, boolean isTaskCaseSensitive) {
		super(tokenizeComments, tokenizeWhiteSpace, checkNonExternalizedStringLiterals,
				sourceLevel, complianceLevel, taskTags, taskPriorities,
				isTaskCaseSensitive);
	}

	@Override
	public int scanIdentifierOrKeyword() {
		return primitiveAsIdentifier(super.scanIdentifierOrKeyword());
	}

	@Override
	public int scanIdentifierOrKeywordWithBoundCheck() {
		return primitiveAsIdentifier(super.scanIdentifierOrKeywordWithBoundCheck());
	}

	private int primitiveAsIdentifier(final int tokenType) {
		switch (tokenType) {
		case TerminalTokens.TokenNameint:
		case TerminalTokens.TokenNameshort:
		case TerminalTokens.TokenNamelong:
		case TerminalTokens.TokenNamechar:
		case TerminalTokens.TokenNamebyte:
		case TerminalTokens.TokenNamefloat:
		case TerminalTokens.TokenNamedouble:
		case TerminalTokens.TokenNameboolean:
			return TerminalTokens.TokenNameIdentifier;
		}
		return tokenType;
	}
}