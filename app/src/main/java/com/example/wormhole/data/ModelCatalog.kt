package com.example.wormhole.data

object ModelCatalog {

    val availableModels = listOf(
        ModelInfo(
            id = "gemma-3n-2b-it-int4",
            name = "Gemma 3n 2B (Quantized Int4)",
            family = "Gemma 3n",
            parameterSize = "2B",
            quantization = "Int4",
            fileSizeBytes = 1_350_000_000L, // 1.35 GB
            ramRequiredMb = 2200,
            downloadUrl = "https://huggingface.co/google/gemma-3n-2b-it-litert/resolve/main/model.task",
            estimatedSpeedTokensSec = 22.4f,
            isRecommended = true,
            isDownloaded = true // Simulated pre-installed active default for instant testability
        ),
        ModelInfo(
            id = "gemma-2-2b-it-int8",
            name = "Gemma 2 2B (Quantized Int8)",
            family = "Gemma 2",
            parameterSize = "2B",
            quantization = "Int8",
            fileSizeBytes = 2_150_000_000L, // 2.15 GB
            ramRequiredMb = 3400,
            downloadUrl = "https://huggingface.co/google/gemma-2-2b-it-litert/resolve/main/model.task",
            estimatedSpeedTokensSec = 17.8f,
            isRecommended = false,
            isDownloaded = false
        ),
        ModelInfo(
            id = "smollm-1.7b-instruct-int4",
            name = "SmolLM 1.7B Instruct (Int4)",
            family = "SmolLM",
            parameterSize = "1.7B",
            quantization = "Int4",
            fileSizeBytes = 980_000_000L, // 980 MB
            ramRequiredMb = 1600,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM-1.7B-Instruct-LiteRT/resolve/main/model.task",
            estimatedSpeedTokensSec = 28.5f,
            isRecommended = false,
            isDownloaded = false
        ),
        ModelInfo(
            id = "function-gemma-2b-action",
            name = "FunctionGemma 2B (Agent & Tools)",
            family = "Gemma Action",
            parameterSize = "2B",
            quantization = "Int4",
            fileSizeBytes = 1_420_000_000L,
            ramRequiredMb = 2400,
            downloadUrl = "https://huggingface.co/google/function-gemma-2b-litert/resolve/main/model.task",
            estimatedSpeedTokensSec = 21.0f,
            isRecommended = false,
            isDownloaded = false
        )
    )

    enum class ComputeBackend {
        GPU,
        CPU,
        NPU_HEXAGON
    }
}
