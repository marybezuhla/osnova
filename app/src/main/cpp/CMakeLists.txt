cmake_minimum_required(VERSION 3.22.1)
project(osnova_speech C)

# eSpeak NG for the English voice's phonemes: a static library prebuilt from source at a fixed release in Hramota
# (its espeak.yml; see /espeak/VERSION), checked against /espeak/SHA256SUMS by CI.
# The same library and bridge as in Hramota.
set(ESPEAK_DIR ${CMAKE_SOURCE_DIR}/../../../../espeak)
file(GLOB ESPEAK_LIBS ${ESPEAK_DIR}/lib/${ANDROID_ABI}/*.a)
add_library(osnova_speech SHARED espeak_jni.c)
target_include_directories(osnova_speech PRIVATE ${ESPEAK_DIR}/include)
target_compile_options(osnova_speech PRIVATE -Wall -Wextra -Werror -O2 -fstack-protector-strong -D_FORTIFY_SOURCE=2)
# (eSpeak's own functions are not exported: only the two JNI calls are)
target_link_options(osnova_speech PRIVATE -Wl,-z,relro,-z,now -Wl,-z,max-page-size=16384 -Wl,--exclude-libs,ALL)
# (the libraries in a group: eSpeak's own parts refer to each other)
target_link_libraries(osnova_speech -Wl,--start-group ${ESPEAK_LIBS} -Wl,--end-group)
