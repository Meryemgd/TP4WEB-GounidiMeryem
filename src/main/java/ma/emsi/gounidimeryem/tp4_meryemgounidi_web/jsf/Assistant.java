package ma.emsi.gounidimeryem.tp4_meryemgounidi_web.jsf;

import dev.langchain4j.service.UserMessage;

public interface Assistant {
    String chat(@UserMessage String message);
}