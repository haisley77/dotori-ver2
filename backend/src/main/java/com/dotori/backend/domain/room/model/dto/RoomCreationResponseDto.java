package com.dotori.backend.domain.room.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreationResponseDto {
    public Long roomId;
    public String token;
}
