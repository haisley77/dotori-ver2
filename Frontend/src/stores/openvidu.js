import {onMounted, ref} from 'vue';
import {defineStore} from 'pinia';
import {OpenVidu} from 'openvidu-browser';

import {localAxios} from '../axios/http-commons';
import {useRouter} from 'vue-router';


const router = useRouter();
const axios = localAxios();
export const useOpenViduStore
  = defineStore('openViduStore', () => {


  const OV = new OpenVidu();
  const session = OV.initSession();
  const ovToken = ref(null);

  const apiRootPath = '/api/rooms';

  const roomId = ref(0);
  const memberId = ref(0);

  const isLoggedIn = ref(false);
  const subscribers = ref([]);
  const mainStreamManager = ref();
  var mainStreamManagerReal = null;
  const isPublished = ref(false);

  const isHost = ref(false);
  const onAir = ref(0);

  const minRole = ref();
  const canvasStream = ref();

  const changeCanvasStream = (stream) => {
    canvasStream.value = stream;
  }

  // 커넥션 설정 정보
  const connection_properties = ref({});

  const memberInfo = ref({
    memberId: 0,
    nickName: '',
    email: '',
    profileImg: null,
  });

  // 방 설정 정보
  const roomInfo = ref({
    hostId: 0,
    title: null,
    password: null,
    isRecording: false,
    joinCnt: 0,
    limitCnt: 0,
    isPublic: true,
  })

  // 책, 역할 설정 정보
  const bookDetail = ref({
    book: {},
    roles: [],
    scenes: [],
  })

  const roomInitializationParam = ref({
    sessionProperties: null,
    connectionProperties: null,
    roomInfo: null,
    bookInfo: null,
  })

  const roomCreationParam = {
    sessionProperties: null,
    connectionProperties: null,
    bookId: null,
    hostId: null,
    title: null,
    password: null,
    isPublic: null,
    limitCnt: null,
  }

  const roomMemberJoinParam = {
    roomId: null,
    memberId: null,
    bookId: null,
  }

  const roomMemberRemoveParam = {
    roomId: null,
    memberId: null,
  }

  const roomConnectionParam = {
    roomId: null,
    connectionProperties: null,
  }

  const roomUpdateParam = {
    roomId: null,
  }

  const createRoom = (bookmodal) => {
    return new Promise((resolve, reject) => {
      const apiPath = apiRootPath + '/session';
      roomCreationParam.bookId = bookmodal.bookId;
      roomCreationParam.hostId = roomInfo.value.hostId;
      roomCreationParam.title = roomInfo.value.title;
      roomCreationParam.password = roomInfo.value.password;
      roomCreationParam.isPublic = roomInfo.value.isPublic;
      roomCreationParam.limitCnt = roomInfo.value.limitCnt;

      axios.post(apiPath, roomCreationParam,{withCredentials: true})
        .then((response) => {
          roomId.value = response.data.roomId;
          ovToken.value = response.data.token;
          resolve(response.data);
        })
        .catch((error) => {
          console.error('방 생성 실패 : ' + error.response);
          reject(error);
        });
    });
  };

  const getConnectionToken = (room) => {  //방에 입장할 때 사용되는 코드
    return new Promise((resolve, reject) => {

      const apiPath = apiRootPath + `/connection`;

      roomConnectionParam.roomId = room.roomId;

      axios.post(apiPath, roomConnectionParam,{withCredentials: true})
        .then((response) => {
          roomId.value = response.data.roomId;
          ovToken.value = response.data.token;

          roomInfo.hostId = room.hostId;
          isHost.value = false;

          resolve(response.data);
        })
        .catch((error) => {
          console.error(error.response);
          reject(error);
        });
    });
  };

  const joinRoomMember = (book) => {
    return new Promise((resolve, reject) => {
      const apiPath = apiRootPath + `/join-member`;

      roomMemberJoinParam.roomId = roomId.value;
      roomMemberJoinParam.memberId = memberId.value;
      roomMemberJoinParam.bookId = book.bookId;

      axios.post(apiPath, roomMemberJoinParam, {withCredentials: true})
        .then((response) => {
          bookDetail.value = response.data.bookInfo;
          minRole.value = bookDetail.value.roles[0].roleId;
          resolve(response.data);
        })
        .catch((error) => {
          console.error('방 참여 정보 갱신 처리 중 오류 발생 : ', error.response);
          reject(error);
        });
    });
  };

  const removeRoomMember = () => {
    return new Promise((resolve, reject) => {
      const apiPath = apiRootPath + `/remove-member`;

      roomMemberRemoveParam.roomId = roomId.value;
      roomMemberRemoveParam.memberId = memberId.value;

      axios.delete(apiPath,{
        data: roomMemberRemoveParam,
        withCredentials: true
      })
        .then((response) => {
          resolve(response.data);
        })
        .catch((error) => {
          console.error('방 나가기 정보 갱신 처리 중 오류 발생 : ', error.response);
          reject(error);
        });
    });
  };

  const updateRoom = (isRecording) => {
    return new Promise((resolve, reject) => {
      const apiPath = apiRootPath + `/update-room`;

      roomUpdateParam.roomId = roomId.value;

      axios.patch(apiPath, roomUpdateParam, {withCredentials: true})
        .then((response) => {
          roomId.value = response.data.roomId;
          roomInfo.value.isRecording = isRecording;
          resolve(response.data);
        })
        .catch((error) => {
          console.error('방 정보 업데이트 처리 중 문제 발생 : ', error.response);
          reject(error);
        });
    });
  };


  const connectToOpenVidu = () => {
    return new Promise((resolve, reject) => {
      //새로운 스트림이 생기면 그 스트림에 구독한다
      session.on('streamCreated', ({stream}) => {
        const subscriber = session.subscribe(stream, stream.streamId);
        subscribers.value.push(subscriber);
      });
      //스트림이 사라지면 그 스트림은 구독을 취소한다
      session.on('streamDestroyed', ({stream}) => {
        const index = subscribers.value.indexOf(stream.streamManager, 0);
        if (index >= 0) {
          subscribers.value.splice(index, 1);
        }
      });

      session.connect(ovToken.value)
        .then(() => {
          console.log(ovToken.value);
          resolve();
        })
        .catch((error) => {
          console.error('ov와 연결 실패 : ', error);
          reject(error);
        });
    });
  };
  const publish = (publisher) => {
    return new Promise((resolve, reject) => {
      session.publish(publisher).then(() => {
        mainStreamManager.value = publisher;
        mainStreamManagerReal = publisher;
        console.log('published my video!');
        isPublished.value = true;
        resolve();
      }).catch((error) => {
        // isPublished.value = true;
        console.log(error);
        reject(error);
      });
    });
  };

  const unpublish = () => {
    console.log(mainStreamManager.value.streamId);
    session.unpublish(mainStreamManagerReal).then(() => {
      console.log('unpublished my video!!');
      isPublished.value = false;
      mainStreamManager.value = null;
      mainStreamManagerReal = null;
    }).catch((error) => {
      // isPublished.value = false;
      console.log(session.connection);
      console.log(mainStreamManager.value.stream.connection);
      console.log('unpblish failed!' + error);
    });

  };

  const playerList = ref([]);
  const myRole = ref();
  // import {localAxios} from 'src/axios/http-commons';
  // const axios = localAxios();
  const checkAuthStatus = () => {
    console.log('isLoggedIn? : ' + isLoggedIn);
    axios.get('/api/members/status', {withCredentials: true}).then(
      (response) => {
        //로그인 된 상태를 확인하고 저장한다
        isLoggedIn.value = response.data.isAuthenticated;
        if (isLoggedIn.value !== false) {
          console.log("로그인 되어있음!");
          axios.get('/api/members/detail', {withCredentials: true})
            .then((response) => {
              //회원정보를 저장한다
              console.log('회원 정보 조회 성공!');
              console.log(response)
              memberInfo.value = response.data;
              console.log(memberInfo.value);
              memberId.value = response.data.memberId;
              sessionStorage.setItem('memberId', response.data.memberId);
            }).catch((error) => {
            console.log('회원정보 조회 실패' + error);
          });
        }
      },
    ).catch((error) => {
      isLoggedIn.value = false;
      console.log('로그인 확인 실패 : ' + error);
    });
  };

  onMounted(() => {
    console.log( "!! : " + document.cookie);
    checkAuthStatus();
  });
  return {
    roomInfo,
    roomId,
    memberId,
    isHost,
    session,
    playerList,
    ovToken,
    bookDetail,
    roomInitializationParam,
    createRoom,
    connectToOpenVidu,
    joinRoomMember,
    updateRoom,
    publish,
    subscribers, mainStreamManager, OV,
    getConnectionToken,
    removeRoomMember,
    onAir,
    unpublish,
    isPublished,
    myRole, minRole, canvasStream, changeCanvasStream, isLoggedIn, memberInfo,
  };
}, {persist: {storage: sessionStorage}});
