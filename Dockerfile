FROM ubuntu:16.04

ENV DEBIAN_FRONTEND=noninteractive

# Install required build dependencies
RUN apt-get update && apt-get install -y \
    git curl unzip wget make automake autoconf libtool pkg-config \
    zlib1g-dev libncurses5-dev openjdk-8-jdk \
    python2 python3 zip \
    cmake ragel protobuf-compiler ant bison flex nasm gettext help2man \
    meson ninja-build \
    && rm -rf /var/lib/apt/lists/*

# Java environment
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

# Android SDK paths
ENV ANDROID_SDK=/opt/android-sdk
ENV PATH=$ANDROID_SDK/tools:$ANDROID_SDK/platform-tools:$PATH

# Install Android SDK (r24.4.1 is stable for KitKat builds)
RUN wget https://dl.google.com/android/android-sdk_r24.4.1-linux.tgz -O /tmp/sdk.tgz \
    && tar -xzf /tmp/sdk.tgz -C /opt \
    && mv /opt/android-sdk-linux $ANDROID_SDK \
    && rm /tmp/sdk.tgz

# Install Android NDK r10e
RUN wget https://dl.google.com/android/repository/android-ndk-r10e-linux-x86_64.zip -O /tmp/ndk.zip \
    && unzip /tmp/ndk.zip -d /opt \
    && rm /tmp/ndk.zip
ENV ANDROID_NDK=/opt/android-ndk-r10e

# Install Gradle 2.10
RUN wget https://services.gradle.org/distributions/gradle-2.10-bin.zip -O /tmp/gradle.zip \
    && unzip /tmp/gradle.zip -d /opt \
    && rm /tmp/gradle.zip
ENV PATH=/opt/gradle-2.10/bin:$PATH

# Workaround for gnulib fseterr.c portability issue
ENV ac_cv_func_fseterr=no

WORKDIR /workspace
