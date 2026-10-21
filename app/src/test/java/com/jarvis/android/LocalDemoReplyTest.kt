package com.jarvis.android

import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDemoReplyTest {
    @Test
    fun greetingGetsAssistantResponse() {
        assertTrue(localDemoReply("hello").contains("Hello"))
    }

    @Test
    fun unknownPromptExplainsDemoMode() {
        assertTrue(localDemoReply("tell me a story").contains("demo mode"))
    }
}
