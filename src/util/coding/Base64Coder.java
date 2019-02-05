package util.coding;

public class Base64Coder implements Coder.Delegate {

	@Override
	public String getDescription() {
		return "BASE 64";
	}

	@Override
	public byte[] encode(byte[] source) {
		return java.util.Base64.getEncoder().encode(source);
	}

	@Override
	public byte[] decode(byte[] source) throws Exception {
		return java.util.Base64.getDecoder().decode(source);
	}
}
