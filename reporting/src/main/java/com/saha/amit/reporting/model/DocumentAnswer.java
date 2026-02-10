package com.saha.amit.reporting.model;

import java.util.List;

public record DocumentAnswer(
        String answer,
        List<SourceChunk> sources
) {}



