NATIVE_DIR := native
NATIVE_OUTPUT_DIR := target/native
JAVA_HOME ?= $(shell /usr/libexec/java_home -v 21)

.PHONY: all native classpath

all: native classpath

native:
	mkdir -p $(NATIVE_OUTPUT_DIR)
	clang -dynamiclib \
		-I"$(JAVA_HOME)/include" \
		-I"$(JAVA_HOME)/include/darwin" \
		-framework IOSurface \
		-framework CoreFoundation \
		-framework OpenGL \
		$(NATIVE_DIR)/iosurface_bridge.c \
		$(NATIVE_DIR)/jscene3d_rendezvous.c \
		$(NATIVE_DIR)/jscene3d_control_channel.c \
		$(NATIVE_DIR)/jscene3d_iosurface.c \
		-o $(NATIVE_OUTPUT_DIR)/libiosurface_bridge.dylib

classpath:
	../mvnw -q dependency:build-classpath -Dmdep.outputFile=classpath.txt
