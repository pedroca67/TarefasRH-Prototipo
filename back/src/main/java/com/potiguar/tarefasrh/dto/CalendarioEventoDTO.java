package com.potiguar.tarefasrh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarioEventoDTO {
    private Long id;
    private String title;
    private String start;
    private String color;
    private String status;
    private String url;
}
