package com.biszku.BloggingPlatformAPI;

import java.util.List;

public record PostToSave(String title, String content, String category, List<String> tags) {}
