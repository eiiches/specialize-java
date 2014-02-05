package net.thisptr.specialize.processor.internal.eclipse;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.specialize.processor.internal.eclipse.util.Utils;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SpecializeTypeReferenceVisitor extends ASTVisitor {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(SpecializeTypeReferenceVisitor.class);

	public static TypeReference specializeTypeReference(final ParameterizedQualifiedTypeReference typeReference) {
		final StringBuilder specializedName = new StringBuilder();
		final char[][] tokens = ((ParameterizedQualifiedTypeReference) typeReference).tokens;
		specializedName.append(tokens[tokens.length - 1]);
		specializedName.append("$specialized");

		//		final ParameterizedSingleTypeReference typeApply = (ParameterizedSingleTypeReference) typeReference;

		final List<TypeReference> typeArguments = new ArrayList<TypeReference>();
		// arguments = ((ParameterizedQualifiedTypeReference) typeReference).typeArguments;
		for (final TypeReference[] trs : ((ParameterizedQualifiedTypeReference) typeReference).typeArguments) {
			if (trs == null)
				continue;
			for (final TypeReference tr : trs)
				typeArguments.add(tr);
		}

		final List<TypeReference> genericArguments = new ArrayList<TypeReference>();
		final List<TypeReference> primitiveArguments = new ArrayList<TypeReference>();

		for (final TypeReference argument : typeArguments) {
			final TypeReference specializedArgument = specializeTypeReference(argument);

			if (Utils.dollaredTypes.containsKey(specializedArgument.toString())) {
				primitiveArguments.add(argument);
				specializedName.append("$" + specializedArgument.toString());
			} else {
				genericArguments.add(argument);
				specializedName.append("$_");
			}
		}

		//		if (primitiveArguments.isEmpty())
		//			return typeApply;
		//
		//		if (genericArguments.isEmpty()) { // fully specialized
		//
		//		} else { // apply remaining type argument
		//
		//		}

		//		final JCExpression specializedClass = treeMaker.Select(typeApply.clazz, names.fromString(specializedName.toString()));
		//
		//		if (genericArguments.isEmpty()) {
		//			// fully specialized
		//			return specializedClass;
		//		} else {
		//			// apply remaining type argument
		//			final JCTypeApply result = treeMaker.TypeApply(specializedClass, genericArguments);
		//			return result;
		//		}

		return typeReference;
	}

	public static TypeReference specializeTypeReference(final ParameterizedSingleTypeReference typeReference) {
		final StringBuilder specializedName = new StringBuilder(new String(typeReference.token) + "$specialized");

		final List<TypeReference> typeArguments = new ArrayList<TypeReference>();
		for (final TypeReference tr : typeReference.typeArguments)
			typeArguments.add(tr);

		final List<TypeReference> genericArguments = new ArrayList<TypeReference>();
		final List<TypeReference> primitiveArguments = new ArrayList<TypeReference>();

		for (final TypeReference argument : typeArguments) {
			final TypeReference specializedArgument = specializeTypeReference(argument);

			if (Utils.dollaredTypes.containsKey(specializedArgument.toString())) {
				primitiveArguments.add(specializedArgument);
				specializedName.append("$" + Utils.dollaredTypes.get(specializedArgument.toString()));
			} else {
				genericArguments.add(specializedArgument);
				specializedName.append("$_");
			}
		}

		if (primitiveArguments.isEmpty())
			return typeReference;

		if (genericArguments.isEmpty()) { // fully specialized
			return new SingleTypeReference(specializedName.toString().toCharArray(),
					Utils.makePos(typeReference.sourceStart, typeReference.sourceEnd));
		} else { // apply remaining type arguments
			return new ParameterizedSingleTypeReference(specializedName.toString().toCharArray(),
					genericArguments.toArray(new TypeReference[genericArguments.size()]),
					typeReference.dimensions,
					Utils.makePos(typeReference.sourceStart, typeReference.sourceEnd));
		}
	}

	public static TypeReference specializeTypeReference(final TypeReference typeReference) {
		if (typeReference == null)
			return null;

		if (typeReference instanceof ParameterizedSingleTypeReference)
			return specializeTypeReference((ParameterizedSingleTypeReference) typeReference);

		if (typeReference instanceof ParameterizedQualifiedTypeReference)
			return specializeTypeReference((ParameterizedQualifiedTypeReference) typeReference);

		return typeReference;
	}

	public static TypeReference[] specializeTypeReference(final TypeReference[] typeReferences) {
		if (typeReferences == null)
			return null;

		final TypeReference[] result = new TypeReference[typeReferences.length];

		boolean modified = false;
		for (int i = 0; i < typeReferences.length; ++i) {
			result[i] = specializeTypeReference(typeReferences[i]);
			if (result[i] != typeReferences[i])
				modified = true;
		}

		if (!modified)
			return typeReferences; /* return the original */

		return result;
	}

	@Override
	public boolean visit(final LocalDeclaration localDeclaration, final BlockScope scope) {
		localDeclaration.type = specializeTypeReference(localDeclaration.type);
		return true;
	}

	@Override
	public boolean visit(final FieldDeclaration fieldDeclaration, final MethodScope scope) {
		fieldDeclaration.type = specializeTypeReference(fieldDeclaration.type);
		return true;
	}

	@Override
	public boolean visit(final Argument argument, final BlockScope scope) {
		argument.type = specializeTypeReference(argument.type);
		return true;
	}

	@Override
	public boolean visit(final Argument argument, final ClassScope scope) {
		argument.type = specializeTypeReference(argument.type);
		return true;
	}

	@Override
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		allocationExpression.type = specializeTypeReference(allocationExpression.type);
		return true;
	}

	@Override
	public boolean visit(final MethodDeclaration methodDeclaration, final ClassScope scope) {
		methodDeclaration.returnType = specializeTypeReference(methodDeclaration.returnType);
		return true;
	}

	@Override
	public boolean visit(final TypeDeclaration typeDeclaration, final CompilationUnitScope scope) {
		typeDeclaration.superclass = specializeTypeReference(typeDeclaration.superclass);
		typeDeclaration.superInterfaces = specializeTypeReference(typeDeclaration.superInterfaces);
		return true;
	}

	@Override
	public boolean visit(final TypeDeclaration memberTypeDeclaration, final ClassScope scope) {
		memberTypeDeclaration.superclass = specializeTypeReference(memberTypeDeclaration.superclass);
		memberTypeDeclaration.superInterfaces = specializeTypeReference(memberTypeDeclaration.superInterfaces);
		return true;
	}

	@Override
	public boolean visit(final TypeDeclaration localTypeDeclaration, final BlockScope scope) {
		localTypeDeclaration.superclass = specializeTypeReference(localTypeDeclaration.superclass);
		localTypeDeclaration.superInterfaces = specializeTypeReference(localTypeDeclaration.superInterfaces);
		return true;
	}

	@Override
	public boolean visit(final ExplicitConstructorCall explicitConstructor, final BlockScope scope) {
		explicitConstructor.typeArguments = specializeTypeReference(explicitConstructor.typeArguments);
		return true;
	}

	@Override
	public boolean visit(final CastExpression castExpression, final BlockScope scope) {
		castExpression.type = specializeTypeReference(castExpression.type);
		return true;
	}

	@Override
	public boolean visit(final QualifiedAllocationExpression qualifiedAllocationExpression, final BlockScope scope) {
		qualifiedAllocationExpression.type = specializeTypeReference(qualifiedAllocationExpression.type);
		qualifiedAllocationExpression.typeArguments = specializeTypeReference(qualifiedAllocationExpression.typeArguments);
		return true;
	}
}