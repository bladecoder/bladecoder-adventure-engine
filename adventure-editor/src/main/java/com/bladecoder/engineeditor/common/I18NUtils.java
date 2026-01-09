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

import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engineeditor.common.OrderedProperties.OrderedPropertiesBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map.Entry;
import java.util.Set;

public class I18NUtils {
    private static final String SEPARATOR = "\t";
    private static final String TSV_EXT = ".tsv";
    private static final String PROPERTIES_EXT = ".properties";

    public static void exportTSV(String modelPath, String outFile, final String chapterId, String defaultLocale)
            throws IOException {
        File defaultChapter = new File(modelPath, chapterId + PROPERTIES_EXT);

        File outputFile;

        if (outFile == null)
            outputFile = new File(modelPath, chapterId + TSV_EXT);
        else
            outputFile = new File(outFile);

        // 1. Find all chapter properties
        File[] files = new File(modelPath).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File arg0, String arg1) {
                if (!arg1.endsWith(PROPERTIES_EXT) || !arg1.startsWith(chapterId + "_"))
                    return false;

                return true;
            }
        });

        OrderedProperties[] props = new OrderedProperties[files.length + 1];

        props[0] = new OrderedPropertiesBuilder().withSuppressDateInComment(true).withOrdering().build();

        props[0].load(new InputStreamReader(new FileInputStream(defaultChapter), I18N.ENCODING));

        for (int i = 1; i < props.length; i++) {
            props[i] = new OrderedPropertiesBuilder().withSuppressDateInComment(true).withOrdering().build();
            props[i].load(new InputStreamReader(new FileInputStream(files[i - 1]), I18N.ENCODING));
        }

        // WRITE THE OUTPUT FILE
        BufferedWriter writer = null;

        writer = new BufferedWriter(new FileWriter(outputFile));

        String lang = defaultLocale;

        writer.write("KEY");

        // write header
        for (int i = 0; i < props.length; i++) {
            if (i != 0)
                lang = files[i - 1].getName().substring(files[i - 1].getName().lastIndexOf('_') + 1,
                        files[i - 1].getName().lastIndexOf('.'));

            writer.write(SEPARATOR + lang);
        }

        writer.write("\n");

        Set<Entry<String, String>> keySet = props[0].entrySet();

        for (Entry<String, String> e : keySet) {
            writer.write(e.getKey());

            for (OrderedProperties p : props) {
                if (p.getProperty(e.getKey()) == null) {
                    writer.write(SEPARATOR + "**" + props[0].getProperty(e.getKey()).replace("\n", "\\n"));
                    System.out.println("KEY NOT FOUND: " + e);
                } else {
                    writer.write(SEPARATOR + p.getProperty(e.getKey()).replace("\n", "\\n"));
                }
            }

            writer.write("\n");
        }

        writer.close();
    }

    public static void importTSV(String modelPath, String tsvFile, String chapterId, String defaultLocale)
            throws IOException {
        File inputFile = new File(tsvFile);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF8"))) {
            // get header
            String line = br.readLine();

            if (line != null) {
                String[] langs = line.split(SEPARATOR);
                OrderedProperties[] props = new OrderedProperties[langs.length - 1];

                for (int i = 0; i < props.length; i++) {
                    OrderedPropertiesBuilder builder = new OrderedPropertiesBuilder();
                    builder.withSuppressDateInComment(true);
                    props[i] = builder.build();
                }

                // get keys and texts
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(SEPARATOR);

                    if (values.length != langs.length) {
                        EditorLogger.error("Incorrect line in .tsv: " + line);
                        continue;
                    }

                    String key = values[0];

                    for (int i = 0; i < props.length; i++) {
                        String value = values[i + 1];
                        if (value != null)
                            value = value.replace("\\n", "\n");

                        props[i].setProperty(key, value);
                    }
                }

                // save properties
                for (int i = 0; i < props.length; i++) {

                    String i18nFilename;

                    if (langs[i + 1].equals(defaultLocale)) {
                        i18nFilename = modelPath + "/" + chapterId + PROPERTIES_EXT;
                    } else {
                        i18nFilename = modelPath + "/" + chapterId + "_" + langs[i + 1] + PROPERTIES_EXT;
                    }

                    FileOutputStream os = new FileOutputStream(i18nFilename);
                    Writer out = new OutputStreamWriter(os, I18N.ENCODING);
                    props[i].store(out, null);
                }
            }
        }
    }

    public static void newLocale(String modelPath, final String chapterId, String defaultLocale, String newLocale)
            throws IOException {
        File defaultChapter = new File(modelPath, chapterId + PROPERTIES_EXT);
        File newChapter = new File(modelPath, chapterId + "_" + newLocale + PROPERTIES_EXT);

        OrderedProperties defaultProp = new OrderedPropertiesBuilder().withSuppressDateInComment(true).withOrdering()
                .build();
        OrderedProperties newProp = new OrderedPropertiesBuilder().withSuppressDateInComment(true).withOrdering()
                .build();

        defaultProp.load(new InputStreamReader(new FileInputStream(defaultChapter), I18N.ENCODING));

        for (Entry<String, String> e : defaultProp.entrySet()) {
            newProp.setProperty(e.getKey(), "**" + defaultProp.getProperty(e.getKey()));
        }

        // save new .properties
        FileOutputStream os = new FileOutputStream(newChapter);
        Writer out = new OutputStreamWriter(os, I18N.ENCODING);
        newProp.store(out, newChapter.getName());
    }

    public static void compare(String modelPath, final String chapterId, String defaultLocale, String destLocale)
            throws IOException {
        File defaultChapter = new File(modelPath, chapterId + PROPERTIES_EXT);
        File destChapter = new File(modelPath, chapterId + "_" + destLocale + PROPERTIES_EXT);

        OrderedProperties defaultProp = new OrderedPropertiesBuilder().withSuppressDateInComment(true).withOrdering()
                .build();
        OrderedProperties destProp = new OrderedPropertiesBuilder().withSuppressDateInComment(true).withOrdering()
                .build();

        defaultProp.load(new InputStreamReader(new FileInputStream(defaultChapter), I18N.ENCODING));
        destProp.load(new InputStreamReader(new FileInputStream(destChapter), I18N.ENCODING));

        // SEARCH FOR NOT EXISTING DEST KEYS
        for (Entry<String, String> e : defaultProp.entrySet()) {
            if (destProp.getProperty(e.getKey()) == null) {
                EditorLogger.error("Key not found in '" + destLocale + "' locale: " + e.getKey());
            }
        }

        // SEARCH FOR NOT EXISTING DEFAULT CHAPTER KEYS
        for (Entry<String, String> e : destProp.entrySet()) {
            if (defaultProp.getProperty(e.getKey()) == null) {
                EditorLogger.error("Key not found in default locale: " + e.getKey());
            }
        }
    }

    public static void sync(String modelPath, final String chapterId, String defaultLocale, String destLocale)
            throws IOException {
        File defaultChapter = new File(modelPath, chapterId + PROPERTIES_EXT);
        File destChapter = new File(modelPath, chapterId + "_" + destLocale + PROPERTIES_EXT);

        OrderedProperties defaultProp = new OrderedPropertiesBuilder().withSuppressDateInComment(true).withOrdering()
                .build();
        OrderedProperties destProp = new OrderedPropertiesBuilder().withSuppressDateInComment(true).withOrdering()
                .build();

        defaultProp.load(new InputStreamReader(new FileInputStream(defaultChapter), I18N.ENCODING));
        destProp.load(new InputStreamReader(new FileInputStream(destChapter), I18N.ENCODING));

        // SEARCH FOR NOT EXISTING DEST KEYS
        for (String key : defaultProp.stringPropertyNames()) {
            if (destProp.getProperty(key) == null) {
                System.out.println("ADDING Key not found in '" + destLocale + "' locale: " + key + "="
                        + defaultProp.getProperty(key));
                destProp.setProperty(key, "**" + defaultProp.getProperty(key));
            }
        }

        // SEARCH FOR NOT EXISTING DEFAULT CHAPTER KEYS
        for (String key : destProp.stringPropertyNames()) {
            if (defaultProp.getProperty(key) == null) {
                System.out.println("DELETE MANUALLY Key not found in default locale: " + key);
            }
        }

        // save dest .properties
        FileOutputStream os = new FileOutputStream(destChapter);
        Writer out = new OutputStreamWriter(os, I18N.ENCODING);
        destProp.store(out, destChapter.getName());
    }
}
