/*
 * Copyright 2012 zhongl
 *
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

package com.github.zhongl.sequence;

import com.github.zhongl.page.Accessor;
import com.github.zhongl.page.Page;
import com.github.zhongl.page.ReadOnlyChannels;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static java.util.Collections.singletonList;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
@NotThreadSafe
class LinkedPage<T> implements Comparable<Cursor>, Closeable {
    private final File file;
    private final Accessor<T> accessor;
    private final int capacity;
    private final InnerPage page;
    private final ReadOnlyChannels readOnlyChannels;

    private volatile int position;

    LinkedPage(File file, Accessor<T> accessor, int capacity, ReadOnlyChannels readOnlyChannels) throws IOException {
        this.file = file;
        this.accessor = accessor;
        this.capacity = capacity;
        this.readOnlyChannels = readOnlyChannels;
        page = new InnerPage(file, accessor);
        position = (int) file.length();
    }

    LinkedPage(File file, Accessor<T> accessor, ReadOnlyChannels readOnlyChannels) throws IOException {
        this(file, accessor, (int) file.length(), readOnlyChannels);
    }

    public Cursor append(T object) throws OverflowException, IOException {
        Accessor.Writer writer = accessor.writer(object);

        if (position + writer.byteLength() > capacity) throw new OverflowException();

        Cursor cursor = new Cursor(begin() + position);
        position += page.add(object);
        return cursor;
    }

    public LinkedPage<T> multiply() throws IOException {
        File newFile = new File(file.getParentFile(), begin() + length() + "");
        return new LinkedPage<T>(newFile, accessor, capacity, readOnlyChannels);
    }

    public T get(Cursor cursor) throws IOException {
        int offset = (int) (cursor.offset - begin());
        FileChannel readOnlyChannel = readOnlyChannels.getOrCreateBy(file);
        readOnlyChannel.position(offset);
        return accessor.reader().readFrom(readOnlyChannel);
    }

    public Cursor next(Cursor cursor) throws IOException {
        return cursor.forword(accessor.writer(get(cursor)).byteLength());
    }

    @Override
    public int compareTo(Cursor cursor) {
        if (cursor.offset < begin()) return 1;
        if (cursor.offset >= begin() + length()) return -1;
        return 0;
    }

    public void fix() throws IOException {
        page.fix();
    }

    @Override
    public void close() throws IOException {
        fix();
        readOnlyChannels.close(file);
    }

    public long begin() {
        return page.number();
    }

    public long length() {
        return position;
    }

    public void clear() {
        page.clear();
    }

    /**
     * There are three split cases:
     * <p/>
     * <pre>
     * Case 1: split to three pieces and keep left and right.
     *         begin                 end
     *    |@@@@@@|--------------------|@@@@@@@@|
     *
     * Case 2: split to two pieces and keep right
     *  begin                   end
     *    |----------------------|@@@@@@@@@@@@@|
     *
     * Case 3: too small interval to split
     *  begin end
     *    |@@@@|@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@|
     *
     * </pre>
     */
    public List<LinkedPage<T>> split(Cursor begin, Cursor end) throws IOException {
        T value = get(begin);
        if (end.offset - begin.offset < accessor.writer(value).byteLength())
            return singletonList(this);                                         // Case 3
        if (begin.offset == begin()) return singletonList(right(end));          // Case 2
        LinkedPage<T> right = right0(end); // do right first for avoiding delete by left
        LinkedPage<T> left = left(begin);
        return Arrays.asList(left, right);                                       // Case 1
    }

    /**
     * There are two right cases:
     * <pre>
     * Case 1: keep right and abandon left.
     *         cursor
     *    |------|@@@@@@@@@@@@@@@@@@@@@@@@@@@@@|
     *
     * Case 2: keep all because cursor is begin or too small interval
     *  cursor
     *    |@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@|
     * </pre>
     */
    public LinkedPage<T> right(Cursor cursor) throws IOException {
        if (cursor.offset == begin()) return this;        // Case 2
        LinkedPage<T> chunk = right0(cursor);
        clear();                                    // Case 1
        return chunk;
    }

    /**
     * There are throe left cases:
     * <pre>
     * Case 1: keep left and abandon right.
     *         cursor
     *    |@@@@@@|-----------------------------|
     *
     * Case 2: abandon all
     *  cursor
     *    |------------------------------------|
     *
     * </pre>
     */
    public LinkedPage<T> left(Cursor cursor) throws IOException {
        close();
        if (cursor.offset <= begin()) {                                          // Case 2
            clear();
            return null;
        }

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.setLength(cursor.offset - begin());
        randomAccessFile.close();
        return new LinkedPage<T>(file, accessor, readOnlyChannels);          // Case 1
    }

    private LinkedPage<T> right0(Cursor cursor) throws IOException {
        File newFile = new File(file.getParentFile(), Long.toString(cursor.offset));
        long offset = cursor.offset - begin();
        long length = length() - offset;

        Files.copy(ByteStreams.slice(Files.newInputStreamSupplier(file), offset, length), newFile);

        return new LinkedPage<T>(newFile, accessor, readOnlyChannels);
    }

    private class InnerPage extends Page<T> {

        public InnerPage(File file, Accessor<T> accessor) throws IOException {
            super(file, accessor);
        }

        @Override
        public Iterator<T> iterator() {
            return null;
        }

        protected WritableByteChannel createWriteOnlyChannel(File file) throws FileNotFoundException {
            return new FileOutputStream(file).getChannel();
        }
    }
}