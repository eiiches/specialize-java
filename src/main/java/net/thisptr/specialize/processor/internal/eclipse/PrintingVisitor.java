package net.thisptr.specialize.processor.internal.eclipse;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.AssertStatement;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BreakStatement;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CharLiteral;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.JavadocArgumentExpression;
import org.eclipse.jdt.internal.compiler.ast.JavadocArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocArraySingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocImplicitTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.jdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.jdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedSuperReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.StringLiteralConcatenation;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.UnionTypeReference;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintingVisitor extends ASTVisitor {
	private static Logger log = LoggerFactory.getLogger(PrintingVisitor.class);
	
	public int indent = 0;
	
	public void print(final String str) {
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < indent; ++i)
			builder.append("|---");
		builder.append(str);
		log.info(builder.toString());
	}
	
	public void afterVisit(ASTNode node, Scope scope) {
		indent -= 1;
	}
	
	public boolean beforeVisit(ASTNode node, Scope scope) {
		indent += 1;
		if (node instanceof MethodDeclaration) {
			print(String.format("%s: %s()", node.getClass().getSimpleName(), new String(((MethodDeclaration) node).selector)));
		} else if (node instanceof TypeDeclaration) {
			print(String.format("%s: %s", node.getClass().getSimpleName(), new String(((TypeDeclaration) node).name)));
		} else {
			print(node.getClass().getSimpleName());
		}
		return true;
	}

	@Override
	public void endVisit(AllocationExpression allocationExpression,
			BlockScope scope) {
		afterVisit(allocationExpression, scope);
	}

	@Override
	public void endVisit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		afterVisit(and_and_Expression, scope);
	}

	@Override
	public void endVisit(AnnotationMethodDeclaration annotationTypeDeclaration,
			ClassScope classScope) {
		afterVisit(annotationTypeDeclaration, classScope);
	}

	@Override
	public void endVisit(Argument argument, BlockScope scope) {
		afterVisit(argument, scope);
	}

	@Override
	public void endVisit(Argument argument, ClassScope scope) {
		afterVisit(argument, scope);
	}

	@Override
	public void endVisit(ArrayAllocationExpression arrayAllocationExpression,
			BlockScope scope) {
		afterVisit(arrayAllocationExpression, scope);
	}

	@Override
	public void endVisit(ArrayInitializer arrayInitializer, BlockScope scope) {
		afterVisit(arrayInitializer, scope);
	}

	@Override
	public void endVisit(
			ArrayQualifiedTypeReference arrayQualifiedTypeReference,
			BlockScope scope) {
		afterVisit(arrayQualifiedTypeReference, scope);
	}

	@Override
	public void endVisit(
			ArrayQualifiedTypeReference arrayQualifiedTypeReference,
			ClassScope scope) {
		afterVisit(arrayQualifiedTypeReference, scope);
	}

	@Override
	public void endVisit(ArrayReference arrayReference, BlockScope scope) {
		afterVisit(arrayReference, scope);
	}

	@Override
	public void endVisit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		afterVisit(arrayTypeReference, scope);
	}

	@Override
	public void endVisit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		afterVisit(arrayTypeReference, scope);
	}

	@Override
	public void endVisit(AssertStatement assertStatement, BlockScope scope) {
		afterVisit(assertStatement, scope);
	}

	@Override
	public void endVisit(Assignment assignment, BlockScope scope) {
		afterVisit(assignment, scope);
	}

	@Override
	public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
		afterVisit(binaryExpression, scope);
	}

	@Override
	public void endVisit(Block block, BlockScope scope) {
		afterVisit(block, scope);
	}

	@Override
	public void endVisit(BreakStatement breakStatement, BlockScope scope) {
		afterVisit(breakStatement, scope);
	}

	@Override
	public void endVisit(CaseStatement caseStatement, BlockScope scope) {
		afterVisit(caseStatement, scope);
	}

	@Override
	public void endVisit(CastExpression castExpression, BlockScope scope) {
		afterVisit(castExpression, scope);
	}

	@Override
	public void endVisit(CharLiteral charLiteral, BlockScope scope) {
		afterVisit(charLiteral, scope);
	}

	@Override
	public void endVisit(ClassLiteralAccess classLiteral, BlockScope scope) {
		afterVisit(classLiteral, scope);
	}

	@Override
	public void endVisit(Clinit clinit, ClassScope scope) {
		afterVisit(clinit, scope);
	}

	@Override
	public void endVisit(CompilationUnitDeclaration compilationUnitDeclaration,
			CompilationUnitScope scope) {
		afterVisit(compilationUnitDeclaration, scope);
	}

	@Override
	public void endVisit(CompoundAssignment compoundAssignment, BlockScope scope) {
		afterVisit(compoundAssignment, scope);
	}

	@Override
	public void endVisit(ConditionalExpression conditionalExpression,
			BlockScope scope) {
		afterVisit(conditionalExpression, scope);
	}

	@Override
	public void endVisit(ConstructorDeclaration constructorDeclaration,
			ClassScope scope) {
		afterVisit(constructorDeclaration, scope);
	}

	@Override
	public void endVisit(ContinueStatement continueStatement, BlockScope scope) {
		afterVisit(continueStatement, scope);
	}

	@Override
	public void endVisit(DoStatement doStatement, BlockScope scope) {
		afterVisit(doStatement, scope);
	}

	@Override
	public void endVisit(DoubleLiteral doubleLiteral, BlockScope scope) {
		afterVisit(doubleLiteral, scope);
	}

	@Override
	public void endVisit(EmptyStatement emptyStatement, BlockScope scope) {
		afterVisit(emptyStatement, scope);
	}

	@Override
	public void endVisit(EqualExpression equalExpression, BlockScope scope) {
		afterVisit(equalExpression, scope);
	}

	@Override
	public void endVisit(ExplicitConstructorCall explicitConstructor,
			BlockScope scope) {
		afterVisit(explicitConstructor, scope);
	}

	@Override
	public void endVisit(ExtendedStringLiteral extendedStringLiteral,
			BlockScope scope) {
		afterVisit(extendedStringLiteral, scope);
	}

	@Override
	public void endVisit(FalseLiteral falseLiteral, BlockScope scope) {
		afterVisit(falseLiteral, scope);
	}

	@Override
	public void endVisit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		afterVisit(fieldDeclaration, scope);
	}

	@Override
	public void endVisit(FieldReference fieldReference, BlockScope scope) {
		afterVisit(fieldReference, scope);
	}

	@Override
	public void endVisit(FieldReference fieldReference, ClassScope scope) {
		afterVisit(fieldReference, scope);
	}

	@Override
	public void endVisit(FloatLiteral floatLiteral, BlockScope scope) {
		afterVisit(floatLiteral, scope);
	}

	@Override
	public void endVisit(ForeachStatement forStatement, BlockScope scope) {
		afterVisit(forStatement, scope);
	}

	@Override
	public void endVisit(ForStatement forStatement, BlockScope scope) {
		afterVisit(forStatement, scope);
	}

	@Override
	public void endVisit(IfStatement ifStatement, BlockScope scope) {
		afterVisit(ifStatement, scope);
	}

	@Override
	public void endVisit(ImportReference importRef, CompilationUnitScope scope) {
		afterVisit(importRef, scope);
	}

	@Override
	public void endVisit(Initializer initializer, MethodScope scope) {
		afterVisit(initializer, scope);
	}

	@Override
	public void endVisit(InstanceOfExpression instanceOfExpression,
			BlockScope scope) {
		afterVisit(instanceOfExpression, scope);
	}

	@Override
	public void endVisit(IntLiteral intLiteral, BlockScope scope) {
		afterVisit(intLiteral, scope);
	}

	@Override
	public void endVisit(Javadoc javadoc, BlockScope scope) {
		afterVisit(javadoc, scope);
	}

	@Override
	public void endVisit(Javadoc javadoc, ClassScope scope) {
		afterVisit(javadoc, scope);
	}

	@Override
	public void endVisit(JavadocAllocationExpression expression,
			BlockScope scope) {
		afterVisit(expression, scope);
	}

	@Override
	public void endVisit(JavadocAllocationExpression expression,
			ClassScope scope) {
		afterVisit(expression, scope);
	}

	@Override
	public void endVisit(JavadocArgumentExpression expression, BlockScope scope) {
		afterVisit(expression, scope);
	}

	@Override
	public void endVisit(JavadocArgumentExpression expression, ClassScope scope) {
		afterVisit(expression, scope);
	}

	@Override
	public void endVisit(JavadocArrayQualifiedTypeReference typeRef,
			BlockScope scope) {
		afterVisit(typeRef, scope);
	}

	@Override
	public void endVisit(JavadocArrayQualifiedTypeReference typeRef,
			ClassScope scope) {
		afterVisit(typeRef, scope);
	}

	@Override
	public void endVisit(JavadocArraySingleTypeReference typeRef,
			BlockScope scope) {
		afterVisit(typeRef, scope);
	}

	@Override
	public void endVisit(JavadocArraySingleTypeReference typeRef,
			ClassScope scope) {
		afterVisit(typeRef, scope);
	}

	@Override
	public void endVisit(JavadocFieldReference fieldRef, BlockScope scope) {
		afterVisit(fieldRef, scope);
	}

	@Override
	public void endVisit(JavadocFieldReference fieldRef, ClassScope scope) {
		afterVisit(fieldRef, scope);
	}

	@Override
	public void endVisit(JavadocImplicitTypeReference implicitTypeReference,
			BlockScope scope) {
		afterVisit(implicitTypeReference, scope);
	}

	@Override
	public void endVisit(JavadocImplicitTypeReference implicitTypeReference,
			ClassScope scope) {
		afterVisit(implicitTypeReference, scope);
	}

	@Override
	public void endVisit(JavadocMessageSend messageSend, BlockScope scope) {
		afterVisit(messageSend, scope);
	}

	@Override
	public void endVisit(JavadocMessageSend messageSend, ClassScope scope) {
		afterVisit(messageSend, scope);
	}

	@Override
	public void endVisit(JavadocQualifiedTypeReference typeRef, BlockScope scope) {
		afterVisit(typeRef, scope);
	}

	@Override
	public void endVisit(JavadocQualifiedTypeReference typeRef, ClassScope scope) {
		afterVisit(typeRef, scope);
	}

	@Override
	public void endVisit(JavadocReturnStatement statement, BlockScope scope) {
		afterVisit(statement, scope);
	}

	@Override
	public void endVisit(JavadocReturnStatement statement, ClassScope scope) {
		afterVisit(statement, scope);
	}

	@Override
	public void endVisit(JavadocSingleNameReference argument, BlockScope scope) {
		afterVisit(argument, scope);
	}

	@Override
	public void endVisit(JavadocSingleNameReference argument, ClassScope scope) {
		afterVisit(argument, scope);
	}

	@Override
	public void endVisit(JavadocSingleTypeReference typeRef, BlockScope scope) {
		afterVisit(typeRef, scope);
	}

	@Override
	public void endVisit(JavadocSingleTypeReference typeRef, ClassScope scope) {
		afterVisit(typeRef, scope);
	}

	@Override
	public void endVisit(LabeledStatement labeledStatement, BlockScope scope) {
		afterVisit(labeledStatement, scope);
	}

	@Override
	public void endVisit(LocalDeclaration localDeclaration, BlockScope scope) {
		afterVisit(localDeclaration, scope);
	}

	@Override
	public void endVisit(LongLiteral longLiteral, BlockScope scope) {
		afterVisit(longLiteral, scope);
	}

	@Override
	public void endVisit(MarkerAnnotation annotation, BlockScope scope) {
		afterVisit(annotation, scope);
	}

	@Override
	public void endVisit(MemberValuePair pair, BlockScope scope) {
		afterVisit(pair, scope);
	}

	@Override
	public void endVisit(MessageSend messageSend, BlockScope scope) {
		afterVisit(messageSend, scope);
	}

	@Override
	public void endVisit(MethodDeclaration methodDeclaration, ClassScope scope) {
		afterVisit(methodDeclaration, scope);
	}

	@Override
	public void endVisit(StringLiteralConcatenation literal, BlockScope scope) {
		afterVisit(literal, scope);
	}

	@Override
	public void endVisit(NormalAnnotation annotation, BlockScope scope) {
		afterVisit(annotation, scope);
	}

	@Override
	public void endVisit(NullLiteral nullLiteral, BlockScope scope) {
		afterVisit(nullLiteral, scope);
	}

	@Override
	public void endVisit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		afterVisit(or_or_Expression, scope);
	}

	@Override
	public void endVisit(
			ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
			BlockScope scope) {
		afterVisit(parameterizedQualifiedTypeReference, scope);
	}

	@Override
	public void endVisit(
			ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
			ClassScope scope) {
		afterVisit(parameterizedQualifiedTypeReference, scope);
	}

	@Override
	public void endVisit(
			ParameterizedSingleTypeReference parameterizedSingleTypeReference,
			BlockScope scope) {
		afterVisit(parameterizedSingleTypeReference, scope);
	}

	@Override
	public void endVisit(
			ParameterizedSingleTypeReference parameterizedSingleTypeReference,
			ClassScope scope) {
		afterVisit(parameterizedSingleTypeReference, scope);
	}

	@Override
	public void endVisit(PostfixExpression postfixExpression, BlockScope scope) {
		afterVisit(postfixExpression, scope);
	}

	@Override
	public void endVisit(PrefixExpression prefixExpression, BlockScope scope) {
		afterVisit(prefixExpression, scope);
	}

	@Override
	public void endVisit(
			QualifiedAllocationExpression qualifiedAllocationExpression,
			BlockScope scope) {
		afterVisit(qualifiedAllocationExpression, scope);
	}

	@Override
	public void endVisit(QualifiedNameReference qualifiedNameReference,
			BlockScope scope) {
		afterVisit(qualifiedNameReference, scope);
	}

	@Override
	public void endVisit(QualifiedNameReference qualifiedNameReference,
			ClassScope scope) {
		afterVisit(qualifiedNameReference, scope);
	}

	@Override
	public void endVisit(QualifiedSuperReference qualifiedSuperReference,
			BlockScope scope) {
		afterVisit(qualifiedSuperReference, scope);
	}

	@Override
	public void endVisit(QualifiedSuperReference qualifiedSuperReference,
			ClassScope scope) {
		afterVisit(qualifiedSuperReference, scope);
	}

	@Override
	public void endVisit(QualifiedThisReference qualifiedThisReference,
			BlockScope scope) {
		afterVisit(qualifiedThisReference, scope);
	}

	@Override
	public void endVisit(QualifiedThisReference qualifiedThisReference,
			ClassScope scope) {
		afterVisit(qualifiedThisReference, scope);
	}

	@Override
	public void endVisit(QualifiedTypeReference qualifiedTypeReference,
			BlockScope scope) {
		afterVisit(qualifiedTypeReference, scope);
	}

	@Override
	public void endVisit(QualifiedTypeReference qualifiedTypeReference,
			ClassScope scope) {
		afterVisit(qualifiedTypeReference, scope);
	}

	@Override
	public void endVisit(ReturnStatement returnStatement, BlockScope scope) {
		afterVisit(returnStatement, scope);
	}

	@Override
	public void endVisit(SingleMemberAnnotation annotation, BlockScope scope) {
		afterVisit(annotation, scope);
	}

	@Override
	public void endVisit(SingleNameReference singleNameReference,
			BlockScope scope) {
		afterVisit(singleNameReference, scope);
	}

	@Override
	public void endVisit(SingleNameReference singleNameReference,
			ClassScope scope) {
		afterVisit(singleNameReference, scope);
	}

	@Override
	public void endVisit(SingleTypeReference singleTypeReference,
			BlockScope scope) {
		afterVisit(singleTypeReference, scope);
	}

	@Override
	public void endVisit(SingleTypeReference singleTypeReference,
			ClassScope scope) {
		afterVisit(singleTypeReference, scope);
	}

	@Override
	public void endVisit(StringLiteral stringLiteral, BlockScope scope) {
		afterVisit(stringLiteral, scope);
	}

	@Override
	public void endVisit(SuperReference superReference, BlockScope scope) {
		afterVisit(superReference, scope);
	}

	@Override
	public void endVisit(SwitchStatement switchStatement, BlockScope scope) {
		afterVisit(switchStatement, scope);
	}

	@Override
	public void endVisit(SynchronizedStatement synchronizedStatement,
			BlockScope scope) {
		afterVisit(synchronizedStatement, scope);
	}

	@Override
	public void endVisit(ThisReference thisReference, BlockScope scope) {
		afterVisit(thisReference, scope);
	}

	@Override
	public void endVisit(ThisReference thisReference, ClassScope scope) {
		afterVisit(thisReference, scope);
	}

	@Override
	public void endVisit(ThrowStatement throwStatement, BlockScope scope) {
		afterVisit(throwStatement, scope);
	}

	@Override
	public void endVisit(TrueLiteral trueLiteral, BlockScope scope) {
		afterVisit(trueLiteral, scope);
	}

	@Override
	public void endVisit(TryStatement tryStatement, BlockScope scope) {
		afterVisit(tryStatement, scope);
	}

	@Override
	public void endVisit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
		afterVisit(localTypeDeclaration, scope);
	}

	@Override
	public void endVisit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
		afterVisit(memberTypeDeclaration, scope);
	}

	@Override
	public void endVisit(TypeDeclaration typeDeclaration,
			CompilationUnitScope scope) {
		afterVisit(typeDeclaration, scope);
	}

	@Override
	public void endVisit(TypeParameter typeParameter, BlockScope scope) {
		afterVisit(typeParameter, scope);
	}

	@Override
	public void endVisit(TypeParameter typeParameter, ClassScope scope) {
		afterVisit(typeParameter, scope);
	}

	@Override
	public void endVisit(UnaryExpression unaryExpression, BlockScope scope) {
		afterVisit(unaryExpression, scope);
	}

	@Override
	public void endVisit(UnionTypeReference unionTypeReference, BlockScope scope) {
		afterVisit(unionTypeReference, scope);
	}

	@Override
	public void endVisit(UnionTypeReference unionTypeReference, ClassScope scope) {
		afterVisit(unionTypeReference, scope);
	}

	@Override
	public void endVisit(WhileStatement whileStatement, BlockScope scope) {
		afterVisit(whileStatement, scope);
	}

	@Override
	public void endVisit(Wildcard wildcard, BlockScope scope) {
		afterVisit(wildcard, scope);
	}

	@Override
	public void endVisit(Wildcard wildcard, ClassScope scope) {
		afterVisit(wildcard, scope);
	}

	@Override
	public boolean visit(AllocationExpression allocationExpression,
			BlockScope scope) {
		return beforeVisit(allocationExpression, scope);
	}

	@Override
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return beforeVisit(and_and_Expression, scope);
	}

	@Override
	public boolean visit(AnnotationMethodDeclaration annotationTypeDeclaration,
			ClassScope classScope) {
		return beforeVisit(annotationTypeDeclaration, classScope);
	}

	@Override
	public boolean visit(Argument argument, BlockScope scope) {
		return beforeVisit(argument, scope);
	}

	@Override
	public boolean visit(Argument argument, ClassScope scope) {
		return beforeVisit(argument, scope);
	}

	@Override
	public boolean visit(ArrayAllocationExpression arrayAllocationExpression,
			BlockScope scope) {
		return beforeVisit(arrayAllocationExpression, scope);
	}

	@Override
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		return beforeVisit(arrayInitializer, scope);
	}

	@Override
	public boolean visit(
			ArrayQualifiedTypeReference arrayQualifiedTypeReference,
			BlockScope scope) {
		return beforeVisit(arrayQualifiedTypeReference, scope);
	}

	@Override
	public boolean visit(
			ArrayQualifiedTypeReference arrayQualifiedTypeReference,
			ClassScope scope) {
		return beforeVisit(arrayQualifiedTypeReference, scope);
	}

	@Override
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		return beforeVisit(arrayReference, scope);
	}

	@Override
	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		return beforeVisit(arrayTypeReference, scope);
	}

	@Override
	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		return beforeVisit(arrayTypeReference, scope);
	}

	@Override
	public boolean visit(AssertStatement assertStatement, BlockScope scope) {
		return beforeVisit(assertStatement, scope);
	}

	@Override
	public boolean visit(Assignment assignment, BlockScope scope) {
		return beforeVisit(assignment, scope);
	}

	@Override
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		return beforeVisit(binaryExpression, scope);
	}

	@Override
	public boolean visit(Block block, BlockScope scope) {
		return beforeVisit(block, scope);
	}

	@Override
	public boolean visit(BreakStatement breakStatement, BlockScope scope) {
		return beforeVisit(breakStatement, scope);
	}

	@Override
	public boolean visit(CaseStatement caseStatement, BlockScope scope) {
		return beforeVisit(caseStatement, scope);
	}

	@Override
	public boolean visit(CastExpression castExpression, BlockScope scope) {
		return beforeVisit(castExpression, scope);
	}

	@Override
	public boolean visit(CharLiteral charLiteral, BlockScope scope) {
		return beforeVisit(charLiteral, scope);
	}

	@Override
	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		return beforeVisit(classLiteral, scope);
	}

	@Override
	public boolean visit(Clinit clinit, ClassScope scope) {
		return beforeVisit(clinit, scope);
	}

	@Override
	public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration,
			CompilationUnitScope scope) {
		return beforeVisit(compilationUnitDeclaration, scope);
	}

	@Override
	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		return beforeVisit(compoundAssignment, scope);
	}

	@Override
	public boolean visit(ConditionalExpression conditionalExpression,
			BlockScope scope) {
		return beforeVisit(conditionalExpression, scope);
	}

	@Override
	public boolean visit(ConstructorDeclaration constructorDeclaration,
			ClassScope scope) {
		return beforeVisit(constructorDeclaration, scope);
	}

	@Override
	public boolean visit(ContinueStatement continueStatement, BlockScope scope) {
		return beforeVisit(continueStatement, scope);
	}

	@Override
	public boolean visit(DoStatement doStatement, BlockScope scope) {
		return beforeVisit(doStatement, scope);
	}

	@Override
	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		return beforeVisit(doubleLiteral, scope);
	}

	@Override
	public boolean visit(EmptyStatement emptyStatement, BlockScope scope) {
		return beforeVisit(emptyStatement, scope);
	}

	@Override
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		return beforeVisit(equalExpression, scope);
	}

	@Override
	public boolean visit(ExplicitConstructorCall explicitConstructor,
			BlockScope scope) {
		return beforeVisit(explicitConstructor, scope);
	}

	@Override
	public boolean visit(ExtendedStringLiteral extendedStringLiteral,
			BlockScope scope) {
		return beforeVisit(extendedStringLiteral, scope);
	}

	@Override
	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		return beforeVisit(falseLiteral, scope);
	}

	@Override
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		return beforeVisit(fieldDeclaration, scope);
	}

	@Override
	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		return beforeVisit(fieldReference, scope);
	}

	@Override
	public boolean visit(FieldReference fieldReference, ClassScope scope) {
		return beforeVisit(fieldReference, scope);
	}

	@Override
	public boolean visit(FloatLiteral floatLiteral, BlockScope scope) {
		return beforeVisit(floatLiteral, scope);
	}

	@Override
	public boolean visit(ForeachStatement forStatement, BlockScope scope) {
		return beforeVisit(forStatement, scope);
	}

	@Override
	public boolean visit(ForStatement forStatement, BlockScope scope) {
		return beforeVisit(forStatement, scope);
	}

	@Override
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		return beforeVisit(ifStatement, scope);
	}

	@Override
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		return beforeVisit(importRef, scope);
	}

	@Override
	public boolean visit(Initializer initializer, MethodScope scope) {
		return beforeVisit(initializer, scope);
	}

	@Override
	public boolean visit(InstanceOfExpression instanceOfExpression,
			BlockScope scope) {
		return beforeVisit(instanceOfExpression, scope);
	}

	@Override
	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		return beforeVisit(intLiteral, scope);
	}

	@Override
	public boolean visit(Javadoc javadoc, BlockScope scope) {
		return beforeVisit(javadoc, scope);
	}

	@Override
	public boolean visit(Javadoc javadoc, ClassScope scope) {
		return beforeVisit(javadoc, scope);
	}

	@Override
	public boolean visit(JavadocAllocationExpression expression,
			BlockScope scope) {
		return beforeVisit(expression, scope);
	}

	@Override
	public boolean visit(JavadocAllocationExpression expression,
			ClassScope scope) {
		return beforeVisit(expression, scope);
	}

	@Override
	public boolean visit(JavadocArgumentExpression expression, BlockScope scope) {
		return beforeVisit(expression, scope);
	}

	@Override
	public boolean visit(JavadocArgumentExpression expression, ClassScope scope) {
		return beforeVisit(expression, scope);
	}

	@Override
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef,
			BlockScope scope) {
		return beforeVisit(typeRef, scope);
	}

	@Override
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef,
			ClassScope scope) {
		return beforeVisit(typeRef, scope);
	}

	@Override
	public boolean visit(JavadocArraySingleTypeReference typeRef,
			BlockScope scope) {
		return beforeVisit(typeRef, scope);
	}

	@Override
	public boolean visit(JavadocArraySingleTypeReference typeRef,
			ClassScope scope) {
		return beforeVisit(typeRef, scope);
	}

	@Override
	public boolean visit(JavadocFieldReference fieldRef, BlockScope scope) {
		return beforeVisit(fieldRef, scope);
	}

	@Override
	public boolean visit(JavadocFieldReference fieldRef, ClassScope scope) {
		return beforeVisit(fieldRef, scope);
	}

	@Override
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference,
			BlockScope scope) {
		return beforeVisit(implicitTypeReference, scope);
	}

	@Override
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference,
			ClassScope scope) {
		return beforeVisit(implicitTypeReference, scope);
	}

	@Override
	public boolean visit(JavadocMessageSend messageSend, BlockScope scope) {
		return beforeVisit(messageSend, scope);
	}

	@Override
	public boolean visit(JavadocMessageSend messageSend, ClassScope scope) {
		return beforeVisit(messageSend, scope);
	}

	@Override
	public boolean visit(JavadocQualifiedTypeReference typeRef, BlockScope scope) {
		return beforeVisit(typeRef, scope);
	}

	@Override
	public boolean visit(JavadocQualifiedTypeReference typeRef, ClassScope scope) {
		return beforeVisit(typeRef, scope);
	}

	@Override
	public boolean visit(JavadocReturnStatement statement, BlockScope scope) {
		return beforeVisit(statement, scope);
	}

	@Override
	public boolean visit(JavadocReturnStatement statement, ClassScope scope) {
		return beforeVisit(statement, scope);
	}

	@Override
	public boolean visit(JavadocSingleNameReference argument, BlockScope scope) {
		return beforeVisit(argument, scope);
	}

	@Override
	public boolean visit(JavadocSingleNameReference argument, ClassScope scope) {
		return beforeVisit(argument, scope);
	}

	@Override
	public boolean visit(JavadocSingleTypeReference typeRef, BlockScope scope) {
		return beforeVisit(typeRef, scope);
	}

	@Override
	public boolean visit(JavadocSingleTypeReference typeRef, ClassScope scope) {
		return beforeVisit(typeRef, scope);
	}

	@Override
	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		return beforeVisit(labeledStatement, scope);
	}

	@Override
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		return beforeVisit(localDeclaration, scope);
	}

	@Override
	public boolean visit(LongLiteral longLiteral, BlockScope scope) {
		return beforeVisit(longLiteral, scope);
	}

	@Override
	public boolean visit(MarkerAnnotation annotation, BlockScope scope) {
		return beforeVisit(annotation, scope);
	}

	@Override
	public boolean visit(MemberValuePair pair, BlockScope scope) {
		return beforeVisit(pair, scope);
	}

	@Override
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		return beforeVisit(messageSend, scope);
	}

	@Override
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		return beforeVisit(methodDeclaration, scope);
	}

	@Override
	public boolean visit(StringLiteralConcatenation literal, BlockScope scope) {
		return beforeVisit(literal, scope);
	}

	@Override
	public boolean visit(NormalAnnotation annotation, BlockScope scope) {
		return beforeVisit(annotation, scope);
	}

	@Override
	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		return beforeVisit(nullLiteral, scope);
	}

	@Override
	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return beforeVisit(or_or_Expression, scope);
	}

	@Override
	public boolean visit(
			ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
			BlockScope scope) {
		return beforeVisit(parameterizedQualifiedTypeReference, scope);
	}

	@Override
	public boolean visit(
			ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
			ClassScope scope) {
		return beforeVisit(parameterizedQualifiedTypeReference, scope);
	}

	@Override
	public boolean visit(
			ParameterizedSingleTypeReference parameterizedSingleTypeReference,
			BlockScope scope) {
		return beforeVisit(parameterizedSingleTypeReference, scope);
	}

	@Override
	public boolean visit(
			ParameterizedSingleTypeReference parameterizedSingleTypeReference,
			ClassScope scope) {
		return beforeVisit(parameterizedSingleTypeReference, scope);
	}

	@Override
	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		return beforeVisit(postfixExpression, scope);
	}

	@Override
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		return beforeVisit(prefixExpression, scope);
	}

	@Override
	public boolean visit(
			QualifiedAllocationExpression qualifiedAllocationExpression,
			BlockScope scope) {
		return beforeVisit(qualifiedAllocationExpression, scope);
	}

	@Override
	public boolean visit(QualifiedNameReference qualifiedNameReference,
			BlockScope scope) {
		return beforeVisit(qualifiedNameReference, scope);
	}

	@Override
	public boolean visit(QualifiedNameReference qualifiedNameReference,
			ClassScope scope) {
		return beforeVisit(qualifiedNameReference, scope);
	}

	@Override
	public boolean visit(QualifiedSuperReference qualifiedSuperReference,
			BlockScope scope) {
		return beforeVisit(qualifiedSuperReference, scope);
	}

	@Override
	public boolean visit(QualifiedSuperReference qualifiedSuperReference,
			ClassScope scope) {
		return beforeVisit(qualifiedSuperReference, scope);
	}

	@Override
	public boolean visit(QualifiedThisReference qualifiedThisReference,
			BlockScope scope) {
		return beforeVisit(qualifiedThisReference, scope);
	}

	@Override
	public boolean visit(QualifiedThisReference qualifiedThisReference,
			ClassScope scope) {
		return beforeVisit(qualifiedThisReference, scope);
	}

	@Override
	public boolean visit(QualifiedTypeReference qualifiedTypeReference,
			BlockScope scope) {
		return beforeVisit(qualifiedTypeReference, scope);
	}

	@Override
	public boolean visit(QualifiedTypeReference qualifiedTypeReference,
			ClassScope scope) {
		return beforeVisit(qualifiedTypeReference, scope);
	}

	@Override
	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		return beforeVisit(returnStatement, scope);
	}

	@Override
	public boolean visit(SingleMemberAnnotation annotation, BlockScope scope) {
		return beforeVisit(annotation, scope);
	}

	@Override
	public boolean visit(SingleNameReference singleNameReference,
			BlockScope scope) {
		return beforeVisit(singleNameReference, scope);
	}

	@Override
	public boolean visit(SingleNameReference singleNameReference,
			ClassScope scope) {
		return beforeVisit(singleNameReference, scope);
	}

	@Override
	public boolean visit(SingleTypeReference singleTypeReference,
			BlockScope scope) {
		return beforeVisit(singleTypeReference, scope);
	}

	@Override
	public boolean visit(SingleTypeReference singleTypeReference,
			ClassScope scope) {
		return beforeVisit(singleTypeReference, scope);
	}

	@Override
	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		return beforeVisit(stringLiteral, scope);
	}

	@Override
	public boolean visit(SuperReference superReference, BlockScope scope) {
		return beforeVisit(superReference, scope);
	}

	@Override
	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		return beforeVisit(switchStatement, scope);
	}

	@Override
	public boolean visit(SynchronizedStatement synchronizedStatement,
			BlockScope scope) {
		return beforeVisit(synchronizedStatement, scope);
	}

	@Override
	public boolean visit(ThisReference thisReference, BlockScope scope) {
		return beforeVisit(thisReference, scope);
	}

	@Override
	public boolean visit(ThisReference thisReference, ClassScope scope) {
		return beforeVisit(thisReference, scope);
	}

	@Override
	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		return beforeVisit(throwStatement, scope);
	}

	@Override
	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		return beforeVisit(trueLiteral, scope);
	}

	@Override
	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		return beforeVisit(tryStatement, scope);
	}

	@Override
	public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
		return beforeVisit(localTypeDeclaration, scope);
	}

	@Override
	public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
		return beforeVisit(memberTypeDeclaration, scope);
	}

	@Override
	public boolean visit(TypeDeclaration typeDeclaration,
			CompilationUnitScope scope) {
		return beforeVisit(typeDeclaration, scope);
	}

	@Override
	public boolean visit(TypeParameter typeParameter, BlockScope scope) {
		return beforeVisit(typeParameter, scope);
	}

	@Override
	public boolean visit(TypeParameter typeParameter, ClassScope scope) {
		return beforeVisit(typeParameter, scope);
	}

	@Override
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		return beforeVisit(unaryExpression, scope);
	}

	@Override
	public boolean visit(UnionTypeReference unionTypeReference, BlockScope scope) {
		return beforeVisit(unionTypeReference, scope);
	}

	@Override
	public boolean visit(UnionTypeReference unionTypeReference, ClassScope scope) {
		return beforeVisit(unionTypeReference, scope);
	}

	@Override
	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		return beforeVisit(whileStatement, scope);
	}

	@Override
	public boolean visit(Wildcard wildcard, BlockScope scope) {
		return beforeVisit(wildcard, scope);
	}

	@Override
	public boolean visit(Wildcard wildcard, ClassScope scope) {
		return beforeVisit(wildcard, scope);
	}
}
