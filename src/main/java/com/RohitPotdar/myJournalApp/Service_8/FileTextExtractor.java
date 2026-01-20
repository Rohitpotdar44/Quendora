package com.RohitPotdar.myJournalApp.Service_8;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileTextExtractor {

    private static final int MAX_EXTRACTED_CHARS = 80_000;

    public ExtractionResult extractText(byte[] bytes, String contentType) {
        List<String> warnings = new ArrayList<>();
        if (bytes == null || bytes.length == 0) {
            warnings.add("File has no readable content.");
            return new ExtractionResult("", warnings);
        }

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(MAX_EXTRACTED_CHARS);
        Metadata metadata = new Metadata();
        if (contentType != null && !contentType.isBlank()) {
            metadata.set(Metadata.CONTENT_TYPE, contentType);
        }

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            parser.parse(inputStream, handler, metadata, new ParseContext());
            String text = handler.toString().trim();
            if (text.isBlank()) {
                warnings.add("No text could be extracted from the file.");
            }
            return new ExtractionResult(text, warnings);
        } catch (IOException | SAXException | TikaException e) {
            warnings.add("Text extraction failed: " + e.getMessage());
            return new ExtractionResult("", warnings);
        }
    }

    public record ExtractionResult(String text, List<String> warnings) {}
}
