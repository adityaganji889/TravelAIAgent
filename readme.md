# ✈️ Travel AI Agent

### An Intelligent AI-Powered Travel Planning Assistant

Travel AI Agent is an AI-powered travel assistant built using **Spring Boot 4, Spring AI 2.0, OpenAI Models, and Thymeleaf**. It leverages Large Language Models (LLMs) and intelligent tool calling to interact with external services through SERP AI, providing users with travel-related information such as flights, hotels, and weather through natural language conversations.

Instead of manually searching across multiple platforms, users can interact with an intelligent AI agent that understands their requirements, determines which tools are needed, retrieves relevant external information, and generates contextual responses.

---

## 🌍 Overview

Planning a trip involves gathering information from multiple sources, including flight availability, accommodation options, and weather conditions.

Travel AI Agent simplifies this process by combining the reasoning capabilities of Large Language Models with external API integrations through AI-powered tool calling.

The application uses **Spring AI's tool-calling capabilities** to allow OpenAI models to decide when external tools should be invoked based on the user's query.

For example, when a user asks:

> Find flights from Mumbai to Delhi, suggest hotels in Delhi, and tell me the weather there.

The AI agent can identify the required information, invoke the corresponding flight, hotel, and weather tools, process their responses, and present the information in a conversational format.

## ✨ Key Features

- **AI-Powered Conversational Interface:** Interact with the travel assistant using natural language.
- **LLM-Based Tool Calling:** OpenAI models dynamically determine when external tools are required to answer user queries.
- **Flight Information:** Retrieve flight-related information using integrated SERP AI tools.
- **Hotel Information:** Search for hotel and accommodation information based on travel requirements.
- **Weather Information:** Retrieve weather information for travel destinations.
- **External API Integration:** Connect external travel information services through SERP AI integrations.
- **Contextual AI Responses:** Combine information retrieved from external tools with LLM capabilities to generate meaningful responses.
- **Intelligent Tool Selection:** The model selects relevant tools based on the context and intent of the user's request.
- **Server-Side Rendering:** Thymeleaf provides dynamic HTML rendering for the application's user interface.

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java | Primary programming language |
| Spring Boot 4 | Backend application framework |
| Spring AI 2.0 | AI integration and tool-calling orchestration |
| OpenAI Models | Natural language understanding and response generation |
| SERP AI | External flight, hotel, and weather information retrieval |
| Thymeleaf | Server-side HTML templating and frontend rendering |
| HTML, CSS, JavaScript | Frontend structure, styling, and interactions |
| Maven | Dependency management and build automation |

## 🏗️ System Architecture

The application follows an AI-agent-based architecture where the LLM acts as the central decision-making component.

Thymeleaf provides the frontend, Spring Boot manages application requests and backend operations, and Spring AI orchestrates interactions between OpenAI models and external tools.

### High-Level Architecture

```mermaid
flowchart TD
    A["User"] --> B["Thymeleaf Frontend"]
    B --> C["Spring Boot Controllers"]
    C --> D["Spring AI 2.0"]
    D --> E["OpenAI Model"]

    E --> F{"Tool Calling Required?"}

    F -->|No| G["Generate AI Response"]
    F -->|Yes| H["Select Appropriate Tool"]

    H --> I["Flight Search Tool"]
    H --> J["Hotel Search Tool"]
    H --> K["Weather Search Tool"]

    I --> L["SERP AI / External APIs"]
    J --> L
    K --> L

    L --> M["Tool Results"]
    M --> E

    E --> G
    G --> N["Spring Boot Controller"]
    N --> O["Thymeleaf Template Rendering"]
    O --> P["Response Displayed to User"]
```
## Deployed Version:

🧳<a href="https://travelaiagent-2dn7.onrender.com/" target="_blank">
   Travel AI Agent
</a>

## Demonstration:

<img width="1365" height="767" alt="TravelAIAgentHostedDashboard" src="https://github.com/user-attachments/assets/1aaeb21b-e971-486b-af63-ea1308c07e38" />
<img width="1365" height="767" alt="TravelAIAgentHosted1" src="https://github.com/user-attachments/assets/d8e0adb1-eba7-4a98-88be-35611f0afaf8" />
<img width="1360" height="767" alt="TravelAIAgentHosted2" src="https://github.com/user-attachments/assets/1beef75c-9c7d-4593-ba4b-fd2a79abb44e" />
<img width="1365" height="767" alt="TravelAIAgentHosted3" src="https://github.com/user-attachments/assets/4ab9a3f9-db27-4653-ae6e-290cc19c00ca" />
<img width="1365" height="767" alt="TravelAIAgentHosted4" src="https://github.com/user-attachments/assets/da889c89-0c9c-4b39-ba7f-d0b7d7a23a2e" />
<img width="1365" height="767" alt="TravelAIAgentHosted5" src="https://github.com/user-attachments/assets/8a1d6f56-204f-4a9e-998b-c04a02050fad" />
<img width="1365" height="767" alt="TravelAIAgentHosted6" src="https://github.com/user-attachments/assets/7fedc73d-85f6-4ba5-bf0e-1fb08ceaec58" />
<img width="286" height="513" alt="TravelAIAgentResponsiveDashboard" src="https://github.com/user-attachments/assets/05dfd0a3-8ddc-4b02-808e-3444d351dcfe" />
<img width="296" height="515" alt="TravelAIAgentResponsive1" src="https://github.com/user-attachments/assets/af430294-e42c-434c-ba77-ddd151e6ade8" />
<img width="293" height="520" alt="TravelAIAgentResponsive2" src="https://github.com/user-attachments/assets/5c981383-e68d-495c-b01b-6162c6fc3797" />
<img width="292" height="516" alt="TravelAIAgentResponsive3" src="https://github.com/user-attachments/assets/28b64862-ca96-4bdc-8f77-2392f23e723f" />
<img width="300" height="519" alt="TravelAIAgentResponsive4" src="https://github.com/user-attachments/assets/4faddc2b-a164-4725-8b9f-eba5cb80679a" />
<img width="292" height="519" alt="TravelAIAgentResponsive5" src="https://github.com/user-attachments/assets/500c9c1b-2a2a-4936-9898-aec138d6933c" />
<img width="296" height="513" alt="TravelAIAgentResponsive6" src="https://github.com/user-attachments/assets/796479f3-3348-4756-a177-bae045cefe60" />
<img width="293" height="517" alt="TravelAIAgentResponsive7" src="https://github.com/user-attachments/assets/3ca2f556-32a7-4e7e-8e94-40e78171defe" />
<img width="294" height="519" alt="TravelAIAgentResponsive8" src="https://github.com/user-attachments/assets/2f83204c-d261-4d31-8b4c-1a2948ceaf6d" />




