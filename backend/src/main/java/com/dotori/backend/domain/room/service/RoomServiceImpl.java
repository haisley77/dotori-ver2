package com.dotori.backend.domain.room.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import com.dotori.backend.domain.room.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.dotori.backend.domain.book.model.entity.Book;
import com.dotori.backend.domain.book.repository.BookRepository;
import com.dotori.backend.domain.member.model.entity.Member;
import com.dotori.backend.domain.member.repository.MemberRepository;
import com.dotori.backend.domain.room.model.entity.Room;
import com.dotori.backend.domain.room.model.entity.RoomMember;
import com.dotori.backend.domain.room.repository.RoomMemberRepository;
import com.dotori.backend.domain.room.repository.RoomRepository;

import io.openvidu.java.client.Connection;
import io.openvidu.java.client.ConnectionProperties;
import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.openvidu.java.client.Session;
import io.openvidu.java.client.SessionProperties;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    @Override
    public RoomCreationResponseDto createRoom(OpenVidu openvidu, RoomCreationRequestDto params) {

        // 책을 조회한다.
        Book book = bookRepository.findById(params.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("해당하는 책 정보 없음"));

        // openvidu를 최신화한다.
        updateOpenvidu(openvidu);

        Session session = null;
        Connection connection = null;
        try {
            // 세션을 생성한다.
            session = openvidu.createSession(SessionProperties.fromJson(params.getSessionProperties()).build());
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException("openvidu 세션 생성 중 문제 발생");
        }

        try {
            // 커넥션을 생성한다.
            connection = session.createConnection(ConnectionProperties.fromJson(params.getConnectionProperties()).build());
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException("openvidu 커넥션 생성 중 문제 발생");
        }

        // 대기방을 생성한다.
        Room room = Room.create(params,book, session.getSessionId());
        Room savedRoom = roomRepository.save(room);

        // roomId와 token을 반환한다.
        RoomCreationResponseDto roomCreationResponseDto = new RoomCreationResponseDto(savedRoom.getRoomId(),connection.getToken());

        return roomCreationResponseDto;
    }

    @Override
    public Session findSessionByRoomId(OpenVidu openvidu, Long roomId) {

        // openvidu를 최신화한다.
        updateOpenvidu(openvidu);

        // 방을 조회한다.
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException("해당하는 방을 찾을 수 없습니다.")
        );

        return openvidu.getActiveSession(room.getSessionId());
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAllByOrderByIsRecordingAscCreatedAtDesc()
                .orElseThrow(() -> new EntityNotFoundException("방 조회 중 문제 발생")
        );
    }

    @Override
    public String createConnection(OpenVidu openvidu, Session session,
                                   Map<String, Object> connectionProperties) throws OpenViduJavaClientException, OpenViduHttpException {
        // 방 참여자는 세션과 커넥션을 생성합니다.
        Connection connection = session.createConnection(
                ConnectionProperties.fromJson(connectionProperties).build());
        if (connection == null)
            throw new RuntimeException("토큰 생성 중 문제 발생");
        // 토큰을 반환합니다.
        return connection.getToken();
    }

    private void checkJoinPossible(OpenVidu openvidu, Room room) {

        // 방에 연결된 유효한 connection 리스트를 openvidu 서버에서 불러온다.
        List<Connection> activeConnections = openvidu.getActiveSession(room.getSessionId()).getActiveConnections();

        if (activeConnections.size() >= room.getLimitCnt()) {
            throw new RuntimeException("인원 초과로 참여 불가");
        }

        if (room.getJoinCnt() >= room.getLimitCnt()) {
            throw new RuntimeException("인원 초과로 참여 불가");
        }

    }

    @Override
    public void joinMemberToRoom(OpenVidu openvidu, Long roomId, Long memberId, Long bookId) {

        // 방을 조회한다.
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당하는 방을 찾을 수 없습니다."));

        // 참여 가능 방인지 확인한다.
        checkJoinPossible(openvidu, room);

        // openvidu를 최신화한다.
        updateOpenvidu(openvidu);

        // 멤버를 방에 추가한다.
        Member member = memberRepository.findById(memberId).get();
        RoomMember roomMember = new RoomMember(member, room);
        roomMemberRepository.save(roomMember);

        // 참여 인원을 갱신한다.
        Room.updateJoinCnt(room, room.getJoinCnt() + 1);

    }

    @Override
    public RoomRemovalResponseDto removeMemberFromRoom(OpenVidu openvidu, Long roomId, Long memberId) {

        // openvidu를 최신화한다.
        updateOpenvidu(openvidu);

        // 방 참여 멤버를 DB에서 지운다.
        RoomMember roomMember = roomMemberRepository.findByRoomRoomIdAndMemberMemberId(roomId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당하는 방 참여자를 찾을 수 없습니다.")
        );
        roomMemberRepository.delete(roomMember);

        // 방을 조회한다.
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException("해당하는 방을 찾을 수 없습니다.")
        );

        // 방 인원을 갱신한다.
        Room.updateJoinCnt(room, room.getJoinCnt() - 1);

        // 방에 남은 인원이 없으면 제거한다.
        if (room.getJoinCnt() == 0) {
            roomRepository.delete(room);
        }

        RoomRemovalResponseDto roomRemovalResponseDto = new RoomRemovalResponseDto(memberId);
        return roomRemovalResponseDto;
    }

    @Override
    public void updateRoom(Long roomId, RoomResponseDto roomInfo) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException("해당하는 방이 존재하지 않습니다.")
        );
        room.setIsRecording(true);
        roomRepository.save(room);
    }

    @Override
    public Room getRoom(Long roomId) {
        return roomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException(("해당하는 방이 존재하지 않습니다."))
        );
    }

    @Override
    public void removeExpiredRooms(OpenVidu openvidu) {

        // openvidu를 최신화한다.
        updateOpenvidu(openvidu);

        List<Session> activeSessions = openvidu.getActiveSessions();

        List<String> activeSessionIdList = activeSessions.stream()
                .map(Session::getSessionId)
                .collect(Collectors.toList());

        roomRepository.deleteAllBySessionIdNotIn(activeSessionIdList);
    }

    private void updateOpenvidu(OpenVidu openvidu) {
        try {
            openvidu.fetch();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException(e);
        }
    }

}