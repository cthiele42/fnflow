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

package org.ct42.fnflow.batchfnlib.script;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ct42.fnflow.batchdlt.BatchElement;
import org.ct42.fnflow.cfgfns.ConfigurableFunction;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Sajjad Safaeian
 */
@Slf4j
@Component("ScriptRunner")
@RegisterReflection(classes = JsonPointer.class, memberCategories = MemberCategory.INVOKE_PUBLIC_METHODS)
public class ScriptRunner extends ConfigurableFunction<List<BatchElement>, List<BatchElement>, ScriptProperties> {
    @Override
    public List<BatchElement> apply(List<BatchElement> input) {

        Path pythonLibDir = null;
        try {
            pythonLibDir = extractZipToTempDir();

            try (Context context =
                         Context.newBuilder(ScriptProperties.ScriptLanguage.PYTHON.getName(),
                                         ScriptProperties.ScriptLanguage.JS.getName())
                                 .allowAllAccess(true)
                                 .option("python.StdLibHome", pythonLibDir.toString() + "/python3")
                                 .option("python.ForceImportSite", "true")
                                 .build()
            ) {
                List<BatchElement> result = new ArrayList<>();

                AtomicInteger index = new AtomicInteger();
                input.forEach(batchElement -> {
                    try {
                        int currentInput = index.getAndIncrement();
                        context.getBindings(properties.getScriptLanguage().getName()).putMember("input", batchElement.getInput().toString());

                        Value evaluationResult = context.eval(properties.getScriptLanguage().getName(), properties.getScript());
                        ObjectMapper mapper = new ObjectMapper();
                        List<JsonNode> evaluatedJsons = new ArrayList<>();
                        for (int i = 0; i < evaluationResult.getArraySize(); i++) {
                            Object raw = evaluationResult.getArrayElement(i).as(Object.class);
                            String json = mapper.writeValueAsString(raw);
                            evaluatedJsons.add(mapper.readTree(json));
                        }

                        result.addAll(
                                evaluatedJsons
                                        .stream()
                                        .map(jsonNode -> {
                                            BatchElement element = new BatchElement(batchElement.getInput(), currentInput);
                                            element.processWithOutput(jsonNode);
                                            return element;
                                        })
                                        .toList()
                        );

                    } catch (Exception e) {
                        log.warn("Script: {}", properties.getScript());
                        log.warn("Script Language: {}", properties.getScriptLanguage().getName());
                        log.error("Script runner", e);
                        batchElement.processWithError(e);
                        result.add(batchElement);
                    }
                });

                return result;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                FileSystemUtils.deleteRecursively(pythonLibDir);
            } catch (IOException e) {
                log.error("Could not remove the temp directory: {}", pythonLibDir, e);
            }
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
