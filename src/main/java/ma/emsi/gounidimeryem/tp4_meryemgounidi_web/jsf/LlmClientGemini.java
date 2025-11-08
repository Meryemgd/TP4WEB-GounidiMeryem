package ma.emsi.gounidimeryem.tp4_meryemgounidi_web.jsf;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
public class LlmClientGemini {

    private static final Logger log = LoggerFactory.getLogger(LlmClientGemini.class);

    private Assistant assistant;
    private ChatMemory chatMemory;
    private String systemRole;

    @PostConstruct
    public void init() {
        // Priorité 1: Variable d'environnement
        String apiKey = System.getenv("GeminiKey");

        // Priorité 2: Fichier chat.properties
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Variable d'environnement 'GeminiKey' non trouvée. Tentative de lecture depuis chat.properties.");
            Properties props = new Properties();
            try (InputStream in = LlmClientGemini.class.getResourceAsStream("/chat.properties")) {
                if (in != null) {
                    props.load(in);
                    apiKey = props.getProperty("GeminiKey");
                } else {
                    log.warn("Fichier /chat.properties non trouvé dans les ressources.");
                }
            } catch (Exception e) {
                log.error("Impossible de charger chat.properties", e);
            }
        }

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            log.info("Clé API Gemini trouvée. Initialisation du modèle...");
            ChatModel model = GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gemini-2.5-flash") // Vous pourrez changer pour 2.5-flash plus tard
                    .build();

            this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);

            this.assistant = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    .chatMemory(chatMemory)
                    .build();
            log.info("Assistant IA initialisé avec succès.");
        } else {
            log.error("AUCUNE CLÉ API GEMINI TROUVÉE. L'assistant IA sera désactivé.");
            // L'assistant reste null, mais l'application ne plante pas.
        }
    }

    public void setSystemRole(String systemRole) {
        if (this.chatMemory == null) return; // Sécurité si l'init a échoué
        this.chatMemory.clear();
        this.systemRole = systemRole;
        if (systemRole != null && !systemRole.isEmpty()) {
            this.chatMemory.add(SystemMessage.from(systemRole));
        }
    }

    public String chat(String question) {
        if (this.assistant == null) {
            return "ERREUR : L'assistant n'est pas configuré. Vérifiez la clé API Gemini dans les logs du serveur.";
        }
        return this.assistant.chat(question);
    }
}