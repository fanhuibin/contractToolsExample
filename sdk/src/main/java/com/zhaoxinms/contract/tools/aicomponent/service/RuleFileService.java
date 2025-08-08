package com.zhaoxinms.contract.tools.aicomponent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleFileService {

    private final ObjectMapper objectMapper;
    private final Environment environment;

    public Path resolveBaseDir() {
        String configured = environment.getProperty("zxcm.ai.contract.rules-dir");
        List<String> candidates = new ArrayList<>();
        if (configured != null && !configured.trim().isEmpty()) {
            candidates.add(configured);
        }
        candidates.add("sdk/src/main/resources/contract-extract-rules");
        candidates.add("src/main/resources/contract-extract-rules");
        for (String c : candidates) {
            Path p = Paths.get(c).toAbsolutePath().normalize();
            if (Files.exists(p) && Files.isDirectory(p)) {
                return p;
            }
        }
        throw new IllegalStateException("Rules directory not found. Please configure zxcm.ai.contract.rules-dir");
    }

    public List<Map<String, String>> listModels() throws IOException {
        Path base = resolveBaseDir();
        List<Map<String, String>> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base, "*.json")) {
            for (Path file : stream) {
                try {
                    JsonNode root = objectMapper.readTree(Files.readString(file, StandardCharsets.UTF_8));
                    String contractType = root.has("contractType") ? root.get("contractType").asText() : stripExt(file.getFileName().toString());
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("contractType", contractType);
                    item.put("filename", file.getFileName().toString());
                    result.add(item);
                } catch (Exception e) {
                    log.warn("Failed to parse rules file: {}", file, e);
                }
            }
        }
        result.sort(Comparator.comparing(m -> m.getOrDefault("contractType", "")));
        return result;
    }

    private String stripExt(String name) {
        int i = name.lastIndexOf('.')
                ;
        return i > 0 ? name.substring(0, i) : name;
    }

    public JsonNode readRule(String contractType) throws IOException {
        Path base = resolveBaseDir();
        Path file = base.resolve(contractType + ".json").normalize();
        if (!file.startsWith(base)) {
            throw new SecurityException("Invalid path");
        }
        if (!Files.exists(file)) {
            throw new NoSuchFileException(file.toString());
        }
        return objectMapper.readTree(Files.readString(file, StandardCharsets.UTF_8));
    }

    public void writeRule(String contractType, JsonNode content) throws IOException {
        Path base = resolveBaseDir();
        Path file = base.resolve(contractType + ".json").normalize();
        if (!file.startsWith(base)) {
            throw new SecurityException("Invalid path");
        }
        String pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content);
        Files.writeString(file, pretty, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}


