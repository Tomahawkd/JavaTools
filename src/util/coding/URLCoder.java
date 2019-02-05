package util.coding;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class URLCoder implements Coder.Delegate {

	private String encoding;

	public URLCoder setEncoding(String encoding) {
		this.encoding = encoding;
		return this;
	}

	@Override
	public String getDescription() {
		return "URL Coding: " + encoding;
	}

	@Override
	public byte[] encode(byte[] source) throws UnsupportedEncodingException {
		return URLEncoder.encode(new String(source), encoding).getBytes();
	}

	@Override
	public byte[] decode(byte[] source) throws UnsupportedEncodingException {
		return URLDecoder.decode(new String(source), encoding).getBytes();
	}
}
