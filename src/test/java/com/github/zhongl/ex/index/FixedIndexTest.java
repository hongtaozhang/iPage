/*
 * Copyright 2012 zhongl
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.zhongl.ex.index;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class FixedIndexTest extends IndexTest {
    protected Index newIndex(File dir) throws IOException {return new FixedIndex(dir);}
}
