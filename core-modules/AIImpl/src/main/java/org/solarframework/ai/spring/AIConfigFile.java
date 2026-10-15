package org.solarframework.ai.spring;

import org.solarframework.ai.IAIManager;
import org.solarframework.ai.dto.ChatbotDefinition;
import org.solarframework.ai.dto.ServiceDefinition;
import org.solarframework.json.JSONItem;

import java.util.ArrayList;
import java.util.List;

/** AI services and chatbots as stored in `config/ai/agents.json`. */
class AIConfigFile extends JSONItem<AIConfigFile> {

    private final List<ServiceDefinition> services = new ArrayList<>();
    private final List<ChatbotDefinition> chatbots = new ArrayList<>();

    public AIConfigFile(IAIManager manager) {
        services.addAll(manager.getServiceDefinitions());
        chatbots.addAll(manager.getChatbotDefinitions());
    }

    @Override public String file() { return AIConfig.FILE; }
    public List<ServiceDefinition> getServices() { return services; }
    public List<ChatbotDefinition> getChatbots() { return chatbots; }

}
