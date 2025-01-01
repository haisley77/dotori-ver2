package com.dotori.backend.domain.room.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class RoomMemberJoinRequestDto {
    private final Long roomId;
    private final Long memberId;
    private final Long bookId;
}
