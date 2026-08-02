# Please install the following prerequisites (instructions for each follows):
# 	Android OS SDK: http://source.android.com/download
#
# Install and prepare the Android OS SDK ( http://source.android.com/download )
# on Debian or Ubuntu

EXTERNAL_ROOT := $(shell pwd)

DEBUG ?= 0

# No-op command.
NOOP = true

# Android now has 64-bit and 32-bit versions of the NDK for GNU/Linux.  We
# assume that the build platform uses the appropriate version, otherwise the
# user building this will have to manually set NDK_PROCESSOR or NDK_TOOLCHAIN.
CPU := $(shell uname -m)
ifeq ($(CPU),x86_64)
 NDK_PROCESSOR=x86_64
else
 NDK_PROCESSOR=x86
endif

# Android NDK setup
ANDROID_NDK_HOME := $(ANDROID_HOME)/ndk/29.0.14206865
APP_ABI ?= arm64-v8a

# matching NDK revisions are required for reproducible builds
NDK_REVISION := $(shell sed -n 's,^Pkg.Revision *= *\([^ ]*\),\1,p' $(ANDROID_NDK_HOME)/source.properties)
NDK_REQUIRED_REVISION := 29.0.14206865
MIN_NDK_VERSION := 29
NDK_PLATFORM_LEVEL := 24
ifeq ($(shell test $(MIN_NDK_VERSION) -gt $(firstword $(subst ., ,$(NDK_REVISION))); echo $$?),0)
 $(error NDK r$(MIN_NDK_VERSION) or newer required! r$(NDK_REVISION) is installed at $(ANDROID_NDK_HOME).)
endif
ifneq ($(NDK_REQUIRED_REVISION), $(NDK_REVISION))
 $(warning WARNING: NDK $(NDK_REQUIRED_REVISION) required for reproducible builds, $(NDK_REVISION) is installed.)
endif

ifneq ($(filter arm64-v8a, $(APP_ABI)),)
 HOST := aarch64-linux-android
 ALTHOST := $(HOST)
 NDK_ABI := arm64
endif
ifneq ($(filter armeabi-v7a, $(APP_ABI)),)
 HOST := armv7a-linux-androideabi
 ALTHOST := arm-linux-androideabi
 NDK_ABI := arm
endif
ifneq ($(filter x86, $(APP_ABI)),)
 HOST := i686-linux-android
 ALTHOST := $(HOST)
 NDK_ABI := x86
endif
ifneq ($(filter x86_64, $(APP_ABI)),)
 HOST := x86_64-linux-android
 ALTHOST := $(HOST)
 NDK_ABI := x86_64
endif

NDK_UNAME := $(shell uname -s | tr '[A-Z]' '[a-z]')
NDK_TOOLCHAIN_BASE=$(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)

export CC := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)$(NDK_PLATFORM_LEVEL)-clang
export AR := $(NDK_TOOLCHAIN_BASE)/bin/llvm-ar
export LD := $(NDK_TOOLCHAIN_BASE)/bin/ld
export RANLIB := $(NDK_TOOLCHAIN_BASE)/bin/llvm-ranlib
export STRIP := $(NDK_TOOLCHAIN_BASE)/bin/llvm-strip

export TZ := UTC
export LC_ALL := C.UTF-8
export SOURCE_DATE_EPOCH := 1234567890

# PIEFLAGS for SDK 16/Android L must be set to -fPIE -pie, but can override for earlier targets
PIECFLAGS ?= -fPIE -fPIC
PIELDFLAGS ?= -pie

REPRODUCIBLE_CFLAGS := \
  -fbuild-session-timestamp=$(SOURCE_DATE_EPOCH) \
  -fdebug-compilation-dir . \
  -no-canonical-prefixes \

ALL_CFLAGS := $(REPRODUCIBLE_CFLAGS) $(PIECFLAGS) $(CFLAGS) -Os -DANDROID
ALL_LDFLAGS := $(PIELDFLAGS) $(LDFLAGS)

export CFLAGS := $(ALL_CFLAGS)
export LDFLAGS := $(ALL_LDFLAGS)

.PHONY = clean showsetup \
	assets assets-clean \
	openssl openssl-clean \
	libevent libevent-clean \
	iptables iptables-clean \
	lzma lzma-clean \
	zstd zstd-clean \
	zlib zlib-clean \
	tor tor-clean

all: assets

#------------------------------------------------------------------------------#
# openssl

PATH := $(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)/bin:$(PATH)

# openssl3.0.x looks for NDK with the word root instead of home in the var name
export ANDROID_NDK_ROOT=$(ANDROID_NDK_HOME)

openssl/Makefile: openssl/Configure $(wildcard openssl/Configurations/*.*)
	cd openssl && PATH=$(PATH) && CFLAGS="$(CFLAGS) -Wno-macro-redefined" \
		./Configure \
				no-shared no-ssl3 no-tls1 no-tls1_1 \
				no-ocsp no-nextprotoneg no-psk no-srp no-ts \
				no-cmp no-cms no-ct no-dgram no-dtls no-http \
				no-quic no-srtp no-argon2 no-aria no-bf \
				no-blake2 no-camellia no-cast no-des no-dsa \
				no-gost no-idea no-integrity-only-ciphers no-md2 \
				no-md4 no-mdc2 no-ml-dsa no-ocb no-rc2 no-rc4 \
				no-rc5 no-rmd160 no-scrypt no-seed no-siphash \
				no-slh-dsa no-sm2 no-sm3 no-sm4 no-whirlpool \
				no-ec2m no-ssl-trace no-tls-deprecated-ec \
				no-siv no-tests no-padlockeng no-ui-console \
				no-apps no-async no-comp no-ktls no-legacy no-zlib \
				android-$(NDK_ABI) \
				-D__ANDROID_API__=$(NDK_PLATFORM_LEVEL) \
				--prefix=/ \
				--openssldir=/

openssl-build-stamp: openssl/Makefile
ifeq ($(V), 0)
	PATH=$(PATH) make -j`nproc` --silent -C openssl install_dev DESTDIR=$(EXTERNAL_ROOT) ECHO=':'
else
	PATH=$(PATH) make -j`nproc` -C openssl install_dev DESTDIR=$(EXTERNAL_ROOT)
endif
	touch $@

openssl-clean:
	-rm openssl-build-stamp
	-rm lib/libcrypto.a
	-rm lib/libssl.a
	-make -C openssl uninstall_dev > /dev/null
	-make -C openssl clean
	-cd openssl && \
		git clean -fdx && \
		git reset --hard && \
		git submodule foreach --recursive git clean -fdx && \
		git submodule foreach --recursive git reset --hard

#------------------------------------------------------------------------------#
# libevent

libevent/Makefile: libevent/Makefile.am libevent/configure.ac
	cd libevent && ./autogen.sh
	cd libevent && ./configure \
				LDFLAGS="-L../lib" \
				CFLAGS="-I../include" \
				--host=$(ALTHOST) \
				--disable-libevent-regress \
				--disable-samples \
				--disable-openssl \
				--disable-shared \
				--prefix=/

libevent-build-stamp: openssl-build-stamp libevent/Makefile
	$(MAKE) -C libevent install DESTDIR=$(EXTERNAL_ROOT)
	touch $@

libevent: libevent-build-stamp

libevent-clean:
	-rm -f lib/libevent.a
	-rm -f libevent-build-stamp
	-$(MAKE) -C libevent uninstall DESTDIR=$(EXTERNAL_ROOT)
	-$(MAKE) -C libevent clean
	-cd libevent && \
		git clean -fdx && \
		git reset --hard

#------------------------------------------------------------------------------#
# lzma

xz/Makefile:
	cd xz && ./autogen.sh
	cd xz && \
		CC="$(CC)" \
		CXX="$(CXX)" \
		PKG_CONFIG_PATH="$(EXTERNAL_ROOT)/lib/pkgconfig" \
		AR="$(AR)" \
		RANLIB="$(RANLIB)" \
		CFLAGS="$(CFLAGS) -std=c99" \
		LDFLAGS="$(LDFLAGS)"  \
		 ./configure \
			--host=$(HOST) \
			--enable-static \
			--disable-doc \
			--disable-lzma-links \
			--disable-lzmadec \
			--disable-lzmainfo \
			--disable-xz \
			--disable-xzdec \
			--disable-scripts \
			--disable-shared \
			--prefix=$(EXTERNAL_ROOT)

lzma: lzma-build-stamp
	$(MAKE) -C xz install

lzma-build-stamp: xz/Makefile
	touch lzma-build-stamp

lzma-clean:
	-rm -rf include/lzma
	-rm -f include/lzma.h
	-rm -f lib/liblzma.a
	-rm -f lib/liblzma.la
	-rm -f lib/pkgconfig/liblzma.pc
	-rm -f lzma-build-stamp
	-cd xz && \
		git clean -fdx
#------------------------------------------------------------------------------#
# zstd

zstd: zstd-build-stamp
	$(MAKE) -C zstd/lib \
		CC="$(CC)" \
		CXX="$(CXX)" \
		AR="$(AR)" \
		RANLIB="$(RANLIB)" \
		CFLAGS="$(CFLAGS)"  \
		LDFLAGS="$(LDFLAGS)" \
		PREFIX="$(EXTERNAL_ROOT)" \
		libzstd.a-mt
	$(MAKE) -C zstd/lib \
		CC="$(CC)" \
		CXX="$(CXX)" \
		AR="$(AR)" \
		RANLIB="$(RANLIB)" \
		CFLAGS="$(CFLAGS)"  \
		LDFLAGS="$(LDFLAGS)" \
		PREFIX="$(EXTERNAL_ROOT)" \
		libzstd.pc
	test -d lib || mkdir lib
	test -d lib/pkgconfig || mkdir lib/pkgconfig
	test -d include || mkdir include
	cp zstd/lib/libzstd.a lib
	cp zstd/lib/libzstd.pc lib/pkgconfig
	cp zstd/lib/zstd.h include
	cp zstd/lib/deprecated/zbuff.h include
	cp zstd/lib/zstd_errors.h include
	cp zstd/lib/zdict.h include

zstd-build-stamp:
	touch zstd-build-stamp

zstd-clean:
	-rm -f include/zstd.h include/zstd_errors.h include/zbuff.h include/zdict.h
	-rm -f lib/libzstd.a
	-rm -f lib/pkgconfig/libzstd.pc
	-rm -f zstd-build-stamp
	-cd zstd && \
		git clean -fdx

#------------------------------------------------------------------------------#
# zlib


zlib-build-stamp:
	touch zlib-build-stamp

zlib: zlib-build-stamp
	cd zlib && \
		CC="$(CC)" \
		CXX="$(CXX)" \
		PKG_CONFIG_PATH="$(EXTERNAL_ROOT)/lib/pkgconfig" \
		AR="$(AR)" \
		RANLIB="$(RANLIB)" \
		CFLAGS="$(CFLAGS)" \
		LDFLAGS="$(LDFLAGS)" \
		./configure \
			--static \
			--prefix=$(EXTERNAL_ROOT)
	$(MAKE) -C zlib install

zlib-clean:
	-rm -f lib/libz.a
	-rm -f lib/pkgconfig/zlib.pc
	-rm -f include/zconf.h
	-rm -f include/zlib.h
	-rm -f zlib-build-stamp
	-rm -f share/man/man3/zlib.3
	-cd zlib && \
		git clean -fdx

#------------------------------------------------------------------------------#
# iptables

iptables/Makefile:
	cp iptables-patch-1 iptables
	cp iptables-patch-2 iptables
	cp iptables-patch-3 iptables
	cp iptables-patch-4 iptables
	-cd iptables && \
	 	patch -N -p1 --reject-file=- < iptables-patch-1
	-cd iptables && \
	 	patch -N -p1 --reject-file=- < iptables-patch-2
	-cd iptables && \
	 	patch -N -p1 --reject-file=- < iptables-patch-3
	-cd iptables && \
	 	patch -N -p0 --reject-file=- < iptables-patch-4
	cd iptables && ./autogen.sh
	cd iptables && \
		CC="$(CC)" \
		AR="$(AR)" \
		RANLIB=$(RANLIB) \
		CFLAGS="$(CFLAGS) -I$(EXTERNAL_ROOT)/include" \
		LDFLAGS="$(LDFLAGS)" \
			./configure \
				--host=$(HOST) \
				--disable-shared \
				--enable-static

iptables-build-stamp: iptables/Makefile
	$(MAKE) -C iptables 
	touch iptables-build-stamp
	cp iptables/iptables/xtables-multi bin/xtables

iptables: iptables-build-stamp

iptables-clean:
	-rm -f iptables-build-stamp
	-rm bin/xtables
	-cd iptables && \
		git clean -fdx

#------------------------------------------------------------------------------#
# tor

tor/Makefile:
	@which pkg-config || (echo "ERROR: pkg-config is required! apt-get install pkg-config"; exit 1)
	cd tor && ./autogen.sh
	cd tor && \
		CC="$(CC)" \
		AR="$(AR)" \
		RANLIB=$(RANLIB) \
		LDFLAGS="$(LDFLAGS)" \
		LIBS="-L$(EXTERNAL_ROOT)/lib" \
		CFLAGS="$(CFLAGS) -I$(EXTERNAL_ROOT)/include" \
		LZMA_CFLAGS="-I$(EXTERNAL_ROOT)/include -DLZMA_API_STATIC" \
		LZMA_LIBS="$(EXTERNAL_ROOT)/lib/liblzma.a" \
		ZSTD_CFLAGS="-I$(EXTERNAL_ROOT)/include" \
		ZSTD_LIBS="$(EXTERNAL_ROOT)/lib/libzstd.a" \
		PKG_CONFIG_PATH="$(EXTERNAL_ROOT)/lib/pkgconfig" \
			./configure \
				--host=$(HOST) \
				--enable-android \
				--enable-gpl \
				--enable-lzma \
				--enable-pic \
				--enable-static-libevent --with-libevent-dir=$(EXTERNAL_ROOT) \
				--enable-static-openssl --with-openssl-dir=$(EXTERNAL_ROOT) \
				--enable-static-zlib --with-zlib-dir=$(EXTERNAL_ROOT) \
				--enable-zstd \
				--disable-asciidoc \
				--disable-html-manual \
				--disable-libscrypt \
				--disable-manpage \
				--disable-seccomp \
				--disable-system-torrc \
				--disable-systemd \
				--disable-tool-name-check \
				--prefix=$(EXTERNAL_ROOT)

tor-build-stamp: tor/Makefile
	$(MAKE) -C tor all-am
	touch tor-build-stamp

tor: zlib lzma zstd libevent openssl tor-build-stamp
	test -d bin || mkdir bin
	cp tor/src/app/tor bin
	cp tor/src/config/geoip bin
	cp tor/src/config/geoip6 bin

tor-clean:
	-rm -f bin/tor
	-rm -f bin/geoip
	-rm -f bin/geoip6
	-rm -f tor-build-stamp
	-cd tor && \
		git clean -fdx
	-cd tor && \
		git reset HEAD --hard

#------------------------------------------------------------------------------#
# Lyrebird Pluggable Transports

lyrebird/bin:
	cd lyrebird/cmd/lyrebird && \
		CGO_ENABLED=1 \
		GOOS=android \
		GOARCH=$(NDK_ABI) \
		go build -v -ldflags="-s -w -checklinkname=0"

lyrebird-build-stamp: lyrebird/bin
	touch lyrebird-build-stamp

lyrebird: lyrebird-build-stamp
	test -d bin || mkdir bin
	cp lyrebird/cmd/lyrebird/lyrebird bin

lyrebird-clean:
	-rm -f bin/lyrebird
	-rm -f lyrebird-build-stamp

#------------------------------------------------------------------------------#
#create and clean assets: FYI - tor is stored as a ZIP file with an mp3 extension
#in order to stop Android OS (older devices) from trying to compress/decompress it
#this is related to a bug in compression of assets and resources > 1MB

assets: tor iptables lyrebird
	install -d ../orbotservice/src/main/assets/$(APP_ABI)
ifneq ($(DEBUG), 1)
	-$(STRIP) --strip-unneeded -R .note -R .comment --strip-debug bin/tor
	-$(STRIP) --strip-unneeded -R .note -R .comment --strip-debug bin/xtables
	-$(STRIP) --strip-unneeded -R .note -R .comment --strip-debug ../orbotservice/src/main/libs/$(APP_ABI)/pdnsd
endif
	-zip ../orbotservice/src/main/assets/geoip.mp3 bin/geoip
	-zip ../orbotservice/src/main/assets/geoip6.mp3 bin/geoip6
	$(EXTERNAL_ROOT)/bin/termux-elf-cleaner --api-level $(NDK_PLATFORM_LEVEL) bin/tor
	-zip ../orbotservice/src/main/assets/$(APP_ABI)/tor.mp3 bin/tor
	$(EXTERNAL_ROOT)/bin/termux-elf-cleaner --api-level $(NDK_PLATFORM_LEVEL) bin/xtables
	-zip ../orbotservice/src/main/assets/$(APP_ABI)/xtables.mp3 bin/xtables
	$(EXTERNAL_ROOT)/bin/termux-elf-cleaner --api-level $(NDK_PLATFORM_LEVEL) bin/lyrebird
	-zip ../orbotservice/src/main/assets/$(APP_ABI)/lyrebird.mp3 bin/lyrebird
	$(EXTERNAL_ROOT)/bin/termux-elf-cleaner --api-level $(NDK_PLATFORM_LEVEL) ../orbotservice/src/main/libs/$(APP_ABI)/pdnsd
	-zip ../orbotservice/src/main/assets/$(APP_ABI)/pdnsd.mp3 ../orbotservice/src/main/libs/$(APP_ABI)/pdnsd
	-rm ../orbotservice/src/main/libs/$(APP_ABI)/pdnsd

assets-clean:
	-rm ../orbotservice/src/main/assets/$(APP_ABI)/tor.mp3
	-rm ../orbotservice/src/main/assets/$(APP_ABI)/xtables.mp3
	-rm ../orbotservice/src/main/assets/$(APP_ABI)/lyrebird.mp3
	-rm ../orbotservice/src/main/assets/$(APP_ABI)/pdnsd.mp3


#------------------------------------------------------------------------------#
# cleanup, cleanup, put the toys away

##clean: openssl-clean libevent-clean tor-clean assets-clean
clean: zlib-clean openssl-clean libevent-clean lzma-clean zstd-clean tor-clean iptables-clean lyrebird-clean assets-clean

#------------------------------------------------------------------------------#
# debugging stuff

showsetup:
	@echo "NDK_TOOLCHAIN_VERSION: $(NDK_TOOLCHAIN_VERSION)"
	@echo "NDK_TOOLCHAIN: $(NDK_TOOLCHAIN)"
	@echo "NDK_PLATFORM_LEVEL: $(NDK_PLATFORM_LEVEL)"
	@echo "NDK_SYSROOT: $(NDK_SYSROOT)"
	@echo "APP_ABI: $(APP_ABI)"
	@echo "HOST: $(HOST)"
	@echo "CC: $(CC)"
	@echo "LD: $(LD)"
	@echo "CFLAGS: $(CFLAGS)"
	@echo "LDFLAGS: $(LDFLAGS)"
