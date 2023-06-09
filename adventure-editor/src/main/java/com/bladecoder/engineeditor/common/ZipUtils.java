/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    // 4MB buffer
    private static final byte[] BUFFER = new byte[4096 * 1024];

    // copy input to output stream
    private static void copy(InputStream input, OutputStream output) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(BUFFER)) != -1) {
            output.write(BUFFER, 0, bytesRead);
        }
    }

    public static void mergeZIPs(String f[], String dest) throws Exception {
        // Needed to avoid appending repeated entries.
        HashMap<String, String> destEntries = new HashMap<String, String>();

        // read the org zips
        ZipFile fZip[] = new ZipFile[f.length];

        for (int i = 0; i < fZip.length; i++)
            fZip[i] = new ZipFile(f[i]);

        // write the dest zip
        ZipOutputStream destZip = new ZipOutputStream(new FileOutputStream(dest));

        // copy contents from f zip to the dest zip

        for (ZipFile z : fZip) {
            Enumeration<? extends ZipEntry> entries = z.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();

                String name = e.getName();
                if (destEntries.get(name) == null) {
                    destEntries.put(name, name);
                    EditorLogger.debug("copy: " + e.getName());
                    destZip.putNextEntry(e);
                    EditorLogger.debug("putnextEntry done");
                    if (!e.isDirectory()) {
                        copy(z.getInputStream(e), destZip);
                    }
                    destZip.closeEntry();
                }
            }
        }

        EditorLogger.debug("appending done ");

        // close
        for (ZipFile z : fZip) {
            z.close();
        }

        destZip.close();
    }

    public static void packZip(List<File> sources, File output) throws IOException {
        EditorLogger.debug("Packaging to " + output.getName());
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(output));
        zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);

        for (File source : sources) {
            if (source.isDirectory()) {
                zipDir(zipOut, "", source);
            } else {
                zipFile(zipOut, "", source);
            }
        }
        zipOut.flush();
        zipOut.close();
        EditorLogger.debug("Done");
    }

    private static String buildPath(String path, String file) {
        if (path == null || path.isEmpty()) {
            return file;
        } else {
            return path + "/" + file;
        }
    }

    private static void zipDir(ZipOutputStream zos, String path, File dir) throws IOException {
        if (!dir.canRead()) {
            EditorLogger.error("Cannot read " + dir.getCanonicalPath()
                    + " (maybe because of permissions)");
            return;
        }

        File[] files = dir.listFiles();
        path = buildPath(path, dir.getName());
        EditorLogger.debug("Adding Directory " + path);

        for (File source : files) {
            if (source.isDirectory()) {
                zipDir(zos, path, source);
            } else {
                zipFile(zos, path, source);
            }
        }

        EditorLogger.debug("Leaving Directory " + path);
    }

    private static void zipFile(ZipOutputStream zos, String path, File file) throws IOException {
        if (!file.canRead()) {
            EditorLogger.error("Cannot read " + file.getCanonicalPath()
                    + " (maybe because of permissions)");
            return;
        }

        EditorLogger.debug("Compressing " + file.getName());
        zos.putNextEntry(new ZipEntry(buildPath(path, file.getName())));

        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[4092];
        int byteCount = 0;
        while ((byteCount = fis.read(buffer)) != -1) {
            zos.write(buffer, 0, byteCount);
        }

        fis.close();
        zos.closeEntry();
    }

    public static void unzip(File zipFile, Path targetDir) throws IOException {
        InputStream is = Files.newInputStream(zipFile.toPath());

        targetDir = targetDir.toAbsolutePath();

        try (ZipInputStream zipIn = new ZipInputStream(is)) {
            for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
                Path resolvedPath = targetDir.resolve(ze.getName()).normalize();
                if (!resolvedPath.startsWith(targetDir)) {
                    // see: https://snyk.io/research/zip-slip-vulnerability
                    throw new RuntimeException("Entry with an illegal path: "
                            + ze.getName());
                }
                if (ze.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zipIn, resolvedPath);
                }
            }
        }
    }
}
