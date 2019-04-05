package util.function;

@FunctionalInterface
public interface ThrowableFunction<V, R> {
	R apply(V value) throws Exception;
}
