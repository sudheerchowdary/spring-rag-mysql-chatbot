package com.rag.mysql.rag_mysql_chatbot.service;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LLMSQLService {

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiChatModel llm;

    public LLMSQLService(JdbcTemplate jdbcTemplate,
                         @Value("${openai.api-key}") String apiKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.llm = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
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

        // Step 2: Generate SQL from question
        String sql = llm.generate(prompt);
        sql = sql.replaceAll("(?i)```sql", "")
                .replaceAll("```", "")
                .trim();

        System.out.println("Generated SQL: " + sql);
        // Step 3: Execute SQL
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
