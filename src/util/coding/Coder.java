package util.coding;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Coder {

	public interface Delegate {

		String getDescription();

		byte[] encode(byte[] source) throws Exception;

		byte[] decode(byte[] source) throws Exception;

	}

	private Delegate codingDelegate;

	private byte[] result;

	private Throwable thr;

	public Coder(Delegate delegate) {
		this.codingDelegate = delegate;
	}

	@Contract("_ -> this")
	public final Coder decode(byte[] source) {
		try {
			result = codingDelegate.decode(source);
		} catch (Exception e) {
			thr = e.getCause();
		}
		return this;
	}

	@Contract("_ -> this")
	public final Coder encode(byte[] source) throws Exception {
		try {
			result = codingDelegate.encode(source);
		} catch (Exception e) {
			thr = e.getCause();
		}
		return this;
	}

	@Contract(pure = true)
	public final Throwable getException() {
		return thr;
	}

	@Nullable
	@Contract(pure = true)
	public final String getResult() {
		return result == null ? null : new String(result);
	}

	@Nullable
	@Contract(pure = true)
	public final byte[] getRawResult() {
		return result;
	}

	@NotNull
	public final String getHexResult() {
		StringBuilder md5str = new StringBuilder();
		int digital;
		for (byte aByte : result) {
			digital = aByte;

			if (digital < 0) {
				digital += 256;
			}
			if (digital < 16) {
				md5str.append("0");
			}
			md5str.append(Integer.toHexString(digital));

		}
		return md5str.toString();
	}

	public final String getCodingName() {
		return codingDelegate.getDescription();
	}
}
