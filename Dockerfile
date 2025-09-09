FROM ubuntu:20.04

# Noninteractive mode for apt
ENV DEBIAN_FRONTEND=noninteractive

# Install build deps
RUN apt-get update && apt-get install -y \
    git curl unzip wget make automake autoconf libtool pkg-config \
    zlib1g-dev libncurses5-dev openjdk-8-jdk \
    python2 python3 zip \
    && rm -rf /var/lib/apt/lists/*

# Setup environment
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ENV ANDROID_SDK=/opt/android-sdk
ENV ANDROID_NDK=/opt/android-ndk-r10e
ENV PATH=$ANDROID_SDK/tools:$ANDROID_SDK/platform-tools:$PATH

# Install Android SDK (old r24.4.1)
RUN wget https://dl.google.com/android/android-sdk_r24.4.1-linux.tgz -O /tmp/sdk.tgz \
    && tar -xzf /tmp/sdk.tgz -C /opt \
    && mv /opt/android-sdk-linux $ANDROID_SDK \
    && rm /tmp/sdk.tgz

# Install Android NDK r10e
RUN wget https://dl.google.com/android/repository/android-ndk-r10e-linux-x86_64.bin -O /tmp/ndk.bin \
    && chmod +x /tmp/ndk.bin \
    && /tmp/ndk.bin -o /opt -y \
    && rm /tmp/ndk.bin \
    && mv /opt/android-ndk-r10e $ANDROID_NDK

# Install Gradle 2.10
RUN wget https://services.gradle.org/distributions/gradle-2.10-bin.zip -O /tmp/gradle.zip \
    && unzip /tmp/gradle.zip -d /opt \
    && rm /tmp/gradle.zip
ENV PATH=/opt/gradle-2.10/bin:$PATH

WORKDIR /workspace

