# Copyright 2017 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

hamcrest_jar_path="third_party/hamcrest-core-1.3.jar"
junit_jar_path="third_party/junit4-4.11.jar"

if [ -f $hamcrest_jar_path ] ; then
        mv $hamcrest_jar_path third_party/hamcrest-core.jar
fi

if [ -f $junit_jar_path ] ; then
        mv $junit_jar_path third_party/junit4.jar
fi

mkdir -p bin

javac -Xlint $(find * | grep "\\.java$") -d ./bin -sourcepath src -cp third_party/junit4.jar
javac -Xlint $(find * | grep "\\.java$") -d ./bin -sourcepath test -cp third_party/junit4.jar
