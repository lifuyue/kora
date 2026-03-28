package com.lifuyue.kora.feature.chat

import com.lifuyue.kora.core.network.FastGptApi
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentFlowContractTest {
    @Test
    fun attachmentDraftUiModelExistsForComposerState() {
        val modelClass = Class.forName("com.lifuyue.kora.feature.chat.AttachmentDraftUiModel")

        assertTrue(modelClass.declaredFields.any { it.name == "uploadStatus" })
        assertTrue(modelClass.declaredFields.any { it.name == "progress" })
        assertTrue(modelClass.declaredFields.any { it.name == "uploadedRef" })
    }

    @Test
    fun chatUiStateExposesComposerAttachments() {
        assertTrue(ChatUiState::class.java.declaredFields.any { it.name == "attachments" })
        assertTrue(ChatUiState::class.java.declaredMethods.any { it.name == "getCanSend" })
    }

    @Test
    fun fastGptApiExposesChatAttachmentUploadEndpoint() {
        assertTrue(FastGptApi::class.java.methods.any { it.name == "uploadChatAttachment" })
    }
}
