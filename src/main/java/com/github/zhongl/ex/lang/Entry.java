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

package com.github.zhongl.ex.lang;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class Entry<K extends Comparable<K>, V> implements Comparable<Entry<K, V>> {

    private final K key;
    private final V value;

    public Entry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K key() {
        return key;
    }

    public V value() {
        return value;
    }

    @Override
    public int compareTo(Entry<K, V> o) {
        return key.compareTo(o.key);
    }
}
