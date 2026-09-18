cmake_minimum_required(VERSION 3.16)

project(pastebin LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Used by the FROM-scratch image: link Drogon, its dependencies and libc
# statically so the binary runs with no shared libraries at all.
option(STATIC_BUILD "Link all dependencies and libc statically" OFF)

if(STATIC_BUILD)
  # CMake's platform files set CMAKE_FIND_LIBRARY_SUFFIXES as a normal variable,
  # so overriding it from the command line (-D...) has no effect. It must be
  # (re)set here, before find_package(Drogon), to make the dependencies that
  # Drogon re-resolves in this project prefer the .a archives.
  set(CMAKE_FIND_LIBRARY_SUFFIXES ".a")
  set(OPENSSL_USE_STATIC_LIBS TRUE)
  set(ZLIB_USE_STATIC_LIBS TRUE)
  set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static")
endif()

find_package(Drogon CONFIG REQUIRED)

add_executable(pastebin src/main.cc)
target_link_libraries(pastebin PRIVATE Drogon::Drogon)
