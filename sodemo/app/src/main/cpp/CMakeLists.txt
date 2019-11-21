cmake_minimum_required(VERSION 3.4.1)
add_library(translib SHARED IMPORTED GLOBAL)
set_target_properties(translib PROPERTIES IMPORT_LOCATION ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libtranslib.so)
target_link_libraries(translib)
