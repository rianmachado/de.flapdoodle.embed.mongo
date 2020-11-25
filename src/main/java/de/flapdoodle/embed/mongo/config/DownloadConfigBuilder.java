/**
 * Copyright (C) 2011
 *   Michael Mosmann <michael@mosmann.de>
 *   Martin Jöhren <m.joehren@googlemail.com>
 *
 * with contributions from
 * 	konstantin-ba@github,Archimedes Trajano	(trajano@github)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.embed.mongo.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.Paths;
import de.flapdoodle.embed.process.config.store.IDownloadPath;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.Platform;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import de.flapdoodle.embed.process.io.directories.UserHome;
import de.flapdoodle.embed.process.io.progress.StandardConsoleProgressListener;

public class DownloadConfigBuilder extends de.flapdoodle.embed.process.config.store.DownloadConfigBuilder {

	private static Logger logger = LoggerFactory.getLogger(DownloadConfigBuilder.class);

	public DownloadConfigBuilder packageResolverForCommand(Command command) {
		packageResolver(new Paths(command));
		return this;
	}

	public DownloadConfigBuilder defaultsForCommand(Command command) {
		return defaults().packageResolverForCommand(command);
	}

	public DownloadConfigBuilder defaults() {
		BinariResource.copyMongoFromResource();
		fileNaming().setDefault(new UUIDTempNaming());
		downloadPath().setDefault(new PlattformDependendDownloadPath());
		progressListener().setDefault(new StandardConsoleProgressListener());
		artifactStorePath().setDefault(new UserHome(".embedmongo"));
		downloadPrefix().setDefault(new DownloadPrefix("embedmongo-download"));
		userAgent().setDefault(new UserAgent(
				"Mozilla/5.0 (compatible; Embedded MongoDB; +https://github.com/flapdoodle-oss/embedmongo.flapdoodle.de)"));
		return this;
	}

	public static class BinariResource {

		public static final String MONGO_BINARI_WINDOWS = "/win32/mongodb-win32-x86_64-3.5.5.zip";
		public static final String MONGO_BINARI_MACOS = "/osx/mongodb-win32-x86_64-3.5.5.zip";
		public static final String MONGO_BINARI_LINUX = "/linux/mongodb-win32-x86_64-3.5.5.zip";

		public static void copyMongoFromResource() {
			Platform platform = Platform.detect();
			StringBuilder osDir = new StringBuilder();
			String imputDir = "";
			String outPutDir = "";
			if (Platform.Windows == platform) {
				File file = new File(java.nio.file.Paths.get(System.getProperty("user.home"))
						.resolve(".embedmongo/win32").toString());
				if (file.exists() && file.isDirectory()) {
					return;
				}
				file.mkdir();
				imputDir = "mongo-repo" + MONGO_BINARI_WINDOWS;
				outPutDir = java.nio.file.Paths.get(System.getProperty("user.home"))
						.resolve(".embedmongo" + MONGO_BINARI_WINDOWS).toString();
				osDir.append(outPutDir);
				osDir.append(outPutDir);
				osDir.append(java.io.File.separator);
				osDir.append("win32");
				osDir.append(java.io.File.separator);
				
			} else if (Platform.OS_X == platform) {
				File file = new File(
						java.nio.file.Paths.get(System.getProperty("user.home")).resolve(".embedmongo/osx").toString());
				if (file.exists() && file.isDirectory()) {
					return;
				}
				file.mkdir();
				imputDir = "mongo-repo" + MONGO_BINARI_MACOS;
				outPutDir = java.nio.file.Paths.get(System.getProperty("user.home"))
						.resolve(".embedmongo" + MONGO_BINARI_MACOS).toString();
				osDir.append(outPutDir);
				osDir.append(outPutDir);
				osDir.append(java.io.File.separator);
				osDir.append("osx");
				osDir.append(java.io.File.separator);
				
			} else if (Platform.Linux == platform) {
				File file = new File(java.nio.file.Paths.get(System.getProperty("user.home"))
						.resolve(".embedmongo/linux").toString());
				if (file.exists() && file.isDirectory()) {
					return;
				}
				file.mkdir();
				imputDir = "mongo-repo" + MONGO_BINARI_LINUX;
				outPutDir = java.nio.file.Paths.get(System.getProperty("user.home"))
						.resolve(".embedmongo" + MONGO_BINARI_LINUX).toString();
				osDir.append(outPutDir);
				osDir.append(outPutDir);
				osDir.append(java.io.File.separator);
				osDir.append("linux");
				osDir.append(java.io.File.separator);
			}

			try {
				FileCopy.copyFilePlainJava(imputDir, outPutDir);
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage(), e);
			}

		}

	}

	private static class PlattformDependendDownloadPath implements IDownloadPath {

		@Override
		public String getPath(Distribution distribution) {
			if (distribution.getPlatform() == Platform.Windows) {
				return "http://downloads.mongodb.org/";
			}
			return "http://fastdl.mongodb.org/";
		}

	}

	public static class FileCopy {

		public static void copyFileCommonIO(String from, String to) throws IOException {
			File fromFile = new File(from);
			File toFile = new File(to);
			FileUtils.copyFile(fromFile, toFile);
		}

		public static void copyFilePlainJava(String from, String to) throws IOException {
			InputStream inStream = null;
			OutputStream outStream = null;
			try {
				File toFile = new File(to);
				ClassLoader classLoader = DownloadConfigBuilder.class.getClassLoader();
				inStream = classLoader.getResourceAsStream(from);
				outStream = new FileOutputStream(toFile);
				byte[] buffer = new byte[1024];
				int length;
				while ((length = inStream.read(buffer)) > 0) {
					outStream.write(buffer, 0, length);
					outStream.flush();
				}
			} finally {
				if (inStream != null)
					inStream.close();

				if (outStream != null)
					outStream.close();
			}
		}

		public static void copyFileNIO(String from, String to) throws IOException {
			Path fromFile = java.nio.file.Paths.get(from);
			Path toFile = java.nio.file.Paths.get(to);
			if (Files.notExists(fromFile)) {
				logger.warn("File doesn't exist? " + fromFile);
				return;
			}
			Path parent = toFile.getParent();
			if (parent != null) {
				if (Files.notExists(parent)) {
					Files.createDirectories(parent);
				}
			}
		}

	}

}
