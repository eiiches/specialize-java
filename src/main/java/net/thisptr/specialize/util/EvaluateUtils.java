package net.thisptr.specialize.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

public class EvaluateUtils {
	private static Logger log = LoggerFactory.getLogger(EvaluateUtils.class);

	public static Object evaluateFieldAccess(final JCFieldAccess access) {
		if (access.name.toString().equals("class")) {
			if (access.selected instanceof JCPrimitiveTypeTree)
				return evaluatePrimitiveTypeTree((JCPrimitiveTypeTree) access.selected);
		}

		log.warn("Unhandled FieldAccess: {}[{}]", access, access.getClass());
		return null;
	}

	public static Class<?> evaluatePrimitiveTypeTree(final JCPrimitiveTypeTree tree) {
		if ((tree.toString()).equals("int")) return int.class;
		if ((tree.toString()).equals("double")) return double.class;
		if ((tree.toString()).equals("float")) return float.class;
		if ((tree.toString()).equals("char")) return char.class;
		if ((tree.toString()).equals("short")) return short.class;
		if ((tree.toString()).equals("byte")) return byte.class;
		if ((tree.toString()).equals("boolean")) return boolean.class;
		if ((tree.toString()).equals("long")) return long.class;

		log.warn("Unhandled PrimitiveTypeTree: {}[{}]", tree, tree.getClass());
		return null;
	}

	public static AnnotationInfo evaluateAnnotation(final JCAnnotation annotation) {
		final Map<String, AnnotationInfo.Value> values = new HashMap<String, AnnotationInfo.Value>();

		for (final JCExpression annotationArgExpr : annotation.getArguments()) {
			String key;
			JCExpression valueExpr;

			if (annotationArgExpr instanceof JCAssign) {
				final JCAssign assign = (JCAssign) annotationArgExpr;
				key = assign.lhs.toString();
				valueExpr = assign.rhs;
			} else {
				key = "value";
				valueExpr = annotationArgExpr;
			}

			final DiagnosticPosition position = valueExpr.pos();
			final Object value = evaluateExpression(valueExpr);
			values.put(key, new AnnotationInfo.Value(value, position));
		}

		return new AnnotationInfo(annotation.annotationType.toString(), values);
	}

	public static Object evaluateLiteral(final JCLiteral literal) {
		final Kind kind = literal.getKind();
		switch (kind) {
		case BOOLEAN_LITERAL:
		case CHAR_LITERAL:
		case DOUBLE_LITERAL:
		case FLOAT_LITERAL:
		case INT_LITERAL:
		case LONG_LITERAL:
		case STRING_LITERAL:
			return literal.value;
		case NULL_LITERAL:
			return null;
		default:
			break;
		}

		log.warn("Unhaneld Literal: {}[{}]", literal, literal.getClass());
		return null;
	}

	public static ArrayList<Object> evaluateNewArray(final JCNewArray array) {
		final ArrayList<Object> result = new ArrayList<Object>();
		for (final JCExpression elementExpr : array.elems)
			result.add(evaluateExpression(elementExpr));
		return result;
	}

	public static Object evaluateExpression(final JCExpression expr) {
		if (expr instanceof JCNewArray) {
			return evaluateNewArray((JCNewArray) expr);
		} else if (expr instanceof JCLiteral) {
			return evaluateLiteral((JCLiteral) expr);
		} else if (expr instanceof JCFieldAccess) {
			return evaluateFieldAccess((JCFieldAccess) expr);
		} else if (expr instanceof JCAnnotation) {
			return evaluateAnnotation((JCAnnotation) expr);
		}

		log.warn("Unhandled Expression: {}[{}]", expr, expr.getClass());
		return null;
	}

	public static class AnnotationInfo {
		public final String annotationClassName;

		public static class Value {
			public final DiagnosticPosition position;
			public final Object value;

			public Value(final Object value, final DiagnosticPosition position) {
				this.value = value;
				this.position = position;
			}

			@Override
			public String toString() {
				return "Value [position=" + position + ", value=" + value + "]";
			}
		}

		public final Map<String, Value> values;

		public AnnotationInfo(final String annotationClassName, final Map<String, Value> values) {
			this.annotationClassName = annotationClassName;
			this.values = new HashMap<String, Value>(values);
		}

		@Override
		public String toString() {
			return "AnnotationInfo [annotationClassName=" + annotationClassName
					+ ", values=" + values + "]";
		}
	}
}
