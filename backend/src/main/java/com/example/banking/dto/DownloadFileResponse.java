package com.example.banking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadFileResponse {
    private String fileName;
    private String mimeType;
    private String content;
}
