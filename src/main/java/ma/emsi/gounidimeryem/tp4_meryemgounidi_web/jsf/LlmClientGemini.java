package ma.emsi.gounidimeryem.tp4_meryemgounidi_web.jsf;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
public class LlmClientGemini {

    private static final Logger log = LoggerFactory.getLogger(LlmClientGemini.class);

    @Inject
    private RagService ragService;

    private Assistant assistant;
    private ChatMemory chatMemory;
    private String systemRole;

    @PostConstruct
    public void init() {
        String apiKey = System.getenv("GeminiKey");
        if (apiKey == null || apiKey.isBlank()) {
            Properties p = new Properties();
            try (InputStream in = getClass().getResourceAsStream("/chat.properties")) {
                if (in != null) {
                    p.load(in);
                    apiKey = p.getProperty("GeminiKey");
                }
            } catch (Exception e) {
                log.error("Lecture chat.properties échouée", e);
            }
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Clé Gemini absente.");
            return;
        }

        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-flash")
                .build();

        chatMemory = MessageWindowChatMemory.withMaxMessages(12);

        ContentRetriever retriever = (ragService != null) ? ragService.getContentRetriever() : null;

        if (retriever != null) {
            assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .chatMemory(chatMemory)
                    .contentRetriever(retriever)
                    .build();
            log.info("Assistant initialisé avec RAG.");
        } else {
            assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .chatMemory(chatMemory)
                    .build();
            log.info("Assistant initialisé sans RAG (retriever indisponible).");
        }
    }

    public void setSystemRole(String systemRole) {
        if (chatMemory == null) return;
        chatMemory.clear();
        this.systemRole = systemRole;
        if (systemRole != null && !systemRole.isBlank()) {
            chatMemory.add(SystemMessage.from(systemRole));
        }
    }

    public String chat(String question) {
        if (assistant == null) return "Assistant non disponible (clé API manquante).";
        return assistant.chat(question);
    }
}