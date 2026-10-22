package com.fincard.audit.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fincard.audit.dto.TransactionApprovedMessage;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService implements IAuditService {

    private final JsonMapper jsonMapper;

    private final Set<String> registeredEventIds = new HashSet<>();

    @Value("${app.audit.file}")
    private String auditFile;

    private Path path;

    @PostConstruct
    public void initialize() {

        path = Path.of(auditFile).toAbsolutePath();

        try {
            Files.createDirectories(path.getParent());

            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            loadExistingEventIds();

            log.info(
                    "Auditoria preparada: archivo={}, eventos existentes={}",
                    path,
                    registeredEventIds.size()
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo preparar el archivo de auditoria",
                    exception
            );
        }
    }

    private void loadExistingEventIds() throws IOException {

        try (BufferedReader reader = Files.newBufferedReader(
                path,
                StandardCharsets.UTF_8)) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {
                    TransactionApprovedMessage message =
                            jsonMapper.readValue(
                                    line,
                                    TransactionApprovedMessage.class
                            );

                    if (message == null
                            || message.eventId() == null
                            || message.eventId().isBlank()) {

                        throw new IllegalArgumentException(
                                "El evento no contiene eventId"
                        );
                    }

                    registeredEventIds.add(message.eventId());

                } catch (RuntimeException exception) {
                    throw new IllegalStateException(
                            "Linea de auditoria invalida: " + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    @Override
    public synchronized boolean register(
            TransactionApprovedMessage message) {

        String eventId = message.eventId();
        String json = jsonMapper.writeValueAsString(message);

        if (registeredEventIds.contains(eventId)) {
            log.info("EVENTO YA REGISTRADO: {}", json);
            return false;
        }

        try {
            Files.writeString(
                    path,
                    json + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            registeredEventIds.add(eventId);

            log.info("EVENTO AGREGADO A AUDITORIA: {}", json);

            return true;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo escribir el evento: " + eventId,
                    exception
            );
        }
    }
}