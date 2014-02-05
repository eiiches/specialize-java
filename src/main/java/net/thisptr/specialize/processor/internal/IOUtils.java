package net.thisptr.specialize.processor.internal;

import java.io.Closeable;
import java.io.IOException;

public class IOUtils {
	public static void closeQuietly(final Closeable closeable) {
		try {
			closeable.close();
		} catch (IOException e) {}
	}
}
