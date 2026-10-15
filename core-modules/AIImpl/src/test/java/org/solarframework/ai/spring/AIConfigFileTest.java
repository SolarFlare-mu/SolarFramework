package org.solarframework.ai.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solarframework.ai.Chatbot;
import org.solarframework.ai.IAIService;
import org.solarframework.ai.dto.AIOptions;
import org.solarframework.ai.dto.ChatbotDefinition;
import org.solarframework.core.util.SolarHome;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Config-file round-trip. Needs no server. */
class AIConfigFileTest {

    private Path file;
    private AIManager manager;
    private String previousHome;

    @BeforeEach void setUp() throws Exception {
        previousHome = System.getProperty(SolarHome.PROPERTY);
        Path home = Files.createTempDirectory("ai-config");
        System.setProperty(SolarHome.PROPERTY, home.toString());
        SolarHome.forgetDataFolder();
        file = home.resolve(AIConfig.FILE);
        manager = AIManagerTest.freshManager();
    }

    @AfterEach void tearDown() throws Exception {
        Files.deleteIfExists(file);
        Files.deleteIfExists(file.getParent());
        Files.deleteIfExists(file.getParent().getParent());
        Files.deleteIfExists(file.getParent().getParent().getParent());
        if (previousHome == null) System.clearProperty(SolarHome.PROPERTY);
        else System.setProperty(SolarHome.PROPERTY, previousHome);
        SolarHome.forgetDataFolder();
    }

    /** The file is committed or shared, so what it holds must stay the variable's name, even with the variable set. */
    @Test void aKeyNamingAVariableIsSavedAsTheNameNotTheSecret() throws Exception {
        System.setProperty("SOLAR_AI_CONFIG_TEST_KEY", "gsk_secret");
        try {
            manager.makeNewService("cloud", "https://api.groq.com/openai", "${SOLAR_AI_CONFIG_TEST_KEY}", "openai/gpt-oss-120b");
            manager.SaveAsFile();
            assertFalse(Files.readString(file).contains("gsk_secret"));
            AIManager reloaded = AIManagerTest.freshManager();
            reloaded.LoadFromFile();
            assertEquals("${SOLAR_AI_CONFIG_TEST_KEY}", reloaded.getService("cloud").getApiKey());
        } finally {
            System.clearProperty("SOLAR_AI_CONFIG_TEST_KEY");
        }
    }

    @Test void savesServicesThenReadsThemBack() {
        manager.makeNewService("coder", "qwen/qwen3-coder-30b");
        manager.SaveAsFile();

        assertTrue(new File(file.toString()).isFile());

        AIManager reloaded = AIManagerTest.freshManager();
        reloaded.LoadFromFile();

        IAIService coder = reloaded.getService("coder");
        assertEquals("coder", coder.getName());
        assertEquals("qwen/qwen3-coder-30b", coder.getModel());
        assertEquals(120, coder.getTimeoutSeconds(), "the timeout should round-trip");
        assertEquals(30, coder.getRequestsPerMinute(), "the rate limit should round-trip");
    }

    /** The file is where an agent lives between two runs, so everything that is settings has to come back - persona, model, bounds, and whatever the application asked to have remembered alongside them. */
    @Test void savesChatbotsThenReadsThemBack() {
        manager.makeNewService("coder", "qwen/qwen3-coder-30b");
        manager.addChatbot(manager.getService("coder").createChatbot().name("reviewer")
                .systemPrompt("You review code.").maxSteps(9).chatOptions(AIOptions.INFORMATIONIST)
                .attributes(List.of("readsManual", "keepHours:48")).build());
        manager.SaveAsFile();

        AIManager reloaded = AIManagerTest.freshManager();
        reloaded.LoadFromFile();

        Chatbot reviewer = reloaded.getChatbot("reviewer");
        assertNotNull(reviewer);
        assertEquals("You review code.", reviewer.getSystemPrompt());
        assertEquals(9, reviewer.getMaxSteps());
        assertEquals("coder", reviewer.getService().getName(), "it should come back on the service it named");
        assertEquals(List.of("readsManual", "keepHours:48"), reviewer.getAttributes());
        assertTrue(reviewer.getTools().isEmpty(), "tools are live objects, handed to a restored agent in code");
    }

    /** An agent may be written down before any endpoint has been configured. It cannot answer, but it must survive the next save rather than be dropped for having nowhere to run. */
    @Test void anAgentWithNoServiceStillComesBack() {
        manager.removeService(IAIService.DEFAULT);
        manager.addChatbot(new ChatbotDefinition("orphan", "", "You wait.", null, null, 6));
        manager.SaveAsFile();

        AIManager reloaded = AIManagerTest.freshManager();
        reloaded.LoadFromFile();

        assertNotNull(reloaded.getChatbot("orphan"));
        assertEquals("You wait.", reloaded.getChatbot("orphan").getSystemPrompt());
    }

    /** Nothing is seeded from a property file any more, so the file is the whole truth - including for the default. */
    @Test void theFileGivesTheDefaultServiceItsEndpointToo() {
        manager.SaveAsFile();

        AIManager reloaded = AIManagerTest.freshManager();
        reloaded.getDefaultService().setBaseUrl("http://elsewhere:9999");
        reloaded.LoadFromFile();

        assertEquals("http://localhost:1234", reloaded.getDefaultService().getBaseUrl(), "what the file says, not what the manager held before");
        assertEquals("openai/gpt-oss-20b", reloaded.getDefaultService().getModel());
    }

    @Test void loadingReplacesNonDefaultServices() {
        manager.makeNewService("kept");
        manager.SaveAsFile();
        manager.makeNewService("not-in-the-file");

        manager.LoadFromFile();
        assertTrue(manager.hasService("kept"));
        assertFalse(manager.hasService("not-in-the-file"), "services absent from the file should not survive");
    }

    @Test void missingFileIsCreatedFromTheCurrentSetup() {
        manager.makeNewService("triage");
        manager.LoadFromFile();

        assertTrue(new File(file.toString()).isFile());
        assertTrue(manager.hasService("triage"));
    }
}
