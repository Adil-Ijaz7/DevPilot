# DevPilot - AI Pair Programmer

DevPilot is a sophisticated Java Swing application designed to act as your AI-powered coding assistant. It leverages the OpenRouter API to provide real-time code analysis, debugging, refactoring, and test generation.

## Features

*   **AI Code Analysis**: Connects to advanced LLMs (via OpenRouter) to understand and improve your code.
*   **Multiple Modes**:
    *   Explain Code
    *   Find Bugs
    *   Refactor Code
    *   Add Comments
    *   Generate Unit Tests
*   **Modern UI**: Built with Java Swing and styled with FlatLaf for a clean, modern look (Light Theme).
*   **History Tracking**: Maintains a session history of your queries and AI responses using efficient data structures.
*   **Model Selection**: Choose from various AI models like Mistral, GPT-4o-mini, OpenChat, Gemini, and Grok.

## Prerequisites

*   Java 21 or higher.
*   `flatlaf-3.2.jar` (Included in the project).
*   OpenRouter API Key.

## Setup & Run

1.  **Set API Key**:
    Set the `OPENROUTER_API_KEY` environment variable on your system.
    ```powershell
    $env:OPENROUTER_API_KEY = "your_key_here"
    ```

2.  **Run the Application**:
    Use the provided PowerShell script to compile and run:
    ```powershell
    .\run.ps1
    ```

## Technologies Used

*   Java Swing
*   FlatLaf (UI Library)
*   Java HTTP Client
*   OpenRouter API

---
*Developed for DSA Project Assignment.*
