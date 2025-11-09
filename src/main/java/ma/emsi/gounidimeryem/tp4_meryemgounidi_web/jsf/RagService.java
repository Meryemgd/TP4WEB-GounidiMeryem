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
    private ContentRetriever contentRetriever;

    @PostConstruct
    public void init() {
        try {
            EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
                    .modelId("sentence-transformers/all-MiniLM-L6-v2")
                    .accessToken(System.getenv("HUGGING_FACE_API_KEY"))
                    .build();

            EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

            URL docsUrl = getClass().getClassLoader().getResource("documents");
            if (docsUrl == null) {
                log.warn("resources/documents introuvable.");
                return;
            }
            Path docsPath = Paths.get(docsUrl.toURI());
            File[] pdfs = docsPath.toFile().listFiles(f -> f.getName().toLowerCase().endsWith(".pdf"));
            if (pdfs == null || pdfs.length == 0) {
                log.warn("Aucun PDF.");
                return;
            }

            DocumentParser parser = new ApachePdfBoxDocumentParser();
            for (File pdf : pdfs) {
                log.info("Ingestion {}", pdf.getName());
                Document doc = FileSystemDocumentLoader.loadDocument(pdf.toPath(), parser);
                List<TextSegment> segments = DocumentSplitters.recursive(300, 50).split(doc);
                store.addAll(embeddingModel.embedAll(segments).content(), segments);
                log.info("  {} segments.", segments.size());
            }

            contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(store)
                    .embeddingModel(embeddingModel)
                    .maxResults(3)
                    .minScore(0.60)
                    .build();

            log.info("ContentRetriever prêt.");
        } catch (Exception e) {
            log.error("Init RAG échec", e);
        }
    }

    public ContentRetriever getContentRetriever() {
        return contentRetriever;
    }
}