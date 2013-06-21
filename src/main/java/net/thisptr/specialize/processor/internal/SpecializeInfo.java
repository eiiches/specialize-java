package net.thisptr.specialize.processor.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.thisptr.specialize.Specialize;

public class SpecializeInfo {
	private Map<String, SpecializeInfo.TypeParam> typeParams = new HashMap<String, SpecializeInfo.TypeParam>();

	public static class TypeParam {
		public final String key;
		public final Set<String> types;

		public TypeParam(final String key, final String[] types) {
			this.key = key;
			this.types = new HashSet<String>(Arrays.asList(types));
			if (this.types.contains("*")) {
				this.types.remove("*");
				this.types.addAll(Arrays.asList("long", "int", "short", "byte", "char", "float", "double", "boolean"));
			}
		}

		public boolean shouldGenerateGeneric() {
			return types.contains("?");
		}
	}

	private SpecializeInfo(final List<SpecializeInfo.TypeParam> typeParams) {
		for (final SpecializeInfo.TypeParam typeParam : typeParams)
			this.typeParams.put(typeParam.key, typeParam);
	}

	public static SpecializeInfo parse(final Specialize specialize) {
		final List<SpecializeInfo.TypeParam> typeParams = new ArrayList<SpecializeInfo.TypeParam>();
		for (final String value : specialize.value()) {
			final String[] token = value.split(":");

			final String key = token[0].trim();
			final String[] types = trimTokens(token[1].split(","));

			typeParams.add(new SpecializeInfo.TypeParam(key, types));
		}
		return new SpecializeInfo(typeParams);
	}

	private static String[] trimTokens(final String[] tokens) {
		final String[] result = new String[tokens.length];
		for (int i = 0; i < tokens.length; ++i)
			result[i] = tokens[i] != null ? tokens[i].trim() : null;
			return result;
	}

	public boolean shouldGenerateGeneric() {
		for (final SpecializeInfo.TypeParam typeParam : typeParams.values()) {
			if (!typeParam.shouldGenerateGeneric())
				return false;
		}
		return true;
	}

	public List<SpecializeInfo.TypeParam> getTypeParams() {
		return new ArrayList<SpecializeInfo.TypeParam>(typeParams.values());
	}

	private static void eachSpecialization(final java.util.List<InjectInfo> results, final java.util.List<SpecializeInfo.TypeParam> specialization, final Stack<InjectInfo.TypeParam> injections) {
		if (specialization.isEmpty()) {
			if (injections.isEmpty()) // all generic version
				return;

			final java.util.List<InjectInfo.TypeParam> result = new ArrayList<InjectInfo.TypeParam>();
			for (final InjectInfo.TypeParam typeParam : injections)
				result.add(typeParam);
			results.add(new InjectInfo(result));
		} else {
			final SpecializeInfo.TypeParam head = specialization.get(0);

			for (final String type : head.types) {
				if (type.equals("?")) {
					eachSpecialization(results, specialization.subList(1, specialization.size()), injections);
				} else {
					injections.push(new InjectInfo.TypeParam(head.key, type));
					eachSpecialization(results, specialization.subList(1, specialization.size()), injections);
					injections.pop();
				}
			}
		}
	}

	public List<InjectInfo> getTypeCombinations() {
		final java.util.List<InjectInfo> results = new ArrayList<InjectInfo>();
		eachSpecialization(results, new ArrayList<SpecializeInfo.TypeParam>(this.getTypeParams()), new Stack<InjectInfo.TypeParam>());
		return results;
	}
}