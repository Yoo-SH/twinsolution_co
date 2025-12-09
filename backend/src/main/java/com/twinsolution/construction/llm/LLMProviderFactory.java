package com.twinsolution.construction.llm;

import com.twinsolution.construction.entity.Project;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * LLM Provider Factory 인터페이스
 * 프로젝트 설정 또는 직접 지정한 provider/modelName에 따라 적절한 LLM 모델을 반환합니다.
 */
public interface LLMProviderFactory {

    /**
     * 프로젝트 설정에 맞는 ChatLanguageModel을 반환합니다.
     * @param project 프로젝트 엔티티
     * @return ChatLanguageModel 인스턴스
     */
    ChatLanguageModel getChatModel(Project project);

    /**
     * 직접 지정한 provider와 modelName에 맞는 ChatLanguageModel을 반환합니다.
     * @param llmProvider LLM 제공자 (OPENAI, OLLAMA)
     * @param modelName 모델 이름
     * @return ChatLanguageModel 인스턴스
     */
    ChatLanguageModel getChatModel(String llmProvider, String modelName);

    /**
     * 프로젝트 설정에 맞는 StreamingChatLanguageModel을 반환합니다.
     * @param project 프로젝트 엔티티
     * @return StreamingChatLanguageModel 인스턴스
     */
    StreamingChatLanguageModel getStreamingChatModel(Project project);

    /**
     * 직접 지정한 provider와 modelName에 맞는 StreamingChatLanguageModel을 반환합니다.
     * @param llmProvider LLM 제공자 (OPENAI, OLLAMA)
     * @param modelName 모델 이름
     * @return StreamingChatLanguageModel 인스턴스
     */
    StreamingChatLanguageModel getStreamingChatModel(String llmProvider, String modelName);

    /**
     * Ollama 서비스 가용 여부를 확인합니다.
     * @return Ollama 사용 가능 여부
     */
    boolean isOllamaAvailable();
}
