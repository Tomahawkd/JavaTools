package file;

import util.function.ThrowableFunction;
import util.function.ThrowableSupplier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;

public class FileHelper {

	public static String readFile(String path) throws IOException {
		var file = new File(path);
		if (!file.exists()) throw new FileNotFoundException("File Not Found.");
		if (!file.canRead()) throw new FileSystemException("File Cannot be read.");

		try (FileInputStream in = new FileInputStream(file)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	public static boolean isDirExist(String path) {
		var file = new File(path);
		return file.exists() && file.isDirectory();
	}

	public static void writeFile(String path, String data, boolean overwrite) throws IOException {
		var file = new File(path);
		if (file.exists() && overwrite)
			if (!file.delete()) throw new FileSystemException("File cannot be deleted.");

		if (!file.exists()) {
			if (!file.createNewFile()) throw new FileSystemException("File cannot be created.");
		} else throw new FileAlreadyExistsException("File already exist.");

		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(data.getBytes(StandardCharsets.UTF_8));
			out.flush();
		}
	}

	public static void createDir(String path) throws IOException {
		var file = new File(path);
		if (file.exists()) throw new FileAlreadyExistsException("Directory already exist.");
		if (!file.mkdir()) throw new FileSystemException("Directory cannot be created.");
	}

	public static boolean isFileExist(String path) {
		var file = new File(path);
		return file.exists() && file.isFile();
	}

	public static void deleteFile(String path) throws IOException {
		if (!isFileExist(path)) return;
		var file = new File(path);
		if (!file.delete()) throw new FileSystemException("File cannot be deleted.");
	}

	public static class Cache {

		public static String getContentIfValidOrDefault(String file, ThrowableSupplier<String> onInvalid)
				throws Exception {

			// this is for cache
			writeFile(file, onInvalid.get(), true);
			return getIfValidOrDefault(file, FileHelper::readFile, onInvalid);
		}

		public static String getIfValidOrDefault(
				String file, ThrowableFunction<String, String> onValid,
				ThrowableSupplier<String> onInvalid) throws Exception {

			if (FileHelper.isFileExist(file)) {
				if (!isTempFileExpired(file)) return onValid.apply(file);
				else FileHelper.deleteFile(file);
			}

			return onInvalid.get();
		}


		public static boolean isTempFileExpired(String path) {

			// file must be exist
			assert isFileExist(path);

			var file = new File(path);
			return System.currentTimeMillis() - file.lastModified() > 1000 * 60 * 60 * 24 * 7;
		}
	}
}
