package com.rag.mysql.rag_mysql_chatbot.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class LLMSQLService {

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiChatModel openAiLlm;
    private final OllamaChatModel ollamaLlm;
    private boolean useOllamaFallback = false;

    public LLMSQLService(JdbcTemplate jdbcTemplate,
                         @Value("${openai.api-key}") String apiKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.openAiLlm = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o")
                .temperature(0.2)
                .build();
        
        // Initialize Ollama fallback model
        this.ollamaLlm = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")  // ✅ Correct Ollama default port
                .modelName("neural-chat")  // or "neural-chat", "llama2", etc.
                .temperature(0.2)
                .build();
    }

    public String queryDatabase(String question) {
        // Step 1: Provide schema context to LLM
        String schemaInfo = """
                You are an expert SQL assistant.
                Database: MySQL
                Tables:
                  products(product_id INT PRIMARY KEY AUTO_INCREMENT, product_name VARCHAR(255) NOT NULL,description TEXT, price DECIMAL(10, 2) NOT NULL, stock_quantity INT NOT NULL DEFAULT 0)
                  orders(order_id INT PRIMARY KEY AUTO_INCREMENT,customer_name VARCHAR(255) NOT NULL,order_date DATETIME DEFAULT CURRENT_TIMESTAMP,total_amount DECIMAL(10, 2) NOT NULL)
                  order_items (order_item_id INT PRIMARY KEY AUTO_INCREMENT,order_id INT NOT NULL,product_id INT NOT NULL,quantity INT NOT NULL,price_at_purchase DECIMAL(10, 2) NOT NULL, FOREIGN KEY (order_id) REFERENCES orders(order_id),FOREIGN KEY (product_id) REFERENCES products(product_id)
                Convert the user question into a safe SQL query.
                """;

        String prompt = schemaInfo + "\n\nUser question: " + question +
                "\nReturn only SQL query.";

        // Step 2: Generate SQL from question (with fallback)
        String sql = generateSQL(prompt);
        
        // Step 3: Execute SQL and return formatted results
        return executeSql(sql);
    }

    /**
     * Generate SQL with fallback from OpenAI to Ollama
     */
    private String generateSQL(String prompt) {
        String sql;
        
        // Try OpenAI first (unless fallback is already active)
        if (!useOllamaFallback) {
            try {
                sql = openAiLlm.generate(prompt);
                System.out.println("Using OpenAI GPT-4o");
            } catch (Exception e) {
                // Check if it's a quota error
                if (isQuotaExceededError(e)) {
                    System.out.println("⚠️  OpenAI quota exceeded. Attempting fallback to Ollama...");
                    
                    // Check if Ollama is actually available before trying
                    if (isOllamaAvailable()) {
                        useOllamaFallback = true;
                        sql = generateWithOllama(prompt);
                    } else {
                        // Both OpenAI quota and Ollama unavailable
                        System.err.println("❌ OpenAI quota exceeded AND Ollama is not running");
                        sql = returnGracefulFallbackResponse();
                    }
                } else {
                    throw e;
                }
            }
        } else {
            // Use Ollama if fallback is already active
            if (isOllamaAvailable()) {
                sql = generateWithOllama(prompt);
            } else {
                // Ollama became unavailable during operation
                System.err.println("❌ Ollama became unavailable");
                sql = returnGracefulFallbackResponse();
            }
        }
        
        // Clean SQL (remove markdown formatting)
        if (sql != null && !sql.startsWith("ERROR:")) {
            sql = sql.replaceAll("(?i)```sql", "")
                    .replaceAll("```", "")
                    .trim();
        }
        
        System.out.println("Generated SQL: " + sql);
        return sql;
    }

    /**
     * Check if Ollama is actually available
     */
    private boolean isOllamaAvailable() {
        try {
            // Try to make a simple request to Ollama to verify it's running
            // This prevents trying to use Ollama when it's not available
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            
            Request request = new Request.Builder()
                    .url("http://localhost:11434/api/tags")
                    .build();
            
            Response response = client.newCall(request).execute();
            response.close();
            
            System.out.println("✓ Ollama is available at localhost:11434");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️  Ollama is not available: " + e.getMessage());
            return false;
        }
    }

    /**
     * Return a graceful fallback message when both LLMs fail
     */
    private String returnGracefulFallbackResponse() {
        return """
                ERROR: Unable to generate SQL. Both OpenAI and Ollama are unavailable.
                
                Options:
                1. If OpenAI quota exceeded: Wait for quota reset or upgrade plan
                2. If you want to use Ollama fallback:
                   - Install: brew install ollama
                   - Start: ollama serve
                   - Pull model: ollama pull mistral
                   - Then retry your request
                
                3. Or use Docker: docker run -d -p 11434:11434 ollama/ollama""";
    }

    /**
     * Generate SQL using Ollama fallback model
     */
    private String generateWithOllama(String prompt) {
        try {
            System.out.println("Using Ollama (local LLM) as fallback");
            return ollamaLlm.generate(prompt);
        } catch (Exception e) {
            System.err.println("❌ Ollama generation failed: " + e.getMessage());
            
            // Return graceful fallback instead of crashing
            if (e.getMessage().contains("Connection refused") || 
                e.getMessage().contains("Failed to connect")) {
                return returnGracefulFallbackResponse();
            }
            
            // For other errors, also return graceful message instead of crash
            return "ERROR: Failed to generate SQL using Ollama. Details: " + e.getMessage();
        }
    }

    /**
     * Check if the error is due to OpenAI quota exceeded
     */
    private boolean isQuotaExceededError(Exception e) {
        String errorMessage = e.getMessage();
        return errorMessage != null && (
            errorMessage.contains("insufficient_quota") ||
            errorMessage.contains("You exceeded your current quota") ||
            errorMessage.contains("quota_exceeded")
        );
    }

    /**
     * Execute the generated SQL and return formatted results
     */
    private String executeSql(String sql) {
        // If SQL is an error message, return it directly
        if (sql != null && sql.startsWith("ERROR:")) {
            return sql;
        }
        
        // If SQL is null or empty, return error
        if (sql == null || sql.isEmpty()) {
            return "ERROR: Unable to generate SQL query";
        }
        
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            if (rows.isEmpty()) {
                return "No records found.";
            }

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    sb.append(entry.getKey())
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Error executing SQL: " + e.getMessage() + "\nSQL: " + sql;
        }
    }
}
