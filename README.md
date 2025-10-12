# Getting Started

### 🧠 Overview

Goal:
Use an LLM to understand natural language questions (like “Show me all users who joined last month”), retrieve the relevant context (schema or docs), generate the correct SQL query, execute it against MySQL, and return the result.

This is a RAG pipeline:

Retriever → fetch context (e.g., table schemas or previous examples).

Generator (LLM) → generate SQL query from the question + context.

Executor → run the SQL query in MySQL and return results.

### ⚙️ Tech Stack

Spring Boot (Java)

MySQL (database)

LangChain4j or Spring AI (for LLM + RAG)

OpenAI GPT or Ollama (local LLM)


### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.
