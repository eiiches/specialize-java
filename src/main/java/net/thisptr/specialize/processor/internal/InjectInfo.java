package net.thisptr.specialize.processor.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public TypeParam get(final String key) {
		return typeParams.get(key);
	}

	public boolean contains(final String key) {
		return typeParams.containsKey(key);
	}
}