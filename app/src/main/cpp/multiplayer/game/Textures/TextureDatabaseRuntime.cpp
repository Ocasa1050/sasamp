//
// Created on 11.01.2023.
//

#include "TextureDatabaseRuntime.h"
#include "../../main.h"
#include "util/patch.h"

namespace {
TextureDatabaseFormat gPreferredTextureDatabaseFormat = TextureDatabaseFormat::DF_Default;
}

void SetPreferredTextureDatabaseFormat(TextureDatabaseFormat format) {
    gPreferredTextureDatabaseFormat = format;
    Log("Texture database format selected: %s", GetTextureDatabaseFormatName(format));
}

TextureDatabaseFormat GetPreferredTextureDatabaseFormat() {
    return gPreferredTextureDatabaseFormat;
}

const char* GetTextureDatabaseFormatName(TextureDatabaseFormat format) {
    switch (format) {
        case TextureDatabaseFormat::DF_DXT:
            return "DXT";
        case TextureDatabaseFormat::DF_ETC:
            return "ETC";
        case TextureDatabaseFormat::DF_PVR:
            return "PVR";
        case TextureDatabaseFormat::DF_Default:
            return "Default";
        default:
            return "Unknown";
    }
}

TextureDatabaseRuntime* TextureDatabaseRuntime::Load(const char *withName, bool fullyLoad, TextureDatabaseFormat forcedFormat)
{
    if (forcedFormat == TextureDatabaseFormat::DF_Default &&
        gPreferredTextureDatabaseFormat != TextureDatabaseFormat::DF_Default) {
        forcedFormat = gPreferredTextureDatabaseFormat;
    }

    Log("Loading texture database: name=%s format=%s", withName, GetTextureDatabaseFormatName(forcedFormat));
    auto *database = CHook::CallFunction<TextureDatabaseRuntime*>(
        g_libGTASA + (VER_x32 ? 0x001EA864 + 1 : 0x28771C),
        withName,
        fullyLoad,
        forcedFormat
    );
    Log("Texture database result: name=%s status=%s",
        withName,
        database != nullptr ? "loaded" : "FAILED");
    return database;
}

void TextureDatabaseRuntime::Register(TextureDatabaseRuntime *toRegister) {
    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x1E9B48 + 1 : 0x2865D8), toRegister);
//    if (std::find(registered.dataPtr, registered.dataPtr + registered.numEntries, toRegister) != registered.dataPtr + registered.numEntries) {
//        return; // ��� ���������������, �������
//    }
//
//    if (registered.numAlloced < registered.numEntries + 1) {
//        size_t newAlloc = ((3 * (registered.numEntries + 1)) >> 1) + 3;
//        TextureDatabaseRuntime** newData = static_cast<TextureDatabaseRuntime**>(malloc(sizeof(TextureDatabaseRuntime*) * newAlloc));
//
//        if (registered.dataPtr) {
//            std::memcpy(newData, registered.dataPtr, sizeof(TextureDatabaseRuntime*) * registered.numEntries);
//            free(registered.dataPtr);
//        }
//
//        registered.dataPtr = newData;
//        registered.numAlloced = newAlloc;
//    }
//
//    registered.dataPtr[registered.numEntries++] = toRegister;
}

void TextureDatabaseRuntime::Unregister(TextureDatabaseRuntime *toUnregister) {
    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x1E9C00 + 1 : 0x2866A4), toUnregister);
}

RwTexture* TextureDatabaseRuntime::GetTexture(const char *name) {
    return ((RwTexture*(*)(const char*))(g_libGTASA + (VER_x32 ? 0x001E9C64 + 1 : 0x286718)))(name);
}

void TextureDatabaseRuntime::UpdateStreaming(float deltaTime, bool flush) {
    CHook::CallFunction<void>(g_libGTASA + (VER_x32 ? 0x1E9518 + 1 : 0x285DBC), deltaTime, flush);
}

TextureDatabaseRuntime* TextureDatabaseRuntime::GetDatabase(const char *dbName) {
    return CHook::CallFunction<TextureDatabaseRuntime*>(g_libGTASA + (VER_x32 ? 0x1EAC0C + 1 : 0x287AF4), dbName);
//    for (unsigned int i = 0; i < TextureDatabaseRuntime::loaded.numEntries; ++i) {
//        TextureDatabaseRuntime *currentDatabase = TextureDatabaseRuntime::loaded.dataPtr[i];
//        if (strcmp(currentDatabase->name, dbName) == 0) {
//            return currentDatabase;
//        }
//    }
//
//    return nullptr;
}

void TextureDatabaseRuntime::InjectHooks() {
//    CHook::Write(g_libGTASA + (VER_x32 ? 0x675FCC : 0x84A020), &detailTextures);
//    CHook::Write(g_libGTASA + (VER_x32 ? 0x676720 : 0x84AEB0), &storedTexels);
//    CHook::Write(g_libGTASA + (VER_x32 ? 0x679768 : 0x850EE8), &loaded);
//    CHook::Write(g_libGTASA + (VER_x32 ? 0x679E70 : 0x851CF8), &registered);
}

RwBool TextureAnnihilate(RwTexture *texture) {
    return CHook::CallFunction<RwBool>(g_libGTASA + (VER_x32 ? 0x1DB33C + 1 : 0x273A1C), texture);
}
