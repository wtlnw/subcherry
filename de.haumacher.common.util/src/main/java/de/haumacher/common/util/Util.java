package de.haumacher.common.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Util {

	public static boolean equals(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		} else {
			return o1.equals(o2);
		}
	}

	public static String nonNull(String s) {
		if (s == null) {
			return "";
		} else {
			return s;
		}
	}

	public static boolean isEmpty(String s) {
		if (s == null) {
			return true;
		} else {
			return s.length() == 0;
		}
	}
	
	public static boolean notEmpty(String s) {
		if (s == null) {
			return false;
		} else {
			return s.length() > 0;
		}
	}

	public static <T> Collection<T> nonEmpty(Collection<T> collection) {
		if (collection.isEmpty()) {
			return null;
		}
		return collection;
	}
	
	public static <T> Collection<T> nonNull(Collection<T> collection) {
		if (collection == null) {
			return Collections.emptyList();
		}
		return collection;
	}

	public static <T> List<T> nonEmpty(List<T> collection) {
		if (collection.isEmpty()) {
			return null;
		}
		return collection;
	}
	
	public static <T> List<T> nonNull(List<T> collection) {
		if (collection == null) {
			return Collections.emptyList();
		}
		return collection;
	}
	
	public static <T> Set<T> nonEmpty(Set<T> collection) {
		if (collection.isEmpty()) {
			return null;
		}
		return collection;
	}

	public static <T> Set<T> nonNull(Set<T> collection) {
		if (collection == null) {
			return Collections.emptySet();
		}
		return collection;
	}

	public static String[] toStringArray(Collection<String> list) {
		return list.toArray(new String[list.size()]);
	}
	
}
