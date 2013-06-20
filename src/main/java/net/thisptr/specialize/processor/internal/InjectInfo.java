package net.thisptr.specialize.processor.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.thisptr.specialize.InjectPrimitive;

public class InjectInfo {
	private final Map<String, TypeParam> typeParams = new HashMap<String, TypeParam>();

	public static class TypeParam {
		public final String key;
		public final String type;

		public TypeParam(final String key, final String type) {
			this.key = key;
			this.type = type;
		}
	}

	public InjectInfo(final List<TypeParam> typeParams) {
		for (final TypeParam typeParam : typeParams)
			this.typeParams.put(typeParam.key, typeParam);
	}

	public static InjectInfo parse(final InjectPrimitive injectPrimitive) {
		final List<TypeParam> typeParams = new ArrayList<TypeParam>();
		for (final String value : injectPrimitive.value()) {
			final String[] token = value.split(":");

			final String key = token[0].trim();
			final String type = token[1].trim();

			typeParams.add(new TypeParam(key, type));
		}
		return new InjectInfo(typeParams);
	}

	public TypeParam get(final String key) {
		return typeParams.get(key);
	}

	public boolean contains(final String key) {
		return typeParams.containsKey(key);
	}
}