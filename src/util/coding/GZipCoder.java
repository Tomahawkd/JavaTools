package util.coding;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZipCoder implements Coder.Delegate {

	@Override
	public String getDescription() {
		return "GZip";
	}

	public byte[] decode(byte[] source) throws IOException {
		GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(source));
		BufferedInputStream is = new BufferedInputStream(in);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int count;
		while ((count = is.read(buf)) != -1) {
			os.write(buf, 0, count);
		}
		return os.toByteArray();
	}

	public byte[] encode(byte[] source) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(source);
		gzip.close();
		return out.toByteArray();
	}
}
