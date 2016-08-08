FROM clojure:onbuild

WORKDIR /tmp

ENV WATCHMAN_VERSION=4.5.0

RUN apt-get update && apt-get install -y autogen autoconf make gcc python-dev

RUN wget --quiet https://github.com/facebook/watchman/archive/v$WATCHMAN_VERSION.tar.gz -O /tmp/watchman-v$WATCHMAN_VERSION.tar.gz \
    && tar -C /tmp/ -zxf watchman-v$WATCHMAN_VERSION.tar.gz \
    && cd /tmp/watchman-4.5.0/ && ./autogen.sh && ./configure && make && make install

ENV PATH $PATH:node_modules/.bin

RUN wget -qO- https://deb.nodesource.com/setup_4.x | bash -
RUN apt-get install -y nodejs
RUN npm install -g react-native-cli re-natal

# Install 32bit support for Android SDK
RUN dpkg --add-architecture i386 && \
    apt-get update -q && \
    apt-get install -qy --no-install-recommends libstdc++6:i386 libgcc1:i386 zlib1g:i386 libncurses5:i386

##
## Install Android SDK
##
# Set correct environment variables.
ENV ANDROID_SDK_FILE android-sdk_r24.0.1-linux.tgz
ENV ANDROID_SDK_URL http://dl.google.com/android/$ANDROID_SDK_FILE

# # Install Android SDK
ENV ANDROID_HOME /usr/local/android-sdk-linux
RUN cd /usr/local && \
    wget $ANDROID_SDK_URL && \
    tar -xzf $ANDROID_SDK_FILE && \
    export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools && \
    chgrp -R users $ANDROID_HOME && \
    chmod -R 0775 $ANDROID_HOME && \
    rm $ANDROID_SDK_FILE

ENV PATH $PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/23.0.1

# --- Install Android SDKs and other build packages

# Other tools and resources of Android SDK
#  you should only install the packages you need!
# To get a full list of available options you can use:
#  android list sdk --no-ui --all --extended
# (!!!) Only install one package at a time, as "echo y" will only work for one license!
#       If you don't do it this way you might get "Unknown response" in the logs,
#         but the android SDK tool **won't** fail, it'll just **NOT** install the package.
RUN echo y | android update sdk --no-ui --all --filter platform-tools
RUN echo y | android update sdk --no-ui --all --filter extra-android-support

# google apis
# Please keep these in descending order!
RUN echo y | android update sdk --no-ui --all --filter addon-google_apis-google-23

# SDKs
# Please keep these in descending order!
RUN echo y | android update sdk --no-ui --all --filter android-23

# build tools
# Please keep these in descending order!
RUN echo y | android update sdk --no-ui --all --filter build-tools-23.0.1

# Android System Images, for emulators
# Please keep these in descending order!
RUN echo y | android update sdk --no-ui --all --filter sys-img-x86_64-android-23
RUN echo y | android update sdk --no-ui --all --filter sys-img-armeabi-v7a-android-23
# Extras
RUN echo y | android update sdk --no-ui --all --filter extra-android-m2repository
RUN echo y | android update sdk --no-ui --all --filter extra-google-m2repository
RUN echo y | android update sdk --no-ui --all --filter extra-google-google_play_services

RUN echo y | android update sdk --no-ui --all --filter tools

# Create emulator
RUN echo "no" | android create avd \
                --force \
                --name test \
                --target android-23 \
                --abi armeabi-v7a \
                --skin WVGA800 \
                --sdcard 512M

# Support Gradle
ENV TERM dumb
ENV JAVA_OPTS -Xms256m -Xmx512m

ENV GRADLE_USER_HOME /usr/src/app/android/gradle_deps

WORKDIR /usr/src/app

CMD ["lein", "run"]
