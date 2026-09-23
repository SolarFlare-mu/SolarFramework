package org.solarframework.ai.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solarframework.ai.IAIManager;
import org.solarframework.ai.dto.ChatbotDefinition;
import org.solarframework.ai.dto.ServiceDefinition;
import org.solarframework.json.JSONItem;

import java.util.ArrayList;
import java.util.List;

/** AI services and chatbots as stored in `config/ai/agents.json`. */
class AIConfigFile extends JSONItem<AIConfigFile> {
    private static final Logger log = LoggerFactory.getLogger(AIConfigFile.class);

    private final List<ServiceDefinition> services = new ArrayList<>();
    private final List<ChatbotDefinition> chatbots = new ArrayList<>();

    public AIConfigFile(IAIManager manager) {
        services.addAll(manager.getServiceDefinitions());
        chatbots.addAll(manager.getChatbotDefinitions());
    }

    @Override public String file() { return AIConfig.FILE; }
    public List<ServiceDefinition> getServices() { return services; }
    public List<ChatbotDefinition> getChatbots() { return chatbots; }

    /** A missing or unreadable file is written from what the manager currently holds. */
    public static AIConfigFile read(String path, IAIManager manager) {
        AIConfigFile file = ReadJSON(path, AIConfigFile.class);
        if (file != null) return file;
        log.warn("No readable AI config file at {}, creating one from the current setup", path);
        return ReadOrCreate(path, AIConfigFile.class, new AIConfigFile(manager));
    }
}
