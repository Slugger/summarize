#!/bin/bash
#
# Copyright 2015 Battams, Derek
# 
#	Licensed under the Apache License, Version 2.0 (the "License");
#	you may not use this file except in compliance with the License.
#	You may obtain a copy of the License at
# 
#		http://www.apache.org/licenses/LICENSE-2.0
#
#	Unless required by applicable law or agreed to in writing, software
#	distributed under the License is distributed on an "AS IS" BASIS,
#	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#	See the License for the specific language governing permissions and
#	limitations under the License.
#
set -e
rm -rf build
cd ..
./gradlew clean war
cd docker
mkdir -p build/static build/webapps
cp -r ../jquery build/static/.
cp -r ../kickstart/* build/static/.
cp -r ../apps/* build/webapps/.
cp -r ../build/libs/*.war build/webapps/summarize.war
cp Dockerfile build/.
