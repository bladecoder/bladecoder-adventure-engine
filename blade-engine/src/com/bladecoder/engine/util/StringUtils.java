package com.bladecoder.engine.util;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;

public final class StringUtils {
	@Contract("null -> true")
	public static boolean isEmpty(@Nullable String string) {
		return string == null || string.trim().isEmpty();
	}
}
