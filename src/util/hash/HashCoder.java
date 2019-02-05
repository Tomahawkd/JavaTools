package util.hash;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.coding.Coder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCoder implements Coder.Delegate {

	private MessageDigest md;

	public HashCoder setAlgorithm(String algorithm) throws NoSuchAlgorithmException {
		md = MessageDigest.getInstance(algorithm);
		return this;
	}

	@Override
	public String getDescription() {
		return md.getAlgorithm();
	}

	@Override
	public byte[] encode(byte[] source) {
		return md.digest(source);
	}

	@Override
	public byte[] decode(byte[] source) {
		throw new NotImplementedException();
	}
}