package com.dotori.backend.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Member
    MEMBER_NOT_FOUND("M-001", "Member Not Found"),
    PROFILE_IMG_NOT_UPDATED("M-002", "Profile Image Not Updated"),
    ACCESS_TOKEN_NOT_FOUND("M-003", "AccessToken Not Found"),
    ACCESS_TOKEN_INVALID("M-004", "AccessToken Invalid"),
    ACCESS_TOKEN_EXPIRED("M-005", "AccessToken Expired"),
    REFRESH_TOKEN_NOT_FOUND("M-006", "RefreshToken Not Found"),
    REFRESH_TOKEN_INVALID("M-007", "RefreshToken Invalid"),
    REFRESH_TOKEN_EXPIRED("M-008", "RefreshToken Expired"),
    EMAIL_NOT_FOUND("M-009", "Member Email Not Found"),
    ROLE_NOT_FOUND("M-010", "Member Role Not Found"),
    COOKIE_NOT_FOUND("M-011", "Http Cookie Not Found"),
    SESSION_OUT("M-012", "Login Session Out"),

    // Room
    ROOM_NOT_FOUND("R-001", "Room Not Found"),
    ROOM_NOT_AVAILABLE("R-002", "Room Not Available"),
    ROOM_MEMBER_NOT_FOUND("R-003", "Room Member Not Found"),

    // Book
    BOOK_NOT_FOUND("B-001", "Book Not Found"),
    SCENE_NOT_FOUND("B-001", "Scene Not Found"),

    // Openvidu
    OPENVIDU_CONNECTION_NOT_CREATED("O-001", "Openvidu Connection Not Created"),
    OPENVIDU_SESSION_NOT_CREATED("O-002", "Openvidu Session Not Created"),
    OPENVIDU_NOT_FETCHED("O-003", "Openvidu Not Fetched"),

    // Video
    CHUNK_FILES_NOT_UPLOADED("V-001", "Chunk Files Not Upload"),
    CHUNK_FILES_NOT_MERGED("V-002", "Chunk Files Not Merged"),
    VIDEO_NOT_MERGED("V-003", "Video Not Merged"),
    VIDEO_NOT_FOUND("V-004", "Video Not Found");

    private final String code;
    private final String message;

    ErrorCode(final String code, final String message) {
        this.code = code;
        this.message = message;
    }
}
