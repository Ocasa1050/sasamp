//
// Created on 18.09.2023.
//

#include "GameFilesCheck.h"
#include "raknet/BitStream.h"
#include "net/netgame.h"

namespace {
void SendGameFilesCheckResult(bool ok) {
    if (pNetGame == nullptr || pNetGame->GetRakClient() == nullptr) {
        return;
    }

    RakNet::BitStream bsSend;
    bsSend.Write((uint8_t) ID_CUSTOM_RPC);
    bsSend.Write((uint8_t) RPC_REQUEST_CHECK_FILES);
    bsSend.Write((uint8_t) ok);

    pNetGame->GetRakClient()->Send(
            &bsSend,
            MEDIUM_PRIORITY,
            RELIABLE_SEQUENCED,
            0
    );
}
}

void CGameFilesCheck::RequestChecked() {
    // File downloading/checking is temporarily disabled. Reply directly so
    // the server can continue without entering the Java callback path.
    SendGameFilesCheckResult(true);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_russia_game_core_Samp_00024Companion_gameFilesChecked(JNIEnv *env, jobject clazz, jboolean ok) {
    SendGameFilesCheckResult(ok == JNI_TRUE);
}
