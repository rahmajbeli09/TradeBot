package com.example.chatbotnasoft.service;

import com.example.chatbotnasoft.dto.RawFeedLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class FileReadingService {

    public Stream<RawFeedLine> readFileLines(Path filePath) throws IOException {
        log.info("📂 Début de la lecture du fichier: {}", filePath.getFileName());
        
        if (!Files.exists(filePath)) {
            throw new IOException("Le fichier n'existe pas: " + filePath);
        }

        if (!Files.isReadable(filePath)) {
            throw new IOException("Le fichier n'est pas lisible: " + filePath);
        }

        String fileName = filePath.getFileName().toString();
        log.info("✅ Fichier trouvé et lisible: {}", fileName);
        
        try {
            Stream<String> lines = Files.lines(filePath);
            log.info("🔄 Stream de lignes créé pour: {}", fileName);
            return processLines(lines, fileName)
                    .onClose(() -> log.info("🔚 Lecture du fichier terminée: {}", fileName));
        } catch (IOException e) {
            log.error("❌ Erreur lors de la lecture du fichier: {}", filePath, e);
            throw e;
        }
    }

    private Stream<RawFeedLine> processLines(Stream<String> lines, String fileName) {
        Iterator<String> lineIterator = lines.iterator();
        Iterator<RawFeedLine> rawFeedLineIterator = new Iterator<>() {
            private int lineNumber = 0;

            @Override
            public boolean hasNext() {
                return lineIterator.hasNext();
            }

            @Override
            public RawFeedLine next() {
                String rawLine = lineIterator.next();
                lineNumber++;
                
                String trimmedLine = rawLine.trim();
                
                if (trimmedLine.isEmpty()) {
                    log.debug("⏭️ Ligne vide ignorée: {} (ligne {})", fileName, lineNumber);
                    return null;
                }

                RawFeedLine feedLine = new RawFeedLine(trimmedLine, lineNumber, fileName);
                log.info("📝 Ligne lue: {} (ligne {}) -> '{}'", fileName, lineNumber, trimmedLine);
                
                return feedLine;
            }
        };

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(rawFeedLineIterator, Spliterator.ORDERED),
                false
        ).filter(line -> line != null && line.isValid());
    }

    public long countLines(Path filePath) throws IOException {
        log.info("Comptage des lignes du fichier: {}", filePath.getFileName());
        
        try (Stream<String> lines = Files.lines(filePath)) {
            long count = lines
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .count();
            
            log.info("Fichier {} contient {} lignes valides", filePath.getFileName(), count);
            return count;
        }
    }

    public boolean isValidFeedFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        return fileName != null && fileName.startsWith("FEED") && fileName.endsWith(".txt");
    }
}
