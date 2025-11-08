package ma.emsi.gounidimeryem.tp4_meryemgounidi_web.jsf;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ApplicationScoped
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;
    private ContentRetriever contentRetriever;

    @PostConstruct
    public void init() {
        try {
            embeddingModel = HuggingFaceEmbeddingModel.builder()
                    .modelId("sentence-transformers/all-MiniLM-L6-v2")
                    .build();
            embeddingStore = new InMemoryEmbeddingStore<>();

            URL dirUrl = getClass().getClassLoader().getResource("documents");
            if (dirUrl == null) {
                log.error("Dossier resources/documents introuvable.");
                return;
            }
            Path dirPath = Paths.get(dirUrl.toURI());
            File[] pdfFiles = dirPath.toFile().listFiles(f -> f.getName().toLowerCase().endsWith(".pdf"));
            if (pdfFiles == null || pdfFiles.length < 2) {
                log.warn("Moins de 2 PDF trouvés. RAG limité.");
            }

            DocumentParser parser = new ApachePdfBoxDocumentParser();

            for (File f : pdfFiles) {
                log.info("Ingestion PDF: {}", f.getName());
                Document doc = FileSystemDocumentLoader.loadDocument(f.toPath(), parser);
                List<TextSegment> segments = DocumentSplitters.recursive(300, 50).split(doc);
                embeddingStore.addAll(
                        embeddingModel.embedAll(segments).content(),
                        segments
                );
                log.info("  {} segments ingérés.", segments.size());
            }

            contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(3)
                    .minScore(0.60) // filtre basique
                    .build();

            log.info("ContentRetriever prêt.");
        } catch (Exception e) {
            log.error("Erreur init RAG", e);
        }
    }

    public ContentRetriever getContentRetriever() {
        return contentRetriever;
    }
}