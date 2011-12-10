/*
 * Copyright 2011 zhongl                                                                                                                              
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

package com.github.zhongl.ipage;

import com.github.zhongl.accessor.Accessor;
import com.github.zhongl.integerity.Validator;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link com.github.zhongl.ipage.ReadOnlyChunk}
 *
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a>
 */
@NotThreadSafe
class ReadOnlyChunk<T> extends Chunk<T> {

    private final File file;
    private final int capacity;
    private final int minimizeCollectLength;

    public ReadOnlyChunk(File file, long beginPosition, int capacity, Accessor<T> accessor, int minimizeCollectLength) throws IOException {
        super(file, beginPosition, accessor, capacity, true);
        this.file = file;
        this.capacity = capacity;
        this.minimizeCollectLength = minimizeCollectLength;
    }

    @Override
    public long append(T object) throws ReadOnlyBufferException, BufferOverflowException {
        throw new ReadOnlyBufferException();
    }

    @Override
    public long endPosition() {
        return beginPosition() + capacity - 1;
    }

    @Override
    public void close() {
        mappedBufferFile.release();
    }

    @Override
    public boolean validateOrRecoverBy(Validator<T, IOException> validator) throws IOException {
        return true; // unsupport for read only chunk
    }

    /** @see Chunk#split(long, long) */
    @Override
    public List<Chunk<T>> split(long begin, long end) throws IOException {
        T value = get(begin);
        long minimizeInterval = (long) Math.max(accessor.byteLengthOf(value), minimizeCollectLength);
        if (end - begin <= minimizeInterval) return Collections.emptyList();                // Case 3
        if (begin == beginPosition()) return Arrays.asList(right(end));                     // Case 2
        Chunk<T> right = right0(end); // do right first for avoiding delete by left
        Chunk<T> left = left(begin);
        return Arrays.asList(left, right);                                                  // Case 1
    }

    /** @see Chunk#left(long) */
    @Override
    public Chunk<T> left(long offset) throws IOException {
        if (offset == beginPosition()) {                                                    // Case 2
            delete();
            return null;
        }
        close();
        long size = offset - beginPosition();
        truncate(file, size);                                                               // Case 1
        return new ReadOnlyChunk(file, beginPosition(), (int) size, accessor, minimizeCollectLength);
    }

    /** @see Chunk#right(long) */
    @Override
    public Chunk<T> right(long offset) throws IOException {
        if (offset == beginPosition()) return this;  // Case 2
        Chunk<T> chunk = right0(offset);
        delete();                                    // Case 1
        return chunk;
    }

    private Chunk<T> right0(long offset) throws IOException {
        File newFile = new File(file.getParentFile(), Long.toString(offset));
        long length = endPosition() - offset + 1;
        offset -= beginPosition();
        InputSupplier<InputStream> from = ByteStreams.slice(Files.newInputStreamSupplier(file), offset, length);
        Files.copy(from, newFile);
        return new ReadOnlyChunk(newFile, offset, (int) newFile.length(), accessor, minimizeCollectLength);
    }
}
