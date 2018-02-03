

cmake_minimum_required(VERSION 3.4.1)
set(distribution_DIR ${CMAKE_SOURCE_DIR}/../../../../src/main/jniLibs)

include_directories(src/main/cpp/include)

file(GLOB my_source_path  src/main/cpp/*.cpp )

add_library( # Sets the name of the library.
             Imgprocess

             # Sets the library as a shared library.
             SHARED

             ${my_source_path} )


add_library( lib_opencv SHARED IMPORTED)
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

find_library(jnigraphics-lib
        jnigraphics )

target_link_libraries( # Specifies the target library.
                       Imgprocess

                       # Links the target library to the log library
                       # included in the NDK.
                        ${jnigraphics-lib}
                       ${log-lib}
                        lib_opencv)