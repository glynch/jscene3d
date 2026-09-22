NATIVE_DIR := native
BRIDGE_SOURCE := src/main/java/io/github/glynch/jscene3d/iosurface/macos/IOSurfaceBridge.java
DESCRIPTOR_SOURCE := src/main/java/io/github/glynch/jscene3d/iosurface/macos/IOSurfaceDescriptor.java
JAVA_HOME ?= $(shell /usr/libexec/java_home -v 21)

.PHONY: all native classpath

all: native classpath

native:
	javac -h $(NATIVE_DIR) -d target/classes $(BRIDGE_SOURCE) $(DESCRIPTOR_SOURCE)
	clang -dynamiclib \
		-I"$(JAVA_HOME)/include" \
		-I"$(JAVA_HOME)/include/darwin" \
		-I$(NATIVE_DIR) \
		-framework IOSurface \
		-framework CoreFoundation \
		-framework OpenGL \
		$(NATIVE_DIR)/iosurface_bridge.c \
		$(NATIVE_DIR)/jscene3d_rendezvous.c \
		$(NATIVE_DIR)/jscene3d_control_channel.c \
		$(NATIVE_DIR)/jscene3d_iosurface.c \
		-o $(NATIVE_DIR)/libiosurface_bridge.dylib

classpath:
	../mvnw -q dependency:build-classpath -Dmdep.outputFile=classpath.txt
