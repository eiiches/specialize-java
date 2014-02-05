package net.thisptr.specialize.processor.internal.eclipse.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;

import net.thisptr.specialize.processor.internal.IOUtils;

import org.eclipse.jdt.internal.compiler.apt.dispatch.RoundEnvImpl;
import org.eclipse.jdt.internal.compiler.apt.model.ExecutableElementImpl;
import org.eclipse.jdt.internal.compiler.apt.model.TypeElementImpl;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MemberTypeBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static Logger log = LoggerFactory.getLogger(Utils.class);

	public static CompilationUnitDeclaration[] getCompilationUnitDeclarations(final RoundEnvImpl roundEnv) {
		try {
			final Field _units = RoundEnvImpl.class.getDeclaredField("_units");
			_units.setAccessible(true);
			return (CompilationUnitDeclaration[]) _units.get(roundEnv);
		} catch (IllegalAccessException e) {
			log.error("Could not obtain compilationUnits.");
			return null;
		} catch (IllegalArgumentException e) {
			log.error("Could not obtain compilationUnits.");
			return null;
		} catch (SecurityException e) {
			log.error("Could not obtain compilationUnits.");
			return null;
		} catch (NoSuchFieldException e) {
			log.error("Could not obtain compilationUnits.");
			return null;
		}
	}
	
	public static long makePos(final int sourceStart, final int sourceEnd) {
		return (long)sourceEnd | (((long) sourceStart)<<32);
	}
	
	public static int getSourceStart(final long pos) {
		return (int) (pos>>>32)  ;
	}
	
	public static int getSourceEnd(final long pos) {
		return (int) (pos & 0x00000000FFFFFFFFL) ;
	}

	public static void dumpSource(final File out, final CompilationUnitDeclaration unit) throws IOException {
		final StringBuffer buffer = new StringBuffer();
		unit.print(0, buffer);
		
		final BufferedWriter writer = new BufferedWriter(new FileWriter(out));
		try {
			writer.write(buffer.toString());
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
	
	public static String[] primitiveTypes = new String[] {
		"int", "short", "char", "byte", "long", "float", "double", "boolean"
	};

	public static Map<String, String> underscoredTypes = new HashMap<String, String>();
	static {
		for (final String primitiveType : primitiveTypes)
			underscoredTypes.put("_" + primitiveType, primitiveType);
	}

	public static Map<String, String> dollaredTypes = new HashMap<String, String>();
	static {
		for (final String primitiveType : primitiveTypes)
			dollaredTypes.put("$" + primitiveType, primitiveType);
	}
	
	public static ASTNode getASTNote(Element element) {
		if (element instanceof TypeElementImpl) {
			return (((MemberTypeBinding)((TypeElementImpl) element)._binding).scope).referenceContext;
		} else if (element instanceof ExecutableElementImpl) {
			// FIXME: return ((MemberTypeBinding)((ExecutableElementImpl) element)._binding);
			return null;
		} else {
			log.error("Unhandled type: {}[{}]", element, element.getClass());
			return null;
		}
	}
}
