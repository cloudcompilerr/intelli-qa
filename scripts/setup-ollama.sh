#!/bin/bash

# Setup script for Ollama with required models
echo "Setting up Ollama with required models..."

# Wait for Ollama to be ready
echo "Waiting for Ollama to start..."
until curl -f http://localhost:11434/api/tags > /dev/null 2>&1; do
    echo "Waiting for Ollama service..."
    sleep 5
done

echo "Ollama is ready. Pulling required models..."

# Pull CodeLlama model for code understanding
echo "Pulling CodeLlama 7B model..."
curl -X POST http://localhost:11434/api/pull -d '{"name": "codellama:7b"}'

# Pull Mistral model as alternative
echo "Pulling Mistral 7B model..."
curl -X POST http://localhost:11434/api/pull -d '{"name": "mistral:7b"}'

echo "Model setup complete!"
echo "Available models:"
curl -s http://localhost:11434/api/tags | jq '.models[].name'