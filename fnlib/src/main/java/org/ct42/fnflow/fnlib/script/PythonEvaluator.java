/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ct42.fnflow.fnlib.script;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Sajjad Safaeian
 */
@Slf4j
@Component
public class PythonEvaluator extends AbstractScriptEvaluator {

    private Path pythonLibDir;

    @Override
    public JsonNode evaluate(String script, JsonNode input) {
        try (Context context =
                     Context.newBuilder(ScriptProperties.ScriptLanguage.PYTHON.getName())
                             .allowAllAccess(true)
                             .option("python.StdLibHome", pythonLibDir.toString() + "/python3")
                             .option("python.ForceImportSite", "true")
                             .build()
        ) {
            String language = ScriptProperties.ScriptLanguage.PYTHON.getName();

            return evaluate(script, input, language, context);
        }
    }

    @PostConstruct
    private void initializePythonEnvironment() {
        try {
            pythonLibDir = extractZipToTempDir();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void destroyPythonEnvironment() {
        try {
            FileSystemUtils.deleteRecursively(pythonLibDir);
        } catch (IOException e) {
            log.error("Could not remove the temp directory: {}", pythonLibDir, e);
        }

    }

    private static Path extractZipToTempDir() throws IOException {
        Path tempDir = Files.createTempDirectory("zip-extract-");

        InputStream zipStream = ScriptRunner.class.getResourceAsStream("/python3.zip");

        if (zipStream == null) {
            throw new IllegalArgumentException("Resource /python3.zip not found");
        }

        try (
            zipStream;
            ZipInputStream zis = new ZipInputStream(zipStream)
        ) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                Path resolvedPath = tempDir.resolve(entryName).normalize();
                if (!resolvedPath.startsWith(tempDir)) {
                    throw new IOException("Zip entry outside target dir: " + entryName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zis, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        return tempDir;
    }
}
