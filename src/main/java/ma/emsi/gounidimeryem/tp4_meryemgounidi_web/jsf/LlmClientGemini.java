package ma.emsi.gounidimeryem.tp4_meryemgounidi_web.jsf;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
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
        try {
            String apiKey = System.getenv("GeminiKey");
            if (apiKey == null || apiKey.isBlank()) {
                Properties p = new Properties();
                try (InputStream in = getClass().getResourceAsStream("/chat.properties")) {
                    if (in != null) {
                        p.load(in);
                        apiKey = p.getProperty("GeminiKey");
                    }
                }
            }
            if (apiKey == null || apiKey.isBlank()) {
                log.error("Clé Gemini absente.");
                return;
            }

            ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gemini-2.5-flash")
                    .build();

            chatMemory = MessageWindowChatMemory.withMaxMessages(12);

            var b = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .chatMemory(chatMemory);

            if (ragService != null && ragService.getContentRetriever() != null) {
                b = b.contentRetriever(ragService.getContentRetriever());
                log.info("Assistant avec RAG.");
            } else {
                log.info("Assistant sans RAG.");
            }

            assistant = b.build();
        } catch (Exception e) {
            log.error("Init LLM échec", e);
        }
    }

    public void setSystemRole(String role) {
        systemRole = role;
        if (chatMemory != null) {
            chatMemory.clear();
            if (role != null && !role.isBlank()) {
                chatMemory.add(SystemMessage.from(role));
            }
        }
    }

    public String chat(String question) {
        if (assistant == null) return "Assistant indisponible.";
        return assistant.chat(question);
    }

    public void reset() {
        if (chatMemory != null) {
            chatMemory.clear();
            if (systemRole != null && !systemRole.isBlank()) {
                chatMemory.add(SystemMessage.from(systemRole));
            }
        }
    }
}